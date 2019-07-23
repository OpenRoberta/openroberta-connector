package de.fhg.iais.roberta.ui.serialMonitor;

import org.apache.commons.codec.Charsets;

import de.fhg.iais.roberta.ui.OraButton;
import de.fhg.iais.roberta.ui.main.ImageHelper;
import de.fhg.iais.roberta.util.IOraUiListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ResourceBundle;


class SerialMonitorView extends JFrame {

    static final String CMD_RESTART = "restart";
    static final String CMD_CLEAR = "clear";

    private final JTextArea textArea = new JTextArea();
    private final JScrollPane scrollPane = new JScrollPane(this.textArea,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    private final JPanel options = new JPanel();
    private final OraButton restartButton = new OraButton();
    private final JComboBox<Integer> rateSelection = new JComboBox<>(
        new Integer[] { 1200, 2400, 4800, 9600, 19200, 38400, 57600, 74880, 115200 });
    private final OraButton clearButton = new OraButton();

    SerialMonitorView(ResourceBundle messages, IOraUiListener listener) {
        // General
        this.setSize(700, 500);
        this.setLocationRelativeTo(null);
        this.addWindowListener(listener);

        // Titlebar
        this.setIconImage(ImageHelper.getTitleIconImage());
        this.setTitle(messages.getString("serialMonitor"));

        this.add(this.scrollPane, BorderLayout.CENTER);
        this.scrollPane.setBorder(new EmptyBorder(4, 4, 4, 4));
        this.textArea.setRows(16);
        this.textArea.setColumns(40);
        this.textArea.setEditable(false);

        this.add(this.options, BorderLayout.PAGE_END);
        this.options.setBorder(new EmptyBorder(4, 4, 4, 4));
        this.options.setLayout(new FlowLayout(FlowLayout.TRAILING));
        this.options.add(Box.createHorizontalGlue());

        this.options.add(this.restartButton);
        this.restartButton.setText(messages.getString("restart"));
        this.restartButton.setActionCommand(CMD_RESTART);
        this.restartButton.addActionListener(listener);

        this.options.add(this.rateSelection);
        this.rateSelection.setMaximumSize(this.rateSelection.getMinimumSize());
        this.rateSelection.setSelectedIndex(3);
        this.rateSelection.addActionListener(listener);
        this.rateSelection.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        this.options.add(Box.createRigidArea(new Dimension(8, 0)));
        this.options.add(Box.createRigidArea(new Dimension(8, 0)));

        this.options.add(this.clearButton);
        this.clearButton.setText(messages.getString("clearOutput"));
        this.clearButton.setActionCommand(CMD_CLEAR);
        this.clearButton.addActionListener(listener);
    }

    int getSerialRate() {
        Object selected = this.rateSelection.getSelectedItem();
        return (selected != null) ? (int) selected : 0;
    }

    void appendText(byte[] bytes) {
        this.textArea.append(new String(bytes, Charsets.UTF_8));
        this.textArea.setCaretPosition(this.textArea.getDocument().getLength());
    }

    void clearText() {
        this.textArea.setText("");
    }
}
