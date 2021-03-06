package blakesteel.MusicService;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;

/**
 * @author Palisade
 */
public class SupportShoutcast {
    
    static public int find(int index, List<StreamInfo> outStreams, CommandSender sender, String searchString)
            throws URISyntaxException, MalformedURLException {

        URI request = new URI("http", "www.shoutcast.com", "/Internet-Radio/" + searchString, null);
        List<StreamInfo> streams = search(request.toURL());
        
        if (streams != null && streams.size() > 0) {
            sender.sendMessage("[Shoutcast Results]");
            
            int i = index;
            for (StreamInfo result : streams) {
                sender.sendMessage("[" + (i++) + "] " + result.Title);
                outStreams.add(result);
            }
            return i;
        }
        return 0;
    }
    
    static public List<StreamInfo> search(URL url) {
        // List of streams.
        List<StreamInfo> streams = new ArrayList<StreamInfo>();
        
        // The search url.
        String search = url.toString();
        
        // Get the search results.
        Object[] results = Utility.httpGet(search);
        
        // Find all of the streams.
        for (Object result : results) {
            // A search result line.
            String line = (String)result;
            
            if (line.contains("no radio stations matching")) {
                return null;
            }
            
            // Valid stream .pls url?
            if (line.contains("a href") &&          // Is a link?
                line.contains("yp.shoutcast") &&    // a shoutcast .pls?
                !line.contains("ttsl.html"))        // not the ttsl.html
            {
                // Parse the .pls for the stream info.
                StreamInfo info = Utility.getShoutcastStreamInfo(line.split("\"")[1]);
                
                if (info != null) {
                    streams.add(info);
                }
            }
        }
        
        return streams;
    }
}
