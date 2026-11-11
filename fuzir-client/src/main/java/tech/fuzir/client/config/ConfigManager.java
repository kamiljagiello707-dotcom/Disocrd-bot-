package tech.fuzir.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.fuzir.client.FuzirClient;
import tech.fuzir.client.gui.ClickGuiScreen;
import tech.fuzir.client.module.Module;
import tech.fuzir.client.module.ModuleManager;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Persists module states, settings, binds, HUD positions and the ClickGUI panel layout to
 * {@code config/fuzir/config.json}. Writes go to a temp file first and are moved into place atomically.
 */
public final class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Fuzir/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file = FabricLoader.getInstance().getConfigDir().resolve("fuzir").resolve("config.json");
    private final ModuleManager modules;
    private JsonObject guiLayout;

    public ConfigManager(ModuleManager modules) {
        this.modules = modules;
    }

    public void load() {
        if (!Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) return;
            JsonObject object = root.getAsJsonObject();
            if (object.has("modules") && object.get("modules").isJsonObject()) {
                JsonObject saved = object.getAsJsonObject("modules");
                for (Module module : modules.getModules()) {
                    JsonElement element = saved.get(module.getName());
                    if (element == null || !element.isJsonObject()) continue;
                    try {
                        module.load(element.getAsJsonObject());
                    } catch (RuntimeException e) {
                        LOGGER.warn("Skipping invalid config entry for {}", module.getName(), e);
                    }
                }
            }
            if (object.has("gui") && object.get("gui").isJsonObject()) guiLayout = object.getAsJsonObject("gui");
        } catch (IOException | JsonParseException | IllegalStateException e) {
            LOGGER.error("Failed to read {}, using defaults", file, e);
        }
    }

    public void save() {
        JsonObject root = new JsonObject();
        JsonObject saved = new JsonObject();
        for (Module module : modules.getModules()) saved.add(module.getName(), module.save());
        root.add("modules", saved);
        ClickGuiScreen gui = FuzirClient.get().getClickGuiIfCreated();
        if (gui != null) guiLayout = gui.saveLayout();
        if (guiLayout != null) root.add("gui", guiLayout);

        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling("config.json.tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to write {}", file, e);
        }
    }

    /** Panel layout loaded from disk, consumed when the ClickGUI is first built. */
    public JsonObject getGuiLayout() {
        return guiLayout;
    }
}
