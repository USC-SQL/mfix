package mfix;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;

import mfix.approxAlgo.Chromosome;
import mfix.approxAlgo.Gene;
import mfix.domTree.Element;
import mfix.domTree.HtmlDomTree;
import mfix.domTree.HtmlElement;

import org.apache.commons.lang3.StringEscapeUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;


public class Util
{
	private static Map<String, String> elementPropValueCache;	// <xpath#prop, val>
	
	public static Map<String, String> getElementPropValueCache()
	{
		return elementPropValueCache;
	}

	public static void setElementPropValueCache(Map<String, String> elementPropValueCache)
	{
		Util.elementPropValueCache = elementPropValueCache;
	}

	public static List<Double> getNumbersFromString(String string)
	{
		List<Double> numbers = new ArrayList<Double>();
		Pattern p = Pattern.compile("(\\d+(?:\\.\\d+)?)");
		Matcher m = p.matcher(string);
		while (m.find())
		{
			numbers.add(Double.valueOf(m.group()));
		}
		return numbers;
	}

	public static String getUnitFromStringValue(String string)
	{
		Pattern p = Pattern.compile("[a-zA-Z%]+");
		Matcher m = p.matcher(string);
		String returnValue = "";
		while (m.find())
		{
			returnValue = m.group();
		}
		return returnValue;
	}

	public static double convertNanosecondsToSeconds(long time)
	{
		return (double) time / 1000000000.0;
	}

	public static void applyNewValues(Chromosome chromosome)
	{
		WebDriverSingleton.loadPage(MFix.getFilepath());
		WebDriver d = WebDriverSingleton.getDriver();
		
		HtmlDomTree instance2 = HtmlDomTree.getInstance(MFix.getFilepath());
		for (Gene gene : chromosome.getGenes()) 
		{
			System.out.println("\nApplying values for gene " + gene);
			String javascriptCode = "";
			int i = 0;
			List<WebElement> webElements = new ArrayList<>();
			
			// call dependency graph to get elements to modify based on segment
			List<Element> elementsToChange = MFix.getSegmentToDG().get(gene.getSegmentIssueId()).getElementsToChange(gene.getCssProperty(), gene.getValue(), gene.getIssueType());
			
			System.out.println("Elements to change size = " + elementsToChange.size());
			for(Element element : elementsToChange)
			{
				String valueString = element.getValue();
				if(valueString.matches(".*\\d+.*"))
				{
					// if numeric value, round it off
					double value = Util.getNumbersFromString(element.getValue()).get(0);
					valueString = Math.round(value) + "px";
				}
				
				if(instance2.isElementPresent(element.getXpath()))
				{
					System.out.println("Patch: Applying " + element.getCssProperty() + ": " + valueString + " to " + element.getXpath());
					WebElement e = d.findElement(By.xpath(element.getXpath()));
					webElements.add(e);
					javascriptCode = javascriptCode + "arguments[" + i + "].style.setProperty('" + element.getCssProperty() + "', '" + valueString + "', 'important');";
					i++;
				}
			}
			// modify test page with the new values
			((JavascriptExecutor) d).executeScript(javascriptCode, webElements.toArray());
		}

		// a further check to see if any of the leaves of the new dom tree have text that is overflowing their element's bounding box
		HtmlDomTree.resetInstance();
		HtmlDomTree instance = HtmlDomTree.getInstance(MFix.getFilepath());
		instance.buildHtmlDomTree();
		List<HtmlElement> leaves = instance.getLeaves();
		String javascriptCode = "";
		int i = 0;
		List<WebElement> webElements = new ArrayList<>();
		for(HtmlElement leaf : leaves)
		{
			if(leaf.getTextCoords().size() > 0)
			{
				mfix.domTree.Rectangle leafElementRect = leaf.getCoord();
				int minX = leafElementRect.x; 
				int minY = leafElementRect.y;
				int maxX = leafElementRect.x + leafElementRect.width;
				int maxY = leafElementRect.y + leafElementRect.height;
				
				for(mfix.domTree.Rectangle leafTextRect : leaf.getTextCoords())
				{
					minX = Math.min(leafTextRect.x, minX);
					minY = Math.min(leafTextRect.y, minY);
					maxX = Math.max(leafTextRect.x + leafTextRect.width, maxX);
					maxY = Math.max(leafTextRect.y + leafTextRect.height, maxY);
				}
				// adjust height
				if(maxY > (leafElementRect.y + leafElementRect.height))
				{
					WebElement e = d.findElement(By.xpath(leaf.getXpath()));
					webElements.add(e);
					javascriptCode = javascriptCode + "arguments[" + i + "].style.setProperty('height', '" + (maxY - minY) + "px', 'important');";
					i++;
				}
			}
		}
		// modify test page with the new values of height
		if(webElements.size() > 0)
		{
			((JavascriptExecutor) d).executeScript(javascriptCode, webElements.toArray());
		}
	}

	public static String getValueFromElement(WebDriver d, String xpath, String cssProperty)
	{
		if(elementPropValueCache.containsKey(xpath + "#" + cssProperty))
		{
			return elementPropValueCache.get(xpath + "#" + cssProperty);
		}
		
		WebElement e = d.findElement(By.xpath(xpath));
		String val = e.getCssValue(cssProperty);
		if(val.equalsIgnoreCase("auto") || val.equalsIgnoreCase("inherit"))
		{
			if(cssProperty.equalsIgnoreCase("width"))
			{
				val = e.getSize().width + "px";
			}
			else if(cssProperty.equalsIgnoreCase("height"))
			{
				val = e.getSize().height + "px";
			}
		}
		elementPropValueCache.put(xpath + "#" + cssProperty, val);
		return val;
	}

	public static double getRandomDoubleValueInRange(double min, double max)
	{
		Random r = new Random();
		return min + (max - min) * r.nextDouble();
	}

	public static int getRandomIntValueInRange(int min, int max)
	{
		if(max - min == 0)
			return 0;
		
		Random generator = new Random();
		return generator.nextInt(max - min) + min;
	}

	public static double getWeightedAverage(double value1, double value2, double weight)
	{
		return ((weight * value1) + ((1 - weight) * value2));
	}
	
	public static String getSanitizedString(String input)
	{
		return StringEscapeUtils.unescapeHtml4(input);
	}

	public static double round(double value)
	{
		int places = 2;
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	public static Color hex2Rgb(String colorStr)
	{
		return new Color(Integer.valueOf(colorStr.substring(1, 3), 16), Integer.valueOf(colorStr.substring(3, 5), 16), Integer.valueOf(colorStr.substring(5, 7), 16));
	}

	private static Color getRandomColor(List<String> visitedColors)
	{
		Random rand = new Random();
		int cnt = 0;
		while (cnt < 50)
		{
			int r = rand.nextInt(255);
			int g = rand.nextInt(255);
			int b = rand.nextInt(255);
			Color randomColor = new Color(r, g, b);
			String color = String.format("#%02x%02x%02x", r, g, b);
			if (!visitedColors.contains(color))
			{
				visitedColors.add(color);
				return randomColor;
			}
			cnt++;
		}
		return hex2Rgb(visitedColors.get(0));
	}
	public static double getEuclideanDistanceBetweenPoints(int x1, int y1, int x2, int y2)
	{
		return Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2));
	}

	public static void drawClusters(String imagePath, List<List<Rectangle>> clusterRectangles) throws IOException
	{
		List<String> visitedColors = new ArrayList<String>();
		BufferedImage bi = ImageIO.read(new File(imagePath));

		int i = 0;
		Random rand = new Random();
		for (List<Rectangle> rects : clusterRectangles)
		{
			if (rects.size() == 0)
				continue;

			// find cluster minimum bounding rectangle
			int minX, minY, maxX, maxY;
			minX = minY = Integer.MAX_VALUE;
			maxX = maxY = Integer.MIN_VALUE;
			for (Rectangle r : rects)
			{
				minX = Math.min(r.x, minX);
				minY = Math.min(r.y, minY);
				maxX = Math.max((r.x + r.width), maxX);
				maxY = Math.max((r.y + r.height), maxY);
			}
			Rectangle mbr = new Rectangle(minX, minY, (maxX - minX), (maxY - minY));

			// draw minimum bounding rectangle
			Graphics graphics = bi.getGraphics();
			graphics.setColor(Color.RED);
			graphics.setFont(new Font("Arial Black", Font.BOLD, 14));

			int x = mbr.x + 10 + rand.nextInt(11);
			int y = mbr.y + 10 + rand.nextInt(11);
			graphics.drawString("C" + (i + 1), x, y);

			Graphics2D g2D = (Graphics2D) graphics;
			Color color = getRandomColor(visitedColors);
			g2D.setColor(color);
			g2D.setStroke(new BasicStroke(3F));
			g2D.drawRect(mbr.x, mbr.y, mbr.width, mbr.height);

			// draw filled rectangles for individual cluster elements
			float dash[] = { 10.0f };
			g2D.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
			int alpha = 50;
			Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
			g2D.setPaint(c);
			for (Rectangle r : rects)
			{
				g2D.fill(r);
			}

			i++;
		}
		ImageIO.write(bi, "png", new File(imagePath));
	}
	
	public static String getRandomAPIKey()
	{
		int index = getRandomIntValueInRange(0, Constants.GOOGLE_API_KEYS.size());
		return Constants.GOOGLE_API_KEYS.get(index);
	}
	
	public static String getProcessOutput(String command) throws IOException, InterruptedException
	{
		String[] c = command.split(" ");
		ProcessBuilder builder = new ProcessBuilder(c);
		Process p = builder.start();
		p.waitFor();
		BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = input.readLine()) != null)
		{
			sb.append(line);
			sb.append(System.getProperty("line.separator"));
		}
		String result = sb.toString();
		input.close();

		return result;
	}
	public static String getAPIOutputForPostRequest(String httpsURL, String urlParameter) throws IOException
	{
		String json = "{\"requestScreenshot\": true, \"url\": \"" + urlParameter + "\"}";
		
	    URL myurl = new URL(httpsURL);
	    HttpsURLConnection con = (HttpsURLConnection) myurl.openConnection();
	    con.setRequestProperty("Content-Type", "application/json");
	    con.setRequestMethod("POST");
	    con.setDoOutput(true);
	    con.setDoInput(true);
	    
	    OutputStream os = con.getOutputStream();
        os.write(json.getBytes("UTF-8"));
        os.close();
	    
	    InputStream ins = con.getInputStream();
	    InputStreamReader isr = new InputStreamReader(ins);
	    BufferedReader in = new BufferedReader(isr);

	    String inputLine;
	    String result = "";
	    while ((inputLine = in.readLine()) != null)
	    {
	      result = result + inputLine;
	    }
	    in.close();

		return result;
	}
	public static String getAPIOutputForGetRequest(String httpsURL) throws IOException
	{
	    URL myurl = new URL(httpsURL);
	    HttpsURLConnection con = (HttpsURLConnection) myurl.openConnection();
	    InputStream ins = con.getInputStream();
	    InputStreamReader isr = new InputStreamReader(ins);
	    BufferedReader in = new BufferedReader(isr);

	    String inputLine;
	    String result = "";
	    while ((inputLine = in.readLine()) != null)
	    {
	      result = result + inputLine;
	    }
	    in.close();

		return result;
	}
	public static double getGaussianValue(double mean, double min, double max)
	{
		double stddev = (max - min) / 6d;
		Random r = new Random();
		double x1 = r.nextDouble();
		double x2 = r.nextDouble();

		if (x1 == 0)
			x1 = 1;
		if (x2 == 0)
			x2 = 1;

		double y1 = Math.sqrt(-2.0 * Math.log(x1)) * Math.cos(2.0 * Math.PI * x2);
		double val = y1 * stddev + mean;
		
		if (val >= max)
			return max;
		if (val <= min)
			return min;
		return val;
	}
	
	// finding input values for guassian distribution based on issue type
	public static List<Double> generateGaussianInputs(String issue) {
		// list of inputs in this order {mean, min value, max value}
		List<Double> vals = new ArrayList<>();
		double googleVal = 0;
		String str = "";
		if (issue.equals(Constants.CONTENT_SIZE_PROBLEM)) {
			str = Constants.GOOGLE_SUGGESTED_VALUES.get(Constants.CONTENT_SIZE_PROBLEM).get("width");
			googleVal = Util.getNumbersFromString(str).get(0);
			double mean = googleVal - 20;
			vals.add(mean);
			vals.add(mean - 50);
			vals.add(mean + 50);

		} else if(issue.equals(Constants.TAP_TARGET_PROBLEM)){
			
			str = Constants.GOOGLE_SUGGESTED_VALUES.get(Constants.TAP_TARGET_PROBLEM).get("margin");
			googleVal = Util.getNumbersFromString(str).get(0);
			vals.add(googleVal);
			vals.add(googleVal - 8);
			vals.add(googleVal + 8);

			}
		else if(issue.equals(Constants.FONT_SIZE_PROBLEM)){
				str = Constants.GOOGLE_SUGGESTED_VALUES.get(Constants.FONT_SIZE_PROBLEM).get("font-size");
				googleVal = Util.getNumbersFromString(str).get(0); 
				
				double mean = googleVal + 14;
				vals.add(mean);
				vals.add(mean - 15);
				vals.add(mean + 15);
			}
		return vals;
	}
	
	public static String getSegmentIssueId(int segmentId, String issue)
	{
		return "S" + segmentId + "_" + issue;
	}
	
	public static int getSegmentIdFromSegmentIssueId(String segmentIssueId)
	{
		String s = segmentIssueId.replace("S", "").split("_")[0];
		return Integer.parseInt(s);
	}
	
	public static String getInstance(int chromosomeCount)
	{
		int index = chromosomeCount % MFix.getAwsInstances().size();
		return MFix.getAwsInstances().get(index);
	}
	
	public static String getFormattedTimestamp()
	{
		String pattern = "MM-dd-yyyy-hh-mm-ss-a";
		SimpleDateFormat format = new SimpleDateFormat(pattern);
		String timestamp = format.format(new Date());
		return timestamp;
	}
}