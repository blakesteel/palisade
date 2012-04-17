package blakesteel.MusicService;

import com.massivecraft.factions.Faction;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import couk.Adamki11s.Regios.Regions.GlobalRegionManager;
import couk.Adamki11s.Regios.Regions.Region;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.techguard.izone.managers.ZoneManager;
import net.techguard.izone.zones.Zone;
import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Music Service plugin for Bukkit
 * 
 * @author Palisade
 */
public class MusicService extends JavaPlugin {
    public static boolean DEBUG = false;
    private static final Logger logger = Logger.getLogger("Minecraft");
    private static final String LOG_PREFIX = "[MusicService] ";
    
    private boolean pluginEnabled = false;
    
    private Map<String, Integer> radioTowers = new HashMap<String, Integer>();
    private Map<String, String> radioStations = new HashMap<String, String>();
    private Map<String, String> playerStations = new HashMap<String, String>();
    private Map<String, List<StreamInfo>> playerSearches = new HashMap<String, List<StreamInfo>>();
    private Map<String, String> playerStandingInTown = new HashMap<String, String>();
    private Map<String, String> playerStandingInRegios = new HashMap<String, String>();
    
    private String refreshUrl;
    private String relativeUrl;
    
    private String refresh = "<meta HTTP-EQUIV=\"REFRESH\" content=\"1; url=#REFRESH" +
                             "music/#USER.html\">There is currently no music station available at this position.";
    
    private File wwwRootDirectory;
    private File musicDirectory;
    
    private Plugin factions;
    private Plugin regios;
    private Plugin izone;
    
    private boolean useExternalWebServer = true;
    
    private WebServer webserver;
    private int wwwPort = 8888;
    
    private String pluginDir;
    
    //
    // Properties
    //
    
    public boolean isPluginEnabled() {
        return pluginEnabled;
    }
    
    public Map<String, Integer> getRadioTowers() {
        return radioTowers;
    }
    
    //
    // Bukkit Plugin Events
    //
    
    @Override
    public void onEnable() {
        pluginDir = new File(new File(new File(".").getAbsolutePath(), "plugins"), "MusicService").toString();
        info("pluginDir: " + pluginDir);

        // Register the music service listener for events.
        getServer().getPluginManager().registerEvents(new MusicServiceListener(this), this);

        // Try to hook the towny listener.
        try {
            Towny towny = SupportTowny.getTowny(getServer());
            getServer().getPluginManager().registerEvents(new MusicServiceTownyListener(towny, this), this);
            info("Hooked Towny Support");
        } catch (PluginUnavailableException ex) {
            info("Skipped Towny Support");
        }
        
        // Try to hook the regios support.
        try {
            regios = SupportRegios.getRegios(getServer());
            info("Hooked Regios Support");
        } catch (PluginUnavailableException ex) {
            info("Skipped Regios Support");
        }
        
        // Try to hook the iZone support.
        try {
            izone = SupportIZone.getIZone(getServer());
            info("Hooked iZone Support");
        } catch (PluginUnavailableException ex) {
            info("Skipped iZone Support");
        }

        // Creates a config.yml if there isn't yet one.
        getConfig().options().copyDefaults(false);
        saveConfig();
        
        // Default to plugin disabled.
        pluginEnabled = false;
        
        info("Loading configuration....");
        
        // Is the wwwRoot configured?
        if (getConfig().contains("wwwRoot")) {
            // Establish paths to the www root and music directory.
            wwwRootDirectory = new File(getConfig().getString("wwwRoot"));
            musicDirectory = new File(wwwRootDirectory, "music");
            
            info("Web Directory: " + wwwRootDirectory.getPath());

            // Is the refreshUrl configured?
            if (getConfig().contains("refreshUrl")) {
                // Get the refresh URL.
                refreshUrl = getConfig().getString("refreshUrl");
                
                info("Refresh URL: " + refreshUrl);

                // Is the relativeUrl configured? This is optional.
                if (getConfig().contains("relativeUrl")) {
                    // Get the relative URL.
                    relativeUrl = getConfig().getString("relativeUrl");
                    
                    info("Relative URL: " + relativeUrl);
                }
                // Not relative, set to "".
                else {
                    relativeUrl = "";
                }
                
                if (getConfig().contains("useExternalWebserver")) {
                    useExternalWebServer = getConfig().getBoolean("useExternalWebserver");

                    info("External Webserver: " + useExternalWebServer);
                }
                else {
                    useExternalWebServer = false;

                    if (getConfig().contains("wwwPort")) {
                        wwwPort = getConfig().getInt("wwwPort");
                    }
                }
                
                if (!useExternalWebServer) {
                    wwwRootDirectory = new File(pluginDir, "www");
                    musicDirectory = new File(wwwRootDirectory, "music");
                    
                    try {
                        info("Rebuilding built-in music folder.");
                        Utility.rebuildMusicFolder(getClass(), musicDirectory);
                        info("Rebuild completed.");
                    }
                    catch (Exception ex) {
                        severe("Error Rebuilding: " + ex.getMessage());
                    }
                    
                    webserver = new WebServer();

                    info("Builtin webserver: " + wwwPort);
                    webserver.start(wwwRootDirectory.getPath(), wwwPort);
                }
                else {
                    try {
                       info("Rebuilding htdocs music folder.");
                       Utility.rebuildMusicFolder(getClass(), musicDirectory);
                       info("Rebuild completed.");
                    }
                    catch (Exception ex) {
                        severe("Error Rebuilding: " + ex.getMessage());
                    }
                }

                // Plugin is ready at this point.
                pluginEnabled = true;
            }
        }
        // No www root. Fall back on built-in webserver.
        else {
            wwwRootDirectory = new File(pluginDir, "www");
            musicDirectory = new File(wwwRootDirectory, "music");

            try {
                info("Rebuilding htdocs music folder.");
                Utility.rebuildMusicFolder(getClass(), musicDirectory);
                info("Rebuild completed.");
            }
            catch (Exception ex) {
                severe("Error Rebuilding: " + ex.getMessage());
            }

            info("Web Directory: " + wwwRootDirectory.getPath());
            
            // Is the refreshUrl configured?
            if (getConfig().contains("refreshUrl")) {
                // Get the refresh URL.
                refreshUrl = getConfig().getString("refreshUrl");
                
                info("Refresh URL: " + refreshUrl);

                if (getConfig().contains("wwwPort")) {
                    wwwPort = getConfig().getInt("wwwPort");
                }
                
                webserver = new WebServer();

                info("Builtin webserver: " + wwwPort);
                webserver.start(wwwRootDirectory.getPath(), wwwPort);

                // Plugin is ready at this point.
                pluginEnabled = true;
            }
        }
        
        try {
            // Get the factions plugin, if it exists.
            factions = SupportFactions.getFactions(getServer());
        } catch (PluginUnavailableException ex) {
            severe("Could not find Factions plugin.");
        }
        
        info(pluginEnabled ? "Enabled" : "Disabled");
    }

    @Override
    public void onDisable() {
        // Plugin disabled.
        pluginEnabled = false;
        
        if (webserver != null)
            webserver.shutdown();
        
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
            sender.sendMessage("MusicService: Command can only be run by a player.");
            return false;
        }

        // setmusic
        if (cmd.getName().equalsIgnoreCase("setmusic")) {
            // Has arguments?
            if (args.length > 0) {
                // Set music on faction land.
                boolean stationSet = setMusicOnFactionLand(player, args);
                
                // Has no faction here or no plugin?
                if (!stationSet) {
                    stationSet = setTownyStation(player, args);
                    
                    // Has no town here or no plugin?
                    if (!stationSet) {
                        // Set music in wg region.
                        stationSet = setWorldGuardStation(player, args);

                        // Has no wg region here or no plugin?
                        if (!stationSet) {
                            stationSet = setRegiosStation(player, args);

                            // Has no regios region here or no plugin?
                            if (!stationSet) {
                                stationSet = setIZoneStation(player, args);
                                
                                // Has no iZone zone here or no plugin?
                                if (!stationSet) {
                                    // Set music in the wilderness.
                                    stationSet = setMusicInWilderness(player, args);

                                    // Only fails without a tower.
                                    if (!stationSet) {
                                        sender.sendMessage("No tower here.");
                                    }
                                }
                            }
                        }
                    }
                }
                
                return stationSet;
            }
            // No arguments.
            else {
                player.sendMessage("Command lacked URL argument, e.g. ip:port");
            }
        }
        // reloadmusic
        else if (cmd.getName().equalsIgnoreCase("reloadmusic")) {
            player.sendMessage("Reloading music.");
            
            // Unset their music file.
            noPlayerMusic(player);

            // Schedule player station update.
            Bukkit.getScheduler().scheduleSyncDelayedTask(
                this,
                new Runnable(){
                    @Override
                    public void run() {
                        updatePlayerStation(player, player.getLocation());
                    }
                },
                40 // 20 ticks = 1 second
            );

            return true;
        }
        // findmusic
        else if (cmd.getName().equalsIgnoreCase("findmusic")) {
            // Has arguments?
            if (args.length > 0) {
                // Combine the arguments into one string.
                String searchString = StringEscapeUtils.escapeJava(Arrays.toString(args));

                info(player.getName() + " findmusic: " + searchString);
                
                // Find the music and get the streams.
                List<StreamInfo> streams = SupportShoutcast.find(sender, searchString);
                
                // Track the search results for this player.
                playerSearches.put(player.getName(), streams);
                return true;
            }
            else {
                player.sendMessage("Command lacked arguments.");
            }
        }
        else if (cmd.getName().equalsIgnoreCase("pickmusic")) {
            // Get the player name.
            String playerName = player.getName();
            
            // Player conducted a search?
            if (playerSearches.containsKey(playerName)) {
                // Get the streams they found.
                List<StreamInfo> streams = playerSearches.get(playerName);
                
                // Pick the music stream # they wanted.
                return pickMusic(player, streams, Integer.parseInt(args[0]));
            } else {
                player.sendMessage("MusicService: First use: /findmusic <searchstring>");
            }
        }

        return false;
    }

    //
    // Music Stream Support
    //
    
    private boolean setMusicInWilderness(final Player player, String[] args) {
        if (!pluginEnabled) return false;
        
        String regionName = Utility.getWildernessID(player.getLocation());
        
        // Has region name?
        if (regionName != null) {
            // User chose to blank out this parcel.
            if (args[0].equalsIgnoreCase("blank")) {
                info(String.format("Removing music station for faction: %s", regionName));

                // Set the URL for this chunk in the config to nothing.
                getConfig().set(regionName,  "");
                saveConfig();

                // Stop the music.
                noPlayerMusic(player);

                player.sendMessage("Removed music station.");

                return true;
            }

            // Unset their music file.
            noPlayerMusic(player);

            // Get the URL argument.
            final String url = args[0];

            // Has URL argument?
            if (url != null) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(
                    this,
                    new Runnable(){
                        @Override
                        public void run(){
                            setWildernessTowerStation(player, url);
                        }
                    },
                    40 // 20 ticks = 1 second
                );

                return true;
            }
        }
        
        return false;
    }
    
    private boolean setMusicOnFactionLand(final Player player, String[] args) {
        if (!pluginEnabled) return false;

        // Has factions?
        if (factions != null && factions.isEnabled()) {
            // Get the faction at the player's location, if any.
            final Faction faction = SupportFactions.getFactionAtLocation(player);

            // Does not have faction here?
            if (faction == null) {
                player.sendMessage("Faction did not exist.");
                return false;
            }

            // Get faction name.
            String regionName = faction.getTag();

            // Assume we're not in wilderness.
            boolean isWilderness = false;

            // Has name?
            if (regionName != null) {
                // Is it wilderness?
                isWilderness = regionName.contains("Wilderness");
            }

            // Not a safe zone, war zone, or wilderness?
            if (!(faction.isSafeZone() || faction.isWarZone() || isWilderness)) {
                // Player was not the faction admin? Bail out.
                if (!SupportFactions.isPlayerFactionAdmin(faction, player)) {
                    player.sendMessage("This is not your faction land.");
                    return false;
                }
            }

            // Not in wilderness.
            if (!isWilderness) {
                // User chose to blank out this parcel.
                if (args[0].equalsIgnoreCase("blank")) {
                    setStation("faction", regionName, player, "");
                    return true;
                }

                // Unset their music file.
                noPlayerMusic(player);

                // Get the URL argument.
                final String url = args[0];

                // Has URL argument?
                if (url != null) {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(
                        this,
                        new Runnable(){
                            @Override
                            public void run(){
                                setFactionStation(faction, player, url);
                            }
                        },
                        40 // 20 ticks = 1 second
                    );

                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean pickMusic(final Player player, List<StreamInfo> streams, int index) {
        boolean stationSet = false;
        if (streams.size() > 0) {
            String ipPort = Utility.getStream(streams, index);
            String[] args = new String[] {ipPort};

            info("Setting music stream [" + index + "]: " + ipPort);

            // Set music on faction land.
            stationSet = setMusicOnFactionLand(player, args);

            // Has no faction here or no plugin?
            if (!stationSet) {
                stationSet = setTownyStation(player, args);
                
                // Has no town here or no plugin?
                if (!stationSet) {
                    // Set music in wg region.
                    stationSet = setWorldGuardStation(player, args);

                    // Has no wg region here or no plugin?
                    if (!stationSet) {
                        stationSet = setRegiosStation(player, args);

                        // Has no regios region here or no plugin?
                        if (!stationSet) {
                            stationSet = setIZoneStation(player, args);

                            // Has no iZone zone here or no plugin?
                            if (!stationSet) {
                                // Set music in the wilderness.
                                stationSet = setMusicInWilderness(player, args);

                                // Only fails without a tower.
                                if (!stationSet) {
                                    player.sendMessage("No tower here.");
                                }
                            }
                        }
                    }
                }
            }
        }
        return stationSet;
    }
    
    private void setMusicStation(Player player, String url) {
        if (!pluginEnabled) return;
        
        //FileMutex txtSemaphore = null;
        //FileMutex playerSemaphore;
        
        // Get the player name.
        String playerName = player.getName();

        File playerMusicFile = new File(musicDirectory, playerName + ".html");
        //playerSemaphore = new FileMutex(playerMusicFile.getPath(), FileMutex.ONE_SECOND);
        
        try {
            //playerSemaphore.acquire();

            String newUrl = "http://" + url.replace("http://", "");
            
            File musicHtmlFile = new File(musicDirectory, "music.html");
            
            BufferedReader reader = new BufferedReader(new FileReader(musicHtmlFile));
            PrintWriter writer = new PrintWriter(new FileWriter(playerMusicFile));
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (newUrl != null)
                    line = line.replace("#URL", newUrl);
                
                if (playerName != null)
                    line = line.replace("#USER", playerName);
                
                if (relativeUrl != null)
                    line = line.replace("#RELATIVE", relativeUrl);
                
                writer.println(line);
            }

            reader.close();
            writer.close();
           
            File txtFile = new File(musicDirectory, playerName + ".txt");
            
            //txtSemaphore = new FileMutex(txtFile.getPath(), FileMutex.ONE_SECOND);
            //txtSemaphore.acquire();
            
            FileWriter fstream = new FileWriter(txtFile);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(" ");
            out.close();
        }
        catch (Exception e) {
            severe(String.format("Failed to write the music service file: %s", playerMusicFile.getPath()));
        }
        finally {
            //if (playerSemaphore != null) {
            //    playerSemaphore.release();
            //}
            
            //if (txtSemaphore != null) {
            //    txtSemaphore.release();
            //}
        }
    }
    
    private void changePlayerMusic(final Player player, final String url) {
        if (!pluginEnabled) return;
        
        if (url.equals("")) {
            // No station, turn off music and return.
            noPlayerMusic(player);
            return;
        }

        if (playerStations.containsKey(player.getName())) {
            if (playerStations.get(player.getName()).equals(url)) {
                // Player already on station, just return.
                return;
            }
        }
        
        noPlayerMusic(player);
        
        playerStations.put(player.getName(), url);
        
        Bukkit.getScheduler().scheduleSyncDelayedTask(
            this,
            new Runnable(){
                @Override
                public void run(){
                    setMusicStation(player, url);
                }
            },
            40 // 20 ticks = 1 second
        );
    }  
    
    private void noPlayerMusic(Player player) {
        if (!pluginEnabled) return;
        
        //FileMutex playerSemaphore = null;
        //FileMutex txtSemaphore = null;
        
        // Get the player name.
        String playerName = player.getName();

        // Establish the player file name.
        File playerMusicFile = new File(musicDirectory, playerName + ".html");

        // A file for refreshing the client via AJAX.
        File refreshedFile = new File(musicDirectory, playerName + ".txt");

        try {
            boolean deleted = false;

            if ((refreshedFile.exists())) {
                //txtSemaphore = new FileMutex(refreshedFile.getPath(), FileMutex.ONE_SECOND);
                //txtSemaphore.acquire();
                
                // Delete the txt file to indicate the need to refresh.
                // If it fails to succeed then this html already exists, don't create it.
                if (Utility.deleteFile(refreshedFile.getPath())) {
                    deleted = true;
                }
            }
            else {
                deleted = true;
            }

            if (deleted) {
                //playerSemaphore = new FileMutex(refreshedFile.getPath(), FileMutex.ONE_SECOND);
                //playerSemaphore.acquire();
                
                // Create the file and write the contents, replacing the URL with the one provided.
                FileWriter fstream = new FileWriter(playerMusicFile);
                BufferedWriter out = new BufferedWriter(fstream);

                // Replace all occurances of #USER with the player name in the text.
                String newRefresh = refresh.replace("#USER", playerName);
                newRefresh = newRefresh.replace("#REFRESH", refreshUrl);

                // Write the new html for the refresh to the file.
                out.write(newRefresh);
                out.close();

                // Nothing is something too! ;-)
                playerStations.put(player.getName(), "");
            }
        }
        catch (Exception e) {
            severe(String.format("Failed to write the music service file: %s", playerMusicFile.getPath()));
        }
        finally {
            //if (playerSemaphore != null) {
            //    playerSemaphore.release();
            //}
            
            //if (txtSemaphore != null) {
            //    txtSemaphore.release();
            //}
        }
    }
    
    public void updatePlayerStation(Player player, Location location) {
        if (!pluginEnabled) return;
        
        if (player != null && location != null) {
            // Change station for faction. Fails on wilderness / no faction.
            if (!handleFactions(SupportFactions.getFactionAtLocation(location), player)) {
                debug("not a faction");
                if (!handleTowny(player)) {
                    debug("not a town");
                    // Change station for wg region. Fails on no region.
                    if (!handleWorldGuard(player)) {
                        debug("not a wg region");
                        if (!handleRegios(player)) {
                            debug("not a regios region");
                            if (!handleIZone(player)) {
                                debug("not an iZone region");
                                // Must be wilderness, check for tower.
                                if (!handleWilderness(player)) {
                                    debug("no radio tower");
                                }
                            }
                        }
                    }
                }
            }
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
    
    private boolean handleFactions(Faction faction, Player player) {
        if (!pluginEnabled) return false;
        
        // Faction exists?
        if (factions != null && factions.isEnabled() && faction != null) {
            // Get the faction region name.
            String regionName = faction.getTag();
            
            // Has region and it's not Wilderness, therfore it is faction land?
            if (regionName != null) {
                if (!regionName.contains("Wilderness")) {
                    return listenStation("faction", regionName, player);
                }
            }
        }
        
        return false;
    }

    private boolean setFactionStation(Faction faction, Player player, String url) {
        if (!pluginEnabled) return false;
        
        // Get the faction region name.
        String regionName = faction.getTag();

        if (regionName != null) {
            setStation("faction", regionName, player, url);
            return true;
        }
        
        return false;
    }
    
    //
    // Wilderness Support
    //
    
    private boolean handleWilderness(Player player) {
        if (!pluginEnabled) return false;
        
        for (String locationString : radioTowers.keySet()) {
            int radius = radioTowers.get(locationString);

            Location location = Utility.stringToLocation(getServer(), locationString);
            
            if (Utility.towerIntersects(location, radius, player.getLocation())) {
                return listenStation("wilderness", locationString, player);
            }
        }
        
        // Stop the music, we did not find any towers.
        noPlayerMusic(player);
        
        return false;
    }
    
    private boolean setWildernessTowerStation(Player player, String url) {
        if (!pluginEnabled) return false;
        
        for (String locationString : radioTowers.keySet()) {
            int radius = radioTowers.get(locationString);

            Location location = Utility.stringToLocation(getServer(), locationString);
            
            debug(String.format("setTowerStation - TowerLocation: %f, %f TowerRadius: %d", location.getX(), location.getZ(), radius));
            
            if (Utility.towerIntersects(location, radius, player.getLocation())) {
                if (url != null) {
                    radioStations.put(locationString, url);
                    setStation("wilderness", locationString, player, url);
                    return true;
                }
            }
        }
        
        return false;
    }
    
    //
    // WorldGuard Regions Support
    //

    private boolean handleWorldGuard(Player player) {
        if (!pluginEnabled) return false;

        // Has player?
        if (player != null) {
            try {
                ProtectedRegion stationRegion = SupportWorldguard.getWorldGuardRegionAt(getServer(), player);
                
                if (stationRegion != null) {
                    return listenStation("worldguard", stationRegion.getId(), player);
                }
            }
            catch (PluginUnavailableException ex) {
                // Fail silently.
            }
        }

        return false;
    }
    
    private boolean setWorldGuardStation(Player player, String[] args) {
        if (!pluginEnabled) return false;
        
        try {
            ProtectedRegion stationRegion =
                    SupportWorldguard.isAdminAtWorldGuardRegionAt(getServer(), player);
            
            // Has a station region?
            if (stationRegion != null) {
                if (args[0].equalsIgnoreCase("blank"))
                    setStation("worldguard", stationRegion.getId(), player, "");
                else
                    setStation("worldguard", stationRegion.getId(), player, args[0]);
                
                return true;
            }
        }
        catch (PluginUnavailableException ex) {
            // Quietly fail if there is no WorldGuard, user may have chosen not to install it.
            debug("No worldguard detected.");
        }
        
        return false;
    }
    
    //
    // Towny Advanced Support
    //
    
    public void onTownyPlayerMoveChunk(Player player, WorldCoord from, WorldCoord to,
                                  Location fromLoc, Location toLoc) {
        TownBlock block;
        try {
            block = to.getWorld().getTownBlock(to.getX(), to.getZ());
        
            if (block != null && block.hasTown()) {
                Town town = block.getTown();
                String townName = town.getName();
                debug("TOWN: " + townName);
                playerNowStandingInTown(player, townName);
                handleTowny(player);
            }
            else {
                playerNowStandingInTown(player, null);
            }
        } catch (NotRegisteredException ex) {
            playerNowStandingInTown(player, null);
        }
    }
    
    private boolean handleTowny(Player player) {
        if (!pluginEnabled) return false;

        try {
            // Has player?
            if (player != null) {
                String name = getTownPlayerIsStandingIn(player);

                // Has town here?
                if (name != null) {
                    debug("has town here: " + name);
                    return listenStation("town", name, player);
                }
                else {
                    debug("no town found here");
                }
            }
        }
        catch (Exception ex) {
            // Fail silently. Might mean towny not installed, or no town here.
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            debug("Error handleTowny: " + sw.toString());
        }

        return false;
    }
    
    private boolean setTownyStation(Player player, String[] args) {
        if (!pluginEnabled) return false;
        
        try {
            String name = getTownPlayerIsStandingIn(player);
            
            // Has town info?
            if (name != null) {
                if (args[0].equalsIgnoreCase("blank"))
                    setStation("town", name, player, "");
                else
                    setStation("town", name, player, args[0]);

                return true;
            }
        }
        catch (Exception ex) {
            // Fail silently. No plugin or no town here.
            debug("Error setTownyStation: " + ex.getMessage());
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
    
    //
    // Station Support
    //
    
    private String getFieldName(String prefix, String name) {
        return prefix + "-" + name;
    }
    
    private void setStation(String prefix, String name, Player player, String station) {
        String fieldName = getFieldName(prefix, name);

        // Set the URL for the field in the config.
        getConfig().set(fieldName, station);
        saveConfig();

        if (station.equals("")) {
            // Stop the music.
            noPlayerMusic(player);

            player.sendMessage(String.format("Removed music station for: %s", fieldName));
        }
        else {
            // Change the player's music to the URL specified immediately.
            changePlayerMusic(player, station);

            player.sendMessage(String.format("Set Music Station: %s", station));
        }
    }
    
    private boolean listenStation(String prefix, String name, Player player) {
        if (player != null) {
            String fieldName = getFieldName(prefix, name);

            // Config holds an entry for the station?
            if (getConfig().contains(fieldName)) {
                // Get the station url.
                String url = (String)getConfig().get(fieldName);

                if (url != null) {
                    debug(String.format("Changing station for %s in %s to %s", player.getName(), fieldName, url));

                    // Change the player music to the url for this station.
                    changePlayerMusic(player, url);

                    return true;
                }
            }
            // No entry at this chunk.
            else {
                // No music at this chunk.
                noPlayerMusic(player);
            }
        }
        
        return false;
    }
    
    //
    // Regios Support
    //
    
    private boolean handleRegios(Player player) {
        if (!pluginEnabled || regios == null) return false;

        try {
            // Has player?
            if (player != null) {
                Region region = GlobalRegionManager.getRegion(player);

                // Has regios region here?
                if (region != null) {
                    String name = region.getName();
                    debug("has regios here: " + name);
                    return listenStation("regios", name, player);
                }
                else {
                    debug("no regios found here");
                }
            }
        }
        catch (Exception ex) {
            // Fail silently. Might mean regios not installed, or no region here.
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            debug("Error handleRegios: " + sw.toString());
        }

        return false;
    }
    
    private boolean setRegiosStation(Player player, String[] args) {
        if (!pluginEnabled || regios == null) return false;
        
        try {
            Region region = GlobalRegionManager.getRegion(player);
            
            // Has regios info?
            if (region != null) {
                String name = region.getName();

                debug(String.format("setRegiosStation: %s %s %s", player.getName(), name, args[0]));
                
                if (args[0].equalsIgnoreCase("blank"))
                    setStation("regios", name, player, "");
                else
                    setStation("regios", name, player, args[0]);

                return true;
            }
        }
        catch (Exception ex) {
            // Fail silently. No plugin or no region here.
            debug("Error setRegiosStation: " + ex.getMessage());
        }
        
        return false;
    }
    
    //
    // iZone Support
    //
    
    private boolean handleIZone(Player player) {
        if (!pluginEnabled || izone == null) return false;

        try {
            // Has player?
            if (player != null) {
                Zone zone = ZoneManager.getZone(player.getLocation());

                // Has iZone zone here?
                if (zone != null) {
                    String name = zone.getName();
                    debug("has zone here: " + name);
                    return listenStation("izone", name, player);
                }
                else {
                    debug("no zone found here");
                }
            }
        }
        catch (Exception ex) {
            // Fail silently. Might mean iZone not installed, or no zone here.
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            debug("Error handleIZone: " + sw.toString());
        }

        return false;
    }
    
    private boolean setIZoneStation(Player player, String[] args) {
        if (!pluginEnabled || izone == null) return false;
        
        try {
            Zone zone = ZoneManager.getZone(player.getLocation());
            
            // Has zone info?
            if (zone != null) {
                String name = zone.getName();

                debug(String.format("setIZoneStation: %s %s %s", player.getName(), name, args[0]));
                
                if (args[0].equalsIgnoreCase("blank"))
                    setStation("izone", name, player, "");
                else
                    setStation("izone", name, player, args[0]);

                return true;
            }
        }
        catch (Exception ex) {
            // Fail silently. No plugin or no zone here.
            debug("Error setIZoneStation: " + ex.getMessage());
        }
        
        return false;
    }
}