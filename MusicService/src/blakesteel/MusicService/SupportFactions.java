package blakesteel.MusicService;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * @author Palisade
 */
public class SupportFactions {
    static public Plugin getFactions(Server server) throws PluginUnavailableException {
        Plugin plugin = server.getPluginManager().getPlugin("Factions");
        
        if (plugin == null) {
            throw new PluginUnavailableException();
        }
        
        return plugin;
    }
    
    static public Faction getFactionAtLocation(Player player) {
        // Get a faction located at the player's position.
        return Board.getFactionAt(new FLocation(player));
    }
    
    static public Faction getFactionAtLocation(Location location) {
        // Get a faction located at the player's position.
        return Board.getFactionAt(new FLocation(location));
    }

    static public boolean isPlayerFactionAdmin(Faction faction, Player player) {
        // Faction exists?
        if (faction != null) {
            // Get the faction's admin.
            FPlayer factionAdmin = faction.getFPlayerAdmin();
            
            if (factionAdmin != null && factionAdmin.getPlayer().equals(player)) {
                return true;
            }
        }
        return false;
    }
}
