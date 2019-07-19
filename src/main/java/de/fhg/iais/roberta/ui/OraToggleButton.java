package de.fhg.iais.roberta.ui;

import javax.swing.ButtonModel;
import javax.swing.JToggleButton;

import static de.fhg.iais.roberta.ui.main.MainView.BUTTON_BACKGROUND_COLOR;
import static de.fhg.iais.roberta.ui.main.MainView.HOVER_COLOR;

public class OraToggleButton extends JToggleButton {
    private static final long serialVersionUID = 1L;

    public OraToggleButton() {
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
