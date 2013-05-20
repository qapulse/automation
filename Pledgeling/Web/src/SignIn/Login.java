package SignIn;

import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
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
	 
	 @Parameters({"host","browser","port"})
	 
	 /*
	  * This class loads the environments where tests will be performed  
	  */
	 @BeforeClass
	 public void beforeClass(String host,String browser,String port){
	  DesiredCapabilities capability= new DesiredCapabilities();
	  capability.setBrowserName(browser);
	
	  try {
	    driver= new RemoteWebDriver(new URL("http://".concat(host)+":".concat(port).concat("/wd/hub")), capability);
	  } catch (MalformedURLException e) {
	   e.printStackTrace();
	  }
	 }
	 
	 /*
	  * Open the Pledgeling website
	  */
	 @Test
	 public void openPledgelingsite(){
		 driver.get("http://pledgeling-dev.logn.co/");
		  (new WebDriverWait(driver, 10)).until((ExpectedConditions.elementToBeClickable(By.className("btn-signin"))));
			 
	 }
	 
	 @Test
	 public void openSingInModal(){
		 driver.findElement(By.className("btn-signin")).click();
		  (new WebDriverWait(driver, 10)).until(new ExpectedCondition<WebElement>() {
		         public WebElement apply(WebDriver d) {
		             return d.findElement(By.name("email"));
		         }
		     });
	 }

	 @Test
	 public void signInCorrectly(){
		 WebElement emailAddress = driver.findElement(By.name("email"));
		 WebElement password = driver.findElement(By.name("password"));
		 
		 emailAddress.sendKeys("mobcrtest01@gmail.com");
		 password.sendKeys("test1234");
	 }
	 
	 @Test
	 public void clickSingIn(){
		driver.findElement(By.xpath("html/body/div[2]/div[3]/div/button[4]")).click();
	 }
	 
}
