package com.littleinc.MessageMe.chat;


public interface MediaDownloadListener {
    
    public void onDownloadCompleted(String mediaKey);

    public void onDownloadError(String messageError);

}
