package blakesteel.MusicService;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.listeners.TownyPlayerListener;
import com.palmergames.bukkit.towny.object.*;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * The MusicService plugin's Towny Listener for tracking if a player is in a town.
 * 
 * @author Palisade
 */
public class MusicServiceTownyListener extends TownyPlayerListener {
    private MusicService plugin;
    
    static final Logger logger = Logger.getLogger("Minecraft");
    static final String LOG_PREFIX = "[MusicService] ";
    
    public MusicServiceTownyListener(Towny towny, MusicService plugin) {
        super(towny);
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getBlockX() == to.getBlockX() &&
            from.getBlockY() == to.getBlockY() &&
            from.getBlockZ() == to.getBlockZ())
                return;
        
        try {
            
            TownyWorld fromWorld = TownyUniverse.getDataSource().getWorld(from.getWorld().getName());
            WorldCoord fromCoord = new WorldCoord(fromWorld, Coord.parseCoord(from));
            TownyWorld toWorld = TownyUniverse.getDataSource().getWorld(to.getWorld().getName());
            WorldCoord toCoord = new WorldCoord(toWorld, Coord.parseCoord(to));

            if (!fromCoord.equals(toCoord)) {
                onTownyPlayerMoveChunk(player, fromCoord, toCoord, from, to);
            }
        } catch (NotRegisteredException e) {
            // Fail silently, this land was not a town.
        }
    }
    
    private void onTownyPlayerMoveChunk(Player player, WorldCoord from, WorldCoord to,
                                  Location fromLoc, Location toLoc) {
        TownBlock block;
        try {
            block = to.getWorld().getTownBlock(to.getX(), to.getZ());
        
            if (block != null && block.hasTown()) {
                Town town = block.getTown();
                String townName = town.getName();
                
                //debug("TOWN: " + townName);
                plugin.playerNowStandingInTown(player, townName);
                plugin.handleTowny(player);
            }
            else {
                plugin.playerNowStandingInTown(player, null);
            }
        } catch (NotRegisteredException ex) {
            plugin.playerNowStandingInTown(player, null);
        }
    }
}
