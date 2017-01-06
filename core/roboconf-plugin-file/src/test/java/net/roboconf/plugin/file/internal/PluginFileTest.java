/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.plugin.file.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.plugin.file.internal.PluginFile.Action;
import net.roboconf.plugin.file.internal.PluginFile.ActionType;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PluginFileTest {

	@Test
	public void testPlugin() throws Exception {

		PluginFile pl = new PluginFile();
		Assert.assertEquals( PluginFile.PLUGIN_NAME, pl.getPluginName());

		// Make sure we can invoke invoke method in any order
		pl.setNames( "My Agent", null );

		pl.initialize( null );
		pl.initialize( new Instance( "inst" ));
	}


	@Test
	public void testAction() {

		Action action = new Action( 1, ActionType.DOWNLOAD, "http://..." );
		Assert.assertEquals( action, action );

		Action action2 = new Action( 1, ActionType.DOWNLOAD, "http://something else" );
		Assert.assertEquals( action, action2 );

		Action action3 = new Action( 2, ActionType.DOWNLOAD, "http://..." );
		Assert.assertNotSame( action, action3 );
		Assert.assertFalse( action.equals( action3 ));

		Action action4 = new Action( 1, ActionType.COPY, "http://..." );
		Assert.assertNotSame( action, action4 );
		Assert.assertFalse( action.equals( action4 ));

		Assert.assertNotSame( action, "a string" );
		Assert.assertFalse( action.equals( "a string" ));

		Assert.assertNotSame( 0, action.hashCode());
		Assert.assertNotSame( 0, new Action( 1, null, null ).hashCode());
	}


	@Test
	public void testReadProperties() throws Exception {

		Instance instance = new Instance( "inst" ).component( new Component( "comp" ).installerName( "something" ));
		PluginFile plugin = new PluginFile();

		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( instance );
		Utils.deleteFilesRecursively( instanceDirectory );

		Properties properties = plugin.readProperties( instance );
		Assert.assertEquals( 0, properties.size());
		Assert.assertTrue( instanceDirectory.mkdirs());

		File file = new File( instanceDirectory, PluginFile.FILE_NAME );
		String content = "toto = 1\ntiti=something\n1.re.s = pou";
		Utils.writeStringInto( content, file );

		properties = plugin.readProperties( instance );
		Assert.assertEquals( 3, properties.size());
		Assert.assertEquals( "1", properties.getProperty( "toto" ));
		Assert.assertEquals( "something", properties.getProperty( "titi" ));
		Assert.assertEquals( "pou", properties.getProperty( "1.re.s" ));

		Utils.deleteFilesRecursively( instanceDirectory );
	}


	@Test
	public void testFindActions() {

		Properties properties = new Properties();
		properties.setProperty( "deploy.1.download", "http://..." );
		properties.setProperty( "deploy.0.ignore", "oops" );
		properties.setProperty( "DeploY.2.copy", "/tmp/roboconf_tmp_file -> /home/toto/file.txt" );
		properties.setProperty( "start.1.delete", "/tmp/roboconf_tmp_file" );
		properties.setProperty( "deploy.-50.hop", "invalid" );
		properties.setProperty( "deploy.to.server", "invalid" );
		properties.setProperty( "", "" );

		PluginFile plugin = new PluginFile();
		Set<Action> actions = plugin.findActions( "deploy", properties );
		Assert.assertEquals( 3, actions.size());

		List<ActionType> orderedActions = new ArrayList<ActionType> ();
		Map<ActionType,Action> typeToAction = new HashMap<ActionType,Action> ();
		for( Action action : actions ) {
			orderedActions.add( action.actionType );
			typeToAction.put( action.actionType, action );
		}

		Assert.assertEquals( ActionType.NOTHING, orderedActions.get( 0 ));
		Assert.assertEquals( ActionType.DOWNLOAD, orderedActions.get( 1 ));
		Assert.assertEquals( ActionType.COPY, orderedActions.get( 2 ));

		Action action = typeToAction.get( ActionType.NOTHING );
		Assert.assertEquals( "oops", action.parameter );
		Assert.assertEquals( 0, action.position );

		action = typeToAction.get( ActionType.DOWNLOAD );
		Assert.assertEquals( "http://...", action.parameter );
		Assert.assertEquals( 1, action.position );

		action = typeToAction.get( ActionType.COPY );
		Assert.assertEquals( "/tmp/roboconf_tmp_file -> /home/toto/file.txt", action.parameter );
		Assert.assertEquals( 2, action.position );

		actions = plugin.findActions( "STARt", properties );
		Assert.assertEquals( 1, actions.size());
		action = actions.iterator().next();

		Assert.assertEquals( "/tmp/roboconf_tmp_file", action.parameter );
		Assert.assertEquals( 1, action.position );
		Assert.assertEquals( ActionType.DELETE, action.actionType );
	}


	@Test
	public void testExecute() throws Exception {

		Instance instance = new Instance( "inst" ).component( new Component( "c" ).installerName( "whatever" ));
		PluginFile plugin = new PluginFile();
		plugin.setNames( "my-app", "my-root" );

		// We execute a full life cycle here.
		// Let's prepare it.
		File instructionsFile = TestUtils.findTestFile( "/instructions.properties" );
		Assert.assertTrue( instructionsFile.exists());

		File resourceFile = TestUtils.findTestFile( "/resource.txt" );
		Assert.assertTrue( resourceFile.exists());

		String s = Utils.readFileContent( instructionsFile );
		s = s.replace( "$URL", resourceFile.toURI().toURL().toString());

		String path = System.getProperty( "java.io.tmpdir" );
		if( path.endsWith( "/" ))
			path = path.substring( 0, path.length() - 1 );

		s = s.replace( "$TMP", path );

		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( instance );
		Utils.deleteFilesRecursively( instanceDirectory );
		Assert.assertTrue( instanceDirectory.mkdirs());

		File targetFile = new File( instanceDirectory, PluginFile.FILE_NAME );
		Utils.writeStringInto( s, targetFile );
		Assert.assertTrue( targetFile.exists());


		// Now, let's run it.
		File tmpFile = new File( path, PluginFile.TMP_FILE );
		File bckFile = new File( path, PluginFile.TMP_FILE + "_bck" );
		File file_2 = new File( path, PluginFile.TMP_FILE + "_2" );

		Utils.deleteFilesRecursively( tmpFile, bckFile, file_2 );
		Assert.assertFalse( tmpFile.exists());
		Assert.assertFalse( bckFile.exists());
		Assert.assertFalse( file_2.exists());

		plugin.deploy( instance );
		Assert.assertTrue( tmpFile.exists());
		Assert.assertTrue( bckFile.exists());
		Assert.assertFalse( file_2.exists());

		plugin.start( instance );
		Assert.assertFalse( tmpFile.exists());
		Assert.assertTrue( bckFile.exists());
		Assert.assertTrue( file_2.exists());

		plugin.update( instance, null, null );
		Assert.assertFalse( tmpFile.exists());
		Assert.assertTrue( bckFile.exists());
		Assert.assertTrue( file_2.exists());

		plugin.stop( instance );
		Assert.assertTrue( tmpFile.exists());
		Assert.assertTrue( bckFile.exists());
		Assert.assertFalse( file_2.exists());

		plugin.undeploy( instance );
		Assert.assertFalse( tmpFile.exists());
		Assert.assertFalse( bckFile.exists());
		Assert.assertFalse( file_2.exists());


		// Clean...
		Utils.deleteFilesRecursively( instanceDirectory );
	}


	@Test
	public void testInvalidCopyParameters() throws Exception {

		// No error
		Action action = new Action( 1, ActionType.COPY, "my target only" );
		new PluginFile().executeAction( action );
	}


	@Test
	public void testInvalidMoveParameters() throws Exception {

		// No error
		Action action = new Action( 1, ActionType.MOVE, "1 -> 2 -> 3" );
		new PluginFile().executeAction( action );
	}
}
