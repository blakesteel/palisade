package blakesteel.MusicService;

import couk.Adamki11s.Regios.Main.Regios;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

/**
 * @author Palisade
 */
public class SupportRegios {
    static public Regios getPlugin(Server server)
            throws PluginUnavailableException {
        Plugin plugin = server.getPluginManager().getPlugin("Regios");
        
        if (plugin == null || !(plugin instanceof Regios)) {
            throw new PluginUnavailableException();
        }
        
        return (Regios)plugin;
    }
}
