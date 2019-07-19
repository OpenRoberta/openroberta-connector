package de.fhg.iais.roberta.ui;

import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static de.fhg.iais.roberta.ui.main.MainView.HOVER_COLOR;

/**
 * Adapted from ButtonColumn by Rob Camick https://tips4java.wordpress.com/2009/07/12/table-button-column/
 *
 * The OraButtonColumn class provides a renderer and an editor that looks like a
 * JButton. The renderer and editor will then be used for a specified column
 * in the table. The TableModel will contain the String to be displayed on
 * the button.
 * The button can be invoked by a mouse click or by pressing the space bar
 * when the cell has focus.
 * When the button is invoked the provided Action is invoked. The
 * source of the Action will be the table. The actionListener command will contain
 * the model row number of the button that was clicked.
 */
public class OraButtonColumn extends AbstractCellEditor implements TableCellRenderer, TableCellEditor, ActionListener, MouseListener {
    private final JTable table;
    private final ActionListener actionListener;
    private final String baseCommand;

    private final JButton renderButton;
    private final JButton editButton;
    private Object editorValue = null;
    private boolean isButtonColumnEditor = false;

    /**
     * Create the OraButtonColumn to be used as a renderer and editor. The
     * renderer and editor will automatically be installed on the TableColumn
     * of the specified column.
     *
     * @param table  the table containing the button renderer/editor
     * @param actionListener the ActionListener to be invoked when the button is invoked
     * @param column the column to which the button renderer/editor is added
     */
    public OraButtonColumn(JTable table, ActionListener actionListener, String baseCommand, int column) {
        this.table = table;
        this.actionListener = actionListener;
        this.baseCommand = baseCommand;

        this.renderButton = new JButton();
        this.editButton = new JButton();
        this.editButton.setFocusPainted(false);
        this.editButton.addActionListener(this);

        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(column).setCellRenderer(this);
        columnModel.getColumn(column).setCellEditor(this);
        table.addMouseListener(this);
    }

    @Override
    public Component getTableCellEditorComponent(
        JTable table, Object value, boolean isSelected, int row, int column) {
        if ( value == null ) {
            this.editButton.setText("");
            this.editButton.setIcon(null);
        } else if ( value instanceof Icon ) {
            this.editButton.setText("");
            this.editButton.setIcon((Icon) value);
        } else {
            this.editButton.setText(value.toString());
            this.editButton.setIcon(null);
        }

        this.editorValue = value;
        return this.editButton;
    }

    @Override
    public Object getCellEditorValue() {
        return this.editorValue;
    }

    //
    //  Implement TableCellRenderer interface
    //
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if ( isSelected ) {
            this.renderButton.setForeground(table.getSelectionForeground());
            this.renderButton.setBackground(HOVER_COLOR);
        } else {
            this.renderButton.setForeground(table.getForeground());
            this.renderButton.setBackground(UIManager.getColor("Button.background"));
        }

        if ( value == null ) {
            this.renderButton.setText("");
            this.renderButton.setIcon(null);
        } else if ( value instanceof Icon ) {
            this.renderButton.setText("");
            this.renderButton.setIcon((Icon) value);
        } else {
            this.renderButton.setText(value.toString());
            this.renderButton.setIcon(null);
        }

        return this.renderButton;
    }

    //
    //  Implement ActionListener interface
    //
    /*
     *	The button has been pressed. Stop editing and invoke the custom Action
     */
    public void actionPerformed(ActionEvent e) {
        int row = this.table.convertRowIndexToModel(this.table.getEditingRow());
        fireEditingStopped();

        //  Invoke the Action

        ActionEvent event = new ActionEvent(this.table, ActionEvent.ACTION_PERFORMED, this.baseCommand + row);
        this.actionListener.actionPerformed(event);
    }

    //
    //  Implement MouseListener interface
    //
    /*
     *  When the mouse is pressed the editor is invoked. If you then then drag
     *  the mouse to another cell before releasing it, the editor is still
     *  active. Make sure editing is stopped when the mouse is released.
     */
    public void mousePressed(MouseEvent e) {
        if ( this.table.isEditing() && (this.table.getCellEditor() == this) )
            this.isButtonColumnEditor = true;
    }

    public void mouseReleased(MouseEvent e) {
        if ( this.isButtonColumnEditor && this.table.isEditing() )
            this.table.getCellEditor().stopCellEditing();

        this.isButtonColumnEditor = false;
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }
}
