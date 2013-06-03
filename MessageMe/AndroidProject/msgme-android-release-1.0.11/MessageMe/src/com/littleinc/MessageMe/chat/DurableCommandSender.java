package com.littleinc.MessageMe.chat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import com.coredroid.util.LogIt;
import com.crittercism.app.Crittercism;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.data.DataBaseHelper;
import com.littleinc.MessageMe.error.ImageUploadException;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.util.BatchTask;
import com.littleinc.MessageMe.util.FileSystemUtil;
import com.littleinc.MessageMe.util.ImageUtil;
import com.littleinc.MessageMe.util.NetUtil;

public class DurableCommandSender implements Runnable {

    /**
     * The FIFO queue for sending durable commands.  The order is not actually
     * maintained as some messages take longer to process than others, e.g.
     * messages with media need to upload the media before the PBCommand gets
     * sent to the server.
     */
    Queue<DurableCommand> mProcessQueue = new ConcurrentLinkedQueue<DurableCommand>();

    /**
     * This queue is just used to block the current thread when it doesn't 
     * have any commands to send, the values in this queue should not be used. 
     */
    private BlockingQueue<Object> signalQueue = new LinkedBlockingQueue<Object>();
    
    /**
     * DurableCommands are written to file as well as the send queue so the app
     * can retry them if it is killed.  These variables defines that location.
     */
    public static final String SEND_QUEUE_DIRECTORY = "sendqueue";

    /**
     * This field should only be access through getSendQueueDirectory() 
     */
    private static File sQueueDir;

    private File[] mPendingCommandFiles;

    /**
     * If sent message has not been acknowledged in this time interval it 
     * should be sent again.  Defined in milliseconds.
     */
    public static final int CMD_RETRY_INTERVAL = 4000;

    /**
     * This is when the UI should indicate that the message has not yet been 
     * successfully sent.  Defined in seconds. 
     */
    public static final int CMD_DELAYED_INTERVAL_SECONDS = 7;

    /**
     * When there are no messages ready to send, this is how long we sleep for
     * before checking again. 
     */
    public static final long PROCESS_INTERVAL = 2000;

    /** Singleton instance of the DurableCommandSender. */
    private static DurableCommandSender sInstance = null;

    /** The thread that runs the DurableCommandSender. */
    private Thread mSendThread = null;

    /** Flag for shutting down our thread used to send the messages. */
    private volatile boolean mShutdownRequested = false;

    /**
     * The MessagingService instance to actually send commands through. 
     * 
     * This is a Service, which extends Context, so we can pass this
     * into any calls that require a context. 
     */
    private MessagingService mMessagingService;

    /**
     * Thread pool used for uploading media.
     */
    private ExecutorService mMediaUploadThreadPool = null;

    private static final int MEDIA_UPLOAD_THREAD_POOL_SIZE = 2;

    private DurableCommandSender(MessagingService messagingService) {

        LogIt.d(this, "Create durable command sender");

        mMessagingService = messagingService;

        // Load any pending commands from file into our process queue
        loadPendingCommandsFromFile();

        createThreadsIfRequired();
    }

    /**
     * This may need to be done again later if the threads have been stopped,
     * e.g. if somebody has stopped the MessagingService through the Android 
     * Settings page.
     */
    public synchronized void createThreadsIfRequired() {

        if (mSendThread == null) {
            LogIt.d(this, "Create the message sending thread");
            mShutdownRequested = false;
            mSendThread = new Thread(this);
            mSendThread.start();
        }

        if (mMediaUploadThreadPool == null) {
            // Create ExecutorService to manage upload threads       
            LogIt.d(this, "Create upload thread pool");
            mMediaUploadThreadPool = Executors
                    .newFixedThreadPool(MEDIA_UPLOAD_THREAD_POOL_SIZE);
        }
    }

    public static synchronized DurableCommandSender getInstance(
            MessagingService messagingService) {
        if (sInstance == null) {
            LogIt.d(DurableCommandSender.class,
                    "Creating singleton DurableCommandSender");
            sInstance = new DurableCommandSender(messagingService);
        }

        return sInstance;
    }

    private static File getSendQueueDirectory() {

        if (sQueueDir == null) {
            sQueueDir = ImageUtil.getInternalFilesDir(SEND_QUEUE_DIRECTORY);

            if (!sQueueDir.exists()) {
                // Create parent folders if required
                if (sQueueDir.mkdirs()) {
                    LogIt.d(DurableCommandSender.class,
                            "Creating durable command directory", sQueueDir);
                } else {
                    LogIt.w(DurableCommandSender.class,
                            "Error creating durable command directory",
                            sQueueDir);
                }
            }
        }

        return sQueueDir;
    }

    /**
     * Callers of this method should check if the returned object is null
     * before trying to use it.
     * 
     * Generally the {@link #getInstance(MessagingService)} method should
     * be used instead of this one. 
     */
    public static DurableCommandSender getInstance() {
        if (sInstance == null) {
            // This would indicate a programming error
            LogIt.w(DurableCommandSender.class,
                    "getInstance() called before DurableCommandSender has been created");
        }

        return sInstance;
    }

    /** 
     * Add a DurableCommand to the send queue.
     */
    public void addToQueue(DurableCommand durableCmd) {
        LogIt.d(this, "Add DurableCommand to send queue",
                durableCmd.getClientId());

        if (mProcessQueue.add(durableCmd)) {
            if (signalQueue.isEmpty()) {
                LogIt.d(this, "Add command to empty signal queue");
                signalQueue.add(durableCmd);                
            }
        } else {
            LogIt.e(DataBaseHelper.class,
                    "Failed to add durable command to send queue");
        }

        File cmdFile = getFileForCmd(durableCmd);

        if (cmdFile.exists()) {
            LogIt.w(this,
                    "Error, this command is already queued on disk!  Ignore it.");
            return;
        } else {
            writeCommandToDisk(durableCmd);
        }

        createThreadsIfRequired();
    }
    
    /**
     * This doesn't check if the command already exists on disk as callers
     * should be able to use this to overwrite existing DurableCommand objects,
     * e.g. to update that its media has been uploaded. 
     */
    static void writeCommandToDisk(DurableCommand durableCmd) {

        File cmdFile = getFileForCmd(durableCmd);

        ObjectOutputStream objOutputStream = null;

        try {
            LogIt.d(DurableCommandSender.class,
                    "Writing DurableCommand to disk", cmdFile);

            // Write the command to disk
            //
            // If we were more paranoid we would write to a temporary file and
            // then call File.renameTo, as that is usually atomic
            //
            // Use a buffered output stream as apparently performance is better
            // if your data is smaller than 8KB 
            //   http://stackoverflow.com/a/8713017/112705
            objOutputStream = new ObjectOutputStream(new BufferedOutputStream(
                    new FileOutputStream(cmdFile)));
            objOutputStream.writeObject(durableCmd);
        } catch (Exception e) {
            LogIt.w(DurableCommandSender.class,
                    "Error writing command to file", cmdFile, e);
        } finally {
            FileSystemUtil.closeOutputStream(objOutputStream);
        }
    }

    /** 
     * Helper method to remove a PBCommandEnvelope from the send queue.
     */
    public void removeFromQueue(PBCommandEnvelope pbCommand) {

        removeFromQueue(new DurableCommand(pbCommand));
    }

    /** 
     * Remove a DurableCommand from the send queue as we have received
     * acknowledgment from the server that it was received.
     */
    public void removeFromQueue(DurableCommand cmdToRemove) {

        // The clientID field is used to uniquely identify each command
        long clientID = cmdToRemove.getClientId();

        // Remove the command from the in memory send queue
        if (mProcessQueue.remove(cmdToRemove)) {
            LogIt.d(this,
                    "Successfully removed DurableCommand from send queue",
                    cmdToRemove.getClientId());

            // The durable command was removed from the in-memory send queue,
            // so check whether any file on disk also needs to be deleted.  
            //
            // We don't want to make unnecessary calls to check whether to 
            // delete files from disk as that will impact performance, 
            // especially on first time logins (as the user will receive 
            // lots of MESSAGE_NEW commands for messages that they sent).
            File cmdFile = getFileForCmd(clientID);

            if (cmdFile.exists()) {
                if (cmdFile.delete()) {
                    LogIt.d(this, "DurableCommand file deleted successfully",
                            cmdFile);
                } else {
                    LogIt.w(this, "Failed to delete DurableCommand", cmdFile);
                }
            } else {
                LogIt.d(this, "No DurableCommand on disk to remove");
                return;
            }
        } else {
            // This can happen in normal circumstances, e.g. when logging into 
            // a device with an existing account (as all the user's sent 
            // messages get sent to them in a BATCH).  For performance we
            // don't want to log this.
            //            LogIt.d(this, "No DurableCommand to remove from send queue", 
            //                    cmdToRemove.getClientId());
        }
    }

    /**
     * Load any pending DurableCommand objects from file and add them to the
     * mProcessQueue for processing.
     * 
     * The only time the commands should be loaded from file is when the app
     * starts up, as commands usually get added to mProcessQueue and written
     * to file at the same time.
     */
    private void loadPendingCommandsFromFile() {

        LogIt.d(this, "Load any pending commands from file");

        // Get files that need to be processed
        mPendingCommandFiles = getSendQueueDirectory().listFiles();

        if ((mPendingCommandFiles == null)
                || (mPendingCommandFiles.length == 0)) {
            LogIt.d(this, "No pending commands");
        } else {
            for (File cmdFile : mPendingCommandFiles) {

                ObjectInputStream objInputStream = null;

                try {
                    LogIt.d(this, "Read DurableCommand from file...", cmdFile);

                    objInputStream = new ObjectInputStream(new FileInputStream(
                            cmdFile));
                    Object obj = objInputStream.readObject();

                    if (obj instanceof DurableCommand) {
                        DurableCommand durableCmd = (DurableCommand) obj;
                        
                        if (durableCmd.getIsUploading()) {
                            // This can happen if the application is stopped
                            // after the media upload has already started.  
                            // Since we are loading the command from file we
                            // know it isn't uploading now.  This is necessary
                            // otherwise the upload won't start again!
                            LogIt.i(this,
                                    "Update DurableCommand to say it is NOT uploading");
                            durableCmd.setUploading(false);
                        }
                        
                        mProcessQueue.add(durableCmd);
                        LogIt.d(this, "Added DurableCommand to process queue",
                                durableCmd.getClientId());
                        
                        if (signalQueue.isEmpty()) {
                            LogIt.d(this, "Add command to empty signal queue");
                            signalQueue.add(durableCmd);                
                        }
                    }
                } catch (Exception e) {
                    LogIt.w(this, "Error loading command from file", cmdFile, e);
                } finally {
                    FileSystemUtil.closeInputStream(objInputStream);
                }
            }
        }

    }

    @Override
    public void run() {

        LogIt.d(this, "Starting DurableCommand processing thread");

        while (!mShutdownRequested && !Thread.currentThread().isInterrupted()) {
            LogIt.d(this,
                    "DurableCommand processing thread waking up, send queue size: "
                            + mProcessQueue.size());

            if (mProcessQueue.size() > 0) {
                // We have commands to process, so check we have a network 
                // connection and a connected web socket
                if (mMessagingService != null
                        && NetUtil.checkInternetConnection(mMessagingService)
                        && mMessagingService.isConnected()) {

                    for (DurableCommand durableCmd : mProcessQueue) {

                        LogIt.d(this, "Process DurableCommand",
                                durableCmd.getClientId());

                        // We have a connection so try to upload the message and its media
                        if (durableCmd.isReadyToSendPBCommand()) {
                            sendPBMessageToServer(durableCmd, mMessagingService);
                        } else if (durableCmd.isReadyToUploadMedia()
                                || durableCmd.isReadyToUploadThumb()) {
                            try {
                                startUploadingMedia(durableCmd);
                            } catch (Exception ex) {
                                LogIt.e(this, ex,
                                        "Error when sending DurableCommand",
                                        durableCmd.getClientId());
                            }
                        }
                    }
                } else {
                    LogIt.d(this,
                            "Not connected, don't try to process any commands");
                }
                
                try {
                    // We need to keep waking up to check if any commands, or
                    // their next piece of media needs to be sent
                    LogIt.d(this, "Worked through current process queue, sleep");
                    Thread.sleep(PROCESS_INTERVAL);
                } catch (InterruptedException ex) {
                    LogIt.e(this, ex,
                            "Exception sleeping DurableCommandSender thread");
                }                
            } else {
                try {
                    // Now that mProcessQueue is empty, use the signalQueue to
                    // block until another command needs to be processed
                    LogIt.d(this,
                            "Wait until a new command needs to be sent");
                    signalQueue.take();
                } catch (InterruptedException ex) {
                    LogIt.i(this, ex, "InterruptedException");
                }
            }
        }

        LogIt.i(this, "Shutting down DurableCommandSender thread");
    }

    private void startUploadingMedia(DurableCommand durableCmd) {
        switch (durableCmd.getMediaUploadDestination()) {
        case S3:
            LogIt.d(DurableCommandSender.class, "Start S3 upload",
                    durableCmd.getMediaToUpload());

            durableCmd.setUploading(true);
            
            UploadToS3Task s3Upload = new UploadToS3Task(durableCmd,
                    mMessagingService);
            mMediaUploadThreadPool.execute(s3Upload);
            break;
        case IMAGE_SERVER:
            LogIt.d(DurableCommandSender.class, "Start image server upload",
                    durableCmd.getMediaToUpload());

            durableCmd.setUploading(true);
            
            UploadToImageServerTask imgServerUpload = new UploadToImageServerTask(
                    durableCmd, mMessagingService);
            mMediaUploadThreadPool.execute(imgServerUpload);
            break;
        default:
            LogIt.w(DurableCommandSender.class,
                    "Unknown MediaUploadDestination",
                    durableCmd.getMediaUploadDestination());
            break;
        }
    }

    static void sendPBMessageToServer(DurableCommand durableCmd,
            MessagingService messagingService) {
        durableCmd.updateLastSentTime();
        messagingService.sendCommand(durableCmd.getPBCommandEnvelop());
    }

    public synchronized void shutDown() {

        if (mSendThread == null) {
            LogIt.i(this, "No send thread to shut down");
        } else {
            LogIt.i(this, "Shut down the message send thread");
            mShutdownRequested = true;
            
            // If the thread is blocked this will make it wake up
            mSendThread.interrupt();
            mSendThread = null;
        }

        if (mMediaUploadThreadPool == null) {
            LogIt.d(this, "No thread pool to shut down");
        } else {
            LogIt.i(this, "Shut down thread pool");
            mMediaUploadThreadPool.shutdown();
            mMediaUploadThreadPool = null;
        }
    }

    /**
     * Get the File where the DurableCommand is being stored.
     */
    private static File getFileForCmd(DurableCommand cmd) {
        return new File(getSendQueueDirectory() + File.separator
                + cmd.getClientId());
    }

    /**
     * Get the File where the DurableCommand is being stored based on the
     * client ID.
     */
    private static File getFileForCmd(long clientID) {
        return new File(getSendQueueDirectory() + File.separator + clientID);
    }

    /**
     * Something really bad has happened so delete this command from the
     * send queue and update the database so the UI shows an error next to
     * the message.
     */
    public static void handleUnrecoverableError(Throwable e, File fileToUpload,
            final DurableCommand cmd) {
        LogIt.e(DurableCommandSender.class,
                e,
                "Unexpected error uploading file, delete DurableCommand from queue",
                cmd.getClientId(), fileToUpload);

        DurableCommandSender durableSender = DurableCommandSender.getInstance();

        if (durableSender == null) {
            LogIt.w(DurableCommandSender.class,
                    "Cannot remove DurableCommand from queue", cmd);
        } else {
            durableSender.removeFromQueue(cmd);
        }
        
        // Update the command ID to show that it failed so the UI shows an
        // error next to it
        final IMessage msg = cmd.getLocalMessage();
        
        if (msg == null) {
            LogIt.w(DurableCommandSender.class,
                    "Cannot mark message as failed to send", cmd.getClientId());
        } else {                        
            msg.setCommandId(0);
            
            // We don't need to update any UI, and we can't use a DatabaseTask
            // as we are already in a background Runnable
            new BatchTask() {
                
                @Override
                public void work() {
                    msg.save(false);
                }
            };
        }
        
        ImageUploadException ex = new ImageUploadException(e,
                fileToUpload, cmd.getMessageType());
        Crittercism.logHandledException(ex);
    }

    public DurableCommand getDurableCommand(long clientId) {
        for (DurableCommand durableCommand : mProcessQueue) {
            if (clientId == durableCommand.getClientId()) {
                return durableCommand;
            }
        }

        return null;
    }
}
