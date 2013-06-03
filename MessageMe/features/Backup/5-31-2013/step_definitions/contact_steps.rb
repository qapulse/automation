
# Find a image by google
Then /^I find "([^\"]*)" from the contact list$/ do |contactName|
	puts "start of the find contact"
	findContact(contactName)
	puts "end of the find contact"
	
end