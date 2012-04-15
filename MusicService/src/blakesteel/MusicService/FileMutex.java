package blakesteel.MusicService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.Date;

/**
 * A file based mutual exclusion lock that allows to acquire and release the underlying 
 * resource. On acquiring the lock the file is created with the given contents. The 
 * lock is free on construction.
 * 
 * The lock is also associated with a maximum unused period. If the lock is not 
 * updated during the given period, any other thread waiting to acquire the lock
 * will be given the lock.
 * 
 * The lock is said to be acquired only when the file exists and file last used 
 * timestamp is within unusedMsec. permitted
 * 
 * The read and write operation to the mutex file are synchronized across 
 * different process by encapsulating that with a create & lock operation
 * on a separate file. So before a process can acquire/read mutex (update mutex
 * content & timestamp) , it needs to create & lock another predefined
 * file and also delete the lock file after the operation on mutex are done. This is
 * to ensure that multiple processes/threads dont acquire the mutex at the same time
 */
public class FileMutex {
    /** The lock file name **/
    private String mutex = System.getProperty("java.io.tmpdir") + "app.lck";

    /** Msecs. file can be left unused before another thread acquires the lock File **/
    private long unusedMSec;

    /** file contents telling which component/application is holding the lock **/
    private String mutexContent;

    /** FileName of the lock for mutex **/
    private String lockOnMutex = mutex + ".lck";

    public static final long ONE_SECOND = 1000;
    public static final long ONE_MINUTE = 60 * ONE_SECOND;

    /**
     * @param lockFileContent
     * @param unusedMSec
     */
    public FileMutex(String lockFileContent, long unusedMSec) {
        this.mutexContent = lockFileContent;
        this.unusedMSec = unusedMSec;
    }

    public void acquire() throws InterruptedException {
        try {
            while (isInuseMutex())
                Thread.sleep(ONE_SECOND);
            createMutex();
        } catch (Exception e) {
            throw new InterruptedException("Error acquiring lock: " + e.getMessage());
        }
    }

    public synchronized void release() {
        releaseMutex();
    }

    public boolean tryAcquire() throws InterruptedException {
        try {
            if (!isInuseMutex()) {
                boolean created = createMutex();
                return created;
            }
        } catch (Exception e) {
            throw new InterruptedException("Error acquiring lock: " + e.getMessage());
        }
        return false;
    }

    public boolean update() {
        createMutex();
        return true;
    }

    /**
     * @return
     */
    private boolean isInuseMutex() {
        File lockOnMutexFile = new File(lockOnMutex);
        FileChannel channel = null;
        FileLock lock = null;
        try {
            channel = new RandomAccessFile(lockOnMutexFile, "rw").getChannel();
            lockOnMutexFile.createNewFile();
            try {
                lock = channel.tryLock();
            } catch (OverlappingFileLockException e) {
                // File is already locked in this thread or virtual machine
                return true;
            }

            File mutexFile = new File(mutex);
            if (!mutexFile.exists()) {
                return false;
            }

            Date lockLastUsedDate = new Date(mutexFile.lastModified());
            Date oldestAllowedLockUsedDate = 
                         new Date(System.currentTimeMillis() - unusedMSec);
            if (lockLastUsedDate.before(oldestAllowedLockUsedDate)) {
                return false;
            }
        } catch (Exception e) {
            System.out.println("Error acquiring lock: " + e.getMessage());
        } finally {
            try {
                if (lock != null)
                    lock.release();
            } catch (IOException e) {
            }
            try {
                channel.close();
            } catch (IOException e) {
            }
            lockOnMutexFile.delete();
        }
        return true;
    }

    /**
     * Creates the lock file with the given contents.
     * 
     */
    private boolean createMutex() {
        File lockOnMutexFile = new File(lockOnMutex);
        FileChannel channel = null;
        FileLock lock = null;
        try {
            channel = new RandomAccessFile(lockOnMutexFile, "rw").getChannel();
            lockOnMutexFile.createNewFile();
            try {
                lock = channel.tryLock();
            } catch (OverlappingFileLockException e) {
                // File is already locked in this thread or virtual machine
                return false;
            }

            File mutexFile = new File(mutex);
            mutexFile.createNewFile();
            FileWriter writer = new FileWriter(mutexFile);
            writer.append(mutexContent);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.out.println("Error acquiring lock: " + e.getMessage());
        } finally {
            try {
                if (lock != null)
                    lock.release();
            } catch (IOException e) {
            }
            try {
                channel.close();
            } catch (IOException e) {
            }
            lockOnMutexFile.delete();
        }
        return true;
    }

    /**
     * Release the mutex .. that is Delete the underlying lock file, if it exists
     */
    private void releaseMutex() {
        File mutexFile = new File(mutex);
        mutexFile.delete();
    }
}