package mfix.approxAlgo;

import java.util.ArrayList;

import mfix.Constants;


public class Gene
{
	// root cause
	private String segmentIssueId;
	private ArrayList<String> xpaths;
	private String cssProperty;
	private String issueType;
	private String value;
	private String segmentID;
	private String originalValue;
	private double impactScore;
	private boolean isProcessed = false;
	
	public Gene(){
		this.xpaths = new ArrayList<>();
	}
	public Gene(ArrayList<String> xpaths, String cssProperty, String issue, String value, String segmentIssueId)
	{
		this.segmentIssueId = segmentIssueId;
		this.xpaths = new ArrayList<>();
		this.issueType = issue;
		for (String xpath : xpaths)
		{
			addXpath(xpath);
		}

		this.cssProperty = cssProperty;
		if (value.equals(""))
		{
			this.value = Constants.GOOGLE_SUGGESTED_VALUES.get(issueType).get(cssProperty);
		}
		else if (value.equalsIgnoreCase("auto") || value.equalsIgnoreCase("none") || value.equalsIgnoreCase("normal"))
		{
			// set some default value, 0px
			this.value = "0px";// Constants.GOOGLE_SUGGESTED_VALUES.get(cssProperty);
		}
		else
		{
			this.value = value;
		}
	}

	public Gene(String xpath, String cssProperty, String issue, String value)
	{
		this.xpaths = new ArrayList<>();
		xpaths.add(xpath);
		this.cssProperty = cssProperty;
		this.issueType = issue;
		if (value.equals(""))
		{
			this.value = Constants.GOOGLE_SUGGESTED_VALUES.get(issueType).get(cssProperty);
		}
		else if (value.equalsIgnoreCase("auto") || value.equalsIgnoreCase("none") || value.equalsIgnoreCase("normal"))
		{
			// set some default value, 0px
			this.value = "0px";// Constants.GOOGLE_SUGGESTED_VALUES.get(cssProperty);
		}
		else
		{
			this.value = value;
		}
	}

	public ArrayList<String> getXpaths()
	{
		return xpaths;
	}

	public void addXpath(String xpath)
	{
		if (!xpaths.contains(xpath))
		{
			xpaths.add(xpath);
		}
	}

	public String getCssProperty()
	{
		return cssProperty;
	}

	public void setCssProperty(String cssProperty)
	{
		this.cssProperty = cssProperty;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}
	
	public double getImpactScore()
	{
		return impactScore;
	}

	public void setImpactScore(double impactScore)
	{
		this.impactScore = impactScore;
	}
	
	public String getOriginalValue()
	{
		return originalValue;
	}

	public void setOriginalValue(String originalValue)
	{
		this.originalValue = originalValue;
	}
	public String getIssueType()
	{
		return issueType;
	}

	public void setIssueType(String issueType)
	{
		this.issueType = issueType;
	}

	public Gene copy()
	{
		Gene copiedGAGene = new Gene(this.xpaths, this.cssProperty, this.issueType, this.value, this.segmentIssueId);
		copiedGAGene.setOriginalValue(originalValue);
		copiedGAGene.setSegmentID(segmentID);
		copiedGAGene.setImpactScore(impactScore);
		return copiedGAGene;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((segmentIssueId == null) ? 0 : segmentIssueId.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Gene other = (Gene) obj;
		if (segmentIssueId == null) {
			if (other.segmentIssueId != null)
				return false;
		} else if (!segmentIssueId.equals(other.segmentIssueId))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	@Override
	public String toString()
	{
		return "<" + segmentIssueId + ", " + cssProperty + ", " + value + ">";
	}
	public String getSegmentID() {
		return segmentID;
	}
	public void setSegmentID(String segmentID) {
		this.segmentID = segmentID;
	}
	public boolean isProcessed()
	{
		return isProcessed;
	}

	public void setProcessed(boolean isProcessed)
	{
		this.isProcessed = isProcessed;
	}
	public String getSegmentIssueId()
	{
		return segmentIssueId;
	}
	public void setSegmentIssueId(String segmentIssueId)
	{
		this.segmentIssueId = segmentIssueId;
	}
	public void setXpaths(ArrayList<String> xpaths)
	{
		this.xpaths = xpaths;
	}
}