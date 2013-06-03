package com.littleinc.MessageMe.bo;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.restlet.data.Form;

import android.text.TextUtils;

import com.coredroid.util.LogIt;
import com.google.protobuf.InvalidProtocolBufferException;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.DatabaseTable;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.data.DataBaseHelper;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope.CommandType;
import com.littleinc.MessageMe.protocol.Commands.PBCommandRoomJoin;
import com.littleinc.MessageMe.protocol.Commands.PBCommandRoomLeave;
import com.littleinc.MessageMe.protocol.Objects.PBRoom;
import com.littleinc.MessageMe.protocol.Objects.PBUser;
import com.littleinc.MessageMe.util.DateUtil;
import com.littleinc.MessageMe.util.ImageLoader;

@DatabaseTable(tableName = Room.TABLE_NAME)
public class Room extends Contact {

    public static final String TABLE_NAME = "room";

    public static final String UNNAMED = "unnamed";

    public static final String CREATOR_ID = "creator_id";

    public static final String DATE_CREATED = "date_created";

    private static Dao<Room, Long> sDao;

    @DatabaseField(columnName = Room.CREATOR_ID, canBeNull = false, dataType = DataType.LONG)
    private long creatorId;

    @DatabaseField(columnName = Room.DATE_CREATED, canBeNull = false, dataType = DataType.INTEGER)
    private int dateCreated;

    @DatabaseField(columnName = Room.UNNAMED, dataType = DataType.BOOLEAN)
    private boolean unnamed = false;

    @ForeignCollectionField(eager = false)
    private Collection<RoomMember> members = new LinkedList<RoomMember>();

    public Room() {
        init();
    }

    public Room(long roomId) {
        init();
        setContactId(roomId);
    }

    private void init() {
        setContactType(ContactType.GROUP.getValue());

        // Rooms default to being shown as the user only sees rooms
        // once they have created or joined one
        setShown(true);
    }

    public static Dao<Room, Long> getDao() {

        if (sDao == null) {
            try {
                sDao = DataBaseHelper.getInstance().getDao(Room.class);

                // Check here for more information about ORMLite cache documentation
                // http://ormlite.com/javadoc/ormlite-core/doc-files/ormlite_5.html#Object-Caches
                sDao.setObjectCache(true);

            } catch (SQLException e) {
                LogIt.e(Room.class, e, e.getMessage());
            }
        }

        return sDao;
    }

    public static long getChannelIdFromPrivateRoom(PBRoom room) {
        for (PBUser user : room.getUsersList()) {
            if (user.getUserID() != MessageMeApplication.getCurrentUser()
                    .getUserId()) {
                return user.getUserID();
            }
        }

        return -1;
    }

    /**
     * Checks whether this Room exists in the local database.
     */
    public static boolean exists(long contactID) {
        Room loadedRoom = null;
        try {
            Dao<Room, Long> dao = getDao();
            loadedRoom = dao.queryForId(contactID);
        } catch (SQLException e) {
            LogIt.e(Room.class, e, e.getMessage());
        }

        return loadedRoom == null ? false : true;
    }

    @Override
    public void load() {
        try {
            Dao<Room, Long> dao = getDao();
            Room loadedRoom = dao.queryForId(getRoomId());

            setShown(loadedRoom.isShown());
            setFirstName(loadedRoom.getName());
            setContactId(loadedRoom.getRoomId());
            setLastName(loadedRoom.getLastName());
            setCreatorId(loadedRoom.getCreatorId());
            setDateCreated(loadedRoom.getDateCreated());
            setCoverImageKey(loadedRoom.getCoverImageKey());
            setProfileImageKey(loadedRoom.getProfileImageKey());
            setUnnamed(loadedRoom.isUnnamed());

            loadRoomMembers();
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }
    }

    private void loadRoomMembers() {
        try {
            Dao<RoomMember, Long> dao = RoomMember.getDao();
            setMembers(dao.queryForEq(RoomMember.ROOM_ID_COLUMN, getRoomId()));
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }
    }

    public static List<Room> getRoomList() {
        List<Room> rooms = new ArrayList<Room>();

        try {
            Dao<Room, Long> dao = getDao();

            QueryBuilder<Room, Long> queryBuilder = dao.queryBuilder();
            Where<Room, Long> where = queryBuilder.where();

            where.eq(User.IS_SHOWN_COLUMN, true);
            rooms.addAll(queryBuilder.query());
        } catch (SQLException e) {
            LogIt.e(Room.class, e, e.getMessage());
        }

        return rooms;
    }

    public static List<Room> search(String name) {
        List<Room> rooms = new ArrayList<Room>();

        try {
            Dao<Room, Long> dao = getDao();
            QueryBuilder<Room, Long> queryBuilder = dao.queryBuilder();
            Where<Room, Long> where = queryBuilder.where();

            where.like(FIRST_NAME_COLUMN, "%" + name + "%");
            return queryBuilder.query();
        } catch (SQLException e) {
            LogIt.e(Room.class, e, e.getMessage());
        }

        return rooms;
    }

    public long save() {
        try {
            Dao<Room, Long> dao = getDao();
            dao.createOrUpdate(this);
            setContactId(dao.extractId(this));
            User currentUser = MessageMeApplication.getCurrentUser();

            for (RoomMember member : getMembers()) {
                member.setRoom(this);

                // Only add not existing members and avoid add current user as a member
                if (currentUser.getUserId() != member.getUserId()
                        && !member.exists()) {
                    member.save();
                }
            }

            return getRoomId();
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    public long getRoomId() {
        return contactId;
    }

    public void setName(String name) {
        setFirstName(name);
    }

    public String getName() {
        return getDisplayName();
    }

    public static void join(MessagingService service, User newMember, Room room)
            throws Exception {
        PBCommandRoomJoin.Builder roomJoinBuilder = PBCommandRoomJoin
                .newBuilder();

        roomJoinBuilder.setRoomID(room.getRoomId());
        roomJoinBuilder.setUser(newMember.toPBUser());
        roomJoinBuilder.setDateCreated(DateUtil.getCurrentTimestamp());

        PBCommandEnvelope.Builder commandEnvelopeBuilder = PBCommandEnvelope
                .newBuilder();
        commandEnvelopeBuilder.setType(CommandType.ROOM_JOIN);
        commandEnvelopeBuilder.setRoomJoin(roomJoinBuilder.build());

        service.sendCommand(commandEnvelopeBuilder.build());
    }

    public static PBCommandEnvelope serializeRoomLeaveCommand(Room room) {
        PBCommandRoomLeave.Builder roomLeaveBuilder = PBCommandRoomLeave
                .newBuilder();

        roomLeaveBuilder.setRoomID(room.getRoomId());
        roomLeaveBuilder.setDateCreated(DateUtil.getCurrentTimestamp());

        PBCommandEnvelope.Builder commandEnvelopeBuilder = PBCommandEnvelope
                .newBuilder();

        commandEnvelopeBuilder.setClientID(DateUtil.now().getTime());
        commandEnvelopeBuilder.setUserID(MessageMeApplication.getCurrentUser()
                .getUserId());
        commandEnvelopeBuilder.setType(CommandType.ROOM_LEAVE);
        commandEnvelopeBuilder.setRoomLeave(roomLeaveBuilder.build());

        return commandEnvelopeBuilder.build();
    }

    public static long addMember(User member, Room room) {
        RoomMember roomMember = new RoomMember(member, room);
        room.addMember(roomMember);
        return roomMember.save();
    }

    public static long removeMember(User member, Room room) {
        RoomMember roomMember = new RoomMember(member, room);
        room.removeMember(roomMember);
        return roomMember.delete();
    }

    /**
     * Build a Form for creating a new Room over the REST endpoint.  The room
     * ID will be returned by the server.  There are no profile or cover photos
     * as they can only be set after the room is created.
     */
    public Form toForm() {
        Form form = new Form();

        form.add("user_id", String.valueOf(MessageMeApplication
                .getCurrentUser().getUserId()));
        form.add("name", getName());

        for (RoomMember member : getMembers()) {
            form.add("members[]", String.valueOf(member.getUserId()));
        }

        return form;
    }

    public PBRoom toPBRoom() {
        PBRoom.Builder builder = PBRoom.newBuilder();

        if (!TextUtils.isEmpty(getName())) {
            builder.setName(getName());
        }

        builder.setRoomID(getRoomId());
        builder.setCreatorID(getCreatorId());

        if (!TextUtils.isEmpty(getCoverImageKey())) {
            builder.setCoverImageKey(getCoverImageKey());
        }

        if (!TextUtils.isEmpty(getProfileImageKey())) {
            builder.setProfileImageKey(getProfileImageKey());
        }

        builder.setDateCreated(getDateCreated());
        builder.setUnnamed(isUnnamed());

        List<RoomMember> members = new LinkedList<RoomMember>(this.members);

        for (int i = 0; i < members.size(); i++) {
            builder.addUsers(i, members.get(i).toPBUser());
        }

        return builder.build();
    }

    /**
     * Create a Room based on the provided PBRoom
     */
    public static Room parseFrom(PBRoom room) {
        Room newRoom = new Room();

        newRoom.setName(room.getName());
        newRoom.setContactId(room.getRoomID());
        newRoom.setCreatorId(room.getCreatorID());
        newRoom.setCoverImageKey(room.getCoverImageKey());
        newRoom.setProfileImageKey(room.getProfileImageKey());
        newRoom.setDateCreated(room.getDateCreated());

        if (room.hasUnnamed()) {
            newRoom.setUnnamed(room.getUnnamed());
        }

        // Add all the currently active users to the room
        for (PBUser pbUser : room.getUsersList()) {
            newRoom.addMember(RoomMember.parseFrom(pbUser));
        }

        return newRoom;
    }

    public static Room parseFrom(byte[] data) {
        try {
            return parseFrom(PBRoom.parseFrom(data));
        } catch (InvalidProtocolBufferException e) {
            LogIt.e(User.class, e);
        }

        return null;
    }

    /**
     * Save any Users that haven't yet been saved locally.  
     */
    public static void createRoomUsers(PBRoom room) {
        // Add all the currently active users into the user table
        User.addUsers(room.getUsersList());

        // The inactive user list includes all the users who have ever been in
        // the room.  We need to add these users as the room may contain 
        // messages from users that are no longer in the active users list.
        User.addUsers(room.getInactiveUsersList());
    }

    /**
     * Update this Room with the information in the provided PBRoom.  The
     * ROOM_UPDATE command only includes the information that has changed.
     * 
     * The ROOM_UPDATE will never include a list of users, as ROOM_JOIN and
     * ROOM_LEAVE cover those changes.
     */
    public void updateFromPBRoom(PBRoom roomUpdate) {

        if (roomUpdate.hasName()) {
            LogIt.d(this, "Update room name", roomUpdate.getName());
            setName(roomUpdate.getName());
        }

        if (roomUpdate.hasCoverImageKey()) {
            LogIt.d(this, "Update room cover image",
                    roomUpdate.getCoverImageKey());
            setCoverImageKey(roomUpdate.getCoverImageKey());
        }

        if (roomUpdate.hasProfileImageKey()) {
            LogIt.d(this, "Update room profile image",
                    roomUpdate.getProfileImageKey());
            setProfileImageKey(roomUpdate.getProfileImageKey());

            // Delete the old profile photos from the cache
            //
            // Profile photos are stored by contact ID, not
            // by image key, so old ones must be deleted
            // before the new ones will show up.
            ImageLoader.getInstance().deleteProfilePictureFromCaches(this);
        }

        if (roomUpdate.hasUnnamed()) {
            LogIt.d(this, "Update room unnamed", roomUpdate.getUnnamed());
            setUnnamed(roomUpdate.getUnnamed());
        }

        // These two fields should never change after a room has been created
        if (roomUpdate.hasCreatorID()) {
            LogIt.w(this, "Unexpected 'creator ID' in ROOM_UPDATE");
        }

        if (roomUpdate.hasDateCreated()) {
            LogIt.w(this, "Unexpected 'date created' in ROOM_UPDATE");
        }
    }

    private boolean addMember(RoomMember member) {
        return members.add(member);
    }

    private boolean removeMember(RoomMember memberToRemove) {
        for (RoomMember member : members) {
            if (member.getUserId() == memberToRemove.getUserId()) {
                LogIt.d(this, "Remove room member", memberToRemove.getUserId());
                members.remove(member);
                return true;
            }
        }

        return false;
    }

    public long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(long creatorId) {
        this.creatorId = creatorId;
    }

    public int getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(int dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * @return whether this is an unnamed room.
     */
    public boolean isUnnamed() {
        return unnamed;
    }

    public void setUnnamed(boolean unnamed) {
        this.unnamed = unnamed;

        // Unnamed rooms should not be displayed
        setShown(!unnamed);
    }

    public List<RoomMember> getMembers() {
        return (List<RoomMember>) members;
    }

    public void setMembers(List<RoomMember> members) {
        this.members = members;
    }

    @Override
    public int clear() {
        return delete(true);
    }

    /**
     * Deletes or clears the current channel depending on the given flag
     */
    @Override
    public int delete(boolean isClear) {
        int result = 0;

        try {
            Dao<Message, Long> messageDao = Message.getDao();

            DeleteBuilder<Message, Long> messageDeleteBuilder = messageDao
                    .deleteBuilder();
            messageDeleteBuilder.where().eq(Message.CHANNEL_ID_COLUMN,
                    getRoomId());
            result += messageDeleteBuilder.delete();

            if (!isClear) {
                Dao<Room, Long> roomDao = getDao();

                for (RoomMember member : getMembers()) {
                    member.delete();
                }

                result += roomDao.delete(this);
                LogIt.d(this, "Deleted room", getRoomId());
            } else {
                LogIt.d(this, "Cleared room", getRoomId());
            }
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return result;
    }

    @Override
    public int delete() {
        return delete(false);
    }
}