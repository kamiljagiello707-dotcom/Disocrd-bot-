-- EQService (ModuleScript)
-- Miejsce: ServerScriptService > EQSystem > EQService
-- Logika ekwipunku RP po stronie serwera. Serwer decyduje o wszystkim,
-- klient tylko wysyła prośby (użyj, wyrzuć, podaj), więc exploiter nie doda sobie przedmiotów.
--
-- Użycie z innych skryptów serwera (sklepy, praca, questy):
--   local EQ = require(game.ServerScriptService.EQSystem.EQService)
--   EQ.AddItem(player, "Chleb", 3)        --> ile się NIE zmieściło (0 = wszystko weszło)
--   EQ.RemoveItem(player, "Klucze", 1)    --> true / false (false = za mało, nic nie zabrano)
--   EQ.CountItem(player, "Woda")          --> liczba sztuk
--   EQ.HasItem(player, "Telefon")         --> true / false
--   EQ.CanCarry(player, "Apteczka", 2)    --> czy się zmieści (waga + miejsce)
--   EQ.GetWeight(player)                  --> aktualna waga (kg)
--   EQ.Notify(player, "Tekst")            --> powiadomienie na ekranie
--   EQ.SpawnItem("Pizza", 1, Vector3.new(0, 5, 0), 30)  --> przedmiot na ziemi (respawn 30 s)
--   EQ.ItemAdded / EQ.ItemRemoved / EQ.ItemUsed   --> BindableEvent (player, id, amount)

local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local ServerStorage = game:GetService("ServerStorage")
local DataStoreService = game:GetService("DataStoreService")
local RunService = game:GetService("RunService")
local Debris = game:GetService("Debris")

local Shared = ReplicatedStorage:WaitForChild("EQShared")
local Config = require(Shared:WaitForChild("EQConfig"))
local Items = require(Shared:WaitForChild("ItemData"))

local MAX = Config.MaxStacks
local EPS = 1e-6

local EQ = {}
EQ.ItemAdded = Instance.new("BindableEvent")
EQ.ItemRemoved = Instance.new("BindableEvent")
EQ.ItemUsed = Instance.new("BindableEvent")

local profiles = {} -- [Player] = profile
local started = false
local store = nil
local UpdateEvent, ActionEvent, NotifyEvent

-- ════════════════════════ pomocnicze ════════════════════════
local function maxStack(id)
	local def = Items[id]
	return (def and def.MaxStack and def.MaxStack >= 1) and math.floor(def.MaxStack) or 1
end

local function weightOf(id)
	local def = Items[id]
	return def and def.Weight or 0
end

local function rarity(id)
	local def = Items[id]
	return Config.Rarities[def and def.Rarity or "Common"] or Config.Rarities.Common
end

local function emptySlots()
	return table.create(MAX, false)
end

local function validSlot(x)
	return type(x) == "number" and x == x and x % 1 == 0 and x >= 1 and x <= MAX
end

local function validAmount(x)
	return type(x) == "number" and x == x and x % 1 == 0 and x >= 1 and x <= 100000
end

local function totalWeight(p)
	local w = 0
	for i = 1, MAX do
		local s = p.slots[i]
		if s then
			w += weightOf(s.id) * s.n
		end
	end
	return w
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
		weight = totalWeight(p),
		maxWeight = Config.MaxWeight,
	})
end

function EQ.Notify(player, text, color)
	if NotifyEvent and player and player.Parent then
		NotifyEvent:FireClient(player, tostring(text), color or Config.Theme.Text)
	end
end

local function itemLabel(id, amount)
	local def = Items[id]
	return def.Name .. (amount > 1 and (" x" .. amount) or "")
end

-- ════════════════════════ dodawanie / zabieranie ════════════════════════
-- ile sztuk zmieści się wagowo
local function weightAllows(p, id, amount)
	local w = weightOf(id)
	if w <= 0 then
		return amount
	end
	local free = Config.MaxWeight - totalWeight(p)
	return math.max(0, math.min(amount, math.floor(free / w + EPS)))
end

-- Dodaje bez synchronizacji. Zwraca resztę, która się nie zmieściła.
local function addRaw(p, id, amount, ignoreWeight)
	local allowed = ignoreWeight and amount or weightAllows(p, id, amount)
	local left = allowed
	local stack = maxStack(id)
	if left > 0 and stack > 1 then
		for i = 1, MAX do
			local s = p.slots[i]
			if s and s.id == id and s.n < stack then
				local put = math.min(stack - s.n, left)
				s.n += put
				left -= put
				if left <= 0 then
					break
				end
			end
		end
	end
	if left > 0 then
		for i = 1, MAX do
			if not p.slots[i] then
				local put = math.min(stack, left)
				p.slots[i] = { id = id, n = put }
				left -= put
				if left <= 0 then
					break
				end
			end
		end
	end
	return (amount - allowed) + left
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

local function slotsAllow(p, id)
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

-- dlaczego się nie zmieściło
local function fullReason(p, id)
	if weightAllows(p, id, 1) < 1 then
		return "Za ciężko! Nie uniesiesz więcej ⚖️"
	end
	return "Brak miejsca w ekwipunku"
end

local unequip
local function fixEquipped(player, p)
	if not p.equippedRef then
		p.equipped = nil
		return
	end
	for i = 1, MAX do
		if p.slots[i] == p.equippedRef then
			p.equipped = i
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

function EQ.CanCarry(player, id, amount)
	amount = amount or 1
	local p = profiles[player]
	return p ~= nil and p.loaded and Items[id] ~= nil and weightAllows(p, id, amount) >= amount and slotsAllow(p, id) >= amount
end

function EQ.GetWeight(player)
	local p = profiles[player]
	return p and totalWeight(p) or 0
end

function EQ.GetInventory(player)
	local p = profiles[player]
	return p and p.loaded and serialize(p.slots) or nil
end

-- ════════════════════════ narzędzia (Tool) ════════════════════════
local BAT_GRIP = CFrame.new(0, 0, -1.4) * CFrame.fromMatrix(Vector3.zero, Vector3.new(0, 1, 0), Vector3.new(0, 0, 1))
local SWING_GRIP = BAT_GRIP * CFrame.Angles(math.rad(-80), 0, 0)

local function attachMelee(tool, def)
	local last = 0
	tool.Activated:Connect(function()
		local char = tool.Parent
		local hum = char and char:FindFirstChildOfClass("Humanoid")
		local root = char and char:FindFirstChild("HumanoidRootPart")
		if not hum or not root or hum.Health <= 0 then
			return
		end
		local cd = def.Cooldown or 0.8
		if os.clock() - last < cd then
			return
		end
		last = os.clock()
		tool.Grip = SWING_GRIP
		task.delay(math.min(0.2, cd * 0.5), function()
			if tool.Parent then
				tool.Grip = BAT_GRIP
			end
		end)
		local range = def.Range or 6
		local params = OverlapParams.new()
		params.FilterType = Enum.RaycastFilterType.Exclude
		params.FilterDescendantsInstances = { char }
		local parts = workspace:GetPartBoundsInBox(root.CFrame * CFrame.new(0, 0, -range / 2), Vector3.new(5, 6, range), params)
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
	local folder = ServerStorage:FindFirstChild("EQTools")
	local template = folder and folder:FindFirstChild(def.ToolName or id)
	if template and template:IsA("Tool") then
		local t = template:Clone()
		t.CanBeDropped = false
		return t
	end

	-- automatyczny prosty model, gdy nie ma własnego Toola w ServerStorage > EQTools
	local tool = Instance.new("Tool")
	tool.Name = def.Name
	tool.ToolTip = def.Name
	tool.CanBeDropped = false
	tool.RequiresHandle = true

	local handle = Instance.new("Part")
	handle.Name = "Handle"
	handle.CanCollide = false
	handle.Massless = true
	handle.Color = def.Color or Color3.fromRGB(40, 40, 44)
	handle.Material = Enum.Material.SmoothPlastic
	handle.Parent = tool

	if def.Damage then
		handle.Size = Vector3.new(0.35, 0.35, 3.5)
		handle.Material = Enum.Material.Wood
		tool.Grip = BAT_GRIP
		attachMelee(tool, def)
	elseif def.Light then
		handle.Size = Vector3.new(0.3, 0.3, 1.2)
		tool.Grip = CFrame.new(0, 0, 0.3)
		local att = Instance.new("Attachment")
		att.Position = Vector3.new(0, 0, -0.6)
		att.Parent = handle
		local spot = Instance.new("SpotLight")
		spot.Face = Enum.NormalId.Front
		spot.Range = 45
		spot.Angle = 50
		spot.Brightness = 4
		spot.Enabled = false
		spot.Parent = handle
		local bulb = Instance.new("PointLight")
		bulb.Range = 6
		bulb.Brightness = 1
		bulb.Enabled = false
		bulb.Parent = att
		tool.Activated:Connect(function()
			spot.Enabled = not spot.Enabled
			bulb.Enabled = spot.Enabled
		end)
	else
		handle.Size = Vector3.new(0.4, 0.8, 0.1)
		tool.Grip = CFrame.new(0, -0.2, 0)
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
	p.tool = tool
	p.equipped = slot
	p.equippedRef = s
	tool.Parent = char
end

-- ════════════════════════ używanie ════════════════════════
local function applyBoost(hum, prop, add, duration, key)
	local baseAttr = "EQBase" .. prop
	if hum:GetAttribute(baseAttr) == nil then
		hum:SetAttribute(baseAttr, hum[prop])
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

local function useItem(player, p, slot)
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
		EQ.Notify(player, "Tego przedmiotu nie można użyć", Config.Theme.SubText)
		return
	end
	if os.clock() - (p.lastUse or 0) < 0.5 then
		return
	end
	local char = player.Character
	local hum = char and char:FindFirstChildOfClass("Humanoid")
	if not hum or hum.Health <= 0 then
		return
	end
	if def.Heal and not def.SpeedBoost and not def.JumpBoost and hum.Health >= hum.MaxHealth then
		EQ.Notify(player, "Masz pełne zdrowie", Config.Theme.SubText)
		return
	end
	p.lastUse = os.clock()

	local duration = def.Duration or 10
	if def.Heal then
		hum.Health = math.min(hum.MaxHealth, hum.Health + def.Heal)
	end
	if def.SpeedBoost then
		applyBoost(hum, "WalkSpeed", def.SpeedBoost, duration, "EQSpeedToken")
	end
	if def.JumpBoost then
		if hum.UseJumpPower then
			applyBoost(hum, "JumpPower", def.JumpBoost, duration, "EQJumpToken")
		else
			applyBoost(hum, "JumpHeight", def.JumpBoost / 3, duration, "EQJumpToken")
		end
	end

	s.n -= 1
	if s.n <= 0 then
		p.slots[slot] = false
	end
	EQ.ItemUsed:Fire(player, s.id, 1)
	EQ.ItemRemoved:Fire(player, s.id, 1)
	EQ.Notify(player, "Użyto: " .. def.Name, Config.Theme.Text)
end

-- ════════════════════════ przedmioty na ziemi ════════════════════════
local setupDone = setmetatable({}, { __mode = "k" })

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
			EQ.Notify(player, fullReason(p, id), Config.Theme.Danger)
			return
		end
		EQ.ItemAdded:Fire(player, id, taken)
		sync(player)
		EQ.Notify(player, "+" .. taken .. "  " .. def.Name, rarity(id).Color)
		if left > 0 then
			inst:SetAttribute("EQAmount", left)
			EQ.Notify(player, fullReason(p, id), Config.Theme.SubText)
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
	local part = Instance.new("Part")
	part.Name = "EQ_" .. id
	part.Size = Vector3.new(1, 0.5, 1)
	part.Material = Enum.Material.SmoothPlastic
	part.Color = Color3.fromRGB(235, 235, 240)
	part.CFrame = cframe

	local bb = Instance.new("BillboardGui")
	bb.Size = UDim2.fromOffset(150, 56)
	bb.StudsOffset = Vector3.new(0, 1.6, 0)
	bb.AlwaysOnTop = true
	bb.MaxDistance = 30
	bb.Parent = part
	local box = Instance.new("Frame")
	box.Size = UDim2.fromScale(1, 1)
	box.BackgroundColor3 = Config.Theme.Panel
	box.BackgroundTransparency = 0.25
	box.Parent = bb
	local c = Instance.new("UICorner")
	c.CornerRadius = UDim.new(0, 10)
	c.Parent = box
	local icon
	if def.Icon and def.Icon ~= "" then
		icon = Instance.new("ImageLabel")
		icon.Image = def.Icon
		icon.ScaleType = Enum.ScaleType.Fit
	else
		icon = Instance.new("TextLabel")
		icon.Text = def.Emoji or "📦"
		icon.TextScaled = true
	end
	icon.BackgroundTransparency = 1
	icon.Position = UDim2.fromOffset(8, 8)
	icon.Size = UDim2.fromOffset(40, 40)
	icon.Parent = box
	local name = Instance.new("TextLabel")
	name.BackgroundTransparency = 1
	name.Position = UDim2.fromOffset(54, 0)
	name.Size = UDim2.new(1, -60, 1, 0)
	name.Font = Enum.Font.GothamBold
	name.TextSize = 14
	name.TextWrapped = true
	name.TextXAlignment = Enum.TextXAlignment.Left
	name.TextColor3 = Config.Theme.Text
	name.Text = itemLabel(id, amount)
	name.Parent = box

	part:SetAttribute("EQItem", id)
	part:SetAttribute("EQAmount", amount)
	part:GetAttributeChangedSignal("EQAmount"):Connect(function()
		name.Text = itemLabel(id, part:GetAttribute("EQAmount") or 1)
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

	local part = makeWorldItem(id, amount, root.CFrame * CFrame.new(0, 0, -3))
	part:SetAttribute("EQDropped", true)
	part.AssemblyLinearVelocity = root.CFrame.LookVector * 8 + Vector3.new(0, 8, 0)
	part.Parent = workspace
	setupPickup(part)
	Debris:AddItem(part, Config.DropLifetime)
	EQ.Notify(player, "Wyrzucono: " .. itemLabel(id, amount), Config.Theme.SubText)
end

-- ════════════════════════ akcje od klienta ════════════════════════
local Actions = {}

function Actions.Request(player, p)
	-- klient prosi o aktualny stan
end

function Actions.Use(player, p, slot)
	if validSlot(slot) then
		useItem(player, p, slot)
	end
end

function Actions.Drop(player, p, slot, amount)
	if not validSlot(slot) or (amount ~= nil and not validAmount(amount)) then
		return
	end
	if os.clock() - (p.lastDrop or 0) < 0.3 then
		return
	end
	p.lastDrop = os.clock()
	dropItem(player, p, slot, amount)
end

function Actions.Give(player, p, slot, targetId)
	if not validSlot(slot) or type(targetId) ~= "number" then
		return
	end
	local s = p.slots[slot]
	local target = Players:GetPlayerByUserId(targetId)
	local tp = target and profiles[target]
	if not s or not target or target == player or not tp or not tp.loaded then
		return
	end
	local r1 = player.Character and player.Character:FindFirstChild("HumanoidRootPart")
	local r2 = target.Character and target.Character:FindFirstChild("HumanoidRootPart")
	if not r1 or not r2 or (r1.Position - r2.Position).Magnitude > Config.GiveDistance + 2 then
		EQ.Notify(player, "Gracz jest za daleko", Config.Theme.Danger)
		return
	end
	if os.clock() - (p.lastGive or 0) < 0.4 then
		return
	end
	p.lastGive = os.clock()
	local id = s.id
	if addRaw(tp, id, 1) > 0 then
		EQ.Notify(player, target.DisplayName .. " nie uniesie więcej", Config.Theme.Danger)
		return
	end
	s.n -= 1
	if s.n <= 0 then
		p.slots[slot] = false
	end
	fixEquipped(player, p)
	EQ.ItemRemoved:Fire(player, id, 1)
	EQ.ItemAdded:Fire(target, id, 1)
	sync(target)
	EQ.Notify(player, "Podano " .. Items[id].Name .. " → " .. target.DisplayName, Config.Theme.Text)
	EQ.Notify(target, player.DisplayName .. " dał Ci: " .. Items[id].Name, rarity(id).Color)
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
		addRaw(p, s.id, s.n, true)
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
		if left > 0 then
			EQ.Notify(player, fullReason(profiles[player], id), Config.Theme.Danger)
		end
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
			p.tool = nil
			p.equipped = nil
			p.equippedRef = nil
			sync(player)
		end
	end)
	sync(player)
end

local function onPlayerAdded(player)
	local p = { slots = emptySlots(), loaded = false, canSave = false, window = os.clock(), actions = 0 }
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
					addRaw(p, it.Id, it.Amount or 1, true)
				end
			end
		else
			applySave(p, saved)
		end
	else
		EQ.Notify(player, "Nie udało się wczytać ekwipunku – postęp nie będzie zapisywany", Config.Theme.Danger)
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
		{ "Apteczka", 1 }, { "Bandaz", 3 }, { "Pizza", 2 }, { "Latarka", 1 }, { "Klucze", 1 },
		{ "SztabkaZlota", 2 }, { "Kawa", 1 }, { "Kij", 1 }, { "Zegarek", 1 }, { "Tabletki", 2 },
	}
	for i, d in demo do
		if Items[d[1]] then
			local angle = (i / #demo) * math.pi * 2
			EQ.SpawnItem(d[1], d[2], base + Vector3.new(math.cos(angle) * 14, 2, math.sin(angle) * 14), 15)
		end
	end
end

function EQ.Start()
	if started then
		return
	end
	started = true

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
			warn("[EQ] DataStore niedostępny – ekwipunek nie będzie zapisywany:", res)
		end
	end

	ActionEvent.OnServerEvent:Connect(function(player, action, a, b)
		local p = profiles[player]
		if not p or not p.loaded or type(action) ~= "string" then
			return
		end
		if os.clock() - p.window > 1 then
			p.window = os.clock()
			p.actions = 0
		end
		p.actions += 1
		if p.actions > 20 then
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

	for _, inst in workspace:GetDescendants() do
		setupPickup(inst)
	end
	workspace.DescendantAdded:Connect(function(inst)
		task.defer(setupPickup, inst)
	end)
	if Config.DemoPickups then
		demoPickups()
	end

	task.spawn(function()
		while true do
			task.wait(Config.AutosaveInterval)
			for pl, p in profiles do
				task.spawn(saveData, pl, p)
			end
		end
	end)

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

	print(("[EQ] Ekwipunek RP uruchomiony (max %d kg)"):format(Config.MaxWeight))
end

return EQ
