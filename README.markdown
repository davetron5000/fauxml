# FauXML

FauXML is a way to create hierarchical data that should be turned into XML in a way that doesn't require all the noise and quoting of XML, with a few features to make it a bit simpler.  This is **not** a new serialization language, but a way to facilitate hand-creating XML for Java applications

# Overview

Many Java technologies require XML configuration files.  While XML is suitable for data 
interchange, it is not very good at configuration.  Authoring and maintaining such files is 
difficult and error-prone, mostly due to the strict requirements of an XML document.  

FauXML is a simplified way of creating and maintaining these files.  The signal-to-noise ratio is very high, and a rudimentary property system allows for the removal of redunant information.

Here's an example, based on the J2EE web application configuration:

    <web-app>
        <display-name>Prototype</display-name>
        <servlet id="1">
            <servlet-name>action</servlet-name>
            <servlet-class>org.apache.struts.action.ActionServlet</servlet-class>
            <init-param>
                <param-name>config</param-name>
                <param-value>/WEB-INF/struts-config.xml</param-value>
            </init-param>
            <load-on-startup>1</load-on-startup>
        </servlet>
        <servlet-mapping>
            <servlet-name>action</servlet-name>
            <url-pattern>*.do</url-pattern>
        </servlet-mapping>
    </web-app>

As with most XML configuration files, it's very difficult to pick out the actual information as the signal to noise ratio is very low.  Indenting helps, but even a moderately-sized XML file can be difficult to navigate without special editing tools.  The reason is the verbosity of XML, with angle brackets, closing tags and quotes interspersed amongst the data.  If we make a few conventions in our formatting, this information becomes superfluous and can be removed.  FauXML can reconstruct it using context to eliminate errors related to quoting, tag closing and repetition of values.  Following is a FauXML document that can be translated into equivalent XML document above:

    $name=action
    $package=org.apache.struts.$name
    #
    web-app
        display-name Prototype
        servlet
            id=1
            servlet-name $name
            servlet-class ${package}.ActionServlet
            init-param
                param-name config
                param-value /WEB-INF/struts-config.xml
            load-on-startup 1
        servlet-mapping
            servlet-name $name
            url-pattern *.do

This demonstrates the major features and benefits of FauXML:

* Easier to read the information/content
* Properties allow for compile-time checking of information (mistyping the servlet-name can be detected way before deployment, in this case)
* The hierarchy of the information is not only obvious, but required (XML has no indentation requirements, making it even more difficult to read)

While this may not seem like a huge gain for such a simple file, you will find that authoring more complex XML documents such as ANT build files or Spring bean configuration files is much simpler and much less error-prone by using FauXML.  There is an ANT task included that will automatically translate your documents to XML.  

It is also important to understand that *this is not a new file format* but rather a simplified means of creating XML.  The purpose of FauXML is to reduce the time and effort it takes to create and maintain XML files required by your tools and software and *not* to replace them in the software you build
(you should consider YAML for that).

## Syntax

FauXML uses whitespace and line termination to determine the structure of a document.  A FauXML document has two parts, a preamble where properties are declared and defined, and a content area where the actual information is stored.  To mirror the hierarchical nature of XML, indenting is used to communicate parent/child and sibling relationships.  The children of a tag can be attributes, raw content or more tags (or all three).  Properties can be used in attribute values and raw data.  Comments can be embedded as well (these are not passed on to the resulting XML document)

### BNF ###

The parser is currently not implemented based on a formal definition, however the following BNF should adequately describe what is possibe.  The idea of FauXML is that it's instantly obvious what is going on and what information is being provided.

    <fauxml-document> ::= <line> | <line><linebreak><fauxml-document>

    <line> ::= <property-declaration> | <comment> | <tag-declaration> 
            | <attribute-declaration-line> | <tag-content-line>
            | <implicit-property-declaration>


    <property-declaration> ::= "$"<property-name>=<property-value>

    <property-name> ::= <alphanumeric> | <alphanumeric><property-name>
    <property-value> ::= <string>

    <implicit-property-declaration> ::= "#IMPLICIT $"<property-name>

    <comments> ::= "#"<string>

    <tag-declaration> ::= <indent><tag-characters> | <indent><tag-characters> <tag-content>

    <atribute-declaration-line> ::= <indent><attribute-declaration>
    <attribute-declaration> :: = <tag-characters>=<string>

    <tag-content-line> ::= <indent>"|"<tag-content>
    <tag-content> ::= <string>

    <indent> ::= <empty-string> | " "<indent>
    <tag-characters> ::= <alphanumeric> | <alphanumeric><tag-characters>

You'll note that some sequences can match multiple tokens above.  In most cases, the sensible approach is taken (for example if a line begins with a pipe and contains and equals sign, it is assumed to be raw content)

## Semantics ##

Based on the syntax above, each line is of one of six types:

1. A property declaration - This sets the value of a property
2. A comment - This is unparsed text that will be discarded
3. A tag declaration - This is essentially an "open tag" in XML; it indicates the start of a new node or element.  This can optionally contain some content 
4. An attribute declaration - This is the child of a tag declaration and results in setting an attribute for that tag
5. A content declaration - This is basically raw text inside a tag.
6. An implicit property declaration - This is useful mostly for ANT files, where the ultimate user of the XML file makes certain properties available via a syntax simliar to FauXML's; this will keep the parser from warning you about an undeclared property
 
Beyond just the types of lines, the structure of the document communicates its meaning.  An XML document is hierarchical.  FauXML encodes this hierarchy with whitespace indenting.  The data encoded in a tag is either an attribute or tag contents (which can be raw text and/or other tags).  Consider the following FauXML:

    root
        child
            |contents
        sibling
            child sex=male

There are a total of four elements: root, child, sibling, and a second child.  The children of root are, as  you'd expect, the first child and sibling.  child has a raw text content of the string "contents".  Sibling contains only another elemnt (the second child).  That child's contents are only an attribute.  The XML would be:

    <root>
        <child>contents</child>
        <sibling>
            <child sex="male" />
        </sibling>
    </root>

An important thing to note is that since FauXML is geared toward XML data that is edited/maintained by humans (as opposed to
generated and parsed by a machine), the semantics make certain assumptions.  While the FauXML above contains 6 tokens total (the four tags, plus the raw content, plus the attribute of the second child), the XML document it produces would be considered to contain 10 tokens:
1. The root tag
1. The space between it and the child tag
1. The child tag
1. The child tag's contents
1. The space between the closing child tag and the sibling tag
1. The space after the sibling tag
1. The second child tag
1. The child tag's attribute
1. The space after the child tag
1. The space after the sibling's close tag and before the root' close tag

An XML document more equivalent the FauXML would be:

    <root><child>contents</child><sibling><child sex="male" /></sibling></root>

This could certainly be genetated, however, the assumption is that most usage patterns of such XML files would ignore the whitespace
elements (and all-on-one-line XML files are not conducive to version control, should you choose to version control your generated files).

### Properties ###

The second significant piece of FauXML is the ability to create and use properties.  Since XML is hierarchical, but much configuration
information is either relational or poorly designed (as is the case with the J2EE web.xml file), there tends to be a lot of repeated
magic strings, many of which are not checked until the XML file is actually used in context.  By allowing a rudimentary system of
properties, the author can be alterted to typos or other errors.  It can also save some typing.  

The property system is similar to ANT's and extremely simple.  Properties are declared with a line like:

    $property=value

A property can be used thusly:

    some_tag
        |The contents of this tag are $property, or possibly ${property}right?

For convienience, all Java standard system properties are available for use.

In the case of ANT build files, the property referencing sytax is similar to the FauXML syntax.  Some properties are provided by the XML file's user and we don't want to be warned about them.  In this case, we can set those properties as implicit:

    #IMPLICIT ${basedir}

You can also do

    #IMPLICIT $ANT_BUILTIN

which is a special name that will mark as implicit all properties ANT defines for you.

### Raw Content ###

Raw content can be placed in a tag in two ways.  For small one-line content, it can follow the tag name on the same line:

    servlet-name struts

For multi-line content, the content is indented inside the tag and each line preceded with a pipe:

    description
         |This is a servlet used
         |for the struts action
         |framework

This will preserve the newlines:

    <description>This is a servlet used
    for the struts action
    framework</description>

#### Using markup inside content
Raw content is placed verbatim (minus the leading pipes) into the output file, so you can put XML in there, if you'd like:

    description
         |This is a <i>servlet</i> used
         |for the <b>struts action framework</b>

You are free to put invalid XML in a raw content block, no checking is done.  Because of this, you are also free
to encode your raw content in a CDATA block:

    description
         |<![CDATA[This is <i>servlet</i> used
         |for the struts framework<br>]]>

results in:

    <description><![CDATA[This is <i>servlet</i> used
    for the struts framework<br>]]></description>

## Examples ##

### ANT Build file ###

Here is how you might create a simple ANT build file:

    #IMPLICIT $ANT_BUILTIN

    $srcdir=${basedir}/src/java
    $classesdir=${basedir}/build/classes
    $compile=compile

    project
        name=uxml
        default=all
        basedir=.
        description Build file for UXML
        path
            id=build.compile.classpath
            fileset
                includes=*.jar
                dir=${basedir}/lib
        target
            name=all
            depends=$compile
        target
            name=$compile
            depend
                srcdir=$srcdir
                destdir=$classesdir
                closure=yes
                cache=depcache
            javac
                deprecation=on
                srcdir=$srcdir
                destdir=$classesdir
                debug=on
                classpathref=build.compile.classpath
            copy
                todir=$classesdir
                fileset
                    dir=${basedir}/config

This will result in the following XML:

    <project name="uxml" default="all" basedir="." >
        <description>Build file for UXML</description>
        <path id="build.compile.classpath" >
            <fileset includes="*.jar" dir="${basedir}/lib" />
        </path>
        <target name="all" depends="compile" />
        <target name="compile" >
            <depend srcdir="${basedir}/src/java" destdir="{$basedir}/build/classes" closure="yes" cache="depcache" />
            <javac deprecation="on" srcdir="${basedir}/src/java" destdir="${basedir}/build/classes" debug="on" classpathref="build.compile.classpath" />
            <copy todir="${basedir}/build/classes" >
                <fileset dir="${basedir}/config" />
            </copy>
        </target>
    </project>

It is important to note that while FauXML accepts a non-braced reference, ANT does not.  If you had used:

    srcdir=$basedir/src

your ant file would not work.  FauXML currently doesn't canonicalize your property references for ANT.

### Spring beans.xml ###

Spring requires a lot of fully-qualified class refencing and relies heavily on magic strings to relate objects to each other.  It can be frustrating to discover a typo at runtime.  FauXML's properties and compile-time syntax checking can identify these types of bugs quickly (and save a lot of typing)

    $package=com.naildrivin5.todo.controller

    beans
        xmlns=http://www.springframework.org/schema/beans
        xmlns:xsi=http://www.w3.org/2001/XMLSchema-instance
        xsi:schemaLocation=http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.0.xsd
        # This one is for in memory
        bean
            id=inMemTodoDataSource
            parent=abstractTodoDataSource
            class=$package.InMemoryTodoDataSource
            constructor-arg 
                # Allows us to switch out locks
                ref=readWriteLock
        bean
            id=todoDataSource
            parent=abstractTodoDataSource
            class=$package.SerializedTodoDataSource
            constructor-arg 
                ref=readWriteLock
            constructor-arg 
                ref=serializedDataStore
        bean
            id=serializedDataStore
            class=java.io.File
            scope=prototype
            constructor-arg 
                type=java.lang.String
                value=db.ser

The resulting XML:

    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.0.xsd" >
        <bean parent="abstractTodoDataSource" id="inMemTodoDataSource" class="com.naildrivin5.todo.controller.InMemoryTodoDataSource" >
            <constructor-arg ref="readWriteLock" />
        </bean>
        <bean parent="abstractTodoDataSource" id="todoDataSource" class="com.naildrivin5.todo.controller.SerializedTodoDataSource" >
            <constructor-arg ref="readWriteLock" />
            <constructor-arg ref="serializedDataStore" />
        </bean>
        <bean id="serializedDataStore" class="java.io.File" scope="prototype" >
            <constructor-arg value="db.ser" type="java.lang.String" />
        </bean>
    </beans>

## Usage ##

The easiest way to use FauXML is via the included ANT task.  It takes a list of FauXML files and creates their XML equivalent in the same directory.  So, the best practice is to put your FauXML files wherever you would normally put your XML configuration files and have FauXML create them at the start of your build (or prior to whever they get used).

You can also programmatically use the parser and XML generator if you wish.

## Ant Task ##

Included with the parser is com.naildrivin5.applications.uxml.ant.FauXMLTask.  You can use it in your build file like so:

    <taskdef name="usexml" classname="com.naildrivin5.applications.fauxml.ant.FauXMLTask" classpath="${basedir}/build/classes" />

It has three properties:

* **`extension`** - The file extension to use, defaults to "xml" (the dot is not needed)
* **`failonerror`** - True if any error in parsing should abort the task and fail (default true)
* **`failonundefinedproperties`** - If true, any usage of an undefined (or non-implicit) property will cause failure (default false).  Undefined property warnings are printed out regardless of this setting

The fauxml task should contain at least one fileset to indicate which files to process.  All files are parsed and XML generated using the same basename, but with the configured extension.  You may also set properties using property tags.  These will be available to the FauXML files.

### Typical Usage

An example of a typical usage of the fauxml task that sets the property "foo" to "bar"

    <target name="parse">
        <fauxml>
            <property name="foo" value="bar" />
            <fileset dir="." includes="**/*.fauxml" />
        </fauxml>
    </target>

### Overriding Defaults

Here is how you might create Hibernate mapping files and use a different extension for the output as well as to fail on any undefined properties.

    <target name="parse">
        <fauxml extension="hbm.xml" failonundefinedproperties="yes">
            <fileset dir="meta/hibernate/mapping" includes="*.fauxml" />
        </fauxml>
    </target>
