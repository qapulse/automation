package com.littleinc.MessageMe.bo;

import java.sql.SQLException;

import com.coredroid.util.LogIt;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.littleinc.MessageMe.data.DataBaseHelper;

/**
 * Model to store the relation between the AB contact id and the messageme user id
 * 
 * This entity will be useful in the invite address book friends activity to exclude the already 
 * message me users from the list
 */
@DatabaseTable(tableName = MatchedABRecord.TABLE_NAME)
public class MatchedABRecord {

    public static final String TABLE_NAME = "matched_ab_record";

    public static final String USER_ID = "user_id";

    public static final String AB_RECORD_ID = "ab_record_id";

    @DatabaseField(columnName = MatchedABRecord.USER_ID, canBeNull = false, dataType = DataType.LONG)
    private long userId;

    @DatabaseField(columnName = MatchedABRecord.AB_RECORD_ID, id = true, canBeNull = false, dataType = DataType.LONG)
    private long abRecordId;

    public MatchedABRecord() {
    }

    public MatchedABRecord(long abRecordId, long userId) {
        setAbRecordId(abRecordId);
        setUserId(userId);
    }

    /**
     * This method saves the current instance into the database if the given abRecordId is not
     * present already
     */
    public static void save(long abRecordId, long userId) {
        Dao<MatchedABRecord, Long> dao = getDao();

        MatchedABRecord matchedABRecord;
        try {
            matchedABRecord = dao.queryForId(abRecordId);

            if (matchedABRecord == null) {
                LogIt.d(MatchedABRecord.class, "Adding MatchedABRecord",
                        abRecordId, userId);
                matchedABRecord = new MatchedABRecord(abRecordId, userId);
                dao.create(matchedABRecord);
            } else {
                LogIt.d(MatchedABRecord.class,
                        "MatchedABRecord already exists", abRecordId, userId);
            }
        } catch (SQLException e) {
            LogIt.e(MatchedABRecord.class, e);
        }
    }

    public static Dao<MatchedABRecord, Long> getDao() {
        try {
            return DataBaseHelper.getInstance().getDao(MatchedABRecord.class);
        } catch (SQLException e) {
            LogIt.e(MatchedABRecord.class, e);
        }

        return null;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getAbRecordId() {
        return abRecordId;
    }

    public void setAbRecordId(long abRecordId) {
        this.abRecordId = abRecordId;
    }
}