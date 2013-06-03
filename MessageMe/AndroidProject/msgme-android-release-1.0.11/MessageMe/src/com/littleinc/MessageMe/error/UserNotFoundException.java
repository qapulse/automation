package com.littleinc.MessageMe.error;

import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.bo.User;

public class UserNotFoundException extends Exception {

    /**
     * Specify the "version" of this class.  If its member fields change
     * then this value should be updated to ensure that newer code 
     * cannot deserialize an old serialized object (as the 
     * deserialization code will not know how to read the old object any
     * more).
     */
    private static final long serialVersionUID = 1L;
    
    private long mUserId;
    
    private long mReaderId;
    
    private long mChannelId;
    
    public UserNotFoundException(long channelId, long readerId) {
        super();

        User currentUser = MessageMeApplication.getCurrentUser();
        
        if (currentUser == null) {
            mUserId = -1;
        } else {            
            mUserId = currentUser.getUserId();
        }
        
        mReaderId = readerId;
        mChannelId = channelId;
    }
    
    @Override
    public String getMessage() {
        return new StringBuilder()
                .append("Reader not found in database. Current user=")
                .append(mUserId).append(" ChannelID=").append(mChannelId)
                .append(" ReaderID=").append(mReaderId).toString();
    }
}
