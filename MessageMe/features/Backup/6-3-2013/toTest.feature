Feature: Create an new account using the email invite an user and and send messages

Scenario: As a User I want to log in
	Given I wait to see "Log In"
	Then I wait for 3 seconds
	Then I press the "Log In" button
	Then I wait to see "Email"
	Then I enter valid credentials
	Then I press the "Log In" button
	Then I wait to see "Would you like"
	Then I tab on the "Cancel" text
	Then I wait to see "MESSAGES"
	
Scenario: Log out
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
	Then I press the "Start Messaging" button
	Then I wait to see "Would you like"
	Then I tab on the "Cancel" text
	Then I wait to see "MESSAGES"
	
Scenario: Invite an user and send many messages
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" text
	Then I press new contact button
	Then I enter text "NM 647 QPN" into field with id "friends_invite_input"
	Then I press the enter button
	Then I wait to see "Send Message"
	Then I wait for 3 seconds
	Then I press the "Send Message" button
	Then I wait for 3 seconds
	Then I send a doddle by message with a google image as background
	Then I wait for 3 seconds
	