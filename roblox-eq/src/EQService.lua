-- EQService (ModuleScript)
-- Miejsce: ServerScriptService > EQSystem > EQService
-- Cała logika ekwipunku po stronie serwera. Serwer decyduje o wszystkim,
-- klient tylko wysyła prośby (przenieś, użyj, wyrzuć...), więc nie da się
-- zhakować przedmiotów z poziomu klienta.
--
-- Użycie z innych skryptów serwera:
--   local EQ = require(game.ServerScriptService.EQSystem.EQService)
--   EQ.AddItem(player, "Diament", 3)      --> zwraca ile się NIE zmieściło (0 = wszystko weszło)
--   EQ.RemoveItem(player, "Drewno", 10)   --> true / false (false = za mało, nic nie zabrano)
--   EQ.CountItem(player, "Zloto")         --> liczba sztuk
--   EQ.HasItem(player, "Kilof")           --> true / false
--   EQ.HasSpace(player, "Drewno", 64)     --> czy zmieści się tyle sztuk
--   EQ.Notify(player, "Tekst", Color3)    --> powiadomienie na ekranie
--   EQ.ItemAdded / EQ.ItemRemoved         --> BindableEvent (player, id, amount)

local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local ServerStorage = game:GetService("ServerStorage")
local DataStoreService = game:GetService("DataStoreService")
local RunService = game:GetService("RunService")
local Debris = game:GetService("Debris")

local Shared = ReplicatedStorage:WaitForChild("EQShared")
local Config = require(Shared:WaitForChild("EQConfig"))
local Items = require(Shared:WaitForChild("ItemData"))

local MAX = Config.MaxSlots
local HOTBAR = Config.HotbarSlots

local EQ = {}
EQ.ItemAdded = Instance.new("BindableEvent")
EQ.ItemRemoved = Instance.new("BindableEvent")

local profiles = {} -- [Player] = profile
local started = false
local store = nil
local UpdateEvent, ActionEvent, NotifyEvent

-- ════════════════════════ pomocnicze ════════════════════════
local function maxStack(id)
	local def = Items[id]
	return (def and def.MaxStack and def.MaxStack >= 1) and math.floor(def.MaxStack) or 1
end

local function rarity(id)
	local def = Items[id]
	return Config.Rarities[def and def.Rarity or "Common"] or Config.Rarities.Common
end

local function emptySlots()
	local t = table.create(MAX, false)
	return t
end

local function validSlot(x)
	return type(x) == "number" and x == x and x % 1 == 0 and x >= 1 and x <= MAX
end

local function validAmount(x)
	return type(x) == "number" and x == x and x % 1 == 0 and x >= 1 and x <= 100000
end

local function serialize(slots)
	local out = table.create(MAX, false)
	for i = 1, MAX do
		local s = slots[i]
		if s then
			out[i] = { id = s.id, n = s.n }
		end
	end
	return out
end

local function sync(player)
	local p = profiles[player]
	if not p or not p.loaded then
		return
	end
	UpdateEvent:FireClient(player, {
		slots = serialize(p.slots),
		equipped = p.equipped or 0,
		max = MAX,
		hotbar = HOTBAR,
	})
end

function EQ.Notify(player, text, color)
	if NotifyEvent and player and player.Parent then
		NotifyEvent:FireClient(player, tostring(text), color or Config.Theme.Text)
	end
end

-- ════════════════════════ dodawanie / zabieranie ════════════════════════
-- Dodaje bez synchronizacji. Zwraca resztę, która się nie zmieściła.
local function addRaw(p, id, amount)
	local stack = maxStack(id)
	local left = amount
	-- 1) dopełnij istniejące stosy
	if stack > 1 then
		for i = 1, MAX do
			local s = p.slots[i]
			if s and s.id == id and s.n < stack then
				local put = math.min(stack - s.n, left)
				s.n += put
				left -= put
				if left <= 0 then
					return 0
				end
			end
		end
	end
	-- 2) wolne sloty (najpierw pasek, potem plecak)
	for i = 1, MAX do
		if not p.slots[i] then
			local put = math.min(stack, left)
			p.slots[i] = { id = id, n = put }
			left -= put
			if left <= 0 then
				return 0
			end
		end
	end
	return left
end

local function spaceFor(p, id)
	local stack = maxStack(id)
	local space = 0
	for i = 1, MAX do
		local s = p.slots[i]
		if not s then
			space += stack
		elseif s.id == id then
			space += stack - s.n
		end
	end
	return space
end

local function count(p, id)
	local n = 0
	for i = 1, MAX do
		local s = p.slots[i]
		if s and s.id == id then
			n += s.n
		end
	end
	return n
end

-- Pilnuje, żeby trzymany Tool zgadzał się ze slotem (po przeniesieniu itd.)
local unequip
local function fixEquipped(player, p)
	if not p.equippedRef then
		p.equipped = nil
		return
	end
	for i = 1, MAX do
		if p.slots[i] == p.equippedRef then
			p.equipped = i
			if p.tool then
				p.tool:SetAttribute("EQSlot", i)
			end
			return
		end
	end
	unequip(player, p)
end

function EQ.AddItem(player, id, amount)
	amount = math.floor(tonumber(amount) or 1)
	local p = profiles[player]
	if not p or not p.loaded or not Items[id] or amount < 1 then
		return amount
	end
	local left = addRaw(p, id, amount)
	if amount - left > 0 then
		EQ.ItemAdded:Fire(player, id, amount - left)
	end
	sync(player)
	return left
end

function EQ.RemoveItem(player, id, amount)
	amount = math.floor(tonumber(amount) or 1)
	local p = profiles[player]
	if not p or not p.loaded or count(p, id) < amount then
		return false
	end
	local left = amount
	-- zabieraj od końca (najpierw plecak, potem pasek)
	for i = MAX, 1, -1 do
		local s = p.slots[i]
		if s and s.id == id then
			local take = math.min(s.n, left)
			s.n -= take
			left -= take
			if s.n <= 0 then
				p.slots[i] = false
			end
			if left <= 0 then
				break
			end
		end
	end
	fixEquipped(player, p)
	EQ.ItemRemoved:Fire(player, id, amount)
	sync(player)
	return true
end

function EQ.CountItem(player, id)
	local p = profiles[player]
	return p and p.loaded and count(p, id) or 0
end

function EQ.HasItem(player, id, amount)
	return EQ.CountItem(player, id) >= (amount or 1)
end

function EQ.HasSpace(player, id, amount)
	local p = profiles[player]
	return p ~= nil and p.loaded and spaceFor(p, id) >= (amount or 1)
end

function EQ.GetInventory(player)
	local p = profiles[player]
	return p and p.loaded and serialize(p.slots) or nil
end

function EQ.IsLoaded(player)
	local p = profiles[player]
	return p ~= nil and p.loaded == true
end

-- ════════════════════════ narzędzia (Tool) ════════════════════════
local function toolsFolder()
	return ServerStorage:FindFirstChild("EQTools")
end

local SWORD_GRIP = CFrame.new(0, 0, -1.7) * CFrame.fromMatrix(Vector3.zero, Vector3.new(0, 1, 0), Vector3.new(0, 0, 1))
local SWING_GRIP = SWORD_GRIP * CFrame.Angles(math.rad(-80), 0, 0)

local function attachMelee(tool, def)
	local last = 0
	tool.Activated:Connect(function()
		local char = tool.Parent
		local hum = char and char:FindFirstChildOfClass("Humanoid")
		local root = char and char:FindFirstChild("HumanoidRootPart")
		if not hum or not root or hum.Health <= 0 then
			return
		end
		local cd = def.Cooldown or 0.6
		if os.clock() - last < cd then
			return
		end
		last = os.clock()

		-- prosta animacja zamachu
		tool.Grip = SWING_GRIP
		task.delay(math.min(0.2, cd * 0.5), function()
			if tool.Parent then
				tool.Grip = SWORD_GRIP
			end
		end)

		local range = def.Range or 6
		local params = OverlapParams.new()
		params.FilterType = Enum.RaycastFilterType.Exclude
		params.FilterDescendantsInstances = { char }
		local boxCF = root.CFrame * CFrame.new(0, 0, -range / 2)
		local parts = workspace:GetPartBoundsInBox(boxCF, Vector3.new(5, 6, range), params)
		local hit = {}
		local attacker = Players:GetPlayerFromCharacter(char)
		for _, part in parts do
			local model = part:FindFirstAncestorOfClass("Model")
			local h = model and model:FindFirstChildOfClass("Humanoid")
			if h and not hit[h] and h.Health > 0 then
				hit[h] = true
				local victim = Players:GetPlayerFromCharacter(model)
				local friendly = victim and (not Config.PvP or (attacker and attacker.Team and victim.Team == attacker.Team and not attacker.Neutral))
				if not friendly then
					h:TakeDamage(def.Damage or 10)
				end
			end
		end
	end)
end

local function makeTool(id)
	local def = Items[id]
	local folder = toolsFolder()
	local template = folder and folder:FindFirstChild(def.ToolName or id)
	if template and template:IsA("Tool") then
		local t = template:Clone()
		t.CanBeDropped = false
		return t
	end

	-- automatyczny model, gdy nie ma własnego Toola
	local tool = Instance.new("Tool")
	tool.Name = def.Name
	tool.ToolTip = def.Name
	tool.CanBeDropped = false
	tool.RequiresHandle = true
	tool.Grip = SWORD_GRIP

	local handle = Instance.new("Part")
	handle.Name = "Handle"
	handle.Size = Vector3.new(0.4, 0.4, 4)
	handle.Material = Enum.Material.Metal
	handle.Color = def.Color or rarity(id).Color
	handle.CanCollide = false
	handle.Massless = true
	handle.Parent = tool

	local guard = Instance.new("Part")
	guard.Name = "Guard"
	guard.Size = Vector3.new(1.4, 0.3, 0.3)
	guard.Color = Color3.fromRGB(60, 50, 40)
	guard.CanCollide = false
	guard.Massless = true
	guard.CFrame = handle.CFrame * CFrame.new(0, 0, -1.1)
	local weld = Instance.new("WeldConstraint")
	weld.Part0 = handle
	weld.Part1 = guard
	weld.Parent = guard
	guard.Parent = tool

	if def.Damage then
		attachMelee(tool, def)
	end
	return tool
end

function unequip(player, p)
	local tool = p.tool
	p.tool = nil
	p.equipped = nil
	p.equippedRef = nil
	if tool then
		tool:Destroy()
	end
end

local function equip(player, p, slot)
	local s = p.slots[slot]
	if not s then
		unequip(player, p)
		return
	end
	local def = Items[s.id]
	if not def or def.Type ~= "Tool" then
		return
	end
	if p.equippedRef == s then
		unequip(player, p) -- drugi raz = schowaj
		return
	end
	unequip(player, p)
	local char = player.Character
	local hum = char and char:FindFirstChildOfClass("Humanoid")
	if not hum or hum.Health <= 0 then
		return
	end
	local tool = makeTool(s.id)
	tool:SetAttribute("EQToolItem", s.id)
	tool:SetAttribute("EQSlot", slot)
	p.tool = tool
	p.equipped = slot
	p.equippedRef = s
	tool.Parent = char
end

-- ════════════════════════ używanie ════════════════════════
local function applyBoost(hum, prop, base, add, duration, key)
	local baseAttr = "EQBase" .. prop
	if hum:GetAttribute(baseAttr) == nil then
		hum:SetAttribute(baseAttr, base)
	end
	local original = hum:GetAttribute(baseAttr)
	hum[prop] = original + add
	local token = (hum:GetAttribute(key) or 0) + 1
	hum:SetAttribute(key, token)
	task.delay(duration, function()
		if hum.Parent and hum:GetAttribute(key) == token then
			hum[prop] = original
			hum:SetAttribute(baseAttr, nil)
		end
	end)
end

local function consume(player, p, slot)
	local s = p.slots[slot]
	local def = s and Items[s.id]
	if not def then
		return
	end
	if def.Type == "Tool" then
		equip(player, p, slot)
		return
	end
	if def.Type ~= "Consumable" then
		EQ.Notify(player, "Tego przedmiotu nie można użyć", Config.Theme.Danger)
		return
	end
	if os.clock() - (p.lastUse or 0) < 0.4 then
		return
	end
	local char = player.Character
	local hum = char and char:FindFirstChildOfClass("Humanoid")
	if not hum or hum.Health <= 0 then
		return
	end
	local onlyHeal = def.Heal and not def.SpeedBoost and not def.JumpBoost
	if onlyHeal and hum.Health >= hum.MaxHealth then
		EQ.Notify(player, "Masz pełne zdrowie ❤️", Config.Theme.SubText)
		return
	end
	p.lastUse = os.clock()

	local duration = def.Duration or 10
	if def.Heal then
		hum.Health = math.min(hum.MaxHealth, hum.Health + def.Heal)
	end
	if def.SpeedBoost then
		applyBoost(hum, "WalkSpeed", hum.WalkSpeed, def.SpeedBoost, duration, "EQSpeedToken")
	end
	if def.JumpBoost then
		if hum.UseJumpPower then
			applyBoost(hum, "JumpPower", hum.JumpPower, def.JumpBoost, duration, "EQJumpToken")
		else
			applyBoost(hum, "JumpHeight", hum.JumpHeight, def.JumpBoost / 3, duration, "EQJumpToken")
		end
	end

	s.n -= 1
	if s.n <= 0 then
		p.slots[slot] = false
	end
	EQ.ItemRemoved:Fire(player, s.id, 1)
	EQ.Notify(player, "Użyto: " .. (def.Emoji or "") .. " " .. def.Name, rarity(s.id).Color)
end

-- ════════════════════════ przedmioty na ziemi ════════════════════════
local setupDone = setmetatable({}, { __mode = "k" })

local function itemLabel(id, amount)
	local def = Items[id]
	return (def.Emoji and def.Emoji .. " " or "") .. def.Name .. (amount > 1 and (" x" .. amount) or "")
end

local function setupPickup(inst)
	if setupDone[inst] then
		return
	end
	local id = inst:GetAttribute("EQItem")
	if type(id) ~= "string" then
		return
	end
	if not Items[id] then
		warn("[EQ] Nieznany przedmiot na mapie:", id, inst:GetFullName())
		return
	end
	local host
	if inst:IsA("BasePart") then
		host = inst
	elseif inst:IsA("Model") then
		host = inst.PrimaryPart or inst:FindFirstChildWhichIsA("BasePart", true)
	end
	if not host then
		return
	end
	setupDone[inst] = true

	local originalAmount = inst:GetAttribute("EQAmount") or 1
	local def = Items[id]

	local prompt = Instance.new("ProximityPrompt")
	prompt.Name = "EQPrompt"
	prompt.ActionText = "Podnieś"
	prompt.ObjectText = itemLabel(id, inst:GetAttribute("EQAmount") or 1)
	prompt.KeyboardKeyCode = Config.PickupKey
	prompt.HoldDuration = Config.PickupHoldTime
	prompt.MaxActivationDistance = Config.PickupDistance
	prompt.RequiresLineOfSight = false
	prompt.Parent = host

	inst:GetAttributeChangedSignal("EQAmount"):Connect(function()
		prompt.ObjectText = itemLabel(id, inst:GetAttribute("EQAmount") or 1)
	end)

	prompt.Triggered:Connect(function(player)
		if not inst.Parent then
			return
		end
		local p = profiles[player]
		if not p or not p.loaded then
			return
		end
		local amount = math.max(1, math.floor(inst:GetAttribute("EQAmount") or 1))
		local left = addRaw(p, id, amount)
		local taken = amount - left
		if taken <= 0 then
			EQ.Notify(player, "Ekwipunek jest pełny! 🎒", Config.Theme.Danger)
			return
		end
		EQ.ItemAdded:Fire(player, id, taken)
		sync(player)
		EQ.Notify(player, "+" .. taken .. "  " .. (def.Emoji or "") .. " " .. def.Name, rarity(id).Color)
		if left > 0 then
			inst:SetAttribute("EQAmount", left)
			EQ.Notify(player, "Nie wszystko się zmieściło", Config.Theme.SubText)
			return
		end
		local respawn = inst:GetAttribute("EQRespawn")
		if inst:GetAttribute("EQDropped") or type(respawn) ~= "number" or respawn <= 0 then
			inst:Destroy()
		else
			local parent = inst.Parent
			inst.Parent = nil
			task.delay(respawn, function()
				if parent and parent.Parent then
					inst:SetAttribute("EQAmount", originalAmount)
					inst.Parent = parent
				end
			end)
		end
	end)
end

local function makeWorldItem(id, amount, cframe)
	local def = Items[id]
	local r = rarity(id)
	local part = Instance.new("Part")
	part.Name = "EQ_" .. id
	part.Size = Vector3.new(1.2, 1.2, 1.2)
	part.Shape = Enum.PartType.Ball
	part.Material = Enum.Material.Neon
	part.Color = r.Color
	part.CFrame = cframe
	part.TopSurface = Enum.SurfaceType.Smooth
	part.BottomSurface = Enum.SurfaceType.Smooth

	local bb = Instance.new("BillboardGui")
	bb.Size = UDim2.fromOffset(160, 50)
	bb.StudsOffset = Vector3.new(0, 1.8, 0)
	bb.AlwaysOnTop = true
	bb.MaxDistance = 40
	bb.Parent = part
	local icon = Instance.new("TextLabel")
	icon.BackgroundTransparency = 1
	icon.Size = UDim2.new(1, 0, 0.6, 0)
	icon.Text = def.Emoji or "📦"
	icon.TextScaled = true
	icon.Parent = bb
	local name = Instance.new("TextLabel")
	name.BackgroundTransparency = 1
	name.Position = UDim2.fromScale(0, 0.6)
	name.Size = UDim2.new(1, 0, 0.4, 0)
	name.Font = Enum.Font.GothamBold
	name.TextScaled = true
	name.TextColor3 = r.Color
	name.TextStrokeTransparency = 0.3
	name.Text = def.Name .. (amount > 1 and (" x" .. amount) or "")
	name.Parent = bb

	part:SetAttribute("EQItem", id)
	part:SetAttribute("EQAmount", amount)
	part:GetAttributeChangedSignal("EQAmount"):Connect(function()
		local n = part:GetAttribute("EQAmount") or 1
		name.Text = def.Name .. (n > 1 and (" x" .. n) or "")
	end)
	return part
end

function EQ.SpawnItem(id, amount, position, respawnSeconds)
	if not Items[id] then
		return nil
	end
	local part = makeWorldItem(id, amount or 1, CFrame.new(position))
	part.Anchored = true
	if respawnSeconds then
		part:SetAttribute("EQRespawn", respawnSeconds)
	end
	part.Parent = workspace
	setupPickup(part)
	return part
end

local function dropItem(player, p, slot, amount)
	local s = p.slots[slot]
	if not s then
		return
	end
	local char = player.Character
	local root = char and char:FindFirstChild("HumanoidRootPart")
	if not root then
		return
	end
	amount = math.min(amount or s.n, s.n)
	local id = s.id
	s.n -= amount
	if s.n <= 0 then
		p.slots[slot] = false
	end
	fixEquipped(player, p)
	EQ.ItemRemoved:Fire(player, id, amount)

	local cf = root.CFrame * CFrame.new(0, 1, -3)
	local part = makeWorldItem(id, amount, cf)
	part:SetAttribute("EQDropped", true)
	part.AssemblyLinearVelocity = root.CFrame.LookVector * 18 + Vector3.new(0, 14, 0)
	part.Parent = workspace
	setupPickup(part)
	Debris:AddItem(part, Config.DropLifetime)
	EQ.Notify(player, "Wyrzucono: " .. itemLabel(id, amount), Config.Theme.SubText)
end

-- ════════════════════════ akcje od klienta ════════════════════════
local function sortBackpack(player, p)
	local list = {}
	for i = HOTBAR + 1, MAX do
		if p.slots[i] then
			table.insert(list, p.slots[i])
		end
		p.slots[i] = false
	end
	-- łączenie stosów
	local merged, openStack = {}, {}
	for _, s in list do
		local stack = maxStack(s.id)
		local open = openStack[s.id]
		if open and open.n < stack then
			local put = math.min(stack - open.n, s.n)
			open.n += put
			s.n -= put
		end
		if s.n > 0 then
			table.insert(merged, s)
			if s.n < stack then
				openStack[s.id] = s
			end
		end
	end
	table.sort(merged, function(a, b)
		local ra, rb = rarity(a.id).Order, rarity(b.id).Order
		if ra ~= rb then
			return ra > rb
		end
		local na, nb = Items[a.id].Name, Items[b.id].Name
		if na ~= nb then
			return na < nb
		end
		return a.n > b.n
	end)
	for i, s in merged do
		p.slots[HOTBAR + i] = s
	end
end

local Actions = {}

function Actions.Request(player, p)
	-- klient prosi o aktualny stan (np. po załadowaniu)
end

function Actions.Move(player, p, from, to)
	if not validSlot(from) or not validSlot(to) or from == to then
		return
	end
	local a, b = p.slots[from], p.slots[to]
	if not a then
		return
	end
	if b and b.id == a.id and maxStack(a.id) > 1 and b.n < maxStack(a.id) then
		local put = math.min(maxStack(a.id) - b.n, a.n)
		b.n += put
		a.n -= put
		if a.n <= 0 then
			p.slots[from] = false
		end
	else
		p.slots[from], p.slots[to] = b or false, a
	end
end

function Actions.Split(player, p, slot)
	if not validSlot(slot) then
		return
	end
	local s = p.slots[slot]
	if not s or s.n < 2 then
		return
	end
	for i = HOTBAR + 1, MAX do
		if not p.slots[i] then
			local half = math.floor(s.n / 2)
			s.n -= half
			p.slots[i] = { id = s.id, n = half }
			return
		end
	end
	for i = 1, HOTBAR do
		if not p.slots[i] then
			local half = math.floor(s.n / 2)
			s.n -= half
			p.slots[i] = { id = s.id, n = half }
			return
		end
	end
	EQ.Notify(player, "Brak wolnego slotu", Config.Theme.Danger)
end

function Actions.Equip(player, p, slot)
	if not validSlot(slot) then
		return
	end
	if not p.slots[slot] then
		unequip(player, p)
		return
	end
	local def = Items[p.slots[slot].id]
	if def and def.Type == "Tool" then
		equip(player, p, slot)
	elseif def and def.Type == "Consumable" then
		consume(player, p, slot)
	else
		unequip(player, p)
	end
end

function Actions.Use(player, p, slot)
	if validSlot(slot) then
		consume(player, p, slot)
	end
end

function Actions.Drop(player, p, slot, amount)
	if not validSlot(slot) then
		return
	end
	if amount ~= nil and not validAmount(amount) then
		return
	end
	if os.clock() - (p.lastDrop or 0) < 0.25 then
		return
	end
	p.lastDrop = os.clock()
	dropItem(player, p, slot, amount)
end

function Actions.DropEquipped(player, p)
	if p.equipped then
		Actions.Drop(player, p, p.equipped, 1)
	end
end

function Actions.Delete(player, p, slot)
	if not validSlot(slot) or not p.slots[slot] then
		return
	end
	local s = p.slots[slot]
	p.slots[slot] = false
	EQ.ItemRemoved:Fire(player, s.id, s.n)
	EQ.Notify(player, "Usunięto: " .. itemLabel(s.id, s.n), Config.Theme.Danger)
end

function Actions.Sort(player, p)
	sortBackpack(player, p)
end

-- ════════════════════════ zapis ════════════════════════
local function keyFor(player)
	return "p_" .. player.UserId
end

local function loadData(player)
	if not store then
		return nil, true
	end
	for attempt = 1, 4 do
		local ok, res = pcall(function()
			return store:GetAsync(keyFor(player))
		end)
		if ok then
			return res, true
		end
		local msg = tostring(res)
		if string.find(msg, "403", 1, true) or string.find(msg, "Studio", 1, true) then
			-- Studio bez dostępu do API: grasz bez zapisu, ale z przedmiotami startowymi
			warn("[EQ] Brak dostępu do DataStore w Studio – zapis wyłączony. Włącz: Game Settings > Security > Enable Studio Access to API Services")
			store = nil
			return nil, true
		end
		warn(("[EQ] Błąd wczytywania %s (próba %d): %s"):format(player.Name, attempt, msg))
		task.wait(attempt * 1.5)
	end
	return nil, false
end

local function saveData(player, p)
	if not store or not p.loaded or not p.canSave then
		return
	end
	local data = { v = 1, slots = serialize(p.slots), t = os.time() }
	for attempt = 1, 3 do
		local ok, err = pcall(function()
			store:UpdateAsync(keyFor(player), function()
				return data
			end)
		end)
		if ok then
			return true
		end
		warn(("[EQ] Błąd zapisu %s (próba %d): %s"):format(player.Name, attempt, tostring(err)))
		task.wait(attempt)
	end
	return false
end

local function applySave(p, saved)
	local overflow = {}
	if type(saved) == "table" and type(saved.slots) == "table" then
		for i, s in pairs(saved.slots) do
			local idx = tonumber(i)
			if type(s) == "table" and type(s.id) == "string" and Items[s.id] and type(s.n) == "number" and s.n >= 1 then
				local n = math.min(math.floor(s.n), maxStack(s.id))
				if idx and idx >= 1 and idx <= MAX and idx % 1 == 0 and not p.slots[idx] then
					p.slots[idx] = { id = s.id, n = n }
				else
					table.insert(overflow, { id = s.id, n = n })
				end
			end
		end
	end
	for _, s in overflow do
		addRaw(p, s.id, s.n)
	end
end

-- ════════════════════════ gracze ════════════════════════
local function isAdmin(player)
	if RunService:IsStudio() then
		return true
	end
	if game.CreatorType == Enum.CreatorType.User and player.UserId == game.CreatorId then
		return true
	end
	return table.find(Config.Admins, player.UserId) ~= nil
end

local function onChat(player, msg)
	if not isAdmin(player) then
		return
	end
	local args = string.split(msg, " ")
	local cmd = string.lower(args[1] or "")
	if cmd == "!daj" then
		local id = args[2]
		local n = tonumber(args[3]) or 1
		if not id or not Items[id] then
			EQ.Notify(player, "Nie ma przedmiotu: " .. tostring(id) .. " (wpisz !lista)", Config.Theme.Danger)
			return
		end
		local left = EQ.AddItem(player, id, n)
		EQ.Notify(player, "Dodano " .. (n - left) .. "x " .. Items[id].Name, Config.Theme.Good)
	elseif cmd == "!wyczysc" then
		local p = profiles[player]
		if p and p.loaded then
			unequip(player, p)
			p.slots = emptySlots()
			sync(player)
			EQ.Notify(player, "Wyczyszczono ekwipunek", Config.Theme.Good)
		end
	elseif cmd == "!lista" then
		local ids = {}
		for id in pairs(Items) do
			table.insert(ids, id)
		end
		table.sort(ids)
		EQ.Notify(player, table.concat(ids, ", "), Config.Theme.Text)
	end
end

local function onCharacter(player, p, char)
	p.tool = nil
	p.equipped = nil
	p.equippedRef = nil
	char.ChildRemoved:Connect(function(child)
		if child == p.tool then
			-- Tool zniknął z postaci (np. zabrany przez inny skrypt)
			p.tool = nil
			p.equipped = nil
			p.equippedRef = nil
			sync(player)
		end
	end)
	sync(player)
end

local function onPlayerAdded(player)
	local p = {
		slots = emptySlots(),
		loaded = false,
		canSave = false,
		window = os.clock(),
		actions = 0,
	}
	profiles[player] = p

	player.CharacterAdded:Connect(function(char)
		onCharacter(player, p, char)
	end)
	player.Chatted:Connect(function(msg)
		onChat(player, msg)
	end)
	if player.Character then
		onCharacter(player, p, player.Character)
	end

	local saved, ok = loadData(player)
	if not player.Parent then
		profiles[player] = nil
		return
	end
	if ok then
		p.canSave = true
		if saved == nil then
			for _, it in Config.StarterItems do
				if Items[it.Id] then
					addRaw(p, it.Id, it.Amount or 1)
				end
			end
		else
			applySave(p, saved)
		end
	else
		-- nie nadpisujemy zapisu, jeśli nie udało się go wczytać
		EQ.Notify(player, "⚠️ Nie udało się wczytać ekwipunku – postęp nie będzie zapisywany", Config.Theme.Danger)
	end
	p.loaded = true
	sync(player)
end

local function onPlayerRemoving(player)
	local p = profiles[player]
	if not p then
		return
	end
	unequip(player, p)
	saveData(player, p)
	profiles[player] = nil
end

-- ════════════════════════ start ════════════════════════
local function demoPickups()
	local spawn = workspace:FindFirstChildWhichIsA("SpawnLocation", true)
	local base = spawn and spawn.Position or Vector3.new(0, 0, 0)
	local demo = {
		{ "Drewno", 8 }, { "Kamien", 5 }, { "Zloto", 2 }, { "Diament", 1 },
		{ "Jablko", 3 }, { "MiksturaSzybkosci", 1 }, { "MiksturaSkoku", 1 },
		{ "Kilof", 1 }, { "OgnistyMiecz", 1 }, { "Gwiazda", 1 },
	}
	for i, d in demo do
		if Items[d[1]] then
			local angle = (i / #demo) * math.pi * 2
			local pos = base + Vector3.new(math.cos(angle) * 14, 3, math.sin(angle) * 14)
			EQ.SpawnItem(d[1], d[2], pos, 10)
		end
	end
end

function EQ.Start()
	if started then
		return
	end
	started = true

	-- RemoteEventy
	local remotes = ReplicatedStorage:FindFirstChild("EQRemotes") or Instance.new("Folder")
	remotes.Name = "EQRemotes"
	local function remote(name)
		local r = remotes:FindFirstChild(name) or Instance.new("RemoteEvent")
		r.Name = name
		r.Parent = remotes
		return r
	end
	UpdateEvent = remote("InventoryUpdate")
	ActionEvent = remote("InventoryAction")
	NotifyEvent = remote("Notify")
	remotes.Parent = ReplicatedStorage

	if Config.SaveEnabled then
		local ok, res = pcall(function()
			return DataStoreService:GetDataStore(Config.DataStoreName)
		end)
		if ok then
			store = res
		else
			warn("[EQ] DataStore niedostępny – ekwipunek nie będzie zapisywany. W Studio włącz: Game Settings > Security > Enable Studio Access to API Services. Błąd:", res)
		end
	end

	ActionEvent.OnServerEvent:Connect(function(player, action, a, b)
		local p = profiles[player]
		if not p or not p.loaded or type(action) ~= "string" then
			return
		end
		-- limit spamu: 25 akcji na sekundę
		if os.clock() - p.window > 1 then
			p.window = os.clock()
			p.actions = 0
		end
		p.actions += 1
		if p.actions > 25 then
			return
		end
		local fn = Actions[action]
		if fn then
			fn(player, p, a, b)
			fixEquipped(player, p)
			sync(player)
		end
	end)

	Players.PlayerAdded:Connect(onPlayerAdded)
	Players.PlayerRemoving:Connect(onPlayerRemoving)
	for _, pl in Players:GetPlayers() do
		task.spawn(onPlayerAdded, pl)
	end

	-- przedmioty ustawione na mapie (atrybut EQItem)
	for _, inst in workspace:GetDescendants() do
		setupPickup(inst)
	end
	workspace.DescendantAdded:Connect(function(inst)
		task.defer(setupPickup, inst)
	end)
	if Config.DemoPickups then
		demoPickups()
	end

	-- autozapis
	task.spawn(function()
		while true do
			task.wait(Config.AutosaveInterval)
			for pl, p in profiles do
				task.spawn(saveData, pl, p)
			end
		end
	end)

	-- zapis przy wyłączaniu serwera
	game:BindToClose(function()
		local pending = 0
		for pl, p in profiles do
			pending += 1
			task.spawn(function()
				saveData(pl, p)
				pending -= 1
			end)
		end
		local t = os.clock()
		while pending > 0 and os.clock() - t < 25 do
			task.wait()
		end
	end)

	print(("[EQ] System ekwipunku uruchomiony (%d slotów)"):format(MAX))
end

return EQ
