# Error messages Steps

#Login Feature
# wait until I see the invalid credentials error message
Then /^wait until I see the invalid credentials error message$/ do
   performAction('wait_for_text', ERROR_MESSAGES[:login_invalid_user_error_message])
end

