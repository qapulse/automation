#I enter valid credentials for login 
Then /^I enter valid credentials$/ do
    performAction('enter_text_into_id_field',USERS[:valid][:email], 'email_address')
	performAction('enter_text_into_id_field',USERS[:valid][:password], 'password')
end

#I enter invalid credentials for login 
Then /^I enter invalid credentials$/ do
    performAction('enter_text_into_id_field',USERS[:invalid][:email], 'email_address')
	performAction('enter_text_into_id_field',USERS[:invalid][:password], 'password')
end
