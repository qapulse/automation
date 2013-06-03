package com.littleinc.MessageMe.error;

import java.io.File;

import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.bo.IMessageType;
import com.littleinc.MessageMe.bo.User;

public class ImageUploadException extends Exception {
    
    /**
     * Specify the "version" of this class.  If its member fields change
     * then this value should be updated to ensure that newer code 
     * cannot deserialize an old serialized object (as the 
     * deserialization code will not know how to read the old object any
     * more).
     */
    private static final long serialVersionUID = 1L;
    
    private long mUserId;
    
    private String mCause;

    private File mUploadFile;

    private IMessageType mType;
    
    public ImageUploadException(Throwable e, File fileToUpload, IMessageType type) {
        super();

        User currentUser = MessageMeApplication.getCurrentUser();
        
        if (currentUser == null) {
            mUserId = -1;
        } else {            
            mUserId = currentUser.getUserId();
        }
        
        if (e == null) {
            mCause = "<no exception>";
        } else {            
            mCause = e.toString();
        }
        
        mUploadFile = fileToUpload;
        mType = type;
    }

    @Override
    public String getMessage() {
        return new StringBuilder().append("Image upload failed")
                .append(", user ").append(mUserId)
                .append(", type ").append(mType.toString())
                .append(", file ").append(mUploadFile)
                .append(", caused by ").append(mCause).toString();
    }
}
