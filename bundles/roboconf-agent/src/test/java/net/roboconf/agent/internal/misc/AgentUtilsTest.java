/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AgentUtilsTest {

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
		TestApplication app = new TestApplication();
		String pluginName = "whatever";
		Map<String,byte[]> fileNameToFileContent = new HashMap<String,byte[]> ();
		fileNameToFileContent.put( "f1.txt", "I am file 1".getBytes( "UTF-8" ));
		fileNameToFileContent.put( "f2.txt", "I am file 2".getBytes( "UTF-8" ));
		fileNameToFileContent.put( "dir1/dir2/f3.txt", "I am file 3".getBytes( "UTF-8" ));

		// Save our resources
		File dir = InstanceHelpers.findInstanceDirectoryOnAgent( app.getTomcat(), pluginName );
		Assert.assertFalse( dir.exists());
		AgentUtils.copyInstanceResources( app.getTomcat(), pluginName, fileNameToFileContent );
		Assert.assertTrue( dir.exists());
		Assert.assertTrue( new File( dir, "f1.txt" ).exists());
		Assert.assertTrue( new File( dir, "f2.txt" ).exists());
		Assert.assertTrue( new File( dir, "dir1/dir2/f3.txt" ).exists());

		// Delete them
		AgentUtils.deleteInstanceResources( app.getTomcat(), pluginName );
		Assert.assertFalse( dir.exists());
	}


	@Test
	public void testInstanceResources_noResources() throws Exception {

		// Prepare our resources
		TestApplication app = new TestApplication();
		String pluginName = "whatever";

		// Save our resources
		File dir = InstanceHelpers.findInstanceDirectoryOnAgent( app.getTomcat(), pluginName );
		Assert.assertFalse( dir.exists());
		AgentUtils.copyInstanceResources( app.getTomcat(), pluginName, null );
		Assert.assertTrue( dir.exists());
		Assert.assertEquals( 0, dir.listFiles().length );

		// Delete them
		AgentUtils.deleteInstanceResources( app.getTomcat(), pluginName );
		Assert.assertFalse( dir.exists());
	}


	@Test( expected = IOException.class )
	public void testInstanceResources_exception_rootDir() throws Exception {

		// The directory where we should write is an existing file.
		// Prepare our resources
		TestApplication app = new TestApplication();
		String pluginName = "whatever";

		File dir = InstanceHelpers.findInstanceDirectoryOnAgent( app.getTomcat(), pluginName );
		Assert.assertTrue( dir.createNewFile());
		Assert.assertTrue( dir.exists());

		// Save our resources
		try {
			AgentUtils.copyInstanceResources( app.getTomcat(), pluginName, null );

		} finally {
			Utils.deleteFilesRecursively( dir );
		}
	}


	@Test( expected = IOException.class )
	public void testInstanceResources_exception_subDir() throws Exception {

		// The directory where we should write contains a conflicting file.
		// Prepare our resources
		TestApplication app = new TestApplication();
		String pluginName = "whatever";
		Map<String,byte[]> fileNameToFileContent = new HashMap<String,byte[]> ();
		fileNameToFileContent.put( "dir1/dir2/f3.txt", "I am file 3".getBytes( "UTF-8" ));

		File dir = InstanceHelpers.findInstanceDirectoryOnAgent( app.getTomcat(), pluginName );
		Assert.assertTrue( dir.mkdir());
		Assert.assertTrue( new File( dir, "dir1" ).createNewFile());

		// Save our resources
		try {
			AgentUtils.copyInstanceResources( app.getTomcat(), pluginName, fileNameToFileContent );

		} finally {
			Utils.deleteFilesRecursively( dir );
		}
	}
}
