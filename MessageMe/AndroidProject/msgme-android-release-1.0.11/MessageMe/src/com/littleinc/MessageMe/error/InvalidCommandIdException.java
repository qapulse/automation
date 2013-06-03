package com.littleinc.MessageMe.error;

public class InvalidCommandIdException extends Exception {

    /**
     * Specify the "version" of this class.  If its member fields change
     * then this value should be updated to ensure that newer code 
     * cannot deserialize an old serialized object (as the 
     * deserialization code will not know how to read the old object any
     * more).
     */
    private static final long serialVersionUID = 1L;

    private long mChannelId;

    private long mCurrentUserId;

    private long mInvalidCommandId;

    public InvalidCommandIdException(long currentUserId, long channelId,
            long invalidCommandId) {
        super();

        mChannelId = channelId;
        mCurrentUserId = currentUserId;
        mInvalidCommandId = invalidCommandId;
    }

    @Override
    public String getMessage() {
        return new StringBuilder().append("Invalid commandId ")
                .append(getInvalidCommandId()).append(", in user ")
                .append(getCurrentUserId()).append(", for channel ")
                .append(getChannelId()).toString();
    }

    public long getCurrentUserId() {
        return mCurrentUserId;
    }

    public long getChannelId() {
        return mChannelId;
    }

    public long getInvalidCommandId() {
        return mInvalidCommandId;
    }
}