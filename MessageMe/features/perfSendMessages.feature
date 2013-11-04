Feature: Performance Send Messages

Scenario: As a User I want to log in
	Given I wait to see "Log In"
	Then I wait for 1 seconds
	Then I touch the screeen to activate it
	Then I press the "Log In" button
	Then I wait to see "Email"
	Then I enter valid credentials
	Then I press the "Log In" button
	Then I wait to see "Would you like"
	Then I tab on the "Cancel" text
	Then I wait to see "Messages"

Scenario: I send 1 google image message
	Given I wait to see "Messages"
	Then I touch the "DAJ Big" text
	Then I send 1 google image message
	Then I wait

Scenario: I send a new text message
	Given I wait to see "Messages"
	Then I touch the "DAJ Big" text
	Then I send a new text message
	Then I wait

Scenario: I send 200 google image messages
	Given I wait to see "Messages"
	Then I touch the "DAJ Big" text
	Then I send 200 google image messages
	Then I wait
