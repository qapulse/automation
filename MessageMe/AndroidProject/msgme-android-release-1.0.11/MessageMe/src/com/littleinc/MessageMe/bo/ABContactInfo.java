package com.littleinc.MessageMe.bo;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
import android.util.SparseArray;

import com.coredroid.util.LogIt;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.littleinc.MessageMe.data.DataBaseHelper;

/**
 * The android ConatctsProvider returns a record for each type of entry of a
 * contact information, in our case we need the phones, emails and names of each
 * contact. The purpose of this class is hold the data of each contact
 * information entry and provide some util methods to read the contacts
 * information
 */
@DatabaseTable(tableName = ABContactInfo.TABLE_NAME)
public class ABContactInfo implements Serializable {

    /**
     * Specify the "version" of this class.  If its member fields change
     * then this value should be updated to ensure that newer code 
     * cannot deserialize an old serialized object (as the 
     * deserialization code will not know how to read the old object any
     * more).
     */
    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME = "contact_info";

    public static final String ID_COLUMN = BaseColumns._ID;

    public static final String CONTACT_ID_COLUMN = "contact_id";

    public static final String DATA_VERSION_COLUMN = "data_version";

    @DatabaseField(columnName = ID_COLUMN, id = true, dataType = DataType.LONG)
    private long id;

    @DatabaseField(columnName = CONTACT_ID_COLUMN, dataType = DataType.LONG)
    private long contactId;

    @DatabaseField(columnName = DATA_VERSION_COLUMN, dataType = DataType.INTEGER)
    private int dataVersion;

    private String email;

    private String phone;

    /**
     * Record type e.g {@link Email}, {@link Phone}, {@link StructuredName}
     */
    private String mimeType;

    private String lastName;

    private String firstName;

    private String displayName;

    private int typeLabelResource;

    public ABContactInfo() {
    }

    /**
     * This constructor will map to the new instance with the given cursor data
     */
    public ABContactInfo(Cursor cursor) {

        if (cursor.getColumnIndex(Data._ID) != -1) {
            setId(cursor.getLong(cursor.getColumnIndex(Data._ID)));
        }

        if (cursor.getColumnIndex(Data.CONTACT_ID) != -1) {
            setContactId(cursor.getLong(cursor.getColumnIndex(Data.CONTACT_ID)));
        }

        if (cursor.getColumnIndex(Data.DATA_VERSION) != -1) {
            setDataVersion(cursor.getInt(cursor
                    .getColumnIndex(Data.DATA_VERSION)));
        }

        if (cursor.getColumnIndex(Data.DISPLAY_NAME) != -1) {
            setDisplayName(cursor.getString(cursor
                    .getColumnIndex(Data.DISPLAY_NAME)));
        }

        if (cursor.getColumnIndex(Email.DATA) != -1) {
            setEmail(cursor.getString(cursor.getColumnIndex(Email.DATA)));
        }

        if (cursor.getColumnIndex(Phone.DATA) != -1) {
            setPhone(cursor.getString(cursor.getColumnIndex(Phone.DATA)));
        }

        if (cursor.getColumnIndex(Data.MIMETYPE) != -1) {
            setMimeType(cursor.getString(cursor.getColumnIndex(Data.MIMETYPE)));
        }

        if (isEmail() && cursor.getColumnIndex(Email.TYPE) != -1) {
            setTypeLabelResource(Email.getTypeLabelResource(cursor
                    .getInt(cursor.getColumnIndex(Email.TYPE))));
        } else if (isPhone() && cursor.getColumnIndex(Phone.TYPE) != -1) {
            setTypeLabelResource(Phone.getTypeLabelResource(cursor
                    .getInt(cursor.getColumnIndex(Phone.TYPE))));
        } else if (isName()) {
            if (cursor.getColumnIndex(StructuredName.GIVEN_NAME) != -1) {
                setFirstName(cursor.getString(cursor
                        .getColumnIndex(StructuredName.GIVEN_NAME)));
            }

            if (cursor.getColumnIndex(StructuredName.FAMILY_NAME) != -1) {
                setLastName(cursor.getString(cursor
                        .getColumnIndex(StructuredName.FAMILY_NAME)));
            }
        }
    }

    public static Dao<ABContactInfo, Long> getDao() {
        try {
            return DataBaseHelper.getInstance().getDao(ABContactInfo.class);
        } catch (SQLException e) {
            LogIt.e(ABContactInfo.class, e, e.getMessage());
        }

        return null;
    }

    public boolean save() {
        Dao<ABContactInfo, Long> dao = getDao();

        try {
            dao.createOrUpdate(this);
            return true;
        } catch (SQLException e) {
            LogIt.e(ABContactInfo.class, e);
        }

        return false;
    }

    private static boolean contactHasChanged(long id, int dataVersion) {
        try {
            ABContactInfo entry = getDao().queryForId(id);

            if (entry != null) {
                return entry.dataVersion != dataVersion;
            } else {
                // Recently added entries will not exist in our database at
                // first check, so
                // return as changed to be added
                return true;
            }
        } catch (SQLException e) {
            LogIt.e(ABContactInfo.class, e);
        }

        return false;
    }

    /**
     * Finds the instance with the mimetype {@link StructuredName} in the given
     * list
     */
    public static ABContactInfo findContactNameInfo(
            List<ABContactInfo> listOfContactInfo) {
        for (ABContactInfo abContactInfo : listOfContactInfo) {
            if (abContactInfo.isName()) {
                return abContactInfo;
            }
        }

        return null;
    }

    public static List<ABContactInfo> parseFromToList(Cursor cursor) {

        List<ABContactInfo> abContactInfos = new ArrayList<ABContactInfo>();

        while (cursor != null && cursor.moveToNext()) {

            ABContactInfo newABContactInfo = new ABContactInfo(cursor);

            if (!abContactInfos.contains(newABContactInfo)) {
                abContactInfos.add(newABContactInfo);
            }
        }

        return abContactInfos;
    }

    /**
     * Using the unique contact id this method creates a map that contains a
     * list with each info entry (e.g mobile phone, home phone, work email, etc)
     * per contact index
     */
    public static SparseArray<List<ABContactInfo>> parseFromToMap(Cursor cursor) {
        List<ABContactInfo> contactEntries = null;
        SparseArray<List<ABContactInfo>> contactsMap = new SparseArray<List<ABContactInfo>>();

        while (cursor != null && cursor.moveToNext()) {
            ABContactInfo entry = null;
            int contactId = cursor.getInt(cursor
                    .getColumnIndex(Data.CONTACT_ID));

            if (contactsMap.get(contactId) != null) {
                // Adds a new entry into the existing contact
                entry = new ABContactInfo(cursor);

                if (ABContactInfo.contactHasChanged(entry.getId(),
                        entry.getDataVersion())) {
                    contactEntries.add(entry);
                }
            } else {
                entry = new ABContactInfo(cursor);

                if (ABContactInfo.contactHasChanged(entry.getId(),
                        entry.getDataVersion())) {
                    // Creates a entry info list instance and adds the first
                    // entry
                    contactEntries = new LinkedList<ABContactInfo>();

                    contactEntries.add(entry);

                    // Adds the new contact into the map with the corresponding
                    // entry list
                    contactsMap.append(contactId, contactEntries);
                }
            }
        }

        return contactsMap;
    }

    public String getNameInitial() {

        if (!TextUtils.isEmpty(getDisplayName())) {

            Character initial = getDisplayName().substring(0).charAt(0);

            if (Character.isLetter(initial)) {

                return initial.toString();
            } else {

                return "#";
            }
        } else {

            return "#";
        }
    }

    public boolean isEmail() {
        if (Email.CONTENT_ITEM_TYPE.equals(getMimeType())) {
            return true;
        }

        return false;
    }

    public boolean isPhone() {
        if (Phone.CONTENT_ITEM_TYPE.equals(getMimeType())) {
            return true;
        }

        return false;
    }

    public boolean isName() {
        if (StructuredName.CONTENT_ITEM_TYPE.equals(getMimeType())) {
            return true;
        }

        return false;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getTypeLabelResource() {
        return typeLabelResource;
    }

    public void setTypeLabelResource(int typeLabelResource) {
        this.typeLabelResource = typeLabelResource;
    }

    public String getData() {
        return TextUtils.isEmpty(getEmail()) ? getPhone() : getEmail();
    }

    public long getContactId() {
        return contactId;
    }

    public void setContactId(long contactId) {
        this.contactId = contactId;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public int getDataVersion() {
        return dataVersion;
    }

    public void setDataVersion(int dataVersion) {
        this.dataVersion = dataVersion;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }

        // Return true if the objects are identical.
        // (This is just an optimization, not required for correctness.)
        if (this == obj) {
            return true;
        }

        // Return false if the other object has the wrong type.
        // This type may be an interface depending on the interface's specification.
        if (!(obj instanceof ABContactInfo)) {
            return false;
        }

        // Cast to the appropriate type.
        // This will succeed because of the instanceof, and lets us access private fields.
        ABContactInfo lhs = (ABContactInfo) obj;

        // Check each field. Primitive fields, reference fields, and nullable reference
        // fields are all treated differently.
        return id == lhs.id
                && contactId == lhs.contactId
                && dataVersion == lhs.dataVersion
                && (email == null ? lhs.email == null : email
                        .equalsIgnoreCase(lhs.email))
                && (phone == null ? lhs.phone == null : phone
                        .equalsIgnoreCase(lhs.phone))
                && (mimeType == null ? lhs.mimeType == null : mimeType
                        .equalsIgnoreCase(lhs.mimeType))
                && (lastName == null ? lhs.lastName == null : lastName
                        .equalsIgnoreCase(lhs.lastName))
                && (firstName == null ? lhs.firstName == null : firstName
                        .equalsIgnoreCase(lhs.firstName))
                && (displayName == null ? lhs.displayName == null : displayName
                        .equalsIgnoreCase(lhs.displayName))
                && typeLabelResource == lhs.typeLabelResource;
    }

    @Override
    public int hashCode() {

        if (this == null) {
            return 0;
        } else {

            int result = 17;

            result += (int) id;
            result += (int) contactId;
            result += (int) typeLabelResource;
            result += (email == null ? 0 : email.hashCode());
            result += (phone == null ? 0 : phone.hashCode());
            result += (mimeType == null ? 0 : mimeType.hashCode());
            result += (lastName == null ? 0 : lastName.hashCode());
            result += (firstName == null ? 0 : firstName.hashCode());
            result += (displayName == null ? 0 : displayName.hashCode());

            return result;
        }
    }
}