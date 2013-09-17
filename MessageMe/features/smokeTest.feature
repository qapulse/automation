Feature: Create an new account using the email invite an user and and send messages

Scenario: Create a valid account
	Given I wait to see "Log In"
	Then I press the "Register" button
	#Then I wait to see "Connect with Facebook"
	#Then I tab on the "No" text
	Then I enter valid user information new user
	Then I scroll down
	Then I wait to see "Start Messaging"
	Then I press the "Start Messaging" button
	Then I wait for 3 seconds
	Then I touch the continue button
	Given I skip adding friends while creating account
	Then I wait for 3 seconds
	Then I wait to see "Would you like"
	Then I tab on the "Cancel" text
	Then I wait to see "Messages"
	
Scenario: Invite an user and send many messages
#Given I wait to see "Notifications are Off" or "MESSAGES" if "Notifications are Off" is visible tab on "OK"/
	Given I wait to see "Messages"
	Then I touch the new Contacts button
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
	Then I wait to see "Messages"
	Then I touch the new Contacts button
	Then I press new contact button
	Then I enter text "NM 647 QPN" into field with id "friends_invite_input"
	Then I press the enter button
	Then I wait to see "Send Message"
	Then I press the "Add Contact" button
	Then I wait to see "Okay"
	Then I tab on the "Okay" text	
	
Scenario: Update the Profile Name
#Given I wait to see "Notifications are Off" or "MESSAGES" if "Notifications are Off" is visible tab on "OK"/
	Given I wait to see "Messages"
	Then I touch the new MyProfile button
	Then I press change profile name button
	Then I clear input field number 1
	Then I clear input field number 2
	Then I enter "Didier A" into input field number 1
	Then I enter "Corrales " into input field number 2
	Then I tab on the "Save" text

Scenario: Log out
	Given I wait to see "Messages"
	Then I touch the new MyProfile button
	Then I touch the "Settings" text
	Then I scroll down
	Then I press the "Log Out" button
	Then I wait to see "Log Out of MessageMe"
	Then I press the "Yes" button
	Then I wait to see "Log In"