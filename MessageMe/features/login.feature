Feature: Login

Scenario: As a User I want to try to log in  using invalid credentials
	Given I wait to see "Log In"
	Then I press the "Log In" button
	Then I should see "Email"
	Then I enter invalid credentials
	Then I press the "Log In" button
	I wait until I see the login invalid credentials error message

Scenario: As a User I want to log in
	Given I wait to see "Log In"
	Then I press the "Log In" button
	Then I should see "Email"
	Then I enter valid credentials
	Then I press the "Log In" button
	Then I wait to see "Would you like"
	Then I tab on the "Cancel" text
	Then I wait to see "MESSAGES"
	
