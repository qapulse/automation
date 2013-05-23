def findContact (contactName)
	
	#This variable indicates if the element where found in the screen
	flag = false 
	
	#Cicle to scroll the screen until the end
	for i in 0...50
			if (element_exists("* marked:'#{contactName}'"))
				i = i+ 50
				flag= true
			else
				performAction('scroll_down')
			end
	end
	
	return flag
end