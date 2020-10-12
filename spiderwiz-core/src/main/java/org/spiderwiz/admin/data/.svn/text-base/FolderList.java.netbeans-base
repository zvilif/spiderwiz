package org.spiderwiz.admin.data;

import java.util.ArrayList;
import java.util.List;
import org.spiderwiz.annotation.WizField;
import org.spiderwiz.annotation.WizSerializable;

/**
 * Holds data that describes file folder content for the use of <a href="http://spideradmin.com">SpiderAdmin</a>.
 * <p>
 * The class is used internally by the SpiderAdmin agent and is rarely accessed explicitly. It is documented here for those
 * rare cases.
 */
@WizSerializable
public class FolderList {

    /**
     * Holds data that describes one entry in a file folder content list.
     */
    @WizSerializable
    public static class FolderEntry {
        @WizField private String name;
        @WizField private boolean type;
        @WizField private String modified;
        @WizField private int size;
        @WizField private double download;

        /**
         * Gets the value of the name property.
         * <p>
         * The {@code name} property holds the name of the file or sub-folder that is described by the entry.
         * @return the name property
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the value of the name property.
         * <p>
         * The {@code name} property holds the name of the file or sub-folder that is described by the entry.
         * @param value
         */
        public void setName(String value) {
            this.name = value;
        }

        /**
         * Gets the value of the type property.
         * <p>
         * The {@code type} property is {@code true} for a sub-folder and {@code false} for a file.
         * @return the type property
         */
        public boolean isType() {
            return type;
        }

        /**
         * Sets the value of the type property.
         * <p>
         * The {@code type} property is {@code true} for a sub-folder and {@code false} for a file.
         * @param value
         */
        public void setType(boolean value) {
            this.type = value;
        }

        /**
         * Gets the value of the modified property.
         * <p>
         * The {@code modified} property holds a String that represents the date and time the file or sub-folder described by
         * the entry was last modified.
         * @return the modified property
         */
        public String getModified() {
            return modified;
        }

        /**
         * Sets the value of the modified property.
         * <p>
         * The {@code modified} property holds a String that represents the date and time the file or sub-folder described by
         * the entry was last modified.
         * @param value
         */
        public void setModified(String value) {
            this.modified = value;
        }

        /**
         * Gets the value of the size property.
         * <p>
         * The {@code size} property holds the size of the file or sub-folder that is described by the entry in bytes.
         * @return the size property
         */
        public int getSize() {
            return size;
        }

        /**
         * Sets the value of the size property.
         * <p>
         * The {@code size} property holds the size of the file or sub-folder that is described by the entry in bytes.
         * @param value
         */
        public void setSize(int value) {
            this.size = value;
        }

        /**
         * Gets the value of the download property.
         * <p>
         * The {@code download} property holds the estimated time for downloading the file described by the entry in seconds.
         * @return the download property
         */
        public double getDownload() {
            return download;
        }

        /**
         * Sets the value of the download property.
         * <p>
         * The {@code download} property holds the estimated time for downloading the file described by the entry in seconds.
         * @param value
         */
        public void setDownload(double value) {
            this.download = value;
        }
    }

    @WizField private final ArrayList<FolderList.FolderEntry> folderEntry;

    public FolderList() {
        folderEntry = new ArrayList<>();
    }

    /**
     * Gets the value of the folderEntry property.
     * <p>
     * The {@code folderEntry} property is a list of entries that describes all the files and sub-folders in this folder.
     * @return the folderEntry property
     */
    public List<FolderList.FolderEntry> getFolderEntry() {
        return folderEntry;
    }

    /**
     * Adds one entry to the list of entries that describes all the files and sub-folders in this folder.
     * @param name          file or sub-folder name.
     * @param type          true if this is a sub-folder, false if this is a file.
     * @param modified      a String representing the date and time the file or sub-folder was last modified.
     * @param size          size of the file in bytes. 
     * @param download      the estimated download time in seconds of the file. Zero in case this is a sub-folder entry.
     * @return this object.
     */
    public FolderList addFolderEntry(String name, boolean type, String modified, int size, double download) {
        if (name == null)
            return this;
        FolderEntry entry = new FolderEntry();
        entry.setName(name);
        entry.setType(type);
        entry.setModified(modified);
        entry.setSize(size);
        entry.setDownload(download);
        folderEntry.add(entry);
        return this;
    }
}