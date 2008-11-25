package com.naildrivin5.fauxml.ant;

import java.util.*;
import java.io.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.*;
import com.naildrivin5.fauxml.*;

public class FauXMLTask extends Task
{
    private List<FileSet> itsFileSets;
    private String itsExtension = "xml";
    private boolean itsFailOnError = true;
    private boolean itsFailOnUndefinedProperties = false;
    private Map<String,String> itsProperties;

    public void setFailOnError(boolean b) { itsFailOnError = b; }
    public boolean getFailOnError() { return itsFailOnError; }
    public void setFailOnUndefinedProperties(boolean b) { itsFailOnUndefinedProperties = b; }
    public boolean getFailOnUndefinedProperties() { return itsFailOnUndefinedProperties; }

    public FauXMLTask()
    {
        itsFileSets = new ArrayList<FileSet>();
        itsProperties = new HashMap<String,String>();
    }

    public void execute()
        throws BuildException
    {
        try
        {
            for (FileSet f: itsFileSets)
            {
                DirectoryScanner scanner = f.getDirectoryScanner(getProject());
                for (String file: scanner.getIncludedFiles())
                {
                    String outputFile = getOutputFile(file);
                    Parser p = new Parser(new FileInputStream(new File(file)),itsProperties);
                    Generator g = new Generator(new PrintStream(new File(outputFile)));
                    Node parsed = p.parse();
                    if ( (parsed == null) || (p.getErrorMessages().size() > 0) )
                    {
                        handleErrorOutput("Parsing of " + file + " Failed");
                        for (String s : p.getErrorMessages())
                        {
                            handleErrorOutput(s);
                        }
                        if (getFailOnError())
                        {
                            throw new BuildException("Parsing of " + file + " Failed");
                        }
                    }
                    for (String var: p.getUnusedProperties().keySet())
                    {
                        int line = p.getUnusedProperties().get(var);
                        handleErrorOutput("Property " + var + " on line " + line + " of " + file + " was not defined");
                    }
                    if ( (p.getUnusedProperties().size() > 0) && getFailOnUndefinedProperties() )
                        throw new BuildException(file + " had undefined variables");
                    if (parsed != null)
                        g.generate(parsed);
                }
            }
        }
        catch (IOException ioe)
        {
            throw new BuildException(ioe);
        }
    }

    private String getOutputFile(String inputFile)
    {
        int i = inputFile.lastIndexOf('.');
        if (i != -1)
        {
            inputFile = inputFile.substring(0,i);
        }
        return inputFile + "." + getExtension();
    }

    public String getExtension() { return itsExtension; }
    public void setExtension(String extension) { itsExtension = extension; }

    public void addFileset(FileSet set) { itsFileSets.add(set); }

    public void addConfiguredProperty(Property p)
    {
        if (p.getName() != null)
            itsProperties.put("$" + p.getName(),p.getValue());
    }
    
}
