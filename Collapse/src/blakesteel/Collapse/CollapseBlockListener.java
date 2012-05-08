package blakesteel.Collapse;

import java.util.Random;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * @author Palisade
 */
public class CollapseBlockListener implements Listener {
    public static Collapse plugin;
    private double caveinchance = 0.02D;
    private int radius = 3;
    private int maxheight = 6;

    public CollapseBlockListener(Collapse instance) {
        plugin = instance;
    }
    
    public static double doubleBetween(double start, double end) {
        Random random = new Random();

        // We need 64 bits because double have 53 bits precision, so int is too short
        // We have now a value between 0 and Long.MAX_VALUE.
        long value = -1L;
        while (value < 0)
        value = Math.abs(random.nextLong()); // Caution, Long.MIN_VALUE returns negative !

        // Cast to double
        double valueAsDouble = (double) value;

        // Scale so that Long.MAX_VALUE is exactly 1 !
        double diff = (end-start)/(double) Long.MAX_VALUE;

        return start + valueAsDouble * diff;
    }
    
    private boolean hasCollapse() {
        double n = ((caveinchance * 100));
        double r = doubleBetween(0, 100);
        boolean has = r <= n;
        Collapse.debug("r: " + r + " n: " + n + " has: " + has);
        return has;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.isCancelled()) {
            final Material mat = event.getBlock().getType();
            boolean cavedIn = false;
            
            if ((mat.equals(Material.STONE) || mat.equals(Material.DIRT) &&
                    notProtected(event.getPlayer(), event.getBlock()) &&
                    event.getPlayer() != null) &&
                    hasCollapse())
            {
                Collapse.debug("Cave unstable!");
                Location theBlock = event.getBlock().getLocation();

                for(int x = 0; x <= radius * 2; x++) {
                    for(int z = 0; z <= radius * 2; z++) {
                        int Ycount = 0;
                        if (x - radius == 0 && z - radius == 0) Ycount++;
                        
                        for(; theBlock.getBlock().getRelative(x - radius, Ycount, z - radius).getType().equals(Material.AIR) &&
                                Ycount < maxheight; Ycount++) {
                        }
                        
                        if (Ycount <= maxheight) {
                            //Collapse.debug("Cave integrity low!");

                            // Check if there are support beams (fences) nearby and abort the drop.
                            for(int i = 0; i <= Ycount; i++) {
                                int newY = i + Ycount;
                                if (theBlock.getBlock().getRelative(x - radius, newY, z - radius).getType().equals(Material.FENCE)) {
                                    Collapse.debug("Aborted drop, support beams are nearby.");
                                    event.getPlayer().sendMessage(ChatColor.DARK_GRAY + "*You hear the support beams creak.*");
                                    return;
                                }
                            }
                        }
                    }
                }
                
                for(int x = 0; x <= radius * 2; x++) {
                    for(int z = 0; z <= radius * 2; z++) {
                        int Ycount = 0;
                        if (x - radius == 0 && z - radius == 0) Ycount++;
                        
                        for(; theBlock.getBlock().getRelative(x - radius, Ycount, z - radius).getType().equals(Material.AIR) &&
                                Ycount < maxheight; Ycount++) {
                        }
                        
                        if (Ycount <= maxheight) {
                            Collapse.debug("Cave In!!!");
                            cavedIn = true;

                            for(int i = 0; i <= Ycount; i++) {
                                int newY = i + Ycount;
                                if (newY > 0 && theBlock.getBlock().getRelative(x - radius, newY, z - radius).getType().equals(Material.DIRT) || theBlock.getBlock().getRelative(x - radius, newY, z - radius).getType().equals(Material.STONE))
                                    theBlock.getBlock().getRelative(x - radius, newY, z - radius).setType(theBlock.getBlock().getRelative(x - radius, newY, z - radius).getType().equals(Material.DIRT) ? Material.SAND : Material.GRAVEL);
                            }
                        }
                    }
                }
                
                if (cavedIn) {
                    event.getPlayer().sendMessage(ChatColor.RED + "Cave In!!!");
                }
            }
        }
    }

    public boolean notProtected(Player player, Block block) {
        if (plugin == null) return true;
        
        boolean notProtected = true;
        
        Collapse collapse = (Collapse)plugin;
        
        if (collapse.isFaction(block.getLocation())) {
            Collapse.debug("faction protected");
            notProtected = false;
        }
        else if (collapse.isTownyTown(player)) {
            Collapse.debug("towny protected");
            notProtected = false;
        }
        else if (collapse.isIZone(block.getLocation())) {
            Collapse.debug("izone protected");
            notProtected = false;
        }
        else if (collapse.isRegiosRegion(player)) {
            Collapse.debug("regios protected");
            notProtected = false;
        }
        else if (collapse.isWorldGuardRegion(player)) {
            Collapse.debug("worldguard protected");
            notProtected = false;
        }
        
        return notProtected;
    }
}