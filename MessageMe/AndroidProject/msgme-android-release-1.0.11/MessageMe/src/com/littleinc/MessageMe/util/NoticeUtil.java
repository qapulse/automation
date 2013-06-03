package com.littleinc.MessageMe.util;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.Message;
import com.littleinc.MessageMe.bo.NoticeMessage;
import com.littleinc.MessageMe.bo.NoticeType;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandRoomJoin;
import com.littleinc.MessageMe.protocol.Commands.PBCommandRoomLeave;
import com.littleinc.MessageMe.protocol.Commands.PBCommandRoomNew;
import com.littleinc.MessageMe.protocol.Commands.PBCommandRoomUpdate;
import com.littleinc.MessageMe.protocol.Commands.PBCommandUserJoin;

/**
 * Util class created to handle/create the Room notices messages
 *
 */
public class NoticeUtil {

    static NoticeMessage noticeMessage = null;

    private static User currentUser = MessageMeApplication.getCurrentUser();

    /**
     * Creates the corresponding message when the user
     * is added to a new Room, or creates a new one
     */
    public static NoticeMessage createRoomNoticeNewMessage(
            PBCommandEnvelope envelope) {
        String message = "";

        PBCommandRoomNew commandRoomNew = envelope.getRoomNew();

        NoticeMessage noticeMessage = new NoticeMessage();
        noticeMessage.parseFrom(envelope);

        if (noticeMessage.getSenderId() != currentUser.getUserId()) {
            message = MessageMeApplication.getInstance().getString(
                    R.string.room_new_message_added);
            message = String.format(message, noticeMessage.getSender()
                    .getDisplayName());
        } else {
            message = MessageMeApplication.getInstance().getString(
                    R.string.room_new_message);
        }

        noticeMessage.setNotificationMessage(message);
        noticeMessage.setNoticeType(NoticeType.ROOM_NEW);
        noticeMessage.setCreatedAt(commandRoomNew.getDateCreated());
        noticeMessage.setChannelId(commandRoomNew.getRoom().getRoomID());

        return noticeMessage;
    }

    /**
     * Creates the corresponding message when the user adds another
     * user to an existing Room
     */
    public static NoticeMessage createRoomNoticeJoin(PBCommandEnvelope envelope) {
        String message = "";

        PBCommandRoomJoin commandGroupJoin = envelope.getRoomJoin();
        NoticeMessage noticeMessage = new NoticeMessage();
        noticeMessage.parseFrom(envelope);

        if (noticeMessage.getSender() == null) {
            LogIt.w(NoticeUtil.class,
                    "Can't create Notice ROOM_JOIN message, sender is null");
            return null;
        }

        User user = User.parseFrom(commandGroupJoin.getUser());

        noticeMessage.setActionUserID(user.getContactId());

        if (noticeMessage.getSenderId() == currentUser.getUserId()) {

            message = MessageMeApplication.getInstance().getString(
                    R.string.room_join_message_self);

            message = String.format(message, user.getDisplayName());
        } else {
            message = MessageMeApplication.getInstance().getString(
                    R.string.room_join_message_other);

            message = String.format(message, noticeMessage.getSender()
                    .getDisplayName(), user.getDisplayName());
        }

        noticeMessage.setNotificationMessage(message);
        noticeMessage.setNoticeType(NoticeType.ROOM_JOIN);
        noticeMessage.setChannelId(commandGroupJoin.getRoomID());
        noticeMessage.setCreatedAt(commandGroupJoin.getDateCreated());

        return noticeMessage;
    }

    /**
     * Creates the corresponding message when the current user or another
     * user leaves the group
     */
    public static NoticeMessage createRoomNoticeLeave(PBCommandEnvelope envelope) {

        PBCommandRoomLeave commandRoomLeave = envelope.getRoomLeave();

        String message = "";

        NoticeMessage noticeMessage = new NoticeMessage();
        noticeMessage.parseFrom(envelope);

        if (noticeMessage.getSender() == null) {
            LogIt.w(NoticeUtil.class,
                    "Can't create Notice ROOM_LEAVE message, sender is null");
            return null;
        }

        if (noticeMessage.getSenderId() == currentUser.getUserId()) {
            message = MessageMeApplication.getInstance().getString(
                    R.string.room_leave_message_self);
        } else {

            message = MessageMeApplication.getInstance().getString(
                    R.string.room_leave_message_other);
            message = String.format(message, noticeMessage.getSender()
                    .getDisplayName());
        }

        noticeMessage.setNotificationMessage(message);
        noticeMessage.setNoticeType(NoticeType.ROOM_LEAVE);
        noticeMessage.setChannelId(commandRoomLeave.getRoomID());
        noticeMessage.setCreatedAt(commandRoomLeave.getDateCreated());

        return noticeMessage;
    }

    /**
     * Creates the corresponding message when a new user joins messageme
     */
    public static NoticeMessage createUserJoinNotice(PBCommandEnvelope envelope) {

        String message = null;
        PBCommandUserJoin commandUserJoin = envelope.getUserJoin();
        User friendWhoJoined = User.parseFrom(commandUserJoin.getUser());

        NoticeMessage noticeMessage = new NoticeMessage();
        noticeMessage.parseFrom(envelope);

        message = new StringBuilder()
                .append(friendWhoJoined.getDisplayName())
                .append(" ")
                .append(MessageMeApplication.getInstance().getString(
                        R.string.user_banner_desc)).toString();

        noticeMessage.setNotificationMessage(message);
        noticeMessage.setNoticeType(NoticeType.USER_JOIN);
        noticeMessage.setChannelId(friendWhoJoined.getUserId());
        noticeMessage.setCreatedAt(DateUtil.getCurrentTimestamp());

        return noticeMessage;
    }

    /**
     * Creates the corresponding message when the current user or another user 
     * has made an update into the room, which could be the profile image, cover
     * image or the group name
     */
    public static NoticeMessage createRoomNoticeUpdate(
            PBCommandEnvelope envelope) {
        String message = "";

        PBCommandRoomUpdate commandRoomUpdate = envelope.getRoomUpdate();
        NoticeMessage noticeMessage = new NoticeMessage();
        noticeMessage.parseFrom(envelope);

        Room room = Room.parseFrom(commandRoomUpdate.getRoom());
        room.load();

        if (noticeMessage.getSenderId() == currentUser.getUserId()) {
            if (commandRoomUpdate.getRoom().hasProfileImageKey()) {
                message = MessageMeApplication.getInstance().getString(
                        R.string.room_update_message_profile_pic_self);
            } else if (commandRoomUpdate.getRoom().hasCoverImageKey()) {
                message = MessageMeApplication.getInstance().getString(
                        R.string.room_update_message_cover_pic_self);
            } else if (commandRoomUpdate.getRoom().hasName()) {
                message = MessageMeApplication.getInstance().getString(
                        R.string.room_update_message_name_self);

                message = String.format(message, commandRoomUpdate.getRoom()
                        .getName());
            }
        } else {
            if (noticeMessage.getSender() == null) {
                LogIt.w(NoticeUtil.class,
                        "Can't create Notice ROOM_UPDATE message, sender is null");
                return null;
            }
            if (commandRoomUpdate.getRoom().hasProfileImageKey()) {
                message = MessageMeApplication.getInstance().getString(
                        R.string.room_update_message_profile_pic_other);

                message = String.format(message, noticeMessage.getSender()
                        .getDisplayName());

            } else if (commandRoomUpdate.getRoom().hasCoverImageKey()) {
                message = MessageMeApplication.getInstance().getString(
                        R.string.room_update_message_cover_pic_other);

                message = String.format(message, noticeMessage.getSender()
                        .getDisplayName());
            } else if (commandRoomUpdate.getRoom().hasName()) {
                message = MessageMeApplication.getInstance().getString(
                        R.string.room_update_message_name_other);

                message = String.format(message, noticeMessage.getSender()
                        .getDisplayName(), commandRoomUpdate.getRoom()
                        .getName());
            }
        }

        noticeMessage.setChannelId(room.getRoomId());
        noticeMessage.setNotificationMessage(message);
        noticeMessage.setNoticeType(NoticeType.ROOM_NEW);
        noticeMessage.setCreatedAt(commandRoomUpdate.getDateCreated());

        return noticeMessage;
    }

    /**
     * Creates the corresponding message
     * depending of the type of notice
     */
    public static NoticeMessage generateNoticeMessage(
            PBCommandEnvelope envelope, NoticeType type, double nextSortedBy) {

        noticeMessage = null;

        if (envelope.getCommandID() == 0) {
            LogIt.w(MessageMeApplication.getInstance(),
                    "Received Command with ID == 0, ignoring...");
            return null;
        } else {
            // Checks if there's already a Message stored in the DB with the given CommandId
            if (Message.loadByCommandId(envelope.getCommandID()) == null) {
                switch (type) {
                case ROOM_JOIN:
                    LogIt.d(NoticeUtil.class, "ROOM_JOIN notice command",
                            envelope.getCommandID());
                    noticeMessage = createRoomNoticeJoin(envelope);
                    break;
                case ROOM_LEAVE:
                    LogIt.d(NoticeUtil.class, "ROOM_LEAVE notice command",
                            envelope.getCommandID());
                    noticeMessage = createRoomNoticeLeave(envelope);
                    break;
                case ROOM_NEW:
                    LogIt.d(NoticeUtil.class, "ROOM_NEW notice command",
                            envelope.getCommandID());
                    noticeMessage = createRoomNoticeNewMessage(envelope);
                    break;
                case ROOM_UPDATE:
                    LogIt.d(NoticeUtil.class, "ROOM_UPDATE notice command",
                            envelope.getCommandID());
                    noticeMessage = createRoomNoticeUpdate(envelope);
                    break;
                case USER_JOIN:
                    LogIt.d(NoticeUtil.class, "USER_JOIN notice command",
                            envelope.getCommandID());
                    noticeMessage = createUserJoinNotice(envelope);
                    break;
                }

                if (noticeMessage != null) {

                    noticeMessage.setSortedBy(nextSortedBy);
                    noticeMessage.save(false);

                    Conversation.newInstance(noticeMessage).save();
                } else {
                    LogIt.w(NoticeUtil.class, "Notice Message is null");
                }
            } else {
                LogIt.w(NoticeUtil.class,
                        "Room message was already stored in the local DB, ignoring");
            }

        }

        return noticeMessage;
    }
}
