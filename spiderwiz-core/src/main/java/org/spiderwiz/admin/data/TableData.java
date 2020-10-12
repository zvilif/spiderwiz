package org.spiderwiz.admin.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.spiderwiz.admin.services.AdminServices;
import org.spiderwiz.annotation.WizField;
import org.spiderwiz.annotation.WizSerializable;

/**
 * Holds the data used by <a href="http://spideradmin.com">SpiderAdmin</a> to populate a table in an application page.
 * @see PageInfo
 */
@WizSerializable
public class TableData {

    /**
     * Holds data to populate one table row.
     */
    @WizSerializable
    public static class RowData {

        /**
         * Holds data for each table cell.
         */
        @WizSerializable
        public static class CellData {

            @WizField private String column;
            @WizField private String data;
            @WizField private String style;
            @WizField private String uri;

            /**
             * Gets the value of the column property.
             * <p>
             * The {@code column} property identifies the column in which the data shall be displayed by specifying the column
             * title as defined by
             * {@link PageInfo.TableInfo.ColumnInfo#getTitle() PageInfo.TableInfo.ColumnInfo.getTitle()}.
             * @return the value of the column property.
             */
            public String getColumn() {
                return column;
            }

            /**
             * Sets the value of the column property.
             * <p>
             * The {@code column} property identifies the column in which the data shall be displayed by specifying the column
             * title as defined by
             * {@link PageInfo.TableInfo.ColumnInfo#getTitle() PageInfo.TableInfo.ColumnInfo.getTitle()}.
             * @param value
             */
            public void setColumn(String value) {
                this.column = value;
            }

            /**
             * Gets the value of the data property.
             * <p>
             * The {@code data} property contains the data to display in the referenced cell.
             * @return the value of the data property.
             */
            public String getData() {
                return data;
            }

            /**
             * Sets the value of the data property.
             * <p>
             * The {@code data} property contains the data to display in the referenced cell.
             * @param value
             */
            public void setData(String value) {
                this.data = value;
            }

            /**
             * Gets the value of the style property.
             * <p>
             * The {@code style} property specifies the HTML style to use for the data in the referenced cell. If the property
             * value is {@code null} then the default column style defined in
             * {@link PageInfo.TableInfo.ColumnInfo#getStyle() PageInfo.TableInfo.ColumnInfo.getStyle()} is used.
             * @return the value of the style property.
             */
            public String getStyle() {
                return style;
            }

            /**
             * Sets the value of the style property.
             * The {@code style} property specifies the HTML style to use for the data in the referenced cell. If the property
             * value is {@code null} then the default column style defined in
             * {@link PageInfo.TableInfo.ColumnInfo#getStyle() PageInfo.TableInfo.ColumnInfo.getStyle()} is used.
             * @param value
             */
            public void setStyle(String value) {
                this.style = value;
            }

            /**
             * Gets the value of the uri property.
             * <p>
             * If the {@code uri} property is not {@code null} then the cell data is displayed as a link (HTML {@code a} tag)
             * referencing the property value.
             * @return the value of the uri property.
             */
            public String getUri() {
                return uri;
            }

            /**
             * Sets the value of the uri property.
             * <p>
             * If the {@code uri} property is not {@code null} then the cell data is displayed as a link (HTML {@code a} tag)
             * referencing the property value.
             * @param value
             */
            public void setUri(String value) {
                this.uri = value;
            }
        }
        
        @WizField private final ArrayList<TableData.RowData.CellData> cellData;

        /**
         * Constructs and empty object
         */
        public RowData() {
            cellData = new ArrayList<>();
        }

        /**
         * Gets the value of the cellData property.
         * <p>
         * The {@code cellData} property is a list of inner classes that hold data for each cell in the row.
         * @return the value of the cellData property.
         */
        public List<TableData.RowData.CellData> getCellData() {
            return this.cellData;
        }

        /**
         * Add a cell descriptor to the row with the data as a String.
         * <p>
         * The method arguments are:
         * <ul>
         * <li>{@code column}: Identifies the column in which the data shall be displayed by specifying the column title as
         * defined by {@link PageInfo.TableInfo.ColumnInfo#getTitle() PageInfo.TableInfo.ColumnInfo.getTitle()}.</li>
         * <li>{@code data}: Contains the data to display in the referenced cell. This can be any object that can
         * be stringified. If the object is of type {@link java.util.Date Date} then it will be displayed by the browser
         * using its locale.</li>
         * <li>{@code style}: A bit combination of {@link PageInfo.TableInfo.Style} values that specify the HTML styles
         * used for displaying the data in the cell. If the argument value is zero then the default column style defined in
         * {@link PageInfo.TableInfo.ColumnInfo#getStyle() PageInfo.TableInfo.ColumnInfo.getStyle()} is used.</li>
         * <li>{@code uri}: If not {@code null} then the cell data is displayed as a link (HTML {@code a} tag)
         * referencing this value.</li>
         * </ul>
         * @param column    Column title.
         * @param data      Cell data.
         * @param style     Cell style.
         * @param uri       If not null then the data is displayed as a link to this value.
         * @return this object.
         */
        public RowData addCell(String column, Object data, int style, String uri) {
            if (data == null)
                return this;
            CellData cell = new CellData();
            cell.setColumn(column);
            cell.setData(data instanceof Date ? AdminServices.DATE_PREFIX + ((Date)data).getTime() : data.toString());
            if (style != 0)
                cell.setStyle(PageInfo.TableInfo.Style.makeStyles(style));
            cell.setUri(uri);
            cellData.add(cell);
            return this;
        }

        /**
         * Add a cell descriptor to the row with the data as an integer value.
         * <p>
         * The method arguments are:
         * <ul>
         * <li>{@code column}: Identifies the column in which the data shall be displayed by specifying the column title as
         * defined by {@link PageInfo.TableInfo.ColumnInfo#getTitle() PageInfo.TableInfo.ColumnInfo.getTitle()}.</li>
         * <li>{@code data}: Contains an integer value to display in the referenced cell.</li>
         * <li>{@code style}: A bit combination of {@link PageInfo.TableInfo.Style} values that specify the HTML styles
         * used for displaying the data in the cell. If the argument value is zero then the default column style defined in
         * {@link PageInfo.TableInfo.ColumnInfo#getStyle() PageInfo.TableInfo.ColumnInfo.getStyle()} is used.</li>
         * <li>{@code uri}: If not {@code null} then the cell data is displayed as a link (HTML {@code a} tag)
         * referencing this value.</li>
         * </ul>
         * @param column    Column title.
         * @param data      Cell data as an integer value.
         * @param style     Cell style.
         * @param uri       If not null then the data is displayed as a link to this value.
         * @return this object.
         */
        public RowData addCell(String column, int data, int style, String uri) {
            return addCell(column, String.valueOf(data), style, uri);
        }

        /**
         * Add a cell descriptor to the row with the data as a {@code double} value.
         * <p>
         * The method arguments are:
         * <ul>
         * <li>{@code column}: Identifies the column in which the data shall be displayed by specifying the column title as
         * defined by {@link PageInfo.TableInfo.ColumnInfo#getTitle() PageInfo.TableInfo.ColumnInfo.getTitle()}.</li>
         * <li>{@code data}: Contains a {@code double} value to display in the referenced cell.</li>
         * <li>{@code style}: A bit combination of {@link PageInfo.TableInfo.Style} values that specify the HTML styles
         * used for displaying the data in the cell. If the argument value is zero then the default column style defined in
         * {@link PageInfo.TableInfo.ColumnInfo#getStyle() PageInfo.TableInfo.ColumnInfo.getStyle()} is used.</li>
         * <li>{@code uri}: If not {@code null} then the cell data is displayed as a link (HTML {@code a} tag)
         * referencing this value.</li>
         * </ul>
         * @param column    Column title.
         * @param data      Cell data as a {@code double} value.
         * @param style     Cell style.
         * @param uri       If not null then the data is displayed as a link to this value.
         * @return this object.
         */
        public RowData addCell(String column, double data, int style, String uri) {
            return addCell(column, String.valueOf(data), style, uri);
        }
    }

    @WizField private final ArrayList<TableData.RowData> rowData;

    public TableData() {
        rowData = new ArrayList<>();
    }

    /**
     * Gets the value of the rowData property.
     * <p>
     * The {@code rowData} property is a list of inner classes that hold data for each table row.
     * @return the rowData property.
     */
    public List<TableData.RowData> getRowData() {
        return this.rowData;
    }

    /**
     * Adds an empty row descriptor to the table.
     * <p>
     * The returned object can be used to populate the row by calling its
     * {@link RowData#addCell(java.lang.String, java.lang.Object, int, java.lang.String) addCell()} method for each of the row's cells.
     * @return the added object.
     */
    public RowData addRow() {
        RowData row = new RowData();
        getRowData().add(row);
        return row;
    }
}