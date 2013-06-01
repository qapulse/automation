Feature: Send test Messages 

Scenario: Send a specific number of test messages
	Given I wait to see "Log In"
	Then I press the "Create New Account" button
	Then I wait to see "Connect with Facebook"
	Then I tab on the "No" text
	Then I enter valid user information 
	Then I press the "Start Messaging" button
	Then I wait to see "Would you like"
	Then I tab on the "Cancel" text
	Then I wait to see "MESSAGES"
	Then I touch the "CONTACTS" text
	Then I press new contact button
	Then I enter text "NM 647 QPN" into field with id "friends_invite_input"
	Then I press the enter button
	Then I wait to see "Send Message"
	Then I wait for 5 seconds
	Then I wait for 5 seconds
	Then I press the "Send Message" button
	Then I send a new text message
	Then I wait for 15 seconds
	