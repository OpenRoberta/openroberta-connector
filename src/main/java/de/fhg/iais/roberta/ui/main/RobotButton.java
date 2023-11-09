package de.fhg.iais.roberta.ui.main;

import java.awt.*;

import javax.swing.*;

import de.fhg.iais.roberta.ui.UiState;
import static de.fhg.iais.roberta.ui.main.MainView.BACKGROUND_COLOR;

class RobotButton extends JButton {
    private static final Color ROBOT_HOVER_COLOR = Color.decode("#eeeeee");

    private static final String FILENAME_NOT_DISCOVERED = "RobertaNotDiscovered.png";
    private static final String FILENAME_DISCOVERED = "RobertaDiscovered.png";
    private static final String FILENAME_CONNECTED = "RobertaConnected.png";

    RobotButton() {
        this.setBackground(BACKGROUND_COLOR);
        this.setIconTextGap(0);

        this.getModel().addChangeListener(e -> {
            ButtonModel b = (ButtonModel) e.getSource();
            if ( b.isRollover() ) {
                setBackground(ROBOT_HOVER_COLOR);
            } else {
                setBackground(BACKGROUND_COLOR);
            }
        });
        this.setState(UiState.DISCOVERING);

        this.setBorderPainted(true);
        this.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        this.setFocusPainted(false);
    }

    public void setState(UiState state) {
        switch ( state ) {
            case DISCOVERING:
                this.setIcon(ImageHelper.getIcon(FILENAME_NOT_DISCOVERED, 20, 20));
                break;
            case DISCOVERED:
                this.setIcon(ImageHelper.getIcon(FILENAME_DISCOVERED, 20, 20));
                break;
            case CONNECTING:
            case CONNECTED:
                this.setIcon(ImageHelper.getIcon(FILENAME_CONNECTED, 20, 20));
                break;
        }
    }
}
