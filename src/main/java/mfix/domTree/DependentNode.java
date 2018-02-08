package mfix.domTree;

public class DependentNode
{
	private String xpath;
	private String property;
	private double ratio;
	private double numerator;
	private double denominator;

	public DependentNode(String xpath, String property, double ratio, double numerator, double denominator)
	{
		this.xpath = xpath;
		this.property = property;
		this.ratio = ratio;
		this.numerator = numerator;
		this.denominator = denominator;
	}

	public String getXpath()
	{
		return xpath;
	}

	public void setXpath(String xpath)
	{
		this.xpath = xpath;
	}

	public double getRatio()
	{
		return ratio;
	}

	public void setRatio(double ratio)
	{
		this.ratio = ratio;
	}
	
	public String getProperty()
	{
		return property;
	}

	public void setProperty(String property)
	{
		this.property = property;
	}
	
	public double getNumerator()
	{
		return numerator;
	}

	public void setNumerator(double numerator)
	{
		this.numerator = numerator;
	}

	public double getDenominator()
	{
		return denominator;
	}

	public void setDenominator(double denominator)
	{
		this.denominator = denominator;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((property == null) ? 0 : property.hashCode());
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
		DependentNode other = (DependentNode) obj;
		if (property == null)
		{
			if (other.property != null)
				return false;
		}
		else if (!property.equals(other.property))
			return false;
		if (xpath == null)
		{
			if (other.xpath != null)
				return false;
		}
		else if (!xpath.equals(other.xpath))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "<" + xpath + ", " + property + ", (" + numerator + "/" + denominator + ") = " + ratio + ">";
	}
}
