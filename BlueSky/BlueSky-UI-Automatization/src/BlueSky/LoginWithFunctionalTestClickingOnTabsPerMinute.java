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

public class LoginWithFunctionalTestClickingOnTabsPerMinute {
	

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
	 
////////////////////////////////////////////////////////////////////////////////////////////////
	 
	 @Test
	 public void waitAMinuteFrist()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 @Test
	 public void waitAMinuteSecond()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }

	 @Test
	 public void treswaitAMinuteSecond()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 @Test
	 public void cuatrowaitAMinuteSecond()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 @Test
	 public void cincowaitAMinuteSecond()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 @Test
	 public void seisowaitAMinuteSecond()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 @Test
	 public void sietewaitAMinuteSecond()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 @Test
	 public void ochowaitAMinuteSecond()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 @Test
	 public void nuevewaitAMinuteSecond()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 @Test
	 public void diezwaitAMinuteSecond()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 
	 @Test
	 public void oncewaitAMinuteSecond()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 
	 @Test
	 public void dosewaitAMinuteSecond()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 @Test
	 public void trecewaitAMinuteSecond()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 @Test
	 public void catorecewaitAMinuteSecond()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 @Test
	 public void quincewaitAMinuteSecond()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	 
	 @Test
	 public void clickOnSecondTab(){
		 
		
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
			        }
		 	}
		 }
	 }
	 
	 
	 @Test
	 public void dosclickOnSecondTab(){
		 
		
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
			        }
		 	}
		 }
	 }

	 @Test
	 public void tresclickOnSecondTab(){
		 
		
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
			        }
		 	}
		 }
	 }
	 
	 
	 @Test
	 public void cuatroclickOnSecondTab(){
		 
		
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
			        }
		 	}
		 }
	 }
	 
	 @Test
	 public void cincoclickOnSecondTab(){
		 
		
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
			        }
		 	}
		 }
	 }
	 
	 @Test
	 public void seisclickOnSecondTab(){
		 
		
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
			        }
		 	}
		 }
	 }
	 
	 
	 
	 @Test
	 public void sieteclickOnSecondTab(){
		 
		
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
			        }
		 	}
		 }
	 }
	 
	 
	 @Test
	 public void ochoclickOnSecondTab(){
		 
		
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
			        }
		 	}
		 }
	 }
	 
	 
	 @Test
	 public void nueveclickOnSecondTab(){
		 
		
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
			        }
		 	}
		 }
	 }
	 
	 
	 @Test
	 public void diezclickOnSecondTab(){
		 
		
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
			        }
		 	}
		 }
	 }
	 
	 @Test
	 public void onceclickOnSecondTab(){
		 
		
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
			        }
		 	}
		 }
	 }
	 
	 
	 @Test
	 public void doceclickOnSecondTab(){
		 
		
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
			        }
		 	}
		 }
	 }
	 
	 
	 @Test
	 public void trececlickOnSecondTab(){
		 
		
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
			        }
		 	}
		 }
	 }
	 
	 
	 @Test
	 public void catorceclickOnSecondTab(){
		 
		
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
			        }
		 	}
		 }
	 }
	 
	 
	 @Test
	 public void quinceclickOnSecondTab(){
		 
		
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
			        }
		 	}
		 }
	 }
	 
	 //////////////////////////////////////////////////////////////////////////////////////////////////
	 @Test
	 public void clickOnFirstTab(){
		 
		
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
					 
			        
		 }}}
	 }
	 
	 @Test
	 public void dosclickOnFirstTab(){
		 
		
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
					 
			        
		 }}}
	 }
	 @Test
	 public void tresclickOnFirstTab(){
		 
		
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
					 
			        
		 }}}
	 }
	 
	 
	 @Test
	 public void cuatroclickOnFirstTab(){
		 
		
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
					 
			        
		 }}}
	 }
	 @Test
	 public void cincoclickOnFirstTab(){
		 
		
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
					 
			        
		 }}}
	 }
	 
	 @Test
	 public void seisclickOnFirstTab(){
		 
		
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
					 
			        
		 }}}
	 }
	 
	 
	 @Test
	 public void sieteclickOnFirstTab(){
		 
		
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
					 
			        
		 }}}
	 }
	 
	 
	 @Test
	 public void ochoclickOnFirstTab(){
		 
		
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
					 
			        
		 }}}
	 }
	 
	 
	 @Test
	 public void nueveclickOnFirstTab(){
		 
		
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
					 
			        
		 }}}
	 }
	 
	 
	 @Test
	 public void diezclickOnFirstTab(){
		 
		
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
					 
			        
		 }}}
	 }
	 
	 
	 @Test
	 public void onceclickOnFirstTab(){
		 
		
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
					 
			        
		 }}}
	 }
	 
	 
	 @Test
	 public void doceclickOnFirstTab(){
		 
		
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
					 
			        
		 }}}
	 }
	 
	 @Test
	 public void trececlickOnFirstTab(){
		 
		
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
					 
			        
		 }}}
	 }
	 
	 @Test
	 public void catorececlickOnFirstTab(){
		 
		
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
					 
			        
		 }}}
	 }
	 
	 
	 
	 @Test
	 public void quincececlickOnFirstTab(){
		 
		
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
					 
			        
		 }}}
	 }
	 
	 /////////////////////////////////////////////////
	 
	 @Test
	 public void w1()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 
	 @Test
	 public void w2()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 @Test
	 public void w3()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 @Test
	 public void w4()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 @Test
	 public void w5()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 
	 @Test
	 public void w6()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 
	 
	 @Test
	 public void w7()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 @Test
	 public void w8()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 @Test
	 public void w9()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 
	 @Test
	 public void w10()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 @Test
	 public void w11()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 @Test
	 public void w12()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 @Test
	 public void w13()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 @Test
	 public void w14()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 @Test
	 public void w15()
	 {
		 
		 try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
