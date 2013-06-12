Feature: Create an new account using the email invite an user and and send messages

Scenario: As a User I want to try to log in  using invalid credentials
	Given I wait to see "Log In"
	Then I wait for 2 seconds
	Then I touch the screeen to activate it
	Then I press the "Log In" button
	Then I wait to see "Email"
	Then I enter invalid credentials
	Then I press the "Log In" button
	Then I wait until I see the login invalid credentials error message

Scenario: As a User I want to log in
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
	
Scenario: Block an user
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" text
	
	Scenario: Block an user
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" tex

	Scenario: Block an user
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" tex

	Scenario: Block an user
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" tex

	Scenario: Block an user
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" tex

	Scenario: Block an user
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" tex

	Scenario: Block an user
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" tex
	Scenario: Block an user
	Given I wait to see "MESSAGES"
	Then I touch the "CONTACTS" tex
	