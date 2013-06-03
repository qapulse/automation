package com.littleinc.MessageMe.bo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.restlet.resource.ResourceException;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.google.protobuf.InvalidProtocolBufferException;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.DatabaseTable;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.data.DataBaseHelper;
import com.littleinc.MessageMe.net.RestfulClient;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Objects.PBUser;
import com.littleinc.MessageMe.ui.ContactProfileActivity;
import com.littleinc.MessageMe.util.DatabaseTask;
import com.littleinc.MessageMe.util.ImageLoader;
import com.littleinc.MessageMe.util.NetUtil;

@DatabaseTable(tableName = User.TABLE_NAME)
public class User extends Contact {

    public static final String TABLE_NAME = "user";

    public static final String PHONE = "phone";

    public static final String PIN = "pin";

    public static final String IS_BLOCKED = "is_blocked";

    public static final String BLOCKED_BY = "blocked_by";

    public static final String LOCAL_FIRST_NAME = "local_first_name";

    public static final String LOCAL_LAST_NAME = "local_last_name";

    private static Dao<User, Long> sDao;

    @DatabaseField(columnName = User.PHONE, dataType = DataType.STRING)
    private String phone;

    @DatabaseField(columnName = User.PIN, dataType = DataType.STRING)
    private String pin;

    @DatabaseField(columnName = User.IS_BLOCKED, dataType = DataType.BOOLEAN)
    private boolean isBlocked = false;

    @DatabaseField(columnName = User.BLOCKED_BY, dataType = DataType.BOOLEAN)
    private boolean blockedBy = false;

    @DatabaseField(columnName = User.LOCAL_FIRST_NAME, canBeNull = true, dataType = DataType.STRING)
    private String localFirstName;

    @DatabaseField(columnName = User.LOCAL_LAST_NAME, canBeNull = true, dataType = DataType.STRING)
    private String localLastName;

    protected boolean online;

    private String email;

    public User() {
        init();
    }

    public User(long userId) {
        init();
        this.setContactId(userId);
    }

    private void init() {
        setContactType(ContactType.USER.getValue());

        // Users will only be shown once they are added as a Contact 
        setShown(false);
    }

    public long save() {
        try {
            Dao<User, Long> dao = getDao();
            dao.createOrUpdate(this);
            return dao.extractId(this);
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return 0;
    }

    /**
     * Checks whether this User exists in the local database.
     */
    public static boolean exists(long contactId) {
        Dao<User, Long> dao = getDao();

        try {
            return dao.idExists(contactId);
        } catch (SQLException e) {
            LogIt.e(User.class, e);
        }

        return false;
    }

    @Override
    public void load() {
        LogIt.d(this, "Loading User " + getUserId());

        try {
            Dao<User, Long> userDao = getDao();
            User loadedUser = userDao.queryForId(getUserId());

            if (loadedUser != null) {
                setPin(loadedUser.getPin());
                setShown(loadedUser.isShown());
                setPhone(loadedUser.getPhone());
                setContactId(loadedUser.getUserId());
                setLastName(loadedUser.getLastName());
                setFirstName(loadedUser.getFirstName());
                setCoverImageKey(loadedUser.getCoverImageKey());
                setProfileImageKey(loadedUser.getProfileImageKey());
                setIsBlocked(loadedUser.isBlocked());
                setBlockedBy(loadedUser.isBlockedBy());
            } else {
                LogIt.w(this, "User " + getUserId()
                        + " doesn't exist in the database");
            }
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }
    }

    public static Dao<User, Long> getDao() {

        if (sDao == null) {
            try {
                sDao = DataBaseHelper.getInstance().getDao(User.class);

                // Check here for more information about ORMLite cache documentation
                // http://ormlite.com/javadoc/ormlite-core/doc-files/ormlite_5.html#Object-Caches
                sDao.setObjectCache(true);

            } catch (SQLException e) {
                LogIt.e(User.class, e, e.getMessage());
            }
        }

        return sDao;
    }

    public PBUser toPBUser() {
        PBUser.Builder builder = PBUser.newBuilder();

        builder.setUserID(getUserId());

        if (!TextUtils.isEmpty(getFirstName())) {
            builder.setFirstName(getFirstName());
        }

        if (!TextUtils.isEmpty(getLastName())) {
            builder.setLastName(getLastName());
        }

        if (!TextUtils.isEmpty(getCoverImageKey())) {
            builder.setCoverImageKey(getCoverImageKey());
        }

        if (!TextUtils.isEmpty(getProfileImageKey())) {
            builder.setProfileImageKey(getProfileImageKey());
        }

        if (!TextUtils.isEmpty(getPhone())) {
            builder.setPhone(getPhone());
        }

        if (!TextUtils.isEmpty(getPin())) {
            builder.setPin(getPin());
        }

        builder.setIsBlocked(isBlocked());
        builder.setBlockedBy(isBlockedBy());

        return builder.build();
    }

    public static User parseFrom(PBUser user) {
        User newUser = new User();

        newUser.setContactId(user.getUserID());
        newUser.setLastName(user.getLastName());
        newUser.setFirstName(user.getFirstName());
        newUser.setCoverImageKey(user.getCoverImageKey());
        newUser.setProfileImageKey(user.getProfileImageKey());
        newUser.setPhone(user.getPhone());
        newUser.setPin(user.getPin());
        newUser.setIsBlocked(user.getIsBlocked());
        newUser.setBlockedBy(user.getBlockedBy());

        return newUser;
    }

    public static User parseFrom(byte[] data) {
        try {
            return parseFrom(PBUser.parseFrom(data));
        } catch (InvalidProtocolBufferException e) {
            LogIt.e(User.class, e);
        }

        return null;
    }

    /**
     * Update this User with the information in the provided PBUser.  The
     * USER_UPDATE command only includes the information that has changed,
     * which at the moment can only be the profile or cover photos.
     */
    public void updateFromPBUser(PBUser userUpdate) {

        if (userUpdate.hasCoverImageKey()) {
            LogIt.d(this, "Update user cover image",
                    userUpdate.getCoverImageKey());
            setCoverImageKey(userUpdate.getCoverImageKey());
        }

        if (userUpdate.hasProfileImageKey()) {
            LogIt.d(this, "Update user profile image",
                    userUpdate.getProfileImageKey());
            setProfileImageKey(userUpdate.getProfileImageKey());

            // Delete the old profile photos from the cache
            //
            // Profile photos are stored by contact ID, not
            // by image key, so old ones must be deleted
            // before the new ones will show up.
            ImageLoader.getInstance().deleteProfilePictureFromCaches(this);
        }

        if (userUpdate.hasFirstName()) {
            LogIt.d(this, "Update user first name", userUpdate.getFirstName());
            setFirstName(userUpdate.getFirstName());
        }

        if (userUpdate.hasLastName()) {
            LogIt.d(this, "Update user last name", userUpdate.getLastName());
            setLastName(userUpdate.getLastName());
        }

        if (userUpdate.hasIsBlocked()) {
            LogIt.d(this, "Update isBlocked", userUpdate.getIsBlocked());
            setIsBlocked(userUpdate.getIsBlocked());
        }

        if (userUpdate.hasBlockedBy()) {
            LogIt.d(this, "Update blockedBy", userUpdate.getBlockedBy());
            setBlockedBy(userUpdate.getBlockedBy());
        }
    }

    public static List<User> parseFrom(List<PBUser> list) {
        List<User> users = new LinkedList<User>();

        for (PBUser pbUser : list) {
            users.add(User.parseFrom(pbUser));
        }

        LogIt.i(User.class,
                users.size() + " contacts loaded from " + list.size()
                        + " PBUsers");

        return users;
    }

    /**
     * Returns the contact list
     * Only the contacts with isShown true ("Friends") will be in this list
     */
    public static List<User> getContactList() {
        try {
            LogIt.d(User.class, "Loading contact list");
            Dao<User, Long> userDao = getDao();
            QueryBuilder<User, Long> queryBuilder = userDao.queryBuilder();
            Where<User, Long> where = queryBuilder.where();

            where.eq(User.IS_SHOWN_COLUMN, true);
            return queryBuilder.query();
        } catch (SQLException e) {
            LogIt.e(User.class, e, e.getMessage());
        }

        return null;
    }

    public static List<User> search(String name) {

        try {
            LogIt.d(User.class, "Loading contact list");
            Dao<User, Long> userDao = getDao();
            QueryBuilder<User, Long> queryBuilder = userDao.queryBuilder();
            Where<User, Long> where = queryBuilder.where();

            where.eq(User.IS_SHOWN_COLUMN, true);
            where.and();
            where.like(User.FIRST_NAME_COLUMN, "%" + name + "%");
            where.or();
            where.like(User.LAST_NAME_COLUMN, "%" + name + "%");

            return queryBuilder.query();
        } catch (SQLException e) {
            LogIt.e(User.class, e, e.getMessage());
        }

        return null;
    }

    /**
     * Saves the provided list of Users in the local database
     */
    public static void addUsers(List<PBUser> users) {
        User currentUser = MessageMeApplication.getCurrentUser();

        try {
            for (PBUser pbUser : users) {
                if (currentUser.getContactId() != pbUser.getUserID()) {
                    User user = User.parseFrom(pbUser);

                    if (User.getDao().idExists(user.getUserId())) {
                        user.load();
                        LogIt.d(User.class, "Updating user", user.getUserId(),
                                user.getDisplayName());
                    } else {
                        LogIt.d(User.class, "Adding user", user.getUserId(),
                                user.getDisplayName());
                    }

                    user.save();
                }
            }
        } catch (SQLException e) {
            LogIt.e(User.class, e);
        }
    }

    public void toggleBlock(final Context context, final long contactID,
            final boolean isBlockingThisUser,
            final UserBlockingListener listener) {

        if (!NetUtil.checkInternetConnection(context)) {
            LogIt.w(this, "No connection toggling friendship");
            UIUtil.alert(context, R.string.network_error_title,
                    R.string.network_error);
            return;
        } else {
            String progressMsg;

            if (isBlockingThisUser) {
                progressMsg = context
                        .getString(R.string.profile_block_contact_action);
            } else {
                progressMsg = context
                        .getString(R.string.profile_unblock_contact_action);
            }

            final ProgressDialog progressDialog = UIUtil.showProgressDialog(
                    context, progressMsg);

            progressDialog.setCancelable(true);

            new BackgroundTask() {

                private UserBlockingState state;

                @Override
                public void work() {
                    PBCommandEnvelope commandEnvelope;

                    try {
                        if (isBlockingThisUser) {
                            commandEnvelope = RestfulClient.getInstance()
                                    .userBlock(contactID);
                            state = UserBlockingState.USER_BLOCKED;
                        } else {
                            commandEnvelope = RestfulClient.getInstance()
                                    .userUnblock(contactID);
                            state = UserBlockingState.USER_UNBLOCKED;
                        }

                        if (commandEnvelope.hasError()) {
                            LogIt.d(ContactProfileActivity.class,
                                    "Error blocking/unblocking user", contactID);

                            state = UserBlockingState.USER_BLOCKING_ERROR;
                            fail(context
                                    .getString(R.string.unexpected_error_title),
                                    context.getString(R.string.unexpected_error));
                        } else if (commandEnvelope.hasUserBlock()) {
                            LogIt.d(ContactProfileActivity.class,
                                    "Blocked user", contactID);
                        } else if (commandEnvelope.hasUserUnblock()) {
                            LogIt.d(ContactProfileActivity.class,
                                    "Unblocked user", contactID);
                        } else {
                            LogIt.e(ContactProfileActivity.class,
                                    "Unexpected condition");
                            fail(context
                                    .getString(R.string.unexpected_error_title),
                                    context.getString(R.string.unexpected_error));
                        }
                    } catch (ResourceException e) {
                        LogIt.w(ContactProfileActivity.class, e);
                        fail(context.getString(R.string.network_error_title),
                                context.getString(R.string.network_error));
                    } catch (IOException e) {
                        LogIt.w(ContactProfileActivity.class, e);
                        fail(context.getString(R.string.network_error_title),
                                context.getString(R.string.network_error));
                    } catch (Exception e) {
                        LogIt.e(ContactProfileActivity.class, e);
                        fail(context.getString(R.string.unexpected_error_title),
                                context.getString(R.string.unexpected_error));
                    }
                }

                @Override
                public void done() {
                    if (!failed()) {
                        final User userToUpdate = new User(contactID);

                        new DatabaseTask() {

                            @Override
                            public void work() {
                                LogIt.d(ContactProfileActivity.class,
                                        "Update blocked state for contact",
                                        contactID, isBlockingThisUser);
                                userToUpdate.load();
                                userToUpdate.setIsBlocked(isBlockingThisUser);
                                userToUpdate.save();

                                // Inform listeners that this user state has changed
                                Intent intent = new Intent(
                                        MessageMeConstants.INTENT_NOTIFY_USER_CHANGED);
                                intent.putExtra(
                                        MessageMeConstants.RECIPIENT_USER_KEY,
                                        userToUpdate.toPBUser().toByteArray());

                                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager
                                        .getInstance(context);
                                localBroadcastManager.sendBroadcast(intent);
                            }

                            @Override
                            public void done() {
                                listener.onUserBlockingResponse(state,
                                        userToUpdate);
                                progressDialog.dismiss();
                            }
                        };
                    } else {
                        progressDialog.dismiss();
                        listener.onUserBlockingResponse(state, null);
                        UIUtil.alert(context, getExceptionTitle(),
                                getExceptionMessage());
                    }
                }
            };
        }

    }

    /**
     * This method checks if the user is a friend and toggle between friend and unfriend
     */
    public void toggleFriendship(final Context context,
            final UserFriendshipListener listener) {
        if (!NetUtil.checkInternetConnection(context)) {
            LogIt.w(this, "No connection toggling friendship");
            UIUtil.alert(context, R.string.network_error_title,
                    R.string.network_error);
            return;
        } else {

            String progressMsg;

            // If the user isShown then we are unfriending them now
            if (isShown()) {
                progressMsg = context.getString(R.string.removing_contact);
            } else {
                progressMsg = context.getString(R.string.adding_contact);
            }

            final ProgressDialog progressDialog = UIUtil.showProgressDialog(
                    context, progressMsg);
            progressDialog.setCancelable(true);

            new BackgroundTask() {

                private UserFriendshipState state;

                @Override
                public void work() {
                    PBCommandEnvelope commandEnvelope;

                    try {
                        if (isShown()) {
                            commandEnvelope = RestfulClient.getInstance()
                                    .userUnfriend(getUserId());
                        } else {
                            commandEnvelope = RestfulClient.getInstance()
                                    .userFriend(getUserId());
                        }

                        if (commandEnvelope.hasUserFriend()) {
                            LogIt.d(User.class, "Added friendship with user ",
                                    getUserId());

                            performFriend(commandEnvelope, false);
                            state = UserFriendshipState.USER_FRIENDED;
                        } else if (commandEnvelope.hasUserUnfriend()) {
                            LogIt.d(User.class,
                                    "Removed friendship with user ",
                                    getUserId());

                            performUnfriend(commandEnvelope, false);
                            state = UserFriendshipState.USER_UNFRIENDED;
                        } else {
                            state = UserFriendshipState.USER_FRIENDSHIP_ERROR;
                            fail(commandEnvelope.getError().getReason());
                        }
                    } catch (ResourceException e) {
                        LogIt.e(User.class, e);
                        fail(context.getString(R.string.network_error_title),
                                context.getString(R.string.network_error));
                    } catch (IOException e) {
                        LogIt.e(User.class, e);
                        fail(context.getString(R.string.network_error_title),
                                context.getString(R.string.network_error));
                    }
                }

                @Override
                public void done() {
                    progressDialog.dismiss();
                    if (!failed()) {
                        if (listener != null) {
                            // Calling callback
                            listener.onUserFriendshipResponse(state);
                        }
                    } else {
                        UIUtil.alert(context, getExceptionTitle(),
                                getExceptionMessage());
                    }
                }
            };
        }
    }

    /**
     * This method updates the isShown property of each new friendship
     */
    public static void performFriend(PBCommandEnvelope commandEnvelope,
            boolean isInBatch) {
        for (PBUser pbUser : commandEnvelope.getUserFriend().getUsersList()) {
            User user = User.parseFrom(pbUser);
            user.setShown(true);
            user.save();

            if (!isInBatch) {
                LogIt.d(User.class, "Friended user", user.getDisplayName(),
                        user.isBlocked(), user.isBlockedBy());
            }
        }

        if (commandEnvelope.hasCommandID()) {
            // USER_FRIEND action must update the current user cursor 
            MessageMeApplication.getCurrentUser().updateCursor(
                    commandEnvelope.getCommandID(), isInBatch);
        }
    }

    /**
     * This method updates the isShown property of every friendship removed
     */
    public static void performUnfriend(PBCommandEnvelope commandEnvelope,
            boolean isInBatch) {
        for (Long userId : commandEnvelope.getUserUnfriend().getUserIDsList()) {
            User user = new User(userId);
            user.load();
            user.setShown(false);
            user.save();

            if (!isInBatch) {
                LogIt.d(User.class, "Unfriended user", user.getDisplayName(),
                        user.isBlocked(), user.isBlockedBy());
            }
        }

        if (commandEnvelope.hasCommandID()) {
            // USER_UNFRIEND action must update the current user cursor
            MessageMeApplication.getCurrentUser().updateCursor(
                    commandEnvelope.getCommandID(), isInBatch);
        }
    }

    public static String getDisplayNameFromPBUser(PBUser pbUser) {
        StringBuilder displayName = new StringBuilder("");

        String pbFirstName = pbUser.getFirstName();
        String pbLastName = pbUser.getLastName();

        if (!TextUtils.isEmpty(pbFirstName)) {
            displayName.append(pbFirstName.trim());
        }

        if (!TextUtils.isEmpty(pbLastName)) {
            displayName.append(" ");
            displayName.append(pbLastName.trim());
        }

        return displayName.toString();
    }

    public long getUserId() {
        return getContactId();
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnlinePresence(boolean online) {
        this.online = online;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        LogIt.d(this, "Set user e-mail address", email);
        this.email = email;
    }

    public boolean isBlockedBy() {
        return blockedBy;
    }

    public void setBlockedBy(boolean isBlockedBy) {
        this.blockedBy = isBlockedBy;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setIsBlocked(boolean isBlocked) {
        this.isBlocked = isBlocked;
    }

    /**
     * Enum with the with the possible friendship states
     */
    public enum UserFriendshipState {
        USER_FRIENDED, USER_UNFRIENDED, USER_FRIENDSHIP_ERROR
    }

    public enum UserBlockingState {
        USER_BLOCKED, USER_UNBLOCKED, USER_BLOCKING_ERROR
    }

    /**
     * Interface to be implemented as callback at the callers of toggleFriendship method
     */
    public interface UserFriendshipListener {
        void onUserFriendshipResponse(UserFriendshipState state);
    }

    /**
     * Interface to be implemented as a callback at the callers of toggleBlock method
     */
    public interface UserBlockingListener {
        void onUserBlockingResponse(UserBlockingState state, User user);
    }

    @Override
    public int delete() {
        return delete(false);
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
            Dao<MessageMeCursor, Long> cursorDao = MessageMeCursor.getDao();

            DeleteBuilder<Message, Long> messageDeleteBuilder = messageDao
                    .deleteBuilder();
            messageDeleteBuilder.where().eq(Message.CHANNEL_ID_COLUMN,
                    getUserId());
            result += messageDeleteBuilder.delete();

            if (!isClear) {
                DeleteBuilder<MessageMeCursor, Long> cursorDeleteBuilder = cursorDao
                        .deleteBuilder();
                cursorDeleteBuilder.where().eq(MessageMeCursor.CHANNEL_ID,
                        getUserId());

                result += cursorDeleteBuilder.delete();
                LogIt.d(this, "Deleted user", getUserId());
            } else {
                LogIt.d(this, "Cleared channel", getUserId());
            }
        } catch (SQLException e) {
            LogIt.e(this, e, e.getMessage());
        }

        return result;
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof User) ? contactId == ((User) object).contactId
                : (object == this);
    }
}