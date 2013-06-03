package com.littleinc.MessageMe.util;

import com.littleinc.MessageMe.data.DataBaseHelper;

/**
 * Class to perform large writing interactions with the database.
 * These tasks will be added into a queue in the {@link DataBaseHelper} to be processed in the background 
 */
public abstract class BatchTask {

    public BatchTask() {
        DataBaseHelper.getInstance().addBatchTask(this);
    }

    public abstract void work();
}