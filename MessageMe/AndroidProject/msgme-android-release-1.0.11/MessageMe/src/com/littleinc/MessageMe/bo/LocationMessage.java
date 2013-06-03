package com.littleinc.MessageMe.bo;

import java.sql.SQLException;

import android.content.Context;
import android.text.TextUtils;

import com.coredroid.util.LogIt;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.chat.DurableCommand;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.data.DataBaseHelper;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandMessageNew;
import com.littleinc.MessageMe.protocol.Messages.PBMessageEnvelope;
import com.littleinc.MessageMe.protocol.Messages.PBMessageLocation;

@DatabaseTable(tableName = LocationMessage.TABLE_NAME)
public class LocationMessage extends AbstractMessage {

    public static final String TABLE_NAME = "location_message";

    public static final String LATITUDE_COLUMN = "latitude";

    public static final String LONGITUDE_COLUMN = "longitude";

    public static final String LOCATION_ID_COLUMN = "location_id";

    public static final String NAME_COLUMN = "name";

    public static final String ADDRES_COLUMN = "address";

    @DatabaseField(columnName = LocationMessage.LATITUDE_COLUMN, dataType = DataType.FLOAT)
    private float latitude;

    @DatabaseField(columnName = LocationMessage.LONGITUDE_COLUMN, dataType = DataType.FLOAT)
    private float longitude;

    @DatabaseField(columnName = LocationMessage.LOCATION_ID_COLUMN, dataType = DataType.STRING)
    private String locationId;

    @DatabaseField(columnName = LocationMessage.NAME_COLUMN, dataType = DataType.STRING)
    private String name;

    @DatabaseField(columnName = LocationMessage.ADDRES_COLUMN, dataType = DataType.STRING)
    private String address;

    public LocationMessage() {
        super(new Message(IMessageType.LOCATION));
    }

    @Override
    public long save(boolean updateCursor, Conversation conversation) {
        setId(message.save(updateCursor, conversation));

        try {
            Dao<LocationMessage, Long> dao = getDao();
            dao.createOrUpdate(this);
            return dao.extractId(this);
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    public static Dao<LocationMessage, Long> getDao() {
        try {
            return DataBaseHelper.getInstance().getDao(LocationMessage.class);
        } catch (SQLException e) {
            LogIt.e(LocationMessage.class, e, e.getMessage());
        }

        return null;
    }

    @Override
    public PBCommandEnvelope serialize() {
        PBMessageLocation.Builder messageLocationBuilder = PBMessageLocation
                .newBuilder();
        PBMessageEnvelope.Builder messageEnvelopeBuilder = PBMessageEnvelope
                .newBuilder();
        PBCommandMessageNew.Builder commandMessageNewBuilder = PBCommandMessageNew
                .newBuilder();
        PBCommandEnvelope.Builder commandEnvelopeBuilder = PBCommandEnvelope
                .newBuilder();

        if (getName() != null) {
            messageLocationBuilder.setName(getName());
        }

        if (getAddress() != null) {
            messageLocationBuilder.setAddress(getAddress());
        }

        messageLocationBuilder.setLatitude(getLatitude());
        messageLocationBuilder.setLongitude(getLongitude());
        messageLocationBuilder.setLocationID(getLocationID());

        messageEnvelopeBuilder.setType(getType().toMessageType());
        messageEnvelopeBuilder.setCreatedAt(getCreatedAt());
        messageEnvelopeBuilder.setLocation(messageLocationBuilder.build());

        commandMessageNewBuilder.setRecipientID(getChannelId());
        commandMessageNewBuilder.setMessageEnvelope(messageEnvelopeBuilder
                .build());

        commandEnvelopeBuilder.setMessageNew(commandMessageNewBuilder.build());

        return message.serializeWithEnvelopeBuilder(commandEnvelopeBuilder);
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public String getLocationID() {
        return locationId;
    }

    public void setLocationId(String locationID) {
        this.locationId = locationID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public IMessageType getType() {
        return IMessageType.LOCATION;
    }

    @Override
    public boolean load() {
        boolean result = message.load();

        try {
            Dao<LocationMessage, Long> dao = getDao();
            LocationMessage locationMessage = dao.queryForId(getId());

            if (locationMessage != null) {
                setAddress(locationMessage.getAddress());
                setLatitude(locationMessage.getLatitude());
                setLocationId(locationMessage.getLocationID());
                setLongitude(locationMessage.getLongitude());
                setName(locationMessage.getName());

                return result;
            } else {
                LogIt.w(LocationMessage.class,
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
            Dao<LocationMessage, Long> dao = getDao();
            return dao.deleteById(getId());
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    public void send(MessagingService service) {
        DurableCommand durableCmd = new DurableCommand(this);
        message.durableSend(service, durableCmd);
    }

    @Override
    public void parseFrom(PBCommandEnvelope commandEnvelope) {
        message.parseFrom(commandEnvelope);
        PBMessageLocation messageLocation = commandEnvelope.getMessageNew()
                .getMessageEnvelope().getLocation();

        setName(messageLocation.getName());
        setAddress(messageLocation.getAddress());
        setLatitude(messageLocation.getLatitude());
        setLongitude(messageLocation.getLongitude());
        setLocationId(messageLocation.getLocationID());
        setCommandId(commandEnvelope.getCommandID());
        setClientId(commandEnvelope.getClientID());
    }

    @Override
    public String getMessagePreview(Context context) {
        if (getLocationID() != null && TextUtils.isEmpty(getLocationID())) {
            if (wasSentByThisUser()) {
                return String
                        .format(context
                                .getString(R.string.convo_preview_msg_desc_self_location_current));
            } else {
                return String
                        .format(context
                                .getString(R.string.convo_preview_msg_desc_other_location_current),
                                getSender() != null ? getSender()
                                        .getDisplayName() : "");
            }
        } else {
            if (wasSentByThisUser()) {
                return String
                        .format(context
                                .getString(R.string.convo_preview_msg_desc_self_location),
                                getName());
            } else {
                return String
                        .format(context
                                .getString(R.string.convo_preview_msg_desc_other_location),
                                getSender() != null ? getSender()
                                        .getDisplayName() : "", getName());
            }
        }
    }
}