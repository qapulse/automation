def findContact (contactName)
	puts "start of the find contact in metod"
	#This variable indicates if the element where found in the screen
	flag = false 
	 puts contactName
	#Cicle to scroll the screen until the end
	for i in 0...50
			if (element_exists("* marked:'#{contactName}'"))
				i = i+ 50
				flag= true
				puts "found"
			else
				performAction('scroll_down')
			end
	end
	puts "start of the find contact in metod"
	return flag
end