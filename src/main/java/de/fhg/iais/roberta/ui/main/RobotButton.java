package de.fhg.iais.roberta.ui.main;

import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import java.awt.Color;

import de.fhg.iais.roberta.ui.UiState;

import static de.fhg.iais.roberta.ui.main.MainView.BACKGROUND_COLOR;

class RobotButton extends JButton {
    private static final Color ROBOT_HOVER_COLOR = Color.decode("#eeeeee");

    private static final String FILENAME_NOT_DISCOVERED = "Roberta_Menu_Icon_grey.png";
    private static final String FILENAME_DISCOVERED = "Roberta_Menu_Icon_green.png";
    private static final String FILENAME_CONNECTED = "Roberta_Menu_Icon_red.png";

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
        this.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        this.setFocusPainted(false);
    }

    public void setState(UiState state) {
        switch ( state ) {
            case DISCOVERING:
                this.setIcon(ImageHelper.getIcon(FILENAME_NOT_DISCOVERED));
                break;
            case DISCOVERED:
                this.setIcon(ImageHelper.getIcon(FILENAME_DISCOVERED));
                break;
            case CONNECTING:
            case CONNECTED:
                this.setIcon(ImageHelper.getIcon(FILENAME_CONNECTED));
                break;
        }
    }
}
