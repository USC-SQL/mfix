package mfix.segmentation;

import java.util.ArrayList;
import java.util.List;

import mfix.MFix;
import mfix.Util;
import mfix.domTree.HtmlDomTree;
import mfix.domTree.HtmlElement;
import mfix.domTree.Rectangle;
import mfix.fitness.SpatialRelationships;

public class SegmentModel
{
	private List<InterSegmentEdge> edges;	// complete graph
	private Segmentation segmentationObject;

	public SegmentModel()
	{
		this.edges = new ArrayList<>();
	}
	
	public SegmentModel(List<InterSegmentEdge> edges)
	{
		this.edges = edges;
	}

	public List<InterSegmentEdge> getEdges()
	{
		return edges;
	}

	public void setEdges(List<InterSegmentEdge> edges)
	{
		this.edges = edges;
	}

	public void buildSegmentModel()
	{
		runSegmentation();
		
		// iterate over segments and create intrasegment model within the members
		long startTime = System.nanoTime();
		for(Segment s : segmentationObject.getSegments())
		{
			s.calculateEdges();
			System.out.println("\nIntra-segment edges for segment S" + s.getId());
			System.out.println(s.getEdges());
		}
		MFix.setOriginalPageSegments(segmentationObject.getSegments());
		long endTime = System.nanoTime();
		System.out.println("Total intrasegment calculation time = " + Util.convertNanosecondsToSeconds((endTime - startTime)) + " sec");
		
		startTime = System.nanoTime();
		this.edges = calculateEdges(segmentationObject.getSegments());
		addEdgeLabels(edges);
		endTime = System.nanoTime();
		System.out.println("Total intersegment calculation time = " + Util.convertNanosecondsToSeconds((endTime - startTime)) + " sec");
	}
	
	public void runSegmentation()
	{
		long startTime = System.nanoTime();
		HtmlDomTree domTree = HtmlDomTree.getInstance(MFix.getFilepath());
		List<HtmlElement> leafElements = domTree.getLeaves();
		System.out.println("\nLeaves (size = " + leafElements.size() + "): ");
		for(HtmlElement leaf : leafElements)
		{
			System.out.println(leaf.getXpath());
		}
		
		segmentationObject = new Segmentation();
		segmentationObject.performSegmentation(leafElements);
		
		System.out.println("\nSegment terminate threshold value = " + segmentationObject.getSegmentTerminateThreshold());
		System.out.println("Segments (size = " + segmentationObject.getSegments().size() + "): ");
		for(Segment seg : segmentationObject.getSegments())
		{
			System.out.println(seg);
		}
		long endTime = System.nanoTime();
		System.out.println("Total segmentation time = " + Util.convertNanosecondsToSeconds((endTime - startTime)) + " sec");
	}
	
	public List<InterSegmentEdge> calculateEdges(List<Segment> segments)
	{
		// for every segment pair add an edge and add to segment model => complete graph
		List<InterSegmentEdge> edges = new ArrayList<>();
		for (int i = 0; i < segments.size(); i++)
		{
			for (int j = i+1; j < segments.size(); j++)
			{
				Segment segment1 = segments.get(i);
				Segment segment2 = segments.get(j);
				InterSegmentEdge e = new InterSegmentEdge(segment1, segment2);
				edges.add(e);
			}
		}
		return edges;
	}
	
	private void addEdgeLabels(List<InterSegmentEdge> edges)
	{
		for(InterSegmentEdge e : edges)
		{
			List<EdgeLabel> labels = calculateEdgeLabels(e.getSegment1(), e.getSegment2());
			e.setLabels(labels);
		}
	}
	
	public List<EdgeLabel> calculateEdgeLabels(Segment segment1, Segment segment2)
	{
		List<EdgeLabel> labels = new ArrayList<EdgeLabel>();
		SpatialRelationships sr = new SpatialRelationships();
		
		// check containment
		if(segment1.getId() != 0 && segment2.getId() != 0)
		{
			if(sr.isContained(segment1.getMinimumBoundingRectangle(), segment2.getMinimumBoundingRectangle()))
			{
				labels.add(EdgeLabel.CONTAINED_BY);
			}
			else if(sr.isContained(segment2.getMinimumBoundingRectangle(), segment1.getMinimumBoundingRectangle()))
			{
				labels.add(EdgeLabel.CONTAINS);
			}
		}
		
		// check intersection
		else if(sr.isIntersection(segment1.getMinimumBoundingRectangle(), segment2.getMinimumBoundingRectangle()))
		{
			labels.add(EdgeLabel.INTERSECTION);
		}
		
		// check directions
		if(sr.isDirectionAbove(segment1.getMinimumBoundingRectangle(), segment2.getMinimumBoundingRectangle()))
		{
			labels.add(EdgeLabel.ABOVE);
		}
		if(sr.isDirectionBelow(segment1.getMinimumBoundingRectangle(), segment2.getMinimumBoundingRectangle()))
		{
			labels.add(EdgeLabel.BELOW);
		}
		if(sr.isDirectionLeft(segment1.getMinimumBoundingRectangle(), segment2.getMinimumBoundingRectangle()))
		{
			labels.add(EdgeLabel.LEFT);
		}
		if(sr.isDirectionRight(segment1.getMinimumBoundingRectangle(), segment2.getMinimumBoundingRectangle()))
		{
			labels.add(EdgeLabel.RIGHT);
		}
		
		return labels;
	}
	
	public SegmentModel getUpdatedSegmentModel()
	{
		// copy segment model
		SegmentModel segmentModelUpdated = this.copy();
		
		// update segments with new MBRs
		for(InterSegmentEdge e : segmentModelUpdated.getEdges())
		{
			Rectangle mbr1 = Segmentation.getSegmentMBR(e.getSegment1());
			e.getSegment1().setMinimumBoundingRectangle(mbr1);
			
			Rectangle mbr2 = Segmentation.getSegmentMBR(e.getSegment2());
			e.getSegment2().setMinimumBoundingRectangle(mbr2);
		}
		
		// recalculate edges
		addEdgeLabels(segmentModelUpdated.edges);
		
		return segmentModelUpdated;
	}

	public List<InterSegmentEdge> compareToSegmentModel(SegmentModel otherSegmentModel)
	{
		List<InterSegmentEdge> violatedEdges = new ArrayList<>();
		
		// compare the edge labels, and report violations
		for(int i = 0; i < edges.size(); i++)
		{
			List<EdgeLabel> e1Labels = this.edges.get(i).getLabels();
			List<EdgeLabel> e2Labels = otherSegmentModel.getEdges().get(i).getLabels();
			
			// remove all elements in e2Labels from e1Labels => set difference
			List<EdgeLabel> intersectionList = new ArrayList<EdgeLabel>(e1Labels);
			intersectionList.retainAll(e2Labels);
			e1Labels.removeAll(intersectionList);
			e2Labels.removeAll(intersectionList);
			
			List<EdgeLabel> violatedLabels = new ArrayList<>();
			violatedLabels.addAll(e1Labels);
			violatedLabels.addAll(e2Labels);
			
			if(violatedLabels.size() > 0)
			{
				violatedEdges.add(new InterSegmentEdge(this.edges.get(i).getSegment1(), this.edges.get(i).getSegment2(), violatedLabels));
			}
		}
		
		return violatedEdges;
	}
	
	public SegmentModel copy()
	{
		SegmentModel copySegmentModel = new SegmentModel();
		List<InterSegmentEdge> copyEdges = new ArrayList<InterSegmentEdge>();
		for(InterSegmentEdge e : this.edges)
		{
			InterSegmentEdge copyE = e.copy();
			copyEdges.add(copyE);
		}
		copySegmentModel.setEdges(copyEdges);
		
		return copySegmentModel;
	}
	
	@Override
	public String toString()
	{
		String ret = "\n{";
		for(InterSegmentEdge e : edges)
		{
			ret = ret + e + "\n";
		}
		int endIndex = ret.lastIndexOf("\n");
		ret = ret.substring(0, endIndex);
		ret = ret + "}";
		return ret;
	}
}
