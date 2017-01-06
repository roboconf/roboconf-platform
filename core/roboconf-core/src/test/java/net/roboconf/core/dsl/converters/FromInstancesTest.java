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

package net.roboconf.core.dsl.converters;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.dsl.ParsingModelIo;
import net.roboconf.core.dsl.ParsingModelValidator;
import net.roboconf.core.dsl.parsing.FileDefinition;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.RuntimeModelValidator;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ExportedVariable;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;

/**
 * @author Vincent Zurczak - Linagora
 */
public class FromInstancesTest {

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();


	@Test
	public void testZeroInstance() throws Exception {

		FileDefinition def = new FromInstances().buildFileDefinition( new ArrayList<Instance>( 0 ), new File( "whatever.txt" ), false, false );
		Assert.assertEquals( FileDefinition.INSTANCE, def.getFileType());
		Assert.assertEquals( 0, def.getBlocks().size());
	}


	@Test
	public void testOneInstance() throws Exception {

		Graphs graphs = buildGraphs();
		Component vmComponent = ComponentHelpers.findComponent( graphs, "VM" );
		Assert.assertNotNull( vmComponent );

		Instance inst = new Instance( "inst" ).component( vmComponent );
		compareInstances( graphs, Arrays.asList( inst ), false, false );
	}


	@Test
	public void testOneInstanceWithChildren() throws Exception {

		Graphs graphs = buildGraphs();
		Component vmComponent = ComponentHelpers.findComponent( graphs, "VM" );
		Assert.assertNotNull( vmComponent );

		Component tomcatComponent = ComponentHelpers.findComponent( graphs, "Tomcat" );
		Assert.assertNotNull( tomcatComponent );

		Component warComponent = ComponentHelpers.findComponent( graphs, "WAR" );
		Assert.assertNotNull( warComponent );

		Instance vm = new Instance( "i-vm" ).component( vmComponent );
		Instance tomcat = new Instance( "i-tomcat" ).component( tomcatComponent );
		Instance war = new Instance( "i-war" ).component( warComponent );

		tomcat.overriddenExports.put( "Tomcat.port", "9004" );
		InstanceHelpers.insertChild( vm, tomcat );
		InstanceHelpers.insertChild( tomcat, war );

		vm.channel( "channel1" ).channel( "channel2" );
		war.data.put( "prop1", "value1" );
		war.data.put( "prop2", "value2" );

		compareInstances( graphs, Arrays.asList( vm ), false, true );
	}


	@Test
	public void testComplexInstances() throws Exception {

		// Graphs
		Graphs graphs = buildGraphs();
		Component vmComponent = ComponentHelpers.findComponent( graphs, "VM" );
		Assert.assertNotNull( vmComponent );

		Component tomcatComponent = ComponentHelpers.findComponent( graphs, "Tomcat" );
		Assert.assertNotNull( tomcatComponent );

		Component warComponent = ComponentHelpers.findComponent( graphs, "WAR" );
		Assert.assertNotNull( warComponent );

		// 1st instance
		Instance vm = new Instance( "i-vm-1" ).component( vmComponent );
		Instance tomcat = new Instance( "i-tomcat" ).component( tomcatComponent );
		Instance war = new Instance( "i-war" ).component( warComponent );

		tomcat.overriddenExports.put( "Tomcat.port", "9004" );
		InstanceHelpers.insertChild( vm, tomcat );
		InstanceHelpers.insertChild( tomcat, war );

		// 2nd instance
		Instance vm2 = new Instance( "i-vm-2" ).component( vmComponent );

		// 3rd instance
		Instance vm3 = new Instance( "i-vm-3" ).component( vmComponent );
		Instance tomcat2 = new Instance( "i-tomcat-1" ).component( tomcatComponent );
		Instance tomcat3 = new Instance( "i-tomcat-2" ).component( tomcatComponent );
		Instance war2 = new Instance( "i-war" ).component( warComponent );

		tomcat3.overriddenExports.put( "Tomcat.port", "9081" );
		InstanceHelpers.insertChild( vm3, tomcat2 );
		InstanceHelpers.insertChild( vm3, tomcat3 );
		InstanceHelpers.insertChild( tomcat3, war2 );

		compareInstances( graphs, Arrays.asList( vm, vm2, vm3 ), false, true );
	}


	@Test
	public void testOneInstanceWithComments() throws Exception {

		Graphs graphs = buildGraphs();
		Component vmComponent = ComponentHelpers.findComponent( graphs, "VM" );
		Assert.assertNotNull( vmComponent );

		Instance inst = new Instance( "inst" ).component( vmComponent );
		compareInstances( graphs, Arrays.asList( inst ), true, false );
	}


	@Test
	public void testExtraData() throws Exception {

		// Parse
		Component vmComponent = new Component( "VM" ).installerName( "target" );
		Component tomcatComponent = new Component( "Tomcat" ).installerName( "puppet" );
		tomcatComponent.addExportedVariable( new ExportedVariable( "tomcat.ip", null ));
		tomcatComponent.addExportedVariable( new ExportedVariable( "tomcat.port", "8080" ));

		vmComponent.addChild( tomcatComponent );
		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( vmComponent );

		File f = TestUtils.findTestFile( "/configurations/valid/instance-with-extra-data.instances" );
		FromInstanceDefinition fromDef = new FromInstanceDefinition( f.getParentFile());
		Collection<Instance> rootInstances = fromDef.buildInstances( graphs, f );

		Assert.assertEquals( 0, fromDef.getErrors().size());
		Assert.assertEquals( 1, rootInstances.size());

		Instance vmInstance = rootInstances.iterator().next();
		Assert.assertEquals( 1, vmInstance.getChildren().size());
		Assert.assertEquals( "VM1", vmInstance.getName());
		Assert.assertEquals( 1, vmInstance.data.size());
		Assert.assertEquals( "192.168.1.10", vmInstance.data.get( "ec2.elastic.ip" ));

		// Write
		compareInstances( graphs, Arrays.asList( vmInstance ), true, true );
	}


	private Graphs buildGraphs() {
		Graphs graphs = new Graphs();

		Component vmComponent = new Component( "VM" ).installerName( "target" );
		graphs.getRootComponents().add( vmComponent );

		Component tomcatComponent = new Component( "Tomcat" ).installerName( "puppet" );
		tomcatComponent.addExportedVariable( new ExportedVariable( "Tomcat.ip", null ));
		tomcatComponent.addExportedVariable( new ExportedVariable( "Tomcat.port", "8080" ));
		vmComponent.addChild( tomcatComponent );

		Component warComponent = new Component( "WAR" ).installerName( "script" );
		tomcatComponent.addChild( warComponent );

		Assert.assertEquals( 0, RuntimeModelValidator.validate( graphs ).size());
		return graphs;
	}


	/**
	 * Compares an in-memory instances with their written/read version.
	 * @param rootInstances the root instances
	 * @param graphs the graph(s) to rebuild the instances
	 * @param writeComments true to write the comments, false otherwise
	 * @throws Exception
	 */
	private void compareInstances( Graphs graphs, List<Instance> rootInstances, boolean writeComments, boolean saveRuntimeInformation ) throws Exception {

		for( Instance rootInstance : rootInstances ) {
			List<Instance> allTheInstances = InstanceHelpers.buildHierarchicalList( rootInstance );
			Assert.assertEquals( 0, RuntimeModelValidator.validate( allTheInstances ).size());
		}

		File targetFile = this.testFolder.newFile( "roboconf_test.instances" );
		FileDefinition defToWrite = new FromInstances().buildFileDefinition( rootInstances, targetFile, writeComments, saveRuntimeInformation );
		ParsingModelIo.saveRelationsFile( defToWrite, writeComments, System.getProperty( "line.separator" ));

		// Load the saved file
		FileDefinition def = ParsingModelIo.readConfigurationFile( targetFile, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());
		Assert.assertEquals( FileDefinition.INSTANCE, def.getFileType());

		Collection<ParsingError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromInstanceDefinition fromDef = new FromInstanceDefinition( null );
		Collection<Instance> newRootInstances = fromDef.buildInstances( graphs, targetFile );
		Assert.assertEquals(  0, fromDef.getErrors().size());
		for( Instance rootInstance : newRootInstances ) {
			List<Instance> allTheInstances = InstanceHelpers.buildHierarchicalList( rootInstance );
			Assert.assertEquals(  0, RuntimeModelValidator.validate( allTheInstances ).size());
		}

		// Compare the Instances
		Assert.assertEquals( rootInstances.size(), newRootInstances.size());
		for( Instance rootInstance : rootInstances ) {

			// We have the same number of instances
			Application tempApp = new Application( new ApplicationTemplate());
			tempApp.getRootInstances().addAll( newRootInstances );

			String rootInstancePath = InstanceHelpers.computeInstancePath( rootInstance );
			Instance newRootInstance = InstanceHelpers.findInstanceByPath( tempApp, rootInstancePath );
			Assert.assertNotNull( newRootInstance );

			Collection<Instance> instances = InstanceHelpers.buildHierarchicalList( rootInstance );
			Collection<Instance> newInstances = InstanceHelpers.buildHierarchicalList( newRootInstance );
			Assert.assertEquals( instances.size(), newInstances.size());

			instances.removeAll( newInstances );
			Assert.assertEquals( 0, instances.size());

			// Compare the properties of all the instances
			for( Instance newInstance : newInstances ) {
				String newPath = InstanceHelpers.computeInstancePath( newInstance );
				Instance instance = InstanceHelpers.findInstanceByPath( rootInstance, newPath );
				Assert.assertNotNull( instance );

				Assert.assertEquals( instance.channels, newInstance.channels );
				Assert.assertEquals( instance.getComponent(), newInstance.getComponent());
				Assert.assertEquals( instance.getStatus(), newInstance.getStatus());
				Assert.assertEquals( instance.getChildren().size(), newInstance.getChildren().size());
				Assert.assertEquals( instance.data, newInstance.data );
				Assert.assertEquals( instance.overriddenExports.size(), newInstance.overriddenExports.size());

				for( Map.Entry<String,String> entry : instance.overriddenExports.entrySet()) {
					Assert.assertTrue( instance.getName(), newInstance.overriddenExports.containsKey( entry.getKey()));
					String value = newInstance.overriddenExports.get( entry.getKey());
					Assert.assertEquals( instance.getName(), entry.getValue(), value );
				}
			}
		}
	}
}
