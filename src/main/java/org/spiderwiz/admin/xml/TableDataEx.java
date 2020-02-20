package org.spiderwiz.admin.xml;

import java.util.List;

/**
 * Extends TableData by providing methods to populate it
 * @author Zvi 
 */
public class TableDataEx extends TableData{
    /**
     * Extends TableData.RowData by providing methods to populate it
     */
    public static class RowDataEx extends RowData {

        /**
         * Add a cell with values to the row
         * @param column    The title of the column containing this cell
         * @param data      Data to display in the cell
         * @param style     Style code as defined in TableInfoEx.Style class. Multiple styles may be combined by |.
         *                  E.g. Style.RIGHT | Style.RTL
         * @param uri       The URI of an application represented by the data in this cell
         * @return the object itself
         */
        public RowDataEx addCell(String column, String data, int style, String uri) {
            if (data == null)
                return this;
            List<CellData> cells = getCellData();
            CellData cell = new CellData();
            cell.setColumn(column);
            cell.setData(data);
            if (style != 0)
                cell.setStyle(TableInfoEx.Style.makeStyles(style));
            cell.setUri(uri);
            cells.add(cell);
            return this;
        }

        /**
         * Add a cell with values to the row when the data is an integer value
         * @param column    The title of the column containing this cell
         * @param data      Data to display in the cell
         * @param style     Style code as defined in TableInfoEx.Style class. Multiple styles may be combined by |.
         *                  E.g. Style.RIGHT | Style.RTL
         * @param uri       The URI of an application represented by the data in this cell
         * @return the object itself
         */
        public RowDataEx addCell(String column, int data, int style, String uri) {
            return addCell(column, String.valueOf(data), style, uri);
        }

            /**
         * Add a cell with values to the row when the data is a double or float value
         * @param column    The title of the column containing this cell
         * @param data      Data to display in the cell
         * @param style     Style code as defined in TableInfoEx.Style class. Multiple styles may be combined by |.
         *                  E.g. Style.RIGHT | Style.RTL
         * @param uri       The URI of an application represented by the data in this cell
         * @return the object itself
         */
        public RowDataEx addCell(String column, double data, int style, String uri) {
            return addCell(column, String.valueOf(data), style, uri);
        }
}

    public RowDataEx addRow() {
        RowDataEx row = new RowDataEx();
        getRowData().add(row);
        return row;
    }
}
