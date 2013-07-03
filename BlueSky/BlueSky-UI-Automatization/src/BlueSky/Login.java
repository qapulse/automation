package BlueSky;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;


import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class Login {
	

	 private WebDriver driver;
	 private String url;
	 @Parameters({"host","browser","port","siteUrl"})
	 
	 /*
	  * This class loads the environments where tests will be performed  
	  */
	 @BeforeClass
	 public void beforeClass(String host,String browser,String port, String siteUrl){
		 
		 url = siteUrl;
		 DesiredCapabilities capability= new DesiredCapabilities();
		 capability.setBrowserName(browser);
	
		 try {
			 	driver= new RemoteWebDriver(new URL("http://".concat(host)+":".concat(port).concat("/wd/hub")), capability);
		 	} catch (MalformedURLException e) {
		 		e.printStackTrace();
		 		}
	 }
	 
	 /*
	  * Open the website
	  */
	 @Test
	 public void openSite(){
		 
		 // Open the website, the url is set in the TestNG configuration xml file 
		 driver.get(url);
		 
		 // Wait until the elements are shown in the screen
	//	 (new WebDriverWait(driver, 15)).until((ExpectedConditions.elementToBeClickable(By.name("users"))));
			 
	 }
	 
	 @Test
	 public void clikOnSingIn(){
		 
		 // Click on sign In link
		 driver.findElement(By.className("users")).click();
		 try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		// Wait until the elements are shown in the screen
	//	 (new WebDriverWait(driver, 8)).until((ExpectedConditions.elementToBeClickable(By.name("bsHomePageLoginButton"))));
	 }
	 
	 @Test
	 public void enterUserInformation(){
		
		 List <WebElement> inputs = driver.findElements(By.className("text-labellogin"));

		 Iterator<WebElement> input=  inputs.iterator();
		 
		 Integer i= 1 ;
		 WebElement myinput;
		 
		 while(input.hasNext())
	        {
			 	if (i ==1){
				 	myinput = (WebElement)input.next();
				 	myinput.sendKeys("logntest");
				 	i++;
				 	}
			 	if (i ==2){
				 	myinput = (WebElement)input.next();
				 	myinput.sendKeys("passw0rd");
				 	
				 	}
	        }
	 }
	 
	 @Test
	 public void clickSingInButton(){
	
		
		 driver.findElement(By.className("bsHomePageLoginButton")).click();
		
		// Wait until the elements are shown in the screen
		//(new WebDriverWait(driver, 10)).until((ExpectedConditions.elementToBeClickable(By.name("bsEbookselectedNewFav"))));
	 }
	 
	 @Test
	 public void verifyLogged(){
		 
		 int exitValue=0;
		 boolean exitCondition = true;
		 
		 while (exitCondition == true){
			 
			 if (exitValue >= 120)
				 exitCondition = false;
			 else
			 {
				 try {
						
					 	Thread.sleep(10000);		 
						driver.findElement(By.name("searchKeyword"));
						
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				 exitValue++;	 
			 }
		 }
 }
	 
	 private boolean isElementPresent()
	 {
		 
		 boolean present = true;
		 
		 try {
				Thread.sleep(10000);
				
				 try {
					 driver.findElement(By.name("searchKeyword"));
				    present = true;
				 } catch (NoSuchElementException e) {
				    present = false;
				 }
				
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 
		 return present;
	 }
	 
	 
	 @After
	 public void Close()
	 {
		 new WebDriverWait(driver, 5);
		 driver.close();
		 driver.quit();
	 }
	 
	 
}
