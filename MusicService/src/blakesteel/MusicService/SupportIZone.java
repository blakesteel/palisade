package blakesteel.MusicService;

import net.techguard.izone.iZone;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

/**
 * @author Palisade
 */
public class SupportIZone {
    static public iZone getPlugin(Server server)
            throws PluginUnavailableException {
        Plugin plugin = server.getPluginManager().getPlugin("iZone");
        
        if (plugin == null || !(plugin instanceof iZone)) {
            throw new PluginUnavailableException();
        }
        
        return (iZone)plugin;
    }
}
