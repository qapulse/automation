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

Given /^I continue if rate message me is not visible/ do 
    
# buscar si el texto esta en la pantalla 
 $textVisible = waitUntilTextisOnScreen("Rate MessageMe","MESSAGES")


    if $textVisible == "Rate MessageMe"
        touch(query("* marked:'Maybe Later'"))
    end

end

Given /^I wait to see "([^\"]*)" or "([^\"]*)" if "([^\"]*)" is visible tab on "([^\"]*)"/ do  |fristCondition, secondCondition, toEvaluate, textToTab|
    
# buscar si el texto esta en la pantalla 
 $textVisible = waitUntilTextisOnScreen(fristCondition,secondCondition)


    if $textVisible == toEvaluate
        touch(query("* marked:'#{textToTab}'"))
    end

end


#I press change name button in profile screen
Then /^I test$/ do
    puts "entre al i test"
    $textVisible = waitUntilTextisOnScreen("Log In","Start Messaging")
    puts   "sali del  i test"
    puts  $textVisible

    if $textVisible == "Log In"
        puts = "estoy en el login"
    else
        if $textVisible == "Start Messaging"
            puts = "Es lo de crear cueta"
        end
    end
    puts = "fin del ciclo"

end


def waitUntilTextisOnScreen(mytext1, mytext2)

    $isOnScreen = false
    $mytimeout = 0
    $toReturn = ""

    #do it until the text is present
    while $isOnScreen == false

        if $mytimeout > 6
            $isOnScreen=true 
           
        else
         performAction('wait',3)

            checkforFirstTextisVisible = query("* marked:'#{mytext1}'")
            checkforSecondTextisVisible = query("* marked:'#{mytext2}'")

            if checkforFirstTextisVisible.any?
                $isOnScreen=true
                $toReturn = mytext1
               
            else
                if checkforSecondTextisVisible.any?
                    $isOnScreen=true  
                    $toReturn = mytext2   
                    
                end
            end
        end
        
        #Increase the timeout value
        $mytimeout = $mytimeout + 1
    end

    return $toReturn
end
