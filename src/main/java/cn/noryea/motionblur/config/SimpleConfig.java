package cn.noryea.motionblur.config;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class SimpleConfig {

    private static final Logger LOGGER = LogManager.getLogger("SimpleConfig");
    private final Map<String, String> config = new TreeMap<>();
    private final ConfigRequest request;
    private boolean broken = false;

    public interface DefaultConfig {
        String get(String namespace);

        static String empty(String namespace) {
            return "";
        }
    }

    public static class ConfigRequest {
        private final File file;
        private final String filename;
        private DefaultConfig provider;

        private ConfigRequest(File file, String filename) {
            this.file = file;
            this.filename = filename;
            this.provider = DefaultConfig::empty;
        }

        public ConfigRequest provider(DefaultConfig provider) {
            this.provider = provider;
            return this;
        }

        public SimpleConfig request() {
            return new SimpleConfig(this);
        }

        private String getConfig() {
            return provider.get(filename);
        }
    }

    public static ConfigRequest of(String filename) {
        Path path = FabricLoader.getInstance().getConfigDir();
        return new ConfigRequest(path.resolve(filename + ".properties").toFile(), filename);
    }

    private void createConfig() throws IOException {
        request.file.getParentFile().mkdirs();
        Files.createFile(request.file.toPath());

        PrintWriter writer = new PrintWriter(request.file, "UTF-8");
        writer.write(request.getConfig());
        writer.close();
    }

    private void loadConfig() throws IOException {
        Scanner reader = new Scanner(request.file);
        for (int line = 1; reader.hasNextLine(); line++) {
            parseConfigEntry(reader.nextLine(), line);
        }
    }

    private void parseConfigEntry(String entry, int line) {
        if (!entry.isEmpty() && !entry.startsWith("#")) {
            String[] parts = entry.split("=", 2);
            if (parts.length == 2) {
                config.put(parts[0], parts[1]);
            } else {
                throw new RuntimeException("Syntax error in config file on line " + line + "!");
            }
        }
    }

    private void updateWithProvider(MotionBlurConfigProvider configProvider) {
        request.provider = configProvider;
        delete();
        try {
            createConfig();
        } catch (IOException e) {
            LOGGER.error("Config '" + request.file + "' failed to generate!");
            LOGGER.trace(e);
            broken = true;
        }
    }

    private SimpleConfig(ConfigRequest request) {
        this.request = request;
        String identifier = "Config '" + request.filename + "'";

        if (!request.file.exists()) {
            LOGGER.info(identifier + " is missing, generating default one...");

            try {
                createConfig();
            } catch (IOException e) {
                LOGGER.error(identifier + " failed to generate!");
                LOGGER.trace(e);
                broken = true;
            }
        }

        if (!broken) {
            try {
                loadConfig();
            } catch (Exception e) {
                LOGGER.error(identifier + " failed to load!");
                LOGGER.trace(e);
                broken = true;
            }
        }
    }

    public void set(@NotNull String key, int value) {
        boolean success = false;
        MotionBlurConfigProvider configProvider = new MotionBlurConfigProvider();

        for (Map.Entry<String, String> entry : config.entrySet()) {
            if (!entry.getKey().equals(key)) {
                configProvider.addKeyValuePair(new Pair<>(entry.getKey(), entry.getValue()));
                continue;
            }

            config.put(key, "" + value);
            configProvider.addKeyValuePair(new Pair<>(key, value));
            success = true;
        }

        if (success) {
            updateWithProvider(configProvider);
        }
    }

    public void set(@NotNull String key, boolean value) {
        boolean success = false;
        MotionBlurConfigProvider configProvider = new MotionBlurConfigProvider();

        for (Map.Entry<String, String> entry : config.entrySet()) {
            if (!entry.getKey().equals(key)) {
                configProvider.addKeyValuePair(new Pair<>(entry.getKey(), entry.getValue()));
                continue;
            }

            config.put(key, String.valueOf(value));
            configProvider.addKeyValuePair(new Pair<>(key, value));
            success = true;
        }

        if (success) {
            updateWithProvider(configProvider);
        }
    }

    private String get(String key) {
        return config.get(key);
    }

    public String getOrDefault(String key, String def) {
        String val = get(key);
        return val == null ? def : val;
    }

    public int getOrDefault(String key, int def) {
        try {
            return Integer.parseInt(get(key));
        } catch (Exception e) {
            return def;
        }
    }

    public boolean getOrDefault(String key, boolean def) {
        String val = get(key);
        if (val == null) {
            return def;
        }

        try {
            return Boolean.parseBoolean(val);
        } catch (Exception e) {
            return def;
        }
    }

    public boolean isBroken() {
        return broken;
    }

    public boolean delete() {
        return request.file.delete();
    }
}
