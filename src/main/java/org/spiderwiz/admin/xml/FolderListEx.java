package org.spiderwiz.admin.xml;

import java.util.List;

/**
 * Extends FolderList by providing methods to populate it
 * @author Zvi 
 */
public class FolderListEx extends FolderList{
    public FolderListEx addFolderEntry(String name, boolean type, String modified, int size, double download) {
        if (name == null)
            return this;
        List<FolderEntry> entries = getFolderEntry();
        FolderEntry entry = new FolderEntry();
        entry.setName(name);
        entry.setType(type);
        entry.setModified(modified);
        entry.setSize(size);
        entry.setDownload(download);
        entries.add(entry);
        return this;
    }

}
