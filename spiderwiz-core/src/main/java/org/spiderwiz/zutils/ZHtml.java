package org.spiderwiz.zutils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class for generating simple HTML documents.
<<<<<<< HEAD
  * @author @author  zvil
*/
=======
 */
>>>>>>> origin/master
public final class ZHtml {

    private ZHtml() {
    }

    /**
     *  Predefined CSS styles.
     */
    public class Styles {

        private Styles() {
        }
        
        /**
         * <em>Right align</em> style.
         */
        public final static String RIGHT_ALIGN = "text-align:right";

        /**
         * <em>Center align</em> style.
         */
        public final static String CENTER_ALIGN = "text-align:center";

        /**
         * <em>Left align</em> style.
         */
        public final static String LEFT_ALIGN = "text-align:left";
    }
    
    /**
     * Represents an HTML node.
     */
    public abstract class Node {
        private final String tag;
        private final StringBuilder styleBuilder;
        abstract void generateHTML(StringBuilder sb);

        /**
         * Constructs an HTML node object with the given tag.
         * @param tag   the tag to assign to the node.
         */
        private Node(String tag) {
            this(tag, "");
        }
        
        /**
         * Constructs an HTML node object with the given tag and an initial CSS style.
         * <p>
         * More styles can be added after construction with {@link #addStyle(java.lang.String) addStyle()}.
         * @param tag   the tag to assign to the node.
         * @param style the initial style to assign to the node.
         */
        private Node(String tag, String style) {
            styleBuilder = new StringBuilder();
            if (style != null)
                styleBuilder.append(style);
            this.tag = tag;
        }

        /**
         * Instantiates a node with the given text.
         * <p>
         * The method just instantiates the node without connecting it to any other node. This can be done with methods such as
         * {@link TextNode#addNode(org.spiderwiz.zutils.ZHtml.Node) TextNode.addNode()} etc.
         * @param text  the text contained in the returned node.
         * @return the instantiated node.
         */
        public final TextNode createTextNode(String text) {
            return new TextNode(null, text, null);
        }
        
        /**
         * Instantiates a paragraph node ({@code p} tag) with the given text.
         * <p>
         * The method just instantiates the node without connecting it to any other node. This can be done with methods such as
         * {@link TextNode#addNode(org.spiderwiz.zutils.ZHtml.Node) TextNode.addNode()} etc.
         * @param text  the text contained in the returned paragraph node.
         * @return the instantiated paragraph node.
         */
        public final Paragraph createParagraph(String text) {
            return new Paragraph(text);
        }
        
        /**
         * Instantiates an anchor node ({@code a} tag) with the given text, url ({@code ref} attribute) and
         * optional arguments to inject in the url.
         * @param text  the text displayed in the node.
         * @param ref   the url (<em>ref</em> attribute) that the node links to.
         * @param args  a variable list of parameter to inject in the url in the format <em>...?arg1&amp;arg2&amp;arg3</em> etc.
         * @return the instantiated Anchor object.
         */
        public final Anchor createAnchor(String text, String ref, String ... args) {
            return new Anchor(text, ref, args);
        }
        
        /**
         * Adds a style to the node's current list of styles.
         * @param style the style to add
         * @return  the full list of styles, concatenated by a semicolon (;).
         */
        public final String addStyle(String style) {
            if (styleBuilder.length() != 0)
                styleBuilder.append(';');
            styleBuilder.append(style);
            return styleBuilder.toString();
        }

        final String getHTML(StringBuilder sb) {
            if (tag != null) {
                sb.append("<").append(tag).append(getStyle()).append(">");
            }
            generateHTML(sb);
            if (tag != null)
                sb.append("</").append(tag).append(">\n");
            return sb.toString();
        }
        
        private String getStyle(){
            return styleBuilder.length() == 0 ? "" : " style=\"" + styleBuilder.toString() + "\"";
        }
    }
    
    /**
     * Represents the root node of an HTML document.
     * <p>
     * The root node of an HTML document is the node that is tagged {@code html}.
     */
    public class Document extends Node {
        private final Header header;
        private final Body body;
        private final StringBuilder sb;

        /**
         * Constructs a document node with the given title.
         * <p>
         * The title is the text that is tagged {@code title} in the {@code head} part of an HTML document.
         * @param title
         */
        Document(String title) {
            super("html");
            header = new Header(title);
            body = new Body();
            sb = new StringBuilder();
        }
        
        /**
         * Appends a paragraph ({@code p} tag) that wraps the given text to the document body.
         * <p>
         * The given text can contain any character, including characters that have special HTML meanings. When the document is
         * converted to HTML using {@link #toHtml()}, special characters are escaped.
         * @param text  the plain text that are wrapped by the paragraph tag.
         * @return the added Paragraph node.
         */
        public Paragraph addParagraph(String text) {
            Paragraph p = new Paragraph(text);
            body.addNode(p);
            return p;
        }
        
        /**
         * Appends a div element ({@code div} tag) to the document body.
         * @return the added Div node.
         */
        public Div addDiv() {
            Div div = new Div();
            body.addNode(div);
            return div;
        }

        /**
         * Appends a heading ({@code h} tag) wrapping the given text and having the given level to the document body.
         * <p>
         * The tag has the form of {@code <h}<em style=font-size:x-small>level</em>{@code >}.
         * <p>
         * The given text can contain any character, including characters that have special HTML meanings. The method does the
         * necessary HTML escaping.
         * @param text  the plain text wrapped by the heading tag.
         * @param level the level of the heading tag.
         * @return  the added Heading node.
         */
        public Heading addHeading(String text, int level) {
            Heading h = new Heading(level, null, text);
            body.addNode(h);
            return h;
        }
        
        /**
         * Appends a heading ({@code h} tag) wrapping the given text, having the given level and aligned to the given direction
         * to the document body.
         * <p>
         * The tag has the form of {@code <h}<em style=font-size:x-small>level</em>{@code >}.
         * <p>
         * The given text can contain any character, including characters that have special HTML meanings. The method does the
         * necessary HTML escaping.
         * <p>
         * The alignment direction is applied to {@code text-align} style attached to the node.
         * @param text  the plain text that are wrapped by the heading tag.
         * @param level the level of the heading tag.
         * @param align alignment direction - "left", "center" or "right". Null or empty values default to "left".
         * @return  the added Heading node.
         */
        public Heading addHeading(String text, int level, String align) {
            Heading h = align == null ? new Heading(level, text) : new Heading(level, align, text);
            body.addNode(h);
            return h;
        }
        
        /**
         * Appends a heading ({@code h} tag) wrapping the given node and having the given level to the document body.
         * <p>
         * The tag has the form of {@code <h}<em style=font-size:x-small>level</em>{@code >}.
         * @param node  the Node object that are wrapped by the heading tag.
         * @param level the level of the heading tag.
         * @return  the added Heading node.
         */
        public Heading addHeading(Node node, int level) {
            Heading h = new Heading(level, null, node);
            body.addNode(h);
            return h;
        }
        
        /**
         * Appends a heading ({@code h} tag) wrapping the given node, having the given level and aligned to the given direction
         * to the document body.
         * <p>
         * The tag has the form of {@code <h}<em style=font-size:x-small>level</em>{@code >}.
         * @param node  the Node object that are wrapped by the heading tag.
         * @param level the level of the heading tag.
         * @param align alignment direction - "left", "center" or "right". Null or empty value defaults to "left".
         * @return  the added Heading node.
         */
        public Heading addHeading(Node node, int level, String align) {
            Heading h = align == null ? new Heading(level, node) : new Heading(level, align, node);
            body.addNode(h);
            return h;
        }
        
        /**
         * Appends a table ({@code table} tag) with the given list of column headers and of the specified width to the document body.
         * <p>
         * The headers are placed in the table as a row ({@code tr} tag) of header cells ({@code th} tags).
         * <p>
         * The width can be any allowed value in {@code width} property of the {@code table} tag. If it is {@code null}, {@code "100%"}
         * is assumed.
         * @param headers   an array of column headers to display in the table.
         * @param width     the table width. Null value defaults to "100%".
         * @return  the added Table node.
         */
        public Table addTable(String headers[], String width) {
            Table tb = new Table(headers, width);
            body.addNode(tb);
            return tb;
        }
        
        /**
         * Returns the document with all its added nodes as an HTML text.
         * @return  the document with all its added nodes as an HTML text.
         */
        public String toHtml() {
            return getHTML(sb);
        }
        
        @Override
        void generateHTML(StringBuilder sb) {
            sb.append("\n");
            header.getHTML(sb);
            body.getHTML(sb);
        }
    }
    
    /**
     * Represents the header section ({@code header} tag) of an HTML document.
     */
    private class Header extends Node {
        private final Title title;

        /**
         * Constructs the document's head section ({@code head} tag) with the given title.
         * <p>
         * The given title are wrapped by the {@code title} tag.
         * <p>
         * The given title can contain any character, including characters that have special HTML meanings. When the document is
         * converted to HTML using {@link {@link #toHtml()}}, special characters are escaped.
         * @param title the plain text to be used as the document title.
         */
        Header(String title) {
            super("head", null);
            this.title = title == null ? null : new Title(title);
        }

        @Override
        void generateHTML(StringBuilder sb) {
            sb.append("\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n");
            if (title != null)
                title.getHTML(sb);
        }
    }
    
    /**
     * Represents a title ({@code title} tag) node.
     */
    private class Title extends Node {
        private final String text;

        /**
         * Constructs a title node with the given text.
         * <p>
         * The given text can contain any character, including characters that have special HTML meanings. When the document is
         * converted to HTML using {@link {@link #toHtml()}}, special characters are escaped.
         * @param text the plain text to be used as the document title.
         */
        public Title(String text) {
            super("title");
            this.text = text;
        }
        
        @Override
        void generateHTML(StringBuilder sb) {
            sb.append(escape(text));
        }
    }
    
    /**
     * Represents the body section ({@code <body>}) of an HTML document.
     */
    private class Body extends Node {
        private final List<Node> nodes;

        public Body() {
            super("body");
            nodes = new ArrayList<>();
        }
        
        private Node addNode(Node node) {
            nodes.add(node);
            return node;
        }

        @Override
        void generateHTML(StringBuilder sb) {
            sb.append("\n");
            for (Node node : nodes) {
                node.getHTML(sb);
            }
        }
    }
    
    /**
     * Represents a generic node that can be incrementally loaded with anything from plain text to nodes of other types.
     */
    public class TextNode extends Node {
        private String text;

        /**
         * Constructs a node with the given tag and the given initial text.
         * <p>
         * The tag can be an empty string to specify no HTML tag wrapping.
         * <p>
         * The text can contain any text, including special HTML characters. The text is HTML-escaped when the node is converted
         * to HTML. If no initial text is required use an empty string.
         * @param tag       the wrapping tag of the node, an empty string means no wrapping.
         * @param text      initial text to put in the node.
         */
        TextNode(String tag, String text) {
            super(tag);
            this.text = escape(text);
        }
        
        /**
         * Constructs a node with the given tag, the given initial text and the given initial style.
         * <p>
         * The tag must not be an empty string.
         * <p>
         * The text can contain any text, including special HTML characters. The text is HTML-escaped when the node is converted
         * to HTML. If no initial text is required use {@code null}.
         * @param tag       the wrapping tag of the node, an empty string is not allowed.
         * @param text      initial text to put in the node, null means no initial string.
         * @param style     initial style to use for the node.
         */
        TextNode(String tag, String text, String style) {
            super(tag, style);
            this.text = text == null ? "" : escape(text);
        }
        
        /**
         * Appends inner text to the node.
         * <p>
         * The text can contain any text, including special HTML characters. The text is HTML-escaped when the node is converted
         * to HTML.
         * @param text  text to append to the node. If null nothing is appended.
         * @return  the node.
         */
        public TextNode addText(String text) {
            this.text += text == null ? "" : escape(text);
            return this;
        }
        
        /**
         * Appends inner bold text ({@code b} tag) to the node.
         * <p>
         * The text can contain any text, including special HTML characters. The text is HTML-escaped when the node is converted
         * to HTML.
         * @param text  text to append to the node. If null nothing is appended.
         * @return  the node.
         */
        public TextNode addBoldText(String text) {
            this.text += text == null ? "" : bold(escape(text));
            return this;
        }
        
        /**
         * Appends an inner node to this node.
         * @param node  the node to append.
         * @return  the node.
         */
        public TextNode addNode(Node node) {
            text += node.getHTML(new StringBuilder());
            return this;
        }
        
        /**
         * Appends an inner line break ({@code <br/>}) to the node.
         * @return  the node.
         */
        public TextNode addLineBreak() {
            text += "<br/>";
            return this;
        }

        String getText() {
            return text;
        }
        
        @Override
        void generateHTML(StringBuilder sb) {
            sb.append(text);
        }
    }
    
    /**
     * Represents a text paragraph (<code>&lt;p&gt;</code>).
     */
    public class Paragraph extends TextNode {

        /**
         * Construct a paragraph node with the given initial text.
         * <p>
         * The text can contain any text, including special HTML characters. The text is HTML-escaped when the node is converted
         * to HTML.
         * @param text      initial text to put in the node.
         */
        Paragraph(String text) {
            super("p", text);
        }
    }
    
    /**
     * Represents a div element (<code>&lt;div&gt;</code>).
     */
    public class Div extends TextNode {

        /**
         * Construct an empty div element
         * <p>
         * The text can contain any text, including special HTML characters. The text is HTML-escaped when the node is converted
         * to HTML.
         * @param text      initial text to put in the node.
         */
        Div() {
            super("div", "");
        }
    }
    
    /**
     * Represents a heading tag with a given level ({@code <h}<em style=font-size:x-small>level</em>{@code >}).
     */
    public class Heading extends Node {
        private String text = null;
        private Node node = null;

        Heading(int level, String text) {
            super("h" + level);
            this.text = escape(text);
        }

        Heading(int level, String align, String text) {
            super("h" + level, align == null ? null : "text-align:" + align);
            this.text = escape(text);
        }

        Heading(int level, Node node) {
            super("h" + level);
            this.node = node;
        }

        Heading(int level, String align, Node node) {
            super("h" + level, align == null ? null : "text-align:" + align);
            this.node = node;
        }

        @Override
        void generateHTML(StringBuilder sb) {
            if (text != null)
                sb.append(text);
            if (node != null)
                node.generateHTML(sb);
        }
    }
    
    /**
     * Represents a table node ({@code <table>}).
     */
    public class Table extends Node {
        private final String headers[];
        private final ArrayList<Row> rows;

        Table(String[] headers, String width) {
            super("table", "width:" + (width == null ? "100%" : width) + "; border-spacing:3px; margin:0 auto;");
            this.headers = headers;
            rows = new ArrayList<>();
        }

        /**
         * Adds one row ({@code tr} tag) to the table.
         * @return the row node.
         */
        public Row addRow() {
            return addRow(null);
        }
        
        /**
         * Adds one row ({@code tr} tag) with the given style to the table.
         * @param style the style to apply to the row.
         * @return the row node.
         */
        public Row addRow(String style) {
            Row row = new Row(style);
            rows.add(row);
            return row;
        }
        
        @Override
        void generateHTML(StringBuilder sb) {
            sb.append("\n");
            if (headers != null) {
                sb.append("<tr>\n");
                for (String h : headers) {
                    sb.append("<th valign='top'>").append(h).append("</th>\n");
                }
                sb.append("</tr>\n");
            }
            for (Row row : rows) {
                row.getHTML(sb);
            }
        }
    }
    
    /**
     * Represents a table row node ({@code <tr>}).
     */
    public class Row extends Node {
        private final ArrayList<Cell> cells;

        Row() {
            super("tr");
            cells = new ArrayList<>();
        }
        
        Row(String style) {
            super("tr", style);
            cells = new ArrayList<>();
        }
        
        /**
         * Appends a cell node ({@code td} tag) with the given inner text to the row.
         * <p>
         * The text can contain any text, including special HTML characters. The text is HTML-escaped when the node is converted
         * to HTML.
         * @param text  the text of the cell.
         * @return the cell node.
         */
        public Row addCell(String text) {
            return addCell(text, null);
        }
        
        /**
         * Appends a cell node ({@code td} tag) with the given inner text and the given initial style to the row.
         * <p>
         * The text can contain any text, including special HTML characters. The text is HTML-escaped when the node is converted
         * to HTML.
         * @param text  the text of the cell.
         * @param style initial style to apply to the node.
         * @return the cell node.
         */
        public Row addCell(String text, String style) {
            return addCell(new TextNode(null, text, null), style);
        }
        
        /**
         * Appends a cell node ({@code td} tag) with the given inner node to the row.
         * @param node  the inner node to wrap by the cell.
         * @return the cell node.
         */
        public Row addCell(Node node) {
            return addCell(node, null);
        }

        /**
         * Appends a cell node ({@code td} tag) with the given inner node and the given initial style to the row.
         * @param node  the inner node to wrap by the cell.
         * @param style initial style to apply to the node.
         * @return the cell node.
         */
        public Row addCell(Node node, String style){
            cells.add(new Cell(node, style));
            return this;
        }
        
        @Override
        void generateHTML(StringBuilder sb) {
            sb.append("\n");
            for (Cell cell : cells) {
                cell.getHTML(sb);
            }
        }
    }
    
    /**
     * Represents a table cell node ({@code <td>}).
     */
    public class Cell extends Node {
        private final Node content;

        Cell(Node content) {
            super("td");
            this.content = content;
        }

        Cell(Node content, String style) {
            super("td", style);
            this.content = content;
        }

        @Override
        void generateHTML(StringBuilder sb) {
            content.getHTML(sb);
        }
    }
    
    /**
     * Represents an anchor node ({@code <a>}).
     */
    public class Anchor extends TextNode {
        private final String ref;
        private final String[] args;

        Anchor(String text, String ref, String[] args) {
            super(null, text);
            this.ref = ref;
            this.args = args;
        }

        @Override
        void generateHTML(StringBuilder sb) {
            sb.append("<a href=\"").append(ref);
            int count = 0;
            for (String arg : args){
                try {
                    if (arg == null)
                        continue;
                    if (count++ == 0)
                        sb.append("?");
                    else
                        sb.append("&");
                    String s[] = arg.split("=", 2);
                    sb.append(s[0]);
                    if (s.length > 1)
                        sb.append("=").append(URLEncoder.encode(s[1], "UTF-8"));
                } catch (UnsupportedEncodingException ex) {
                }
            }
            sb.append("\">").append(getText()).append("</a>");
        }
    }
    
    private Document createDocumentInstance(String title) {
        return new Document(title);
    }
    
    /**
     * Instantiates a new HTML Document node with no title.
     * @return the document node.
     */
    public static Document createDocument() {
        return createDocument(null);
    }

    /**
     * Instantiates a new HTML Document node with the given title.
     * @param title the title of the document.
     * @return the document node.
     */
    public static Document createDocument(String title) {
        return new ZHtml().createDocumentInstance(title);
    }
    
    /**
     * @param text The text to bold.
     * @return 'text' wrapped with html bold tag
     */
    private static String bold(String text) {
        return "<b>" + text + "</b>";
    }

    /**
     * Encode the give text as valid HTML visible text
     * @param s     plain text
     * @return      HTML text
     */
    public static String escape(String s) {
        StringBuilder builder = new StringBuilder();
        boolean previousWasASpace = false;
        for (char c : s.toCharArray()) {
            if (c == ' ') {
                if (previousWasASpace) {
                    builder.append("&nbsp;");
                    previousWasASpace = false;
                    continue;
                }
                previousWasASpace = true;
            } else {
                previousWasASpace = false;
            }
            switch (c) {
            case '<':
                builder.append("&lt;");
                break;
            case '>':
                builder.append("&gt;");
                break;
            case '&':
                builder.append("&amp;");
                break;
            case '"':
                builder.append("&quot;");
                break;
            case '\n':
                builder.append("<br>");
                break;
            // We need Tab support here, because we print StackTraces as HTML
            case '\t':
                builder.append("&nbsp; &nbsp; &nbsp;");
                break;
            default:
                if (c < 128) {
                    builder.append(c);
                } else {
                    builder.append("&#").append((int) c).append(";");
                }
            }
        }
        return builder.toString();
    }
    
    /**
     * Converts HTML code to plain text while preserving line breaks.
     * @param html  HTML code to convert
     * @return  the converted text.
     */
    public static String toPlainText(String html) {
        return
            /* Use image alt text. */
            html.replaceAll("<img .*?alt=[\"']?([^\"']*)[\"']?.*?/?>", "$1").

            /* Convert links to something useful */
            replaceAll("<a .*?href=[\"']?([^\"']*)[\"']?.*?>(.*)</a>", "$2 [$1]").

            /* Let's try to keep vertical whitespace intact. */
            replaceAll("<(/p|/div|/h\\d|br)\\w?/?>", "\n").

            /* Remove the rest of the tags. */
            replaceAll("<[A-Za-z/][^<>]*>", "").
        
            /* replace HTML escape characters */
            replace("&quot;", "\"").
            replace("&nbsp;", " ").
            replace("&lt;", "<").
            replace("&gt;", ">").
            replace("&amp;", "&");
    }
}
