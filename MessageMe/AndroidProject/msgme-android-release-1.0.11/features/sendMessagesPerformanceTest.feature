Feature: Send test Messages 

Scenario: As a User I want to log in and send many messages using the special funtion to test the performance
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
	Then I touch the "CONTACTS" text
	Then I wait to see "Bri Ca"
	Then I touch the "Bri Ca" text
	Then I wait to see "Send Message"
	Then I press the "Send Message" button
	Then I send "5" test messages
	Then I wait for 30 seconds
	
Scenario: Delete a conversation
	Given I wait to see "MESSAGES"
	Then I long press "Bri Ca"
	Then I wait to see "Conversation"
	Then I tab on the "Delete" text
	Then I wait to see "Confirm Deletion"
	Then I tab on the "Delete Conversation" text
