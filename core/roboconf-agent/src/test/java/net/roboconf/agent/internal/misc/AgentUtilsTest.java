/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of the joint LINAGORA -
 * Université Joseph Fourier - Floralis research program and is designated
 * as a "Result" pursuant to the terms and conditions of the LINAGORA
 * - Université Joseph Fourier - Floralis research program. Each copyright
 * holder of Results enumerated here above fully & independently holds complete
 * ownership of the complete Intellectual Property rights applicable to the whole
 * of said Results, and may freely exploit it in any manner which does not infringe
 * the moral rights of the other copyright holders.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.roboconf.agent.internal.misc;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.internal.tests.TestApplicationTemplate;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 * @author Amadou Diarra - UGA
 */
public class AgentUtilsTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();



	@After
	public void clearAgentDirectories() throws Exception {
		File f = new File( System.getProperty( "java.io.tmpdir" ), "roboconf_agent" );
		Utils.deleteFilesRecursively( f );
	}


	private boolean isUnix() {
		String os = System.getProperty("os.name").toLowerCase();
		return (os.contains("nix")  || os.contains("nux") || os.contains("aix") );
	}


	@Test
	public void testIsValidIP() {

		Assert.assertTrue( AgentUtils.isValidIP( "127.0.0.1" ));
		Assert.assertTrue( AgentUtils.isValidIP( "192.168.1.1" ));
		Assert.assertTrue( AgentUtils.isValidIP( "46.120.105.36" ));
		Assert.assertTrue( AgentUtils.isValidIP( "216.24.131.152" ));

		Assert.assertFalse( AgentUtils.isValidIP( "localhost" ));
		Assert.assertFalse( AgentUtils.isValidIP( "127.0.0." ));
		Assert.assertFalse( AgentUtils.isValidIP( "127.0.0" ));
		Assert.assertFalse( AgentUtils.isValidIP( "-192.168.1.-5" ));
		Assert.assertFalse( AgentUtils.isValidIP( "192.168.1.-5" ));
		Assert.assertFalse( AgentUtils.isValidIP( "192.168.lol.1" ));
		Assert.assertFalse( AgentUtils.isValidIP( "192.168.2.1024" ));
		Assert.assertFalse( AgentUtils.isValidIP( "" ));
		Assert.assertFalse( AgentUtils.isValidIP( null ));
	}


	@Test
	public void testInstanceResources() throws Exception {

		// Prepare our resources
		TestApplicationTemplate app = new TestApplicationTemplate();
		Map<String,byte[]> fileNameToFileContent = new HashMap<> ();
		fileNameToFileContent.put( "f1.txt", "I am file 1".getBytes( "UTF-8" ));
		fileNameToFileContent.put( "f2.txt", "I am file 2".getBytes( "UTF-8" ));
		fileNameToFileContent.put( "dir1/dir2/f3.txt", "I am file 3".getBytes( "UTF-8" ));

		// Save our resources
		File dir = InstanceHelpers.findInstanceDirectoryOnAgent( app.getTomcat());
		Utils.deleteFilesRecursively( dir );
		Assert.assertFalse( dir.exists());

		AgentUtils.copyInstanceResources( app.getTomcat(), fileNameToFileContent );
		Assert.assertTrue( dir.exists());
		Assert.assertTrue( new File( dir, "f1.txt" ).exists());
		Assert.assertTrue( new File( dir, "f2.txt" ).exists());
		Assert.assertTrue( new File( dir, "dir1/dir2/f3.txt" ).exists());

		// Delete them
		AgentUtils.deleteInstanceResources( app.getTomcat());
		Assert.assertFalse( dir.exists());
	}


	@Test
	public void testInstanceResources_noResources() throws Exception {

		// Prepare our resources
		TestApplicationTemplate app = new TestApplicationTemplate();

		// Save our resources
		File dir = InstanceHelpers.findInstanceDirectoryOnAgent( app.getTomcat());
		Utils.deleteFilesRecursively( dir );
		Assert.assertFalse( dir.exists());

		AgentUtils.copyInstanceResources( app.getTomcat(), null );
		Assert.assertTrue( dir.exists());
		Assert.assertEquals( 0, dir.listFiles().length );

		// Delete them
		AgentUtils.deleteInstanceResources( app.getTomcat());
		Assert.assertFalse( dir.exists());
	}


	@Test( expected = IOException.class )
	public void testInstanceResources_exception_rootDir() throws Exception {

		// The directory where we should write is an existing file.
		// Prepare our resources
		TestApplicationTemplate app = new TestApplicationTemplate();
		File dir = InstanceHelpers.findInstanceDirectoryOnAgent( app.getTomcat());
		Utils.deleteFilesRecursively( dir );
		Assert.assertFalse( dir.exists());
		Assert.assertTrue( dir.createNewFile());
		Assert.assertTrue( dir.exists());

		// Save our resources
		try {
			AgentUtils.copyInstanceResources( app.getTomcat(), null );

		} finally {
			Utils.deleteFilesRecursively( dir );
		}
	}


	@Test( expected = IOException.class )
	public void testInstanceResources_exception_subDir() throws Exception {

		// The directory where we should write contains a conflicting file.
		// Prepare our resources
		TestApplicationTemplate app = new TestApplicationTemplate();
		Map<String,byte[]> fileNameToFileContent = new HashMap<> ();
		fileNameToFileContent.put( "dir1/dir2/f3.txt", "I am file 3".getBytes( "UTF-8" ));

		File dir = InstanceHelpers.findInstanceDirectoryOnAgent( app.getTomcat());
		Utils.deleteFilesRecursively( dir );
		Assert.assertFalse( dir.exists());
		Assert.assertTrue( dir.mkdirs());
		Assert.assertTrue( new File( dir, "dir1" ).createNewFile());

		// Save our resources
		try {
			AgentUtils.copyInstanceResources( app.getTomcat(), fileNameToFileContent );

		} finally {
			Utils.deleteFilesRecursively( dir );
		}
	}


	@Test
	public void testChangeRoboconfLogLevel() throws Exception {

		// Null => no exception
		AgentUtils.changeRoboconfLogLevel( Level.FINE.toString(), null );

		// Create an empty directory => no log file to update
		File karafEtc = this.folder.newFolder();
		Assert.assertEquals( 0, karafEtc.listFiles().length );

		AgentUtils.changeRoboconfLogLevel( Level.FINE.toString(), karafEtc.getAbsolutePath());
		Assert.assertEquals( 0, karafEtc.listFiles().length );

		// Create a new file to update
		File configFile = new File( karafEtc, "org.ops4j.pax.logging.cfg" );
		Assert.assertTrue( configFile.createNewFile());
		Assert.assertEquals( 0, configFile.length());

		AgentUtils.changeRoboconfLogLevel( Level.FINE.toString(), karafEtc.getAbsolutePath());
		String content = Utils.readFileContent( configFile );
		Assert.assertTrue( content.contains( Level.FINE.toString() + ", roboconf" ));

		// Try a new update
		AgentUtils.changeRoboconfLogLevel( Level.SEVERE.toString(), karafEtc.getAbsolutePath());
		content = Utils.readFileContent( configFile );
		Assert.assertTrue( content.contains( Level.SEVERE.toString() + ", roboconf" ));
		Assert.assertFalse( content.contains( Level.FINE.toString() + ", roboconf" ));
	}


	@Test
	public void testCollectLogs() throws Exception {

		// Null => no resource
		Map<String,byte[]> map = AgentUtils.collectLogs( null );
		Assert.assertEquals( 0, map.size());

		// Empty directory => no resource
		File karafData = this.folder.newFolder();
		map = AgentUtils.collectLogs( karafData.getAbsolutePath());
		Assert.assertEquals( 0, map.size());

		// Find the logs directory
		File karafLog = new File( karafData, AgentConstants.KARAF_LOGS_DIRECTORY + "/karaf.log" );
		Assert.assertTrue( karafLog.getParentFile().mkdir());
		Assert.assertTrue( karafLog.createNewFile());

		map = AgentUtils.collectLogs( karafData.getAbsolutePath());
		Assert.assertEquals( 1, map.size());
		Assert.assertTrue( map.containsKey( "karaf.log" ));

		// Add other files
		String[] names = { "roboconf", "roboconf2", "whatever" };
		for( String s : names ) {
			karafLog = new File( karafData, AgentConstants.KARAF_LOGS_DIRECTORY + "/" + s + ".log" );
			Assert.assertTrue( karafLog.createNewFile());
		}

		map = AgentUtils.collectLogs( karafData.getAbsolutePath());
		Assert.assertEquals( 2, map.size());
		Assert.assertTrue( map.containsKey( "karaf.log" ));
		Assert.assertTrue( map.containsKey( "roboconf.log" ));
	}


	@Test
	public void testFindIpAddress_invalidNetworkInterface() throws Exception {

		String expectedIp = InetAddress.getLocalHost().getHostAddress();
		Assert.assertEquals( expectedIp, AgentUtils.findIpAddress( "invalid-network" ));
	}


	@Test
	public void testFindIpAddress_validNetworkInterface() throws Exception {

		NetworkInterface nif = NetworkInterface.getByName( "eth0" );
		if( nif != null )
			Assert.assertNotNull( AgentUtils.findIpAddress( "eth0" ));
	}


	@Test
	public void testFindIpAddress_nullNetworkInterface() throws Exception {

		String expectedIp = InetAddress.getLocalHost().getHostAddress();
		Assert.assertEquals( expectedIp, AgentUtils.findIpAddress( null ));
	}


	@Test
	public void testFindIpAddress_nforceDefaultNetworkInterface() throws Exception {

		String expectedIp = InetAddress.getLocalHost().getHostAddress();
		Assert.assertEquals( expectedIp, AgentUtils.findIpAddress( AgentConstants.DEFAULT_NETWORK_INTERFACE ));
	}

	@Test
	public void testExecuteScriptResources() throws IOException, InterruptedException {

		//prepare our resources
		File scriptsDir = this.folder.newFolder();

		AgentUtils.executeScriptResources( scriptsDir );
		Assert.assertEquals(0, scriptsDir.listFiles().length);

		Assume.assumeTrue( isUnix());

		File script1 = new File( scriptsDir, "toto-script-all.sh" );
		File script2 = new File( scriptsDir, "titi-script.sh" );
		Utils.writeStringInto( "#!/bin/bash\necho totototototo > toto.txt", script1);
		Utils.writeStringInto( "#!/bin/bash\necho titiiii > titi.txt", script2);

		AgentUtils.executeScriptResources( scriptsDir );
		File titi = new File(scriptsDir,"titi.txt");
		File toto = new File(scriptsDir,"toto.txt");

		Assert.assertFalse( titi.exists() );
		Assert.assertTrue( toto.exists() );
		Assert.assertEquals( 3,scriptsDir.listFiles().length );

		String s = Utils.readFileContent( toto );
		Assert.assertEquals("totototototo",s.trim());
	}
}
