package mfix.domTree;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.xerces.parsers.DOMParser;
import org.cyberneko.html.HTMLConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.NodeData;
import cz.vutbr.web.domassign.StyleMap;

public class CSSParser
{
	private static String fileFullPath;
	private InputStream is;
	private Document doc;
	private String charset;
	private StyleMap styleMap;

	private static CSSParser instance = null;
	private static boolean isInheritance;

	private CSSParser(String fileFullPath, boolean isInheritance)
	{
		System.out.println("Parsing CSS for " + fileFullPath);
		CSSParser.fileFullPath = fileFullPath;
		CSSParser.isInheritance = isInheritance;
		try
		{
			parseCSS();
		}
		catch (SAXException | IOException e)
		{
			e.printStackTrace();
		}
	}

	public static CSSParser getInstance(String fileFullPath, boolean isInheritance)
	{
		if (instance == null || !CSSParser.fileFullPath.equalsIgnoreCase(fileFullPath) || CSSParser.isInheritance != isInheritance)
		{
			instance = new CSSParser(fileFullPath, isInheritance);
		}
		return instance;
	}

	public static void resetInstance()
	{
		instance = null;
	}

	public InputStream getInputStream()
	{
		return is;
	}

	private void parse() throws SAXException, IOException
	{
		this.is = new FileInputStream(fileFullPath);
		DOMParser parser = new DOMParser(new HTMLConfiguration());
		parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
		if (charset != null)
			parser.setProperty("http://cyberneko.org/html/properties/default-encoding", charset);
		parser.parse(new org.xml.sax.InputSource(is));
		doc = parser.getDocument();
	}

	private static URL createBaseFromFilename(String filename)
	{
		try
		{
			File f = new File(filename);
			return f.toURI().toURL();
		}
		catch (MalformedURLException e)
		{
			return null;
		}
	}

	public void parseCSS() throws SAXException, IOException
	{
		parse();

		// include inherited properties
		styleMap = CSSFactory.assignDOM(doc, "utf-8", createBaseFromFilename(fileFullPath), "screen", isInheritance);
	}

	public Map<String, String> getCSSPropertiesForElement(String xpathExpression)
	{
		Map<String, String> cssProperties = new HashMap<String, String>();

		Element e = null;
		try
		{
			e = getW3CElementFromXPathJava(xpathExpression, doc);
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}

		if (e != null)
		{
			NodeData data = styleMap.get(e);

			// process data
			if (data != null)
			{
				String[] rules = data.toString().split(";");

				for (int i = 0; i < rules.length - 1; i++)
				{
					String[] rule = rules[i].split(":\\s");
					if (rule.length == 2)
					{
						String prop = rule[0].trim();
						String val = rule[1].trim();
						if (!val.isEmpty())
						{
							cssProperties.put(prop, val);
						}
					}
				}
			}
		}
		return cssProperties;
	}

	public org.w3c.dom.Element getW3CElementFromXPathJava(String xPath, org.w3c.dom.Document doc) throws IOException
	{
		String xPathArray[] = xPath.split("/");
		ArrayList<String> xPathList = new ArrayList<String>();

		for (int i = 0; i < xPathArray.length; i++)
		{
			if (!xPathArray[i].isEmpty())
			{
				xPathList.add(xPathArray[i]);
			}
		}

		org.w3c.dom.Element foundElement = null;
		org.w3c.dom.NodeList elements;
		int startIndex = 0;

		String id = getElementId(xPathList.get(0));
		if (id != null && !id.isEmpty())
		{
			foundElement = doc.getElementById(id);
			if (foundElement == null)
				return null;
			elements = foundElement.getChildNodes();
			startIndex = 1;
		}
		else
		{
			elements = doc.getElementsByTagName(xPathList.get(0).replaceFirst("\\[(.+)\\]", ""));
		}
		for (int i = startIndex; i < xPathList.size(); i++)
		{
			String xPathFragment = xPathList.get(i);
			int index = getSiblingIndex(xPathFragment);
			boolean found = false;

			// strip off sibling index in square brackets
			xPathFragment = xPathFragment.replaceFirst("\\[(.+)\\]", "");

			for (int j = 0; j < elements.getLength(); j++)
			{
				if (elements.item(j).getNodeType() != Node.ELEMENT_NODE)
				{
					continue;
				}

				org.w3c.dom.Element element = (org.w3c.dom.Element) elements.item(j);

				if (found == false && xPathFragment.equalsIgnoreCase(element.getTagName()))
				{
					// check if sibling index present
					if (index > 1)
					{
						int siblingCount = 0;

						for (org.w3c.dom.Node siblingNode = element.getParentNode().getFirstChild(); siblingNode != null; siblingNode = siblingNode.getNextSibling())
						{
							if (siblingNode.getNodeType() != Node.ELEMENT_NODE)
							{
								continue;
							}

							org.w3c.dom.Element siblingElement = (org.w3c.dom.Element) siblingNode;
							if ((siblingElement.getTagName().equalsIgnoreCase(xPathFragment)))
							{
								siblingCount++;
								if (index == siblingCount)
								{
									foundElement = siblingElement;
									found = true;
									break;
								}
							}
						}
						// invalid element (sibling index does not exist)
						if (found == false)
							return null;
					}
					else
					{
						foundElement = element;
						found = true;
					}
					break;
				}
			}

			// element not found
			if (found == false)
			{
				return null;
			}

			elements = foundElement.getChildNodes();
		}
		return foundElement;
	}

	private int getSiblingIndex(String xPathElement)
	{
		String value = getValueFromRegex("\\[(.+)\\]", xPathElement);
		if (value == null)
			return 1;
		return Integer.parseInt(value);
	}

	private String getElementId(String xPathElement)
	{
		return getValueFromRegex("\\*\\[@id=['|\"]?(.+[^'\"])['|\"]?\\]", xPathElement);
	}

	public String getValueFromRegex(String regex, String str)
	{
		Pattern p = Pattern.compile(regex, Pattern.DOTALL);
		Matcher m = p.matcher(str);
		if (m.find())
		{
			return m.group(1);
		}
		return null;
	}
}