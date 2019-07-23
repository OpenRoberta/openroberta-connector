package de.fhg.iais.roberta.ui.main;

import java.awt.Image;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;

import javax.swing.ImageIcon;

import de.fhg.iais.roberta.connection.IRobot;
import de.fhg.iais.roberta.ui.UiState;

/**
 * Helper class to load images from the resources.
 */
public final class ImageHelper {

    private static final String IMAGES_PATH = "images/"; // has to be simple slash not File.separator because its used in classpath

    private static final String TITLE_IMAGE_NAME = "OR.png";

    private ImageHelper() {

    }

    private static URL getResource(String name) {
        URL resource = ImageHelper.class.getClassLoader().getResource(IMAGES_PATH + name);
        Objects.requireNonNull(resource);
        return resource;
    }

    /**
     * Loads and returns the fitting GIF for the given state and connection.
     * DISCOVERING ignores the connection type.
     *
     * @param state          the current ui state
     * @param connectionType the connection type of the current robot
     * @return the fitting GIF
     */
    static ImageIcon getGif(UiState state, IRobot.ConnectionType connectionType) {
        String filename;
        switch ( state ) {
            case DISCOVERING:
                filename = state.toString().toLowerCase(Locale.ENGLISH);
                break;
            case DISCOVERED:
            case CONNECTING:
            case CONNECTED:
                filename = state.toString().toLowerCase(Locale.ENGLISH) + '_' + connectionType.toString().toLowerCase(Locale.ENGLISH);
                break;
            default:
                throw new UnsupportedOperationException("UiState not supported!");
        }

        return new ImageIcon(getResource(filename + ".gif"));
    }

    /**
     * Loads and returns the given icon from the resources.
     *
     * @param name the filename in the images folder
     * @return the icon
     */
    public static ImageIcon getIcon(String name) {
        return new ImageIcon(getResource(name));
    }

    /**
     * Returns the title bar image.
     *
     * @return the title bar image
     */
    public static Image getTitleIconImage() {
        return getIcon(TITLE_IMAGE_NAME).getImage();
    }
}
