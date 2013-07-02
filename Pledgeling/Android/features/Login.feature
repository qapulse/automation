Feature: Login feature

    Scenario: Tab on Login button with the email field empty
        Given I wait to see "Sign In"
        Then I press the "Sign In" button
        Then I wait to see "Please enter your email"

    Scenario: Tab on Login button with the password field empty
        Given I wait to see "Sign In"
        Then I enter text "user@logn.co" into field with id "editTextEmail"
        Then I press the "Sign In" button
        Then I wait to see "Please enter your password"

    Scenario: Tab on Login button with an invalid email
        Given I wait to see "Sign In"
        Then I enter text "userlogn.co" into field with id "editTextEmail"
        Then I press the "Sign In" button
        Then I wait to see "Please enter a valid email address."
        
    Scenario: Tab on Login button with an invalid password and a valid user
        Given I wait to see "Sign In"
        Then I enter text "mobcrtest01@gmail.com" into field with id "editTextEmail"
        Then I enter text "mypassword" into field with id "editTextPassword"
        Then I press the "Sign In" button
        Then I wait to see "Error with your login or password"
        
    Scenario: Tab on Login button with an short password
        Given I wait to see "Sign In"
        Then I enter text "mobcrtest01@gmail.com" into field with id "editTextEmail"
        Then I enter text "pwd12" into field with id "editTextPassword"
        Then I press the "Sign In" button
        Then I wait to see "Please enter a valid password between 6 and 20 characters."
        
        Scenario: Tab on Login button with an invalid password and a valid user
        Given I wait to see "Sign In"
        Then I enter text "mobcrtest01@gmail.com" into field with id "editTextEmail"
        Then I enter text "test1234" into field with id "editTextPassword"
        Then I press the "Sign In" button
        Then I wait to see "Upcomings events"
       