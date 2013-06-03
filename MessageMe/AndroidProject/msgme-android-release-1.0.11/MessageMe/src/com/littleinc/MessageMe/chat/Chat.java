package com.littleinc.MessageMe.chat;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.RoomMember;
import com.littleinc.MessageMe.bo.User;

public class Chat {

    private long mChannelId;

    private boolean mIsRoom;

    private Room groupChat;

    private Set<User> participants = new CopyOnWriteArraySet<User>();

    /**
     * Creates an 1 on 1 chat
     * @param chatManager
     * @param participant
     */
    public Chat(User participant) {
        this.participants.add(participant);
        this.mChannelId = participant.getUserId();
        this.mIsRoom = false;
    }

    /**
     * Creates a Chat Room
     * @param mChannelId
     * @param chatManager
     * @param participants
     */
    public Chat(Room group) {
        super();
        this.groupChat = group;
        this.mChannelId = group.getRoomId();
        this.mIsRoom = true;

        List<User> participants = new LinkedList<User>();
        for (RoomMember member : group.getMembers()) {
            User newUser = new User(member.getUserId());
            newUser.setFirstName(member.getFirstName());
            newUser.setLastName(member.getLastName());

            participants.add(newUser);
        }

        this.participants.addAll(participants);
    }

    public User getParticipant(long contactId) {
        for (Iterator<User> participant = participants.iterator(); participant
                .hasNext();) {
            User user = (User) participant.next();

            if (user.getUserId() == contactId)
                return user;
        }

        return null;
    }

    public boolean addParticipand(User user) {
        return participants.add(user);
    }

    public boolean removeParticipant(User user) {
        for (User existingParticipant : participants) {
            if (existingParticipant.getUserId() == user.getUserId()) {
                return participants.remove(existingParticipant);
            }
        }

        return false;
    }

    public long getChatId() {
        return mChannelId;
    }

    public void setChatId(Long chatId) {
        this.mChannelId = chatId;
    }

    public Set<User> getParticipants() {
        return participants;
    }

    public boolean isChatRoom() {
        return (participants.size() > 0);
    }

    public boolean isGroupChat() {
        return (mIsRoom && (groupChat != null));
    }
}