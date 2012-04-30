package blakesteel.MusicService;

import com.massivecraft.factions.Faction;
import com.palmergames.bukkit.towny.Towny;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import couk.Adamki11s.Regios.Regions.GlobalRegionManager;
import couk.Adamki11s.Regios.Regions.Region;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.techguard.izone.managers.ZoneManager;
import net.techguard.izone.zones.Zone;
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
    private Map<String, Boolean> musicChangeScheduled = new HashMap<String, Boolean>();
    private WebMemoryFile templateWebFile = null;
    private String refreshUrl = "http://localhost:8888/";
    private Plugin factions;
    private Plugin regios;
    private Plugin izone;
    private WebServer webserver;
    private int wwwPort = 8888;
    private String refresh = "<meta HTTP-EQUIV=\"REFRESH\" content=\"2; url=#REFRESH"
            + "#USER.html\">There is currently no music station available at this position.";

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
        // Register the music service listener for events.
        getServer().getPluginManager().registerEvents(
                new MusicServiceListener(this), this);

        // Try to hook the towny listener.
        try {
            Towny towny = SupportTowny.getTowny(getServer());
            getServer().getPluginManager().registerEvents(
                    new MusicServiceTownyListener(towny, this), this);
            info("Hooked Towny Support");
        } catch (PluginUnavailableException ex) {
            // info("Skipped Towny Support");
        }

        // Try to hook the regios support.
        try {
            regios = SupportRegios.getRegios(getServer());
            info("Hooked Regios Support");
        } catch (PluginUnavailableException ex) {
            // info("Skipped Regios Support");
        }

        // Try to hook the iZone support.
        try {
            izone = SupportIZone.getIZone(getServer());
            info("Hooked iZone Support");
        } catch (PluginUnavailableException ex) {
            // info("Skipped iZone Support");
        }

        // Creates a config.yml if there isn't yet one.
        getConfig().options().copyDefaults(true);
        saveConfig();

        // Default to plugin disabled.
        pluginEnabled = false;

        info("Loading configuration....");

        // Is the refreshUrl configured?
        if (getConfig().contains("refreshUrl")) {
            // Get the refresh URL.
            refreshUrl = getConfig().getString("refreshUrl");
        }

        info("Refresh URL: " + refreshUrl);

        if (getConfig().contains("wwwPort")) {
            wwwPort = getConfig().getInt("wwwPort");
        }

        info("WWW Port: " + wwwPort);

        try {
            templateWebFile = new WebMemoryFile(
                    Utility.loadFromResourceAsString(getClass(), "music.html"));
        } catch (IOException ex) {
            severe("Error: " + ex.getMessage());
        }

        webserver = new WebServer();
        setWebFile("favicon.ico");
        setWebFile("ffmp3.swf");
        setWebFile("green.png");
        setWebFile("index.html");
        setWebFile("yellow.png");
        webserver.start(wwwPort);

        // Plugin is ready at this point.
        pluginEnabled = true;

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
    public boolean onCommand(CommandSender sender, Command cmd,
            String commandLabel, String[] args) {

        // Plugin not enabled? Return.
        if (!isPluginEnabled())
            return false;

        // Player.
        final Player player;

        // Is a player? Store player variable.
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        // Is server console?
        else {
            sender.sendMessage("MusicService: Command can only be run by a player.");
            return false;
        }

        if (cmd.getName().equalsIgnoreCase("MusicService")) {
            if (args.length > 0) {
                String[] cmdArgs = new String[args.length - 1];
                for (int i = 0; i < args.length - 1; i++) { // Removing first
                                                            // parameter
                                                            // (command)
                    cmdArgs[i] = args[i + 1];
                }
                if (args[0].equalsIgnoreCase("find")) { // /music find
                    if (player.hasPermission("musicservice.find"))
                        return CmdFind(player, cmd, cmdArgs);
                }
                if (args[0].equalsIgnoreCase("pick")) { // /music pick
                    if (player.hasPermission("musicservice.pick"))
                        return CmdPick(player, cmd, cmdArgs);
                }
                if (args[0].equalsIgnoreCase("set")) { // /music set
                    if (player.hasPermission("musicservice.set"))
                        return CmdSet(player, cmd, cmdArgs);
                }
                if (args[0].equalsIgnoreCase("reload")) { // /music reload
                    if (player.hasPermission("musicservice.reload"))
                        return CmdReload(player, cmd, cmdArgs);
                }
                if (args[0].equalsIgnoreCase("save")) { // /music save
                    if (player.hasPermission("musicservice.save"))
                        return CmdSave(player, cmd, cmdArgs);
                }
                player.sendMessage("Unknow command");
                return CmdHelp(player);
            } else {
                return CmdHelp(player);
            }
        }
        return false;
    }

    private boolean CmdFind(Player player, Command cmd, String[] args) {

        // Has arguments?
        if (args.length > 0) {
            // Combine the arguments into one string.
            URI uri;
            URL request;

            String searchString = "";
            for (String token : args) {
                searchString += token + " ";
            }

            try {
                uri = new URI("http", "www.shoutcast.com", "/Internet-Radio/"
                        + searchString, null);
                request = uri.toURL();

                info(player.getName() + " findmusic: " + searchString);

                // Find the music and get the streams.
                List<StreamInfo> streams = SupportShoutcast.find(player,
                        request);

                if (streams == null) {
                    player.sendMessage("We are sorry, there were no radio stations matching: "
                            + searchString);
                    return false;
                } else {
                    // Track the search results for this player.
                    playerSearches.put(player.getName(), streams);
                }
                return true;
            } catch (Exception ex) {
                severe("Error: " + ex.getMessage());
            }
        } else {
            player.sendMessage("Command lacked arguments.");
        }
        return false;
    }

    private boolean CmdPick(Player player, Command cmd, String[] args) {
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
        return false;
    }

    private boolean CmdSet(Player player, Command cmd, String[] args) {
        if (args.length > 0) {
            setStation(player, new String[] { args[0] });
            return true;
        }
        player.sendMessage("Usage: /music setmusic http://mycoolstream.net:8080");
        return false;
    }

    private boolean CmdReload(final Player player, Command cmd, String[] args) {
        player.sendMessage("Reloading music.");

        // Unset their music file.
        noPlayerMusic(player);

        // Schedule player station update.
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                updatePlayerStation(player, player.getLocation());
            }
        }, 40 // 20 ticks = 1 second
                );

        return true;
    }

    private boolean CmdSave(Player player, Command cmd, String[] args) {
        // TODO Auto-generated method stub
        return false;
    }

    private boolean CmdHelp(Player player) {
        player.sendMessage("MusicService commands:");
        if (player.hasPermission("musicservice.find"))
            player.sendMessage("/music find <Search string>");
        if (player.hasPermission("musicservice.pick"))
            player.sendMessage("/music pick <#>");
        if (player.hasPermission("musicservice.set"))
            player.sendMessage("/music set <URL>");
        if (player.hasPermission("musicservice.reload"))
            player.sendMessage("/music reload");
        return true;
    }

    //
    // Webserver Routines
    //

    private void setWebFile(String path) {
        try {
            byte[] bytes = Utility.loadFromResourceAsBytes(getClass(), path);
            webserver.setMemoryValue(path, new WebMemoryFile(bytes));
        } catch (IOException ex) {
            severe("Error: " + ex.getMessage());
        }
    }

    //
    // Music Stream Support
    //

    private boolean setMusicInWilderness(final Player player, String[] args) {
        if (!pluginEnabled)
            return false;

        String regionName = Utility.getWildernessID(player.getLocation());

        // Has region name?
        if (regionName != null) {
            // User chose to blank out this parcel.
            if (args[0].equalsIgnoreCase("blank")) {
                info(String.format("Removing music station for faction: %s",
                        regionName));

                // Set the URL for this chunk in the config to nothing.
                getConfig().set(regionName, "");
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
                Bukkit.getScheduler().scheduleSyncDelayedTask(this,
                        new Runnable() {
                            @Override
                            public void run() {
                                setWildernessTowerStation(player, url);
                            }
                        }, 40 // 20 ticks = 1 second
                        );

                return true;
            }
        }

        return false;
    }

    private boolean setMusicOnFactionLand(final Player player, String[] args) {
        if (!pluginEnabled)
            return false;

        // Has factions?
        if (factions != null && factions.isEnabled()) {
            // Get the faction at the player's location, if any.
            Faction factionLand;

            try {
                factionLand = SupportFactions.getFactionAtLocation(getServer(),
                        player);
            } catch (PluginUnavailableException ex) {
                return false;
            }

            // Does not have faction here?
            if (factionLand == null) {
                player.sendMessage("Faction did not exist.");
                return false;
            }

            // Get faction name.
            String regionName = factionLand.getTag();

            // Assume we're not in wilderness.
            boolean isWilderness = false;

            // Has name?
            if (regionName != null) {
                // Is it wilderness?
                isWilderness = regionName.contains("Wilderness");
            }

            // Not a safe zone, war zone, or wilderness?
            if (!(factionLand.isSafeZone() || factionLand.isWarZone() || isWilderness)) {
                try {
                    // Player was not the faction admin? Bail out.
                    if (!SupportFactions.isPlayerFactionAdmin(getServer(),
                            factionLand, player)) {
                        player.sendMessage("This is not your faction land.");
                        return false;
                    }
                } catch (PluginUnavailableException ex) {
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
                final Faction faction = factionLand;

                // Has URL argument?
                if (url != null) {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(this,
                            new Runnable() {
                                @Override
                                public void run() {
                                    setFactionStation(faction, player, url);
                                }
                            }, 40 // 20 ticks = 1 second
                            );

                    return true;
                }
            }
        }

        return false;
    }

    private boolean pickMusic(final Player player, List<StreamInfo> streams,
            int index) {
        if (streams.size() > 0) {
            String ipPort = Utility.getStream(streams, index);
            String[] args = new String[] { ipPort };

            info("Setting music stream [" + index + "]: " + ipPort);

            return setStation(player, args);
        }
        return false;
    }

    private boolean setStation(Player player, String[] args) {
        // Set music on faction land.
        debug("faction land");
        if (setMusicOnFactionLand(player, args))
            return true;

        // Set music on town
        debug("Towny");
        if (setTownyStation(player, args))
            return true;

        // Set music on wg region.
        debug("worldguard");
        if (setWorldGuardStation(player, args))
            return true;

        // Set music on regios region.
        debug("regios");
        if (setRegiosStation(player, args))
            return true;

        // Set music on iZone zone
        debug("iZone");
        if (setIZoneStation(player, args))
            return true;

        // Set music on wilderness.
        debug("wilderness");
        if (setMusicInWilderness(player, args))
            return true;

        // Only fails without a tower.
        player.sendMessage("No tower here.");
        return false;
    }

    private void setMusicStation(Player player, String url) throws IOException {
        if (!pluginEnabled)
            return;

        String playerName = player.getName();
        String newUrl = "http://" + url.replace("http://", "");
        String playerPath = playerName + ".html";
        String txtPath = playerName + ".txt";
        String playerWebData = templateWebFile.getString();

        String rootUrl;
        try {
            rootUrl = "http://" + (new URL(newUrl).toURI().getHost());
        } catch (Exception ex) {
            return;
        }

        // Has url? Replace #URL.
        if (newUrl != null)
            playerWebData = playerWebData.replace("#URL", newUrl);

        // Has player name? Replace #USER.
        if (playerName != null)
            playerWebData = playerWebData.replace("#USER", playerName);

        // Replace all occurances of #ROOT with the root url.
        playerWebData = playerWebData.replace("#ROOT", rootUrl);

        WebMemoryFile f = new WebMemoryFile(" ");
        webserver.setMemoryValue(txtPath, f);

        webserver.setMemoryValue(playerPath, new WebMemoryFile(playerWebData));

        debug("station set: " + newUrl);

        playerStations.put(player.getName(), newUrl);
    }

    private void changePlayerMusic(final Player player, final String url) {
        if (!pluginEnabled)
            return;

        // debug("changePlayerMusic: " + url);

        // No station, turn off music and return.
        /*
         * if (url.equals("")) { debug("no station"); noPlayerMusic(player);
         * return; }
         */

        // Player is on a station?
        if (playerStations.containsKey(player.getName())) {
            // Station already matches the one we're changing to?
            if (playerStations.get(player.getName()).equals(url)) {
                // debug("player already on station");
                // Done.
                return;
            }
        }

        // Does not contain a music schedule change?
        if (!musicChangeScheduled.containsKey(player.getName())) {
            // Initialize schedule, default to no schedule.
            musicChangeScheduled.put(player.getName(), false);
        }

        // Not yet scheduled to change?
        if (!musicChangeScheduled.get(player.getName())) {
            // Schedule a change.
            musicChangeScheduled.put(player.getName(), true);

            // debug("stopping music...pending station change..");

            // Turn off the music to flag the .txt file for the ajax to reset.
            noPlayerMusic(player);

            // Schedule the task.
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                @Override
                public void run() {
                    try {
                        // debug("setting music station...");
                        setMusicStation(player, url);
                    } catch (IOException ex) {
                        severe(ex.getMessage());
                    }

                    // Music changed, unschedule.
                    musicChangeScheduled.put(player.getName(), false);
                }
            }, 40 // 20 ticks = 1 second
                    );
        }
    }

    private void noPlayerMusic(Player player) {
        if (!pluginEnabled)
            return;

        // Already has a station?
        if (playerStations.containsKey(player.getName())) {
            // Station is already set to none?
            if (playerStations.get(player.getName()).equals("")) {
                // Done.
                return;
            }
        }

        // Nothing is something too! ;-)
        playerStations.put(player.getName(), "");

        // Get the player name.
        String playerName = player.getName();
        String playerPath = playerName + ".html";
        String txtPath = playerName + ".txt";

        String newRefreshUrl = refreshUrl;
        if (!refreshUrl.endsWith("/")) {
            newRefreshUrl = refreshUrl + "/";
        }

        String rootUrl;
        try {
            rootUrl = "http://" + (new URL(refreshUrl).toURI().getHost());
        } catch (Exception ex) {
            return;
        }

        // Replace all occurances of #USER with the player name in the text.
        String newRefresh = refresh.replace("#USER", playerName);

        // Replace all occurances of #REFRESH with the refresh url.
        newRefresh = newRefresh.replace("#REFRESH", newRefreshUrl);

        // Replace all occurances of #ROOT with the root url.
        newRefresh = newRefresh.replace("#ROOT", rootUrl);

        WebMemoryFile f = new WebMemoryFile(" ");
        f.setEnabled(false);

        webserver.setMemoryValue(txtPath, f);
        webserver.setMemoryValue(playerPath, new WebMemoryFile(newRefresh));
    }

    public void updatePlayerStation(Player player, Location location) {
        if (!pluginEnabled)
            return;

        if (player != null && location != null) {
            // Change station for faction. Fails on wilderness / no faction.
            if (!handleFactions(player, location)) {
                // debug("not a faction");
                if (!handleTowny(player)) {
                    // debug("not a town");
                    // Change station for wg region. Fails on no region.
                    if (!handleWorldGuard(player)) {
                        // debug("not a wg region");
                        if (!handleRegios(player)) {
                            // debug("not a regios region");
                            if (!handleIZone(player)) {
                                // debug("not an iZone region");
                                // Must be wilderness, check for tower.
                                if (!handleWilderness(player)) {
                                    // debug("no radio tower");
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
            logger.log(Level.INFO,
                    String.format("%s(DEBUG) %s", LOG_PREFIX, msg));
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

    private boolean handleFactions(Player player, Location location) {
        if (!pluginEnabled)
            return false;

        // Faction exists?
        if (factions != null && factions.isEnabled()) {
            Faction faction = null;
            try {
                faction = SupportFactions.getFactionAtLocation(getServer(),
                        location);
            } catch (PluginUnavailableException ex) {
                return false;
            }

            if (faction != null) {
                // Get the faction region name.
                String regionName = faction.getTag();

                // Has region and it's not Wilderness, therfore it is faction
                // land?
                if (regionName != null) {
                    if (!regionName.contains("Wilderness")) {
                        return listenStation("faction", regionName, player);
                    }
                }
            }
        }

        return false;
    }

    private boolean setFactionStation(Faction faction, Player player, String url) {
        if (!pluginEnabled)
            return false;

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
        if (!pluginEnabled)
            return false;

        for (String locationString : radioTowers.keySet()) {
            int radius = radioTowers.get(locationString);

            Location location = Utility.stringToLocation(getServer(),
                    locationString);

            if (Utility.towerIntersects(location, radius, player.getLocation())) {
                return listenStation("wilderness", locationString, player);
            }
        }

        // Stop the music, we did not find any towers.
        noPlayerMusic(player);

        return false;
    }

    private boolean setWildernessTowerStation(Player player, String url) {
        if (!pluginEnabled)
            return false;

        for (String locationString : radioTowers.keySet()) {
            int radius = radioTowers.get(locationString);

            Location location = Utility.stringToLocation(getServer(),
                    locationString);

            // debug(String.format("setTowerStation - TowerLocation: %f, %f TowerRadius: %d",
            // location.getX(), location.getZ(), radius));

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
        if (!pluginEnabled)
            return false;

        // Has player?
        if (player != null) {
            try {
                ProtectedRegion stationRegion = SupportWorldguard
                        .getWorldGuardRegionAt(getServer(), player);

                if (stationRegion != null) {
                    return listenStation("worldguard", stationRegion.getId(),
                            player);
                }
            } catch (PluginUnavailableException ex) {
                // Fail silently.
            }
        }

        return false;
    }

    private boolean setWorldGuardStation(Player player, String[] args) {

        if (!pluginEnabled)
            return false;

        try {
            ProtectedRegion stationRegion = SupportWorldguard
                    .isAdminAtWorldGuardRegionAt(getServer(), player);

            // Has a station region?
            if (stationRegion != null) {
                if (args[0].equalsIgnoreCase("blank"))
                    setStation("worldguard", stationRegion.getId(), player, "");
                else
                    setStation("worldguard", stationRegion.getId(), player,
                            args[0]);

                return true;
            }
        } catch (PluginUnavailableException ex) {

            // Quietly fail if there is no WorldGuard, user may have chosen not
            // to install it.
            debug("No worldguard detected.");
        }
        return false;
    }

    //
    // Towny Advanced Support
    //

    /*
     * public void onTownyPlayerMoveChunk(Player player, WorldCoord from,
     * WorldCoord to, Location fromLoc, Location toLoc) { TownBlock block; try {
     * block = to.getWorld().getTownBlock(to.getX(), to.getZ());
     * 
     * if (block != null && block.hasTown()) { Town town = block.getTown();
     * String townName = town.getName(); //debug("TOWN: " + townName);
     * playerNowStandingInTown(player, townName); handleTowny(player); } else {
     * playerNowStandingInTown(player, null); } } catch (NotRegisteredException
     * ex) { playerNowStandingInTown(player, null); } }
     */

    public boolean handleTowny(Player player) {
        if (!pluginEnabled)
            return false;

        try {
            // Has player?
            if (player != null) {
                String name = getTownPlayerIsStandingIn(player);

                // Has town here?
                if (name != null) {
                    // debug("has town here: " + name);
                    return listenStation("town", name, player);
                } else {
                    // debug("no town found here");
                }
            }
        } catch (Exception ex) {
            // Fail silently. Might mean towny not installed, or no town here.
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            // debug("Error handleTowny: " + sw.toString());
        }

        return false;
    }

    private boolean setTownyStation(Player player, String[] args) {
        if (!pluginEnabled)
            return false;

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
        } catch (Exception ex) {
            // Fail silently. No plugin or no town here.
            // debug("Error setTownyStation: " + ex.getMessage());
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

    private void setStation(String prefix, String name, Player player,
            String station) {
        String fieldName = getFieldName(prefix, name);

        // Set the URL for the field in the config.
        getConfig().set(fieldName, station);
        saveConfig();

        if (station.equals("")) {
            // Stop the music.
            noPlayerMusic(player);

            player.sendMessage(String.format("Removed music station for: %s",
                    fieldName));
        } else {
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
                String url = (String) getConfig().get(fieldName);

                if (url != null) {
                    // debug(String.format("Changing station for %s in %s to %s",
                    // player.getName(), fieldName, url));

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
        if (!pluginEnabled || regios == null)
            return false;

        try {
            // Has player?
            if (player != null) {
                Region region = GlobalRegionManager.getRegion(player);

                // Has regios region here?
                if (region != null) {
                    String name = region.getName();
                    // debug("has regios here: " + name);
                    return listenStation("regios", name, player);
                } else {
                    // debug("no regios found here");
                }
            }
        } catch (Exception ex) {
            // Fail silently. Might mean regios not installed, or no region
            // here.
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            // debug("Error handleRegios: " + sw.toString());
        }

        return false;
    }

    private boolean setRegiosStation(Player player, String[] args) {
        if (!pluginEnabled || regios == null)
            return false;

        try {
            Region region = GlobalRegionManager.getRegion(player);

            // Has regios info?
            if (region != null) {
                String name = region.getName();

                // debug(String.format("setRegiosStation: %s %s %s",
                // player.getName(), name, args[0]));

                if (args[0].equalsIgnoreCase("blank"))
                    setStation("regios", name, player, "");
                else
                    setStation("regios", name, player, args[0]);

                return true;
            }
        } catch (Exception ex) {
            // Fail silently. No plugin or no region here.
            // debug("Error setRegiosStation: " + ex.getMessage());
        }

        return false;
    }

    //
    // iZone Support
    //

    private boolean handleIZone(Player player) {
        if (!pluginEnabled || izone == null)
            return false;

        try {
            // Has player?
            if (player != null) {
                Zone zone = ZoneManager.getZone(player.getLocation());

                // Has iZone zone here?
                if (zone != null) {
                    String name = zone.getName();
                    // debug("has zone here: " + name);
                    return listenStation("izone", name, player);
                } else {
                    // debug("no zone found here");
                }
            }
        } catch (Exception ex) {
            // Fail silently. Might mean iZone not installed, or no zone here.
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            // debug("Error handleIZone: " + sw.toString());
        }

        return false;
    }

    private boolean setIZoneStation(Player player, String[] args) {
        if (!pluginEnabled || izone == null)
            return false;

        try {
            Zone zone = ZoneManager.getZone(player.getLocation());

            // Has zone info?
            if (zone != null) {
                String name = zone.getName();

                // debug(String.format("setIZoneStation: %s %s %s",
                // player.getName(), name, args[0]));

                if (args[0].equalsIgnoreCase("blank"))
                    setStation("izone", name, player, "");
                else
                    setStation("izone", name, player, args[0]);

                return true;
            }
        } catch (Exception ex) {
            // Fail silently. No plugin or no zone here.
            // debug("Error setIZoneStation: " + ex.getMessage());
        }

        return false;
    }
}