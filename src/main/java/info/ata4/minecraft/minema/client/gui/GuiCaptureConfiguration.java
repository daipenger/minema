package info.ata4.minecraft.minema.client.gui;

import info.ata4.minecraft.minema.CaptureSession;
import info.ata4.minecraft.minema.Minema;
import info.ata4.minecraft.minema.MinemaAPI;
import info.ata4.minecraft.minema.client.config.MinemaConfig;
import info.ata4.minecraft.minema.client.modules.video.VideoHandler;
import info.ata4.minecraft.minema.util.config.ConfigDouble;
import info.ata4.minecraft.minema.util.config.ConfigInteger;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
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
    public GuiTextField engineSpeed;

    public GuiButton showConfig;
    public GuiButton showRecordings;
    public GuiButton record;

    private boolean movieExists;

    @Override
    public void initGui() {
        super.initGui();

        int width = 300;
        int x = (this.width - width) / 2;
        int y = 50;

        this.name = new GuiTextField(0, this.fontRenderer, x + 1, y + 1, 298, 18);

        y += 45;

        this.videoWidth = new GuiTextField(1, this.fontRenderer, x + 1, y + 1, 143, 18);
        this.videoHeight = new GuiTextField(2, this.fontRenderer, x + 155 + 1, y + 1, 143, 18);

        y += 35;

        this.frameRate = new GuiTextField(3, this.fontRenderer, x + 1, y + 1, 143, 18);
        this.frameLimit = new GuiTextField(4, this.fontRenderer, x + 155 + 1, y + 1, 143, 18);

        y += 45;

        this.engineSpeed = new GuiTextField(5, this.fontRenderer, x + 1, y + 1, 298, 18);

        y = this.height - 30;

        this.showConfig = new GuiButton(5, x, y, 95, 20, I18n.format("fml.menu.modoptions"));
        this.showRecordings = new GuiButton(6, x + 100, y, 100, 20, I18n.format("minema.gui.movies_folder"));
        this.record = new GuiButton(7, x + 205, y, 95, 20, I18n.format("minema.gui.record"));

        this.buttonList.add(this.showConfig);
        this.buttonList.add(this.showRecordings);
        this.buttonList.add(this.record);

        /* Fill data */
        MinemaConfig cfg = Minema.instance.getConfig();

        this.videoWidth.setText(cfg.frameWidth.get().toString());
        this.videoHeight.setText(cfg.frameHeight.get().toString());
        this.frameRate.setText(cfg.frameRate.get().toString());
        this.frameLimit.setText(cfg.frameLimit.get().toString());
        this.engineSpeed.setText(cfg.engineSpeed.get().toString());
    }

    public void saveConfigValues() {
        MinemaConfig cfg = Minema.instance.getConfig();
        cfg.frameWidth.set(this.parseInt(this.videoWidth.getText(), cfg.frameWidth));
        cfg.frameHeight.set(this.parseInt(this.videoHeight.getText(), cfg.frameHeight));
        cfg.frameRate.set(this.parseDouble(this.frameRate.getText(), cfg.frameRate));
        cfg.frameLimit.set(this.parseInt(this.frameLimit.getText(), cfg.frameLimit));
        cfg.engineSpeed.set(this.parseDouble(this.engineSpeed.getText(), cfg.engineSpeed));
        cfg.getConfigForge().save();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == this.showConfig) {
            this.saveConfigValues();
            IModGuiFactory guiFactory = FMLClientHandler.instance().getGuiFactoryFor(Minema.container);
            GuiScreen newScreen = guiFactory.createConfigGui(this);

            this.mc.displayGuiScreen(newScreen);
        } else if (button == this.showRecordings) {
            try {
                URI uri = MinemaAPI.getCapturePath().toURI();
                Class<?> clazz = Class.forName("java.awt.Desktop");
                Object object = clazz.getMethod("getDesktop", new Class[0]).invoke(null);

                clazz.getMethod("browse", new Class[] {URI.class}).invoke(object, uri);
            } catch (Throwable t) {}
        } else if (button == this.record) {
            if (this.movieExists)
                return;

            VideoHandler.customName = this.name.getText();
            this.saveConfigValues();
            CaptureSession.singleton.startCapture();

            this.mc.displayGuiScreen(null);

            if (this.mc.currentScreen == null)
                this.mc.setIngameFocus();
        }
    }

    private double parseDouble(String text, ConfigDouble conf) {
        try {
            double rtn = Double.parseDouble(text);
            if (conf.getMax() != null)
            	rtn = Math.min(rtn, conf.getMax());
            if (conf.getMin() != null)
            	rtn = Math.max(rtn, conf.getMin());
            return rtn;
        } catch (Exception e) {
            return conf.get();
        }
    }

    private int parseInt(String text, ConfigInteger conf) {
        try {
            int rtn = Integer.parseInt(text);
            if (conf.getMax() != null)
            	rtn = Math.min(rtn, conf.getMax());
            if (conf.getMin() != null)
            	rtn = Math.max(rtn, conf.getMin());
            return rtn;
        } catch (Exception e) {
            return conf.get();
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
        this.engineSpeed.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_RETURN && !this.movieExists)
            this.actionPerformed(this.record);

        if (keyCode == 1) {
            this.saveConfigValues();
            this.mc.displayGuiScreen(null);

            if (this.mc.currentScreen == null) {
                this.mc.setIngameFocus();
            }
        }

        this.name.textboxKeyTyped(typedChar, keyCode);
        this.videoWidth.textboxKeyTyped(typedChar, keyCode);
        this.videoHeight.textboxKeyTyped(typedChar, keyCode);
        this.frameRate.textboxKeyTyped(typedChar, keyCode);
        this.frameLimit.textboxKeyTyped(typedChar, keyCode);
        this.engineSpeed.textboxKeyTyped(typedChar, keyCode);

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

        this.drawCenteredString(this.fontRenderer, I18n.format("minema.gui.title"), this.width / 2, 10, 0xffffff);

        this.fontRenderer.drawStringWithShadow(I18n.format("minema.gui.name"), this.name.x, this.name.y - 12, 0xffffff);
        this.fontRenderer.drawStringWithShadow(I18n.format("minema.gui.width"), this.videoWidth.x, this.videoWidth.y - 12, 0xffffff);
        this.fontRenderer.drawStringWithShadow(I18n.format("minema.gui.height"), this.videoHeight.x, this.videoHeight.y - 12, 0xffffff);
        this.fontRenderer.drawStringWithShadow(I18n.format("minema.gui.fps"), this.frameRate.x, this.frameRate.y - 12, 0xffffff);
        this.fontRenderer.drawStringWithShadow(I18n.format("minema.gui.limit"), this.frameLimit.x, this.frameLimit.y - 12, 0xffffff);
        this.fontRenderer.drawStringWithShadow(I18n.format("minema.gui.speed"), this.engineSpeed.x, this.engineSpeed.y - 12, 0xffffff);
        this.fontRenderer.drawStringWithShadow(I18n.format("minema.gui.speed.tooltip"), this.engineSpeed.x, this.engineSpeed.y + 24, 0x888888);

        if (this.movieExists)
            this.fontRenderer.drawStringWithShadow(I18n.format("minema.gui.file_exists"), this.name.x, this.name.y + 22, 0xff3355);

        this.name.drawTextBox();
        this.videoWidth.drawTextBox();
        this.videoHeight.drawTextBox();
        this.frameRate.drawTextBox();
        this.frameLimit.drawTextBox();
        this.engineSpeed.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

}