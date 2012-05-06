package blakesteel.MusicService;

import java.awt.Rectangle;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author Palisade
 */
public class Utility {
    static public String getWildernessID(Location location) {
        Chunk chunk = location.getChunk();
        return (chunk != null) ? ("Wilderness_" + chunk.getX() + "_" + chunk.getZ()) : null;
    }
    
    static public boolean deleteFile(String file) {
        return (new File(file)).delete();
    }
    
    static public boolean delete(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) delete(c);
        }
        if (!f.delete())
            return false;
        return true;
    }
    
    static public String locationToString(Location location) {
        return location.getWorld().getName() + "_" + location.getX() +
                "_" + location.getY() + "_" + location.getZ();
    }
    
    static public Location stringToLocation(Server server, String locationString) {
        String[] tokens = locationString.split("_");
        World world = server.getWorld(tokens[0]);
        double x = Double.parseDouble(tokens[1]);
        double y = Double.parseDouble(tokens[2]);
        double z = Double.parseDouble(tokens[3]);
        return new Location(world, x, y, z);
    }
    
    static public boolean towerIntersects(Location towerLocation, int radius, Location playerLocation) {
        int px = (int)playerLocation.getX();
        int pz = (int)playerLocation.getZ();
        
        int tx = (int)towerLocation.getX();
        int tz = (int)towerLocation.getZ();
        
        Rectangle playerRect = new Rectangle(px, pz, 1, 1);
        Rectangle rangeRect = new Rectangle(tx - radius, tz - radius, radius * 2, radius * 2);
        
        return playerRect.intersects(rangeRect);
    }
    
    static private String getStringBetween(String line, String startPattern, String endPattern) {
        String string = line.toLowerCase();
        int start = string.indexOf(startPattern) + startPattern.length();
        int end = string.indexOf(endPattern);
        return line.substring(start, end);
    }
    
    static public StreamInfo getShoutcastStreamInfo(String urlString) {
        StreamInfo info = new StreamInfo();
        info.Type = StreamInfo.StreamType.Shoutcast;
        Object[] results = httpGet(urlString);
        if (results != null) {
            for (Object result : results) {
                String line = (String)result;
                if (line != null) {
                    if (line.startsWith("File1")) {
                        info.Host = line.split("=")[1];
                    }
                    else if (line.startsWith("Title1")) {
                        info.Title = line.split("=")[1];
                        break;
                    }
                }
            }
            return info;
        }
        return null;
    }
    
    static public StreamInfo getIcecastStreamInfo(String urlString) {
        StreamInfo info = new StreamInfo();
        info.Type = StreamInfo.StreamType.Icecast;
        Object[] results = httpGet(urlString);
        if (results != null) {
            for (Object result : results) {
                String line = (String)result;
                if (line != null) {
                    if (line.contains("<title>")) {
                        info.Title = getStringBetween(line, "<title>", "</title>");
                    }
                    if (line.contains("<location>")) {
                        info.Host = getStringBetween(line, "<location>", "</location>");
                        break;
                    }
                }
            }
            return info;
        }
        return null;
    }
    
    static public Object[] httpGet(String urlString) {
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
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            MusicService.severe("Error: " + sw.toString());
        }
        return arr.toArray();
    }

    static public byte[] getBytes(URI uri) throws MalformedURLException, IOException {
        ByteArrayOutputStream bais = new ByteArrayOutputStream();
        InputStream is = null;
        URL url = uri.toURL();
        
        try {
            is = url.openStream();
            byte[] byteChunk = new byte[4096];
            int n;
            while ((n = is.read(byteChunk)) > 0) {
                bais.write(byteChunk, 0, n);
            }
        }
        finally {
            if (is != null) { is.close(); }
        }
        
        return bais.toByteArray();
    }
    
    static public byte[] httpGetBytes(URI uri) {
        byte[] bytes = null;
        try {
            bytes = getBytes(uri);
        } catch (IOException ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            MusicService.severe("Error: " + sw.toString());
        }
        return bytes;
    }
    
    static public void extractResource(Class cls, String resource, File dest)
            throws IOException {
        String destination = dest.toString();
        
        InputStream inputStream = cls.getResourceAsStream("/contents/" + resource);
        
        if (inputStream != null) {
            OutputStream out = new FileOutputStream(new File(destination, resource));

            byte buf[] = new byte[1024];
            int len;

            while ((len = inputStream.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            out.close();
            inputStream.close();
        }
    }
    
    static public void rebuildMusicFolder(Class cls, File wwwRootDirectory) throws Exception {
        // The root www path.
        File musicDir = new File(wwwRootDirectory, "music");

        // Images directory in mc/plugins/MusicService/www/music/images path
        File imagesDir = new File(musicDir.getPath(), "images");

        // Create the www root if it doesn't already exist.
        wwwRootDirectory.mkdir();
        
        // Delete the music directory.
        Utility.delete(musicDir);

        // Create the music directory again.
        musicDir.mkdir();

        // Create the images directory again.
        imagesDir.mkdir();

        // Extract resources.
        extractResource(cls, "green.png", imagesDir);
        extractResource(cls, "yellow.png", imagesDir);
        extractResource(cls, "music.html", musicDir);
        extractResource(cls, "snel.swf", musicDir);
        extractResource(cls, "player2.swf", musicDir);
        extractResource(cls, "swf.js", musicDir);
    }
    
    public static byte[] getBytesFromInputStream(InputStream is) throws IOException {
        long length = is.available();

        if (length > Integer.MAX_VALUE) {
            throw new IOException("File too large.");
        }

        byte[] bytes = new byte[(int) length];

        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        if (offset < bytes.length) {
            throw new IOException("Could not completely read file ");
        }

        is.close();
        return bytes;
    }

    static public byte[] loadFromResourceAsBytes(Class cls, String path) throws IOException {
        InputStream inp = cls.getResourceAsStream("/contents/" + path);
        return getBytesFromInputStream(inp);
    }

    static public String loadFromResourceAsString(Class cls, String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream inputStream = cls.getResourceAsStream("/contents/" + path);
        if (inputStream != null) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
            String s;
            try {
                while (null != (s = rd.readLine())) {
                    sb.append(s + "\n");
                }
            } finally {
                rd.close();
            }
        }
        return sb.toString();
    }
    
    static public FileConfiguration configReload(Plugin plugin, File file) {
        File configFile = new File(plugin.getDataFolder(), file.getPath());
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        InputStream defConfigStream = plugin.getResource(file.getPath());
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            config.setDefaults(defConfig);
        }
        return config;
    }
    
    static public ConfigurationSection configCreateSection(FileConfiguration config, String section) {
        if (!config.isConfigurationSection(section)) {
            return config.createSection(section);
        }
        return config.getConfigurationSection(section);
    }
    
    static public ConfigurationSection configGetSection(FileConfiguration config, String section) {
        return config.getConfigurationSection(section);
    }
    
    static public void configSetSectionKeypair(ConfigurationSection section, String key, Object value) {
        section.set(key, value);
    }

    static public Object configGetSectionKeypair(ConfigurationSection section, String key) {
        return section.get(key);
    }
    
    static public void configSave(Plugin plugin, FileConfiguration config, File file) {
        try {
            File configFile = new File(plugin.getDataFolder(), file.getPath());
            config.save(configFile);
            MusicService.info("Saved configuration: " + configFile.getPath());
        } catch (IOException ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            MusicService.severe("Error: " + sw.toString());
        }
    }
}
