
#I enter valid information for a new user 
Then /^I enter valid user information new user$/ do
	#Clean the inputs
	performAction('clear_id_field', 'first_name')
	performAction('clear_id_field', 'last_name')
	performAction('clear_id_field', 'email_address')
	performAction('clear_id_field', 'password')
	
	#Enter new information in the fields
    performAction('enter_text_into_id_field',NEWUSER[:valid][:firstName], 'first_name')
	performAction('enter_text_into_id_field',NEWUSER[:valid][:lastName], 'last_name')
	performAction('enter_text_into_id_field',NEWUSER[:valid][:email], 'email_address')
	performAction('enter_text_into_id_field',NEWUSER[:valid][:password], 'password')
end