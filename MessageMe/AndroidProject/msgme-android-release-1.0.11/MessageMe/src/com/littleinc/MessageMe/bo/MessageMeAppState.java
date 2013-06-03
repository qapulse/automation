package com.littleinc.MessageMe.bo;

import java.util.List;

import com.coredroid.core.DirtyableCoreObject;
import com.littleinc.MessageMe.ui.MultiInviteActivity;
import com.littleinc.MessageMe.ui.SearchABContactsActivity;

public class MessageMeAppState extends DirtyableCoreObject {

    /**
     * List to maintain the latest loaded AB information while the
     * {@link MultiInviteActivity} is running, this will improve performance
     * searching AB contacts {@link SearchABContactsActivity}
     */
    private List<ABContactInfo> mABContacts;

    /**
     * List to maintain the latest check AB information while the
     * {@link MultiInviteActivity} is running, this will improve performance
     * searching AB contacts {@link SearchABContactsActivity}
     */
    private List<ABContactInfo> mABContactsChecked;

    public List<ABContactInfo> getAbContacts() {
        return mABContacts;
    }

    public void setAbContacts(List<ABContactInfo> abContacts) {
        dirty();
        this.mABContacts = abContacts;
    }

    public List<ABContactInfo> getABContactsChecked() {
        return mABContactsChecked;
    }

    public void setABContactsChecked(List<ABContactInfo> abContactsChecked) {
        dirty();
        this.mABContactsChecked = abContactsChecked;
    }
}