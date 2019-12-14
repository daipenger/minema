package info.ata4.minecraft.minema.client.gui;

import info.ata4.minecraft.minema.CaptureSession;
import info.ata4.minecraft.minema.Minema;
import info.ata4.minecraft.minema.client.config.MinemaConfig;
import info.ata4.minecraft.minema.client.modules.video.VideoHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.IModGuiFactory;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GuiCaptureConfiguration extends GuiScreen {

    public GuiTextField name;

    public GuiTextField videoWidth;
    public GuiTextField videoHeight;
    public GuiTextField frameRate;
    public GuiTextField frameLimit;

    public GuiButton showConfig;
    public GuiButton showRecordings;
    public GuiButton record;

    private boolean movieExists;

    @Override
    public void initGui() {
        super.initGui();

        int width = 300;
        int x = (this.width - width) / 2;
        int y = 60;

        this.name = new GuiTextField(0, this.fontRenderer, x + 1, y + 1, 298, 18);

        y += 60;

        this.videoWidth = new GuiTextField(1, this.fontRenderer, x + 1, y + 1, 143, 18);
        this.videoHeight = new GuiTextField(2, this.fontRenderer, x + 155 + 1, y + 1, 143, 18);

        y += 35;

        this.frameRate = new GuiTextField(3, this.fontRenderer, x + 1, y + 1, 143, 18);
        this.frameLimit = new GuiTextField(4, this.fontRenderer, x + 155 + 1, y + 1, 143, 18);

        y = this.height - 40;

        this.showConfig = new GuiButton(5, x, y, 95, 20, "Mod Options");
        this.showRecordings = new GuiButton(6, x + 100, y, 100, 20, "Movies Folder");
        this.record = new GuiButton(7, x + 205, y, 95, 20, "Record...");

        this.buttonList.add(this.showConfig);
        this.buttonList.add(this.showRecordings);
        this.buttonList.add(this.record);

        /* Fill data */
        MinemaConfig cfg = Minema.instance.getConfig();

        this.videoWidth.setText(cfg.frameWidth.get().toString());
        this.videoHeight.setText(cfg.frameHeight.get().toString());
        this.frameRate.setText(cfg.frameRate.get().toString());
        this.frameLimit.setText(cfg.frameLimit.get().toString());
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        MinemaConfig cfg = Minema.instance.getConfig();

        if (button == this.showConfig) {
            IModGuiFactory guiFactory = FMLClientHandler.instance().getGuiFactoryFor(Minema.container);
            GuiScreen newScreen = guiFactory.createConfigGui(this);

            this.mc.displayGuiScreen(newScreen);
        } else if (button == this.showRecordings) {
            try {
                URI uri = new File(cfg.capturePath.get()).toURI();
                Class<?> clazz = Class.forName("java.awt.Desktop");
                Object object = clazz.getMethod("getDesktop", new Class[0]).invoke(null);

                clazz.getMethod("browse", new Class[] {URI.class}).invoke(object, new Object[] {uri});
            } catch (Throwable t) {}
        } else if (button == this.record) {
            if (this.movieExists)
                return;

            VideoHandler.customName = this.name.getText();
            cfg.frameWidth.set(this.parseInt(this.videoWidth.getText(), cfg.frameWidth.get()));
            cfg.frameHeight.set(this.parseInt(this.videoHeight.getText(), cfg.frameHeight.get()));
            cfg.frameRate.set(this.parseDouble(this.frameRate.getText(), cfg.frameRate.get()));
            cfg.frameLimit.set(this.parseInt(this.frameLimit.getText(), cfg.frameLimit.get()));

            CaptureSession.singleton.startCapture();

            this.mc.displayGuiScreen(null);

            if (this.mc.currentScreen == null)
                this.mc.setIngameFocus();
        }
    }

    private double parseDouble(String text, double orDefault) {
        try {
            return Double.parseDouble(text);
        } catch (Exception e) {
            return orDefault;
        }
    }

    private int parseInt(String text, int orDefault) {
        try {
            return Integer.parseInt(text);
        } catch (Exception e) {
            return orDefault;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        this.name.mouseClicked(mouseX, mouseY, mouseButton);
        this.videoWidth.mouseClicked(mouseX, mouseY, mouseButton);
        this.videoHeight.mouseClicked(mouseX, mouseY, mouseButton);
        this.frameRate.mouseClicked(mouseX, mouseY, mouseButton);
        this.frameLimit.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_RETURN && !this.movieExists)
            this.actionPerformed(this.record);

        super.keyTyped(typedChar, keyCode);

        this.name.textboxKeyTyped(typedChar, keyCode);
        this.videoWidth.textboxKeyTyped(typedChar, keyCode);
        this.videoHeight.textboxKeyTyped(typedChar, keyCode);
        this.frameRate.textboxKeyTyped(typedChar, keyCode);
        this.frameLimit.textboxKeyTyped(typedChar, keyCode);

        this.updateMoviesExist();
    }

    private void updateMoviesExist() {
        MinemaConfig cfg = Minema.instance.getConfig();
        Path folder = Paths.get(cfg.capturePath.get());
        String filename = this.name.getText();

        this.movieExists = !filename.isEmpty() && (Files.exists(folder.resolve(filename)) || Files.exists(folder.resolve(filename + ".mp4")));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        this.drawCenteredString(this.fontRenderer, "Video recording configuration", this.width / 2, 20, 0xffffff);

        this.fontRenderer.drawStringWithShadow("Output name", this.name.x, this.name.y - 12, 0xffffff);
        this.fontRenderer.drawStringWithShadow("Width", this.videoWidth.x, this.videoWidth.y - 12, 0xffffff);
        this.fontRenderer.drawStringWithShadow("Height", this.videoHeight.x, this.videoHeight.y - 12, 0xffffff);
        this.fontRenderer.drawStringWithShadow("Frame rate (FPS)", this.frameRate.x, this.frameRate.y - 12, 0xffffff);
        this.fontRenderer.drawStringWithShadow("Frame limit", this.frameLimit.x, this.frameLimit.y - 12, 0xffffff);

        if (this.movieExists)
            this.fontRenderer.drawStringWithShadow("A file with such name exists already, pick another...", this.name.x, this.name.y + 22, 0xff3355);

        this.name.drawTextBox();
        this.videoWidth.drawTextBox();
        this.videoHeight.drawTextBox();
        this.frameRate.drawTextBox();
        this.frameLimit.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

}