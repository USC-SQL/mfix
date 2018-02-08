package mfix.segmentation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;

import mfix.Constants;
import mfix.MFix;
import mfix.WebDriverSingleton;
import mfix.domTree.HtmlDomTree;
import mfix.domTree.HtmlElement;
import mfix.domTree.Rectangle;

public class Segmentation
{
	private double segmentTerminateThreshold;
	
	private List<Segment> segments;
	private List<HtmlElement> leaves;
	private static Rectangle viewportRectangle;
	
	public List<Segment> getSegments()
	{
		return segments;
	}
	public List<HtmlElement> getLeaves()
	{
		return leaves;
	}
	public void setLeaves(List<HtmlElement> leaves)
	{
		this.leaves = leaves;
	}
	public static Rectangle getViewportRectangle()
	{
		return viewportRectangle;
	}
	public static void setViewportRectangle(Rectangle viewportRectangle)
	{
		Segmentation.viewportRectangle = viewportRectangle;
	}
	public void setSegments(List<Segment> segments)
	{
		this.segments = segments;
	}

	public double getSegmentTerminateThreshold() {
		return segmentTerminateThreshold;
	}
	public void setSegmentTerminateThreshold(double segmentTerminateThreshold) {
		this.segmentTerminateThreshold = segmentTerminateThreshold;
	}
	public void calculateSegmentThresholdValue()
	{
		segmentTerminateThreshold = Constants.SEGMENT_TERMINATE_THRESHOLD;
		
		// strategy 1: average leaf depth / 2
		int avgLeafDepth = 0;
		for(HtmlElement leaf : leaves)
		{
			String xpathArray[] = leaf.getXpath().split("/");
			avgLeafDepth = avgLeafDepth + (xpathArray.length-1);
		}
		avgLeafDepth = avgLeafDepth / leaves.size();
		double newValue = avgLeafDepth / 2;
		segmentTerminateThreshold = Math.min(segmentTerminateThreshold, newValue);
		
		// strategy 2: max leaf depth / 2
		/*int maxLeafDepth = Integer.MIN_VALUE;
		for(HtmlElement leaf : leaves)
		{
			String xpathArray[] = leaf.getXpath().split("/");
			if((xpathArray.length-1) > maxLeafDepth)
			{
				maxLeafDepth = xpathArray.length-1;
			}
		}
		SEGMENT_TERMINATE_THRESHOLD = maxLeafDepth / 2;*/
	}
	
	public void performSegmentation(List<HtmlElement> leaves)
	{
		this.leaves = leaves; 
		segments = new ArrayList<Segment>();
		
		// set dynamic value for segment termination threshold
		calculateSegmentThresholdValue();
		
		// Assign each leaf to its own segment
		int count = 1;
		for(HtmlElement leaf : leaves)
		{
			List<String> segmentMembers = new ArrayList<>();
			segmentMembers.add(leaf.getXpath());
			Segment s = new Segment(count, segmentMembers, leaf.getXpath());
			segments.add(s);
			count++;
		}
		
		while (true)
		{
			// Compute the cost C(x,y) of merging each adjacent pair (x,y) of segments 
			double minCost = Double.MAX_VALUE;
			Segment xMin = null;
			Segment yMin = null;
			for(int i = 0, j = i+1; i < segments.size() && j < segments.size(); i++, j++)
			{
				Segment x = segments.get(i);
				Segment y = segments.get(j);
				
				double costXY = getCost(x.getLowestCommonAncestor(), y.getLowestCommonAncestor());
				
				// Locate the x=x* and y=y* for which C(x,y) is minimal
				if(costXY < minCost)
				{
					minCost = costXY;
					xMin = x;
					yMin = y;
				}
			}
			
			// If C(x,y) = ∞ for all x,y then end
			if(minCost == Double.MAX_VALUE)
			{
				break;
			}
			
			// Merge segments x* and y*
			mergeSegments(xMin, yMin);
		}
		
		// update segment ids in a sequential order
		int cnt = Constants.VIEWPORT_SEGMENT_ID + 1;
		for(Segment s : segments)
		{
			s.setId(cnt);
			cnt++;
		}
		
		// update segments with MBRs
		for(Segment s : segments)
		{
			s.setMinimumBoundingRectangle(getSegmentMBR(s));
		}
		
		// add a "ghost" segment for the viewport
		Segment segGhost = new Segment();
		segGhost.setId(Constants.VIEWPORT_SEGMENT_ID);
		segGhost.setMinimumBoundingRectangle(viewportRectangle);
		segments.add(segGhost);
		
		// draw segments
		String imageFilepath = MFix.getOutputFolderPath() + File.separatorChar + "index-segmentation-" + segmentTerminateThreshold + "-screenshot.png";
		WebDriverSingleton.takeScreenshot(imageFilepath);
		try
		{
			drawSegments(imageFilepath);
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
	}
	
	private void mergeSegments(Segment x, Segment y)
	{
		// copy member of segment y to segment x
		x.addMembers(y.getMembers());
		
		// update segment xpath of x
		String newXpath = getLowestCommonAncestor(x.getMembers());
		x.setLowestCommonAncestor(newXpath);
		
		// remove y from segments
		segments.remove(y);
	}
	
	private String getLowestCommonAncestor(List<String> xpaths)
	{
		if(xpaths.size() == 0)
		{
			return "";
		}
		
		String lowestCommonSubstring = "";
		List<String> xpathsList = new ArrayList<>(xpaths);	// to allow get by index
		String xpath1Array[] = xpathsList.get(0).split("/");
		
		for (int i = 1; i < xpath1Array.length; i++)	// xpath1Array[0] = ""
		{
			for(int j = 1; j < xpathsList.size(); j++)
			{
				String xpath2Array[] = xpathsList.get(j).split("/");
				if(i >= xpath2Array.length)
				{
					return lowestCommonSubstring;
				}
				
				if(!xpath1Array[i].equalsIgnoreCase(xpath2Array[i]))
				{
					return lowestCommonSubstring;
				}
			}
			lowestCommonSubstring = lowestCommonSubstring + "/" + xpath1Array[i];
		}
		return lowestCommonSubstring;
	}
	
	private double getCost(String xpath1, String xpath2)
	{
		// DOM distance between two nodes
		String xpath1Array[] = xpath1.split("/");
		String xpath2Array[] = xpath2.split("/");
		int expectedLength = xpath1Array.length - 1;
		int actualLength = xpath2Array.length - 1;
		int distance;

		int matchingCount = 0;
		for (int i = 1; i < xpath1Array.length && i < xpath2Array.length; i++)
		{
			if (xpath1Array[i].equals(xpath2Array[i]))
			{
				matchingCount++;
			}
			else
			{
				break;
			}
		}

		distance = (actualLength - matchingCount) + (expectedLength - matchingCount);

		if(distance > segmentTerminateThreshold)
		{
			return Double.MAX_VALUE;	// infinity
		}
		
		return distance;
	}
	
	public static Rectangle getSegmentMBR(Segment seg)
	{
		if(seg.getMembers().size() == 0)
		{
			return seg.getMinimumBoundingRectangle();
		}
		
		int minX, minY, maxX, maxY;
		minX = minY = Integer.MAX_VALUE;
		maxX = maxY = Integer.MIN_VALUE;
		HtmlDomTree domTree = HtmlDomTree.getInstance(MFix.getFilepath());
		for (String xpath : seg.getMembers())
		{
			HtmlElement leaf = domTree.searchHtmlDomTreeByXpath(xpath).getData();

			// check if leaf is a text node
			if (leaf.getTextCoords().size() > 0)
			{
				// iterate over all text bounding boxes
				for (mfix.domTree.Rectangle r : leaf.getTextCoords())
				{
					minX = Math.min(r.x, minX);
					minY = Math.min(r.y, minY);
					maxX = Math.max(r.x + r.width, maxX);
					maxY = Math.max(r.y + r.height, maxY);
				}
			}
			else
			{
				minX = Math.min(leaf.getCoord().x, minX);
				minY = Math.min(leaf.getCoord().y, minY);
				maxX = Math.max(leaf.getCoord().x + leaf.getCoord().width, maxX);
				maxY = Math.max(leaf.getCoord().y + leaf.getCoord().height, maxY);
			}
		}
		Rectangle mbr = new Rectangle(minX, minY, (maxX - minX), (maxY - minY));
		return mbr;
	}
	
	public Map<String, List<Rectangle>> getSegmentRectangles()
	{
		Map<String, List<Rectangle>> segmentRectangles = new HashMap<String, List<Rectangle>>();
		
		for(Segment s : segments)
		{
			if(s.getId() == 0)
				continue;
			
			List<Rectangle> members = new ArrayList<>();
			List<Rectangle> membersTemp = new ArrayList<>();
			int minX, minY, maxX, maxY;
			minX = minY = Integer.MAX_VALUE;
			maxX = maxY = Integer.MIN_VALUE;
			for(String xpath : s.getMembers())
			{
				for(HtmlElement leaf : leaves)
				{
					if(leaf.getXpath().equalsIgnoreCase(xpath))
					{
						membersTemp.add(new Rectangle(leaf.getCoord().x, leaf.getCoord().y, leaf.getCoord().width, leaf.getCoord().height));
						minX = Math.min(leaf.getCoord().x, minX);
						minY = Math.min(leaf.getCoord().y, minY);
						maxX = Math.max(leaf.getCoord().x + leaf.getCoord().width, maxX);
						maxY = Math.max(leaf.getCoord().y + leaf.getCoord().height, maxY);
						break;
					}
				}
			}
			Rectangle mbr = new Rectangle(minX, minY, (maxX - minX), (maxY - minY));
			if(s.getMembers().size() == 0)
			{
				mbr = s.getMinimumBoundingRectangle();
			}
			
			s.setMinimumBoundingRectangle(mbr);
			members.add(mbr);
			members.addAll(membersTemp);
			segmentRectangles.put("S" + s.getId(), members);
		}
		
		return segmentRectangles;
	}
	
	public Color hex2Rgb(String colorStr) {
	    return new Color(
	            Integer.valueOf( colorStr.substring( 1, 3 ), 16 ),
	            Integer.valueOf( colorStr.substring( 3, 5 ), 16 ),
	            Integer.valueOf( colorStr.substring( 5, 7 ), 16 ) );
	}
	
	private Color getRandomColor(List<String> visitedColors)
	{
		Random rand = new Random();
		int cnt = 0;
		while(cnt < 50)
		{
			int r = rand.nextInt(255);
			int g = rand.nextInt(255);
			int b = rand.nextInt(255);
			Color randomColor = new Color(r, g, b);
			String color = String.format("#%02x%02x%02x", r, g, b);
			if(!visitedColors.contains(color))
			{
				visitedColors.add(color);
				return randomColor;
			}
			cnt++;
		}
		return hex2Rgb(visitedColors.get(0));
	}
	
	public void drawSegments(String imagePath) throws IOException
	{
		Map<String, List<Rectangle>> segmentRectangles = getSegmentRectangles();
		
		List<String> visitedColors = new ArrayList<String>();
		
		// clusterRectangles first rect: outermost cluster rect
		// other rectangles: cluster elements
		
		BufferedImage bi = ImageIO.read(new File(imagePath));

		Random rand = new Random();
		for (String sId : segmentRectangles.keySet())
		{
			List<Rectangle> rects = segmentRectangles.get(sId);
			if(rects.size() == 0)
				continue;
			
			Graphics graphics = bi.getGraphics();
			graphics.setColor(Color.RED);
			graphics.setFont(new Font("Arial Black", Font.BOLD, 14));
			
			Rectangle rect = rects.get(0);
			int x = rect.x + 10 + rand.nextInt(11);
			int y = rect.y + 10 + rand.nextInt(11);
			graphics.drawString(sId, x, y);

			Graphics2D g2D = (Graphics2D) graphics;
			Color color = getRandomColor(visitedColors);
			g2D.setColor(color);
			g2D.setStroke(new BasicStroke(3F));
			g2D.drawRect(rect.x, rect.y, rect.width, rect.height);

			if(rects.size() > 1)
			{
				// draw dashed rectangles around individual cluster elements
				float dash[] = { 10.0f };
				g2D.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
				int alpha = 50;
				Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
				g2D.setPaint(c);
				for(int m = 1; m < rects.size(); m++)
				{
					Rectangle localRect = rects.get(m);
					java.awt.Rectangle r = new java.awt.Rectangle(localRect.x, localRect.y, localRect.width, localRect.height);
					g2D.fill(r);
				}
			}
		}
		ImageIO.write(bi, "png", new File(imagePath));
	}
}
