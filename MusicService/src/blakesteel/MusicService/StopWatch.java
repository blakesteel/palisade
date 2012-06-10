package blakesteel.MusicService;

/**
 * @author Palisade
 */
public class StopWatch {
    private long startTime = 0;
    private long stopTime = 0;
    private boolean running = false;

    public void start() {
        this.running = true;
        this.startTime = System.currentTimeMillis();
    }

    public void stop() {
        this.running = false;
        this.stopTime = System.currentTimeMillis();
    }

    public long getElapsedMs() {
        if (running) return (System.currentTimeMillis() - startTime);
        else return (stopTime - startTime);
    }

    public long getElapsedSeconds() {
        if (running) return ((System.currentTimeMillis() - startTime) / 1000);
        return ((stopTime - startTime) / 1000);
    }
}