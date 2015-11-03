/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.api.impl;

import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.ExportedVariable.RandomKind;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RandomManagerTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testGenerateAndReleaseRandomValues_noConflictBetweenApplications() throws Exception {

		// Two applications where the Tomcat port will be chosen randomly
		TestApplication app1 = new TestApplication();
		app1.setName( "app1" );
		app1.setDirectory( this.folder.newFolder());
		app1.getWar().getComponent().exportedVariables.get( "port" ).setRandom( true );
		app1.getWar().getComponent().exportedVariables.get( "port" ).setRawKind( RandomKind.PORT.toString());
		app1.getWar().getComponent().exportedVariables.get( "port" ).setValue( null );

		TestApplication app2 = new TestApplication();
		app2.setName( "app2" );
		app2.setDirectory( this.folder.newFolder());
		app2.getWar().getComponent().exportedVariables.get( "port" ).setRandom( true );
		app2.getWar().getComponent().exportedVariables.get( "port" ).setRawKind( RandomKind.PORT.toString());
		app2.getWar().getComponent().exportedVariables.get( "port" ).setValue( null );

		// Verify what happens with the random manager
		RandomMngrImpl mngr = new RandomMngrImpl();

		verify( app1.getWar(), "war.port", null );
		Assert.assertEquals( 0, mngr.agentToRandomPorts.size());
		mngr.generateAllRandomValues( app1 );
		verify( app1.getWar(), "war.port", "10000" );
		Assert.assertEquals( 1, mngr.agentToRandomPorts.size());

		verify( app2.getWar(), "war.port", null );
		mngr.generateAllRandomValues( app2 );
		verify( app2.getWar(), "war.port", "10000" );
		Assert.assertEquals( 2, mngr.agentToRandomPorts.size());

		// Release one
		mngr.releaseAllRandomValues( app1 );
		verify( app2.getWar(), "war.port", "10000" );
		Assert.assertEquals( 1, mngr.agentToRandomPorts.size());

		// Release an instance without any random
		mngr.releaseRandomValues( app2, app2.getMySql());
		verify( app2.getWar(), "war.port", "10000" );
		Assert.assertEquals( 1, mngr.agentToRandomPorts.size());

		// Release the right instance
		mngr.releaseRandomValues( app2, app2.getWar());
		Assert.assertEquals( 0, mngr.agentToRandomPorts.size());
	}


	@Test
	public void testGenerateAndReleaseRandomValues_noConflictBetweenAgents() throws Exception {

		// An application where the Tomcat and MySQL port will be chosen randomly
		TestApplication app1 = new TestApplication();
		app1.setName( "app1" );
		app1.setDirectory( this.folder.newFolder());

		app1.getWar().getComponent().exportedVariables.get( "port" ).setRandom( true );
		app1.getWar().getComponent().exportedVariables.get( "port" ).setRawKind( RandomKind.PORT.toString());
		app1.getWar().getComponent().exportedVariables.get( "port" ).setValue( null );

		app1.getMySql().getComponent().exportedVariables.get( "port" ).setRandom( true );
		app1.getMySql().getComponent().exportedVariables.get( "port" ).setRawKind( RandomKind.PORT.toString());
		app1.getMySql().getComponent().exportedVariables.get( "port" ).setValue( null );

		// Verify what happens with the random manager
		RandomMngrImpl mngr = new RandomMngrImpl();

		// The MySQL instance and the WAR are NOT on the same machine => they can use the same port
		verify( app1.getWar(), "war.port", null );
		verify( app1.getMySql(), "mysql.port", null );
		Assert.assertEquals( 0, mngr.agentToRandomPorts.size());

		mngr.generateAllRandomValues( app1 );

		verify( app1.getWar(), "war.port", "10000" );
		verify( app1.getMySql(), "mysql.port", "10000" );
		Assert.assertEquals( 2, mngr.agentToRandomPorts.size());

		// Release them all
		mngr.releaseAllRandomValues( app1 );
		Assert.assertEquals( 0, mngr.agentToRandomPorts.size());
	}


	@Test
	public void testGenerateAndReleaseRandomValues_noConflictOnAnAgent() throws Exception {

		// An application where the Tomcat port will be chosen randomly
		TestApplication app1 = new TestApplication();
		app1.setName( "app1" );
		app1.setDirectory( this.folder.newFolder());

		app1.getWar().getComponent().exportedVariables.get( "port" ).setRandom( true );
		app1.getWar().getComponent().exportedVariables.get( "port" ).setRawKind( RandomKind.PORT.toString());
		app1.getWar().getComponent().exportedVariables.get( "port" ).setValue( null );

		// Create a new WAR instance on the same VM!
		Instance newWar = new Instance( "new war" ).component( app1.getWar().getComponent());
		InstanceHelpers.insertChild( app1.getTomcat(), newWar );

		// Verify what happens with the random manager
		RandomMngrImpl mngr = new RandomMngrImpl();

		Assert.assertEquals( 0, mngr.agentToRandomPorts.size());
		verify( app1.getWar(), "war.port", null );
		verify( newWar, "war.port", null );

		mngr.generateAllRandomValues( app1 );

		verify( app1.getWar(), "war.port", "10000" );
		verify( newWar, "war.port", "10001" );
		Assert.assertEquals( 1, mngr.agentToRandomPorts.size());
		Assert.assertEquals( 2, mngr.agentToRandomPorts.values().iterator().next().size());

		// Release one instance
		mngr.releaseRandomValues( app1, app1.getWar());
		verify( newWar, "war.port", "10001" );
		Assert.assertEquals( 1, mngr.agentToRandomPorts.size());
		Assert.assertEquals( 1, mngr.agentToRandomPorts.values().iterator().next().size());

		// Release an instance without any random
		mngr.releaseRandomValues( app1, app1.getTomcat());
		verify( newWar, "war.port", "10001" );
		Assert.assertEquals( 1, mngr.agentToRandomPorts.size());
		Assert.assertEquals( 1, mngr.agentToRandomPorts.values().iterator().next().size());

		// Release the right instance
		mngr.releaseRandomValues( app1, newWar );
		Assert.assertEquals( 0, mngr.agentToRandomPorts.size());
	}


	@Test
	public void testGenerateAndReleaseRandomValues_randomButNotPort() throws Exception {

		// An application where the Tomcat port will be chosen randomly
		TestApplication app1 = new TestApplication();
		app1.setName( "app1" );
		app1.setDirectory( this.folder.newFolder());

		app1.getWar().getComponent().exportedVariables.get( "port" ).setRandom( true );
		app1.getWar().getComponent().exportedVariables.get( "port" ).setRawKind( "whatever" );
		app1.getWar().getComponent().exportedVariables.get( "port" ).setValue( null );

		// Verify what happens with the random manager
		RandomMngrImpl mngr = new RandomMngrImpl();
		verify( app1.getWar(), "war.port", null );
		Assert.assertEquals( 0, mngr.agentToRandomPorts.size());

		mngr.generateAllRandomValues( app1 );
		verify( app1.getWar(), "war.port", null );
		Assert.assertEquals( 0, mngr.agentToRandomPorts.size());

		mngr.releaseRandomValues( app1, app1.getWar());
		verify( app1.getWar(), "war.port", null );
		Assert.assertEquals( 0, mngr.agentToRandomPorts.size());
	}


	@Test
	public void testGenerateAndReleaseRandomValues_overriddenExportAlreadySet() throws Exception {

		// An application where the Tomcat port will be chosen randomly
		TestApplication app1 = new TestApplication();
		app1.setName( "app1" );
		app1.setDirectory( this.folder.newFolder());

		app1.getWar().getComponent().exportedVariables.get( "port" ).setRandom( true );
		app1.getWar().getComponent().exportedVariables.get( "port" ).setRawKind( RandomKind.PORT.toString());
		app1.getWar().getComponent().exportedVariables.get( "port" ).setValue( null );
		app1.getWar().overriddenExports.put( "port", "43210" );

		// Verify what happens with the random manager
		RandomMngrImpl mngr = new RandomMngrImpl();
		verify( app1.getWar(), "war.port", "43210" );
		Assert.assertEquals( 0, mngr.agentToRandomPorts.size());

		mngr.generateAllRandomValues( app1 );
		verify( app1.getWar(), "war.port", "43210" );
		Assert.assertEquals( 1, mngr.agentToRandomPorts.size());
	}


	@Test
	public void testRestoreRandomValuesCache_withPreset() throws Exception {

		// Model
		TestApplication app1 = new TestApplication();
		app1.setName( "app1" );
		app1.setDirectory( this.folder.newFolder());

		app1.getWar().getComponent().exportedVariables.get( "port" ).setRandom( true );
		app1.getWar().getComponent().exportedVariables.get( "port" ).setRawKind( RandomKind.PORT.toString());
		app1.getWar().getComponent().exportedVariables.get( "port" ).setValue( null );

		// Define an overridden export
		app1.getWar().overriddenExports.put( "port", "17401" );
		verify( app1.getWar(), "war.port", "17401" );

		// Verify what happens with the random manager
		RandomMngrImpl mngr = new RandomMngrImpl();
		Assert.assertEquals( 0, mngr.agentToRandomPorts.size());
		mngr.restoreRandomValuesCache( app1 );

		// The value cannot have changed
		verify( app1.getWar(), "war.port", "17401" );
		Assert.assertEquals( 1, mngr.agentToRandomPorts.size());
		Assert.assertEquals( 1, mngr.agentToRandomPorts.values().iterator().next().size());
		Assert.assertEquals( 17401, mngr.agentToRandomPorts.values().iterator().next().get( 0 ).intValue());
	}


	@Test
	public void testRestoreRandomValuesCache_noPreset() throws Exception {

		// Model
		TestApplication app1 = new TestApplication();
		app1.setName( "app1" );
		app1.setDirectory( this.folder.newFolder());

		app1.getWar().getComponent().exportedVariables.get( "port" ).setRandom( true );
		app1.getWar().getComponent().exportedVariables.get( "port" ).setRawKind( RandomKind.PORT.toString());
		app1.getWar().getComponent().exportedVariables.get( "port" ).setValue( null );

		// Verify what happens with the random manager
		RandomMngrImpl mngr = new RandomMngrImpl();
		Assert.assertEquals( 0, mngr.agentToRandomPorts.size());
		verify( app1.getWar(), "war.port", null );

		mngr.restoreRandomValuesCache( app1 );
		verify( app1.getWar(), "war.port", null );
		Assert.assertEquals( 0, mngr.agentToRandomPorts.size());
	}


	@Test
	public void testRestoreRandomValuesCache_conflictingPreset_differentAgents() throws Exception {

		// Model
		TestApplication app1 = new TestApplication();
		app1.setName( "app1" );
		app1.setDirectory( this.folder.newFolder());

		app1.getWar().getComponent().exportedVariables.get( "port" ).setRandom( true );
		app1.getWar().getComponent().exportedVariables.get( "port" ).setRawKind( RandomKind.PORT.toString());
		app1.getWar().getComponent().exportedVariables.get( "port" ).setValue( null );

		// Define an overridden export
		app1.getWar().overriddenExports.put( "port", "17401" );
		verify( app1.getWar(), "war.port", "17401" );

		// Proceed the same way with MySQL and set the SAME port
		app1.getMySql().getComponent().exportedVariables.get( "port" ).setRandom( true );
		app1.getMySql().getComponent().exportedVariables.get( "port" ).setRawKind( RandomKind.PORT.toString());
		app1.getMySql().getComponent().exportedVariables.get( "port" ).setValue( null );

		app1.getMySql().overriddenExports.put( "port", "17401" );
		verify( app1.getMySql(), "mysql.port", "17401" );

		// Verify what happens with the random manager
		RandomMngrImpl mngr = new RandomMngrImpl();
		Assert.assertEquals( 0, mngr.agentToRandomPorts.size());
		mngr.restoreRandomValuesCache( app1 );

		// The same port cannot be used twice, once will have been rewritten
		verify( app1.getWar(), "war.port", "17401" );
		verify( app1.getMySql(), "mysql.port", "17401" );
		Assert.assertEquals( 2, mngr.agentToRandomPorts.size());
	}


	@Test
	public void testRestoreRandomValuesCache_conflictingPreset_sameAgent() throws Exception {

		// Model
		TestApplication app1 = new TestApplication();
		app1.setName( "app1" );
		app1.setDirectory( this.folder.newFolder());

		app1.getWar().getComponent().exportedVariables.get( "port" ).setRandom( true );
		app1.getWar().getComponent().exportedVariables.get( "port" ).setRawKind( RandomKind.PORT.toString());
		app1.getWar().getComponent().exportedVariables.get( "port" ).setValue( null );

		// Define an overridden export
		app1.getWar().overriddenExports.put( "port", "17401" );
		verify( app1.getWar(), "war.port", "17401" );

		// Create a new WAR on the same agent and set the SAME port
		Instance newWar = new Instance( "new war" ).component( app1.getWar().getComponent());
		InstanceHelpers.insertChild( app1.getTomcat(), newWar );

		newWar.overriddenExports.put( "port", "17401" );
		verify( newWar, "war.port", "17401" );

		// Verify what happens with the random manager
		RandomMngrImpl mngr = new RandomMngrImpl();
		Assert.assertEquals( 0, mngr.agentToRandomPorts.size());
		mngr.restoreRandomValuesCache( app1 );

		// The same port cannot be used twice, once will have been rewritten
		verify( app1.getWar(), "war.port", "17401" );
		verify( newWar, "war.port", "10000" );
		Assert.assertEquals( 1, mngr.agentToRandomPorts.size());
		Assert.assertEquals( 2, mngr.agentToRandomPorts.values().iterator().next().size());
	}


	@Test
	public void testRestoreRandomValuesCache_noRandom() throws Exception {

		// Model
		TestApplication app1 = new TestApplication();
		app1.setName( "app1" );
		app1.setDirectory( this.folder.newFolder());

		// Verify what happens with the random manager
		RandomMngrImpl mngr = new RandomMngrImpl();
		Assert.assertEquals( 0, mngr.agentToRandomPorts.size());
		mngr.restoreRandomValuesCache( app1 );
		Assert.assertEquals( 0, mngr.agentToRandomPorts.size());
	}



	private void verify( Instance instance, String variableName, String expectedValue ) {

		Map<String,String> exportedVariables = InstanceHelpers.findAllExportedVariables( instance );
		Assert.assertTrue( exportedVariables.containsKey( variableName ));
		Assert.assertEquals( expectedValue, exportedVariables.get( variableName ));
	}
}
