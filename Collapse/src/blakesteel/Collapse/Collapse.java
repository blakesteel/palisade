package blakesteel.Collapse;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Palisade
 */
public class Collapse extends JavaPlugin {
    public static boolean DEBUG = false;
    private static final Logger logger = Logger.getLogger("Minecraft");
    private static final String LOG_PREFIX = "[Collapse] ";
    private boolean pluginEnabled = false;
    //public PluginDescriptionFile pdfFile;
    //public static PermissionHandler Permissions;
    //public static boolean permissionsEnabled = false;
    //private Map<String, FileConfiguration> configurations = new HashMap<String, FileConfiguration>();
    //private Plugin factions;
    //private Plugin regios;
    //private Plugin izone;
    //private Plugin worldguard;
    //private Plugin towny;
    
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
        // Register the music service listener for events.
        getServer().getPluginManager().registerEvents(new CollapseBlockListener(this), this);

        // Try to hook the towny listener.
        /*try {
            towny = SupportTowny.getPlugin(getServer());
            getServer().getPluginManager().registerEvents(new MusicServiceTownyListener((Towny)towny, this), this);
            addConfig("town");
            info("Hooked Towny Support");
        } catch (PluginUnavailableException ex) {
        }
        
        // Try to hook the regios support.
        try {
            regios = SupportRegios.getPlugin(getServer());
            addConfig("regios");
            info("Hooked Regios Support");
        } catch (PluginUnavailableException ex) {
        }
        
        // Try to hook the iZone support.
        try {
            izone = SupportIZone.getPlugin(getServer());
            addConfig("izone");
            info("Hooked iZone Support");
        } catch (PluginUnavailableException ex) {
        }

        try {
            // Get the factions plugin, if it exists.
            factions = SupportFactions.getPlugin(getServer());
            addConfig("faction");
            info("Hooked Factions Support");
        } catch (PluginUnavailableException ex) {
        }
        
        try {
            worldguard = SupportWorldguard.getPlugin(getServer());
            addConfig("worldguard");
            info("Hooked WorldGuard Support");
        } catch (PluginUnavailableException ex) {
        }*/

        // Creates a config.yml if there isn't yet one.
        getConfig().options().copyDefaults(true);
        saveConfig();
        
        // Default to plugin disabled.
        pluginEnabled = false;
        
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
}
