package de.fhg.iais.roberta.ui;

import javax.swing.ButtonModel;
import javax.swing.JButton;

import static de.fhg.iais.roberta.ui.main.MainView.BUTTON_BACKGROUND_COLOR;
import static de.fhg.iais.roberta.ui.main.MainView.HOVER_COLOR;

public class OraButton extends JButton {
    private static final long serialVersionUID = 1L;

    public OraButton() {
        this.getModel().addChangeListener(e -> {
            ButtonModel b = (ButtonModel) e.getSource();
            if ( b.isRollover() ) {
                setBackground(HOVER_COLOR);
            } else {
                setBackground(BUTTON_BACKGROUND_COLOR);
            }
        });
    }
}
