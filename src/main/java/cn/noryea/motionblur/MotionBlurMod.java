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

    private final ManagedShaderEffect motionblur = ShaderEffectManager.getInstance().manage(Identifier.of(ID, "shaders/post/motion_blur.json"),
            shader -> shader.setUniformValue("BlendFactor", getBlur()));

    @Override
    public void onInitializeClient() {
        MotionBlurConfig.registerConfigs(50);

        ClientCommandRegistrationCallback.EVENT.register((callback, a) -> callback.register(
                ClientCommandManager.literal("motionblur")
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

    private static int changeAmount(FabricClientCommandSource src, int amount) {
        MotionBlurConfig.setMotionBlurAmount(amount);

        src.sendFeedback(Text.of("Motion Blur: " + amount + "%"));
        return amount;
    }

    public float getBlur() {
        return MotionBlurConfig.ENABLED ? Math.min(MotionBlurConfig.MOTIONBLUR_AMOUNT, 99) / 100F : 0F;
    }

}
