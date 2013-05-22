Feature: Contacts

Scenario: Block a user
	Given I wait to see "MESSAGES"

	#Then find a contact
	
	Then I press the "Block" button
	Then I wait to see "Blocked"

Scenario: Unblock a user
	Given I wait to see "MESSAGES"

	#Then find a contact
	
	Then I press the "Unlock" button
	Then I wait to see "Send Message"