package me.jellysquid.mods.sodium.client.gui;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.jellysquid.mods.sodium.client.gui.options.TextProvider;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class SodiumGameOptions {
    public final QualitySettings quality = new QualitySettings();
    public final AdvancedSettings advanced = new AdvancedSettings();
    public final NotificationSettings notifications = new NotificationSettings();

    private Path configPath;

    public static class AdvancedSettings {
        public ArenaMemoryAllocator arenaMemoryAllocator = null;

        public boolean alwaysDeferChunkUpdates = true;
        public boolean animateOnlyVisibleTextures = true;
        public boolean useEntityCulling = true;
        public boolean useParticleCulling = true;
        public boolean useFogOcclusion = true;
        public boolean useBlockFaceCulling = true;
        public boolean allowDirectMemoryAccess = true;
        public boolean enableMemoryTracing = false;
        public boolean useAdvancedStagingBuffers = true;

        public int maxPreRenderedFrames = 3;
    }

    public static class QualitySettings {
        public GraphicsQuality weatherQuality = GraphicsQuality.DEFAULT;
        public boolean enableVignette = true;
    }

    public static class NotificationSettings {
        public boolean hideDonationButton = false;
    }

    public enum ArenaMemoryAllocator implements TextProvider {
        ASYNC("sodium.options.chunk_memory_allocator.async"),
        SWAP("sodium.options.chunk_memory_allocator.swap");

        private final Text name;

        ArenaMemoryAllocator(String name) {
            this.name = new TranslatableText(name);
        }

        @Override
        public Text getLocalizedName() {
            return this.name;
        }
    }

    public enum GraphicsQuality implements TextProvider {
        DEFAULT("generator.default"),
        FANCY("options.clouds.fancy"),
        FAST("options.clouds.fast");

        private final Text name;

        GraphicsQuality(String name) {
            this.name = new TranslatableText(name);
        }

        @Override
        public Text getLocalizedName() {
            return this.name;
        }

        public boolean isFancy(GraphicsMode graphicsMode) {
            return (this == FANCY) || (this == DEFAULT && (graphicsMode == GraphicsMode.FANCY || graphicsMode == GraphicsMode.FABULOUS));
        }
    }

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .create();

    public static SodiumGameOptions load(Path path) {
        SodiumGameOptions config;

        if (Files.exists(path)) {
            try (FileReader reader = new FileReader(path.toFile())) {
                config = GSON.fromJson(reader, SodiumGameOptions.class);
            } catch (IOException e) {
                throw new RuntimeException("Could not parse config", e);
            }
        } else {
            config = new SodiumGameOptions();
        }

        config.configPath = path;

        if (config.advanced.arenaMemoryAllocator == null) {
            config.advanced.arenaMemoryAllocator = ArenaMemoryAllocator.ASYNC;
        }

        try {
            config.writeChanges();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't update config file", e);
        }

        return config;
    }

    public void writeChanges() throws IOException {
        Path dir = this.configPath.getParent();

        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        } else if (!Files.isDirectory(dir)) {
            throw new IOException("Not a directory: " + dir);
        }

        // Use a temporary location next to the config's final destination
        Path tempPath = this.configPath.resolveSibling(this.configPath.getFileName() + ".tmp");

        // Write the file to our temporary location
        Files.writeString(tempPath, GSON.toJson(this));

        // Atomically replace the old config file (if it exists) with the temporary file
        Files.move(tempPath, this.configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
