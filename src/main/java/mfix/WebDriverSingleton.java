package mfix;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

public class WebDriverSingleton
{
	private static boolean IS_CUSTOMIZED_BROWSER_WINDOW_SIZE = true;
	private static int BROWSER_WIDTH = 950;
	private static int BROWSER_HEIGHT = 1945;
	
	private static WebDriverSingleton instance = null;
	private static WebDriver driver;

	public enum Browser
	{
		FIREFOX, INTERNET_EXPLORER, CHROME, EMULATED_MOBILE;
	}

	private WebDriverSingleton(Browser browser)
	{
		switch (browser)
		{
			case FIREFOX:
				openFirefox();
				break;
	
			case INTERNET_EXPLORER:
				openInternetExplorer();
				break;
	
			case CHROME:
				openChrome();
				break;
				
			case EMULATED_MOBILE:
				openEmulatedMobile();
				break;
		}
	}

	private void openFirefox()
	{
		WebDriver driver;
		if (Constants.HEADLESS_FIREFOX)
		{
			String Xport = System.getProperty("lmportal.xvfb.id", ":2");
			final File firefoxPath = new File(System.getProperty("lmportal.deploy.firefox.path", "/usr/bin/firefox"));
			FirefoxBinary firefoxBinary = new FirefoxBinary(firefoxPath);
			firefoxBinary.setEnvironmentProperty("DISPLAY", Xport);
			driver = new FirefoxDriver(firefoxBinary, null);
		}
		else
		{
			driver = new FirefoxDriver();
		}
		if (IS_CUSTOMIZED_BROWSER_WINDOW_SIZE)
		{
			driver.manage().window().setSize(new Dimension(BROWSER_WIDTH, BROWSER_HEIGHT));
		}
		else
		{
			driver.manage().window().maximize();
		}
		WebDriverSingleton.driver = driver;
	}

	private void openInternetExplorer()
	{
		System.setProperty("webdriver.ie.driver", Constants.INTERNET_EXPLORER_DRIVER_FILEPATH);
		WebDriver driver = new InternetExplorerDriver();
		if (IS_CUSTOMIZED_BROWSER_WINDOW_SIZE)
		{
			driver.manage().window().setSize(new Dimension(BROWSER_WIDTH, BROWSER_HEIGHT));
		}
		else
		{
			driver.manage().window().maximize();
		}
		WebDriverSingleton.driver = driver;
	}

	private void openChrome()
	{
		System.setProperty("webdriver.chrome.driver", Constants.CHROME_DRIVER_FILEPATH);
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--start-maximized");
		WebDriver driver = new ChromeDriver(options);
		if (IS_CUSTOMIZED_BROWSER_WINDOW_SIZE)
		{
			driver.manage().window().setSize(new Dimension(BROWSER_WIDTH, BROWSER_HEIGHT));
		}
		else
		{
			driver.manage().window().maximize();
		}
		WebDriverSingleton.driver = driver;
	}

	public static void openEmulatedMobile()
	{
		System.setProperty("webdriver.chrome.driver", Constants.CHROME_DRIVER_FILEPATH);

		DesiredCapabilities desiredCapabilities = DesiredCapabilities.chrome();

		Map<String, Object> deviceMetrics = new HashMap<String, Object>();
		deviceMetrics.put("width", BROWSER_WIDTH);
		deviceMetrics.put("height", BROWSER_HEIGHT);
		deviceMetrics.put("pixelRatio", 1.0);

		Map<String, Object> mobileEmulation = new HashMap<String, Object>();
		mobileEmulation.put("deviceMetrics", deviceMetrics);
		mobileEmulation.put("userAgent", "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1");

		ChromeOptions options = new ChromeOptions();
		options.setExperimentalOption("mobileEmulation", mobileEmulation);
		if(Constants.HEADLESS_CHROME)
		{
			//options.setBinary("/Applications/Google Chrome Canary.app/Contents/MacOS/Google Chrome Canary");
			options.addArguments("--headless");
			options.addArguments("--disable-gpu");
			options.addArguments("--window-size=" + BROWSER_WIDTH + "," + BROWSER_HEIGHT);
		}

		desiredCapabilities.setCapability(ChromeOptions.CAPABILITY, options);
		ChromeDriver driver = new ChromeDriver(desiredCapabilities);

		if(!Constants.HEADLESS_CHROME)
		{
			driver.manage().window().setSize(new Dimension(BROWSER_WIDTH, BROWSER_HEIGHT));
		}
		WebDriverSingleton.driver = driver;
	}
	
	public static void openBrowser()
	{
		if (instance == null)
		{
			WebDriverSingleton.driver = null;
			instance = new WebDriverSingleton(Constants.BROWSER);
		}
	}

	public static WebDriver getDriver()
	{
		openBrowser();
		return driver;
	}

	public static void closeBrowser()
	{
		if (driver != null)
		{
			driver.quit();
			driver = null;
			instance = null;
		}
	}

	public static void loadPage(String htmlFileFullPath)
	{
		openBrowser();
		String urlString = htmlFileFullPath;
		if (!urlString.contains("http://"))
		{
			urlString = "file:///" + urlString;
		}
		try
		{
			driver.manage().timeouts().pageLoadTimeout(60, TimeUnit.SECONDS);
			driver.get(urlString);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void takeScreenshot(String imageFilepath)
	{
		WebDriver driver = getDriver();
		File screenshot = null;

		if(Constants.BROWSER.name().equalsIgnoreCase(Browser.CHROME.name()) || Constants.BROWSER.name().equalsIgnoreCase(Browser.EMULATED_MOBILE.name()))
		{
			Screenshot ashotScreenshot = new AShot().shootingStrategy(ShootingStrategies.viewportPasting(100)).takeScreenshot(driver);
			BufferedImage img = ashotScreenshot.getImage();
			try
			{
				screenshot = File.createTempFile("tempScreenshot", ".png");
				ImageIO.write(img, "png", screenshot);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			// reload the page to eliminate any effects of scrolling
			String code = "window.scrollTo(0, 0)";
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript(code);
		 }
		 else
		 {
			 screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
		 }

		try
		{
			FileUtils.copyFile(screenshot, new File(imageFilepath));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}