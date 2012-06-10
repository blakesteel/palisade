package blakesteel.MusicService;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @author Palisade
 */
public interface IWebFile {
    
    boolean isInMemory();
    
    InputStream getInputStream() throws FileNotFoundException;

    byte[] getBytes();

    boolean getEnabled();

    String getString();

    boolean hasBytes();

    boolean hasString();
    
    boolean hasInputStream();

    void setBytes(byte[] b);

    void setEnabled(boolean e);

    void setString(String s);
}
