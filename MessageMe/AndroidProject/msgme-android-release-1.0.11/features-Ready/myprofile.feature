Feature: My profile 

Scenario: Change the user image using a google image
	Given I wait to see "MESSAGES"
	Then I touch the "MY PROFILE" text
	Then I touch the profile image
	Then I touch the "Update Profile Picture" text
	Then I touch the "Google Images" text
	Then I find a "Gmail" image
	Then I wait to see "MY PROFILE"
	

Scenario: Change the cover image using a google image
	Given I wait to see "MESSAGES"
	Then I touch the "MY PROFILE" text
	Then I touch the profile image
	Then I touch the "Update Profile Picture" text
	Then I touch the "Google Images" text
	Then I find a "Gmail" image
	Then I wait to see "MY PROFILE"

Scenario: Update the Profile Name
	Given I wait to see "MESSAGES"
	Then I touch the "MY PROFILE" text
	Then I press change profile name button
	Then I clear input field number 1
	Then I clear input field number 2
	Then I enter "Didier A" into input field number 1
	Then I enter "Corrales " into input field number 2
	Then I tab on the "Save" text

Scenario: Try to log out but tap on No button
	Given I wait to see "MESSAGES"
	Then I touch the "MY PROFILE" text
	Then I touch the "Settings" text
	Then I scroll down
	Then I press the "Log Out" button
	Then I wait to see "Log Out of MessageMe"
	Then I press the "No" button
	Then I wait to see "Log Out"

Scenario: Log out
	Given I wait to see "MESSAGES"
	Then I touch the "MY PROFILE" text
	Then I touch the "Settings" text
	Then I scroll down
	Then I press the "Log Out" button
	Then I wait to see "Log Out of MessageMe"
	Then I press the "Yes" button
	Then I wait to see "Log In"