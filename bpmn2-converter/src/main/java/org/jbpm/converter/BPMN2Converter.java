package org.jbpm.converter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.drools.builder.KnowledgeBuilderConfiguration;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.compiler.PackageBuilderConfiguration;
import org.drools.xml.SemanticModules;
import org.jbpm.bpmn2.xml.XmlBPMNProcessDumper;
import org.jbpm.compiler.xml.ProcessSemanticModule;
import org.jbpm.compiler.xml.XmlProcessReader;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.xml.sax.SAXException;

/**
 * Converter that converts .rf ruleflow file format to BPMN2 file format.
 * 
 * @author Jeff Lindesmith
 *
 */

public class BPMN2Converter {
	
	private static Logger logger = Logger.getLogger(BPMN2Converter.class);
	
	/**
	 * Main method for application.
	 * 
	 * @param args Arguments for application. First argument represents the full path to 
	 * a .rf ruleflow file or a directory containing .rf ruleflow files. 
	 */
	public static final void main(String[] args) {
		if (args.length == 0) {
			logger.info("BMPN2Converter Usage: \n" + 
			"java -jar bpmn2-converter.jar " +
			"<path to .rf file or path to directory containing .rf files>");
			return;
		}
		String fileOrDir = args[0];
		File fileOrDirObj = new File(fileOrDir);
		boolean isDir = fileOrDirObj.isDirectory();
		if (!isDir) {
			convertFile(fileOrDir);
		} else {
			String[] files = new File(fileOrDir).list();
			for (String fileStr: files) {
			    File file = new File(fileOrDir + "/" + fileStr);
			    if (file.isFile() && fileStr.endsWith(".rf")) {
			        convertFile(fileOrDir + "/" + fileStr);			    	
			    }
			}
		}
		
		
	}
	
	/**
	 * Creates InputStream for .rf file and OutputStream for resulting .bpmn2 file and calls
	 * convertToBpmn2 passing these streams.
	 *
	 * @param rfFile The name of the .rf file including the .rf extension
	 */
	public static void convertFile(String rfFile) {
		logger.info("Converting " + rfFile);
		FileInputStream input = null;
		FileOutputStream output = null;
		BufferedInputStream bin = null;
		BufferedOutputStream bout = null;
		try {
            input = new FileInputStream(rfFile);
            bin = new BufferedInputStream(input);
			String fileName = rfFile.substring(0, rfFile.indexOf("."));
            output = new FileOutputStream(fileName + ".bpmn2");
			bout = new BufferedOutputStream(output);
			convertToBpmn2(bin, bout);
	    } catch (Exception e) {    
		    logger.warn("Error converting " + rfFile, e);
		} finally {
		    try {
		        if (bin != null) {
			        bin.close();
				}
		        if (input != null) {
			        input.close();
				}
		        if (bout != null) {
			        bout.close();
				}
				if (output != null) {
			        output.close();
				}
			} catch (IOException e) {
			    logger.error("Error closing io streams", e);	
			}
		}		
	}
	
	/**
	 * Reads .rf file from rulFlowInputStream and writes BPMN2 format to .bpmn2 file using bpmn20OutputStream.
	 * 
	 * @param ruleFlowInputStream InputStream for .rf file
	 * @param bpmn2OutputStream OutputStream for resulting .bpmn2 file
	 * @throws SAXException
	 * @throws IOException
	 */
    public static void convertToBpmn2(InputStream ruleFlowInputStream, OutputStream bpmn2OutputStream) throws SAXException, IOException {
        KnowledgeBuilderConfiguration conf = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration();
        PackageBuilderConfiguration pconf = ((PackageBuilderConfiguration) conf);
        pconf.initSemanticModules();
        pconf.addSemanticModule(new ProcessSemanticModule());
        SemanticModules semanticModules = pconf.getSemanticModules();
        XmlProcessReader processReader = new XmlProcessReader(semanticModules, Thread.currentThread().getContextClassLoader());
        RuleFlowProcess ruleflow = (RuleFlowProcess) processReader.read(ruleFlowInputStream).get(0);
        bpmn2OutputStream.write(XmlBPMNProcessDumper.INSTANCE.dump(ruleflow).getBytes());
    }

}
