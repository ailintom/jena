/*****************************************************************************
 * Source code information
 * -----------------------
 * Original author    Ian Dickinson, HP Labs Bristol
 * Author email       Ian.Dickinson@hp.com
 * Package            Jena 2
 * Web                http://sourceforge.net/projects/jena/
 * Created            14-Apr-2003
 * Filename           $RCSfile: VocabGen.java,v $
 * Revision           $Revision: 1.1 $
 * Release status     $State: Exp $
 *
 * Last modified on   $Date: 2003-04-16 11:59:34 $
 *               by   $Author: ian_dickinson $
 *
 * (c) Copyright 2002-2003, Hewlett-Packard Company, all rights reserved.
 * (see footer for full conditions)
 *****************************************************************************/

// Package
///////////////
package jena;


// Imports
///////////////
import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;

import org.apache.oro.text.regex.*;

import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.*;



/**
 * <p>
 * A vocabulary generator, that will consume an ontology or other vocabulary file,
 * and generate a Java file with the constants from the vocabulary compiled in.
 * Designed to be highly flexible and customisable.
 * </p>
 *
 * @author Ian Dickinson, HP Labs
 *         (<a  href="mailto:Ian.Dickinson@hp.com" >email</a>)
 * @version CVS $Id: VocabGen.java,v 1.1 2003-04-16 11:59:34 ian_dickinson Exp $
 */
public class VocabGen {
    // Constants
    //////////////////////////////////

    /** The namespace for the configuration model is {@value} */
    public static final String NS = "http://jena.hpl.hp.com/2003/04/vocabgen#";
    
    /** The default location of the configuration model is {@value} */
    public static final String DEFAULT_CONFIG_URI = "file:vocab.rdf.xxx";
     
    /** The default marker string for denoting substitutions is '{@value}' */
    public static final String DEFAULT_MARKER = "%";
    
    /** Default template for writing out value declarations */
    public static final String DEFAULT_TEMPLATE = "public static final %valclass% %valname% = m_model.%valcreator%( \"%valuri%\" );";
    
    /** Default template for writing out individual declarations */
    public static final String DEFAULT_INDIVIDUAL_TEMPLATE = "public static final %valclass% %valname% = m_model.%valcreator%( %valtype%, \"%valuri%\" );";
    
    /** Default line length for comments before wrap */
    public static final int COMMENT_LENGTH_LIMIT = 80;
    
    /* Constants for the various options we can set */
    
    /** Select an alternative config file; use <code>-c &lt;filename&gt;</code> on command line */
    protected static final Object OPT_CONFIG_FILE = new Object();
    
    /** Turn off all comment output; use <code>--nocomments</code> on command line;  use <code>vocab:noComments</code> in config file */
    protected static final Object OPT_NO_COMMENTS = new Object();
    
    /** Nominate the URL of the input document; use <code>-i &lt;URL&gt;</code> on command line;  use <code>vocab:input</code> in config file */
    protected static final Object OPT_INPUT = new Object();
    
    /** Specify that the language of the source is DAML+OIL; use <code>--daml</code> on command line;  use <code>vocab:daml</code> in config file */
    protected static final Object OPT_LANG_DAML = new Object();
    
    /** Specify that the language of the source is OWL (the default); use <code>--owl</code> on command line;  use <code>vocab:owl</code> in config file */
    protected static final Object OPT_LANG_OWL = new Object();
    
    /** Specify that destination file; use <code>-o &lt;fileName&gt;</code> on command line;  use <code>vocab:output</code> in config file */
    protected static final Object OPT_OUTPUT = new Object();
    
    /** Specify the file header; use <code>--header "..."</code> on command line;  use <code>vocab:header</code> in config file */
    protected static final Object OPT_HEADER = new Object();
    
    /** Specify the file footer; use <code>--footer "..."</code> on command line;  use <code>vocab:footer</code> in config file */
    protected static final Object OPT_FOOTER = new Object();
    
    /** Specify the uri of the configuration root node; use <code>--root &lt;URL&gt;</code> on command line */
    protected static final Object OPT_ROOT = new Object();
    
    /** Specify the marker string for substitutions, default is '%'; use <code>-m "..."</code> on command line; use <code>vocab:marker</code> in config file */
    protected static final Object OPT_MARKER = new Object();
    
    /** Specify the packagename; use <code>--package &lt;packagename&gt;</code> on command line; use <code>vocab:package</code> in config file */
    protected static final Object OPT_PACKAGENAME = new Object();
    
    /** Use ontology terms in preference to vanilla RDF; use <code>--ontology</code> on command line; use <code>vocab:ontology</code> in config file */
    protected static final Object OPT_ONTOLOGY = new Object();
    
    /** The name of the generated class; use <code>-n &lt;classname&gt;</code> on command line; use <code>vocab:classname</code> in config file */
    protected static final Object OPT_CLASSNAME = new Object();
    
    /** Additional decoration for class header (such as implements); use <code>--classdec &lt;classname&gt;</code> on command line; use <code>vocab:classdec</code> in config file */
    protected static final Object OPT_CLASSDEC = new Object();
    
    /** The base URI for the vocabulary; use <code>--base &lt;uri&gt;</code> on command line; use <code>vocab:base</code> in config file */
    protected static final Object OPT_BASE = new Object();
    
    /** Additional declarations to add at the top of the class; use <code>--declarations &lt;...&gt;</code> on command line; use <code>vocab:declarations</code> in config file */
    protected static final Object OPT_DECLARATIONS = new Object();
    
    /** Section declaration for properties section; use <code>--propSection &lt;...&gt;</code> on command line; use <code>vocab:propSection</code> in config file */
    protected static final Object OPT_PROPERTY_SECTION = new Object();
    
    /** Section declaration for class section; use <code>--classSection &lt;...&gt;</code> on command line; use <code>vocab:classSection</code> in config file */
    protected static final Object OPT_CLASS_SECTION = new Object();
    
    /** Section declaration for individuals section; use <code>--individualsSection &lt;...&gt;</code> on command line; use <code>vocab:individualsSection</code> in config file */
    protected static final Object OPT_INDIVIDUALS_SECTION = new Object();
    
    /** Option to suppress properties in vocab file; use <code>--noproperties &lt;...&gt;</code> on command line; use <code>vocab:noproperties</code> in config file */
    protected static final Object OPT_NOPROPERTIES = new Object();
    
    /** Option to suppress classes in vocab file; use <code>--noclasses &lt;...&gt;</code> on command line; use <code>vocab:noclasses</code> in config file */
    protected static final Object OPT_NOCLASSES = new Object();
    
    /** Option to suppress individuals in vocab file; use <code>--noindividuals &lt;...&gt;</code> on command line; use <code>vocab:noindividuals</code> in config file */
    protected static final Object OPT_NOINDIVIDUALS = new Object();
    
    /** Template for writing out property declarations; use <code>--propTemplate &lt;...&gt;</code> on command line; use <code>vocab:propTemplate</code> in config file */
    protected static final Object OPT_PROP_TEMPLATE = new Object();
    
    /** Template for writing out class declarations; use <code>--classTemplate &lt;...&gt;</code> on command line; use <code>vocab:classTemplate</code> in config file */
    protected static final Object OPT_CLASS_TEMPLATE = new Object();
    
    /** Template for writing out individual declarations; use <code>--individualTemplate &lt;...&gt;</code> on command line; use <code>vocab:individualTemplate</code> in config file */
    protected static final Object OPT_INDIVIDUAL_TEMPLATE = new Object();
    
    /** Option for mapping constant names to uppercase; use <code>--uppercase &lt;...&gt;</code> on command line; use <code>vocab:uppercase</code> in config file */
    protected static final Object OPT_UC_NAMES = new Object();
    
    /** Option for including non-local URI's in vocabulary; use <code>--include &lt;uri&gt;</code> on command line; use <code>vocab:include</code> in config file */
    protected static final Object OPT_INCLUDE = new Object();
    
    /** Option for adding a suffix to the generated class name; use <code>--classnamesuffix &lt;uri&gt;</code> on command line; use <code>vocab:classnamesuffix</code> in config file */
    protected static final Object OPT_CLASSNAME_SUFFIX = new Object();
    
    
    // Static variables
    //////////////////////////////////

    
    // Instance variables
    //////////////////////////////////

    /** The list of command line arguments */
    protected List m_cmdLineArgs;
    
    /** The root of the options in the config file */
    protected Resource m_root;
    
    /** The model that contains the configuration information */
    protected Model m_config = ModelFactory.createDefaultModel();
    
    /** The model that contains the input source */
    protected OntModel m_source;
    
    /** The output stream we write to */
    protected PrintStream m_output;
    
    /** Option definitions */
    protected Object[][] m_optionDefinitions = new Object[][] {
        {OPT_CONFIG_FILE,         new OptionDefinition( "-c", null ) },
        {OPT_ROOT,                new OptionDefinition( "-r", null ) },
        {OPT_NO_COMMENTS,         new OptionDefinition( "--nocomments", "noComments" ) },
        {OPT_INPUT,               new OptionDefinition( "-i", "input" ) },
        {OPT_LANG_DAML,           new OptionDefinition( "--daml", "daml" ) },
        {OPT_LANG_OWL,            new OptionDefinition( "--owl", "owl" ) },
        {OPT_OUTPUT,              new OptionDefinition( "-o", "output" ) },
        {OPT_HEADER,              new OptionDefinition( "--header", "header" ) },
        {OPT_FOOTER,              new OptionDefinition( "--footer", "footer" ) },
        {OPT_MARKER,              new OptionDefinition( "-m", "marker" ) },
        {OPT_PACKAGENAME,         new OptionDefinition( "--package", "package" ) },
        {OPT_ONTOLOGY,            new OptionDefinition( "--ontology", "ontology" ) },
        {OPT_CLASSNAME,           new OptionDefinition( "-n", "classname" ) },
        {OPT_CLASSDEC,            new OptionDefinition( "--classdec", "classdec" ) },
        {OPT_BASE,                new OptionDefinition( "--base", "base" ) },
        {OPT_DECLARATIONS,        new OptionDefinition( "--declarations", "declarations" ) },
        {OPT_PROPERTY_SECTION,    new OptionDefinition( "--propSection", "propSection" ) },
        {OPT_CLASS_SECTION,       new OptionDefinition( "--classSection", "classSection" ) },
        {OPT_INDIVIDUALS_SECTION, new OptionDefinition( "--individualsSection", "individualsSection" ) },
        {OPT_NOPROPERTIES,        new OptionDefinition( "--noproperties", "noproperties" ) },
        {OPT_NOCLASSES,           new OptionDefinition( "--noclasses", "noclasses" ) },
        {OPT_NOINDIVIDUALS,       new OptionDefinition( "--noindividuals", "noindividuals" ) },
        {OPT_PROP_TEMPLATE,       new OptionDefinition( "--propTemplate", "propTemplate" ) },
        {OPT_CLASS_TEMPLATE,      new OptionDefinition( "--classTemplate", "classTemplate" ) },
        {OPT_INDIVIDUAL_TEMPLATE, new OptionDefinition( "--individualTemplate", "individualTemplate" ) },
        {OPT_UC_NAMES,            new OptionDefinition( "--uppercase", "uppercase" ) },
        {OPT_INCLUDE,             new OptionDefinition( "--include", "include" ) },
        {OPT_CLASSNAME_SUFFIX,    new OptionDefinition( "--classnamesuffix", "classnamesuffix" )},
    };
    
    /** Stack of replacements to apply */
    protected List m_replacements = new ArrayList();
    
    /** Perl5 pattern compiler */
    protected Perl5Compiler m_perlCompiler = new Perl5Compiler();
    
    /** Perl5 pattern matcher */
    protected PatternMatcher m_matcher = new Perl5Matcher();
    
    /** Local platform newline char */
    protected String m_nl = "\n"; // System.getProperty( "line.separator" );
    
    /** Size of indent step */
    protected int m_indentStep = 4;
    
    /** Set of names used so far */
    protected Set m_usedNames = new HashSet();
    
    /** Map from resources to java names */
    protected Map m_resourcesToNames = new HashMap();
    
    /** List of allowed base URI strings for admissible values */
    protected List m_includeURI = new ArrayList();
    
    
    // Constructors
    //////////////////////////////////

    // External signature methods
    //////////////////////////////////

    /* Main entry point. See Javadoc for details of the many command line arguments */
    public static void main( String[] args ) {
        new VocabGen().go( args );
    }
    
    
    // Internal implementation methods
    //////////////////////////////////

    /** Read the configuration parameters and do setup */ 
    protected void go( String[] args ) {
        // save the command line params
        m_cmdLineArgs = Arrays.asList( args );
        
        // check to see if there's a specified config file
        String configURI = DEFAULT_CONFIG_URI;
        if (hasValue( OPT_CONFIG_FILE )) {
            configURI = getValue( OPT_CONFIG_FILE );
            
            // check for protocol; add file: if not specified
            if (!configURI.startsWith( "http:" )  || !configURI.startsWith( "file:" ) || !configURI.startsWith( "ftp:" )) {
                configURI = "file:" + configURI;
            }
        }
        
        // try to read the config URI
        try {
            m_config.read( configURI );
        }
        catch (Exception e) {
            // if the user left the default config uri in place, it's not an error to fail to read it
            if (!configURI.equals( DEFAULT_CONFIG_URI )) {
                abort( "Failed to read configuration from URI " + configURI, e );
            }
        }
        
        // got the configuration, now we can begin processing
        processInput();
    }
    
    /** The sequence of steps to process an entire file */
    protected void processInput() {
        determineConfigRoot();
        determineLanguage();
        selectInput();
        selectOutput();
        setGlobalReplacements();
        
        processHeader();
        writeClassDeclaration();
        writeInitialDeclarations();
        writeProperties();
        writeClasses();
        writeIndividuals();
        writeClassClose();
        processFooter();
        closeOutput();
    }
    
    /** Determine the root resource in the configuration file */ 
    protected void determineConfigRoot() {
        if (hasValue( OPT_ROOT )) {
            String rootURI = getValue( OPT_ROOT );
            m_root = m_config.getResource( rootURI );
        }
        else {
            // no specified root, we assume there is only one with type vocab:Config
            StmtIterator i = m_config.listStatements( null, RDF.type, m_config.getResource( NS + "Config" ) );
            if (i.hasNext()) {
                m_root = i.nextStatement().getSubject();
            }
            else {
                // no configuration root, so we invent one 
                m_root = m_config.createResource();
            }
        }
        
        // add any extra uri's that are allowed in the filter
        m_includeURI.addAll( getAllValues( OPT_INCLUDE ) );
    }
    
    /** Create the source model after determining which input language */
    protected void determineLanguage() {
        m_source = ModelFactory.createOntologyModel( isTrue( OPT_LANG_DAML ) ? ProfileRegistry.DAML_LANG : ProfileRegistry.OWL_LANG );
        m_source.getDocumentManager().setProcessImports( false );
    }
    
    /** Identify the URL that is to be read in and translated to a vocab file */
    protected void selectInput() {
        if (!hasResourceValue( OPT_INPUT )) {
            abort( "No input document URL specified.", null );
        }
        
        Resource input = getResource( OPT_INPUT );
        
        try {
            m_source.read( input.getURI() );
        }
        catch (RDFException e) {
            abort( "Failed to read input source " + input, e );
        }
    }
    
    /** Identify the file we are to write the output to */
    protected void selectOutput() {
        String outFile = getValue( OPT_OUTPUT );
        
        if (outFile == null) {
            m_output = System.out;
        }
        else {
            try {
                File out = new File( outFile );
                
                if (out.isDirectory()) {
                    // create a file in this directory named classname.java
                    String fileName = outFile + System.getProperty( "file.separator" ) + getClassName() + ".java";
                    out = new File( fileName ); 
                }
                
                m_output = new PrintStream( new FileOutputStream( out ) );
            }
            catch (Exception e) {
                abort( "I/O error while trying to open file for writing: " + outFile, null );
            }
        }
    }
    
    /** Process the header at the start of the file, if defined */
    protected void processHeader() {
        String header = getValue( OPT_HEADER );
        
        if (header != null) {
            writeln( 0, substitute( header ) );
        }
        else {
            // we have to do the imports at least
            writeln( 0, "import com.hp.hpl.jena.rdf.model.*;" );
            if (isTrue( OPT_ONTOLOGY )) {
                writeln( 0, "import com.hp.hpl.jena.ontology.*;" );
            }
        }
    }
    
    /** Process the footer at the end of the file, if defined */
    protected void processFooter() {
        String footer = getValue( OPT_FOOTER );
        
        if (footer != null) {
            writeln( 0, substitute( footer ) );
        }
    }
    
    /** The list of replacements that are always available */
    protected void setGlobalReplacements() {
        addReplacementPattern( "date", new SimpleDateFormat( "dd MMM yyyy HH:mm").format( new Date() ) );
        addReplacementPattern( "package", getValue( OPT_PACKAGENAME ) );
        addReplacementPattern( "imports", getImports() );
        addReplacementPattern( "classname", getClassName() );
        addReplacementPattern( "sourceURI", getResource( OPT_INPUT ).getURI() );
        addReplacementPattern( "nl", m_nl );
    }
    
    /** Add a pattern-value pair to the list of available patterns */
    protected void addReplacementPattern( String key, String replacement ) {
        if (replacement != null && key != null) {
            String marker = getValue( OPT_MARKER );
            marker = (marker == null) ? DEFAULT_MARKER : marker;
            
            try {
                m_replacements.add( new Replacement( m_perlCompiler.compile( marker + key + marker ),
                                                     new StringSubstitution( replacement ) ) );
            }
            catch (MalformedPatternException e) {
                abort( "Malformed regexp pattern " + marker + key + marker, e );
            }
        }
    }
    
    /** Pop n replacements off the stack */
    protected void pop( int n ) {
        for (int i = 0;  i < n;  i++) {
            m_replacements.remove( m_replacements.size() - 1 );
        }
    }
    
    
    /** Close the output file */
    protected void closeOutput() {
        m_output.flush();
        m_output.close();
    }
    
    
    /** Answer true if the given option is set to true */
    protected boolean isTrue( Object option ) {
        return getOpt( option ).isTrue();
    }
    
    /** Answer true if the given option has value */
    protected boolean hasValue( Object option ) {
        return getOpt( option ).hasValue();
    }
    
    /** Answer true if the given option has a resource value */
    protected boolean hasResourceValue( Object option ) {
        return getOpt( option ).hasResourceValue();
    }
    
    /** Answer the value of the option or null */
    protected String getValue( Object option ) {
        return getOpt( option ).getValue();
    }
    
    /** Answer all values for the given options as Strings */
    protected List getAllValues( Object option ) {
        List values = new ArrayList();
        OptionDefinition opt = getOpt( option );
        
        // look in the command line arguments
        for (Iterator i = m_cmdLineArgs.iterator(); i.hasNext(); ) {
            String s = (String) i.next();
            if (s.equals( opt.m_cmdLineForm )) {
                // next iter value is the arg value
                values.add( i.next() );
            }
        }
        
        // now look in the config file
        for (StmtIterator i = m_root.listProperties( opt.m_prop ); i.hasNext(); ) {
            Statement s = i.nextStatement();
            
            if (s.getObject() instanceof Literal) {
                values.add( s.getString() );
            }
            else {
                values.add( s.getResource().getURI() );
            }
        }
        
        return values;
    }
    
    /** Answer the value of the option or null */
    protected Resource getResource( Object option ) {
        return getOpt( option ).getResource();
    }
    
    /** Answer the option object for the given option */
    protected OptionDefinition getOpt( Object option ) {
        for (int i = 0;  i < m_optionDefinitions.length;  i++) {
            if (m_optionDefinitions[i][0] == option) {
                return (OptionDefinition) m_optionDefinitions[i][1];
            }
        }
        
        return null;
    }
    
    /** Abort due to exception */
    protected void abort( String msg, Exception e ) {
        System.err.println( msg );
        if (e != null) {
            System.err.println( e );
        } 
        System.exit( 1 );
    }
    
    /** Use the current replacements list to do the subs in the given string */
    protected String substitute( String sIn ) {
        String s = sIn;
        
        for (Iterator i = m_replacements.iterator(); i.hasNext(); ) {
            Replacement r = (Replacement) i.next();
            
            s = Util.substitute( m_matcher, r.pattern, r.sub, s, Util.SUBSTITUTE_ALL );
        }
        
        return s;
    }
    
    /** Add the appropriate indent to a buffer */
    protected int indentTo( int i, StringBuffer buf ) {
        int indent = i * m_indentStep;
        for (int j = 0;  j < indent; j++) {
            buf.append( ' ' );
        }
        
        return indent;
    }
    
    /** Write a blank line, with indent and newline */
    protected void writeln( int indent ) {
        writeln( indent, "" );
    }
    
    /** Write out the given string with n spaces of indent, with newline */
    protected void writeln( int indent, String s ) {
        write( indent, s );
        m_output.print( '\n' );
    }
    
    /** Write out the given string with n spaces of indent */
    protected void write( int indentLevel, String s ) {
        for (int i = 0;  i < (m_indentStep * indentLevel);  i++) {
            m_output.print( " " );
        }

        m_output.print( s );
    }
    
    /** Determine the list of imports to include in the file */
    protected String getImports() {
        StringBuffer buf = new StringBuffer();
        buf.append( "import com.hp.hpl.jena.rdf.model.*;" );
        buf.append( m_nl );
        
        if (useOntology()) {
            buf.append( "import com.hp.hpl.jena.ontology.*;" );
        }
        
        return buf.toString();
    }
    
    /** Determine the class name of the vocabulary from the uri */
    protected String getClassName() {
        // if a class name is given, just use that
        if (hasValue( OPT_CLASSNAME )) {
            return getValue(( OPT_CLASSNAME ));
        }
        
        // otherwise, we generate a name based on the URI
        String uri = getResource( OPT_INPUT ).getURI();

        // remove any suffixes
        uri = (uri.endsWith( "#" )) ? uri.substring( 0, uri.length() - 1 ) : uri;
        uri = (uri.endsWith( ".daml" )) ? uri.substring( 0, uri.length() - 5 ) : uri;
        uri = (uri.endsWith( ".owl" )) ? uri.substring( 0, uri.length() - 4 ) : uri;
        uri = (uri.endsWith( ".rdf" )) ? uri.substring( 0, uri.length() - 4 ) : uri;
        uri = (uri.endsWith( ".rdfs" )) ? uri.substring( 0, uri.length() - 5 ) : uri;
        uri = (uri.endsWith( ".n3" )) ? uri.substring( 0, uri.length() - 3 ) : uri;
        
        // now work back to the first non name character from the end
        int i = uri.length() - 1;
        for (; i >= 0; i--) {
            if (!Character.isUnicodeIdentifierPart( uri.charAt( i ) ) &&
                uri.charAt( i ) != '-') {
                i++;
                break;
            }
        }
        
        String name = uri.substring( i );
        
        // optionally add name suffix
        if (hasValue( OPT_CLASSNAME_SUFFIX )) {
            name = name + getValue( OPT_CLASSNAME_SUFFIX );
        }
        
        // now we make the name into a legal Java identifier
        return asLegalJavaID( name, true );
    }
    
    /** Answer true if we are using ontology terms in this vocabulary */
    protected boolean useOntology() {
        return isTrue( OPT_ONTOLOGY );
    }
    
    /** Answer true if all comments are suppressed */
    protected boolean noComments() {
        return isTrue( OPT_NO_COMMENTS );
    }
    
    /** Convert s to a legal Java identifier; capitalise first char if cap is true */
    protected String asLegalJavaID( String s, boolean cap ) {
        StringBuffer buf = new StringBuffer();
        int i = 0;
        
        // treat the first character specially - must be able to start a Java ID, may have to upcase
        for (; !Character.isJavaIdentifierStart( s.charAt( i )); i++) {}
        buf.append( cap ? Character.toUpperCase( s.charAt( i ) ) : s.charAt( i ) );
        
        // copy the remaining characters - replace non-legal chars with '_'
        for (++i; i < s.length(); i++) {
            char c = s.charAt( i );
            buf.append( Character.isJavaIdentifierPart( c ) ? c : '_' );
        }
        
        return buf.toString();
    }
    
    /** The opening class declaration */
    protected void writeClassDeclaration() {
        write( 0, "public class " );
        write( 0, getClassName() );
        write( 0, " " );
        
        if (hasValue( OPT_CLASSDEC )) {
            write( 0, getValue( OPT_CLASSDEC ) );
        }  
        
        writeln( 0, "{" );
    }
    
    /** The close of the class decoration */
    protected void writeClassClose() {
        writeln( 0, "}" );
    }
    
    /** Write the declarations at the head of the class */
    protected void writeInitialDeclarations() {
        writeModelDeclaration();
        writeNamespace();
        
        if (hasValue( OPT_DECLARATIONS )) {
            writeln( 0, getValue( OPT_DECLARATIONS ));
        }
    }
    
    /** Write the declaration of the model */
    protected void writeModelDeclaration() {
        if (useOntology()) {
            writeln( 1, "/** The ontology model that holds the vocabulary terms */" );
            writeln( 1, "private static OntModel m_model = ModelFactory.createOntologyModel( ProfileRegistry." +
                        (isTrue( OPT_LANG_DAML ) ? "DAML" : "OWL" ) +
                        "_LANG );"
                   );
        }
        else {
            writeln( 1, "/** The RDF model that holds the vocabulary terms */" );
            writeln( 1, "private static Model m_model = ModelFactory.createDefaultModel();" );
        }
        
        writeln( 1 );
    }
    
    /** Write the string and resource that represent the namespace */
    protected void writeNamespace() {
        String baseURI = determineBaseURI();
        
        writeln( 1, "/** The namespace of the vocabalary as a string {@value} */" );
        writeln( 1, "public static final String NS = \"" + baseURI + "\";" );
        writeln( 1 );
        
        writeln( 1, "/** The namespace of the vocabalary as a resource {@value} */" );
        writeln( 1, "public static final Resource NAMESPACE = m_model.createResource( \"" + baseURI + "\" );" );
        writeln( 1 );
    }
    
    
    /** Determine what the base URI for this vocabulary is */
    protected String determineBaseURI() {
        // easy: it was set by the user
        if (hasResourceValue( OPT_BASE )) {
            return getResource( OPT_BASE ).getURI();
        }
        
        // if we are using an ontology model, get the base URI from the ontology element
        try {
            Resource ont = m_source.getBaseModel()
                                   .listStatements( null, RDF.type, m_source.getProfile().ONTOLOGY() )
                                   .nextStatement()
                                   .getSubject();
            
            String uri = ont.getURI();
            uri = (!uri.endsWith( "#" )) ? uri + "#" : uri;
            
            // save the base URI as the main included uri for the filter
            m_includeURI.add( uri );
            
            return uri;
        }
        catch (Exception e) {
            abort( "Could not determine the base URI for the input vocabulary", null );
            return null;
        }
    }
    
    
    /** Write the list of properties */
    protected void writeProperties() {
        if (isTrue( OPT_NOPROPERTIES )) {
            return; 
        }
        
        if (hasValue( OPT_PROPERTY_SECTION )) {
            writeln( 0, getValue( OPT_PROPERTY_SECTION ));
        }
        
        if (useOntology()) {
            writeObjectProperties();
            writeDatatypeProperties();
            writeAnnotationProperties();
            
            // we also write out the RDF properties, to mop up any props that are not stated as 
            // object, datatype or annotation properties
            writeRDFProperties();
        }
        else {
            writeRDFProperties();
        }
    }
    
    /** Write any object properties in the vocabulary */
    protected void writeObjectProperties() {
        String template = hasValue( OPT_PROP_TEMPLATE ) ?  getValue( OPT_PROP_TEMPLATE ) : DEFAULT_TEMPLATE;

        for (Iterator i = m_source.listObjectProperties(); i.hasNext(); ) {
            writeValue( (Resource) i.next(), template, "ObjectProperty", "createObjectProperty", "_PROP" );
        }
    }
    
    /** Write any datatype properties in the vocabulary */
    protected void writeDatatypeProperties() {
        String template = hasValue( OPT_PROP_TEMPLATE ) ?  getValue( OPT_PROP_TEMPLATE ) : DEFAULT_TEMPLATE;

        for (Iterator i = m_source.listDatatypeProperties(); i.hasNext(); ) {
            writeValue( (Resource) i.next(), template, "DatatypeProperty", "createDatatypeProperty", "_PROP" );
        }
    }
    
    /** Write any annotation properties in the vocabulary */
    protected void writeAnnotationProperties() {
        String template = hasValue( OPT_PROP_TEMPLATE ) ?  getValue( OPT_PROP_TEMPLATE ) : DEFAULT_TEMPLATE;

        for (Iterator i = m_source.listAnnotationProperties(); i.hasNext(); ) {
            writeValue( (Resource) i.next(), template, "AnnotationProperty", "createAnnotationProperty", "_PROP" );
        }
    }
    
    /** Write any vanilla RDF properties in the vocabulary */
    protected void writeRDFProperties() {
        String template = hasValue( OPT_PROP_TEMPLATE ) ?  getValue( OPT_PROP_TEMPLATE ) : DEFAULT_TEMPLATE;

        for (StmtIterator i = m_source.listStatements( null, RDF.type, RDF.Property ); i.hasNext(); ) {
            writeValue( i.nextStatement().getSubject(), template, "Property", "createProperty", "_PROP" );
        }
    }
    
    /** Write any classes in the vocabulary */
    protected void writeClasses() {
        if (isTrue( OPT_NOCLASSES )) {
            return; 
        }
            
        if (hasValue( OPT_CLASS_SECTION )) {
            writeln( 0, getValue( OPT_CLASS_SECTION ));
        }
            
        if (useOntology()) {
            writeOntClasses();
        }
        else {
            writeRDFClasses();
        }
    }
    
    /** Write classes as ontology terms */
    protected void writeOntClasses() {
        String template = hasValue( OPT_CLASS_TEMPLATE ) ?  getValue( OPT_CLASS_TEMPLATE ) : DEFAULT_TEMPLATE;

        for (Iterator i = m_source.listClasses(); i.hasNext(); ) {
            writeValue( (Resource) i.next(), template, "OntClass", "createClass", "_CLASS" );
        }
    }
    
    /** Write classes as vanilla RDF terms */
    protected void writeRDFClasses() {
        String template = hasValue( OPT_CLASS_TEMPLATE ) ?  getValue( OPT_CLASS_TEMPLATE ) : DEFAULT_TEMPLATE;

        for (StmtIterator i = m_source.listStatements( null, RDF.type, RDFS.Class ); i.hasNext(); ) {
            writeValue( i.nextStatement().getSubject(), template, "Resource", "createResource", "_CLASS" );
        }
    }
    
    /** Write any instances (individuals) in the vocabulary */
    protected void writeIndividuals() {
        if (isTrue( OPT_NOINDIVIDUALS )) {
            return; 
        }
            
        if (hasValue( OPT_INDIVIDUALS_SECTION )) {
            writeln( 0, getValue( OPT_INDIVIDUALS_SECTION ));
        }
            
        if (useOntology()) {
            writeOntIndividuals();
        }
        else {
            writeRDFIndividuals();
        }
    }
    
    /** Write individuals as ontology terms */
    protected void writeOntIndividuals() {
        String template = hasValue( OPT_INDIVIDUAL_TEMPLATE ) ?  getValue( OPT_INDIVIDUAL_TEMPLATE ) : DEFAULT_INDIVIDUAL_TEMPLATE;

        for (StmtIterator i = m_source.listStatements( null, RDF.type, (RDFNode) null ); i.hasNext(); ) {
            Statement candidate = i.nextStatement();
            
            if (candidate.getObject() instanceof Resource) {
                String uri = ((Resource) candidate.getObject()).getURI();
                
                for (Iterator j = m_includeURI.iterator();  j.hasNext(); ) {
                    if (uri.startsWith( (String) j.next() )) {
                        // the subject has an included type
                        Resource ind = candidate.getSubject();
                        
                        // do we have a local class resource
                        String varName = (String) m_resourcesToNames.get( (Resource) candidate.getObject() );
                        String valType = (varName != null) ? varName : "m_model.createClass( \"" + uri + "\" )";
                        
                        // push the individuals type onto the stack
                        addReplacementPattern( "valtype", valType ); 
                        writeValue( ind, template, "Individual", "createIndividual", "_INSTANCE" );
                        pop( 1 );
                        
                        break;
                    }
                } 
            }
        }
    }
    
    /** Write individuals as vanilla RDF terms */
    protected void writeRDFIndividuals() {
        String template = hasValue( OPT_INDIVIDUAL_TEMPLATE ) ?  getValue( OPT_INDIVIDUAL_TEMPLATE ) : DEFAULT_TEMPLATE;

        for (StmtIterator i = m_source.listStatements( null, RDF.type, (RDFNode) null ); i.hasNext(); ) {
            Statement candidate = i.nextStatement();
            
            if (candidate.getObject() instanceof Resource) {
                String uri = ((Resource) candidate.getObject()).getURI();
                
                for (Iterator j = m_includeURI.iterator();  j.hasNext(); ) {
                    if (uri.startsWith( (String) j.next() )) {
                        // the subject of the sentence has a type that's on our include list
                        writeValue( candidate.getSubject(), template, "Resource", "createResource", "_INSTANCE" );
                        
                        break;
                    }
                } 
            }
        }
    }
    
    /** Write the value declaration out using the given template, optionally creating comments */
    protected void writeValue( Resource r, String template, String valueClass, String creator, String disambiguator ) {
        if (!filter( r )) {
            if (!noComments()  &&  hasComment( r )) {
                writeln( 1, formatComment( getComment( r ) ) );
            }
        
            // push the local bindings for the substitution onto the stack
            addReplacementPattern( "valuri", r.getURI() );
            addReplacementPattern( "valname", getValueName( r, disambiguator ));
            addReplacementPattern( "valclass", valueClass );
            addReplacementPattern( "valcreator", creator );
            
            // write out the value
            writeln( 1, substitute( template ) );
            writeln( 1 );
            
            // pop the local replacements off the stack
            pop( 4 );
        }
    }
    
    /** Answer true if the given resource has an rdf:comment or daml:comment */
    protected boolean hasComment( Resource r ) {
        return r.hasProperty( RDFS.comment )  || r.hasProperty( DAML_OIL.comment );
    }
    
    /** Answer all of the commentage on the given resource, as a string */
    protected String getComment( Resource r ) {
        StringBuffer comment = new StringBuffer();
        
        // collect any RDFS or DAML comments attached to the node
        for (NodeIterator ni = m_source.listObjectsOfProperty( r, RDFS.comment );  ni.hasNext(); ) {
            comment.append( ni.next().toString().trim() );
        }
        
        for (NodeIterator ni = m_source.listObjectsOfProperty( r, DAML_OIL.comment );  ni.hasNext(); ) {
            comment.append( ni.next().toString().trim() );
        }
        
        return comment.toString();
    }
    
    /** Format the comment as Javadoc, and limit the line width */ 
    protected String formatComment( String comment ) {
        StringBuffer buf = new StringBuffer();
        buf.append( "/** <p>" );
        
        boolean inSpace = false;
        int pos = buf.length();
        boolean singleLine = true;
        
        // now format the comment by compacting whitespace and limiting the line length
        // add the prefix to the start of each line
        for (int i = 0;  i < comment.length();  i++ ) {
            char c = comment.charAt( i );
            
            // compress whitespace
            if (Character.isWhitespace( c )) {
                if (inSpace) {
                    continue;       // more than one space is ignored
                }
                else {
                    c = ' ';        // map all whitespace to 0x20
                    inSpace = true;
                }
            }
            else {
                inSpace = false;
            }
            
            // escapes?
            if (c == '\\') {
                c = comment.charAt( ++i );
                
                switch (c) {
                    case 'n': 
                        buf.append( m_nl );
                        pos = indentTo( 1, buf );
                        buf.append( " *  " );
                        pos += 3;
                        singleLine = false;
                        break;

                    default:
                        // add other escape sequences above
                        break;
                }
            }
            else {
                // add the char
                buf.append( c );
                pos++;
            }
             
            // wrap any very long lines at 120 chars
            if ((pos > COMMENT_LENGTH_LIMIT) && (inSpace)) {
                buf.append( m_nl );
                pos = indentTo( 1, buf );
                buf.append( " *  " );
                pos += 3;
                singleLine = false;
            }
        }

        buf.append( "</p>" );
        buf.append( singleLine ? "" : m_nl );
        indentTo( singleLine ? 0 : 1, buf );
        buf.append( " */" );
        return buf.toString();
    }
    
    /** Answer true if resource r <b>does not</b> show in output */
    protected boolean filter( Resource r ) {
        if (r.isAnon()) {
            return true;
        }
        
        // if we've already processed this resource once, ignore it next time
        if (m_resourcesToNames.containsKey( r )) {
            return true;
        }
        
        // search the allowed URI's
        for (Iterator i = m_includeURI.iterator(); i.hasNext(); ) {
            String uri = (String) i.next();
            if (r.getURI().startsWith( uri )) {
                // in
                return false;
            }
        }
        
        // default is out
        return true;
    }
    
    /** Answer the Java value name for the URI */
    protected String getValueName( Resource r, String disambiguator ) {
        // the id name is basically the local name of the resource, possibly in upper case
        String name = isTrue( OPT_UC_NAMES ) ? getUCValueName( r ) : r.getLocalName();
        
        // must be legal java
        name = asLegalJavaID( name, false );
        
        // must not clash with an existing name
        int attempt = 0;
        String baseName = name;
        while (m_usedNames.contains( name )) {
            name = (attempt == 0) ? (name + disambiguator) : (baseName + disambiguator + attempt);
            attempt++;
        }
        
        // record this name so that we don't use it again (which will stop the vocab compiling)
        m_usedNames.add( name );
        
        // record the mapping from resource to name
        m_resourcesToNames.put( r, name );
        
        return name;
    }
    
    /** Answer the local name of resource r mapped to upper case */
    protected String getUCValueName( Resource r ) {
        StringBuffer buf = new StringBuffer();
        String localName = r.getLocalName();
        char lastChar = 0;

        for (int i = 0; i < localName.length(); i++) {
            char c = localName.charAt(i);
            
            if (Character.isLowerCase(lastChar) && Character.isUpperCase(c)) {
                buf.append( '_' );
            }
            buf.append( Character.toUpperCase(c) );
            lastChar = c;
        }
        
        return buf.toString();
    }
    
    
    //==============================================================================
    // Inner class definitions
    //==============================================================================

    /** An option that can be set either on the command line or in the RDF config */
    protected class OptionDefinition
    {
        protected String m_cmdLineForm;
        protected Property m_prop;
        
        protected OptionDefinition( String cmdLineForm, String name ) {
            m_cmdLineForm = cmdLineForm;
            if (name != null) {
                m_prop = m_config.getProperty( NS, name );
            } 
        }
        
        /**
         * Answer true if this option is set to true, either on the command line
         * or in the config model
         * 
         * @return boolean
         */
        protected boolean isTrue() {
            if (m_cmdLineArgs.contains( m_cmdLineForm )) {
                return true;
            }
            
            if (m_root.hasProperty( m_prop )) {
                return m_root.getProperty( m_prop ).getBoolean();
            }
            
            return false;
        }
        
        /**
         * Answer the string value of the parameter if set, or null otherwise.
         * 
         * @return String
         */
        protected String getValue() {
            int index = m_cmdLineArgs.indexOf( m_cmdLineForm );
            
            if (index >= 0) {
                try {
                    return (String) m_cmdLineArgs.get( index + 1 );
                }
                catch (IndexOutOfBoundsException e) {
                    System.err.println( "Value for parameter " + m_cmdLineForm  + " not set! Aborting.");
                }
            }
            
            if (m_prop != null  &&  m_root.hasProperty( m_prop )) {
                return m_root.getProperty( m_prop ).getString();
            }
            
            // not set
            return null;
        }
        
        /**
         * Answer true if the parameter has a value at all.
         * 
         * @return boolean
         */
        protected boolean hasValue() {
            return getValue() != null;
        }
        
        
        /**
         * Answer the resource value of the parameter if set, or null otherwise.
         * 
         * @return String
         */
        protected Resource getResource() {
            int index = m_cmdLineArgs.indexOf( m_cmdLineForm );
            
            if (index >= 0) {
                try {
                    return m_config.getResource( (String) m_cmdLineArgs.get( index + 1 ) );
                }
                catch (IndexOutOfBoundsException e) {
                    System.err.println( "Value for parameter " + m_cmdLineForm  + " not set! Aborting.");
                }
            }
            
            if (m_prop != null  &&  m_root.hasProperty( m_prop )) {
                return m_root.getProperty( m_prop ).getResource();
            }
            
            // not set
            return null;
        }
        
        /**
         * Answer true if the parameter has a value at all.
         * 
         * @return boolean
         */
        protected boolean hasResourceValue() {
            return getResource() != null;
        }
    }  // end inner class OptionDefinition
    
    
    /** A pairing of pattern and substitution we want to apply to output */
    protected class Replacement
    {
        protected Substitution sub;
        protected Pattern pattern;
        
        protected Replacement( Pattern pattern, Substitution sub) {
            this.sub = sub;
            this.pattern = pattern;
        }
    } // end inner class Replacement
}


/*
    (c) Copyright Hewlett-Packard Company 2002-2003
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

    1. Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.

    2. Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.

    3. The name of the author may not be used to endorse or promote products
       derived from this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
    IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
    OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
    IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
    INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
    NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
    DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
    THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
