package com.littleinc.MessageMe.bo;

import java.sql.SQLException;
import java.util.Comparator;

import org.restlet.data.Form;

import android.text.Html;
import android.text.TextUtils;

import com.coredroid.core.DirtyableCoreObject;
import com.coredroid.util.LogIt;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope.CommandType;
import com.littleinc.MessageMe.protocol.Commands.PBCommandRoomClear;
import com.littleinc.MessageMe.ui.EmojiUtils;

public abstract class Contact extends DirtyableCoreObject {

    public static final String ID_COLUMN = "id";

    public static final String IS_SHOWN_COLUMN = "is_shown";

    public static final String LAST_NAME_COLUMN = "last_name";

    public static final String FIRST_NAME_COLUMN = "first_name";

    public static final String NAME_INITIAL_COLUMN = "name_initial";

    public static final String COVER_IMAGE_KEY_COLUMN = "cover_image_key";

    public static final String PROFILE_IMAGE_KEY_COLUMN = "profile_image_key";

    @DatabaseField(columnName = Contact.ID_COLUMN, id = true, dataType = DataType.LONG)
    protected long contactId;

    protected int contactType;

    @DatabaseField(columnName = Contact.LAST_NAME_COLUMN, dataType = DataType.STRING)
    protected String lastName;

    @DatabaseField(columnName = Contact.FIRST_NAME_COLUMN, dataType = DataType.STRING)
    protected String firstName;

    @DatabaseField(columnName = Contact.NAME_INITIAL_COLUMN, dataType = DataType.STRING)
    protected String nameInitial;

    @DatabaseField(columnName = Contact.COVER_IMAGE_KEY_COLUMN, dataType = DataType.STRING)
    protected String coverImageKey = "";

    @DatabaseField(columnName = Contact.PROFILE_IMAGE_KEY_COLUMN, dataType = DataType.STRING)
    protected String profileImageKey = "";

    @DatabaseField(columnName = Contact.IS_SHOWN_COLUMN, dataType = DataType.BOOLEAN)
    private boolean isShown;

    /**
     * Locally cached display name.  
     */
    private transient String mDisplayName = null;
    
    /**
     * Locally cached styled name with emojis, for displaying in Contacts tab.
     * 
     * This needs to be marked as transient to stop the MessageMeAppPreferences
     * from trying to serialize and deserialize it, as CharSequence is not
     * serializable.
     */
    private transient CharSequence mStyledNameWithEmojis = null;
    
    /**
     * Locally cached styled name with small emojis, for displaying elsewhere
     * in the app.
     * 
     * This needs to be marked as transient to stop the MessageMeAppPreferences
     * from trying to serialize and deserialize it, as CharSequence is not
     * serializable.
     */
    private transient CharSequence mStyledNameWithSmallEmojis = null;
    
    public abstract int clear();

    public abstract void load();

    public abstract int delete();

    public abstract int delete(boolean isClear);        

    private void updateNameInitial() {
        String base = !TextUtils.isEmpty(firstName) ? firstName : lastName;

        if (!TextUtils.isEmpty(base)) {
            Character initial = base.substring(0).charAt(0);

            if (Character.isLetter(initial)) {
                nameInitial = initial.toString();
            } else {
                nameInitial = "#";
            }
        } else {
            nameInitial = "#";
        }
    }

    /**
     * Build a Form for updating this Contact.  The ID must be provided, 
     * along with any of the fields which have changed.  
     */
    public Form toFormForUpdate(String updateAction) {
        Form form = new Form();

        form.add("room_id", Long.toString(contactId));

        // The only fields that can be updated are the profile and
        // cover photos        
        if (updateAction.equals(MessageMeConstants.PROFILE_PIC)) {
            LogIt.d(this, "Profile photo update");
            form.add("profile_image_key", getProfileImageKey());
        }

        if (updateAction.equals(MessageMeConstants.COVER_PIC)) {
            LogIt.d(this, "Cover photo update");
            form.add("cover_image_key", getCoverImageKey());
        }

        if (updateAction.equals(MessageMeConstants.USER_NAME)) {
            LogIt.d(this, "User name updated ");
            form.add("first_name", getFirstName());
            form.add("last_name", getLastName());
        }

        return form;
    }

    /**
     * @return the display name as a String, with no formatting applied.
     */
    public String getDisplayName() {
        if (mDisplayName == null) {            
            StringBuilder displayName = new StringBuilder("");
            
            if (!TextUtils.isEmpty(firstName)) {
                displayName.append(firstName.trim());
            }
            
            if (!TextUtils.isEmpty(lastName)) {
                displayName.append(" ");
                displayName.append(lastName.trim());
            }

            mDisplayName = displayName.toString();
        }
        
        return mDisplayName;
    }

    /**
     * Return the display name with the first name in bold, and any emojis
     * converted into their images.
     */
    public CharSequence getStyledNameWithEmojis() {
        
        if (mStyledNameWithEmojis == null) {            
            StringBuilder displayName = new StringBuilder();
            
            if (!TextUtils.isEmpty(firstName)) {
                displayName
                        .append("<b>")
                        .append(EmojiUtils.convertEmojisRaw(firstName))
                        .append("</b>").append(" ");
            }
            
            if (!TextUtils.isEmpty(lastName)) {
                displayName.append(EmojiUtils
                        .convertEmojisRaw(lastName));
            }
            
            mStyledNameWithEmojis = Html.fromHtml(displayName.toString(),
                    EmojiUtils.getEmojiImageGetterNormal(), null);
        }
    
        return mStyledNameWithEmojis;
    }
    
    /**
     * Same as {@link #getStyledNameWithEmojis()} but with small emojis.
     */
    public CharSequence getStyledNameWithSmallEmojis() {
        
        if (mStyledNameWithSmallEmojis == null) {            
            StringBuilder displayName = new StringBuilder();
            
            if (!TextUtils.isEmpty(firstName)) {
                displayName
                        .append("<b>")
                        .append(EmojiUtils.convertEmojisRaw(firstName))
                        .append("</b>").append(" ");
            }
            
            if (!TextUtils.isEmpty(lastName)) {
                displayName.append(EmojiUtils
                        .convertEmojisRaw(lastName));
            }
            
            mStyledNameWithSmallEmojis = Html.fromHtml(displayName.toString(),
                    EmojiUtils.getEmojiImageGetterSmall(), null);
        }
    
        return mStyledNameWithSmallEmojis;
    }

    public boolean isUser() {
        return contactType == ContactType.USER.getValue();
    }

    public boolean isGroup() {
        return contactType == ContactType.GROUP.getValue();
    }

    public long getContactId() {
        return contactId;
    }

    public void setContactId(long contactId) {
        dirty();
        this.contactId = contactId;
    }

    public int getContactType() {
        return contactType;
    }

    public void setContactType(int contactType) {
        dirty();
        this.contactType = contactType;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        dirty();
        this.lastName = lastName;
        
        // Reset cached display names
        mDisplayName = null;
        mStyledNameWithEmojis = null;

        updateNameInitial();
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        dirty();
        this.firstName = firstName;
        
        // Reset cached display names
        mDisplayName = null;
        mStyledNameWithEmojis = null;

        updateNameInitial();
    }

    public String getNameInitial() {
        return nameInitial;
    }

    public void setNameInitial(String nameInitial) {
        dirty();
        this.nameInitial = nameInitial;
    }

    public String getCoverImageKey() {
        return coverImageKey;
    }

    public void setCoverImageKey(String coverImageKey) {
        dirty();
        this.coverImageKey = coverImageKey;
    }

    public String getProfileImageKey() {
        return profileImageKey;
    }

    public void setProfileImageKey(String profileImageKey) {
        dirty();
        this.profileImageKey = profileImageKey;
    }

    /**
     * Creates a new record into the db for this contact instance
     */
    public long createCursor(boolean isInBatch) {
        if (!isInBatch) {
            // For performance only log these outside of BATCH commands
            LogIt.d(Contact.class, "Creating cursor for ", getContactId());
        }

        MessageMeCursor cursor = new MessageMeCursor(getContactId());

        return cursor.create();
    }

    /**
     * Creates a new record into the db for this contact instance and set the lastCommandId
     */
    public long createCursor(long lastCommandId, boolean isInBatch) {
        if (!isInBatch) {
            // For performance only log these outside of BATCH commands
            LogIt.d(Contact.class, "Creating cursor for", getContactId(),
                    "with lastCommandId", lastCommandId);
        }

        MessageMeCursor cursor = new MessageMeCursor(getContactId(),
                lastCommandId);

        return cursor.create();
    }

    /**
     * Updates an existing cursor
     */
    public long updateCursor(long lastCommandId, boolean isInBatch) {
        if (!isInBatch) {
            // For performance only log these outside of BATCH commands
            LogIt.d(Contact.class, "Updating cursor for ", getContactId(),
                    "with lastCommandId", lastCommandId);
        }

        if (lastCommandId == 0) {
            LogIt.w(this, "Ignore cursor update with commandId zero");
            return 0;
        }

        MessageMeCursor cursor = new MessageMeCursor(getContactId(),
                lastCommandId);

        return cursor.update();
    }

    /**
     * Disables an existing cursor, this method will update the cursor state and the lastCommandId
     */
    public long disableCursor(long lastCommandId, boolean isInBatch) {
        if (!isInBatch) {
            // For performance only log these outside of BATCH commands
            LogIt.d(Contact.class, "Disabling cursor for ", getContactId(),
                    "with lastCommandId", lastCommandId);
        }

        MessageMeCursor cursor = new MessageMeCursor(getContactId(),
                lastCommandId);

        return cursor.disable();
    }

    /**
     * Disables an existing cursor, this method will update the cursor state only without
     * change the lastCommandId. This because in some places the lastCommandId is not
     * available when we need to disable a cursor
     */
    public long disableCursor(boolean isInBatch) {
        if (!isInBatch) {
            // For performance only log these outside of BATCH commands
            LogIt.d(Contact.class, "Disabling cursor for ", getContactId());
        }

        MessageMeCursor cursor = new MessageMeCursor(getContactId());
        return cursor.disable();
    }

    public boolean isShown() {
        return isShown;
    }

    public void setShown(boolean isShown) {
        this.isShown = isShown;
    }

    /**
     * Factory method to create a new contact instance
     * This method looks for the given contactId into the db to determine if is a {@link Room}
     * or a {@link User}
     */
    public static Contact newInstance(long contactId) {
        Contact contact = null;

        try {
            if (Room.getDao().idExists(contactId)) {
                contact = new Room(contactId);
            } else {
                contact = new User(contactId);
            }
        } catch (SQLException e) {
            LogIt.e(Contact.class, e);
        }

        return contact;
    }

    /**
     * Sorts the contacts using the whole display name
     *
     */
    public static class SortContacts implements Comparator<Contact> {

        private static SortContacts instance;

        public static SortContacts getInstance() {
            if (instance == null) {
                instance = new SortContacts();
            }
            return instance;
        }

        @Override
        public int compare(Contact lhs, Contact rhs) {
            Contact contactOne = (Contact) lhs;
            Contact contactTwo = (Contact) rhs;

            return contactOne.getDisplayName().compareToIgnoreCase(
                    contactTwo.getDisplayName());

        }
    }

    public PBCommandEnvelope serializeRoomClear(long commandId) {
        PBCommandEnvelope.Builder commandEnvelopeBuilder = PBCommandEnvelope
                .newBuilder();
        PBCommandRoomClear.Builder roomClearBuilder = PBCommandRoomClear
                .newBuilder();

        if (commandId == -1) {
            // We should never send a command ID of -1.  A value of zero would
            // be valid if we are clearing a room that never had any messages
            // in it.
            LogIt.w(this, "Don't put a command ID of -1 in a ROOM_CLEAR");
        } else {
            roomClearBuilder.setCommandID(commandId);
        }
        roomClearBuilder.setRecipientID(getContactId());

        commandEnvelopeBuilder.setType(CommandType.ROOM_CLEAR);
        commandEnvelopeBuilder.setRoomClear(roomClearBuilder);

        return commandEnvelopeBuilder.build();
    }
    
    @Override
    public boolean equals(Object contact) {
        if(contact == null){
            return false;
        }
        if(!(contact instanceof Contact)){
            return false;
        }        
        return this.contactId == ((Contact) contact).getContactId();
    }
    
    @Override
    public int hashCode() {
        return Long.valueOf(getContactId()).hashCode();
    }
}