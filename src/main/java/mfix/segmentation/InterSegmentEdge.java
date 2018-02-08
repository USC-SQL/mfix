package mfix.segmentation;

import java.util.ArrayList;
import java.util.List;

public class InterSegmentEdge
{
	private Segment segment1;
	private Segment segment2;
	private List<EdgeLabel> labels;	// label from segment1 to segment2
	
	public InterSegmentEdge()
	{
		this.labels = new ArrayList<>();
	}
	
	public InterSegmentEdge(Segment segment1, Segment segment2)
	{
		this.segment1 = segment1;
		this.segment2 = segment2;
		this.labels = new ArrayList<>();
	}
	
	public InterSegmentEdge(Segment segment1, Segment segment2, List<EdgeLabel> labels)
	{
		this.segment1 = segment1;
		this.segment2 = segment2;
		this.labels = labels;
	}
	public Segment getSegment1()
	{
		return segment1;
	}
	public void setSegment1(Segment segment1)
	{
		this.segment1 = segment1;
	}
	public Segment getSegment2()
	{
		return segment2;
	}
	public void setSegment2(Segment segment2)
	{
		this.segment2 = segment2;
	}
	public List<EdgeLabel> getLabels()
	{
		return labels;
	}
	public void setLabels(List<EdgeLabel> labels)
	{
		this.labels = labels;
	}

	public InterSegmentEdge copy()
	{
		InterSegmentEdge e = new InterSegmentEdge();
		e.setSegment1(segment1.copy());
		e.setSegment2(segment2.copy());
		e.setLabels(new ArrayList<>(labels));
		return e;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((segment1 == null) ? 0 : segment1.hashCode());
		result = prime * result + ((segment2 == null) ? 0 : segment2.hashCode());
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
		InterSegmentEdge other = (InterSegmentEdge) obj;
		if (segment1 == null)
		{
			if (other.segment1 != null)
				return false;
		}
		else if (!segment1.equals(other.segment1))
			return false;
		if (segment2 == null)
		{
			if (other.segment2 != null)
				return false;
		}
		else if (!segment2.equals(other.segment2))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "<" + segment1.getId() + ", " + segment2.getId() + ", " + labels + ">";
	}
}
