package blakesteel.MusicService;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Palisade
 */
public class WebServer extends Thread {
    private static final String LOG_PREFIX = "[MusicService] ";
    private static final Logger logger = Logger.getLogger("Minecraft");
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
    private final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private final String serverSoftware = "bukkit MusicService";
    private final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(5);
    private boolean running = false;    // True if main server thread running.
    private ThreadPoolExecutor threadPool = null;   // Thread pool for handlers.
    private int port;                   // The webserver port.
    private int poolSize = 2;           // Starts at 2 connections handled.
    private int maxPoolSize = 5;        // Up to 5 connections at a time handled.
    private long keepAliveTime = 10;    // Keep-alive time defaults to 10.
    private ServerSocket serversocket = null; // The server accept socket.
    private Map<String, WebMemoryFile> memoryMap = new ConcurrentHashMap<String, WebMemoryFile>();
    
    public void setMemoryValue(String path, WebMemoryFile memoryFile) {
        memoryMap.put(path, memoryFile);
    }
    
    public WebMemoryFile getMemoryValue(String path) {
        return memoryMap.get(path);
    }

    public WebServer() {
        threadPool = new ThreadPoolExecutor(poolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS, queue);
    }
    
    public void start(int listen_port) {
        port = listen_port;
        this.start();
    }
    
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
    
    public void shutdown() {
        // Gracefully shut down any threads spawned by the thread pool.
        threadPool.shutdown();
        
        // Release the main accept thread.
        running = false;
        
        if (serversocket != null) {
            try {
                serversocket.close();
            } catch (IOException ex) {
            }
        }
    }

    @Override
    public void run() {
        try {
            info("Hosting on port: " + Integer.toString(port));
            serversocket = new ServerSocket(port);
            running = true;
        } catch (Exception e) {
            severe("Error: " + e.getMessage());
            return;
        }

        while (running) {
            try {
                Socket connectionsocket = serversocket.accept();
                
                if (connectionsocket != null && connectionsocket.isConnected()) {
                    final InetAddress client = connectionsocket.getInetAddress();
                    final BufferedReader input = new BufferedReader(new InputStreamReader(connectionsocket.getInputStream()));
                    final DataOutputStream output = new DataOutputStream(connectionsocket.getOutputStream());

                    // Spawn off a thread from the threadpool for each handler.
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                http_handler(client, input, output);
                            }
                            catch (Exception ex) {
                                debug("http_handler Communication Error: " + ex.getMessage());
                            }
                        }
                    });
                }
            } catch (Exception e) {
                debug("Error:" + e.getMessage());
            }
        }
    }

    private void logstatus(InetAddress client, int code) {
        // In DEBUG to reduce log spam.
        //debug("SERVER -> " + client.getHostName() + ": " + getStatusString(code));
    }

    private void sendErrorResponse(InetAddress client, DataOutputStream output, int code) {
        try {
            output.writeBytes(construct_http_header(client, null, code, -1));
            output.close();
        } catch (Exception e) {
            severe("Error: " + e.getMessage());
        }
    }

    /*private String getNotFoundHtml(String path) {
        return "<!DOCTYPE html>\r\n<html>\r\n\r\n    <head>\r\n        <title>Not found</title>\r\n        <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>    \r\n    </head>\r\n    <body>\r\n                            <h1>Not found</h1>\r\n            <p>\r\n                The file "
                + path + " does not exist\r\n            </p>\r\n            </body>\r\n</html>\r\n";
    }*/

    private void http_handler(InetAddress client,
                              BufferedReader input,
                              DataOutputStream output) throws IOException {
        int method = 0;
        int start = 0;
        int end = 0;
        String path;

        try {
            try {
                // Read the HTTP request.
                String tmp = input.readLine();
                //debug(client.getHostName() + " -> SERVER: " + tmp);

                debug("REQUEST: " + tmp);

                // Which method are we handling? GET, HEAD, POST?
                if (tmp.matches("(?i)GET.*")) {
                    method = 1;
                }
                else if (tmp.matches("(?i)HEAD.*")) {
                    method = 2;
                }
                else if (tmp.matches("(?i)POST.*")) {
                    method = 3;
                }

                // Unimplemented command?
                if (method == 0) {
                    sendErrorResponse(client, output, 501);
                    return;
                }

                // Skip whitespace.
                for (int a = 0; a < tmp.length(); a++) {
                    if (tmp.charAt(a) == ' ' && start != 0) {
                        end = a;
                        break;
                    }

                    if (tmp.charAt(a) == ' ' && start == 0) {
                        start = a;
                    }
                }

                // Get the path requested by the HTTP command.
                path = tmp.substring(start + 2, end);
            }
            catch (Exception e) {
                debug("http_handler Parse Error: " + e.getMessage());
                
                // 400 Bad request!
                sendErrorResponse(client, output, 400);
                return;
            }

            // Path empty? They're loading the index.html then.
            if (path.equals("")) {
                path = "index.html";
            }

            WebMemoryFile memoryFile = memoryMap.get(path);
            
            // Valid memory file?
            if (memoryFile != null && memoryFile.getEnabled()) {
                // Determine MIME type from path.
                int mimeType = getMimeType(path);

                // Write the 200 OK header.
                output.writeBytes(construct_http_header(client, null, 200, mimeType));

                // If GET or POST, send the file contents.
                if (method == 1 || method == 3) {
                    output.write(memoryFile.getBytes());
                    debug("Sent: " + path);
                }
            }
            // Not found?
            else {
                output.writeBytes(construct_http_header(client, null, 404, -1));
                output.write(0);
                debug("404: " + path);
            }
        }
        catch (Exception ex) {
            // In DEBUG to reduce log spam.
            debug("Error: " + ex.getMessage());

            // 404 error!
            output.writeBytes(construct_http_header(client, null, 404, -1));

            // FIXME: I tried setting type to 0 (html) and sending a 404
            // html but it prepended a Y character to the top for some reason.
            // Now instead I just write a 0 and close the connection,
            // which makes the browser display its own 404 notice.
            // Though, it'd be nice if we could have a custom one.
            output.write(0);
        }
        finally {
            // Close the output stream if any. Close connection first.
            if (output != null) output.close();
        }
    }
    
    private int getMimeType(String path) {
        // Default MIME type to html.
        int mimeType = 0;

        // Pick the MIME type.
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            mimeType = 1;
        } else if (path.endsWith(".gif")) {
            mimeType = 2;
        } else if (path.endsWith(".zip")) {
            mimeType = 3;
        } else if (path.endsWith(".swf")) {
            mimeType = 4;
        } else if (path.endsWith(".js")) {
            mimeType = 5;
        } else if (path.endsWith(".css")) {
            mimeType = 6;
        } else if (path.endsWith(".txt")) {
            mimeType = 7;
        }
        return mimeType;
    }
    
    private String getStatusString(int code) {
        String s = "500 Internal Server Error";
        switch (code) {
            case 200:
                s = "200 OK";
                break;
            case 400:
                s = "400 Bad Request";
                break;
            case 403:
                s = "403 Forbidden";
                break;
            case 404:
                s = "404 Not Found";
                break;
            case 500:
                s = "500 Internal Server Error";
                break;
            case 501:
                s = "501 Not Implemented";
                break;
        }
        return s;
    }
    
    private String formatDate(Date date, String pattern) {
        if (date == null) throw new IllegalArgumentException("date is null");
        if (pattern == null) throw new IllegalArgumentException("pattern is null");
        SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.US);
        formatter.setTimeZone(GMT);
        return formatter.format(date);
    }
	
    private String getLastModified(String filename) {
            long t = (new File(filename)).lastModified();
            return formatDate(new Date(t), PATTERN_RFC1123);
    }

    private String getNow() {
        return formatDate(new Date(), PATTERN_RFC1123);
    }

    private String construct_http_header(InetAddress client,
                                         String filename,
                                         int return_code,
                                         int file_type) {
        logstatus(client, return_code);
        
        String s = "HTTP/1.0 ";
        s += getStatusString(return_code);
        s += "\r\n";
        s += "Connection: close\r\n";
        s += "Server: " + serverSoftware + "\r\n";

        switch (file_type) {
            case -1:
                break;
            case 1:
                s += "Content-Type: image/jpeg\r\n";
                break;
            case 2:
                s += "Content-Type: image/gif\r\n";
                break;
            case 3:
                s += "Content-Type: application/x-zip-compressed\r\n";
                break;
            case 4:
                s += "Content-Type: application/x-shockwave-flash\r\n";
                break;
            case 5:
                s += "Content-Type: text/javascript\r\n";
                break;
            case 6:
                s += "Content-Type: text/css\r\n";
                break;
            case 7:
                s += "Content-Type: text/plain\r\n";
                break;
            case 8:
                s += "Content-Type: multipart/form-data";
                break;
            default:
                s += "Content-Type: text/html\r\n";
                break;
        }

        s += "Last-Modified: " + getNow() + "\r\n";
        //if (filename != null) {
        //    s += "Last-Modified: " + getLastModified(filename) + "\r\n";
        //}
        
        return s + "\r\n";
    }
}