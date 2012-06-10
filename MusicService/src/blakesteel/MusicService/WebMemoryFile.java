package blakesteel.MusicService;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * @author Palisade
 */
public class WebMemoryFile implements IWebFile {
    private boolean enabled = true;
    private byte[] bytes = null;
    private String string = null;

    public WebMemoryFile(String s) {
        string = s;
    }
    
    public WebMemoryFile(byte[] b) {
        bytes = b;
    }
    
    @Override
    public boolean getEnabled() {
        return enabled;
    }
    
    @Override
    public void setEnabled(boolean e) {
        enabled = e;
    }
    
    @Override
    public boolean hasBytes() {
        return bytes != null;
    }
    
    @Override
    public boolean hasString() {
        return string != null;
    }

    @Override
    public boolean hasInputStream() {
        return false;
    }
    
    @Override
    public void setBytes(byte[] b) {
        bytes = Arrays.copyOf(b, b.length);
        string = null;
    }
    
    @Override
    public void setString(String s) {
        string = s;
        bytes = null;
    }
    
    @Override
    public byte[] getBytes() {
        if (string != null) {
            return string.getBytes();
        }
        else if (bytes != null) {
            return bytes;
        }
        return null;
    }
    
    @Override
    public String getString() {
        if (string != null) {
            return string;
        }
        else if (bytes != null) {
            return new String(bytes);
        }
        return null;
    }

    @Override
    public boolean isInMemory() {
        return true;
    }

    @Override
    public InputStream getInputStream() throws FileNotFoundException {
        throw new UnsupportedOperationException("Not supported.");
    }
}
