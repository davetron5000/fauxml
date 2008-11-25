package com.naildrivin5.fauxml;

import java.io.*;
import java.util.*;

/** Generates XML from a Node
 */
public class Generator
{
    private PrintStream itsPrintStream;

    /** Create a Generator
     * @param os an OutputStream where the XML should go
     */
    public Generator(OutputStream os)
    {
        itsPrintStream = new PrintStream(os);
    }

    /** Generates the XML onto the output stream
     * @param node the node from which XML should be generated
     */
    public void generate(Node node)
    {
        StringBuilder indent = new StringBuilder("");
        for (int j=0;j<node.itsDepth; j++) indent.append(" ");

        itsPrintStream.print(indent);
        itsPrintStream.print("<");
        itsPrintStream.print(node.itsName);
        if (node.itsAttributes.size() > 0)
            itsPrintStream.print(" ");
        for (Node.Attribute a: node.itsAttributes)
        {
            itsPrintStream.print(a.itsName);
            itsPrintStream.print("=\"");
            itsPrintStream.print(a.itsValue);
            itsPrintStream.print("\" ");
        }
        if ( (node.itsChildren.size() == 0) && (node.itsText.size() == 0) )
        {
            itsPrintStream.println("/>");
        }
        else
        {
            itsPrintStream.print(">");
            for (int i=0;i<node.itsText.size(); i++)
            {
                String t = node.itsText.get(i).trim();
                itsPrintStream.print(t);
                if ( (node.itsText.size() != 1) && (i != (node.itsText.size() - 1)) )
                    itsPrintStream.println();
            }
            if (node.itsText.size() == 0)
                itsPrintStream.println();
            for (Node child: node.itsChildren)
            {
                generate(child);
            }
            if (node.itsText.size() == 0)
                itsPrintStream.print(indent);
            itsPrintStream.print("</");
            itsPrintStream.print(node.itsName);
            itsPrintStream.println(">");
        }
    }
}
