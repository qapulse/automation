#I send a new message with a simple doddle
Then /^I send a new message with a simple doddle$/ do
   wait_for(:timeout => 5) { query("* id:'package_chooser_button'").size > 0 }
    touch ("* id:'package_chooser_button'")
    performAction('wait', 3)
    touch ("* id:'doodle_btn'")
    performAction('wait', 3)
    performAction('click_on_screen',85, 25)
    performAction('click_on_screen',85, 40)
    performAction('click_on_screen',85, 60)
    performAction('click_on_screen',50, 25)
    performAction('wait', 3)
    performAction('click_on_screen',90, 10)
    wait_for(:timeout => 15) { query("* id:'package_chooser_button'").size > 0 }
end

# I send # of messages with a simple doddle
Then /^I send "([^\"]*)" messages with a simple doddle$/ do |messagesNumber|
	wait_for(:timeout => 5) { query("* id:'package_chooser_button'").size > 0 }
	#Send # messages
	for i in 0...Integer(messagesNumber)
		touch ("* id:'package_chooser_button'")
		performAction('wait', 3)
		touch ("* id:'doodle_btn'")
		performAction('wait', 3)
		performAction('click_on_screen',85, 25)
		performAction('click_on_screen',85, 40)
		performAction('click_on_screen',85, 60)
		performAction('click_on_screen',50, 25)
		performAction('wait', 3)
		performAction('click_on_screen',90, 10)
		wait_for(:timeout => 15) { query("* id:'package_chooser_button'").size > 0 }
	end
end

# I send a new text message
Then /^I send a new text message$/ do
	wait_for(:timeout => 5) { query("* id:'package_chooser_button'").size > 0 }
    performAction('enter_text_into_id_field','message from automation', 'chat_input')
    performAction('wait', 2)
    touch (query("* id:'send_button'"))
    performAction('wait', 2)
end    

# I send # of  texts messages
Then /^I send "([^\"]*)" texts messages$/ do |messagesNumber|
	wait_for(:timeout => 5) { query("* id:'package_chooser_button'").size > 0 }
	#Send # messages
	for i in 0...Integer(messagesNumber)
		performAction('enter_text_into_id_field','message from automation', 'chat_input')
		performAction('wait', 2)
		touch (query("* id:'send_button'"))
		performAction('wait', 2)
	end
end  

# Send a google image by message  
Then /^I send a google image by message$/ do
    wait_for(:timeout => 5) { query("* id:'package_chooser_button'").size > 0 }
	touch ("* id:'package_chooser_button'")
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
	performAction('wait', 10)
	performAction('click_on_screen',90, 50)
	performAction('wait', 10)
	performAction('wait_for_view_by_id', 'picture_confirmation_accept')
	touch ("* id:'picture_confirmation_accept'")
	wait_for(:timeout => 5) { query("* id:'package_chooser_button'").size > 0 }
	
end

# Send a google image by message  
Then /^I send "([^\"]*)" google image by messages$/ do |messagesNumber|
    wait_for(:timeout => 5) { query("* id:'package_chooser_button'").size > 0 }
	
	#Send # messages
	for i in 0...Integer(messagesNumber)
		touch ("* id:'package_chooser_button'")
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
		performAction('wait', 10)
		performAction('click_on_screen',90, 50)
		performAction('wait', 10)
		performAction('wait_for_view_by_id', 'picture_confirmation_accept')
		touch ("* id:'picture_confirmation_accept'")
		wait_for(:timeout => 15) { query("* id:'package_chooser_button'").size > 0 }
	end
end

#Send a doddle by message with a google image as background
Then /^I send a doddle by message with a google image as background$/ do

    wait_for(:timeout => 15) { query("* id:'package_chooser_button'").size > 0 }
	touch ("* id:'package_chooser_button'")
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
	performAction('click_on_screen',90, 50)
	performAction('wait', 10)
	performAction('wait_for_view_by_id', 'picture_button')
	performAction('click_on_screen',90, 10)
	wait_for(:timeout => 15) { query("* id:'package_chooser_button'").size > 0 }
end

#Send # doddles by message with a google image as background
Then /^I send "([^\"]*)" doddles by message with a google image as background$/ do |messagesNumber|
		
	wait_for(:timeout => 15) { query("* id:'package_chooser_button'").size > 0 }
	
	#Send # messages
	for i in 0...Integer(messagesNumber)
		touch ("* id:'package_chooser_button'")
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
		performAction('click_on_screen',90, 50)
		performAction('wait', 10)
		performAction('wait_for_view_by_id', 'picture_button')
		performAction('click_on_screen',90, 10)
		wait_for(:timeout => 15) { query("* id:'package_chooser_button'").size > 0 }
		puts "----"
	end
end


# I send # of test messages including simple, google images, doodle messages
Then /^I send "([^\"]*)" test messages$/ do |messagesNumber|
    
	wait_for(:timeout => 5) { query("* id:'package_chooser_button'").size > 0 }
	
	#Send # messages
	for i in 0...Integer(messagesNumber)	
		
		#Send text message
		performAction('enter_text_into_id_field','message from automation', 'chat_input')
		performAction('wait', 2)
		touch (query("* id:'send_button'"))
		performAction('wait', 3)
			
		#Send "Take Photo" picture message
		# To be created. On Hold because calabash has not the information to use the camera.
		
		#Send simple Doodle
		touch ("* id:'package_chooser_button'")
		performAction('wait', 3)
		touch ("* id:'doodle_btn'")
		performAction('wait', 3)
		performAction('click_on_screen',85, 25)
		performAction('click_on_screen',85, 40)
		performAction('click_on_screen',85, 60)
		performAction('click_on_screen',50, 25)
		performAction('wait', 3)
		performAction('click_on_screen',90, 10)
		wait_for(:timeout => 15) { query("* id:'package_chooser_button'").size > 0 }
		
		#Send "Choose Existing" picture message
		# This image is a google image
		touch ("* id:'package_chooser_button'")
		performAction('wait', 3)
		touch ("* id:'picture_btn'")
		performAction('wait_for_text', 'Google Images')
		touch (query("* marked:'Google Images'"))
		performAction('wait', 3)
		#Then I enter text "doddle" into field with id "search_box"
		performAction('enter_text_into_id_field','doddle', 'search_box')
		performAction('click_on_screen',90, 15)
		performAction('wait', 20)
		performAction('click_on_screen',60, 20)
		performAction('wait', 10)
		performAction('click_on_screen',90, 50)
		performAction('wait', 10)
		performAction('wait_for_view_by_id', 'picture_confirmation_accept')
		touch ("* id:'picture_confirmation_accept'")
		wait_for(:timeout => 15) { query("* id:'package_chooser_button'").size > 0 }
		
		#Send Doodle with Google image background 
		touch ("* id:'package_chooser_button'")
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
		performAction('click_on_screen',60, 70)
		performAction('wait', 10)
		performAction('click_on_screen',90, 50)
		performAction('wait', 10)
		performAction('wait_for_view_by_id', 'picture_button')
		performAction('click_on_screen',90, 10)
		wait_for(:timeout => 15) { query("* id:'package_chooser_button'").size > 0 }
	end
end