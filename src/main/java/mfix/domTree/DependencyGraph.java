package mfix.domTree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

import mfix.Constants;
import mfix.MFix;
import mfix.Util;
import mfix.WebDriverSingleton;
import mfix.segmentation.InterSegmentEdge;
import mfix.segmentation.Segment;

public class DependencyGraph
{
	private TreeMap<String, List<DependentNode>> dependentNodesMap;		// <xpath, {list of dependent nodes}>
	private int segmentId; 

	public DependencyGraph(int segmentId)
	{
		dependentNodesMap = new TreeMap<String, List<DependentNode>>();
		this.segmentId = segmentId;
	}
	
	public TreeMap<String, List<DependentNode>> getDependentNodesMap()
	{
		return dependentNodesMap;
	}

	public void setDependentNodesMap(TreeMap<String, List<DependentNode>> dependentNodesMap)
	{
		this.dependentNodesMap = dependentNodesMap;
	}

	public int getSegmentId()
	{
		return segmentId;
	}

	public void setSegmentId(int segmentId)
	{
		this.segmentId = segmentId;
	}

	public void createDependencyGraph(Node<HtmlElement> root, String issueType)
	{
		switch (issueType)
		{
			case Constants.FONT_SIZE_PROBLEM:
				createDependencyGraphForFontSizeIssue(root);
				break;
			case Constants.TAP_TARGET_PROBLEM:
				createDependencyGraphForTapTargetIssue(root);
				break;
			case Constants.CONTENT_SIZE_PROBLEM:
				createDependencyGraphForContentSizeIssue(root);
				break;
			default:
				System.out.println("Issue type " + issueType + " not supported");
		}
	}
	
	public List<Element> getElementsToChange(String cssProperty, String value, String issueType)
	{
		List<Element> elementsToChange = new ArrayList<>();
		
		switch (issueType)
		{
			case Constants.FONT_SIZE_PROBLEM:
				elementsToChange = getElementsToChangeForFontSizeIssue(cssProperty, value);
				break;
			case Constants.TAP_TARGET_PROBLEM:
				elementsToChange = getElementsToChangeForTapTargetIssue(cssProperty, value);
				break;
			case Constants.CONTENT_SIZE_PROBLEM:
				elementsToChange = getElementsToChangeForContentSizeIssue(cssProperty, value);
				break;
			default:
				System.out.println("Issue type " + issueType + " not supported");
		}
		
		return elementsToChange;
	}
	
	private boolean isSegmentMember(String xpath)
	{
		// find segment of interest
		Segment seg = null;
		for(InterSegmentEdge e : MFix.getOriginalPageSegmentModel().getEdges())
		{
			if(e.getSegment1().getId() == segmentId)
			{
				seg = e.getSegment1();
				break;
			}
			if(e.getSegment2().getId() == segmentId)
			{
				seg = e.getSegment2();
				break;
			}
		}
		if(seg != null)
		{
			if(seg.getLowestCommonAncestor().equalsIgnoreCase(xpath))
				return true;
			
			for(String member : seg.getMembers())
			{
				if(member.equalsIgnoreCase(xpath))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	private void createDependencyGraphForFontSizeIssue(Node<HtmlElement> root)
	{
		// dependentNodesMap = <xpath#property, {list of dependent nodes}>
		
		// don't process elements that cannot have text
		if(root.getData().getTagName().equalsIgnoreCase("img"))
		{
			return;
		}
		
		// check if root has a non-zero font-size value, if not then find a new root (ancestor)
		String rootValue = Util.getValueFromElement(WebDriverSingleton.getDriver(), root.getData().getXpath(), "font-size");
		double rootValueNumber = 16.0;
		if(rootValue.matches(".*\\d+.*"))
		{
			rootValueNumber = Util.getNumbersFromString(rootValue).get(0);
			while(rootValueNumber <= 0 && root != null)
			{
				root = root.getParent();
				rootValue = Util.getValueFromElement(WebDriverSingleton.getDriver(), root.getData().getXpath(), "font-size");
				if(rootValue.matches(".*\\d+.*"))
				{
					rootValueNumber = Util.getNumbersFromString(rootValue).get(0);
				}
			}
		}
		
		// add root to the dependency graph with the main property and all dependent properties
		dependentNodesMap.put(root.getData().getXpath() + "#" + "font-size", new ArrayList<DependentNode>());
		for(String prop : Constants.CSS_PROPERTIES_DEPENDENCY.get("font-size"))
		{
			if(root.getData().getCssMap().containsKey(prop))
			{
				String dependentPropValue = Util.getValueFromElement(WebDriverSingleton.getDriver(), root.getData().getXpath(), prop);
				if(dependentPropValue.matches(".*\\d+.*"))
				{
					double dependentPropValueNumber = Util.getNumbersFromString(dependentPropValue).get(0);
					if(dependentPropValueNumber > 0)
					{
						double ratio = rootValueNumber / dependentPropValueNumber;
						dependentNodesMap.get(root.getData().getXpath() + "#" + "font-size").add(new DependentNode(root.getData().getXpath(), prop, ratio, rootValueNumber, dependentPropValueNumber));
					}
				}
			}
		}
		
		// add children of the root to the queue
		Queue<Node<HtmlElement>> q = new LinkedList<Node<HtmlElement>>();
		if (root.getChildren() != null)
		{
			for (Node<HtmlElement> child : root.getChildren())
			{
				q.add(child);
			}
		}
		
		// process descendants of the root in a bread first fashion
		while (!q.isEmpty())
		{
			Node<HtmlElement> node = q.remove();
			HtmlElement e = node.getData();
			
			// don't process elements that cannot have text
			if(e.getTagName().equalsIgnoreCase("img"))
			{
				continue;
			}

			// check if the element is a leaf or text node, if yes then check if it is a segment member
			if(e.getTextCoords().size() > 0 || node.getChildren() == null)
			{
				boolean isMember = isSegmentMember(e.getXpath());
				if(!isMember)
				{
					continue;
				}
			}
			
			// check if the node has a defined font-size CSS property that is not "inherit" or 
			// is one of the tags with default font-size (h1 - h6, small, sup, sub) and enforced for the root
			if ((e.getCssMap().containsKey("font-size") && !e.getCssMap().get("font-size").equalsIgnoreCase("inherit")) 
					|| Constants.ELEMENTS_WITH_DEFAULT_FONT_SIZE.contains(e.getTagName().toLowerCase()))
			{
				// find ancestor that has a defined font-size property to establish an edge between the ancestor and node
				Node<HtmlElement> ancestor = node.getParent();
				boolean isAncestorFound = false;
				boolean isAncestorGraphKey = false;
				while (!isAncestorFound && ancestor != null)
				{
					if (dependentNodesMap.containsKey(ancestor.getData().getXpath() + "#" + "font-size"))
					{
						isAncestorFound = true;
						isAncestorGraphKey = true;
						break;
					}
					else
					{
						// check if the ancestor is present in the values (dependent nodes)
						for(List<DependentNode> dnList : dependentNodesMap.values())
						{
							for(DependentNode dn : dnList)
							{
								if(dn.getXpath().equalsIgnoreCase(ancestor.getData().getXpath()) && dn.getProperty().equalsIgnoreCase("font-size"))
								{
									isAncestorFound = true;
									isAncestorGraphKey = false;
									break;
								}
							}
							if(isAncestorFound)
								break;
						}
					}
					if(!isAncestorFound)
						ancestor = ancestor.getParent();
				}

				// add edge in dependency graph between ancestor and current node
				if (isAncestorFound)
				{
					String ancestorValue = Util.getValueFromElement(WebDriverSingleton.getDriver(), ancestor.getData().getXpath(), "font-size");
					String nodeValue = Util.getValueFromElement(WebDriverSingleton.getDriver(), e.getXpath(), "font-size");
					
					// check if the css values contain digits to avoid null pointer exception
					if(ancestorValue.matches(".*\\d+.*") && nodeValue.matches(".*\\d+.*"))
					{
						double ancestorValueNumber = Util.getNumbersFromString(ancestorValue).get(0);
						double nodeValueNumber = Util.getNumbersFromString(nodeValue).get(0);
						// to avoid divide by zero error
						if(ancestorValueNumber > 0 && nodeValueNumber > 0)
						{
							double ratio = ancestorValueNumber / nodeValueNumber;
							DependentNode dn = new DependentNode(e.getXpath(), "font-size", ratio, ancestorValueNumber, nodeValueNumber);
							if(isAncestorGraphKey || dependentNodesMap.containsKey(ancestor.getData().getXpath() + "#" + "font-size"))
							{
								dependentNodesMap.get(ancestor.getData().getXpath() + "#" + "font-size").add(dn);
							}
							else
							{
								List<DependentNode> dependentNodes = new ArrayList<DependentNode>();
								dependentNodes.add(dn);
								dependentNodesMap.put(ancestor.getData().getXpath() + "#" + "font-size", dependentNodes);
							}
						}
					}
				}
			}
				
			// process for dependent CSS properties
			for(String prop : Constants.CSS_PROPERTIES_DEPENDENCY.get("font-size"))
			{
				if(e.getCssMap().containsKey(prop))
				{
					// find ancestor that has a defined font-size property to establish an edge between the ancestor and node
					Node<HtmlElement> ancestorDP = node;
					boolean isAncestorFoundDP = false;
					
					// first check if an edge to self should be added or to some ancestor
					if((e.getCssMap().containsKey("font-size") && !e.getCssMap().get("font-size").equalsIgnoreCase("inherit")) 
							|| Constants.ELEMENTS_WITH_DEFAULT_FONT_SIZE.contains(e.getTagName().toLowerCase()))
					{
						isAncestorFoundDP = true;
					}
					boolean isAncestorDPGraphKey = false;
					while (!isAncestorFoundDP && ancestorDP != null)
					{
						if (dependentNodesMap.containsKey(ancestorDP.getData().getXpath() + "#" + "font-size"))
						{
							isAncestorFoundDP = true;
							isAncestorDPGraphKey = true;
							break;
						}
						else
						{
							// check if the ancestor is present in the values (dependent nodes)
							for(List<DependentNode> dnList : dependentNodesMap.values())
							{
								for(DependentNode dn : dnList)
								{
									if(dn.getXpath().equalsIgnoreCase(ancestorDP.getData().getXpath()) && dn.getProperty().equalsIgnoreCase("font-size"))
									{
										isAncestorFoundDP = true;
										isAncestorDPGraphKey = false;
										break;
									}
								}
								if(isAncestorFoundDP)
									break;
							}
						}
						if(!isAncestorFoundDP)
							ancestorDP = ancestorDP.getParent();
					}
					
					// add edge in dependency graph between ancestor and current node for dependent property
					if (isAncestorFoundDP)
					{
						String ancestorFontSizeValue = Util.getValueFromElement(WebDriverSingleton.getDriver(), ancestorDP.getData().getXpath(), "font-size");
						String nodeDependentPropValue = Util.getValueFromElement(WebDriverSingleton.getDriver(), e.getXpath(), prop);
						
						// check if the css values contain digits to avoid null pointer exception
						if(ancestorFontSizeValue.matches(".*\\d+.*") && nodeDependentPropValue.matches(".*\\d+.*"))
						{
							double ancestorFontSizeValueNumber = Util.getNumbersFromString(ancestorFontSizeValue).get(0);
							double nodeDependentPropValueNumber = Util.getNumbersFromString(nodeDependentPropValue).get(0);
							// to avoid divide by zero error
							if(ancestorFontSizeValueNumber > 0 && nodeDependentPropValueNumber > 0)
							{
								double ratio = ancestorFontSizeValueNumber / nodeDependentPropValueNumber;
								DependentNode dn = new DependentNode(e.getXpath(), prop, ratio, ancestorFontSizeValueNumber, nodeDependentPropValueNumber);
								if(isAncestorDPGraphKey || dependentNodesMap.containsKey(ancestorDP.getData().getXpath() + "#" + "font-size"))
								{
									dependentNodesMap.get(ancestorDP.getData().getXpath() + "#" + "font-size").add(dn);
								}
								else
								{
									List<DependentNode> dependentNodes = new ArrayList<DependentNode>();
									dependentNodes.add(dn);
									dependentNodesMap.put(ancestorDP.getData().getXpath() + "#" + "font-size", dependentNodes);
								}
							}
						}
					}
				}
			}

			if (node.getChildren() != null)
			{
				for (Node<HtmlElement> child : node.getChildren())
				{
					q.add(child);
				}
			}
		}
	}
	
	private void createDependencyGraphForTapTargetIssue(Node<HtmlElement> root)
	{
		List<HtmlElement> tapTargets = new ArrayList<>();

		// get all tap targets
		Queue<Node<HtmlElement>> q = new LinkedList<Node<HtmlElement>>();
		q.add(root);

		while (!q.isEmpty())
		{
			Node<HtmlElement> node = q.remove();
			HtmlElement e = node.getData();
			
			// check if the element is a leaf or text node, if yes then check if it is a segment member
			if(node.getChildren() == null)
			{
				boolean isMember = isSegmentMember(e.getXpath());
				if(!isMember)
				{
					continue;
				}
			}

			if (Constants.TAP_TARGET_ELEMENTS.contains(e.getTagName()))
			{
				tapTargets.add(e);
			}

			if (node.getChildren() != null)
			{
				for (Node<HtmlElement> child : node.getChildren())
				{
					q.add(child);
				}
			}
		}
		
		// check for dependencies among the tap target elements
		for(int i = 0; i < tapTargets.size(); i++)
		{
			HtmlElement e = tapTargets.get(i);
			List<DependentNode> dependentNodes = new ArrayList<>();
			
			// check if any other tap target is within the defined radius
			for(int j = 0; j < tapTargets.size(); j++)
			{
				HtmlElement n = tapTargets.get(j);
				if(!e.getXpath().equalsIgnoreCase(n.getXpath()) && isElementCloseToAnother(e, n, Constants.TAP_TARGETS_RADIUS))
				{
					// check the position of the neighboring tap target and apply margin in that direction
					Rectangle eRect = e.getCoord();
					Rectangle nRect = n.getCoord();
					// n is above e: e.y1 > n.y2
					if(eRect.y > (nRect.y + nRect.height))
						dependentNodes.add(new DependentNode(n.getXpath(), "margin-bottom", 1.0, 1.0, 1.0));
					// n is below e: e.y1 < n.y2
					if((eRect.y + eRect.height) < nRect.y)
						dependentNodes.add(new DependentNode(n.getXpath(), "margin-top", 1.0, 1.0, 1.0));
					// n is to the left of e: e.x1 > n.x2
					if(eRect.x > (nRect.x + nRect.width))
						dependentNodes.add(new DependentNode(n.getXpath(), "margin-right", 1.0, 1.0, 1.0));
					// n is to the right of e: e.x2 < n.x1
					if((eRect.x + eRect.width) < nRect.x)
						dependentNodes.add(new DependentNode(n.getXpath(), "margin-left", 1.0, 1.0, 1.0));
				}
			}
			dependentNodesMap.put(e.getXpath(), dependentNodes);
		}
	}
	
	private boolean isElementCloseToAnother(HtmlElement e, HtmlElement n, double distance)
	{
		Rectangle eRect = e.getCoord();
		Rectangle nRect = n.getCoord();
		double eTLnTL = Util.getEuclideanDistanceBetweenPoints(eRect.x, eRect.y, nRect.x, nRect.y);
		double eTLnTR = Util.getEuclideanDistanceBetweenPoints(eRect.x, eRect.y, (nRect.x + nRect.width), nRect.y);
		double eTLnBL = Util.getEuclideanDistanceBetweenPoints(eRect.x, eRect.y, nRect.x, (nRect.y + nRect.height));
		double eTLnBR = Util.getEuclideanDistanceBetweenPoints(eRect.x, eRect.y, (nRect.x +nRect.width), (nRect.y + nRect.height));
		
		double eTRnTL = Util.getEuclideanDistanceBetweenPoints((eRect.x + eRect.width), eRect.y, nRect.x, nRect.y);
		double eTRnTR = Util.getEuclideanDistanceBetweenPoints((eRect.x + eRect.width), eRect.y, (nRect.x + nRect.width), nRect.y);
		double eTRnBL = Util.getEuclideanDistanceBetweenPoints((eRect.x + eRect.width), eRect.y, nRect.x, (nRect.y + nRect.height));
		double eTRnBR = Util.getEuclideanDistanceBetweenPoints((eRect.x + eRect.width), eRect.y, (nRect.x +nRect.width), (nRect.y + nRect.height));
		
		double eBLnTL = Util.getEuclideanDistanceBetweenPoints(eRect.x, (eRect.y + eRect.height), nRect.x, nRect.y);
		double eBLnTR = Util.getEuclideanDistanceBetweenPoints(eRect.x, (eRect.y + eRect.height), (nRect.x + nRect.width), nRect.y);
		double eBLnBL = Util.getEuclideanDistanceBetweenPoints(eRect.x, (eRect.y + eRect.height), nRect.x, (nRect.y + nRect.height));
		double eBLnBR = Util.getEuclideanDistanceBetweenPoints(eRect.x, (eRect.y + eRect.height), (nRect.x +nRect.width), (nRect.y + nRect.height));
		
		double eBRnTL = Util.getEuclideanDistanceBetweenPoints((eRect.x + eRect.width), (eRect.y + eRect.height), nRect.x, nRect.y);
		double eBRnTR = Util.getEuclideanDistanceBetweenPoints((eRect.x + eRect.width), (eRect.y + eRect.height), (nRect.x + nRect.width), nRect.y);
		double eBRnBL = Util.getEuclideanDistanceBetweenPoints((eRect.x + eRect.width), (eRect.y + eRect.height), nRect.x, (nRect.y + nRect.height));
		double eBRnBR = Util.getEuclideanDistanceBetweenPoints((eRect.x + eRect.width), (eRect.y + eRect.height), (nRect.x +nRect.width), (nRect.y + nRect.height));
		
		if(eTLnTL < distance || eTLnTR < distance || eTLnBL < distance || eTLnBR < distance ||
				eTRnTL < distance || eTRnTR < distance || eTRnBL < distance || eTRnBR < distance ||
				eBLnTL < distance || eBLnTR < distance || eBLnBL < distance || eBLnBR < distance ||
				eBRnTL < distance || eBRnTR < distance || eBRnBL < distance || eBRnBR < distance)
		{
			return true;
		}
		return false;
	}
	
	private void createDependencyGraphForContentSizeIssue(Node<HtmlElement> root)
	{
		// dependentNodesMap = <xpath#property, {list of dependent nodes}>
		
		// check if root has a non-zero width value, if not then find a new root (ancestor)
		String rootValue = Util.getValueFromElement(WebDriverSingleton.getDriver(), root.getData().getXpath(), "width");
		double rootValueNumber = root.getData().getCoord().width;
		if(rootValue.matches(".*\\d+.*"))
		{
			rootValueNumber = Util.getNumbersFromString(rootValue).get(0);
			while(rootValueNumber <= 0 && root != null)
			{
				root = root.getParent();
				rootValue = Util.getValueFromElement(WebDriverSingleton.getDriver(), root.getData().getXpath(), "width");
				if(rootValue.matches(".*\\d+.*"))
				{
					rootValueNumber = Util.getNumbersFromString(rootValue).get(0);
				}
			}
		}
		
		// add root to the dependency graph with the main property and all dependent properties
		dependentNodesMap.put(root.getData().getXpath() + "#" + "width", new ArrayList<DependentNode>());
		for(String prop : Constants.CSS_PROPERTIES_DEPENDENCY.get("width"))
		{
			if(root.getData().getCssMap().containsKey(prop))
			{
				String dependentPropValue = Util.getValueFromElement(WebDriverSingleton.getDriver(), root.getData().getXpath(), prop);
				if(dependentPropValue.matches(".*\\d+.*"))
				{
					double dependentPropValueNumber = Util.getNumbersFromString(dependentPropValue).get(0);
					if(dependentPropValueNumber > 0)
					{
						double ratio = rootValueNumber / dependentPropValueNumber;
						dependentNodesMap.get(root.getData().getXpath() + "#" + "width").add(new DependentNode(root.getData().getXpath(), prop, ratio, rootValueNumber, dependentPropValueNumber));
					}
				}
			}
		}
		
		// add children of the root to the queue
		Queue<Node<HtmlElement>> q = new LinkedList<Node<HtmlElement>>();
		if (root.getChildren() != null)
		{
			for (Node<HtmlElement> child : root.getChildren())
			{
				q.add(child);
			}
		}
		
		// process descendants of the root in a bread first fashion
		while (!q.isEmpty())
		{
			Node<HtmlElement> node = q.remove();
			HtmlElement e = node.getData();
			
			// check if the node has a defined width CSS property that is not "inherit", "auto" or
			// is one of the tags with default width (table, td, th)
			if ((e.getCssMap().containsKey("width") && !e.getCssMap().get("width").equalsIgnoreCase("inherit") && !e.getCssMap().get("width").equalsIgnoreCase("auto")) 
					|| Constants.ELEMENTS_WITH_DEFAULT_WIDTH.contains(e.getTagName().toLowerCase()))
			{
				// find ancestor that has a defined width property to establish an edge between the ancestor and node
				Node<HtmlElement> ancestor = node.getParent();
				boolean isAncestorFound = false;
				boolean isAncestorGraphKey = false;
				while (!isAncestorFound && ancestor != null)
				{
					if (dependentNodesMap.containsKey(ancestor.getData().getXpath() + "#" + "width"))
					{
						isAncestorFound = true;
						isAncestorGraphKey = true;
						break;
					}
					else
					{
						// check if the ancestor is present in the values (dependent nodes)
						for(List<DependentNode> dnList : dependentNodesMap.values())
						{
							for(DependentNode dn : dnList)
							{
								if(dn.getXpath().equalsIgnoreCase(ancestor.getData().getXpath()) && dn.getProperty().equalsIgnoreCase("width"))
								{
									isAncestorFound = true;
									isAncestorGraphKey = false;
									break;
								}
							}
							if(isAncestorFound)
								break;
						}
					}
					if(!isAncestorFound)
						ancestor = ancestor.getParent();
				}

				// add edge in dependency graph between ancestor and current node
				if (isAncestorFound)
				{
					String ancestorValue = Util.getValueFromElement(WebDriverSingleton.getDriver(), ancestor.getData().getXpath(), "width");
					String nodeValue = Util.getValueFromElement(WebDriverSingleton.getDriver(), e.getXpath(), "width");
					
					// check if the css values contain digits to avoid null pointer exception
					if(ancestorValue.matches(".*\\d+.*") && nodeValue.matches(".*\\d+.*"))
					{
						double ancestorValueNumber = Util.getNumbersFromString(ancestorValue).get(0);
						double nodeValueNumber = Util.getNumbersFromString(nodeValue).get(0);
						// to avoid divide by zero error
						if(ancestorValueNumber > 0 && nodeValueNumber > 0)
						{
							double ratio = ancestorValueNumber / nodeValueNumber;
							DependentNode dn = new DependentNode(e.getXpath(), "width", ratio, ancestorValueNumber, nodeValueNumber);
							if(isAncestorGraphKey || dependentNodesMap.containsKey(ancestor.getData().getXpath() + "#" + "width"))
							{
								dependentNodesMap.get(ancestor.getData().getXpath() + "#" + "width").add(dn);
							}
							else
							{
								List<DependentNode> dependentNodes = new ArrayList<DependentNode>();
								dependentNodes.add(dn);
								dependentNodesMap.put(ancestor.getData().getXpath() + "#" + "width", dependentNodes);
							}
						}
					}
				}
			}
				
			// process for dependent CSS properties
			for(String prop : Constants.CSS_PROPERTIES_DEPENDENCY.get("width"))
			{
				if(e.getCssMap().containsKey(prop) && !e.getCssMap().get(prop).equalsIgnoreCase("auto") && !e.getCssMap().get(prop).equalsIgnoreCase("inherit"))
				{
					// find ancestor that has a defined width property to establish an edge between the ancestor and node
					Node<HtmlElement> ancestorDP = node;
					boolean isAncestorFoundDP = false;
					
					// first check if an edge to self should be added or to some ancestor
					if((e.getCssMap().containsKey("width") && !e.getCssMap().get("width").equalsIgnoreCase("inherit") && !e.getCssMap().get("width").equalsIgnoreCase("auto")) 
							|| Constants.ELEMENTS_WITH_DEFAULT_WIDTH.contains(e.getTagName().toLowerCase()))
					{
						isAncestorFoundDP = true;
					}
					boolean isAncestorDPGraphKey = false;
					while (!isAncestorFoundDP && ancestorDP != null)
					{
						if (dependentNodesMap.containsKey(ancestorDP.getData().getXpath() + "#" + "width"))
						{
							isAncestorFoundDP = true;
							isAncestorDPGraphKey = true;
							break;
						}
						else
						{
							// check if the ancestor is present in the values (dependent nodes)
							for(List<DependentNode> dnList : dependentNodesMap.values())
							{
								for(DependentNode dn : dnList)
								{
									if(dn.getXpath().equalsIgnoreCase(ancestorDP.getData().getXpath()) && dn.getProperty().equalsIgnoreCase("width"))
									{
										isAncestorFoundDP = true;
										isAncestorDPGraphKey = false;
										break;
									}
								}
								if(isAncestorFoundDP)
									break;
							}
						}
						if(!isAncestorFoundDP)
							ancestorDP = ancestorDP.getParent();
					}
					
					// add edge in dependency graph between ancestor and current node for dependent property
					if (isAncestorFoundDP)
					{
						String ancestorFontSizeValue = Util.getValueFromElement(WebDriverSingleton.getDriver(), ancestorDP.getData().getXpath(), "width");
						String nodeDependentPropValue = Util.getValueFromElement(WebDriverSingleton.getDriver(), e.getXpath(), prop);
						
						// check if the css values contain digits to avoid null pointer exception
						if(ancestorFontSizeValue.matches(".*\\d+.*") && nodeDependentPropValue.matches(".*\\d+.*"))
						{
							double ancestorFontSizeValueNumber = Util.getNumbersFromString(ancestorFontSizeValue).get(0);
							double nodeDependentPropValueNumber = Util.getNumbersFromString(nodeDependentPropValue).get(0);
							// to avoid divide by zero error
							if(ancestorFontSizeValueNumber > 0 && nodeDependentPropValueNumber > 0)
							{
								double ratio = ancestorFontSizeValueNumber / nodeDependentPropValueNumber;
								DependentNode dn = new DependentNode(e.getXpath(), prop, ratio, ancestorFontSizeValueNumber, nodeDependentPropValueNumber);
								if(isAncestorDPGraphKey || dependentNodesMap.containsKey(ancestorDP.getData().getXpath() + "#" + "width"))
								{
									dependentNodesMap.get(ancestorDP.getData().getXpath() + "#" + "width").add(dn);
								}
								else
								{
									List<DependentNode> dependentNodes = new ArrayList<DependentNode>();
									dependentNodes.add(dn);
									dependentNodesMap.put(ancestorDP.getData().getXpath() + "#" + "width", dependentNodes);
								}
							}
						}
					}
				}
			}

			if (node.getChildren() != null)
			{
				for (Node<HtmlElement> child : node.getChildren())
				{
					q.add(child);
				}
			}
		}
	}
	
	private List<Element> getElementsToChangeForFontSizeIssue(String cssProperty, String value)
	{
		List<Element> dependentElements = new ArrayList<>();
		
		// choose seed element as the first element key in the dependent nodes map 
		String xpath = dependentNodesMap.firstKey().split("#")[0];
		
		dependentElements.add(new Element(xpath, cssProperty, value));

		double elementValue = Util.getNumbersFromString(value).get(0);
		String unit = Util.getUnitFromStringValue(value);

		boolean isDone = false;
		Set<String> visitedElements = new HashSet<>();
		Queue<DependentNode> queue = new LinkedList<DependentNode>();
		while (!isDone)
		{
			visitedElements.add(xpath);
			List<DependentNode> dependentNodes = dependentNodesMap.get(xpath + "#" + cssProperty);
			if(dependentNodes != null)
			{			
				for (DependentNode dNode : dependentNodes)
				{
					double dElementValue = elementValue / dNode.getRatio();
					Element dElement = new Element(dNode.getXpath(), dNode.getProperty(), dElementValue + unit);
					
					if(!dependentElements.contains(dElement))
						dependentElements.add(dElement);
					
					if (!visitedElements.contains(dNode.getXpath()))
						queue.add(dNode);
				}
			}
			if (queue.isEmpty())
			{
				isDone = true;
			}
			else
			{
				DependentNode node = queue.remove();
				xpath = node.getXpath();
				for (Element e : dependentElements)
				{
					if (e.getXpath().equalsIgnoreCase(xpath))
					{
						value = e.getValue();
						break;
					}
				}
				elementValue = Util.getNumbersFromString(value).get(0);
			}
		}
		return dependentElements;
	}
	
	private List<Element> getElementsToChangeForTapTargetIssue(String cssProperty, String value)
	{
		Set<Element> dependentElements = new HashSet<>();
		double elementValue = Util.getNumbersFromString(value).get(0);
		String unit = Util.getUnitFromStringValue(value);
		String valueToApply = (elementValue / 2) + unit;	// divide by 2 as the neighbor will also add margin
		
		for(String xpath : dependentNodesMap.keySet())
		{
			// check if element has an image that is less than 48px
			List<HtmlElement> imageElements = getImageElements(xpath);
			String widthValue = Constants.GOOGLE_SUGGESTED_VALUES.get(Constants.TAP_TARGET_PROBLEM).get("width");
			String heightValue = Constants.GOOGLE_SUGGESTED_VALUES.get(Constants.TAP_TARGET_PROBLEM).get("height");
			
			for(HtmlElement e : imageElements)
			{
				// process if elements don't contain background image position and size specified, as it messes up the image
				if (!(e.getCssMap().containsKey("background-image") && e.getCssMap().get("background-image").contains("url")))
				{
					if(e.getCoord().width < Util.getNumbersFromString(widthValue).get(0))
					{
						dependentElements.add(new Element(e.getXpath(), "width", widthValue));
					}
					if(e.getCoord().height < Util.getNumbersFromString(heightValue).get(0))
					{
						dependentElements.add(new Element(e.getXpath(), "height", heightValue));
					}
				}
			}
			
			List<DependentNode> elements = dependentNodesMap.get(xpath);
			for(DependentNode dn : elements)
			{
				dependentElements.add(new Element(dn.getXpath(), dn.getProperty(), valueToApply));
				// to avoid problem of collapsing margins, add display = inline-block
				HtmlDomTree instance = HtmlDomTree.getInstance(MFix.getFilepath());
				HtmlElement eDisplay = instance.searchHtmlDomTreeByXpath(dn.getXpath()).getData();
				if(!eDisplay.getCssMap().containsKey("display") || !eDisplay.getCssMap().get("display").contains("inline"))
				{
					dependentElements.add(new Element(dn.getXpath(), "display", "inline-block"));
				}
				
				// check if element has an image that is less than 48px
				imageElements = getImageElements(xpath);
				for(HtmlElement e : imageElements)
				{
					if(e.getCoord().width < Util.getNumbersFromString(widthValue).get(0))
					{
						dependentElements.add(new Element(e.getXpath(), "width", widthValue));
					}
					if(e.getCoord().height < Util.getNumbersFromString(heightValue).get(0))
					{
						dependentElements.add(new Element(e.getXpath(), "height", heightValue));
					}
				}
			}
		}
		return new ArrayList<>(dependentElements);
	}

	private List<HtmlElement> getImageElements(String xpath)
	{
		List<HtmlElement> imageElements = new ArrayList<>();
		
		HtmlDomTree instance = HtmlDomTree.getInstance(MFix.getFilepath());
		List<Node<HtmlElement>> children = instance.searchHtmlDomTreeByXpath(xpath).getChildren();
		Queue<Node<HtmlElement>> queue = new LinkedList<Node<HtmlElement>>();
		
		if(children != null)
		{
			for(Node<HtmlElement> c : children)
			{
				queue.add(c);
			}
		}
		
		while(!queue.isEmpty())
		{
			Node<HtmlElement> e = queue.remove();
			if(e.getData().getTagName().equalsIgnoreCase("img"))
			{
				imageElements.add(e.getData());
			}
			
			children = e.getChildren();
			if(children != null)
			{
				for(Node<HtmlElement> c : children)
				{
					queue.add(c);
				}
			}
		}
		return imageElements;
	}
	
	private List<Element> getElementsToChangeForContentSizeIssue(String cssProperty, String value)
	{
		List<Element> dependentElements = new ArrayList<>();
		
		// choose seed element as the first element key in the dependent nodes map 
		String xpath = dependentNodesMap.firstKey().split("#")[0];
		
		dependentElements.add(new Element(xpath, cssProperty, value));
		HtmlDomTree instance = HtmlDomTree.getInstance(MFix.getFilepath());
		HtmlElement element = instance.searchHtmlDomTreeByXpath(xpath).getData();
		
		// process for table to make tables respect the width assigned
		if(element.getTagName().equalsIgnoreCase("table"))
		{
			if(!element.getCssMap().containsKey("table-layout") || !element.getCssMap().get("table-layout").contains("fixed"))
			{
				dependentElements.add(new Element(xpath, "table-layout", "fixed"));
			}
		}
		else if(element.getTagName().equalsIgnoreCase("td") || element.getTagName().equalsIgnoreCase("th"))
		{
			if(!element.getCssMap().containsKey("white-space") || !element.getCssMap().get("white-space").contains("normal"))
			{
				dependentElements.add(new Element(xpath, "white-space", "normal"));
			}
			if(!element.getCssMap().containsKey("word-break") || !element.getCssMap().get("word-break").contains("break-all"))
			{
				dependentElements.add(new Element(xpath, "word-break", "break-all"));
			}
		}
		else
		{
			// process for padding-left, padding-right
			if(element.getCssMap().containsKey("padding-left"))
			{
				dependentElements.add(new Element(xpath, "padding-left", "0px"));
			}
			if(element.getCssMap().containsKey("padding-right"))
			{
				dependentElements.add(new Element(xpath, "padding-right", "0px"));
			}
		}
		double elementValue = Util.getNumbersFromString(value).get(0);
		String unit = Util.getUnitFromStringValue(value);

		boolean isDone = false;
		Set<String> visitedElements = new HashSet<>();
		Queue<DependentNode> queue = new LinkedList<DependentNode>();
		while (!isDone)
		{
			visitedElements.add(xpath);
			List<DependentNode> dependentNodes = dependentNodesMap.get(xpath + "#" + cssProperty);
			if(dependentNodes != null)
			{			
				for (DependentNode dNode : dependentNodes)
				{
					double dElementValue = elementValue / dNode.getRatio();
					
					Element dElement = new Element(dNode.getXpath(), dNode.getProperty(), dElementValue + unit);
					if (!dependentElements.contains(dElement))
					{
						element = instance.searchHtmlDomTreeByXpath(dElement.getXpath()).getData();
						
						// increase height as width reduces
						if(dNode.getProperty().equalsIgnoreCase("height"))
						{
							String heightValueString = Util.getValueFromElement(WebDriverSingleton.getDriver(), dNode.getXpath(), "height");
							if(heightValueString.matches(".*\\d+.*"))
							{
								double heightValue = Util.getNumbersFromString(heightValueString).get(0);
								dElementValue = heightValue / dNode.getRatio();
								dElement.setValue(dElementValue + unit);
							}
						}

						// add element to dependent elements
						dependentElements.add(dElement);

						// process for table
						if (element.getTagName().equalsIgnoreCase("table"))
						{
							if (!element.getCssMap().containsKey("table-layout") || !element.getCssMap().get("table-layout").contains("fixed"))
							{
								dependentElements.add(new Element(dElement.getXpath(), "table-layout", "fixed"));
							}
						}
						else if (element.getTagName().equalsIgnoreCase("td") || element.getTagName().equalsIgnoreCase("th"))
						{
							if (!element.getCssMap().containsKey("white-space") || !element.getCssMap().get("white-space").contains("normal"))
							{
								dependentElements.add(new Element(dElement.getXpath(), "white-space", "normal"));
							}
							if (!element.getCssMap().containsKey("word-break") || !element.getCssMap().get("word-break").contains("break-all"))
							{
								dependentElements.add(new Element(dElement.getXpath(), "word-break", "break-all"));
							}
						}
						else
						{
							// process for padding-left, padding-right
							if (element.getCssMap().containsKey("padding-left"))
							{
								dependentElements.add(new Element(dElement.getXpath(), "padding-left", "0px"));
							}
							if (element.getCssMap().containsKey("padding-right"))
							{
								dependentElements.add(new Element(dElement.getXpath(), "padding-right", "0px"));
							}
						}
					}

					if (!visitedElements.contains(dNode.getXpath()))
						queue.add(dNode);
				}
			}
			if (queue.isEmpty())
			{
				isDone = true;
			}
			else
			{
				DependentNode node = queue.remove();
				xpath = node.getXpath();
				for (Element e : dependentElements)
				{
					if (e.getXpath().equalsIgnoreCase(xpath))
					{
						value = e.getValue();
						break;
					}
				}
				elementValue = Util.getNumbersFromString(value).get(0);
			}
		}
		return dependentElements;
	}
	
	@Override
	public String toString()
	{
		String returnValue = "DependencyGraph = (size = " + dependentNodesMap.size() + ")\n";
		for (String xpath : dependentNodesMap.keySet())
		{
			returnValue = returnValue + xpath + " -> " + dependentNodesMap.get(xpath) + "\n";
		}
		return returnValue;
	}
}