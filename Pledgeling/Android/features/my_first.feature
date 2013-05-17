Feature: Login feature

  Scenario: Sign in with an empty password
	Given I wait to see "Sign In"
	Then I enter text "brian.carrillo@lognllc.com" into field with id "editTextEmail"
	Then I press the "Sign In" button	
	Then I wait to see "Please enter your password"
	
	Scenario: Sign in with an empty email
	Given I wait to see "Sign In"
	Then I press the "Sign In" button
	Then I wait
	Then I wait to see "Please enter your email"
	
	Scenario: Sign in with an empty password
	Given I wait to see "Sign In"
	Then I enter text "brian.carrillo@lognllc.com" into field with id "editTextEmail"
	Then I enter text "brian.carrillo@lognllc.com" into field with id "editTextPassword"
	Then I press the "Sign In" button	
	Then I wait to see "Error with your login or password"

	

	