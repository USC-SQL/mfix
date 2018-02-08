package mfix.segmentation;

import java.util.ArrayList;
import java.util.List;

import mfix.domTree.HtmlElement;

public class IntraSegmentEdge
{
	private HtmlElement e1;
	private HtmlElement e2;
	private List<EdgeLabel> labels;	// label from e1 to e2
	
	public IntraSegmentEdge()
	{
		this.labels = new ArrayList<>();
	}
	
	public IntraSegmentEdge(HtmlElement e1, HtmlElement e2)
	{
		this.e1 = e1;
		this.e2 = e2;
		this.labels = new ArrayList<>();
	}
	
	public IntraSegmentEdge(HtmlElement e1, HtmlElement e2, List<EdgeLabel> labels)
	{
		this.e1 = e1;
		this.e2 = e2;
		this.labels = labels;
	}
	public HtmlElement getE1()
	{
		return e1;
	}

	public void setE1(HtmlElement e1)
	{
		this.e1 = e1;
	}

	public HtmlElement getE2()
	{
		return e2;
	}

	public void setE2(HtmlElement e2)
	{
		this.e2 = e2;
	}

	public List<EdgeLabel> getLabels()
	{
		return labels;
	}
	public void setLabels(List<EdgeLabel> labels)
	{
		this.labels = labels;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((e1 == null) ? 0 : e1.hashCode());
		result = prime * result + ((e2 == null) ? 0 : e2.hashCode());
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
		IntraSegmentEdge other = (IntraSegmentEdge) obj;
		if (e1 == null)
		{
			if (other.e1 != null)
				return false;
		}
		else if (!e1.equals(other.e1))
			return false;
		if (e2 == null)
		{
			if (other.e2 != null)
				return false;
		}
		else if (!e2.equals(other.e2))
			return false;
		return true;
	}

	public IntraSegmentEdge copy()
	{
		IntraSegmentEdge copyEdge = new IntraSegmentEdge();
		copyEdge.e1 = e1.copy();
		copyEdge.e2 = e2.copy();
		copyEdge.labels = new ArrayList<>(labels);
		return copyEdge;
	}

	@Override
	public String toString()
	{
		return "<" + e1.getXpath() + ", " + e2.getXpath() + ", " + labels + ">";
	}
}
