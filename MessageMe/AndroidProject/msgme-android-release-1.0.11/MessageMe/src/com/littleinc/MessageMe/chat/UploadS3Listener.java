package com.littleinc.MessageMe.chat;


/**
 * Allows UI components to listen media upload events
 */
public interface UploadS3Listener {

    /**
     * Notify a listener that the media upload has completed.
     * 
     * @param mediaKey the media key used by S3 as the name of the uploaded file
     */
	public void onUploadCompleted(String mediaKey);

	public void onUploadError(String messageTitle, String messageError);
}
