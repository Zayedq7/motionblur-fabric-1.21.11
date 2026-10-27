package cn.noryea.motionblur.modmenu;

import cn.noryea.motionblur.config.MotionBlurConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
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
            int startY = this.height / 2 - 20;

            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Motion Blur: " + (MotionBlurConfig.ENABLED ? "Enabled" : "Disabled")),
                    button -> {
                        MotionBlurConfig.setMotionBlurEnabled(!MotionBlurConfig.ENABLED);
                        button.setMessage(Text.literal("Motion Blur: " + (MotionBlurConfig.ENABLED ? "Enabled" : "Disabled")));
                    }
            ).dimensions(centerX - 100, startY, 200, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> this.close())
                    .dimensions(centerX - 50, startY + 40, 100, 20).build());
        }

        @Override
        public void close() {
            if (this.client != null) {
                this.client.setScreen(this.parent);
            }
        }
    }
}
