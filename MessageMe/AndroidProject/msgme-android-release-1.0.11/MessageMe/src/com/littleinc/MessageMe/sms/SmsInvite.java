package com.littleinc.MessageMe.sms;

import java.io.Serializable;
import java.util.Date;

public class SmsInvite implements Serializable {

    /**
     * Specify the "version" of this class.  If its member fields change
     * then this value should be updated to ensure that newer code 
     * cannot deserialize an old serialized object (as the 
     * deserialization code will not know how to read the old object any
     * more).
     */
    private static final long serialVersionUID = 1L;

    private Date mDateCreated;

    private String mMessageBody;

    private String mPhoneNumber;

    private String mFirstName;

    private String mLastName;

    private String mTempFileName;

    private Date mDateLastAttempt;

    public SmsInvite() {
    }

    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.mPhoneNumber = phoneNumber;
    }

    public String getMessageBody() {
        return mMessageBody;
    }

    public void setMessageBody(String messageBody) {
        this.mMessageBody = messageBody;
    }

    public Date getDateCreated() {
        return mDateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.mDateCreated = dateCreated;
    }

    public Date getDateLastAttempt() {
        return mDateLastAttempt;
    }

    public void setDateLastAttempt(Date dateLastAttempt) {
        this.mDateLastAttempt = dateLastAttempt;
    }

    public String getFirstName() {
        return mFirstName;
    }

    public void setFirstName(String firstName) {
        mFirstName = firstName;
    }

    public String getLastName() {
        return mLastName;
    }

    public void setLastName(String lastName) {
        mLastName = lastName;
    }

    @Override
    public int hashCode() {

        if (this == null) {

            return 0;
        } else {

            return (mPhoneNumber == null ? 0 : mPhoneNumber.hashCode());
        }
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
        if (!(obj instanceof SmsInvite)) {
            return false;
        }

        // Cast to the appropriate type.
        // This will succeed because of the instanceof, and lets us access private fields.
        SmsInvite lhs = (SmsInvite) obj;

        // Check each field. Primitive fields, reference fields, and nullable reference
        // fields are all treated differently.
        return (mPhoneNumber == null ? lhs.mPhoneNumber == null : mPhoneNumber
                .equals(lhs.mPhoneNumber));
    }

    @Override
    public String toString() {

        return new StringBuilder().append(getClass().getName())
                .append("[phoneNumber=").append(getPhoneNumber()).append(", ")
                .append("tempFileName=").append(getTempFileName()).append(", ")
                .append("dateLastAttempt=").append(getDateLastAttempt())
                .append(", ").append("dateCreated=").append(getDateCreated())
                .append(", ").append("firstName=").append(getFirstName())
                .append(", ").append("lastName=").append(getLastName())
                .append("]").toString();
    }

    public String getTempFileName() {
        return mTempFileName;
    }

    public void setTempFileName(String tempFileName) {
        this.mTempFileName = tempFileName;
    }
}
