package de.fhg.iais.roberta.ui;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static de.fhg.iais.roberta.ui.main.MainView.TABLE_HEADER_BACKGROUND_COLOR;

public class OraTable extends JTable {

    public OraTable(List<List<Object>> data, List<String> columnIdentifiers, Set<Integer> editableColumns) {
        // Setup header
        this.getTableHeader().setDefaultRenderer(new BorderlessHeaderRenderer());
        this.getTableHeader().setReorderingAllowed(false);
        this.getTableHeader().setResizingAllowed(false);

        // Setup table
        this.setModel(new FixedTableModel(data, columnIdentifiers, editableColumns));
        this.setRowHeight(25);
        this.setShowGrid(false);
        this.setIntercellSpacing(new Dimension(0, 0));
    }

    /**
     * Adds a row if no duplicates were found.
     *
     * @param rowData        the data of the row to be added
     * @param checkedColumns the columns that should be checked for equality
     * @return -1 if successful, or the index of the existing row TODO this is not really nice but works for now
     */
    public int addRow(List<Object> rowData, Collection<Integer> checkedColumns) {
        int index = rowExists(((FixedTableModel) getModel()).getData(), rowData, checkedColumns);

        if (index >= 0) {
            return index;
        } else {
            ((FixedTableModel) getModel()).addRow(rowData);
            return -1;
        }
    }

    public void updateTable(List<List<Object>> newData, Collection<Integer> checkedColumns) {
        // Check if a row has to be removed
        List<List<Object>> data = ((FixedTableModel) getModel()).getData();
        for ( int i = 0; i < data.size(); i++ ) {
            List<Object> rowData = data.get(i);

            int rowIndex = rowExists(newData, rowData, checkedColumns);
            if ( rowIndex == -1 ) {
                ((FixedTableModel) getModel()).removeRow(i);
            }
        }

        // Check if a row needs to be added
        for ( List<Object> rowData : newData ) {
            addRow(rowData, checkedColumns);
        }
    }

    /**
     * Checks whether a row already exists based on the checked columns.
     *
     * @param tableData   the base table which is examined
     * @param row         the row which searched for in the table
     * @param checkedCols which columns to compare
     * @return -1 if the does not exist, its index otherwise
     */
    private static int rowExists(List<List<Object>> tableData, List<Object> row, Collection<Integer> checkedCols) {
        for ( int i = 0; i < tableData.size(); i++ ) {
            List<Object> otherRow = tableData.get(i);

            int equalColumns = 0;
            for ( Integer checkedColumn : checkedCols ) {
                if (otherRow.get(checkedColumn).equals(row.get(checkedColumn))) {
                    equalColumns++;
                }
            }

            if ( equalColumns == checkedCols.size() ) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Table model with default non editable columns. Editable columns can be specified in the set.
     */
    public static class FixedTableModel extends AbstractTableModel {

        private final List<List<Object>> data;
        private final List<String> columnIdentifiers;
        private final Set<Integer> editableColumns;

        FixedTableModel(List<List<Object>> data, List<String> columnIdentifiers, Set<Integer> editableColumns) {
            this.data = data;
            this.columnIdentifiers = columnIdentifiers;
            this.editableColumns = editableColumns;
        }

        @Override
        public int getRowCount() {
            return this.data.size();
        }

        @Override
        public int getColumnCount() {
            return this.columnIdentifiers.size();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return this.editableColumns.contains(columnIndex);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return this.data.get(rowIndex).get(columnIndex);
        }

        @Override
        public String getColumnName(int column) {
            return this.columnIdentifiers.get(column);
        }

        public void addRow(List<Object> row) {
            this.data.add(row);
            fireTableDataChanged();
        }

        public void removeRow(int index) {
            this.data.remove(index);
            fireTableDataChanged();
        }

        List<List<Object>> getData() {
            return this.data;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            this.data.get(rowIndex).set(columnIndex, aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    private static class BorderlessHeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 2));
            setBackground(TABLE_HEADER_BACKGROUND_COLOR);

            return this;
        }
    }
}
