package mfix.fitness;

import mfix.domTree.Rectangle;

public class SpatialRelationships 
{
	public boolean isPointInRectangle(int x, int y, Rectangle r, boolean isBorderIncluded)
	{
		if (isBorderIncluded)
		{
			if (x >= r.x && y >= r.y && x <= (r.x + r.width) && y <= (r.y + r.height))
				return true;
		}
		else
		{
			if (x > r.x && y > r.y && x < (r.x + r.width) && y < (r.y + r.height))
				return true;
		}
		return false;
	}
	
	public boolean isIntersection(Rectangle r1, Rectangle r2)
	{
		// r1 intersects r2
		int r1X1 = r1.x;
		int r1Y1 = r1.y;
		int r1X2 = r1.x + r1.width;
		int r1Y2 = r1.y + r1.height;
		int r2X1 = r2.x;
		int r2Y1 = r2.y;
		int r2X2 = r2.x + r2.width;
		int r2Y2 = r2.y + r2.height;
		
		// check if there is no containment
		if(isContained(r1, r2) || isContained(r2, r1))
			return false;
		
		if (r1X1 < r2X2 && r1X2 > r2X1 && r1Y1 < r2Y2 && r1Y2 > r2Y1) 
			return true;
		else
			return false;
	}
	
	public boolean isContained(Rectangle r1, Rectangle r2)
	{
		// check all corners of r1 are inside r2 => r1 is contained by r2
		boolean leftTop = isPointInRectangle(r1.x, r1.y, r2, true);
		boolean rightTop = isPointInRectangle((r1.x + r1.width), r1.y, r2, true);
		boolean leftBottom = isPointInRectangle(r1.x, (r1.y + r1.height), r2, true);
		boolean rightBottom = isPointInRectangle((r1.x + r1.width), (r1.y + r1.height), r2, true);
		
		return (leftTop && rightTop && leftBottom && rightBottom);
	}
	
	public boolean isDirectionAbove(Rectangle r1, Rectangle r2)
	{
		// check if y2 of r1 is less than (above) y1 of r2 => r1 is above r2
		if ((r1.y + r1.height) < r2.y) 
		{
			return true;
		}
		return false;
	}
	
	public boolean isDirectionBelow(Rectangle r1, Rectangle r2)
	{
		// check if y1 of r1 is more than (below) y2 of r2 => r1 is below r2
		if (r1.y > (r2.y + r2.height)) 
		{
			return true;
		}
		return false;
	}
	
	public boolean isDirectionLeft(Rectangle r1, Rectangle r2)
	{
		// check if x2 of r1 is less than (left) x1 of r2 => r1 is left of r2
		if ((r1.x + r1.width) < r2.x) 
		{
			return true;
		}
		return false;
	}
	
	public boolean isDirectionRight(Rectangle r1, Rectangle r2)
	{
		// check if x1 of r1 is more than (right) x2 of r2 => r1 is right of r2
		if (r1.x > (r2.x + r2.width)) 
		{
			return true;
		}
		return false;
	}
}
