Feature: Create an new account using the email invite an user and and send messages

Scenario: As a User I want to try to log in  using invalid credentials
	Given I wait to see "Log In"
	Then I wait for 2 seconds
	Then I touch the screeen to activate it
	Then I press the "Log In" button
	Then I wait to see "Email"
	Then I enter invalid credentials
	Then I press the "Log In" button
	Then I wait until I see the login invalid credentials error message

Scenario: As a User I want to log in
	Given I wait to see "Log In"
	Then I wait for 2 seconds
	Then I touch the screeen to activate it
	Then I press the "Log In" button
	Then I wait to see "Email"
	Then I enter valid credentials
	Then I press the "Log In" button
	Then I wait to see "Would you like"
	Then I tab on the "Cancel" text
	Then I wait to see "MESSAGES"
	
Scenario: Block an user
	#Given I wait to see "Notifications are Off" or "MESSAGES" if "Notifications are Off" is visible tab on "OK"/
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" text
	Then I touch the "Bri Ca" text
	Then I scroll down 
	Then I wait to see "Block"
	Then I wait for 3 seconds
	Then I press the "Block" button
	Then I wait to see "Blocked"

Scenario: Unblock an user
#Given I wait to see "Notifications are Off" or "MESSAGES" if "Notifications are Off" is visible tab on "OK"/
	Given I continue if rate message me is not visible
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" text
	#Then find a contact
	Then I wait for 3 seconds
	Then I tab on the "Bri Ca" text
	Then I wait to see "Unblock"
	Then I press the "Unblock" button
	Then I wait to see "Send Message"
	
Scenario: Create Group
#Given I wait to see "Notifications are Off" or "MESSAGES" if "Notifications are Off" is visible tab on "OK"/
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
	Then I wait for 3 seconds
	Then I tab on the "Next" text
	Then I wait to see "Automation Group"
	
Scenario: Delete a conversation
#Given I wait to see "Notifications are Off" or "MESSAGES" if "Notifications are Off" is visible tab on "OK"/
	Given I wait to see "MESSAGES"
	Then I long press "Automation Group"
	Then I wait to see "Conversation"
	Then I tab on the "Delete" text
	Then I wait to see "Confirm Deletion"
	Then I tab on the "Delete Conversation" text
	
Scenario: Send message to the group
#Given I wait to see "Notifications are Off" or "MESSAGES" if "Notifications are Off" is visible tab on "OK"/
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" text
	Then I wait to see "Automation Group"
	Then I touch the "Automation Group" text
	Then I wait to see "Send Message"
	Then I press the "Send Message" button
	Then I send a new text message
	Then I wait

Scenario: Delete a conversation
#Given I wait to see "Notifications are Off" or "MESSAGES" if "Notifications are Off" is visible tab on "OK"/
	Given I wait to see "MESSAGES"
	Then I long press "Automation Group"
	Then I wait to see "Conversation"
	Then I tab on the "Delete" text
	Then I wait to see "Confirm Deletion"
	Then I tab on the "Delete Conversation" text
	
Scenario: Leave a Group
#Given I wait to see "Notifications are Off" or "MESSAGES" if "Notifications are Off" is visible tab on "OK"/
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" text
	# Wait until the group the user want to leave appear
	Then I wait to see "Automation Group"
	Then I touch the "Automation Group" text
	Then I wait to see "Settings"
	Then I scroll down 
	Then I wait to see "Leave Group"
	Then I wait for 3 seconds
	Then I press the "Leave Group" button
	Then I wait to see "Are you sure?"
	Then I tab on the "Leave Group" text
	Then I wait to see "CONTACTS"
	
Scenario: Log out
#Given I wait to see "Notifications are Off" or "MESSAGES" if "Notifications are Off" is visible tab on "OK"/
	Given I wait to see "MESSAGES"
	Then I touch the "MY PROFILE" text
	Then I touch the "Settings" text
	Then I scroll down
	Then I press the "Log Out" button
	Then I wait to see "Log Out of MessageMe"
	Then I press the "Yes" button
	Then I wait to see "Log In"
	
Scenario: Create a valid account
	Given I wait to see "Log In"
	Then I press the "Create New Account" button
	Then I wait to see "Connect with Facebook"
	Then I tab on the "No" text
	Then I enter valid user information new user
	Then I scroll down
	Then I wait to see "Start Messaging"
	Then I press the "Start Messaging" button
	Then I wait for 3 seconds
	Then I wait to see "Would you like"
	Then I tab on the "Cancel" text
	Then I wait to see "MESSAGES"
	
Scenario: Invite an user and send many messages
#Given I wait to see "Notifications are Off" or "MESSAGES" if "Notifications are Off" is visible tab on "OK"/
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" text
	Then I press new contact button
	Then I enter text "NM 647 QPN" into field with id "friends_invite_input"
	Then I press the enter button
	Then I wait to see "Send Message"
	Then I wait for 3 seconds
	Then I press the "Send Message" button
	Then I wait for 3 seconds
	Then I send a new text message
	Then I wait for 3 seconds

Scenario: Invite an user and add him as a friend
#Given I wait to see "Notifications are Off" or "MESSAGES" if "Notifications are Off" is visible tab on "OK"/
	Given I continue if rate message me is not visible
	Then I wait to see "MESSAGES"
	Then I touch the "CONTACTS" text
	Then I press new contact button
	Then I enter text "NM 647 QPN" into field with id "friends_invite_input"
	Then I press the enter button
	Then I wait to see "Send Message"
	Then I press the "Add Contact" button
	Then I wait to see "OK"
	Then I tab on the "OK" text	
	
Scenario: Update the Profile Name
#Given I wait to see "Notifications are Off" or "MESSAGES" if "Notifications are Off" is visible tab on "OK"/
	Given I wait to see "MESSAGES"
	Then I touch the "MY PROFILE" text
	Then I press change profile name button
	Then I clear input field number 1
	Then I clear input field number 2
	Then I enter "Didier A" into input field number 1
	Then I enter "Corrales " into input field number 2
	Then I tab on the "Save" text

Scenario: Log out
	Given I wait to see "MESSAGES"
	Then I touch the "MY PROFILE" text
	Then I touch the "Settings" text
	Then I scroll down
	Then I press the "Log Out" button
	Then I wait to see "Log Out of MessageMe"
	Then I press the "Yes" button
	Then I wait to see "Log In"