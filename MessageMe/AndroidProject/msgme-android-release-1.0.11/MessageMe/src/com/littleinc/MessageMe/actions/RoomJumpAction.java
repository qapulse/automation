package com.littleinc.MessageMe.actions;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.RoomMember;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandRoomJump;
import com.littleinc.MessageMe.protocol.Objects.PBRoom;

public class RoomJumpAction extends BaseAction {

    private RoomJumpAction(Builder builder) {
        super(builder);
    }

    /**
     * Process a received ROOM_JUMP command
     */
    public IMessage process() {

        PBCommandRoomJump commandRoomJump = mCommandEnvelope.getRoomJump();
        PBRoom pbRoom = commandRoomJump.getRoom();
        long channelId = 0;

        if (pbRoom.getRoomID() == 0) {
            channelId = Room.getChannelIdFromPrivateRoom(pbRoom);

            if (User.exists(channelId)) {
                LogIt.d(MessagingService.class, "ROOM_JUMP for private chat",
                        channelId);

                User existingUser = new User(channelId);
                existingUser.load();
                existingUser.clear();
            } else {
                LogIt.w(MessagingService.class,
                        "ROOM_JUMP received for unknown private chat");
                return null;
            }
        } else {
            channelId = pbRoom.getRoomID();

            if (Room.exists(pbRoom.getRoomID())) {

                LogIt.d(MessagingService.class, "ROOM_JUMP for group",
                        pbRoom.getRoomID());

                // Delete our old Room object and recreate it based on the 
                // new one in the command jump
                Room existingRoom = new Room(pbRoom.getRoomID());
                existingRoom.load();
                existingRoom.clear();

                for (RoomMember member : existingRoom.getMembers()) {
                    member.delete();
                }

                Room updatedRoom = Room.parseFrom(pbRoom);
                Room.createRoomUsers(pbRoom);
                updatedRoom.save();
            } else {
                LogIt.w(MessagingService.class,
                        "ROOM_JUMP received for unknown group",
                        pbRoom.getRoomID(), pbRoom.getName());
                return null;
            }

            if (!mInBatch) {
                mMessagingService
                        .notifyChatClient(MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST);

                // The room name might have changed so update Contacts too
                mMessagingService
                        .notifyChatClient(MessageMeConstants.INTENT_NOTIFY_CONTACT_LIST);
            }
        }

        if (mConversation == null) {
            mConversation = Conversation.newInstance(channelId);
        }

        mConversation.setUnreadCount(0);
        mConversation.save();

        return null;
    }

    public static class Builder extends BaseAction.Builder {

        public Builder(MessagingService messagingService,
                PBCommandEnvelope commandEnvelope) {
            super(messagingService, commandEnvelope);
        }

        @Override
        public Builder inBatch(boolean isInBatch) {

            mInBatch = isInBatch;
            return this;
        }

        @Override
        public Builder nextSortedBy(double nextSortedBy) {

            mNextSortedBy = nextSortedBy;
            return this;
        }

        @Override
        public Builder conversation(Conversation conversation) {

            mConversation = conversation;
            return this;
        }

        @Override
        public RoomJumpAction build() {
            return new RoomJumpAction(this);
        }
    }
}
