package cn.noryea.motionblur.modmenu;

import cn.noryea.motionblur.config.MotionBlurConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class MotionBlurModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new MotionBlurConfigScreen(parent);
    }

    private static final class MotionBlurConfigScreen extends Screen {
        private final Screen parent;

        private MotionBlurConfigScreen(Screen parent) {
            super(Text.literal("Motion Blur Settings"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            super.init();

            int centerX = this.width / 2;
            int startY = this.height / 2 - 32;

            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Toggle"),
                    button -> {
                        MotionBlurConfig.setMotionBlurEnabled(!MotionBlurConfig.ENABLED);
                        button.setMessage(Text.literal("Enabled: " + (MotionBlurConfig.ENABLED ? "On" : "Off")));
                    }
            ).dimensions(centerX - 100, startY, 200, 20).build());

            addDrawableChild(ButtonWidget.builder(
                    Text.literal("-"),
                    button -> {
                        int next = Math.max(0, MotionBlurConfig.MOTIONBLUR_AMOUNT - 5);
                        MotionBlurConfig.setMotionBlurAmount(next);
                        MotionBlurConfig.setMotionBlurEnabled(next > 0);
                    }
            ).dimensions(centerX - 110, startY + 30, 40, 20).build());

            addDrawableChild(ButtonWidget.builder(
                    Text.literal("%: " + MotionBlurConfig.MOTIONBLUR_AMOUNT),
                    button -> {}
            ).dimensions(centerX - 60, startY + 30, 120, 20).build());

            addDrawableChild(ButtonWidget.builder(
                    Text.literal("+"),
                    button -> {
                        int next = Math.min(100, MotionBlurConfig.MOTIONBLUR_AMOUNT + 5);
                        MotionBlurConfig.setMotionBlurAmount(next);
                        MotionBlurConfig.setMotionBlurEnabled(next > 0);
                    }
            ).dimensions(centerX + 70, startY + 30, 40, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> this.close())
                    .dimensions(centerX - 50, startY + 70, 100, 20).build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(this.textRenderer, "Motion Blur: " + MotionBlurConfig.MOTIONBLUR_AMOUNT + "%", this.width / 2, this.height / 2 - 55, 0xFFFFFF);
        }

        @Override
        public void close() {
            if (this.client != null) {
                this.client.setScreen(this.parent);
            }
        }
    }
}
