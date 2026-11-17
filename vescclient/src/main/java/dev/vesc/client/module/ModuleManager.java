package dev.vesc.client.module;

import dev.vesc.client.module.combat.*;
import dev.vesc.client.module.movement.*;
import dev.vesc.client.module.player.*;
import dev.vesc.client.module.visuals.*;
import dev.vesc.client.module.camera.*;
import dev.vesc.client.module.world.*;
import dev.vesc.client.module.cosmetics.*;
import dev.vesc.client.module.utility.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Rejestr modułów. Każdy moduł ma własną klasę w pakiecie swojej kategorii:
 * {@code module.combat}, {@code module.movement}, {@code module.player}, {@code module.visuals},
 * {@code module.camera}, {@code module.world}, {@code module.cosmetics}, {@code module.utility}.
 * Działają moduły kosmetyczne i legit (logika w ich klasach lub w {@link Legit}); moduły typu cheat
 * (np. KillAura, Reach, Speed, Scaffold) są atrapami – tylko ustawienia w GUI, bez logiki w grze.
 */
public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    private void add(Module m) { modules.add(m); }

    public ModuleManager() {
        // ======================================================== COMBAT
        add(new KillAura());
        add(new TriggerBot());
        add(new AimAssist());
        add(new AutoClicker());
        add(new Criticals());
        add(new Reach());
        add(new WTap());
        add(new FakeLag());
        add(new HitBoxes());
        add(new Velocity());
        add(new AutoTotem());

        // ======================================================== MOVEMENT
        add(new Sprint());
        add(new Speed());
        add(new Step());
        add(new NoFall());
        add(new Flight());
        add(new LongJump());
        add(new AirJump());
        add(new ElytraBoost());
        add(new Spider());
        add(new Jesus());

        // ======================================================== PLAYER
        add(new AutoArmor());
        add(new InvManager());
        add(new ChestStealer());
        add(new AutoTool());
        add(new FastPlace());
        add(new FastUse());
        add(new NoSlow());
        add(new AutoEat());
        add(new AutoRespawn());

        // ======================================================== VISUALS
        add(new TargetEsp());
        add(new Nametags());
        add(new ESP());
        add(new Tracers());
        add(new BlockOutline());
        add(new ChestESP());
        add(new Fullbright());
        add(new NoFire());

        // ======================================================== CAMERA
        add(new Zoom());
        add(new FreeLook());
        add(new Crosshair());
        add(new ViewModel());
        add(new SwordAnimations());
        add(new NoHurtCam());

        // ======================================================== WORLD
        add(new Sky());
        add(new Ambient());
        add(new TimeChanger());
        add(new Weather());
        add(new Scaffold());
        add(new Nuker());
        add(new FastBreak());
        add(new LiquidPlace());
        add(new AutoSign());
        add(new AutoFarm());
        add(new Timer());

        // ======================================================== COSMETICS
        add(new Cosmetic());
        add(new ChinaHat());
        add(new Trail());
        add(new JumpCircles());
        add(new ToolColor());
        add(new HitSound());

        // ======================================================== UTILITY
        add(new AutoGG());
        add(new ChatSuffix());
        add(new MiddleClick());
        add(new DiscordPresence());
        add(new AutoReconnect());
        add(new AntiAFK());
        add(new AntiBot());
        add(new NameProtect());
        add(new FakePlayer());

        // kolejność w GUI: kategoria -> podgrupa -> kolejność rejestracji
        modules.sort(Comparator.comparingInt((Module m) -> m.getCategory().ordinal())
                .thenComparingInt(m -> m.getCategory().groups.indexOf(m.getGroup())));
    }

    /** moduły kategorii w danej podgrupie (null = wszystkie) */
    public List<Module> byGroup(Category c, String group) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.getCategory() == c && (group == null || group.equals(m.getGroup()))) out.add(m);
        return out;
    }

    /** ostatnio przełączane moduły: {Module, czas} */
    public final java.util.ArrayDeque<Object[]> recent = new java.util.ArrayDeque<>();

    public void noteToggle(Module m) {
        recent.removeIf(o -> o[0] == m);
        recent.addFirst(new Object[]{m, System.currentTimeMillis()});
        while (recent.size() > 8) recent.removeLast();
    }

    public List<Module> all() { return modules; }

    public List<Module> byCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.getCategory() == c) out.add(m);
        return out;
    }

    public Module get(String name) {
        for (Module m : modules) if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }

    public long enabledCount() { return modules.stream().filter(Module::isEnabled).count(); }
}
