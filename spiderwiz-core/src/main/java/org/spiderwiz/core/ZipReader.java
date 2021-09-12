package org.spiderwiz.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 *
 * @author @author  zvil
 */
final class ZipReader extends InputStream {
    private final InputStream in;
    private final Channel channel;
    private boolean decompress = false, signaled = false, aborted = false;
    private Inflater decompresser;
    private byte[] input;

    public ZipReader(InputStream in, Channel channel) {
        this.in = in;
        this.channel = channel;
    }

    private synchronized boolean isAborted() {
        return aborted;
    }

    private synchronized void setAborted(boolean aborted) {
        this.aborted = aborted;
    }

    public synchronized void startDecompression () {
        if (!decompress) {
            decompress = true;
            decompresser = new Inflater();
            input = new byte[1000];
        }
    }
    
    private synchronized boolean isDecompress() {
        return decompress;
    }
    
    private int inflate (byte[] b, int off, int len) throws IOException {
        // even if this is a compressed stream, compression will start only after the other peer signals
        // it with a value 3 byte. So if not signaled yet read byte by byte until encountering it
        int length;
        if (!signaled) {
            length = in.read(b, off, len);
            if (length < 0)
                return length;
            channel.onPhysicalRead(length);
            if (!isDecompress())
                return length;
            // look for the compression mark
            int markPos = length;
            int j = 0;
            for (int i = 0; i < length; i++) {
                byte c = b[off + i];
                if (signaled)
                    input[j++] = c;
                else if (c == CoreConsts.Zip.COMPRESSION_SIGNAL) {
                    signaled = true;
                    markPos = i;
                    if (length - i > input.length)
                        input = new byte[length - i];
                }
            }
            if (j > 0)
                decompresser.setInput(input, 0, j);
            if (markPos > 0)
                return markPos;
        }
        try {
            length = 0;
            while (length == 0) {
                if (decompresser.needsInput()) {
                    length = in.read(input, 0, 1000);
                    if (isAborted() || length < 0)
                        return -1;
                    channel.onPhysicalRead(length);
                    decompresser.setInput(input, 0, length);
                }
                length = decompresser.inflate(b, off, len);
            }
            return length;
        } catch (DataFormatException ex) {
            return -1;
        }
    }

    @Override
    public int read() throws IOException {
        byte[] buf = new byte[1];
        if (read (buf) < 0)
            return -1;
        return buf[0];
    }

    @Override
    public void close() throws IOException {
        setAborted(true);
        if (isDecompress())
            decompresser.end();
        in.close();
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return inflate(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return inflate(b, 0, b.length);
    }

}
