
# Find a image by google
Then /^I find a "([^\"]*)" image$/ do |imageName|
	
	# Enter the name of the image in the input
	performAction('enter_text_into_id_field', imageName, 'search_box')

	#Click on the send button 
	performAction('click_on_screen',90, 15)

	#Wait until the images are loaded
	performAction('wait', 20)

	#Click on any image
	performAction('click_on_screen',60, 45)

	#Wait until the image is loaded
	performAction('wait', 20)
	
end