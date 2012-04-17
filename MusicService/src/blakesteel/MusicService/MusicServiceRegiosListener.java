package blakesteel.MusicService;

import couk.Adamki11s.Regios.CustomEvents.*;
import couk.Adamki11s.Regios.Main.Regios;
import couk.Adamki11s.Regios.Regions.Region;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;

/**
 * @author Palisade
 */
public class MusicServiceRegiosListener extends RegionEventListener {
        private MusicService plugin;
        private Regios regios;

        static final Logger logger = Logger.getLogger("Minecraft");
        static final String LOG_PREFIX = "[MusicService] ";
        
        String[] commands = new String[] {
            "set",
            "cancel",
            "modify", // regionName
            "modify cancel",
            "modify confirm",
            "expandmax", // regionName
            "expandup", //regionName, int
            "expanddown" // regionName, int
        };

        public static void debug(String msg) {
            if (MusicService.DEBUG) {
                logger.log(Level.INFO, String.format("%s(DEBUG) %s", LOG_PREFIX, msg));
            }
        }

        public static void info(String msg) {
            logger.log(Level.INFO, String.format("%s %s", LOG_PREFIX, msg));
        }

        public static void severe(String msg) {
            logger.log(Level.SEVERE, String.format("%s %s", LOG_PREFIX, msg));
        }
        public MusicServiceRegiosListener(Regios regios, MusicService plugin) {
            this.plugin = plugin;
            this.regios = regios;
            debug("RegiosListener Registered");
        }
        
	@Override
	public void onRegionEnter(RegionEnterEvent event) {
            Region region = event.getRegion();
            Player player = event.getPlayer();
            if (player != null && region != null) {
                debug(String.format("onRegiosPlayerMoveChunk: %s %s %s", player.getName(), region.getName(), region.isPlayerInRegion(player)));
                plugin.onRegiosPlayerMoveChunk(player, region.getName(), region.isPlayerInRegion(player));
            }
	}
 
	@Override
	public void onRegionExit(RegionExitEvent event) {
            Region region = event.getRegion();
            Player player = event.getPlayer();
            if (player != null) {
                if (region != null) {
                    debug(String.format("onRegiosPlayerMoveChunk: %s %s false", player.getName(), region.getName()));
                    plugin.onRegiosPlayerMoveChunk(player, region.getName(), false);
                }
                else {
                    debug(String.format("onRegiosPlayerMoveChunk: %s null false", player.getName()));
                    plugin.onRegiosPlayerMoveChunk(player, null, false);
                }
            }
	}
 
	@Override
	public void onRegionCreate(RegionCreateEvent event) {
            Region region = event.getRegion();
            Player player = event.getPlayer();
            if (player != null && region != null) {
                debug(String.format("onRegiosPlayerMoveChunk: %s %s %s", player.getName(), region.getName(), region.isPlayerInRegion(player)));
                plugin.onRegiosPlayerMoveChunk(player, region.getName(), region.isPlayerInRegion(player));
            }
        }
 
	@Override
	public void onRegionDelete(RegionDeleteEvent event) {
            Region region = event.getRegion();
            Player player = event.getPlayer();
            if (player != null) {
                if (region != null) {
                    debug(String.format("onRegiosPlayerMoveChunk: %s %s false", player.getName(), region.getName()));
                    plugin.onRegiosPlayerMoveChunk(player, region.getName(), false);
                }
                else {
                    debug(String.format("onRegiosPlayerMoveChunk: %s null false", player.getName()));
                    plugin.onRegiosPlayerMoveChunk(player, null, false);
                }
            }
        }
   
	@Override
	public void onRegionRestore(RegionRestoreEvent event) {
            Region region = event.getRegion();
            Player player = event.getPlayer();
            if (player != null && region != null) {
                debug(String.format("onRegiosPlayerMoveChunk: %s %s %s", player.getName(), region.getName(), region.isPlayerInRegion(player)));
                plugin.onRegiosPlayerMoveChunk(player, region.getName(), region.isPlayerInRegion(player));
            }
	}
 
	@Override
	public void onRegionCommand(RegionCommandEvent event) {
            // Not implemented.
	}

        @Override
        public void onRegionLightningStrike(RegionLightningStrikeEvent rlse) {
            // Not implemented.
        }

        @Override
        public void onRegionLoad(RegionLoadEvent rle) {
            // Not implemented.
        }

        @Override
        public void onRegionBackup(RegionBackupEvent rbe) {
            // Not implemented.
        }
}