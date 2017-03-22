package de.wilms;

import com.sun.speech.freetts.FreeTTS;
import com.sun.speech.freetts.VoiceManager;
import com.sun.speech.freetts.audio.SingleFileAudioPlayer;
import com.vaadin.server.FileResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.UserError;
import com.vaadin.ui.*;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Image;
import com.vaadin.ui.TextField;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Fabian Wilms
 * VaadinSimpleOfflineCaptcha creates an image captcha as well as an audio version of the captcha.
 * The captcha itself is just a simple arithmetic problem.
 *
 */
public class VaadinSimpleOfflineCaptcha extends CustomField<Boolean> {

    /**
     * Operations that can occure in the captcha
     */
    private enum Operation {
        PLUS("+"), MINUS("-"), TIMES("x");
        public String sign;
        Operation(String sign) {
            this.sign = sign;
        }
    }

    /**
     * Random number generator
     */
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    //---- Parameters ----//
    /**
     * Lowest Number that can be generated.
     */
    private static final int NUM_MIN = 0;
    /**
     * Highest number that can be generated.
     */
    private static final int NUM_MAX = 99;
    /**
     * TTS-Voice name
     */
    private static final String VOICE_NAME = "kevin16";

    /**
     * Solution of this captcha for validation
     */
    private double solution;

    //---- Components ----//
    /**
     * Contains the captcha imgae
     */
    private Image image = new Image();

    /**
     * Contains the audio version of the captcha
     */
    private Audio audio = new Audio();

    /**
     * Plays the audio for color blind users.
     */
    private Button audioPlay = new Button();

    /**
     * Input for the solution
     */
    private TextField textField = new TextField();

    /**
     * Generates a new captcha
     */
    private Button refresh = new com.vaadin.ui.Button();

    /**
     * Session ID used to store the generated captcha for this session in a session specific folder.
     */
    private String sessionID;

    /**
     * Path to the image file.
     */
    private File captchaImage;

    /**
     * Path to the sound file
     */
    private File captchaSound;

    /**
     * Initializes the Vaadin component.
     * Creates a first captcha and builds the layout around all components.
     * @return the initialized VaadinSimpleOfflineCaptcha
     */
    protected Component initContent() {
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        sessionID = UUID.randomUUID().toString();

        createNewCaptcha();

        textField.setInputPrompt("Lösung");

        Page.Styles styles = Page.getCurrent().getStyles();
        styles.add(".audioplayerinvisible {display:none !important;}");

        audio.addStyleName("audioplayerinvisible");

        audioPlay.setIcon(FontAwesome.PLAY);
        audioPlay.addClickListener(clickEvent -> Page.getCurrent().getJavaScript().execute("document.getElementsByClassName(\"audioplayerinvisible\")[0].play();"));

        refresh.setIcon(FontAwesome.REFRESH);
        refresh.addClickListener(clickEvent -> createNewCaptcha());

        HorizontalLayout components = new HorizontalLayout(image, audio, audioPlay, textField, refresh);
        components.setSpacing(true);
        components.setComponentAlignment(image, Alignment.MIDDLE_CENTER);
        return components;
    }

    /**
     * Deletes all old captcha files, generates a new captcha and an according image as well as a sound file.
     * Reloads all components with the new captcha.
     */
    public void createNewCaptcha(){
        if(captchaImage != null) {
            captchaImage.delete();
        }
        if(captchaSound != null) {
            captchaSound.delete();
        }
        String path = getRandomPath();
        String newCaptcha = createCaptcha();
        createImageResource(newCaptcha, path + ".PNG");
        createAudioResource(newCaptcha, path);
        captchaSound = new File(path+ ".wav");
        captchaImage = new File(path+ ".PNG");
        audio.setSource(new FileResource(captchaSound));
        image.setSource(new FileResource(captchaImage));
    }

    /**
     * Helper-Method to create a random path for each file.
     * @return A random path for storing a file.
     */
    private String getRandomPath(){
        return "tmp/captchas/" + sessionID + "/" + UUID.randomUUID().toString();
    }

    /**
     * Validates if the captcha was solved. If it wasn't a new captcha is generated.
     * @return true if the captcha was solved. False if the input wasn't correct.
     */
    @Override
    public Boolean getValue() {
        String value = textField.getValue();
        try {
            Double aDouble = Double.valueOf(value);
            if (aDouble != solution) {
                createNewCaptcha();
                textField.setComponentError(new UserError("Das war leider nicht richtig."));
                return false;
            } else {
                textField.setComponentError(null);
                return true;
            }
        } catch(NumberFormatException e) {
            textField.setComponentError(new UserError("Hier sind nur zahlen erlaubt."));
            createNewCaptcha();
            return false;
        }
    }

    /**
     * Generates a random arithmetic problem as the captcha.
     * This method already calculates the solution and stores it inside {@link VaadinSimpleOfflineCaptcha#solution}.
     * @return The captchas string.
     */
    private String createCaptcha() {
        int left = random.nextInt(NUM_MIN, NUM_MAX);
        int right = random.nextInt(NUM_MIN, NUM_MAX);
        Operation op = Operation.values()[random.nextInt(Operation.values().length)];

        switch (op){
            case PLUS:
                solution = left+right;
                break;
            case MINUS:
                solution = left-right;
                break;
            case TIMES:
                solution = left*right;
                break;
        }

        return left + op.sign + right;
    }

    /**
     * Generates an audio file for the visually impaired. It doesn't add any distortion to it though.
     * @param captcha The captcha to be read out loud.
     * @param path The file where the generated sound file should be saved to.
     */
    private void createAudioResource(String captcha, String path){
        FreeTTS freetts;

        /* The VoiceManager manages all the voices for FreeTTS. */
        com.sun.speech.freetts.Voice helloVoice = VoiceManager.getInstance().getVoice(VOICE_NAME);

        if (helloVoice == null) {
            throw new IllegalArgumentException("No voice named \"" + VOICE_NAME + "\" was found.");
        }

        /* Allocates the resources for the voice. */
        helloVoice.allocate();
        SingleFileAudioPlayer player = new SingleFileAudioPlayer(path, AudioFileFormat.Type.WAVE);
        helloVoice.setAudioPlayer(player);

        /* Synthesize speech. */
        captcha = captcha.replace("x", " times ");
        captcha = captcha.replace("-", " minus ");
        helloVoice.speak(captcha);

        /* Clean up and leave. */
        helloVoice.deallocate();
        player.close();
    }

    /**
     * Creates the captcha image itself with a random gradient in the background.
     * @param captcha The captcha to be read out loud.
     * @param path The file where the generated image file should be saved to.
     */
    private void createImageResource(String captcha, String path){
        int width = 80;
        int height = 30;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        Font font = new Font("Arial", Font.PLAIN, 24);
        g2d.setFont(font);

        RenderingHints rh = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        rh.put(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        g2d.setRenderingHints(rh);

        int x1 = random.nextInt(0, 50);
        int y1 = random.nextInt(0, 50);
        int x2 = random.nextInt(0, 50);
        int y2 = random.nextInt(0, 50);

        GradientPaint gp = new GradientPaint(x1, y1, Color.CYAN, x2, y2, Color.lightGray, true);

        g2d.setPaint(gp);
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(new Color(255, 153, 0));

        g2d.drawString(captcha, 0, g2d.getFontMetrics().getAscent());
        g2d.dispose();
        try {
            File outputFile = new File(path);
            outputFile.getParentFile().mkdirs();
            ImageIO.write(img, "png", outputFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public Class<? extends Boolean> getType() {
        return boolean.class;
    }

    /**
     * Deletes all files created in this UI session.
     * Detach when the user navigates to a different site in this vaaadin application
     * or if the page gets refreshed.
     * If the browser or tab gets closed Vaadin waits for three missing heartbeats of this session (by default one heartbeat every 5 minutes)
     * until this session gets closed and the cleanup is executed.
     */
    @Override
    public void detach() {
        try {
            FileUtils.deleteDirectory(new File("tmp/captchas/" + sessionID + "/"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.detach();
    }
}
