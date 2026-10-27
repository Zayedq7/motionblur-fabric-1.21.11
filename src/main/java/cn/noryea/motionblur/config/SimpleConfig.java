package cn.noryea.motionblur.config;


/*
 * Copyright (c) 2021 magistermaks
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import com.mojang.datafixers.util.Pair;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
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

        /**
         * Sets the default config provider, used to generate the
         * config if it's missing.
         *
         * @param provider default config provider
         * @return current config request object
         * @see DefaultConfig
         */
        public ConfigRequest provider(DefaultConfig provider) {
            this.provider = provider;
            return this;
        }

        /**
         * Loads the config from the filesystem.
         *
         * @return config object
         * @see SimpleConfig
         */
        public SimpleConfig request() {
            return new SimpleConfig(this);
        }

        private String getConfig() {
            return provider.get(filename);
        }

    }

    /**
     * Creates new config request object, ideally `namespace`
     * should be the name of the mod id of the requesting mod
     *
     * @param filename - name of the config file
     * @return new config request object
     */
    public static ConfigRequest of(String filename) {
        Path path = FabricLoader.getInstance().getConfigDir();
        return new ConfigRequest(path.resolve(filename + ".properties").toFile(), filename);
    }

    private void createConfig() throws IOException {

        // try creating missing files
        request.file.getParentFile().mkdirs();
        Files.createFile(request.file.toPath());

        // write default config data
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
            LOGGER.info( identifier + " is missing, generating default one..." );

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

    /**
     * Queries a value from config, returns `null` if the
     * key does not exist.
     *
     * @return value corresponding to the given key
     * @see SimpleConfig#getOrDefault
     */
    private String get(String key) {
        return config.get(key);
    }

    /**
     * Returns string value from config corresponding to the given
     * key, or the default string if the key is missing.
     *
     * @return value corresponding to the given key, or the default value
     */
    public String getOrDefault(String key, String def) {
        String val = get(key);
        return val == null ? def : val;
    }

    /**
     * Returns integer value from config corresponding to the given
     * key, or the default integer if the key is missing or invalid.
     *
     * @return value corresponding to the given key, or the default value
     */
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

    /**
     * If any error occurred during loading or reading from the config
     * a 'broken' flag is set, indicating that the config's state
     * is undefined and should be discarded using `delete()`
     *
     * @return the 'broken' flag of the configuration
     */
    public boolean isBroken() {
        return broken;
    }

    /**
     * deletes the config file from the filesystem
     *
     * @return true if the operation was successful
     */
    public boolean delete() {
        return request.file.delete();
    }

}
