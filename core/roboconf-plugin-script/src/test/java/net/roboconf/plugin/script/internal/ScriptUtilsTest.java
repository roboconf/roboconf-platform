/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.plugin.script.internal;

import java.io.File;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ExportedVariable;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.plugin.script.internal.ScriptUtils.ActionFileFilter;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ScriptUtilsTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testSetScriptsExecutableOnFile() throws Exception {

		File f = this.folder.newFile();
		Assert.assertFalse( f.canExecute());

		ScriptUtils.setScriptsExecutable( f );
		Assert.assertTrue( f.canExecute());
	}


	@Test
	public void testSetScriptsExecutableOnDirectory() throws Exception {

		File dir = this.folder.newFolder();
		File f = new File( dir, "whatever" );
		Assert.assertTrue( f.createNewFile());
		Assert.assertFalse( f.canExecute());

		ScriptUtils.setScriptsExecutable( dir );
		Assert.assertTrue( f.canExecute());
	}


	@Test
	public void testActionFileFilter() throws Exception {

		File dir = this.folder.newFolder();
		Assert.assertTrue( new File( dir, "whatever-1" ).createNewFile());
		Assert.assertTrue( new File( dir, "whatever-2" ).createNewFile());
		Assert.assertTrue( new File( dir, "what" ).createNewFile());

		File[] files = dir.listFiles( new ActionFileFilter( "" ));
		Assert.assertNotNull( files );
		Assert.assertEquals( 0, files.length );

		files = dir.listFiles( new ActionFileFilter( null ));
		Assert.assertNotNull( files );
		Assert.assertEquals( 0, files.length );

		files = dir.listFiles( new ActionFileFilter( "what" ));
		Assert.assertNotNull( files );
		Assert.assertEquals( 3, files.length );

		files = dir.listFiles( new ActionFileFilter( "whatever" ));
		Assert.assertNotNull( files );
		Assert.assertEquals( 2, files.length );

		files = dir.listFiles( new ActionFileFilter( "whatever-1" ));
		Assert.assertNotNull( files );
		Assert.assertEquals( 1, files.length );
	}


	@Test
	public void testFormatExportedVars_singleInstance() {

		Component c = new Component( "c" );
		c.exportedVariables.put( "forced-ip", new ExportedVariable( "forced-ip", "127.0.0.1" ));
		c.exportedVariables.put( "port", new ExportedVariable( "port", "49535" ));

		Instance i = new Instance( "i" ).component( c );
		i.overriddenExports.put( "something", "else" );

		Map<String,String> exportedVariables = ScriptUtils.formatExportedVars( i );
		Assert.assertEquals( 3, exportedVariables.size());
		Assert.assertEquals( "127.0.0.1", exportedVariables.get( "forced_ip" ));
		Assert.assertEquals( "49535", exportedVariables.get( "port" ));
		Assert.assertEquals( "else", exportedVariables.get( "something" ));
	}


	@Test
	public void testFormatExportedVars_severalInstances() {

		TestApplication app = new TestApplication();
		app.getTomcat().getComponent().exportedVariables.put( "path", new ExportedVariable( "path", "apps" ));

		Map<String,String> exportedVariables = ScriptUtils.formatExportedVars( app.getTomcatVm());
		Assert.assertEquals( 0, exportedVariables.size());

		exportedVariables = ScriptUtils.formatExportedVars( app.getTomcat());
		Assert.assertEquals( 1, exportedVariables.size());
		Assert.assertEquals( "apps", exportedVariables.get( "path" ));

		exportedVariables = ScriptUtils.formatExportedVars( app.getWar());
		Assert.assertEquals( 3, exportedVariables.size());
		Assert.assertEquals( "apps", exportedVariables.get( "ANCESTOR_tomcat_path" ));
		Assert.assertEquals( "8080", exportedVariables.get( "port" ));
		Assert.assertNull( exportedVariables.get( "ip" ));
	}
}
