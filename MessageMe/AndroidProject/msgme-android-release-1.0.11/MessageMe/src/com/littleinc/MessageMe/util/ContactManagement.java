package com.littleinc.MessageMe.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.bo.Contact;
import com.littleinc.MessageMe.bo.Contact.SortContacts;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.User;

public class ContactManagement {

    private ContactManagement() {
        LogIt.d(this, "Create new ContactManagement");
    }

    public static void setAlphabeticallySortedAdapter(List<Contact> contacts,
            ContactAdapter adapter) {
        User currentUser = MessageMeApplication.getCurrentUser();

        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).getContactId() == currentUser.getUserId()) {
                LogIt.d(ContactManagement.class,
                        "Omit current user from the contact list");
                continue;
            }

            if (!StringUtil.isEmpty(contacts.get(i).getFirstName())) {
                if (i == 0) {
                    adapter.addContactSeparator(contacts.get(i));
                }

                adapter.addContact(contacts.get(i));

                if ((i < contacts.size() - 1)
                        && (!contacts
                                .get(i + 1)
                                .getNameInitial()
                                .toUpperCase()
                                .equals(contacts.get(i).getNameInitial()
                                        .toUpperCase()))) {
                    adapter.addContactSeparator(contacts.get(i + 1));
                }
            }
        }
    }

    /**
     * Loads all the rooms and the contact list
     */
    public static List<Contact> loadContactListAndRooms(List<Contact> contacts) {
        contacts = new ArrayList<Contact>();

        contacts.addAll(Room.getRoomList());
        contacts.addAll(User.getContactList());
        Collections.sort(contacts, new SortContacts());

        return contacts;
    }

    public static List<Contact> searchContactListAndRooms(String name) {
        List<Contact> contacts = new ArrayList<Contact>();

        contacts.addAll(Room.search(name));
        contacts.addAll(User.search(name));
        Collections.sort(contacts, SortContacts.getInstance());

        return removeListDuplicates(contacts);
    }

    public static List<Contact> searchUsers(String name) {
        List<Contact> contacts = new ArrayList<Contact>();

        contacts.addAll(User.search(name));
        Collections.sort(contacts, SortContacts.getInstance());

        return contacts;
    }

    public static List<Contact> removeListDuplicates(List<Contact> contacts) {
        // Using a Set, as it doesn't allow duplicates
        Set<Contact> setItems = new LinkedHashSet<Contact>(contacts);
        contacts.clear();
        contacts.addAll(setItems);

        return contacts;
    }

    /**
     * Only loads the contact list
     */
    public static List<Contact> loadContactList(List<Contact> contacts) {
        contacts = new ArrayList<Contact>();

        contacts.addAll(User.getContactList());
        Collections.sort(contacts, new SortContacts());

        return contacts;
    }

    public static List<Contact> doSearch(String terms, List<Contact> contacts) {

        List<Contact> sortedContacts = new ArrayList<Contact>();
        int textLength = 0;

        textLength = terms.length();

        for (int i = 0; i < contacts.size(); i++) {
            if (textLength <= contacts.get(i).getDisplayName().length()) {
                if (contacts.get(i).getDisplayName().toLowerCase()
                        .contains(terms.toLowerCase())) {
                    sortedContacts.add(contacts.get(i));
                }
            }
        }
        return sortedContacts;
    }
}