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
import com.j256.ormlite.table.DatabaseTable;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.chat.DurableCommand;
import com.littleinc.MessageMe.chat.MediaDownloadListener;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.data.DataBaseHelper;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandMessageNew;
import com.littleinc.MessageMe.protocol.Messages.PBMessageEnvelope;
import com.littleinc.MessageMe.protocol.Messages.PBMessageVoice;
import com.littleinc.MessageMe.util.ChatAdapter;

@DatabaseTable(tableName = VoiceMessage.TABLE_NAME)
public class VoiceMessage extends AbstractMessage {

    public static final String TABLE_NAME = "voice_message";

    public static final String SOUND_KEY_COLUMN = "sound_Key";

    public static final String SECONDS_KEY_COLUMN = "seconds";

    @DatabaseField(columnName = SOUND_KEY_COLUMN, dataType = DataType.STRING)
    private String soundKey;

    @DatabaseField(columnName = SECONDS_KEY_COLUMN, dataType = DataType.INTEGER)
    private int seconds;

    public VoiceMessage() {
        super(new Message(IMessageType.VOICE));
    }

    @Override
    public long save(boolean updateCursor, Conversation conversation) {
        setId(message.save(updateCursor, conversation));

        try {
            Dao<VoiceMessage, Long> dao = getDao();
            dao.createOrUpdate(this);
            return dao.extractId(this);
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    public static Dao<VoiceMessage, Long> getDao() {
        try {
            return DataBaseHelper.getInstance().getDao(VoiceMessage.class);
        } catch (SQLException e) {
            LogIt.e(VoiceMessage.class, e, e.getMessage());
        }

        return null;
    }

    @Override
    public PBCommandEnvelope serialize() {
        PBMessageVoice.Builder messageVoiceBuilder = PBMessageVoice
                .newBuilder();
        PBMessageEnvelope.Builder messageEnvelopeBuilder = PBMessageEnvelope
                .newBuilder();
        PBCommandMessageNew.Builder commandMessageNewBuilder = PBCommandMessageNew
                .newBuilder();
        PBCommandEnvelope.Builder commandEnvelopeBuilder = PBCommandEnvelope
                .newBuilder();

        messageVoiceBuilder.setSeconds(getSeconds());

        if (getSoundKey() != null) {
            messageVoiceBuilder.setSoundKey(getSoundKey());
        }

        messageEnvelopeBuilder.setType(getType().toMessageType());
        messageEnvelopeBuilder.setCreatedAt(getCreatedAt());
        messageEnvelopeBuilder.setVoice(messageVoiceBuilder.build());

        commandMessageNewBuilder.setRecipientID(getChannelId());
        commandMessageNewBuilder.setMessageEnvelope(messageEnvelopeBuilder
                .build());

        commandEnvelopeBuilder.setMessageNew(commandMessageNewBuilder.build());

        return message.serializeWithEnvelopeBuilder(commandEnvelopeBuilder);
    }

    public String getSoundKey() {
        return this.soundKey;
    }

    public void setSoundKey(String soundKey) {
        this.soundKey = soundKey;
    }

    public Integer getSeconds() {
        return this.seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    @Override
    public IMessageType getType() {
        return IMessageType.VOICE;
    }

    @Override
    public boolean load() {
        boolean result = message.load();

        try {
            Dao<VoiceMessage, Long> dao = getDao();
            VoiceMessage voiceMessage = dao.queryForId(getId());

            if (voiceMessage != null) {
                setSeconds(voiceMessage.getSeconds());
                setSoundKey(voiceMessage.getSoundKey());

                return result;
            } else {
                LogIt.w(VoiceMessage.class,
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
            Dao<VoiceMessage, Long> dao = getDao();
            return dao.deleteById(getId());
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    public void send(Context context, MessagingService service, File file) {

        LogIt.d(this, "Send VoiceMessage");

        ProgressDialog progressDialog = UIUtil.showProgressDialog(context,
                context.getString(R.string.send_voice_dialog));

        DurableCommand durableCmd = new DurableCommand(this, file, null);

        service.addToDurableSendQueue(durableCmd);
        service.notifyChatClient(MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST,
                getId(), getChannelId());

        progressDialog.dismiss();
    }

    public void receive(ChatAdapter adapter, MessagingService service,
            String filename) {
        service.getChatManager().retrieveFile(filename, IMessageType.VOICE,
                new VoiceMediaDownloadListener(adapter, service, this));
    }

    public class VoiceMediaDownloadListener implements MediaDownloadListener {

        private ChatAdapter adapter;

        private VoiceMessage voiceMessage;

        public VoiceMediaDownloadListener(ChatAdapter adapter,
                MessagingService service, VoiceMessage voiceMessage) {
            this.adapter = adapter;
            this.voiceMessage = voiceMessage;
        }

        @Override
        public void onDownloadCompleted(String mediaKey) {
            voiceMessage.setSoundKey(mediaKey);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onDownloadError(String messageError) {
            LogIt.e(this, null, messageError);
        }

    }

    @Override
    public void parseFrom(PBCommandEnvelope commandEnvelope) {
        message.parseFrom(commandEnvelope);
        PBMessageVoice messageVoice = commandEnvelope.getMessageNew()
                .getMessageEnvelope().getVoice();

        setSeconds(messageVoice.getSeconds());
        setSoundKey(messageVoice.getSoundKey());
        setCommandId(commandEnvelope.getCommandID());
        setClientId(commandEnvelope.getClientID());
    }

    @Override
    public String getMessagePreview(Context context) {
        if (wasSentByThisUser()) {
            return context
                    .getString(R.string.convo_preview_msg_desc_self_voice);
        } else {
            return String.format(context
                    .getString(R.string.convo_preview_msg_desc_other_voice),
                    getSender() != null ? getSender().getDisplayName() : "");
        }
    }
}