package org.spiderwiz.core;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;

/**
 * class that implements writing to a stream with optional compression
 *
 * @author Zvi 
 */
final class ZipWriter extends OutputStream {

    private final OutputStream out;
    private boolean compress = false, signaled = false;
    private Deflater compresser;
    private byte[] output;

    public ZipWriter(OutputStream out) {
        this.out = out;
    }

    public synchronized void startCompression() {
        if (!compress) {
            compress = true;
            compresser = new Deflater();
            output = new byte[1000];
        }
    }

    private synchronized boolean isCompress() throws IOException {
        return compress;
    }

    private void deflate(boolean flush) throws IOException {
        // check if compression mode has been already signaled to the other peer, and signal it
        // (with a byte of the value 3) if not
        if (!signaled) {
            out.write(CoreConsts.Zip.COMPRESSION_SIGNAL);
//            out.write(new byte[]{0, 1, 2, 3, 4, 5, 6});
            signaled = true;
        }
        int length;
        int flushMode = flush ? Deflater.SYNC_FLUSH : Deflater.NO_FLUSH;
        while ((length = compresser.deflate(output, 0, 1000, flushMode)) > 0) {
            out.write(output, 0, length);
        }
    }

    @Override
    public void write(int b) throws IOException {
        if (isCompress()) {
            byte[] buf = new byte[1];
            buf[0] = (byte) b;
            write(buf);
        } else {
            out.write(b);
        }
    }

    @Override
    public void close() throws IOException {
        if (isCompress())
            compresser.end();
        out.close();
    }

    @Override
    public void flush() throws IOException {
        if (isCompress())
            deflate(true);
        out.flush();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (isCompress()) {
            compresser.setInput(b, off, len);
            deflate(false);
        } else {
            out.write(b, off, len);
        }
    }
}
