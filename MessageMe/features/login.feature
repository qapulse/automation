Feature: Login

Scenario: As a User I want to see the error message when enter invalid credentials
	Given I wait to see "Log In"
	Then I press the "Log In" button
	Then I should see "Email"
	Then I enter valid credentials
	Then I press the "Log In" button
	Then I wait for 15 seconds
