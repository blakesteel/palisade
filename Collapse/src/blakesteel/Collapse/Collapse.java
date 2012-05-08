package blakesteel.Collapse;

import com.massivecraft.factions.Faction;
import com.palmergames.bukkit.towny.Towny;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import couk.Adamki11s.Regios.Regions.GlobalRegionManager;
import couk.Adamki11s.Regios.Regions.Region;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.techguard.izone.managers.ZoneManager;
import net.techguard.izone.zones.Zone;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Palisade
 */
public class Collapse extends JavaPlugin {
    public static boolean DEBUG = false;
    private static final Logger logger = Logger.getLogger("Minecraft");
    private static final String LOG_PREFIX = "[Collapse] ";
    private boolean pluginEnabled = false;
    private Map<String, String> playerStandingInTown = new HashMap<String, String>();
    private Plugin factions;
    private Plugin regios;
    private Plugin izone;
    private Plugin worldguard;
    private Plugin towny;
    
    //
    // Properties
    //
    
    public boolean isPluginEnabled() {
        return pluginEnabled;
    }
    
    //
    // Bukkit Plugin Events
    //

    @Override
    public void onEnable() {
        // Default to plugin disabled.
        pluginEnabled = false;

        // Register the music service listener for events.
        getServer().getPluginManager().registerEvents(new CollapseBlockListener(this), this);

        // Try to hook the towny listener.
        try {
            towny = SupportTowny.getPlugin(getServer());
            getServer().getPluginManager().registerEvents(new CollapseTownyListener((Towny)towny, this), this);
            info("Hooked Towny Support");
        } catch (PluginUnavailableException ex) {
        }
        
        // Try to hook the regios support.
        try {
            regios = SupportRegios.getPlugin(getServer());
            info("Hooked Regios Support");
        } catch (PluginUnavailableException ex) {
        }
        
        // Try to hook the iZone support.
        try {
            izone = SupportIZone.getPlugin(getServer());
            info("Hooked iZone Support");
        } catch (PluginUnavailableException ex) {
        }

        try {
            // Get the factions plugin, if it exists.
            factions = SupportFactions.getPlugin(getServer());
            info("Hooked Factions Support");
        } catch (PluginUnavailableException ex) {
        }
        
        try {
            worldguard = SupportWorldguard.getPlugin(getServer());
            info("Hooked WorldGuard Support");
        } catch (PluginUnavailableException ex) {
        }

        // Creates a config.yml if there isn't yet one.
        getConfig().options().copyDefaults(true);
        saveConfig();
        
        // Plugin is ready at this point.
        pluginEnabled = true;
        
        info(pluginEnabled ? "Enabled" : "Disabled");
    }
    
    @Override
    public void onDisable() {
        // Plugin disabled.
        pluginEnabled = false;

        info("Disabled.");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        // Plugin not enabled? Return.
        if (!isPluginEnabled()) return false;

        // Player.
        final Player player;

        // Is a player? Store player variable.
        if (sender instanceof Player) {
            player = (Player)sender;
        }
        // Is server console?
        else {
            sender.sendMessage("Collapse: Command can only be run by a player.");
            return false;
        }
        if (cmd.getName().equalsIgnoreCase("Collapse")) {
            if (args.length > 0) {
                // Removing first parameter (command)
                String[] cmdArgs = new String[args.length - 1];
                System.arraycopy(args, 1, cmdArgs, 0, args.length - 1);
                
                if (args[0].equalsIgnoreCase("something")) {
                    if (player.hasPermission("collapse.something"))
                        return commandSomething(player, cmdArgs);
                }
                // It might be a known command but you may not have permission, so we need an else here.
                else {
                    sendMessage(player, "Unknown command...");
                }
                return commandHelp(player);
            } else {
                return commandHelp(player);
            }
        }
        return false;
    }
    
    private boolean commandSomething(Player player, String[] args) {
        if (args.length > 0) {
            return true;
        }
        sendMessage(player, "Type /cl something <data>");
        return false;
    }
    
    private boolean commandHelp(Player player) {
        sendMessage(player, "Commands:");
        if (player.hasPermission("collapse.something"))
            sendMessage(player, "/cl something");
        return true;
    }
    
    private void sendMessage(Player player, String message) {
        if (player != null) {
            player.sendMessage("Collapse: " + message);
        }
    }
    
    //
    // Logging Support
    //
    
    public static void debug(String msg) {
        if (DEBUG) {
            logger.log(Level.INFO, String.format("%s(DEBUG) %s", LOG_PREFIX, msg));
        }
    }

    public static void info(String msg) {
        logger.log(Level.INFO, String.format("%s %s", LOG_PREFIX, msg));
    }
    
    public static void severe(String msg) {
        logger.log(Level.SEVERE, String.format("%s %s", LOG_PREFIX, msg));
    }
    
    //
    // Factions Support
    //
    
    public boolean isFaction(Location location) {
        if (!pluginEnabled || factions == null) return false;
        
        // Faction exists?
        if (factions.isEnabled()) {
            try {
                Faction faction = SupportFactions.getFactionAtLocation(getServer(), location);
                
                if (faction != null) {
                    // Get the faction region name.
                    String regionName = faction.getTag();

                    if (regionName != null) {
                        return true;
                    }
                }
            } catch (Exception ex) {
                // Fail silently. Might mean Factions not installed, or no faction here.
            }
        }
        
        return false;
    }
    
    //
    // iZone Support
    //
    
    public boolean isIZone(Location location) {
        if (!pluginEnabled || izone == null) return false;

        try {
            Zone zone = ZoneManager.getZone(location);

            // Has iZone zone here?
            if (zone != null) {
                return true;
            }
        }
        catch (Exception ex) {
            // Fail silently. Might mean iZone not installed, or no zone here.
        }

        return false;
    }
    
    //
    // Regios Support
    //
    
    public boolean isRegiosRegion(Player player) {
        if (!pluginEnabled || regios == null) return false;

        try {
            // Has player?
            if (player != null) {
                Region region = GlobalRegionManager.getRegion(player);

                // Has regios region here?
                if (region != null) {
                    return true;
                }
            }
        }
        catch (Exception ex) {
            // Fail silently. Might mean regios not installed, or no region here.
        }

        return false;
    }
    
    //
    // WorldGuard Regions Support
    //

    public boolean isWorldGuardRegion(Player player) {
        if (!pluginEnabled || worldguard == null) return false;

        // Has player?
        if (player != null) {
            try {
                ProtectedRegion stationRegion = SupportWorldguard.getWorldGuardRegionAt(getServer(), player);
                
                if (stationRegion != null) {
                    return true;
                }
            }
            catch (Exception ex) {
                // Fail silently. Might mean worldguard not installed, or no region here.
            }
        }

        return false;
    }
    
    //
    // Towny Support
    //
    
    public boolean isTownyTown(Player player) {
        if (!pluginEnabled || towny == null) return false;

        try {
            // Has player?
            if (player != null) {
                String name = getTownPlayerIsStandingIn(player);

                // Has town here?
                if (name != null) {
                    return true;
                }
            }
        }
        catch (Exception ex) {
            // Fail silently. Might mean towny not installed, or no town here.
        }

        return false;
    }
    
    public void playerNowStandingInTown(Player player, String name) {
        if (player != null) {
            playerStandingInTown.put(player.getName(), name);
        }
    }
    
    public String getTownPlayerIsStandingIn(Player player) {
        if (player != null) {
            if (playerStandingInTown.containsKey(player.getName())) {
                return playerStandingInTown.get(player.getName());
            }
        }
        return null;
    }
}
