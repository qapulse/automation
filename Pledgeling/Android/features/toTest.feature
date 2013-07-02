Feature: test feature

  Scenario: Tab on Sign up button with a user already registered
	Given I wait to see "Sign In"
	Then I touch the "Sign Up" text
	Then I enter text "brian carrillo" into field with id "editTextFullName"
	Then I enter text "mail@mail.com" into field with id "editTextEmail"
	Then I enter text "password" into field with id "editTextPassword"
	Then I press the "Sign Up" button	
	Then I wait to see "Email Welcome back! You ve already created an account with us. Please sign in"
