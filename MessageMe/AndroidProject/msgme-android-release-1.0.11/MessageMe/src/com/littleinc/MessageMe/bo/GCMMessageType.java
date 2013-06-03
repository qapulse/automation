package com.littleinc.MessageMe.bo;


public enum GCMMessageType {

    // "pm" message
    GCM_TEXT("pmt"), 
    GCM_PHOTO("pmp"), 
    GCM_DOODLE("pmd"), 
    GCM_DOODLE_PIC("pmdp"), 
    GCM_VIDEO("pmv"), 
    GCM_YOUTUBE("pmy"), 
    GCM_VOICE("pmvc"), 
    GCM_SONG("pms"), 
    GCM_LOCATION_SPECIFIC("pml"), 
    GCM_LOCATION_CURRENT("pmlc"), 
    GCM_CONTACT("pmc"), 
    // "pg" group
    GCM_NEW_GROUP("pga"), 
    GCM_ADDED_TO_GROUP_OTHER("pgao"),     
    GCM_GROUP_UPDATE_NAME("pgun"), 
    GCM_GROUP_UPDATE_COVER("pguc"), 
    GCM_GROUP_UPDATE_PROFILE("pgup"), 
    GCM_USER_GROUP_LEFT("pgl"),
    // "pu" user
    GCM_USER_JOIN("puj"),
    // "ab" address book
    GCM_AB_MATCH_SINGLE("pabmo"),
    GCM_AB_MATCH_MULTIPLE("pabm"),
    // unknown push notifications
    GCM_UNSUPPORTED("unsupported");

    private String value = "";

    private GCMMessageType(String value) {
        this.value = value;
    }

    public String toString() {
        return this.value;
    }

    public static GCMMessageType parseFrom(String type) {
        if (type.equals(GCM_TEXT.toString())) {
            return GCM_TEXT;
        } else if (type.equals(GCM_PHOTO.toString())) {
            return GCM_PHOTO;
        } else if (type.equals(GCM_DOODLE.toString())) {
            return GCM_DOODLE;
        } else if (type.equals(GCM_DOODLE_PIC.toString())) {
            return GCM_DOODLE_PIC;
        } else if (type.equals(GCM_VIDEO.toString())) {
            return GCM_VIDEO;
        } else if (type.equals(GCM_YOUTUBE.toString())) {
            return GCM_YOUTUBE;
        } else if (type.equals(GCM_VOICE.toString())) {
            return GCM_VOICE;
        } else if (type.equals(GCM_SONG.toString())) {
            return GCM_SONG;
        } else if (type.equals(GCM_LOCATION_SPECIFIC.toString())) {
            return GCM_LOCATION_SPECIFIC;
        } else if (type.equals(GCM_LOCATION_CURRENT.toString())) {
            return GCM_LOCATION_CURRENT;
        } else if (type.equals(GCM_CONTACT.toString())) {
            return GCM_CONTACT;
        } else if (type.equals(GCM_NEW_GROUP.toString())) {
            return GCM_NEW_GROUP;
        } else if (type.equals(GCM_ADDED_TO_GROUP_OTHER.toString())) {
            return GCM_ADDED_TO_GROUP_OTHER;
        } else if (type.equals(GCM_GROUP_UPDATE_NAME.toString())) {
            return GCM_GROUP_UPDATE_NAME;
        } else if (type.equals(GCM_GROUP_UPDATE_COVER.toString())) {
            return GCM_GROUP_UPDATE_COVER;
        } else if (type.equals(GCM_GROUP_UPDATE_PROFILE.toString())) {
            return GCM_GROUP_UPDATE_PROFILE;
        } else if (type.equals(GCM_USER_GROUP_LEFT.toString())) {
            return GCM_USER_GROUP_LEFT;
        } else if (type.equals(GCM_USER_JOIN.toString())) {
            return GCM_USER_JOIN;
        } else if (type.equals(GCM_AB_MATCH_SINGLE.toString())) {
            return GCM_AB_MATCH_SINGLE;
        } else if (type.equals(GCM_AB_MATCH_MULTIPLE.toString())) {
            return GCM_AB_MATCH_MULTIPLE;
        } else {
            return GCM_UNSUPPORTED;
        }
    }        
}