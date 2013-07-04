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
import org.xml.sax.InputSource;

public class LoginWithFunctionalTestClickingOnTabs {
	

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
			Thread.sleep(5000);
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
		(new WebDriverWait(driver, 10)).until((ExpectedConditions.elementToBeClickable(By.name("searchKeyword"))));
	 }
	 

	 @Test
	 public void clickOnMinutesButton(){
	//	 bsBookpublishedContainer
		// (new WebDriverWait(driver, 10)).until((ExpectedConditions.elementToBeClickable(By.name("bsBookshelfMinusImage"))));
	 
		 WebElement element = driver.findElement(By.className("bsBookpublishedContainer"));
		 WebElement minusbutton = element.findElement(By.className("bsBookshelfMinusImage"));
		 minusbutton.click();
	
	 }
	  
	 
	 @Test
	 public void enterInfoInSearchButton(){
		 
				
					(new WebDriverWait(driver, 10)).until((ExpectedConditions.elementToBeClickable(By.name("searchKeyword"))));
					 
						driver.findElement(By.name("searchKeyword")).clear();
						driver.findElement(By.name("searchKeyword")).sendKeys("latin");
						driver.findElement(By.className("bsBooksearchButton")).click();
		 }
	 
	 @Test
	 public void waitTenMinutes(){
	try {
		Thread.sleep(600000);
		} catch (InterruptedException e) {
		// TODO Auto-generated catch block
	//	e.printStackTrace();
			
			driver.findElement(By.name("mipatitofeo")).clear();
		}
	 }
	 
	 @Test
	 public void clickOnSecondTabs(){
		 
		 try {
				Thread.sleep(5000);
				} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//	e.printStackTrace();
				}
		 (new WebDriverWait(driver, 15)).until((ExpectedConditions.elementToBeClickable(By.className("bsEbookselectedNewFav"))));
			
		 List <WebElement> inputs = driver.findElements(By.className("bsEbookselectedNewFav"));

		 Iterator<WebElement> input=  inputs.iterator();
		 
		 WebElement myinput;

		 if(inputs.isEmpty())
			 driver.findElement(By.name("mipatitofeo")).clear();
		 else{
		 int i = 1;	 
			input.next();
		 while(input.hasNext()){
			 
			
					 	if (i ==1){
						 	myinput = (WebElement)input.next();
						 	myinput.click();
						 	
					
						 	try {
								Thread.sleep(5000);
								} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								//	e.printStackTrace();
								}
						 	}
					 
			        }
		 }
	 }

	 
	 @Test
	 public void clickOnFirstTabs(){
		 
		 try {
				Thread.sleep(5000);
				} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//	e.printStackTrace();
				}
		 (new WebDriverWait(driver, 15)).until((ExpectedConditions.elementToBeClickable(By.className("bsEbookselectedNewFav"))));
			
		 List <WebElement> inputs = driver.findElements(By.className("bsEbookselectedNewFav"));

		 Iterator<WebElement> input=  inputs.iterator();
		 
		 WebElement myinput;

		 if(inputs.isEmpty())
			 driver.findElement(By.name("mipatitofeo")).clear();
		 else{
		 int i = 1;	 
			//input.next();
		 while(input.hasNext()){
			 
			
					 	if (i ==1){
						 	myinput = (WebElement)input.next();
						 	myinput.click();
						 	
					
						 	try {
								Thread.sleep(5000);
								} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								//	e.printStackTrace();
								}
						 	}
					 
			        }
		 }
	 }
	 
	 

	 @Test
	 public void clickOnSecondTabs1(){
		 
		 try {
				Thread.sleep(5000);
				} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//	e.printStackTrace();
				}
		 (new WebDriverWait(driver, 15)).until((ExpectedConditions.elementToBeClickable(By.className("bsEbookselectedNewFav"))));
			
		 List <WebElement> inputs = driver.findElements(By.className("bsEbookselectedNewFav"));

		 Iterator<WebElement> input=  inputs.iterator();
		 
		 WebElement myinput;

		 if(inputs.isEmpty())
			 driver.findElement(By.name("mipatitofeo")).clear();
		 else{
		 int i = 1;	 
			input.next();
		 while(input.hasNext()){
			 
			
					 	if (i ==1){
						 	myinput = (WebElement)input.next();
						 	myinput.click();
						 	
					
						 	try {
								Thread.sleep(5000);
								} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								//	e.printStackTrace();
								}
						 	}
					 
			        }
		 }
	 }

	 
	 @Test
	 public void clickOnFirstTabs1(){
		 
		 try {
				Thread.sleep(5000);
				} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//	e.printStackTrace();
				}
		 (new WebDriverWait(driver, 15)).until((ExpectedConditions.elementToBeClickable(By.className("bsEbookselectedNewFav"))));
			
		 List <WebElement> inputs = driver.findElements(By.className("bsEbookselectedNewFav"));

		 Iterator<WebElement> input=  inputs.iterator();
		 
		 WebElement myinput;

		 if(inputs.isEmpty())
			 driver.findElement(By.name("mipatitofeo")).clear();
		 else{
		 int i = 1;	 
			//input.next();
		 while(input.hasNext()){
			 
			
					 	if (i ==1){
						 	myinput = (WebElement)input.next();
						 	myinput.click();
						 	
					
						 	try {
								Thread.sleep(5000);
								} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								//	e.printStackTrace();
								}
						 	}
					 
			        }
		 }
	 }

	 
	 
	 @Test
	 public void Close()
	 {
		 new WebDriverWait(driver, 5);
	 driver.close();
		// driver.quit();
	 }
	 
	 
}
