Feature: Personalization Flow

	Scenario: Tab on Sign up button with a new valid user
        Given I wait to see "Sign In"
        Then I touch the "Sign Up" text
        Then I enter text "brian carrillo" into field with id "editTextFullName"
        Then I enter text "mail06@logn.co" into field with id "editTextEmail"
        Then I enter text "password" into field with id "editTextPassword"
        Then I press the "Sign Up" button
        Then I wait to see "Welcome to Pledgeling"
        Then I touch the "Select your age" text
        Then I wait for 3 seconds
        Then I touch the "18" text
        Then I wait for 3 seconds
        Then I touch the "Next" text
        Then I wait for 3 seconds
        Then I touch the "Next" text
        Then I wait for 3 seconds
        Then I touch the "Done" text
	