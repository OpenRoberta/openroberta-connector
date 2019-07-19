package de.fhg.iais.roberta.ui.main;

import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import java.awt.Color;
import java.util.Objects;

import static de.fhg.iais.roberta.ui.main.MainView.BACKGROUND_COLOR;
import static de.fhg.iais.roberta.ui.main.MainView.IMAGES_PATH;

class RobotButton extends JButton {
    enum State {
        NOT_DISCOVERED,
        DISCOVERED,
        CONNECTED
    }

    private static final Color ROBOT_HOVER_COLOR = Color.decode("#eeeeee");

    private static final String NOT_DISCOVERED_ICON = IMAGES_PATH + "Roberta_Menu_Icon_grey.png";
    private static final String DISCOVERED_ICON = IMAGES_PATH + "Roberta_Menu_Icon_green.png";
    private static final String CONNECTED_ICON = IMAGES_PATH + "Roberta_Menu_Icon_red.png";

    private static final Icon ICON_ROBOT_CONNECTED = new ImageIcon(Objects.requireNonNull(RobotButton.class.getClassLoader().getResource(CONNECTED_ICON)));
    private static final Icon ICON_ROBOT_DISCOVERED = new ImageIcon(Objects.requireNonNull(RobotButton.class.getClassLoader().getResource(DISCOVERED_ICON)));
    private static final Icon ICON_ROBOT_NOT_DISCOVERED = new ImageIcon(Objects.requireNonNull(RobotButton.class.getClassLoader().getResource(NOT_DISCOVERED_ICON)));

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
        this.setState(State.NOT_DISCOVERED);

        this.setBorderPainted(true);
        this.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        this.setFocusPainted(false);
    }

    public void setState(State state) {
        switch ( state ) {
            case NOT_DISCOVERED:
                this.setIcon(ICON_ROBOT_NOT_DISCOVERED);
                break;
            case DISCOVERED:
                this.setIcon(ICON_ROBOT_DISCOVERED);
                break;
            case CONNECTED:
                this.setIcon(ICON_ROBOT_CONNECTED);
                break;
        }
    }
}
