Feature: Contacts

Scenario: Invite an user
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" text
	Then I press new contact button
	Then I enter text "NM 647 QPN" into field with id "friends_invite_input"
	Then I press the enter button
	Then I wait to see "Send Message"
	
Scenario: Invite an user and add him as a friend
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" text
	Then I press new contact button
	Then I enter text "NM 647 QPN" into field with id "friends_invite_input"
	Then I press the enter button
	Then I wait to see "Send Message"
	Then I press the "Add Contact" button
	Then I wait to see "OK"
	Then I tab on the "OK" text	

Scenario: Block an user
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" text
	Then I touch the "Bri Ca" text
	Then I press the "Block" button
	Then I wait to see "Blocked"

Scenario: Unblock an user
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" text
	#Then find a contact
	Then I wait for 3 seconds
	Then I tab on the "Bri Ca" text
	Then I press the "Unblock" button
	Then I wait to see "Send Message"
	
Scenario: Create Group
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" text
	Then I wait to see "New Group Chat"
	Then I tab on the "New Group Chat" text
	Then I wait to see "Enter group name"
	Then I enter text "Automation Group" into field with id "group_name_input"
	Then I press the enter button
	Then I wait to see "Automation Group"
	Then I tab on the image with id "add_contact_button"
	Then I wait to see "Select members"
	Then I tab on the "Bri Ca" text
	Then I tab on the "David Logn" text
	Then I tab on the "Next" text
	Then I wait to see "Automation Group"
	
Scenario: Leave a Group
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" text
	# Wait until the group the user want to leave appear
	Then I wait to see "Automation Group"
	Then I touch the "Automation Group" text
	Then I wait to see "Settings"
	Then I scroll down 
	Then I wait to see "Leave Group"
	Then I press the "Leave Group" button
	Then I wait to see "Are you sure?"
	Then I tab on the "Leave Group" text
	Then I wait to see "CONTACTS"
	
Scenario: Delete a conversation
	Given I wait to see "MESSAGES"
	Then I long press "Automation Group"
	Then I wait to see "Conversation"
	Then I tab on the "Delete" text
	Then I wait to see "Confirm Deletion"
	Then I tab on the "Delete Conversation" text

Scenario: Rate Later MessageMe
	Given I wait to see "Rate MessageMe"
	Then I tab on the "Maybe Later" text
	Then I wait to see "MESSAGES"
	