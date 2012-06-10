package blakesteel.MusicService;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.getspout.spoutapi.SpoutManager;

/**
 * @author Palisade
 */
public class SupportSpout {
    static public Plugin getPlugin(Server server)
            throws PluginUnavailableException {
        Plugin plugin = server.getPluginManager().getPlugin("Spout");
        
        if (plugin == null) {// || !(plugin instanceof SpoutPlugin)) {
            throw new PluginUnavailableException();
        }
        
        return plugin;
    }
    
    static public void playMusic(Plugin plugin, String url, Location loc, int distance, int volume) {
        //SpoutManager.getSoundManager().playGlobalCustomMusic(plugin, url, true, loc, distance, volume);
        SpoutManager.getSoundManager().playGlobalCustomSoundEffect(plugin, url, true, loc, distance, volume);
    }
    
    static public void scheduleNextSong(final int index,
                                        int dwell,
                                        final Plugin plugin,
                                        final List<SpoutMusic> musicList,
                                        final Location loc,
                                        final int distance,
                                        final int volume)
    {
        if (musicList.size() > 0 && index < musicList.size()) {
            /*new Timer(dwell * 1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    // Get the song.
                    SpoutMusic song = musicList.get(index);

                    // Play the song.
                    SupportSpout.playMusic(plugin, song.Url, loc, distance, volume);

                    // Schedule the next song.
                    scheduleNextSong(index + 1, song.DurationInSeconds, plugin,
                        musicList, loc, distance, volume);

                    MusicService.info("Next song playing...");
                }  
            }).start();*/
            
            // Assume a fifteen second load delay between songs. I'm eyeballing this
            // because there's no event that tells me when the song is done downloading
            // or finished playing. That I know of.
            final int loadDelay = 15;
            
            Bukkit.getScheduler().scheduleAsyncDelayedTask(
                plugin,
                new Runnable() {
                    @Override
                    public void run() {
                        // Get the song.
                        SpoutMusic song = musicList.get(index);
                        
                        // Play the song.
                        SupportSpout.playMusic(plugin, song.Url, loc, distance, volume);
                        
                        // Schedule the next song.
                        scheduleNextSong(index + 1, song.DurationInSeconds, plugin,
                                musicList, loc, distance, volume);
                        
                        MusicService.info("Next song playing...");
                    }
                },
                20 * (dwell + loadDelay) // 20 ticks = 1 second
            );
        }
    }
    
    /*static private long getOggDuration(File file) {
        try {
            VorbisFile vf = new VorbisFile(file.getAbsolutePath());
            int durationMs = (int)Math.round((vf.time_total(-1)) * 1000);
            return new Long(durationMs * 1000);
        } catch (JOrbisException ex) {
            MusicService.severe("Error: " + ex.getMessage());
        }
        return -1;
    }*/
    
    static public void playMusicList(Plugin plugin,
                                     List<SpoutMusic> musicList,
                                     Location loc,
                                     int distance,
                                     int volume)
    {
        //long durationSeconds = getOggDuration(new File("c:\\hydrate.ogg"));
        //MusicService.info("durationSeconds: " +  durationSeconds);
        
        // Has a list of songs?
        if (musicList.size() > 0) {
            // Get the first song.
            SpoutMusic firstSong = musicList.get(0);
            
            // Start playing the first song in the list.
            SupportSpout.playMusic(plugin, firstSong.Url, loc, distance, volume);
            
            // Schedule the next song to play after this song is completed.
            scheduleNextSong(1, firstSong.DurationInSeconds, plugin, musicList, loc, distance, volume);
            
            MusicService.info("First song playing...");
        }
    }
}
