package blakesteel.MusicService;

import java.util.List;
import net.moraleboost.streamscraper.Stream;

/**
 * @author Palisade
 */
public class StreamInfo {
    public enum StreamType {
        Shoutcast,  // Found with Shoutcast.com search.
        Icecast     // Found with Icecast.org search.
    };
    public String Title;
    public String Host;
    public StreamType Type;
    public List<Stream> Streams;
}
