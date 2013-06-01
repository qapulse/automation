# Error messages Steps

#Login Feature
# wait until I see the invalid credentials error message for the login
Then /^I wait until I see the login invalid credentials error message$/ do
   performAction('wait_for_text', ERROR_MESSAGES[:login_invalid_user_error_message])
end

