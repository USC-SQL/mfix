package mfix.domTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HtmlElement
{
	private String xpath;
	private transient String tagName;
	private transient String id;
	private Rectangle coord;
	private transient List<String> textContent;
	private List<Rectangle> textCoords;
	private boolean isVisible;
	private transient Map<String, String> attributes;
	private transient Map<String, String> cssMap;

	public String getXpath()
	{
		return xpath;
	}

	public void setXpath(String xpath)
	{
		this.xpath = xpath;
	}

	public String getTagName()
	{
		return tagName;
	}

	public void setTagName(String tagName)
	{
		this.tagName = tagName;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public Rectangle getCoord()
	{
		return coord;
	}

	public void setCoord(Rectangle coord)
	{
		this.coord = coord;
	}

	public boolean isVisible()
	{
		return isVisible;
	}

	public void setVisible(boolean isVisible)
	{
		this.isVisible = isVisible;
	}

	public Map<String, String> getAttributes()
	{
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes)
	{
		this.attributes = attributes;
	}

	public Map<String, String> getCssMap()
	{
		return cssMap;
	}

	public void setCssMap(Map<String, String> cssMap)
	{
		this.cssMap = cssMap;
	}

	public List<Rectangle> getTextCoords()
	{
		return textCoords;
	}

	public void setTextCoords(List<Rectangle> textCoords)
	{
		this.textCoords = textCoords;
	}
	
	public List<String> getTextContent()
	{
		return textContent;
	}

	public void setTextContent(List<String> textContent)
	{
		this.textContent = textContent;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((xpath == null) ? 0 : xpath.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HtmlElement other = (HtmlElement) obj;
		if (xpath == null)
		{
			if (other.xpath != null)
				return false;
		}
		else if (!xpath.equals(other.xpath))
			return false;
		return true;
	}

	public HtmlElement copy()
	{
		HtmlElement copy = new HtmlElement();
		copy.xpath = xpath;
		copy.tagName = tagName;
		copy.id = id;
		copy.coord = new Rectangle(coord);
		copy.textCoords = new ArrayList<Rectangle>(textCoords);
		copy.isVisible = isVisible;
		if(textContent != null)
		{
			copy.textContent = new ArrayList<String>(textContent);
		}
		if(attributes != null)
		{
			copy.attributes = new HashMap<>(attributes);
		}
		if(cssMap != null)
		{
			copy.cssMap = new HashMap<>(cssMap);
		}
		return copy;
	}
	
	@Override
	public String toString()
	{
		return "HtmlElement [xpath=" + xpath + ", tagName=" + tagName + ", id=" + id + ", coord=" + coord + ", textCoords=" + textCoords + ", isVisible=" + isVisible + ", attributes=" + attributes + ", cssMap=" + cssMap + "]";
	}
}
