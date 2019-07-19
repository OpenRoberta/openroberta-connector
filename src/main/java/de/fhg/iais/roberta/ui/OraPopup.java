package de.fhg.iais.roberta.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;

import de.fhg.iais.roberta.ui.main.MainController;

public final class OraPopup {
    private static final Logger LOG = LoggerFactory.getLogger(OraPopup.class);

    private static final int WIDTH = 250;

    private OraPopup() {
    }

    public static int showPopup(Component parent, String keyTitle, String keyMessage, ResourceBundle messages, Icon icon, String[] txtButtons, String... entries) {
        OraButton[] buttons = new OraButton[txtButtons.length];

        for ( int i = 0; i < txtButtons.length; i++ ) {
            OraButton oraButton = new OraButton();
            oraButton.setText(messages.getString(txtButtons[i]));
            oraButton.addActionListener(e -> {
                JOptionPane pane = (JOptionPane) ((Component) e.getSource()).getParent().getParent();
                pane.setValue(oraButton);
            });

            buttons[i] = oraButton;
        }

        String message = messages.getString(keyMessage);
        Matcher matcher = Pattern.compile("\\{\\d+\\}").matcher(message);
         int matchCount = 0;
        while ( matcher.find() ) {
            matchCount++;
        }
        if ( entries.length != matchCount ) {
            throw new IllegalArgumentException("Number of entries does not match key placeholders");
        }
        String formattedMessage = "<html><body<p style='width: " + WIDTH + "px;'>" + MessageFormat.format(message, (Object[]) entries) + "</body></html>";

        JEditorPane pane = new JEditorPane();
        pane.addHyperlinkListener(hyperlinkEvent -> {
            if ( hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse((hyperlinkEvent.getURL().toURI()));
                } catch ( IOException | URISyntaxException e1 ) {
                    LOG.error("Could not open browser: {}", e1.getMessage());
                }
            }
        });
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
        pane.setText(formattedMessage);
        pane.setEditable(false);

        return JOptionPane.showOptionDialog(parent,
                                            pane,
                                            messages.getString(keyTitle),
                                            JOptionPane.DEFAULT_OPTION,
                                            JOptionPane.PLAIN_MESSAGE,
                                            icon,
                                            buttons,
                                            buttons[0]);
    }
}
