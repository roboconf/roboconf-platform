package net.roboconf.core.utils;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;


/**
 * 
 * @author Pierre-Yves Gibello - Linagora
 *
 */
public class ProcessStoreTest {
	
	String applicationName = "app1";
	String scopedInstancePath = "/anything";

	@Test
	public void testProcessFunctions() throws IOException {
		Assert.assertNull(ProcessStore.getProcess(applicationName, scopedInstancePath));
		
		Process process = (new ProcessBuilder("ls")).start();
		
		ProcessStore.setProcess(applicationName, scopedInstancePath, process);
		Assert.assertEquals(ProcessStore.getProcess(applicationName, scopedInstancePath), process);
		
		ProcessStore.setProcess(null, null, process);	
		Assert.assertEquals(ProcessStore.getProcess(null, null), process);
		
		ProcessStore.clearProcess(applicationName, scopedInstancePath);
		Assert.assertEquals(ProcessStore.getProcess(null, null), process);
		Assert.assertNull(ProcessStore.getProcess(applicationName, scopedInstancePath));
		
		ProcessStore.clearProcess(null, null);
		Assert.assertNull(ProcessStore.getProcess(null, null));
	}
	
}
