package com.littleinc.MessageMe.bo;

import java.sql.SQLException;

import com.coredroid.util.LogIt;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.DatabaseTable;
import com.littleinc.MessageMe.data.DataBaseHelper;
import com.littleinc.MessageMe.protocol.Objects.PBUser;

@DatabaseTable(tableName = RoomMember.TABLE_NAME)
public class RoomMember {

    public static final String TABLE_NAME = "room_member";

    public static final String ID_COLUMN = "id";

    public static final String ROOM_ID_COLUMN = "room_id";

    public static final String USER_ID_COLUMN = "user_id";

    public static final String LAST_NAME_COLUMN = "last_name";

    public static final String FIRST_NAME_COLUMN = "first_name";

    @DatabaseField(columnName = RoomMember.ID_COLUMN, generatedId = true, dataType = DataType.LONG)
    private long id;

    @DatabaseField(columnName = RoomMember.ROOM_ID_COLUMN, foreign = true, foreignAutoCreate = true, foreignAutoRefresh = true)
    private Room room;

    @DatabaseField(columnName = RoomMember.USER_ID_COLUMN, dataType = DataType.LONG)
    private long userId;

    @DatabaseField(columnName = RoomMember.LAST_NAME_COLUMN, dataType = DataType.STRING)
    private String lastName;

    @DatabaseField(columnName = RoomMember.FIRST_NAME_COLUMN, dataType = DataType.STRING)
    private String firstName;

    public RoomMember() {
    }

    public RoomMember(User user) {
        setUserId(user.getUserId());
        setLastName(user.getLastName());
        setFirstName(user.getFirstName());
    }

    public RoomMember(User user, Room room) {
        setRoom(room);
        setUserId(user.getUserId());
        setLastName(user.getLastName());
        setFirstName(user.getFirstName());
    }

    public static Dao<RoomMember, Long> getDao() {
        try {
            return DataBaseHelper.getInstance().getDao(RoomMember.class);
        } catch (SQLException e) {
            LogIt.e(RoomMember.class, e, e.getMessage());
        }

        return null;
    }

    public long save() {
        try {
            Dao<RoomMember, Long> dao = getDao();
            dao.createOrUpdate(this);
            return dao.extractId(this);
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    public boolean exists() {
        Dao<RoomMember, Long> dao = getDao();

        try {
            QueryBuilder<RoomMember, Long> queryBuilder = dao.queryBuilder();
            Where<RoomMember, Long> where = queryBuilder.where();

            where.eq(RoomMember.USER_ID_COLUMN, getUserId());
            where.and();
            where.eq(RoomMember.ROOM_ID_COLUMN, getRoom().getRoomId());

            RoomMember member = queryBuilder.queryForFirst();

            if (member != null) {
                return true;
            }
        } catch (SQLException e) {
            LogIt.e(this, e);
        }

        return false;
    }

    public long delete() {
        try {
            Dao<RoomMember, Long> dao = getDao();
            DeleteBuilder<RoomMember, Long> deleteBuilder = dao.deleteBuilder();
            Where<RoomMember, Long> where = deleteBuilder.where();

            where.eq(RoomMember.ROOM_ID_COLUMN, room.getRoomId());
            where.and();
            where.eq(RoomMember.USER_ID_COLUMN, getUserId());

            return deleteBuilder.delete();
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    public PBUser toPBUser() {
        PBUser.Builder builder = PBUser.newBuilder();

        builder.setUserID(getUserId());
        builder.setFirstName(getFirstName());
        builder.setLastName(getLastName());

        return builder.build();
    }

    public static RoomMember parseFrom(User user) {
        RoomMember newGroupMember = new RoomMember();

        newGroupMember.setUserId(user.getUserId());
        newGroupMember.setLastName(user.getLastName());
        newGroupMember.setFirstName(user.getFirstName());

        return newGroupMember;
    }

    public static RoomMember parseFrom(PBUser user) {
        RoomMember newGroupMember = new RoomMember();

        newGroupMember.setUserId(user.getUserID());
        newGroupMember.setLastName(user.getLastName());
        newGroupMember.setFirstName(user.getFirstName());

        return newGroupMember;
    }
    
    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }
}