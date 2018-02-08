package mfix.fitness;

public class Coordinate
{
	private String left;
	private String top;
	private String width;
	private String height;
	private String cssProperty;
	private String value;

	public Coordinate(String left, String top, String width, String height)
	{
		this.left = left;
		this.width = width;
		this.top = top;
		this.height = height;
	}

	public Coordinate(String left, String top, String width, String height, String prop, String value)
	{
		this.left = left;
		this.width = width;
		this.top = top;
		this.height = height;
		cssProperty = prop;
		this.value = value;
	}

	public void setProperty(String prop)
	{
		cssProperty = prop;
	}

	public String getProperty()
	{
		return cssProperty;
	}

	public String getTop()
	{
		return top;
	}

	public String getLeft()
	{
		return left;
	}

	public String getWidth()
	{
		return width;
	}

	public String getHeight()
	{
		return height;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}
}
