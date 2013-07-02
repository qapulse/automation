#I press menu button
Then /^I press new message button$/ do
    performAction('click_on_screen',10, 10)
    performAction('wait_for_text', 'ACCOUNT')
end