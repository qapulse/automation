Feature: My profile 

Scenario: Change the user image using a google image
	Given I wait to see "MESSAGES"
	Then I touch the "MY PROFILE" text
	# THEN CLICK IN THE IMAGE
	Then I touch the "Update Profile Picture" text
	Then I touch the "Google Images" text
	Then I find a "Gmail" image
	Then I wait to see "MY PROFILE"
	

Scenario: Change the cover image using a google image
	Given I wait to see "MESSAGES"
	Then I touch the "MY PROFILE" text
	# THEN CLICK IN THE IMAGE
	Then I touch the "Update Profile Picture" text
	Then I touch the "Google Images" text
	Then I find a "Gmail" image
	Then I wait to see "MY PROFILE"