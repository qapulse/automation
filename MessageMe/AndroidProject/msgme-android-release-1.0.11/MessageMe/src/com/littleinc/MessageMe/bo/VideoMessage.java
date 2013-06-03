package com.littleinc.MessageMe.bo;

import java.io.File;
import java.sql.SQLException;

import android.app.ProgressDialog;
import android.content.Context;

import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.DatabaseTable;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.chat.DurableCommand;
import com.littleinc.MessageMe.chat.MediaDownloadListener;
import com.littleinc.MessageMe.chat.MediaManager;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.data.DataBaseHelper;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandMessageNew;
import com.littleinc.MessageMe.protocol.Messages.PBMessageEnvelope;
import com.littleinc.MessageMe.protocol.Messages.PBMessageVideo;
import com.littleinc.MessageMe.util.ChatAdapter;
import com.littleinc.MessageMe.util.FileSystemUtil;
import com.littleinc.MessageMe.util.StringUtil;

@DatabaseTable(tableName = VideoMessage.TABLE_NAME)
public class VideoMessage extends AbstractMessage {

    public static final String TABLE_NAME = "video_message";

    public static final String VIDEO_KEY_COLUMN = "video_key";

    public static final String THUMB_KEY_COLUMN = "thumb_key";

    public static final String VIDEO_DURATION_COLUMN = "video_duration";

    @DatabaseField(columnName = VIDEO_KEY_COLUMN, dataType = DataType.STRING)
    private String videoKey;

    @DatabaseField(columnName = THUMB_KEY_COLUMN, dataType = DataType.STRING)
    private String thumbKey;

    @DatabaseField(columnName = VIDEO_DURATION_COLUMN, dataType = DataType.INTEGER)
    private int duration;

    public VideoMessage() {
        super(new Message(IMessageType.VIDEO));
    }

    public static Dao<VideoMessage, Long> getDao() {
        try {
            return DataBaseHelper.getInstance().getDao(VideoMessage.class);
        } catch (SQLException e) {
            LogIt.e(VideoMessage.class, e, e.getMessage());
        }

        return null;
    }

    @Override
    public long save(boolean updateCursor, Conversation conversation) {
        setId(message.save(updateCursor, conversation));

        try {
            Dao<VideoMessage, Long> dao = getDao();
            dao.createOrUpdate(this);
            return dao.extractId(this);
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    public String getVideoKey() {
        return this.videoKey;
    }

    public void setVideoKey(String newVideoKey, boolean forceSet) {

        if (forceSet || (videoKey == null)) {
            videoKey = newVideoKey;
        } else {
            // Only set the video key if it doesn't already point at a local file
            // which exists (as this allows us to playback messages while they are
            // being uploaded).  
            boolean setKey = true;

            // Amazon S3 paths do not have a leading slash, but local paths do.
            if (videoKey.startsWith(File.separator)) {
                File localFile = new File(this.videoKey);

                if (localFile.exists()) {
                    LogIt.d(this,
                            "Do not set video key as it already points at a local file which exists",
                            videoKey);
                    setKey = false;
                }
            }

            if (setKey) {
                LogIt.d(this, "Set video key", newVideoKey);
                videoKey = newVideoKey;
            }
        }
    }

    public String getThumbKey() {
        return this.thumbKey;
    }

    public void setThumbKey(String thumbKey) {
        this.thumbKey = thumbKey;
    }

    public int getDuration() {
        return this.duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public PBCommandEnvelope serialize() {
        PBMessageVideo.Builder messageVideoBuilder = PBMessageVideo
                .newBuilder();
        PBMessageEnvelope.Builder messageEnvelopeBuilder = PBMessageEnvelope
                .newBuilder();
        PBCommandMessageNew.Builder commandMessageNewBuilder = PBCommandMessageNew
                .newBuilder();
        PBCommandEnvelope.Builder commandEnvelopeBuilder = PBCommandEnvelope
                .newBuilder();

        if (getVideoKey() != null) {
            messageVideoBuilder.setVideoKey(getVideoKey());
        }

        if (getThumbKey() != null) {
            // Only set this if it isn't null.  In some error conditions we may
            // have uploaded the video file but not the thumbnail, in which case
            // this can be null.
            messageVideoBuilder.setThumbKey(getThumbKey());
        }

        messageVideoBuilder.setSeconds(getDuration());

        messageEnvelopeBuilder.setType(getType().toMessageType());
        messageEnvelopeBuilder.setCreatedAt(getCreatedAt());
        messageEnvelopeBuilder.setVideo(messageVideoBuilder.build());

        commandMessageNewBuilder.setRecipientID(getChannelId());
        commandMessageNewBuilder.setMessageEnvelope(messageEnvelopeBuilder
                .build());

        commandEnvelopeBuilder.setMessageNew(commandMessageNewBuilder.build());

        return message.serializeWithEnvelopeBuilder(commandEnvelopeBuilder);
    }

    @Override
    public IMessageType getType() {
        return IMessageType.VIDEO;
    }

    @Override
    public boolean load() {
        boolean result = message.load();

        try {
            Dao<VideoMessage, Long> dao = getDao();
            VideoMessage videoMessage = dao.queryForId(getId());

            if (videoMessage != null) {
                setDuration(videoMessage.getDuration());
                setThumbKey(videoMessage.getThumbKey());
                setVideoKey(videoMessage.getVideoKey(), true);

                return result;
            } else {
                LogIt.w(VideoMessage.class,
                        "Trying to load a non-existing message", getId());
            }
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return false;
    }

    @Override
    public int delete() {
        message.delete();

        try {
            Dao<VideoMessage, Long> dao = getDao();
            return dao.deleteById(getId());
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    public void send(Context context, MessagingService service,
            String filename, boolean preserveOriginalFile) {
        LogIt.d(this, "Send VideoMessage");

        ProgressDialog dialog = UIUtil.showProgressDialog(context,
                context.getString(R.string.send_video_dialog));

        String videoMediaKey = MediaManager.generateObjectName(
                MediaManager.MESSAGE_FOLDER, StringUtil.getRandomFilename(),
                IMessageType.VIDEO);

        // XXX For now we do not attempt to move the video file into the
        // application internal storage media file cache as that means the
        // external media player cannot play back the video.
        //        // Put the video file in the application data directory
        //        if (preserveOriginalFile) {
        //            // Don't delete the original video file from the user's gallery
        //            if (!ImageUtil.copyFile(filename, videoMediaKey)) {
        //                LogIt.w(this,
        //                        "Failed to copy video file into application data directory",
        //                        filename, videoMediaKey);
        //                setCommandId(0);
        //            }
        //        } else {
        //            // Video files seem to often get created on external storage, so we
        //            // need to transfer them into internal storage (a normal File.renameTo
        //            // does not work)
        //            if (!ImageUtil.transferFile(filename, videoMediaKey)) {
        //                LogIt.w(this,
        //                        "Failed to move video file into application data directory",
        //                        filename, videoMediaKey);
        //                setCommandId(0);
        //            }
        //        }
        //        
        //        // Get the video file location now it is in the app data directory
        //        File videoFile = ImageUtil.getFile(videoMediaKey);
        //
        //        if (getCommandId() != 0) {

        File videoThumb = new File(getThumbKey());

        // Get the S3 path for the thumbnail
        String thumbMediaKey = MediaManager.generateObjectName(
                MediaManager.MESSAGE_FOLDER,
                FileSystemUtil.removeExtension(videoThumb.getName()),
                IMessageType.PHOTO);

        // Update the thumbnail media key with the S3 key instead of 
        // the local file  location
        setThumbKey(thumbMediaKey);

        // Update the video file media key with the S3 key instead of  
        // the local file location
        setVideoKey(videoMediaKey, true);

        DurableCommand durableCmd = new DurableCommand(this,
                new File(filename), videoThumb);

        service.addToDurableSendQueue(durableCmd);
        service.notifyChatClient(MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST,
                getId(), getChannelId());

        // Set the local version of our video media key back to the
        // local file so the user can play it back without streaming it.
        setVideoKey(filename, true);

        dialog.dismiss();
    }

    public void receive(ChatAdapter adapter, MessagingService service,
            String filename) {
        service.getChatManager().retrieveFile(filename, IMessageType.VIDEO,
                new VideoMediaDownloadListener(adapter, service, this));
    }

    public class VideoMediaDownloadListener implements MediaDownloadListener {

        private ChatAdapter adapter;

        private VideoMessage videoMessage;

        private MessagingService service;

        public VideoMediaDownloadListener(ChatAdapter adapter,
                MessagingService service, VideoMessage videoMessage) {
            this.adapter = adapter;
            this.videoMessage = videoMessage;
            this.service = service;
        }

        @Override
        public void onDownloadCompleted(String videoMediaKey) {
            LogIt.d(this, "Video media downloaded", videoMediaKey);
            videoMessage.setVideoKey(videoMediaKey, true);

            // The thumbnail needs to be downloaded as an image, not a video
            service.getChatManager().retrieveFile(videoMessage.getThumbKey(),
                    IMessageType.PHOTO, new MediaDownloadListener() {

                        @Override
                        public void onDownloadCompleted(String thumbMediaKey) {
                            LogIt.d(this, "Video thumb downloaded",
                                    thumbMediaKey);
                            videoMessage.setThumbKey(thumbMediaKey);
                        }

                        @Override
                        public void onDownloadError(String messageError) {
                            LogIt.w(this, "Error downloading video thumb",
                                    messageError, videoMessage.getThumbKey());
                        }
                    });

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onDownloadError(String messageError) {
            LogIt.w(this, "Error downloading video media", messageError,
                    videoMessage.getVideoKey());
        }
    }

    public static VideoMessage load(long commandId) {
        Message message = Message.loadByCommandId(commandId);

        if (message != null) {
            try {
                Dao<VideoMessage, Long> dao = VideoMessage.getDao();
                QueryBuilder<VideoMessage, Long> queryBuilder = dao
                        .queryBuilder();
                Where<VideoMessage, Long> where = queryBuilder.where();

                where.eq(Message.ID_COLUMN, message.getId());
                return queryBuilder.queryForFirst();
            } catch (SQLException e) {
                LogIt.e(VideoMessage.class, e);
            }
        }

        return null;
    }

    @Override
    public void parseFrom(PBCommandEnvelope commandEnvelope) {
        message.parseFrom(commandEnvelope);
        PBMessageVideo messageVideo = commandEnvelope.getMessageNew()
                .getMessageEnvelope().getVideo();

        setDuration(messageVideo.getSeconds());

        VideoMessage localMessage = VideoMessage.load(getCommandId());

        if (localMessage == null) {
            setThumbKey(messageVideo.getThumbKey());
            setVideoKey(messageVideo.getVideoKey(), false);
        } else {
            setThumbKey(localMessage.getThumbKey());
            setVideoKey(localMessage.getVideoKey(), false);
        }
    }

    @Override
    public String getMessagePreview(Context context) {
        if (wasSentByThisUser()) {
            return context
                    .getString(R.string.convo_preview_msg_desc_self_video);
        } else {
            return String.format(context
                    .getString(R.string.convo_preview_msg_desc_other_video),
                    getSender() != null ? getSender().getDisplayName() : "");
        }
    }
}