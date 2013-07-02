Feature: Sign Up

  Scenario: Tab on Sign up button with the empty fields
	Given I wait to see "Sign In"
	Then I touch the "Sign Up" text
	Then I wait to see "Sign Up"
	Then I wait for 2 seconds
	Then I press the "Sign Up" button	
	Then I wait to see "Please enter your name"
	
	Scenario: Tab on Sign up button with the  name field empty 
	Given I wait to see "Sign In"
	Then I touch the "Sign Up" text
	Then I press the "Sign Up" button	
	Then I wait to see "Please enter your name"
	
	Scenario: Tab on Sign up button with the  email field empty 
	Given I wait to see "Sign In"
	Then I touch the "Sign Up" text
	Then I enter text "brian carrillo" into field with id "editTextFullName"
	Then I press the "Sign Up" button	
	Then I wait to see "Please enter your email"
	
	Scenario: Tab on Sign up button with the  password field empty 
	Given I wait to see "Sign In"
	Then I touch the "Sign Up" text
	Then I enter text "brian carrillo" into field with id "editTextFullName"
	Then I enter text "mail@mail.com" into field with id "editTextEmail"
	Then I press the "Sign Up" button	
	Then I wait to see "Please enter your password"
	
	Scenario: Tab on Sign up button with the  password field empty 
	Given I wait to see "Sign In"
	Then I touch the "Sign Up" text
	Then I enter text "brian carrillo" into field with id "editTextFullName"
	Then I enter text "mail@mail.com" into field with id "editTextEmail"
	Then I enter text "passw" into field with id "editTextPassword"
	Then I press the "Sign Up" button	
	Then I wait to see "Please enter a valid password between 6 and 20 characters"
	
	Scenario: Tab on Sign up button with a user already registered
	Given I wait to see "Sign In"
	Then I touch the "Sign Up" text
	Then I enter text "brian carrillo" into field with id "editTextFullName"
	Then I enter text "mail@mail.com" into field with id "editTextEmail"
	Then I enter text "password" into field with id "editTextPassword"
	Then I press the "Sign Up" button	
	Then I wait to see "Welcome back! You've already created an account with us. Please sign in"
	
	Scenario: Tab on Sign up button with a new valid user
	Given I wait to see "Sign In"
	Then I touch the "Sign Up" text
	Then I enter text "brian carrillo" into field with id "editTextFullName"
	Then I enter text "mail@mail02.com" into field with id "editTextEmail"
	Then I enter text "password" into field with id "editTextPassword"
	Then I press the "Sign Up" button	
	Then I wait to see "Welcome to Pledgeling"
	