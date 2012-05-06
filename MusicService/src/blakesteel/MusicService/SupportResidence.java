package blakesteel.MusicService;

import com.bekvon.bukkit.residence.Residence;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

/**
 * @author Palisade
 */
public class SupportResidence {
    static public Residence getPlugin(Server server)
            throws PluginUnavailableException {
        Plugin plugin = server.getPluginManager().getPlugin("Residence");
        
        if (plugin == null || !(plugin instanceof Residence)) {
            throw new PluginUnavailableException();
        }
        
        return (Residence)plugin;
    }
}
