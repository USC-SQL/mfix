package mfix.domTree;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.TreeMap;

import mfix.Constants;
import mfix.MFix;
import mfix.Util;
import mfix.WebDriverSingleton;
import mfix.segmentation.Segmentation;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.JavascriptExecutor;

public class HtmlDomTree
{
	private static final String[] NON_VISUAL_TAGS = new String[] { "head", "script", "link", "meta", "style", "title", "br", "noscript"};

	private static Node<HtmlElement> root;
	private static String filePath;

	private static HtmlDomTree instance;
	
	private static int numberOfHtmlElements;
	
	private HtmlDomTree(String filePath)
	{
		HtmlDomTree.filePath = filePath;
		HtmlDomTree.root = null;
		numberOfHtmlElements = 0;
	}
	
	public static int getNumberOfHtmlElements() {
		return numberOfHtmlElements;
	}

	public static HtmlDomTree getInstance(String filePath)
	{
		if (instance == null || !HtmlDomTree.filePath.equalsIgnoreCase(filePath))
		{
			instance = new HtmlDomTree(filePath);
		}
		return instance;
	}

	public static void resetInstance()
	{
		instance = null;
		numberOfHtmlElements = 0;
	}

	public String getDOMJson()
	{
		String fileContents = "";
		InputStream in = getClass().getClassLoader().getResourceAsStream("domInfo.js");
		StringWriter writer = new StringWriter();
		try
		{
			IOUtils.copy(in, writer, StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		fileContents = writer.toString();
		
		JavascriptExecutor js = (JavascriptExecutor) WebDriverSingleton.getDriver();
		String json = (String) js.executeScript(fileContents);
		return json;
	}

	public void buildHtmlDomTree()
	{
		if (root != null)
			return;
		
		System.out.println("building DOM tree");
		// get DOM json by running javascript on rendered page
		String json = getDOMJson();
		//System.out.println("\nJSON: \n" + json);

		// read json to create DOM tree and R-tree
		Map<Integer, Node<HtmlElement>> tempMapToCreateDomTree = new HashMap<Integer, Node<HtmlElement>>();
		JSONArray arrDom = new JSONArray(json.trim());

		// first pass to collect text from text nodes
		Map<Integer, TreeMap<Rectangle, String>> textMap = new HashMap<>();
		for (int i = 0; i < arrDom.length(); i++)
		{
			JSONObject nodeData = arrDom.getJSONObject(i);
			int type = nodeData.getInt("type");
			if (type == 0)
			{
				int pid = nodeData.getInt("pid");
				String text = "";
				try
				{
					text = URLDecoder.decode(nodeData.getString("text"), "UTF-8");
				}
				catch (UnsupportedEncodingException e)
				{
					System.err.println("Node text cannot be decoded :" + text);
					e.printStackTrace();
				}
				
				// don't add to the DOM tree if the text is empty
				text = text.trim();
				if(text.isEmpty())
					continue;
				
				JSONArray data = nodeData.getJSONArray("coord");
				for (int i1 = 0; i1 < data.length(); i1++)
				{
					if (!NumberUtils.isNumber(data.get(i1).toString()))
					{
						data.put(i1, 0);
					}
				}
				int[] coords = { data.getInt(0), data.getInt(1), data.getInt(2), data.getInt(3) };
				Rectangle rect = new Rectangle(coords[0], coords[1], (coords[2] - coords[0]), (coords[3] - coords[1]));
				
				if (textMap.containsKey(pid))
				{
					textMap.get(pid).put(rect, text);
				}
				else
				{
					TreeMap<Rectangle, String> textCoordMap = new TreeMap<>();
					textCoordMap.put(rect, text);
					textMap.put(pid, textCoordMap);
				}
			}
			else if (type == 2)	// viewport entry
			{
				JSONArray data = nodeData.getJSONArray("coord");
				for (int i1 = 0; i1 < data.length(); i1++)
				{
					if (!NumberUtils.isNumber(data.get(i1).toString()))
					{
						data.put(i1, 0);
					}
				}
				int[] coords = { data.getInt(0), data.getInt(1), data.getInt(2), data.getInt(3) };
				Rectangle rect = new Rectangle(coords[0], coords[1], (coords[2] - coords[0]), (coords[3] - coords[1]));
				Segmentation.setViewportRectangle(rect);
			}
		}

		// second pass for element nodes
		for (int i = 0; i < arrDom.length(); i++)
		{
			JSONObject nodeData = arrDom.getJSONObject(i);
			int type = nodeData.getInt("type");
			if (type == 1)
			{
				numberOfHtmlElements++;
				
				int pid = nodeData.getInt("pid");
				int nodeId = nodeData.getInt("nodeid");
				String tagName = nodeData.getString("tagname").toLowerCase();
				if (Arrays.asList(NON_VISUAL_TAGS).contains(tagName) || Constants.IGNORE_TAGS.contains(tagName))
					continue;

				String id = null;
				if (nodeData.has("id"))
				{
					id = nodeData.getString("id");
				}
				String xpath = nodeData.getString("xpath").toLowerCase();
				JSONArray data = nodeData.getJSONArray("coord");
				for (int i1 = 0; i1 < data.length(); i1++)
				{
					if (!NumberUtils.isNumber(data.get(i1).toString()))
					{
						data.put(i1, 0);
					}
				}
				int[] coords = { data.getInt(0), data.getInt(1), data.getInt(2), data.getInt(3) };

				Map<Rectangle, String> textCoordMap = new TreeMap<Rectangle, String>();
				if (textMap.containsKey(nodeId))
				{
					textCoordMap = textMap.get(nodeId).descendingMap();
				}

				Map<String, String> attributes = getAttributes(nodeData);
				CSSParser cssParser = CSSParser.getInstance(filePath, false);
				Map<String, String> cssMap = cssParser.getCSSPropertiesForElement(xpath);

				boolean visible = nodeData.getInt("visible") == 1? true : false;
				if(visible)
				{
					// additional coordinate checks for visibility
					if (coords[0] < 0 || coords[1] < 0 || coords[2] <= 0 || coords[3] <= 0) 
					{
						visible = false;
				    }
				    if (coords[2] - coords[0] <= 0 || coords[3] - coords[1] <= 0)	// width or height is Zero 
				    {
				    	visible = false;
				    }
				}
				
				HtmlElement e = new HtmlElement();
				e.setXpath(xpath);
				e.setId(id);
				e.setTagName(tagName);
				e.setCoord(new Rectangle(coords[0], coords[1], (coords[2] - coords[0]), (coords[3] - coords[1])));
				e.setVisible(visible);
				e.setAttributes(attributes);
				e.setCssMap(cssMap);
				e.setTextContent(new ArrayList<>(textCoordMap.values()));
				e.setTextCoords(new ArrayList<>(textCoordMap.keySet()));

				if (pid == -1)
				{
					// root of the DOM tree
					root = new Node<HtmlElement>(null, e);
					tempMapToCreateDomTree.put(nodeId, root);
				}
				else
				{
					// attach "e" to its respective parent
					Node<HtmlElement> parent = tempMapToCreateDomTree.get(pid);
					Node<HtmlElement> newNode = new Node<HtmlElement>(parent, e);
					tempMapToCreateDomTree.put(nodeId, newNode);
				}
			}
		}
		System.out.println("Number of elements in the DOM tree = " + numberOfHtmlElements);
	}

	private static Map<String, String> getAttributes(JSONObject object)
	{
		Map<String, String> attributes = new HashMap<String, String>();
		try
		{
			JSONObject attr = object.getJSONObject("attributes");
			Iterator<String> it = attr.keys();
			while (it.hasNext())
			{
				String key = it.next();
				String value = attr.getString(key);
				attributes.put(key.toLowerCase(), URLDecoder.decode(value, "UTF-8"));
			}
			return attributes;
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public Node<HtmlElement> searchHtmlDomTreeByXpath(String xpath)
	{
		Queue<Node<HtmlElement>> q = new LinkedList<Node<HtmlElement>>();
		q.add(root);
		
		while(!q.isEmpty())
		{
			Node<HtmlElement> node = q.remove();
			if(node.getData().getXpath().equalsIgnoreCase(xpath))
			{
				return node;
			}
			if (node.getChildren() != null)
			{
				for (Node<HtmlElement> child : node.getChildren())
				{
					q.add(child);
				}
			}
		}
		return root;
	}
	
	public void preOrderTraversalDomTree()
	{
		preOrderTraversalDomTree(root);
	}

	public boolean isElementPresent(String xpath)
	{
		Queue<Node<HtmlElement>> q = new LinkedList<Node<HtmlElement>>();
		q.add(root);
		
		while(!q.isEmpty())
		{
			Node<HtmlElement> node = q.remove();
			if(node.getData().getXpath().equalsIgnoreCase(xpath))
			{
				return true;
			}
			if (node.getChildren() != null)
			{
				for (Node<HtmlElement> child : node.getChildren())
				{
					q.add(child);
				}
			}
		}
		return false;
	}
	
	private void preOrderTraversalDomTree(Node<HtmlElement> node)
	{
		if (node == null)
		{
			return;
		}
		System.out.println(node.getData().getTagName() + ": " + node.getData());
		if (node.getChildren() != null)
		{
			for (Node<HtmlElement> child : node.getChildren())
			{
				preOrderTraversalDomTree(child);
			}
		}
	}

	public List<String> getMatchingElements(String textSnippet)
	{
		WebDriverSingleton.loadPage(MFix.getFilepath());
		buildHtmlDomTree();

		List<String> matchingElements = new ArrayList<>();

		// sanitize the text snippet
		textSnippet = Util.getSanitizedString(textSnippet);

		// breadth first search over the html dom tree
		Queue<Node<HtmlElement>> q = new LinkedList<Node<HtmlElement>>();
		q.add(root);

		while (!q.isEmpty())
		{
			Node<HtmlElement> node = q.remove();
			HtmlElement e = node.getData();

			if (isMatch(textSnippet, e))
			{
				matchingElements.add(e.getXpath());
			}

			if (node.getChildren() != null)
			{
				for (Node<HtmlElement> child : node.getChildren())
				{
					q.add(child);
				}
			}
		}
		return matchingElements;
	}

	private static boolean isMatch(String textSnippet, HtmlElement e)
	{
		String text = "";
		for(String t : e.getTextContent())
		{
			text = text + t;
		}
		
		// check if the text snippet has a tagname
		if (textSnippet.startsWith("<"))
		{
			return isTagInformationMathch(textSnippet, e);
		}
		// check if the text snippet has "..."
		else if (textSnippet.contains("…"))
		{
			return isPartialMatch(textSnippet, text);
		}
		// text snippet contains the exact text
		else
		{
			return isExactMatch(textSnippet, text);
		}
	}

	private static boolean isTagInformationMathch(String textSnippet, HtmlElement e)
	{
		// get substring within the angular brackets <........>
		textSnippet = textSnippet.substring(1, textSnippet.indexOf(">"));

		String textSnippetArr[] = textSnippet.split(" ");

		// get tagname
		String tagname = textSnippetArr[0];
		if (e.getTagName().equalsIgnoreCase(tagname))
		{
			// get attributes
			for (int i = 1; i < textSnippetArr.length; i++)
			{
				if (textSnippetArr[i].contains("="))
				{
					String attributeArr[] = textSnippetArr[i].split("=");
					if (e.getAttributes().containsKey(attributeArr[0]))
					{
						String attributeValueFromElement = e.getAttributes().get(attributeArr[0]);
						String attributeValueFromTextSnippet = attributeArr[1].replaceAll("\"", "");

						if (isExactMatch(attributeValueFromTextSnippet, attributeValueFromElement) || isPartialMatch(attributeValueFromTextSnippet, attributeValueFromElement))
						{
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private static boolean isExactMatch(String textSnippet, String elementText)
	{
		return elementText.contains(textSnippet);
	}

	private static boolean isPartialMatch(String textSnippet, String elementText)
	{
		String textSnippetArr[] = textSnippet.split("…");
		// check for matching of string before "..." followed by the after part
		if (elementText.contains(textSnippetArr[0]))
		{
			int startIndex = elementText.indexOf(textSnippetArr[0]);
			String textSubstring = elementText.substring(startIndex);
			if (textSubstring.contains(textSnippetArr[1]))
			{
				return true;
			}
		}
		return false;
	}

	public Node<HtmlElement> getRoot()
	{
		return root;
	}

	public List<HtmlElement> getLeaves()
	{
		List<HtmlElement> leafElements = new ArrayList<HtmlElement>();
		
		// depth first traversal: preorder
		Stack<Node<HtmlElement>> stack = new Stack<Node<HtmlElement>>();
		stack.add(root);
		
		while(!stack.isEmpty())
		{
			Node<HtmlElement> node = stack.pop();
			HtmlElement e = node.getData();
			
			if (node.getChildren() != null)
			{
				for (Node<HtmlElement> child : node.getChildren())
				{
					stack.add(child);
				}
				
				// check if the element is a text node (the parent element might not necessarily be a leaf node)
				if(e.isVisible() && e.getTextCoords().size() > 0)
				{
					leafElements.add(e);
				}
			}
			else
			{
				// general leaf nodes
				if(e.isVisible())
				{
					// further filtration criteria: consider element as not visible if it has no text, image, or background-image (e.g. it is a spacer div)
					if(e.getTextCoords().size() > 0 || 
							e.getTagName().equalsIgnoreCase("img") || 
							e.getCssMap().containsKey("background-image") ||
							e.getTagName().equalsIgnoreCase("input") ||
							e.getTagName().equalsIgnoreCase("button"))
					{
						leafElements.add(e);
					}
				}
			}
		}
		return leafElements;
	}
}
