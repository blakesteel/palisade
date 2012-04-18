package blakesteel.MusicService;

import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

/**
 * @author Palisade
 */
public class MusicServiceListener implements Listener {
    private MusicService plugin;
    
    static final Logger logger = Logger.getLogger("Minecraft");
    static final String LOG_PREFIX = "[MusicService] ";
    
    public MusicServiceListener(MusicService plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (!plugin.isPluginEnabled()) return;
        
        // Get the player.
        final Player player = event.getPlayer();
        
        // We need to delay because the teleport can take a bit...
        Bukkit.getScheduler().scheduleSyncDelayedTask(
            plugin,
            new Runnable(){
                @Override
                public void run(){
                    // Update the player station at their location.
                    plugin.updatePlayerStation(player, player.getLocation());
                }
            },
            40 // 20 ticks = 1 second
        );
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerTeleported(PlayerTeleportEvent event) {
        if (!plugin.isPluginEnabled()) return;
        
        Chunk sourceChunk = event.getFrom().getChunk();
        Chunk targetChunk = event.getTo().getChunk();

        // Only bother to check for a station change when chunks have changed.
        if (sourceChunk.getX() != targetChunk.getX() ||
                sourceChunk.getZ() != targetChunk.getZ()) {
            // Update the player station at the To location.
            plugin.updatePlayerStation(event.getPlayer(), event.getTo());
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoined(PlayerJoinEvent event) {
        if (!plugin.isPluginEnabled()) return;
        
        // Update the player station.
        plugin.updatePlayerStation(event.getPlayer(), event.getPlayer().getLocation());
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.isPluginEnabled()) return;
        
        /*Chunk sourceChunk = event.getFrom().getChunk();
        Chunk targetChunk = event.getTo().getChunk();

        // Only bother to check for a station change when chunks have changed.
        if (sourceChunk.getX() != targetChunk.getX() ||
                sourceChunk.getZ() != targetChunk.getZ())*/
            // Update the player station at the To location.
            plugin.updatePlayerStation(event.getPlayer(), event.getTo());
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.isPluginEnabled() || event.getPlayer() == null) return;
        
        // Player left clicked a block?
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Get the item.
            ItemStack item = event.getItem();
            
            // Item was a stick?
            if (item != null && item.getType() == Material.STICK) {
                // Get the block that was clicked.
                Block block = event.getClickedBlock();

                // Block was iron? Start counting iron blocks for the tower...
                if (block != null && block.getType() == Material.IRON_BLOCK) {
                    // Start with the clicked block.
                    Block nextBlock = block;
                    
                    // We know we have at least 1 block.
                    int radioTowerHeight = 1;

                    // Count up to the sky.
                    do {
                        // Get the next block above us.
                        nextBlock = nextBlock.getRelative(BlockFace.UP);
                        
                        // Block is iron?
                        if (nextBlock != null && nextBlock.getType() == Material.IRON_BLOCK) {
                            // Increment the tower height.
                            radioTowerHeight++;
                        }
                    } while (nextBlock != null && nextBlock.getType() == Material.IRON_BLOCK);

                    // Start at the clicked block again.
                    nextBlock = block;

                    // Count down to the bedrock.
                    do {
                        // Get the next block below us.
                        nextBlock = nextBlock.getRelative(BlockFace.DOWN);
                        
                        // Block is iron?
                        if (nextBlock != null && nextBlock.getType() == Material.IRON_BLOCK) {
                            // Increment the tower height.
                            radioTowerHeight++;
                        }
                    } while (nextBlock != null && nextBlock.getType() == Material.IRON_BLOCK);

                    event.getPlayer().sendMessage(String.format("Radio Tower Height: %d", radioTowerHeight));

                    // Horizon calculation for radio propagation on Earth in miles.
                    // 1 block = 1 metre... for a 12 block tall antenna...
                    // 3.57 * sqrt(12) = ~12.36 km = 12366 antenna range in blocks
                    // radius in chunks = ~772
                    // eyeballed it to around 54 away, so divide by 228 instead
                    double horizonKm = 3.57 * Math.sqrt(radioTowerHeight);
                    int radius = (int)((horizonKm * 1000.0) / 228.0);

                    Location towerBase = block.getLocation();

                    plugin.getRadioTowers().put(Utility.locationToString(towerBase), radius);
                }
            }
        }
    }
}
