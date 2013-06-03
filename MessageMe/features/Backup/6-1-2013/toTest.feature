Feature: Create an new account using the email invite an user and and send messages

Scenario: Send a specific number of test messages
	Given I wait to see "Log In"
	Then I press the "Log In" button
	Then I should see "Email"
	Then I enter valid credentials
	Then I press the "Log In" button
	Then I wait to see "Would you like"
	Then I tab on the "Cancel" text
	Then I wait to see "MESSAGES"
	
Scenario: Invite an user
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" text
	Then I tab on the "Bri Ca" text
	Then I wait to see "Send Message"
	Then I press the "Send Message" button
	Then I send a doddle by message with a google image as background
	Then I wait for 3 seconds
	Then I send "3" doddles by message with a google image as background
	Then I wait for 3 seconds	
	Then I send "2" google image by messages
	Then I wait for 3 seconds
	Then I send a google image by message
	Then I wait for 3 seconds
	Then I send "3" texts messages
	Then I wait for 3 seconds
	Then I send a new message with a simple doddle
	Then I send "2" messages with a simple doddle