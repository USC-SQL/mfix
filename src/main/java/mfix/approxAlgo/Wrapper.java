package mfix.approxAlgo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import mfix.Constants;
import mfix.MFix;
import mfix.Util;
import mfix.domTree.DependencyGraph;
import mfix.domTree.HtmlDomTree;
import mfix.domTree.HtmlElement;
import mfix.domTree.Node;
import mfix.fitness.FitnessFunction;
import mfix.fitness.GoogleAPIResults;
import mfix.segmentation.InterSegmentEdge;
import mfix.segmentation.Segment;

public class Wrapper
{
	private GoogleAPIResults gar;
	
	public Wrapper(GoogleAPIResults gar)
	{
		this.gar = gar;
	}
	
	public Chromosome extractGenesBasedOnIssueType()
	{
		Chromosome chromosome = new Chromosome();
		
		HtmlDomTree instance = HtmlDomTree.getInstance(MFix.getFilepath());
		
		// get set of segments
		Set<Segment> segments = new HashSet<Segment>();
		for(InterSegmentEdge e : MFix.getOriginalPageSegmentModel().getEdges())
		{
			segments.add(e.getSegment1());
			segments.add(e.getSegment2());
		}
		
		String segmentIssueId = "";
		for(String issue : Constants.PROBLEM_TYPES_MAP.values())
		{
			double impactScore = gar.getUsabilityScoreAPIObj().getRuleImpactScore(issue);
			if(impactScore > 0.0)
			{
				Set<String> cssProperties = getApplicableCSSProperties(issue);
				
				if(issue.equalsIgnoreCase(Constants.CONTENT_SIZE_PROBLEM))
				{
					int[] widths = gar.getUsabilityScoreAPIObj().getContentWidth();
					HashMap<String, String> contentSizeWidth = new HashMap<String, String>();
					contentSizeWidth.put("width", widths[1] + "px");
					Constants.GOOGLE_SUGGESTED_VALUES.put(issue, contentSizeWidth);
					
					// create dependency graph for the viewport (ghost) segment
					DependencyGraph dg = new DependencyGraph(Constants.VIEWPORT_SEGMENT_ID);
					dg.createDependencyGraph(instance.getRoot(), issue);
					
					// create a gene only if dependency graph exists
					if(dg.getDependentNodesMap().size() > 0)
					{
						segmentIssueId = Util.getSegmentIssueId(Constants.VIEWPORT_SEGMENT_ID, issue);
						MFix.getSegmentToDG().put(segmentIssueId, dg);
						System.out.println("\nDependency graph for " + segmentIssueId);
						System.out.println(dg);
						
						// create gene and add to chromosome
						for(String property : cssProperties)
						{
							String value = Constants.GOOGLE_SUGGESTED_VALUES.get(issue).get(property);
	
							Gene gene = new Gene();
							gene.setCssProperty(property);
							gene.setValue(value);
							gene.setOriginalValue(value);
							gene.setIssueType(issue);
							gene.setImpactScore(impactScore);
							gene.setSegmentIssueId(segmentIssueId);
							chromosome.addGene(gene);
						}
					}
				}
				else
				{
					// create dependency graph for each segment based on the issue types legibility and tap targets
					for(Segment seg : segments)
					{
						if(seg.isGhostSegment()){
							continue;
						}
						Node<HtmlElement> segmentRoot = instance.searchHtmlDomTreeByXpath(seg.getLowestCommonAncestor());
						DependencyGraph dg = new DependencyGraph(seg.getId());
						dg.createDependencyGraph(segmentRoot, issue);
						
						// create a gene only if dependency graph exists
						if(dg.getDependentNodesMap().size() > 0)
						{
							segmentIssueId = Util.getSegmentIssueId(seg.getId(), issue);
							MFix.getSegmentToDG().put(segmentIssueId, dg);
							System.out.println("\nDependency graph for " + segmentIssueId);
							System.out.println(dg);
							
							for(String property : cssProperties)
							{
								String value = Constants.GOOGLE_SUGGESTED_VALUES.get(issue).get(property);
		
								Gene gene = new Gene();
								gene.setCssProperty(property);
								gene.setValue(value);
								gene.setOriginalValue(value);
								gene.setIssueType(issue);
								gene.setImpactScore(impactScore);
								gene.setSegmentIssueId(segmentIssueId);
								chromosome.addGene(gene);
							}
						}
					}
				}
			}
		}
		FitnessFunction ff = new FitnessFunction();
		ff.setUsabilityScore(Double.MAX_VALUE);
		ff.setAestheticScore(0.0);
		ff.setFitnessScore(ff.getUsabilityScore());
		chromosome.setFitnessFunctionObj(ff);
		
		return chromosome;
	}
	
	public Set<String> getApplicableCSSProperties(String problemType)
	{
		Set<String> cssProperties = new HashSet<>();
		if (problemType.equals(Constants.FONT_SIZE_PROBLEM))
		{
			cssProperties.add("font-size");
		}
		else if (problemType.equals(Constants.TAP_TARGET_PROBLEM))
		{
			cssProperties.add("margin");
		}
		else if(problemType.equals(Constants.CONTENT_SIZE_PROBLEM))
		{
			cssProperties.add("width");
		}
		return cssProperties;
	}
}
