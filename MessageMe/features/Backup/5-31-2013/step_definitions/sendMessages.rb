#I send a new message with a simple doddle
Then /^I send a new message with a simple doddle$/ do
    performAction('click_on_screen',90, 10)
    performAction('wait_for_text', 'Awesomeness')
    performAction('click_on_text', 'Awesomeness')
    performAction('wait_for_text', 'Awesomeness')
    touch ("* id:'add_chat_button'")
    performAction('wait', 3)
    touch ("* id:'doodle_btn'")
    performAction('wait', 3)
    performAction('click_on_screen',85, 25)
    performAction('click_on_screen',85, 40)
    performAction('click_on_screen',85, 60)
    performAction('click_on_screen',50, 25)
    performAction('wait', 3)
    performAction('click_on_screen',90, 10)
    performAction('wait', 20)
end

# I send # of messages with a simple doddle
Then /^I send "([^\"]*)" messages with a simple doddle$/ do |messagesNumber|
    performAction('click_on_screen',90, 10)
    performAction('wait_for_text', 'Awesomeness')
    performAction('click_on_text', 'Awesomeness')
    performAction('wait_for_text', 'Awesomeness')
	
	#Send # messages
	for i in 0...Integer(messagesNumber)
		touch ("* id:'add_chat_button'")
		performAction('wait', 3)
		touch ("* id:'doodle_btn'")
		performAction('wait', 3)
		performAction('click_on_screen',85, 25)
		performAction('click_on_screen',85, 40)
		performAction('click_on_screen',85, 60)
		performAction('click_on_screen',50, 25)
		performAction('wait', 3)
		performAction('click_on_screen',90, 10)
		performAction('wait', 20)	
	end
end

# I send a new text message
Then /^I send a new text message$/ do
    performAction('click_on_screen',90, 10)
    performAction('wait_for_text', 'Awesomeness')
    performAction('click_on_text', 'Awesomeness')
    performAction('wait_for_text', 'Awesomeness')
    performAction('enter_text_into_id_field','message from automation', 'chat_input')
    performAction('wait', 2)
    touch (query("* marked:'Send'"))
    performAction('wait', 10)
end    

# I send # of  texts messages
Then /^I send "([^\"]*)" texts messages$/ do |messagesNumber|
    performAction('click_on_screen',90, 10)
    performAction('wait_for_text', 'Awesomeness')
    performAction('click_on_text', 'Awesomeness')
    performAction('wait_for_text', 'Awesomeness')
	#Send # messages
	for i in 0...Integer(messagesNumber)
		performAction('enter_text_into_id_field','message from automation', 'chat_input')
		performAction('wait', 2)
		touch (query("* marked:'Send'"))
		performAction('wait', 10)
	end
end  

# Send a google image by message  
Then /^I send a google image by message$/ do
    performAction('click_on_screen',90, 10)
    performAction('wait_for_text', 'Awesomeness')
    performAction('click_on_text', 'Awesomeness')
    performAction('wait_for_text', 'Awesomeness')
	touch ("* id:'add_chat_button'")
    performAction('wait', 3)
	touch ("* id:'picture_btn'")
	performAction('wait_for_text', 'Google Images')
	touch (query("* marked:'Google Images'"))
	performAction('wait', 3)
	#Then I enter text "doddle" into field with id "search_box"
	performAction('enter_text_into_id_field','doddle', 'search_box')
	performAction('click_on_screen',90, 15)
	performAction('wait', 20)
	performAction('click_on_screen',60, 45)
	#performAction('wait_for_view_by_id', 'picture_confirmation_accept')
	#touch ("* id:'picture_confirmation_accept'")
	performAction('wait_for_text', 'Awesomeness')
	performAction('wait', 10)
	
end

# Send a google image by message  
Then /^I send "([^\"]*)" google image by messages$/ do |messagesNumber|
    performAction('click_on_screen',90, 10)
    performAction('wait_for_text', 'Awesomeness')
    performAction('click_on_text', 'Awesomeness')
    performAction('wait_for_text', 'Awesomeness')
	
	#Send # messages
	for i in 0...Integer(messagesNumber)
		touch ("* id:'add_chat_button'")
		performAction('wait', 3)
		touch ("* id:'picture_btn'")
		performAction('wait_for_text', 'Google Images')
		touch (query("* marked:'Google Images'"))
		performAction('wait', 3)
		#Then I enter text "doddle" into field with id "search_box"
		performAction('enter_text_into_id_field','doddle', 'search_box')
		performAction('click_on_screen',90, 15)
		performAction('wait', 20)
		performAction('click_on_screen',60, 45)
		#performAction('wait_for_view_by_id', 'picture_confirmation_accept')
		#touch ("* id:'picture_confirmation_accept'")
		performAction('wait_for_text', 'Awesomeness')
		performAction('wait', 10)
	end
end

#Send a doddle by message with a google image as background
Then /^I send a doddle by message with a google image as background$/ do
performAction('click_on_screen',90, 10)
    performAction('wait_for_text', 'Awesomeness')
    performAction('click_on_text', 'Awesomeness')
    performAction('wait_for_text', 'Awesomeness')
	touch ("* id:'add_chat_button'")
    performAction('wait', 3)
	touch ("* id:'doodle_btn'")
	performAction('wait_for_view_by_id', 'picture_button')
	touch ("* id:'picture_button'")
	performAction('wait_for_text', 'Google Images')
	touch (query("* marked:'Google Images'"))
	performAction('wait', 3)
	performAction('enter_text_into_id_field','doddle', 'search_box')
	performAction('click_on_screen',90, 15)
	performAction('wait', 20)
	performAction('click_on_screen',60, 45)
	performAction('wait', 10)
	performAction('click_on_screen',90, 10)
	performAction('wait', 10)
end

#Send # doddles by message with a google image as background
Then /^I send "([^\"]*)" doddles by message with a google image as background$/ do |messagesNumber|
performAction('click_on_screen',90, 10)
    performAction('wait_for_text', 'Awesomeness')
    performAction('click_on_text', 'Awesomeness')
    performAction('wait_for_text', 'Awesomeness')
	
	#Send # messages
	for i in 0...Integer(messagesNumber)
		touch ("* id:'add_chat_button'")
		performAction('wait', 3)
		touch ("* id:'doodle_btn'")
		performAction('wait_for_view_by_id', 'picture_button')
		touch ("* id:'picture_button'")
		performAction('wait_for_text', 'Google Images')
		touch (query("* marked:'Google Images'"))
		performAction('wait', 3)
		performAction('enter_text_into_id_field','doddle', 'search_box')
		performAction('click_on_screen',90, 15)
		performAction('wait', 20)
		performAction('click_on_screen',60, 45)
		performAction('wait', 10)
		performAction('click_on_screen',90, 10)
		performAction('wait', 10)
	end
end


# I send # of test messages including simple, google images, doodle messages
Then /^I send "([^\"]*)" test messages$/ do |messagesNumber|
    performAction('click_on_screen',90, 10)
    performAction('wait_for_text', 'Awesomeness')
    performAction('click_on_text', 'Awesomeness')
    performAction('wait_for_text', 'Awesomeness')
	
	#Send # messages
	for i in 0...Integer(messagesNumber)	
		
		#Send text message
		performAction('enter_text_into_id_field','message from automation', 'chat_input')
		performAction('wait', 2)
		touch (query("* marked:'Send'"))
		performAction('wait', 10)
			
		#Send "Take Photo" picture message
		# To be created. On Hold because calabash has not the information to use the camera.
		
		#Send simple Doodle
		touch ("* id:'add_chat_button'")
		performAction('wait', 3)
		touch ("* id:'doodle_btn'")
		performAction('wait', 3)
		performAction('click_on_screen',85, 25)
		performAction('click_on_screen',85, 40)
		performAction('click_on_screen',85, 60)
		performAction('click_on_screen',50, 25)
		performAction('wait', 3)
		performAction('click_on_screen',90, 10)
		performAction('wait_for_text', 'Awesomeness')
		performAction('wait', 3)
		
		#Send "Choose Existing" picture message
		# This image is a google image
		touch ("* id:'add_chat_button'")
		performAction('wait', 3)
		touch ("* id:'picture_btn'")
		performAction('wait_for_text', 'Google Images')
		touch (query("* marked:'Google Images'"))
		performAction('wait', 3)
		#Then I enter text "doddle" into field with id "search_box"
		performAction('enter_text_into_id_field','doddle', 'search_box')
		performAction('click_on_screen',90, 15)
		performAction('wait', 20)
		performAction('click_on_screen',60, 45)
		performAction('wait_for_text', 'Awesomeness')
		performAction('wait', 10)
		
		#Send Doodle with Google image background 
		touch ("* id:'add_chat_button'")
		performAction('wait', 3)
		touch ("* id:'doodle_btn'")
		performAction('wait_for_view_by_id', 'picture_button')
		touch ("* id:'picture_button'")
		performAction('wait_for_text', 'Google Images')
		touch (query("* marked:'Google Images'"))
		performAction('wait', 3)
		performAction('enter_text_into_id_field','doddle', 'search_box')
		performAction('click_on_screen',90, 15)
		performAction('wait', 20)
		performAction('click_on_screen',60, 45)
		performAction('wait_for_view_by_id', 'picture_button')
		performAction('click_on_screen',90, 10)
		performAction('wait_for_text', 'Awesomeness')
		performAction('wait', 10)
	end
end