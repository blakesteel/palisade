package blakesteel.MusicService;

import com.palmergames.bukkit.towny.Towny;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;


/**
 * @author Palisade
 */
public class SupportTowny {
    static public Plugin getPlugin(Server server)
            throws PluginUnavailableException {
        Plugin plugin = server.getPluginManager().getPlugin("Towny");
        
        if (plugin == null || !(plugin instanceof Towny)) {
            throw new PluginUnavailableException();
        }
        
        return plugin;
    }
}