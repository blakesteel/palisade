package blakesteel.MusicService;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * @author Palisade
 */
public class SupportWorldguard {
    static public WorldGuardPlugin getWorldGuard(Server server)
            throws PluginUnavailableException {
        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");
        
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            throw new PluginUnavailableException();
        }
        
        return (WorldGuardPlugin)plugin;
    }
    
    static public ProtectedRegion getWorldGuardRegionAt(Server server, Player player)
            throws PluginUnavailableException {
        // Get the world guard plugin.
        WorldGuardPlugin worldGuard = getWorldGuard(server);

        // Get the local player.
        LocalPlayer localPlayer = worldGuard.wrapPlayer(player);

        // Get the region manager.
        RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());

        // Get the regions at the player's location.
        ApplicableRegionSet set = regionManager.getApplicableRegions(player.getLocation());

        // The station region.
        ProtectedRegion stationRegion = null;

        // Check all overlapping regions at this location for ownership.
        for (ProtectedRegion region : set) {
            // Do we have a station region yet?
            if (stationRegion != null) {
                // Current region is a greater priority than the station region?
                if (region.getPriority() > stationRegion.getPriority()) {
                    // This region is now the station region.
                    stationRegion = region;
                }
            }
            // We did not have a station region.
            else {
                // Store the station region.
                stationRegion = region;
            }
        }

        return stationRegion;
    }
    
    static public ProtectedRegion isAdminAtWorldGuardRegionAt(Server server, Player player)
            throws PluginUnavailableException {
        // Get the world guard plugin.
        WorldGuardPlugin worldGuard = getWorldGuard(server);

        // Get the local player.
        LocalPlayer localPlayer = worldGuard.wrapPlayer(player);

        // Get the region manager.
        RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());

        // Get the regions at the player's location.
        ApplicableRegionSet set = regionManager.getApplicableRegions(player.getLocation());

        // The station region.
        ProtectedRegion stationRegion = null;

        // Check all overlapping regions at this location for ownership.
        for (ProtectedRegion region : set) {
            // Player has ownership of this region?
            if (region.isOwner(localPlayer)) {
                // Do we have a station region yet?
                if (stationRegion != null) {
                    // Current region is a greater priority than the station region?
                    if (region.getPriority() > stationRegion.getPriority()) {
                        // This region is now the station region.
                        stationRegion = region;
                    }
                }
                // We did not have a station region.
                else {
                    // Store the station region.
                    stationRegion = region;
                }
            }
        }

        return stationRegion;
    }
}
