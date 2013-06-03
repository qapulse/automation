Feature: Login

Scenario: As a User I want to see the error message when enter invalid credentials
	Given I wait to see "Log In"
	Then I press the "Create New Account" button
	Then I wait to see "Connect with Facebook"
	Then I tab on the "No" text
	Then I enter valid user information new user
	Then I press the "Start Messaging" button
	Then I wait to see "Would you like"
	Then I tab on the "Cancel" text
	Then I wait to see "MESSAGES"
	
Scenario: As a User I want to create an account
	Given I wait to see "Log In"
	Then I press the "Create New Account" button
	Then I wait to see "Connect with Facebook"
	Then I tab on the "No" text
	Then I enter valid user information new user
	Then I press the "Start Messaging" button
	Then I wait to see "Would you like"
	Then I tab on the "Cancel" text
	Then I wait to see "MESSAGES"
	