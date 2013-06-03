Feature: Send Messages

Scenario: Login
	Given I wait to see "Log In"
	Then I press the "Log In" button
	Then I should see "Email"
	Then I enter text "brian.carrillo@lognllc.com" into field with id "email_address"
	Then I wait 
	Then I enter text "asdfg" into field with id "password"
	Then I press the "Log In" button
	Then I wait to see "Would you"
	Then I touch the "No" text
	Then I wait to see "MESSAGES"
	
	
Scenario: I send a new message with a simple doddle	
	Given I wait to see "MESSAGES"
	Then I send a new message with a simple doddle
	Then I wait
	
Scenario:  I send 2 messages with a simple doddle
	Given I wait to see "MESSAGES"
	Then I send "2" messages with a simple doddle
	Then I wait

Scenario: I send a new text message
	Given I wait to see "MESSAGES"
	Then I send a new text message
	Then I wait
	
Scenario: I send 3 texts messages
	Given I wait to see "MESSAGES"
	Then I send "3" texts messages
	Then I wait

	
Scenario: I send a google image by message
	Given I wait to see "MESSAGES"
	Then I send a google image by message
	Then I wait

Scenario: I send 2 google image by messages
	Given I wait to see "MESSAGES"
	Then I send "2" google image by messages
	Then I wait
	
Scenario: I send a doddle by message with a google image as background
	Given I wait to see "MESSAGES"
	Then I send a doddle by message with a google image as background
	Then I wait
	
	
Scenario: Send 2 doddles by message with a google image as background
    Given I wait to see "MESSAGES"
	Then I send "2" doddles by message with a google image as background
	Then I wait
