package org.spiderwiz.admin.data;

import java.util.ArrayList;
import java.util.List;
import org.spiderwiz.annotation.WizField;
import org.spiderwiz.annotation.WizSerializable;

/**
 * Specifies the layout of an application page in <a href="http://spideradmin.com">SpiderAdmin</a>.
 * <p>
 * The exact layout of the page that SpiderAdmin displays for each application is determined by the method
 * {@link org.spiderwiz.core.Main#getPageInfo(java.lang.String) Main.getPageInfo()} that the application implements (or inherits
 * from {@link org.spiderwiz.core.Main}). The method returns an object of the class that is described here.
 * <p>
 * An application page consists of operation buttons at the top and then a list of tables. This class specifies the buttons and the
 * functions that are assigned to them, the tables and the structure of each table.
 * @see TableData
 */
@WizSerializable
public class PageInfo {

    /**
     * Specifies the label and the function of a custom button in an application page.
     */
    @WizSerializable
    public static class ButtonInfo {

        @WizField private String text;
        @WizField private String service;
        @WizField private boolean verify;

        /**
         * Gets the value of the text property.
         * <p>
         * The {@code text} property specifies the label of the button.
         * @return the value of the text property.
         */
        public String getText() {
            return text;
        }

        /**
         * Sets the value of the text property.
         * <p>
         * The {@code text} property specifies the label of the button.
         * @param value
         */
        public void setText(String value) {
            this.text = value;
        }

        /**
         * Gets the value of the service property.
         * <p>
         * The {@code service} property specifies the service code attached to the button that is used by
         * {@link org.spiderwiz.core.Main#customAdminService(java.lang.String, java.lang.String) Main.customAdminService()} to
         * determine the operation of the button.
         * @return the value of the service property.
         */
        public String getService() {
            return service;
        }

        /**
         * Sets the value of the service property.
         * <p>
         * The {@code service} property specifies the service code attached to the button that is used by
         * {@link org.spiderwiz.core.Main#customAdminService(java.lang.String, java.lang.String) Main.customAdminService()} to
         * determine the operation of the button.
         * @param value
         */
        public void setService(String value) {
            this.service = value;
        }

        /**
         * Gets the value of the verify property.
         * <p>
         * The {@code verify} property determines whether a user that clicks this button is asked to verify the operation.
         * @return the value of the verify property.
         */
        public boolean isVerify() {
            return verify;
        }

        /**
         * Sets the value of the verify property.
         * <p>
         * The {@code verify} property determines whether a user that clicks this button is asked to verify the operation.
         * @param value
         */
        public void setVerify(boolean value) {
            this.verify = value;
        }
    }

    /**
     * Specifies the structure of a table displayed in an application page.
     */
    @WizSerializable
    public static class TableInfo {

        /**
         * Specifies the parameters of a table column.
         */
        @WizSerializable
        public static class ColumnInfo {

            @WizField private String title;
            @WizField private String subTitle;
            @WizField private String style;
            @WizField private int summary;

            /**
             * Gets the value of the title property.
             * <p>
             * The {@code title} property specifies the column title that is displayed in the table header for the referenced
             * column. This value also identifies the column when {@link TableData#addRow() TableData.addRow()}
             * is used to populate the table.
             * @return the value of the title property.
             */
            public String getTitle() {
                return title;
            }

            /**
             * Sets the value of the title property.
             * <p>
             * The {@code title} property specifies the column title that is displayed in the table header for the referenced
             * column. This value also identifies the column when {@link TableData#addRow() TableData.addRow()}
             * is used to populate the table.
             * <p>
             * You should not set the property directly but rather specify a title when calling
             * {@link #addColumn(java.lang.String, java.lang.String, int, int) addColumn()}.
             * @param value
             */
            public void setTitle(String value) {
                this.title = value;
            }

            /**
             * Gets the value of the subTitle property.
             * <p>
             * The {@code subTitle} property specifies the column sub-title that is displayed in the table sub-header for the
             * referenced column. If the property value is null nothing is displayed in the table sub-header for this column.
             * @return the value of the subTitle property.
             */
            public String getSubTitle() {
                return subTitle;
            }

            /**
             * Sets the value of the subTitle property.
             * <p>
             * The {@code subTitle} property specifies the column sub-title that is displayed in the table sub-header for the
             * referenced column. If the property value is null nothing is displayed in the table sub-header for this column.
             * <p>
             * You should not set the property directly but rather specify a sub-title when calling
             * {@link #addColumn(java.lang.String, java.lang.String, int, int) addColumn()}.
             * @param value
             */
            public void setSubTitle(String value) {
                this.subTitle = value;
            }

            /**
             * Gets the value of the style property.
             * <p>
             * The {@code style} property specifies the default HTML styles used for displaying the cells in the referenced column.
             * The property can be overridden for specific cells when {@link TableData#addRow() TableData.addRow()} is used.
             * @return the value of the style property.
             */
            public String getStyle() {
                return style;
            }

            /**
             * Sets the value of the style property.
             * <p>
             * The {@code style} property specifies the default HTML styles used for displaying the cells in the referenced column.
             * The property can be overridden for specific cells when {@link TableData#addRow() TableData.addRow()} is used.
             * <p>
             * You should not set the property directly but rather specify a style bit mask when calling
             * {@link #addColumn(java.lang.String, java.lang.String, int, int) addColumn()}.
             * @param value
             */
            public void setStyle(String value) {
                this.style = value;
            }

            /**
             * Gets the value of the summary property.
             * The {@code summary} property determines the value displayed in the table summary line for the referenced column.
             * The possible values are defined in {@link Summary}.
             * @return the value of the summary property.
             */
            public int getSummary() {
                return summary;
            }

            /**
             * Sets the value of the summary property.The {@code summary} property determines the value displayed in the table summary line for the referenced column.
             * The possible values are defined in {@link Summary}.
             * <p>
             * You should not set the property directly but rather specify the summary code when calling
             * {@link #addColumn(java.lang.String, java.lang.String, int, int) addColumn()}.
             * @param value
             */
            public void setSummary(int value) {
                this.summary = value;
            }
        }

        /**
         * Helper class for managing HTML styles.
         */
        public static class Style {

            /**
             * A bit mask that represents no style.
             */
            public static final int NONE = 0;

            /**
             * A bit mask that represents the "left" style.
             */
            public static final int LEFT = 1;

            /**
             * A bit mask that represents the "center" style.
             */
            public static final int CENTER = 2;

            /**
             * A bit mask that represents the "right" style.
             */
            public static final int RIGHT = 4;

            /**
             * A bit mask that represents the "rtl" style.
             */
            public static final int RTL = 8;

            /**
             * A bit mask that represents the "alert" CSS class
             */
            public static final int ALERT = 0x10;

            private static final String styles[] = {"", "left", "center", "right", "rtl", "alert"};

            static String makeStyles(int codes) {
                String result = "";
                for (int i = 1; i < styles.length; i++) {
                    if ((codes & (1 << (i - 1))) != 0) {
                        if (result.length() > 0)
                            result += ",";
                        result += styles[i];
                    }
                }
                return result.isEmpty() ? null : result;
            }

            /**
             * If the given text is in Hebrew then return the RIGHT+RTL bit mask, otherwise return NONE.
             * @param text the given text
             * @return the RIGHT+RTL bit mask for Hebrew text, otherwise return NONE
             */
            public static int styleHebrew(String text) {
                if (text == null)
                    return 0;
                return text.matches(".*[\u05D0-\u05EA].*") ? RIGHT + RTL : NONE;
            }
        }

        /**
         * Defines codes that determine the value in the summary line for a specific table column.
         */
        public class Summary {

            /**
             * Specifies that no value shall be displayed in the summary line for the referenced column.
             */
            public static final int NONE = 0;

            /**
             * Specifies that the summary line for the referenced column shall contain the summary of all column values.
             */
            public static final int TOTAL = 1;

            /**
             * Specifies that the summary line for the referenced column shall contain the maximum of all the values in the column.
             */
            public static final int MAX = 2;
        }

        @WizField private String title;
        @WizField private String service;
        @WizField private boolean hideData;
        @WizField private final ArrayList<PageInfo.TableInfo.ColumnInfo> columnInfo;

        /**
         * Constructs an empty object.
         */
        public TableInfo() {
            columnInfo = new ArrayList<>();
        }

        /**
         * Constructs an object with initial values.
         * <p>
         * The following values are set by this constructor:
         * <ul>
         * <li>{@code title}: The title that SpiderAdmin displays above the table.</li>
         * <li>{@code service}: The service code that is used by
         * {@link org.spiderwiz.core.Main#customAdminService(java.lang.String, java.lang.String) Main.customAdminService()}
         * to determine the data displayed in the table.</li>
         * <li>{@code hideData}: Specifies whether SpiderAdmin shall initially hide the table when displaying the page. The table can
         * be revealed by the user by clicking the drop-down icon near the table title (which is not hidden if the table
         * contains data).</li>
         * </ul>
         * 
         * @param title     The displayed table title (if not null).
         * @param service   The service code used by the application to populate the table.
         * @param hideData  If true then the table is initially hidden.
         */
        public TableInfo(String title, String service, boolean hideData) {
            this();
            this.title = title;
            this.service = service;
            this.hideData = hideData;
        }

        /**
         * Gets the value of the title property.
         * <p>
         * The {@code title} property is the title that SpiderAdmin displays above the table.
         * @return the value of the title property.
         */
        public String getTitle() {
            return title;
        }

        /**
         * Sets the value of the title property.
         * <p>
         * The {@code title} property is the title that SpiderAdmin displays above the table.
         * You should not set it directly but rather specify when activating the class constructor.
         * @param value
         */
        public void setTitle(String value) {
            this.title = value;
        }

        /**
         * Gets the value of the service property.
         * <p>
         * The {@code service} property is the service code that is used by
         * {@link org.spiderwiz.core.Main#customAdminService(java.lang.String, java.lang.String) Main.customAdminService()}
         * to determine the data displayed in the table.
         * @return the value of the service property.
         */
        public String getService() {
            return service;
        }

        /**
         * Sets the value of the service property.
         * <p>
         * The {@code service} property is the service code that is used by
         * {@link org.spiderwiz.core.Main#customAdminService(java.lang.String, java.lang.String) Main.customAdminService()}
         * to determine the data displayed in the table.
         * <p>
         * You should not set it directly but rather specify when activating the class constructor.
         * @param value
         */
        public void setService(String value) {
            this.service = value;
        }

        /**
         * Gets the value of the hideData property.
         * <p>
         * The {@code hideData} property specifies whether SpiderAdmin shall initially hide the table when displaying the page.
         * The table can be revealed by the user by clicking the drop-down icon near the table title (which is not hidden if the table
         * contains data).
         * @return the value of the hideData property.
         */
        public boolean isHideData() {
            return hideData;
        }

        /**
         * Sets the value of the hideData property.
         * <p>
         * The {@code hideData} property specifies whether SpiderAdmin shall initially hide the table when displaying the page.
         * The table can be revealed by the user by clicking the drop-down icon near the table title (which is not hidden if the table
         * contains data).
         * <p>
         * You should not set it directly but rather specify when activating the class constructor.
         * @param value
         */
        public void setHideData(boolean value) {
            this.hideData = value;
        }

        /**
         * Gets the value of the columnInfo property.
         * <p>
         * The {@code columnInfo} property lists all the column descriptors of the table.
         * @return the value of the columnInfo property.
         */
        public List<PageInfo.TableInfo.ColumnInfo> getColumnInfo() {
            return this.columnInfo;
        }

        /**
         * Add a column definition to the table specifying the following parameters:
         * <ul>
         * <li>{@code title}: The column title that is displayed in the table header for the referenced column.
         * This value also identifies the column when {@link TableData#addRow() TableData.addRow()} is used to populate the table.</li>
         * <li>{@code subTitle}: The column subtitle that is displayed in the table sub-header for the
         * referenced column. If the property value is null nothing is displayed in the table sub-header for this column.</li>
         * <li>{@code style}: A bit combination of {@link Style} values that specify the default HTML styles used for displaying
         * the cells in the column. The property can be overridden for specific cells when
         * {@link TableData#addRow() TableData.addRow()} is used.</li>
         * <li>{@code summary}: Determines the value displayed in the table summary line for the column. The possible values are
         * defined in {@link Summary}.
         * </ul>
         * 
         * @param title     Column title
         * @param subTitle  Column sub-title
         * @param style     Default styles for the column's cells.
         * @param summary   The value to be displayed in the table's summary line.
         * @return this object.
         */
        public TableInfo addColumn(String title, String subTitle, int style, int summary) {
            ColumnInfo column = new ColumnInfo();
            column.setTitle(title);
            if (subTitle != null){
                column.setSubTitle(subTitle);
            }
            if (style != 0)
                column.setStyle(Style.makeStyles(style));
            if (summary > 0)
                column.setSummary(summary);
            columnInfo.add(column);
            return this;
        }
    }

    @WizField private final ArrayList<PageInfo.ButtonInfo> buttonInfo;
    @WizField private final ArrayList<PageInfo.TableInfo> tableInfo;
    @WizField private Boolean showReloadSettings;
    @WizField private Boolean showUpdateSettings;
    @WizField private Boolean showFlushLogs;
    @WizField private boolean externalAddress;

    /**
     * Constructs a default object.
     */
    public PageInfo() {
        this(true, true);
    }

    /**
     * Constructs an object with initial values.
     *
     * @param showUpdateSettings true if the page shall show the "Update Configuration" button.
     * @param showFlushLogs true if the page shall show the "Flush Logs" button.
     */
    public PageInfo(Boolean showUpdateSettings, Boolean showFlushLogs) {
        this.showUpdateSettings = showUpdateSettings;
        this.showFlushLogs = showFlushLogs;
        this.showReloadSettings = false;
        buttonInfo = new ArrayList<>();
        tableInfo = new ArrayList<>();
    }

    /**
     * Gets the value of the buttonInfo property.
     * <p>
     * The {@code buttonInfo} property is a list of descriptors for all the custom buttons in the page.
     * @return the value of the buttonInfo property.
     */
    public List<PageInfo.ButtonInfo> getButtonInfo() {
        return buttonInfo;
    }

    /**
     * Gets the value of the tableInfo property.
     * <p>
     * The {@code tableInfo} property is a list of descriptors for all the tables in the page.
     * @return the value of the tableInfo property.
     */
    public List<PageInfo.TableInfo> getTableInfo() {
        return this.tableInfo;
    }

    /**
     * Gets the value of the showReloadSettings property.
     * <p>
     * The {@code showReloadSettings} property specifies whether to show the "Reload Configuration" button at the top of the page.
     * When this button is clicked, application configuration is loaded from the configuration file assuming that it was edited
     * manually.
     * @return the value of the showReloadSettings property.
     */
    public Boolean isShowReloadSettings() {
        return showReloadSettings;
    }

    /**
     * Sets the value of the showReloadSettings property.
     * The {@code showReloadSettings} property specifies whether to show the "Reload Configuration" button at the top of the page.
     * When this button is clicked, application configuration is loaded from the configuration file assuming that it was edited
     * manually.
     * @param value
     */
    public void setShowReloadSettings(Boolean value) {
        this.showReloadSettings = value;
    }

    /**
     * Gets the value of the showUpdateSettings property.
     * <p>
     * The {@code showUpdateSettings} property specifies whether to show the "Update Configuration" button at the top of the page.
     * @return the value of the showUpdateSettings property.
     */
    public Boolean isShowUpdateSettings() {
        return showUpdateSettings;
    }

    /**
     * Sets the value of the showUpdateSettings property.
     * <p>
     * The {@code showUpdateSettings} property specifies whether to show the "Update Configuration" button at the top of the page.
     * @param value
     */
    public void setShowUpdateSettings(Boolean value) {
        this.showUpdateSettings = value;
    }

    /**
     * Gets the value of the showFlushLogs property.
     * <p>
     * The {@code showFlushLogs} property specifies whether to show the "Flush Logs" button at the top of the page.
     * @return the value of the showFlushLogs property.
     */
    public Boolean isShowFlushLogs() {
        return showFlushLogs;
    }

    /**
     * Sets the value of the showFlushLogs property.
     * <p>
     * The {@code showFlushLogs} property specifies whether to show the "Flush Logs" button at the top of the page.
     * @param value
     */
    public void setShowFlushLogs(Boolean value) {
        this.showFlushLogs = value;
    }

    /**
     * Gets the value of the externalAddress property.
     * <p>
     * The {@code externalAddress} property informs that SpiderAdmin has been activated from an external IP address
     * and therefore the "Update Configuration" button is replaced by "Show Configuration" and the "Maintenance" button is not
     * shown at all.
     * @return the value of the externalAddress property.
     */
    public boolean isExternalAddress() {
        return externalAddress;
    }

    /**
     * Sets the value of the externalAddress property.
     * <p>
     * The {@code externalAddress} property informs that SpiderAdmin has been activated from an external IP address
     * and therefore the "Update Configuration" button is replaced by "Show Configuration" and the "Maintenance" button is not
     * shown at all.
     * @param value
     */
    public void setExternalAddress(boolean value) {
        this.externalAddress = value;
    }

    /**
     * Adds a custom button to the page layout.
     * <p>
     * The method arguments specify the attributes of the button as follows:
     * <ul>
     * <li>{@code text}: The label of the button.</li>
     * <li>{@code service}: The service code attached to the button that is used by
     * {@link org.spiderwiz.core.Main#customAdminService(java.lang.String, java.lang.String) Main.customAdminService()} to
     * determine the operation of the button.</li>
     * <li>{@code verify}: Determines whether a user that clicks this button is asked to verify the operation.</li>
     * </ul>
     *
     * @param text      Button's label.
     * @param service   Button's service code.
     * @param verify    true when the browser shall verify operation after button click,
     * @return this object
     */
    public PageInfo addButton(String text, String service, boolean verify) {
        ButtonInfo grButton = new ButtonInfo();
        grButton.setText(text);
        grButton.setService(service);
        grButton.setVerify(verify);
        buttonInfo.add(grButton);
        return this;
    }

    /**
     * Adds a table descriptor to the page layout.
     *
     * @param tableInfo     specifies the table layout.
     * @return this object
     */
    public PageInfo addTable(TableInfo tableInfo) {
        this.tableInfo.add(tableInfo);
        return this;
    }
}