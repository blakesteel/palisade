package blakesteel.MusicService;

import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.event.TownyPlayerListener;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
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
                plugin.onTownyPlayerMoveChunk(player, fromCoord, toCoord, from, to);
            }
        } catch (NotRegisteredException e) {
            // Fail silently, this land was not a town.
        }
    }
}
