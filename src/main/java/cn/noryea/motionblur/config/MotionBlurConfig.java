package cn.noryea.motionblur.config;

import cn.noryea.motionblur.MotionBlurMod;
import com.mojang.datafixers.util.Pair;

public class MotionBlurConfig {

    public static SimpleConfig CONFIG;

    public static int MOTIONBLUR_AMOUNT;
    public static boolean ENABLED;

    public static void registerConfigs(int amount) {
        MotionBlurConfigProvider provider = new MotionBlurConfigProvider();
        provider.addKeyValuePair(new Pair<>("motionblur.amount", amount));
        provider.addKeyValuePair(new Pair<>("motionblur.enabled", true));

        CONFIG = SimpleConfig.of(MotionBlurMod.ID).provider(provider).request();
        syncConfigs();
    }

    private static void syncConfigs() {
        MOTIONBLUR_AMOUNT = CONFIG.getOrDefault("motionblur.amount", 50);
        ENABLED = CONFIG.getOrDefault("motionblur.enabled", true);
    }

    public static void setMotionBlurAmount(int value) {
        CONFIG.set("motionblur.amount", value);
        syncConfigs();
    }

    public static void setMotionBlurEnabled(boolean value) {
        CONFIG.set("motionblur.enabled", value);
        syncConfigs();
    }
}
