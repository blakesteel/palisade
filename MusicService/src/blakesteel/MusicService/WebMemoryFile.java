package blakesteel.MusicService;

import java.util.Arrays;

/**
 * @author Palisade
 */
public class WebMemoryFile {
    private boolean enabled = true;
    private byte[] bytes = null;
    private String string = null;
    
    public boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean e) {
        enabled = e;
    }
    
    public WebMemoryFile(String s) {
        string = s;
    }
    
    public WebMemoryFile(byte[] b) {
        bytes = b;
    }
    
    public boolean hasBytes() {
        return bytes != null;
    }
    
    public boolean hasString() {
        return string != null;
    }
    
    public void setBytes(byte[] b) {
        bytes = Arrays.copyOf(b, b.length);
        string = null;
    }
    
    public void setString(String s) {
        string = s;
        bytes = null;
    }
    
    public byte[] getBytes() {
        if (string != null) {
            return string.getBytes();
        }
        else if (bytes != null) {
            return bytes;
        }
        return null;
    }
    
    public String getString() {
        if (string != null) {
            return string;
        }
        else if (bytes != null) {
            return new String(bytes);
        }
        return null;
    }
}
