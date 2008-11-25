package com.naildrivin5.fauxml;

import java.util.*;
import java.util.regex.*;
import java.io.*;

/** A parser that, given an input stream of FAUXML, returns a Node, which contains the parsed info, sufficient
 * to generate XML
 */
public class Parser
{
    private static final String ANT_SPECIAL_IMPLICIT = "$ANT_BUILTIN";
    private static final String[] theAntImplicitProperties = 
    {
        "$basedir",
        "$ant.file",
        "$ant.version",
        "$ant.project.name",
        "$ant.java.version",
        "$ant.home"
    };
    private InputStream itsInputStream;

    private static String thePropertyUsePatternStrings[] = 
    {
        "(\\$\\{[\\w\\d_\\.:]+\\})",
        "(\\$[\\w\\d_\\.:]+)\\s+",
        "(\\$[\\w\\d_\\.:]+)$",
    };

    private static Pattern thePropertyUsePatterns[];

    static
    {
        thePropertyUsePatterns = new Pattern[thePropertyUsePatternStrings.length * 1];

        int i=0;
        for (String s: thePropertyUsePatternStrings)
        {
            thePropertyUsePatterns[i] = Pattern.compile(s);
            i++;
        }
    }


    private Map<String,Integer> itsUnusedProperties;
    private List<String> itsErrorMessages;
    private Set<String> itsImplicitProperties;
    private Map<String,String> itsProperties;

    /** Create a new Parser
     * @param is the inputstream from which to read
     */
    public Parser(InputStream is) 
    {
        this(is,new HashMap<String,String>());
    }
    public Parser(InputStream is, Map<String,String> properties) 
    { 
        if (properties == null)
            throw new IllegalArgumentException("properties may not be null");
        itsInputStream = is; 
        itsProperties = properties;
        itsUnusedProperties = new HashMap<String,Integer>();
        itsErrorMessages = new ArrayList<String>();
        itsImplicitProperties = new HashSet<String>();
        for (Object o: System.getProperties().keySet())
        {
            String var = o.toString();
            String val = System.getProperty(var);
            itsProperties.put("$" + var,val);
        }
    }

    /** Returns a mapping of unused property names to line numbers */
    public Map<String,Integer> getUnusedProperties()
    {
        return Collections.unmodifiableMap(itsUnusedProperties);
    }

    /** Returns a list of error messages encountered during parsing */
    public List<String> getErrorMessages()
    {
        return Collections.unmodifiableList(itsErrorMessages);
    }

    /** Parse the input stream.
     * @return a Node containing the parsed data
     */
    public Node parse()
    {
        try
        {
            LineNumberReader reader = new LineNumberReader(new InputStreamReader(itsInputStream));
            Node root = new Node(null,-1);
            parse(reader,root);
            if (root.itsChildren.size() != 1)
            {
                itsErrorMessages.add("Expected only one root, but got " + root.itsChildren.size() + " instead!");
                return null;
            }
            else
            {
                Node n = root.itsChildren.get(0);
                n.itsParent = null;
                return n;
            }
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
            return null;
        }
    }
    
    
    // $1 is the text
    private Pattern itsTextPattern = Pattern.compile("^\\s*\\|(.*$)");
    // $1 is the var
    // $2 is the value
    private Pattern itsPropertyPattern = Pattern.compile("^(\\$[\\w\\d_\\.:]+)=(.*$)");
    // $1 is the whitespace indent
    // $2 is the tag name
    // $3 is the optional "easy text"
    private Pattern itsElementPattern = Pattern.compile("^(\\s*)([\\w\\-:]+)\\s*(.*)$");

    // $1 is the whitespace indent
    // $2 is the attribute name
    // $3 is the attribute value
    private Pattern itsAttributePattern = Pattern.compile("^(\\s*)([^=\\s]+)=(.*$)");

    // $1 is the whitespace indent
    // $2 is the comment
    private Pattern itsCommentPattern = Pattern.compile("^(\\s*)#(.*$)");

    // $1 is the property
    private Pattern itsImplicitPropertyPattern = Pattern.compile("^#IMPLICIT\\s+(\\$[\\w\\d_\\.:]+)\\s*$");


    private void parse(LineNumberReader reader, Node currentNode)
        throws IOException
    {
        String line = reader.readLine();
        if (line != null)
        {
            if (line.trim().length() > 0)
            {
                Matcher implicitMatcher = itsImplicitPropertyPattern.matcher(line);
                Matcher commentMatcher = itsCommentPattern.matcher(line);
                Matcher textMatcher = itsTextPattern.matcher(line);
                Matcher varMatcher = itsPropertyPattern.matcher(line);
                Matcher elementMatcher = itsElementPattern.matcher(line);
                Matcher attributeMatcher = itsAttributePattern.matcher(line);
                if (implicitMatcher.matches())
                {
                    String var = implicitMatcher.group(1);
                    if (var.equals(ANT_SPECIAL_IMPLICIT))
                    {
                        for (String s: theAntImplicitProperties)
                        {
                            itsImplicitProperties.add(getCanonical(s));
                        }
                    }
                    else
                    {
                        itsImplicitProperties.add(getCanonical(implicitMatcher.group(1)));
                    }
                }
                else if (commentMatcher.matches())
                {
                    // do nothing
                }
                else if (textMatcher.matches())
                {
                    currentNode.addText(replaceProperties(textMatcher.group(1),reader.getLineNumber()));
                }
                else if (varMatcher.matches())
                {
                    String var = varMatcher.group(1);
                    String val = varMatcher.group(2);
                    val = replaceProperties(val,reader.getLineNumber());
                    itsProperties.put(var,val);
                }
                else if (attributeMatcher.matches())
                {
                    String space = attributeMatcher.group(1);
                    String var = attributeMatcher.group(2);
                    String val = attributeMatcher.group(3);
                    currentNode.addAttribute(var,replaceProperties(val,reader.getLineNumber()));
                }
                else if (elementMatcher.matches())
                {
                    String space = elementMatcher.group(1);
                    String element = elementMatcher.group(2);
                    String rest = elementMatcher.group(3).trim();
                    int currentDepth = space.length();

                    Node newNode = new Node(element,currentDepth);
                    if (rest.length() > 0)
                        newNode.addText(replaceProperties(rest,reader.getLineNumber()));

                    if (currentDepth < currentNode.itsDepth)
                    {
                        // Need to find an ancestor with the correct depth 
                        Node newParent = currentNode;
                        while (newParent.itsDepth >= currentDepth)
                        {
                            newParent = newParent.itsParent;
                            if (newParent == null)
                                break;
                        }
                        if (newParent != null)
                        {
                            newNode.itsParent = newParent;
                            newParent.addChild(newNode);
                            currentNode = newNode;
                        }
                        else
                        {
                            if (currentDepth == 0)
                                itsErrorMessages.add("Element on line " + reader.getLineNumber() + " is another root, which isn't allowed");
                            else
                                itsErrorMessages.add( "Couldn't locate an ancestor for element on line " + reader.getLineNumber());
                        }
                    }
                    else if (currentDepth == currentNode.itsDepth)
                    {
                        Node parent = currentNode.itsParent;
                        if (parent != null)
                        {
                            newNode.itsParent = parent;
                            parent.addChild(newNode);
                            currentNode = newNode;
                        }
                        else
                        {
                            if (currentDepth == 0)
                                itsErrorMessages.add("Element on line " + reader.getLineNumber() + " is another root, which isn't allowed");
                            else
                                itsErrorMessages.add("Couldn't locate an ancestor for element on line " + reader.getLineNumber());
                        }
                    }
                    else
                    {
                        currentNode.addChild(newNode);
                        newNode.itsParent = currentNode;
                        currentNode = newNode;
                    }
                }
                else
                {
                    // do nothing
                }
            }
            parse(reader,currentNode);
        }
    }

    private String replaceProperties(String value, int lineNumber)
    {
        value = value + " ";
        for (String var: itsProperties.keySet())
        {
            String val = itsProperties.get(var);
            String var2 = "${" + var.substring(1) + "}";
            value = value.replace(var2,val);

            val += " ";
            var = var + " ";
            value = value.replace(var,val);
        }
        for (Pattern p: thePropertyUsePatterns)
        {
            Matcher propertyMatcher = p.matcher(value);
            while (propertyMatcher.find())
            {
                String var = propertyMatcher.group(1);
                if (!itsImplicitProperties.contains(getCanonical(var)))
                    itsUnusedProperties.put(var,lineNumber);
            }
        }
        return value.trim();
    }

    /** Returns the canonical represenation of this property, which is to say the form
     * with braces around it
     */
    private String getCanonical(String var)
    {
        if (var.charAt(1) != '{')
        {
            return "${" + var.substring(1) + "}";
        }
        else
        {
            return var;
        }
    }
}
