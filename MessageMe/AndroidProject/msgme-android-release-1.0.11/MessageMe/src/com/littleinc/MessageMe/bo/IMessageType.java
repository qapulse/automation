package com.littleinc.MessageMe.bo;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.protocol.Messages.PBMessageEnvelope.MessageType;

/**
 * Enum class created to wrap MessageType from the Protobufs.  This is
 * needed to support some message types that are never actually sent, and 
 * therefore must never be added to the Protobufs.
 * 
 * Message types that will never be sent to the server have an arbitrary
 * large fake Protobuf value (starting at 1000) so they will never clash with 
 * real MessageType values that get added later.
 * 
 * IMPORTANT!!! Do not change ANY of the existing values of this enum below, 
 * as they get saved into the database along with messages!
 */
public enum IMessageType {
    
    TEXT(0,MessageType.TEXT_VALUE),
    DOODLE(1, MessageType.DOODLE_VALUE),
    SONG(2, MessageType.SONG_VALUE),
    VOICE(3, MessageType.VOICE_VALUE),
    PHOTO(4, MessageType.PHOTO_VALUE),
    DOODLE_PIC(5, MessageType.DOODLE_PIC_VALUE),
    LOCATION(6, MessageType.LOCATION_VALUE),
    VIDEO(7, MessageType.VIDEO_VALUE),
    YOUTUBE(8, MessageType.YOUTUBE_VALUE),
    CONTACT(9, MessageType.CONTACT_VALUE),
    NOTICE(10, 1000),
    /**
     * Since this enum value could wrap multiple message types, the real 
     * MessageType for unsupported messages is stored in a custom field in
     * UnsupportedMessage.  Retrieve it using 
     * {@link UnsupportedMessage#getPBMsgType()}.
     */
    UNSUPPORTED(11, 1001);
    
    public static final int TEXT_VALUE = MessageType.TEXT_VALUE;
    public static final int DOODLE_VALUE = MessageType.DOODLE_VALUE;
    public static final int SONG_VALUE = MessageType.SONG_VALUE;
    public static final int VOICE_VALUE = MessageType.VOICE_VALUE;
    public static final int PHOTO_VALUE = MessageType.PHOTO_VALUE;
    public static final int DOODLE_PIC_VALUE = MessageType.DOODLE_PIC_VALUE;
    public static final int LOCATION_VALUE = MessageType.LOCATION_VALUE;
    public static final int VIDEO_VALUE = MessageType.VIDEO_VALUE;
    public static final int YOUTUBE_VALUE = MessageType.YOUTUBE_VALUE;
    public static final int CONTACT_VALUE = MessageType.CONTACT_VALUE;
    public static final int NOTICE_VALUE = 1000;
    public static final int UNSUPPORTED_VALUE = 1001;
    
    public static IMessageType valueOf(int value) {
        switch (value) {
          case MessageType.TEXT_VALUE: return TEXT;
          case MessageType.DOODLE_VALUE: return DOODLE;
          case MessageType.SONG_VALUE: return SONG;
          case MessageType.VOICE_VALUE: return VOICE;
          case MessageType.PHOTO_VALUE: return PHOTO;
          case MessageType.DOODLE_PIC_VALUE: return DOODLE_PIC;
          case MessageType.LOCATION_VALUE: return LOCATION;
          case MessageType.VIDEO_VALUE: return VIDEO;
          case MessageType.YOUTUBE_VALUE: return YOUTUBE;
          case MessageType.CONTACT_VALUE: return CONTACT;
          case NOTICE_VALUE: return NOTICE;
          case UNSUPPORTED_VALUE: return UNSUPPORTED;
          // Any new MessageType values defined in the Protobuf but not defined
          // in our IMessageType must be handled as UnsupportedMessage objects
          default: return UNSUPPORTED;
        }
      }
    
    public MessageType toMessageType(){
        if ((this == UNSUPPORTED) || (this == NOTICE)) {
            throw new UnsupportedOperationException("Cannot create a MessageType from this IMessageType");
        }
        return MessageType.valueOf(getProtobufNumber());
    }
    
    public final int getInternalValue() { return mInternalValue; }
    
    public final int getProtobufNumber() {
        if (this == UNSUPPORTED) {
            // Use UnsupportedMessage.getPBMsgType() instead
            LogIt.w(this, "You should not use the Protobuf number stored in an UnsupportedMessage");
        }
        
        return mProtobufValue; 
    }
    
    private final int mInternalValue;
    private final int mProtobufValue;
    
    IMessageType(int index, int value){
        this.mInternalValue = index;
        this.mProtobufValue = value;
    }
}
