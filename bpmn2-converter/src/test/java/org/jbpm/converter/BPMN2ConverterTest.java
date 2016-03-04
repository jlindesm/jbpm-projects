package org.jbpm.converter;
import java.io.ByteArrayOutputStream;

import org.apache.log4j.Logger;
import org.drools.KnowledgeBase;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;
import org.jbpm.test.JbpmJUnitTestCase;
import org.junit.BeforeClass;
import org.junit.Test;


public class BPMN2ConverterTest extends JbpmJUnitTestCase {
	
	private static Logger logger = Logger.getLogger(BPMN2ConverterTest.class); 
			
			
	private static byte[] firstruleflowBytes;
	private static byte[] secondruleflowBytes;

	public BPMN2ConverterTest() {
	    super(true);	
	    setPersistence(true);
	}
	
	@BeforeClass
	public static void setupBefore() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BPMN2Converter.convertToBpmn2(BPMN2ConverterTest.class.getResourceAsStream("firstruleflow.rf"), out);
		out.close();
		firstruleflowBytes = out.toByteArray();
		out.reset();
		BPMN2Converter.convertToBpmn2(BPMN2ConverterTest.class.getResourceAsStream("secondruleflow.rf"), out);
		out.close();
		secondruleflowBytes = out.toByteArray();
	}
	
	@Test
	public void testFirstRuleflowConversion() throws Exception {
		StatefulKnowledgeSession ksession = createKnowledgeSession(readKnowledgeBase(firstruleflowBytes));
		ProcessInstance pi = ksession.startProcess("org.jbpm.converter.firstruleflow");
		ksession.fireAllRules();
		assertNodeTriggered(pi.getId(), "Start");
		assertNodeTriggered(pi.getId(), "Script Node");
		assertNodeTriggered(pi.getId(), "End");
		assertProcessInstanceCompleted(pi.getId(), ksession);
		ksession.dispose();
	}

	@Test
	public void testSecondRuleflowConversion() throws Exception {
		StatefulKnowledgeSession ksession = createKnowledgeSession(readKnowledgeBase(secondruleflowBytes));
		ksession.getWorkItemManager().registerWorkItemHandler("Log", new LogWorkItemHandler());
		ProcessInstance pi = ksession.startProcess("org.jbpm.converter.secondruleflow");
		ksession.fireAllRules();
		assertNodeTriggered(pi.getId(), "Start");
		assertNodeTriggered(pi.getId(), "Success Log");
		assertNodeTriggered(pi.getId(), "End");
		assertProcessInstanceCompleted(pi.getId(), ksession);
		ksession.dispose();
	}

    private static KnowledgeBase readKnowledgeBase(byte[] process) throws Exception {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add(ResourceFactory.newByteArrayResource(process), ResourceType.BPMN2);
        // Check for errors
        if (kbuilder.hasErrors()) {
            if (kbuilder.getErrors().size() > 0) {
                boolean errors = false;
                for (KnowledgeBuilderError error : kbuilder.getErrors()) {
                    logger.error(error.toString());
                    errors = true;
                }
                assertFalse("Could not build knowldge base.", errors);
            }
        }
        return kbuilder.newKnowledgeBase();
    }
    
    private class LogWorkItemHandler implements WorkItemHandler {

    	private Logger wihLogger = Logger.getLogger(LogWorkItemHandler.class);

		public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
			wihLogger.warn(workItem.getParameter("Message"));
			manager.completeWorkItem(workItem.getId(), null);
		}

		public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		} 
    	
    }
}
