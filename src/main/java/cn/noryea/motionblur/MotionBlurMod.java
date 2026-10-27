package cn.noryea.motionblur;

import cn.noryea.motionblur.config.MotionBlurConfig;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.ladysnake.satin.api.event.ShaderEffectRenderCallback;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.ladysnake.satin.api.managed.ShaderEffectManager;

public class MotionBlurMod implements ClientModInitializer {

    public static String ID = "motionblur";
    private float currentBlur;

    private final ManagedShaderEffect motionblur = ShaderEffectManager.getInstance().manage(
            Identifier.of(ID, "shaders/post/motion_blur.json"),
            shader -> shader.setUniformValue("BlendFactor", getBlur())
    );

    @Override
    public void onInitializeClient() {
        MotionBlurConfig.registerConfigs(50);

        ClientCommandRegistrationCallback.EVENT.register((callback, registryAccess) -> callback.register(
                ClientCommandManager.literal("motionblur")
                        .executes(context -> toggle(context.getSource()))
                        .then(ClientCommandManager.literal("help")
                                .executes(context -> help(context.getSource())))
                        .then(ClientCommandManager.literal("toggle")
                                .executes(context -> toggle(context.getSource())))
                        .then(ClientCommandManager.argument("percent", IntegerArgumentType.integer(0, 100))
                                .executes(context -> changeAmount(context.getSource(), IntegerArgumentType.getInteger(context, "percent"))))
        ));

        ShaderEffectRenderCallback.EVENT.register((deltaTick) -> {
            if (!MotionBlurConfig.ENABLED) {
                return;
            }

            if (getBlur() != 0) {
                if (currentBlur != getBlur()) {
                    motionblur.setUniformValue("BlendFactor", getBlur());
                    currentBlur = getBlur();
                }
                motionblur.render(deltaTick);
            }
        });
    }

    private static int toggle(FabricClientCommandSource src) {
        MotionBlurConfig.setMotionBlurEnabled(!MotionBlurConfig.ENABLED);
        src.sendFeedback(Text.literal("Motion Blur: " + (MotionBlurConfig.ENABLED ? "Enabled" : "Disabled")));
        return MotionBlurConfig.ENABLED ? 1 : 0;
    }

    private static int help(FabricClientCommandSource src) {
        src.sendFeedback(Text.literal("Motion Blur Commands:\n" +
                "- /motionblur - toggle on/off\n" +
                "- /motionblur <percent> - set blur strength\n" +
                "- /motionblur help - show this help text"));
        return 1;
    }

    private static int changeAmount(FabricClientCommandSource src, int amount) {
        MotionBlurConfig.setMotionBlurAmount(amount);
        MotionBlurConfig.setMotionBlurEnabled(amount > 0);

        src.sendFeedback(Text.literal("Motion Blur: " + amount + "%"));
        return amount;
    }

    public float getBlur() {
        return MotionBlurConfig.ENABLED ? Math.min(MotionBlurConfig.MOTIONBLUR_AMOUNT, 99) / 100F : 0F;
    }
}
