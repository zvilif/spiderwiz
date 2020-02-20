package org.spiderwiz.admin.xml;

import java.util.List;

/**
 * Extends PageInfo.TableInfo by providing methods to populate it.
 * @author Zvi 
 */
public class TableInfoEx extends PageInfo.TableInfo {
    public static class Style {
        public static final int NONE = 0;
        public static final int LEFT = 1;
        public static final int CENTER = 2;
        public static final int RIGHT = 4;
        public static final int RTL = 8;
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
         * Check if the text is Hebrew, if yes return Hebrew style
         * @param text
         * @return RIGHT+RTL if Hebrew, NONE otherwise
         */
        public static int styleHebrew(String text) {
            if (text == null)
                return 0;
            return text.matches(".*[\u05D0-\u05EA].*") ? RIGHT + RTL : NONE;
        }
    }
    
    public class Summary {
        public static final int NONE = 0;
        public static final int TOTAL = 1;
        public static final int MAX = 2;
    }

    /**
     * Construct a TableInfo object with initial values
     * @param title     Displayed table title (if not null)
     * @param service   Service to call on server in order to get the data that populates the table.
     * @param hideData  Indicates whether to hide the table data by default.
     */
    public TableInfoEx(String title, String service, boolean hideData) {
        setTitle(title);
        setService(service);
        setHideData(hideData);
    }
    
    /**
     * Add a column definition to the table
     * @param title     Column title
     * @param subTitle  Displayed column sub title
     * @param style     Style code as defined in TableInfoEx.Style class. Multiple styles may be combined by |.
     *                  E.g. Style.RIGHT | Style.RTL
     * @param summary   Summary code as defined in TableInfoEx.Summary class.
     * @return
     */
    public TableInfoEx addColumn(String title, String subTitle, int style, int summary) {
        List<ColumnInfo> columns = getColumnInfo();
        ColumnInfo column = new ColumnInfo();
        column.setTitle(title);
        if (subTitle != null){
            column.setSubTitle(subTitle);
        }
        if (style != 0)
            column.setStyle(Style.makeStyles(style));
        if (summary > 0)
            column.setSummary(summary);
        columns.add(column);
        return this;
    }
}
