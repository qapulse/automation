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
	Then I send "1" test messages
	Then I send "1" test messages
	Then I wait for 30 seconds
	Then I send "1" test messages
	Then I wait
	