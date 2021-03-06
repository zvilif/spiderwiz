package org.spiderwiz.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import org.spiderwiz.core.Channel;
import org.spiderwiz.core.Main;

/**
 * A class that implements a file channel.
 *
 * @author Zvi 
 */
public class FileChannel extends Channel{
    private File inputFile = null;
    private File outputFile = null;

    @Override
    protected boolean configure(Map<String, String> initParams, int type, int n) {
        return init(initParams.get(PluginConsts.FileChannel.INFILE), initParams.get(PluginConsts.FileChannel.OUTFILE), type, n);
    }

    /**
     * Initialize the object to read input from file and write to output file
     * @param inputPathname
     * @param outputPathname
     * @return this object
     */
    private boolean init(String inputPathname, String outputPathname, int type, int n) {
        if (inputPathname == null && outputPathname == null) {
            Main.getInstance().sendExceptionMail(null,
                String.format(PluginConsts.FileChannel.NO_FILES, PluginConsts.TYPES[type] ,n), null, true);
            return false;
        }
        inputFile = inputPathname == null ? null : new File(inputPathname);
        outputFile = outputPathname == null ? null : new File(outputPathname);
        return true;
    }
    
    @Override
    protected InputStream getInputStream() throws IOException {
        return inputFile == null ? null : new FileInputStream(inputFile);
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        outputFile.createNewFile();
        return outputFile == null ? null : new FileOutputStream(outputFile);
    }

    @Override
    public String getRemoteAddress() {
        return inputFile == null ? null : inputFile.getName();
    }

    @Override
    public boolean isFileChannel() {
        return true;
    }
}
