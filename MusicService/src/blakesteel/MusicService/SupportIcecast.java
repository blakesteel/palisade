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
public class SupportIcecast {

    static public int find(int index, List<StreamInfo> outStreams, CommandSender sender, String searchString)
            throws URISyntaxException, MalformedURLException {

        URL url = new URL("http://dir.xiph.org/search?search=" + searchString);
        List<StreamInfo> streams = search(url);
        
        if (streams != null && streams.size() > 0) {
            sender.sendMessage("[Icecast Results]");

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
            
            // No results?
            if (line.contains("Sorry, no result for your search")) {
                return null;
            }
            
            // Valid stream url?
            if (line.contains("a href") &&          // Is a link?
                line.contains("listen.xspf"))       // an xspf entry?
            {
                String entry = null;
                String[] tokens = line.split("\"");
                
                for (String token : tokens) {
                    if (token.contains("listen.xspf")) {
                        entry = token;
                        break;
                    }
                }

                if (entry != null) {
                    String link = "http://dir.xiph.org" + entry;

                    // Parse the xspf for the stream info.
                    StreamInfo info = Utility.getIcecastStreamInfo(link);

                    if (info != null) {
                        streams.add(info);
                    }
                }
                else {
                    return null;
                }
            }
        }
        
        return streams;
    }
}
