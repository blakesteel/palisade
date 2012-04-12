// FIXED in 1.0.7 - bug #00001 - setting music doesn't reload page
// FIXED in 1.0.6 - bug #00002 - can't remove music from a parcel
// FIXED in 1.0.7 - bug #00003 - initial file does not exist for html
// FIXED in 1.1.3 - bug #00004 - timer times out after a really long time and never kicks back in
//                - bug #00005 - urls are not safe?
// FIXED in 1.0.7 - bug #00006 - when logging in, parcel does not select music station
// FIXED in 1.1.3 - bug #00007 - when you teleport the music doesn't play
//                - bug #00008 - most users dont use the music but still incur load on server
// FIXED in 1.1.3 - bug #00009 - support factions instead of individual chunks
// FIXED in 1.1.3 - bug #00010 - handle authority of setting / unsetting music
// FIXED in 1.2.0 - bug #00011 - Safe Zones and War Zones have no music.
// FIXED in 1.1.8 - bug #00012 - When you travel from one faction to another faction music stays.
package blakesteel.MusicService;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.awt.Rectangle;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

class StreamInfo {
    public String Title;
    public String Stream;
}

/**
 * Music Service
 * 
 * @author blakesteel
 */
public class MusicService extends JavaPlugin implements Listener {
    static final String LOG_PREFIX = "[MusicService] ";
    static final String shoutcastSearchUrl = "http://www.shoutcast.com/Internet-Radio/%s";
    static final Logger logger = Logger.getLogger("Minecraft");

    
    private boolean pluginEnabled = false;
    
    private Map<String, Integer> radioTowers = new HashMap<String, Integer>();
    private Map<String, String> radioStations = new HashMap<String, String>();
    private Map<String, String> playerStations = new HashMap<String, String>();
    private Map<String, List<StreamInfo>> playerSearches = new HashMap<String, List<StreamInfo>>();
    
    private String refreshUrl;
    private String relativeUrl;
    
    private String refresh = "<meta HTTP-EQUIV=\"REFRESH\" content=\"1; url=#REFRESH" +
                             "music/#USER.html\">There is currently no music station available at this position.";
    
    private File wwwRootDirectory;
    private File musicDirectory;
    
    private Plugin factions;
    
    //
    // Bukkit Events
    //
    
    @Override
    public void onEnable() {
        // Register the music service for listener events.
        getServer().getPluginManager().registerEvents(this, this);

        // Creates a config.yml if there isn't yet one.
        getConfig().options().copyDefaults(false);
        saveConfig();
        
        // Default to plugin disabled.
        pluginEnabled = false;
        
        // Is the wwwRoot configured? This is required.
        if (getConfig().contains("wwwRoot")) {
            // Establish paths to the www root and music directory.
            wwwRootDirectory = new File(getConfig().getString("wwwRoot"));
            musicDirectory = new File(wwwRootDirectory, "music");

            // Is the refreshUrl configured? This is required.
            if (getConfig().contains("refreshUrl")) {
                // Get the refresh URL.
                refreshUrl = getConfig().getString("refreshUrl");

                // Is the relativeUrl configured? This is optional.
                if (getConfig().contains("relativeUrl")) {
                    // Get the relative URL.
                    relativeUrl = getConfig().getString("relativeUrl");
                }
                // Not relative, set to "".
                else {
                    relativeUrl = "";
                }

                // Plugin is ready at this point.
                pluginEnabled = true;
            }
        }
        // No www root, this was required.
        else {
            severe("No www root path defined in the config.yml file.");

            // Plugin is not enabled.
            pluginEnabled = false;
        }
        
        try {
            // Get the factions plugin, if it exists.
            factions = getFactions();
        } catch (PluginUnavailableException ex) {
            severe("Could not find Factions plugin.");
        }
        
        info(pluginEnabled ? "Enabled" : "Disabled");
    }

    @Override
    public void onDisable() {
        // Plugin disabled.
        pluginEnabled = false;
        
        info("Disabled.");
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (!pluginEnabled) return;
        
        // Get the player.
        final Player player = event.getPlayer();
        
        // We need to delay because the teleport can take a bit...
        Bukkit.getScheduler().scheduleSyncDelayedTask(
            this,
            new Runnable(){
                @Override
                public void run(){
                    // Update the player station at their location.
                    updatePlayerStation(player, player.getLocation());
                }
            },
            40 // 20 ticks = 1 second
        );
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerTeleported(PlayerTeleportEvent event) {
        if (!pluginEnabled) return;
        
        Chunk sourceChunk = event.getFrom().getChunk();
        Chunk targetChunk = event.getTo().getChunk();

        // Only bother to check for a station change when chunks have changed.
        if (sourceChunk.getX() != targetChunk.getX() ||
                sourceChunk.getZ() != targetChunk.getZ()) {
            // Update the player station at the To location.
            updatePlayerStation(event.getPlayer(), event.getTo());
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoined(PlayerJoinEvent event) {
        if (!pluginEnabled) return;
        
        // Update the player station.
        updatePlayerStation(event.getPlayer(), event.getPlayer().getLocation());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!pluginEnabled) return;
        
        Chunk sourceChunk = event.getFrom().getChunk();
        Chunk targetChunk = event.getTo().getChunk();

        // Only bother to check for a station change when chunks have changed.
        if (sourceChunk.getX() != targetChunk.getX() ||
                sourceChunk.getZ() != targetChunk.getZ()) {
            // Update the player station at the To location.
            updatePlayerStation(event.getPlayer(), event.getTo());
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!pluginEnabled || event.getPlayer() == null) return;
        
        // Player left clicked a block?
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
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

                    radioTowers.put(locationToString(towerBase), radius);
                    
                    // TODO: Dump the tower location to a yml file.
                }
            }
        }
    }
    
    private String getWildernessID(Location location) {
        Chunk chunk = location.getChunk();
        return (chunk != null) ? ("Wilderness_" + chunk.getX() + "_" + chunk.getZ()) : null;
    }
    
    private boolean setMusicInWilderness(final Player player, String[] args) {
        if (!pluginEnabled) return false;
        
        String regionName = getWildernessID(player.getLocation());
        
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
                            setTowerStation(player, url);
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
        if (!pluginEnabled || !isFactionsEnabled()) return false;
        
        final Faction faction = getFactionAtLocation(player);
        
        if (faction == null) {
            player.sendMessage("Faction did not exist.");
            return false;
        }
        
        String regionName = faction.getTag();

        boolean isWilderness = false;
        
        if (regionName != null) {
            isWilderness = regionName.contains("Wilderness");
        }
        
        // Not a safe zone, war zone, or wilderness?
        if (!(faction.isSafeZone() || faction.isWarZone() || isWilderness)) {
            // Player was not the faction admin?
            if (!isPlayerFactionAdmin(faction, player)) {
                player.sendMessage("This is not your faction land.");
                return false;
            }
        }
        
        // Not in wilderness.
        if (!isWilderness) {
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
                            setFactionStation(faction, player, url);
                        }
                    },
                    40 // 20 ticks = 1 second
                );

                return true;
            }
        }
        
        return false;
    }
    
    private List<StreamInfo> findMusic(CommandSender sender, String searchString) {
        List<StreamInfo> streams = searchShoutcast(searchString);
        if (streams.size() > 0) {
            int i = 0;
            for (StreamInfo result : streams) {
                sender.sendMessage("[" + (i++) + "] " + result.Title);
            }
        }
        return streams;
    }
    
    private boolean pickMusic(final Player player, List<StreamInfo> streams, int index) {
        boolean stationSet = false;
        if (streams.size() > 0) {
            String ipPort = getStream(streams, index);
            String[] args = new String[] {ipPort};

            System.out.println("Setting music stream [" + index + "]: " + ipPort);

            // Set music on faction land.
            stationSet = setMusicOnFactionLand(player, args);

            // Has no faction here or no plugin?
            if (!stationSet) {
                // Set music in wg region.
                stationSet = setWorldGuardStation(player, args);

                // Has no region here or no plugin?
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
        return stationSet;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (!pluginEnabled) return false;
        
        final Player player;

        if (sender instanceof Player) {
            player = (Player)sender;
        } else {
            sender.sendMessage("MusicService: Command can only be run by a player.");
            return false;
        }

        if (cmd.getName().equalsIgnoreCase("setmusic")) {
            if (args.length > 0) {
                // Set music on faction land.
                boolean stationSet = setMusicOnFactionLand(player, args);
                
                // Has no faction here or no plugin?
                if (!stationSet) {
                    // Set music in wg region.
                    stationSet = setWorldGuardStation(player, args);
                    
                    // Has no region here or no plugin?
                    if (!stationSet) {
                        // Set music in the wilderness.
                        stationSet = setMusicInWilderness(player, args);
                        
                        // Only fails without a tower.
                        if (!stationSet) {
                            sender.sendMessage("No tower here.");
                        }
                    }
                }
                
                return stationSet;
            }
            else {
                player.sendMessage("Command lacked URL argument, e.g. ip:port");
            }
        }
        else if (cmd.getName().equalsIgnoreCase("reloadmusic")) {
            player.sendMessage("Reloading music.");
            
            // Unset their music file.
            noPlayerMusic(player);

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
        else if (cmd.getName().equalsIgnoreCase("findmusic")) {
            if (args.length > 0) {
                // Combine the arguments into one string.
                String searchString = StringEscapeUtils.escapeJava(Arrays.toString(args));
                
                // Find the music and get the streams.
                List<StreamInfo> streams = findMusic(sender, searchString);
                
                // Track the search results for this player.
                playerSearches.put(player.getName(), streams);
                return true;
            }
        }
        else if (cmd.getName().equalsIgnoreCase("pickmusic")) {
            String playerName = player.getName();
            if (playerSearches.containsKey(playerName)) {
                List<StreamInfo> streams = playerSearches.get(playerName);
                boolean stationSet = pickMusic(player, streams, Integer.parseInt(args[0]));
                playerSearches.put(player.getName(), null);
                return stationSet;
            } else {
                player.sendMessage("MusicService: First use: /findmusic <searchstring>");
            }
        }

        return false;
    }

    //
    // Music Routines
    //
    
    private void setMusicStation(Player player, String url) {
        if (!pluginEnabled) return;
        
        // Get the player name.
        String playerName = player.getName();

        // Establish the player file name.
        //String filename = wwwRoot + playerName + ".html";

        File playerMusicFile = new File(musicDirectory, playerName + ".html");

        try {
            String newUrl = "http://" + url.replace("http://", "");
            
            File musicHtmlFile = new File(musicDirectory, "music.html");
            
            //BufferedReader reader = new BufferedReader(new FileReader(wwwRoot + "music.html"));
            
            BufferedReader reader = new BufferedReader(new FileReader(musicHtmlFile));
            PrintWriter writer = new PrintWriter(new FileWriter(playerMusicFile));
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replace("#URL", newUrl);
                line = line.replace("#USER", playerName);
                line = line.replace("#RELATIVE", relativeUrl);
                
                writer.println(line);
            }

            reader.close();
            writer.close();
           
            File txtFile = new File(musicDirectory, playerName + ".txt");
            
            //String txtFile = wwwRoot + playerName + ".txt";
            FileWriter fstream = new FileWriter(txtFile);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(" ");
            out.close();
        }
        catch (IOException e) {
            severe(String.format("Failed to write the music service file: %s", playerMusicFile.getPath()));
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
        
        // Get the player name.
        String playerName = player.getName();

        // Establish the player file name.
        File playerMusicFile = new File(musicDirectory, playerName + ".html");

        // A file for refreshing the client via AJAX.
        File refreshedFile = new File(musicDirectory, playerName + ".txt");

        try {
            boolean deleted = false;

            if ((refreshedFile.exists())) {
                // Delete the txt file to indicate the need to refresh.
                // If it fails to succeed then this html already exists, don't create it.
                if (deleteFile(refreshedFile.getPath())) {
                    deleted = true;
                }
            }
            else {
                deleted = true;
            }

            if (deleted) {
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
        catch (IOException e) {
            severe(String.format("Failed to write the music service file: %s", playerMusicFile.getPath()));
        }
    }

    //
    // File operations.
    //
    
    private boolean deleteFile(String file) {
        return (new File(file)).delete();
    }
    
    //
    // Logging
    //
    
    public static void info(String msg) {
        logger.log(Level.INFO, String.format("%s %s", LOG_PREFIX, msg));
    }
    
    public static void severe(String msg) {
        logger.log(Level.SEVERE, String.format("%s %s", LOG_PREFIX, msg));
    }
    
    //
    // Factions API calls
    //
    private Plugin getFactions() throws PluginUnavailableException {
        Plugin plugin = getServer().getPluginManager().getPlugin("Factions");
        
        if (plugin == null) {
            throw new PluginUnavailableException();
        }
        
        return plugin;
    }
    
    private boolean isFactionsEnabled() {
        return factions != null && factions.isEnabled();
    }

    private void updatePlayerStation(Player player, Location location) {
        if (!pluginEnabled) return;
        
        if (player != null && location != null) {
            // Has factions?
            if (isFactionsEnabled()) {
                // Change station for faction. Fails on wilderness / no faction.
                if (!changeStation(getFactionAtLocation(location), player)) {
                    info("must be wg region or wilderness...");
                    
                    // Change station for wg region. Fails on no region.
                    if (!handleWorldGuard(player)) {
                        info("wilderness?");
                        
                        // Must be wilderness, check for tower.
                        handleWilderness(player);
                    }
                }
            }
            // No factions, try worldguard first then.
            else {
                // Change station for wg region. Fails on no region.
                if (!handleWorldGuard(player)) {
                    // Must be wilderness, check for tower.
                    handleWilderness(player);
                }
            }
        }
    }

    private Faction getFactionAtLocation(Player player) {
        // Get a faction located at the player's position.
        return Board.getFactionAt(new FLocation(player));
    }
    
    private Faction getFactionAtLocation(Location location) {
        // Get a faction located at the player's position.
        return Board.getFactionAt(new FLocation(location));
    }

    private boolean isPlayerFactionAdmin(Faction faction, Player player) {
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

    private boolean setFactionStation(Faction faction, Player player, String url) {
        if (!pluginEnabled) return false;
        
        // Get the faction region name.
        String regionName = faction.getTag();

        if (regionName != null) {
            // Set the URL for this faction in the config.
            getConfig().set(regionName, url);
            saveConfig();

            player.sendMessage(String.format("Music Station: %s", url));

            // Change the player's music to the URL specified immediately.
            changePlayerMusic(player, url);

            return true;
        }
        
        return false;
    }
    
    private void setTowerStation(Player player, String url) {
        if (!pluginEnabled) return;
        
        for (String locationString : radioTowers.keySet()) {
            int radius = radioTowers.get(locationString);
            Location location = stringToLocation(locationString);
            
            info(String.format("setTowerStation - TowerLocation: %f, %f TowerRadius: %d", location.getX(), location.getZ(), radius));
            
            //Location playerLocation = player.getLocation();
            //info(String.format("playerLocation: %f, %f", playerLocation.getX(), playerLocation.getZ()));
            
            if (towerIntersects(location, radius, player.getLocation())) {
                if (url != null) {
                    radioStations.put(locationString, url);

                    // Change the player music to the url for this faction.
                    changePlayerMusic(player, url);
                }
                return;
            }
        }
    }
    
    private String locationToString(Location location) {
        return location.getWorld().getName() + "|" + location.getX() + "|" + location.getY() + "|" + location.getZ();
    }
    
    private Location stringToLocation(String locationString) {
        String[] tokens = locationString.split("\\|");
        
        double x = Double.parseDouble(tokens[1]);
        double y = Double.parseDouble(tokens[2]);
        double z = Double.parseDouble(tokens[3]);
        
        World world = getServer().getWorld(tokens[0]);
        
        return new Location(world, x, y, z);
    }
    
    private void handleWilderness(Player player) {
        if (!pluginEnabled) return;
        
        for (String locationString : radioTowers.keySet()) {
            int radius = radioTowers.get(locationString);
            Location location = stringToLocation(locationString);
            
            //info(String.format("handleWilderness - TowerLocation: %f, %f TowerRadius: %d", location.getX(), location.getZ(), radius));
            
            if (towerIntersects(location, radius, player.getLocation())) {
                if (radioStations.containsKey(locationString)) {
                    String url = radioStations.get(locationString);
                    
                    if (url != null) {
                        // Change the player music to the url for this faction.
                        changePlayerMusic(player, url);
                    }
                }
                return;
            }
        }
        
        // Stop the music.
        noPlayerMusic(player);
    }
    
    private boolean changeStation(Faction faction, Player player) {
        if (!pluginEnabled) return false;
        
        // Faction exists?
        if (faction != null) {
            // Get the faction region name.
            String regionName = faction.getTag();
            
            // Has region and it's not Wilderness, therfore it is faction land?
            if (regionName != null) {
                if (!regionName.contains("Wilderness")) {
                    // Has player?
                    if (player != null) {
                        // Config holds an entry for the faction?
                        if (getConfig().contains(regionName)) {
                            // Get the faction's url.
                            String url = (String) getConfig().get(regionName);

                            if (url != null) {
                                //info(String.format("Changing station for %s in %s to %s", player.getName(), regionName, url));
                                
                                // Change the player music to the url for this faction.
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
                }
            }
        }
        
        return false;
    }
    
    private boolean handleWorldGuard(Player player) {
        if (!pluginEnabled) return false;

        // Has player?
        if (player != null) {
            try {
                info("handling wg region");
                
                ProtectedRegion stationRegion = getWorldGuardRegionAt(player);
                
                if (stationRegion != null) {
                    info("worldguard region: " + stationRegion);
                    
                    // Config holds an entry for the faction?
                    if (getConfig().contains(stationRegion.getId())) {
                        // Get the faction's url.
                        String url = (String) getConfig().get(stationRegion.getId());

                        if (url != null) {
                            //info(String.format("Changing station for %s in %s to %s", player.getName(), regionName, url));

                            // Change the player music to the url for this faction.
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
            } catch (PluginUnavailableException ex) {
                return false;
            }
        }

        return false;
    }
    
    private boolean towerIntersects(Location towerLocation, int radius, Location playerLocation) {
        int px = (int)playerLocation.getX();
        int pz = (int)playerLocation.getZ();
        
        int tx = (int)towerLocation.getX();
        int tz = (int)towerLocation.getZ();
        
        Rectangle playerRect = new Rectangle(px, pz, 1, 1);
        Rectangle rangeRect = new Rectangle(tx - radius, tz - radius, radius * 2, radius * 2);
        
        return playerRect.intersects(rangeRect);
    }
    
    //
    // WorldGuard regions support
    //
    
    private WorldGuardPlugin getWorldGuard() throws PluginUnavailableException {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
        
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            throw new PluginUnavailableException();
        }
        
        return (WorldGuardPlugin)plugin;
    }
    
    private ProtectedRegion getWorldGuardRegionAt(Player player) throws PluginUnavailableException {
        // Get the world guard plugin.
        WorldGuardPlugin worldGuard = getWorldGuard();

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
    
    private boolean setWorldGuardStation(Player player, String[] args) {
        if (!pluginEnabled) return false;
        
        try {
            ProtectedRegion stationRegion = getWorldGuardRegionAt(player);
            
            // Has a station region?
            if (stationRegion != null) {
                // User chose to blank out this parcel.
                if (args[0].equalsIgnoreCase("blank")) {
                    info(String.format("Removing music station for region: %s", stationRegion.getId()));

                    // Set the URL for this region in the config to nothing.
                    getConfig().set(stationRegion.getId(),  "");
                    saveConfig();

                    // Stop the music.
                    noPlayerMusic(player);

                    player.sendMessage("Removed music station.");
                }
                // User assigning station.
                else {
                    // Set the URL for this region in the config.
                    getConfig().set(stationRegion.getId(), args[0]);
                    saveConfig();

                    player.sendMessage(String.format("WorldGuard Music Station: %s", args[0]));

                    // Change the player's music to the URL specified immediately.
                    changePlayerMusic(player, args[0]);
                }
                
                return true;
            }
        }
        catch (PluginUnavailableException ex) {
            player.sendMessage("Could not set station, you do not own this region.");
        }
        
        return false;
    }
    
    private String getStream(List<StreamInfo> streams, int index) {
        return streams.get(index).Stream;
    }
    
    private List searchShoutcast(String searchString) {
        // List of streams.
        List streams = new ArrayList();
        
        // The search url.
        String search = String.format(shoutcastSearchUrl, searchString);
        
        // Get the search results.
        Object[] results = httpGet(search);
        
        // Find all of the streams.
        for (Object result : results) {
            // A search result line.
            String line = (String)result;
            
            // Valid stream .pls url?
            if (line.contains("a href") &&          // Is a link?
                line.contains("yp.shoutcast") &&    // a shoutcast .pls?
                !line.contains("ttsl.html"))        // not the ttsl.html
            {
                
                // Parse the .pls for the stream info.
                StreamInfo info = getStreamInfo(line.split("\"")[1]);
                
                if (info != null) {
                    streams.add(info);
                }
            }
        }
        
        return streams;
    }
    
    private StreamInfo getStreamInfo(String urlString) {
        StreamInfo info = new StreamInfo();
        Object[] results = httpGet(urlString);
        for (Object result : results) {
            String line = (String)result;
            if (line.startsWith("File1")) {
                info.Stream = line.split("=")[1];
            }
            else if (line.startsWith("Title1")) {
                info.Title = line.split("=")[1];
                break;
            }
        }
        return info;
    }
    
    private Object[] httpGet(String urlString) {
        List arr = new ArrayList();
        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            BufferedReader in =
                    new BufferedReader(
                        new InputStreamReader(
                            conn.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                arr.add(inputLine);
            }
            in.close();
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
        return arr.toArray();
    }
}
