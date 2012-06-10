package blakesteel.MusicService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @author Palisade
 */
public class WebFile implements IWebFile {

    private boolean enabled = true;
    private File file;
    
    public WebFile(File file) throws FileNotFoundException {
        this.file = file;
    }
    
    @Override
    public boolean isInMemory() {
        return false;
    }

    @Override
    public InputStream getInputStream() throws FileNotFoundException {
        return new FileInputStream(file);
    }

    @Override
    public byte[] getBytes() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean getEnabled() {
        return enabled;
    }

    @Override
    public String getString() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean hasBytes() {
        return false;
    }

    @Override
    public boolean hasString() {
        return false;
    }
    
    @Override
    public boolean hasInputStream() {
        return true;
    }

    @Override
    public void setBytes(byte[] b) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void setEnabled(boolean e) {
        enabled = e;
    }

    @Override
    public void setString(String s) {
        throw new UnsupportedOperationException("Not supported.");
    }
}
