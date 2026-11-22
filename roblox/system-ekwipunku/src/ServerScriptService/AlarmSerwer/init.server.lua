--[[
	SYSTEM ALARMOWANIA OSP  |  Start serwera.
	Logika jest w module "Alarmy" (w środku tego skryptu).
	Ustawienia: ReplicatedStorage > AlarmOSP > Ustawienia
]]

require(script:WaitForChild("Alarmy")).Start()
