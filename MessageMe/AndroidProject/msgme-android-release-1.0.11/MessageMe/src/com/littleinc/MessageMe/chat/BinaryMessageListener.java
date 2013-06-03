package com.littleinc.MessageMe.chat;

import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;

/**
 * Provides a mechanism to listen all messages in its "raw" form.
 * @see ChatConnection#addMessageListener(BinaryMessageListener)
 * @author LogN LLC 2012
 *
 */
public interface BinaryMessageListener {

    public void processBinaryMessage(PBCommandEnvelope envelop);

}
