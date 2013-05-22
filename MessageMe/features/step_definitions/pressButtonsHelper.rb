#I press new message button 
Then /^I press new message button$/ do
    performAction('click_on_screen',90, 10)
    performAction('wait_for_text', 'New Message')
end

#I press new contact button 
Then /^I press new contact button$/ do
    performAction('click_on_screen',90, 10)
    #wait until the input is shown
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