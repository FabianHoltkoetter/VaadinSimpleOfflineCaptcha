package de.wilms;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import javax.servlet.annotation.WebServlet;

/**
 * Demo Application to showcase the VaadinSimpleOfflineCaptcha
 */
public class MyUI extends UI {

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        final VerticalLayout layout = new VerticalLayout();
        Label l = new Label();

        VaadinSimpleOfflineCaptcha vaadinSimpleOfflineCaptcha = new VaadinSimpleOfflineCaptcha();
        vaadinSimpleOfflineCaptcha.setCaption("Please confirm that you are human.");

        Button button = new Button("Check captcha");
        button.addClickListener( e -> l.setValue("Captcha was " + (vaadinSimpleOfflineCaptcha.getValue()? "solved! :)" : "not solved! :(")));

        layout.addComponents(vaadinSimpleOfflineCaptcha, button, l);
        
        setContent(layout);
    }

    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {}
}
