package com.naildrivin5.fauxml;

import java.io.*;
import java.util.*;

/** Holds the basic data of an XML tag, which is a name, a list of attributes, a list of children
 * and any text between the two tags.  Also has some housekeeping items such as depth and parent
 */
public class Node implements Serializable
{
    /** The name of the tag */
    String itsName;
    /** The ordered list of attributes that go in the tag */
    List<Attribute> itsAttributes = new ArrayList<Attribute>();
    /** The child nodes */
    List<Node> itsChildren = new ArrayList<Node>();
    /** Lines of text this tag contains */
    List<String> itsText = new ArrayList<String>();
    /** Depth, which is just a count of spaces/indent level.  This is used primarily to
     * determine where a node is in a tree; if a new element is encountered and it's depth is less than the previously
     * processed one, we use the depth to find it's siblings
     */
    int itsDepth;
    /** Link to the parent of this node (null for the root ) */
    Node itsParent;

    /** Create a new node with the given name and depth
     */
    public Node(String name, int depth)
    {
        itsName = name;
        itsDepth = depth;
    }

    public void addChild(Node child) { itsChildren.add(child); }
    public void addAttribute(String key, String value) { itsAttributes.add(new Attribute(key,value)); }
    public void addText(String s) { itsText.add(s); }

    /** Simple class for storing attributes
     */
    public class Attribute
    {
        public String itsName;
        public String itsValue;
        public Attribute(){}
        public Attribute(String n, String v) 
        { 
            this();
            itsName = n; 
            itsValue = v; 
        }
    }

}
