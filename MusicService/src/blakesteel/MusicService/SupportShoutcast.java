package blakesteel.MusicService;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;

/**
 * @author Palisade
 */
public class SupportShoutcast {
    static final String shoutcastSearchUrl = "http://www.shoutcast.com/Internet-Radio/%s";

    static public List<StreamInfo> find(CommandSender sender, URL request) {
        List<StreamInfo> streams = search(request);
        if (streams != null && streams.size() > 0) {
            int i = 0;
            for (StreamInfo result : streams) {
                sender.sendMessage("[" + (i++) + "] " + result.Title);
            }
        }
        return streams;
    }
    
    static public List<StreamInfo> search(URL url) {
        // List of streams.
        List<StreamInfo> streams = new ArrayList<StreamInfo>();
        
        // The search url.
        //String search = String.format(shoutcastSearchUrl, searchString);
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
                StreamInfo info = Utility.getStreamInfo(line.split("\"")[1]);
                
                if (info != null) {
                    streams.add(info);
                }
            }
        }
        
        return streams;
    }
    
    /*static public List<StreamInfo> find(CommandSender sender, String searchString) {
        List<StreamInfo> streams = search(searchString);
        if (streams.size() > 0) {
            int i = 0;
            for (StreamInfo result : streams) {
                sender.sendMessage("[" + (i++) + "] " + result.Title);
            }
        }
        return streams;
    }
    
    static public List search(String searchString) {
        // List of streams.
        List streams = new ArrayList();
        
        // The search url.
        String search = String.format(shoutcastSearchUrl, searchString);
        
        // Get the search results.
        Object[] results = Utility.httpGet(search);
        
        // Find all of the streams.
        for (Object result : results) {
            // A search result line.
            String line = (String)result;
            
            // Valid stream .pls url?
            if (line.contains("a href") &&          // Is a link?
                line.contains("yp.shoutcast") &&    // a shoutcast .pls?
                !line.contains("ttsl.html"))        // not the ttsl.html
            {
                
                // Parse the .pls for the stream info.
                StreamInfo info = Utility.getStreamInfo(line.split("\"")[1]);
                
                if (info != null) {
                    streams.add(info);
                }
            }
        }
        
        return streams;
    }*/
}
