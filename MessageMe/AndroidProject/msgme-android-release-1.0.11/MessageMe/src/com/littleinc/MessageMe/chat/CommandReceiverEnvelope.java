package com.littleinc.MessageMe.chat;

import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;

/**
 * This class is used to wrap PBCommandEnvelope objects for processing
 * in the CommandReceiver receive queue.  This is required so we can
 * know whether the commands came from the normal web socket connection,
 * or if they came from a REST call after a user pressed Load Earlier Messages.
 */
public class CommandReceiverEnvelope {
    
    public PBCommandEnvelope mCmdEnvelope;
    
    public double mSortedByTimestamp = -1.0;
    
    public CommandReceiverEnvelope(PBCommandEnvelope cmdEnvelope, double sortedByTimestamp) {
        mCmdEnvelope = cmdEnvelope;
        mSortedByTimestamp = sortedByTimestamp;
    }

    public CommandReceiverEnvelope(PBCommandEnvelope cmdEnvelope) {
        mCmdEnvelope = cmdEnvelope;
    }
    
    public PBCommandEnvelope getPBCmdEnvelope() {
        return mCmdEnvelope;
    }
    
    public double getSortedByTimestamp() {
        return mSortedByTimestamp;
    }
    
    public boolean isCmdFromLoadEarlierMessages() {
        // When loading commands from the server mSortedByTimestamp will have
        // been set to something other than -1.0
        return !(mSortedByTimestamp == -1.0);
    }
}
