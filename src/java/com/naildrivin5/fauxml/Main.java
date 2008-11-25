package com.naildrivin5.fauxml;

import java.util.*;
import java.io.*;

public class Main
{
    public static void main(String args[])
        throws Exception
    {
        if (args.length < 1)
        {
            System.err.println("Usage: " + Main.class.getName() + " fauxml_file [xml_file]");
            System.exit(-1);
        }

        List<File> filesToProcess = new ArrayList<File>(args.length);
        boolean errors = false;
        File f = new File(args[0]);
        if ( (f.exists()) && (f.canRead()) )
        {
            filesToProcess.add(f);
        }
        else
        {
            errors = true;
            System.err.println("Cannot read " + args[0]);
        }
        if (errors)
        {
            System.err.println("There were errors.  Exiting without processing...");
            System.exit(-2);
        }

        Parser p = new Parser(new FileInputStream(new File(args[0])));
        PrintStream stream;
        if (args.length == 1)
            stream = System.out;
        else
            stream = new PrintStream(new File(args[1]));
        Generator g = new Generator(stream);
        g.generate(p.parse());
    }
}
