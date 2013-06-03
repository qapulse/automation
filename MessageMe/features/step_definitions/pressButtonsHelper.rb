#I press new message button 
Then /^I press new message button$/ do
    performAction('click_on_screen',90, 10)
    performAction('wait_for_text', 'New Message')
end

#I press new contact button 
Then /^I press new contact button$/ do
    #Tab on the icon 
	performAction('click_on_screen',90, 10)
    #wait until the input is shown
	wait_for(:timeout => 5) { query("* id:'friends_invite_input'").size > 0 }
end

#I press edit profile button at the top of the profile screen 
Then /^I press edit profile button at the top of the profile screen$/ do
    performAction('click_on_screen',90, 10)
    performAction('wait_for_text', 'Edit My Profile')
end

#I press the search button at the top of the screen 
Then /^I press edit profile at the top of the profile screen$/ do
    performAction('click_on_screen',80, 10)
    performAction('wait_for_text', 'Edit My Profile')
end

Then /^I tab on the "([^\"]*)" text$/ do |text|
	touch(query("* marked:'#{text}'"))
end

Then /^I tab on the image with id "([^\"]*)"$/ do |id|
	touch(query("* id:'#{id}'"))
end

#I press change name button in profile screen
Then /^I press change profile name button$/ do
    performAction('click_on_screen',90, 10)
    performAction('wait_for_text', 'Edit My Profile')
end

#I press change name button in profile screen
Then /^I touch the screeen to activate it$/ do
    performAction('click_on_screen',90, 50)
end