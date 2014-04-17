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

package net.roboconf.core.model.converters;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.io.ParsingModelIo;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.validators.ParsingModelValidator;
import net.roboconf.core.model.validators.RuntimeModelValidator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class FromInstancesTest {

	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();


	@Test
	public void testZeroInstance() throws Exception {

		FileDefinition def = new FromInstances().buildFileDefinition( new ArrayList<Instance>( 0 ), new File( "whatever.txt" ), false );
		Assert.assertEquals( FileDefinition.INSTANCE, def.getFileType());
		Assert.assertEquals( 0, def.getBlocks().size());
	}


	@Test
	public void testOneInstance() throws Exception {

		Graphs graphs = buildGraphs();
		Component vmComponent = ComponentHelpers.findComponent( graphs, "VM" );
		Assert.assertNotNull( vmComponent );

		Instance inst = new Instance( "inst" ).component( vmComponent );
		compareInstances( graphs, Arrays.asList( inst ), false );
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

		tomcat.getOverriddenExports().put( "Tomcat.port", "9004" );
		InstanceHelpers.insertChild( vm, tomcat );
		InstanceHelpers.insertChild( tomcat, war );

		compareInstances( graphs, Arrays.asList( vm ), false );
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

		tomcat.getOverriddenExports().put( "Tomcat.port", "9004" );
		InstanceHelpers.insertChild( vm, tomcat );
		InstanceHelpers.insertChild( tomcat, war );

		// 2nd instance
		Instance vm2 = new Instance( "i-vm-2" ).component( vmComponent );

		// 3rd instance
		Instance vm3 = new Instance( "i-vm-3" ).component( vmComponent );
		Instance tomcat2 = new Instance( "i-tomcat-1" ).component( tomcatComponent );
		Instance tomcat3 = new Instance( "i-tomcat-2" ).component( tomcatComponent );
		Instance war2 = new Instance( "i-war" ).component( warComponent );

		tomcat3.getOverriddenExports().put( "Tomcat.port", "9081" );
		InstanceHelpers.insertChild( vm3, tomcat2 );
		InstanceHelpers.insertChild( vm3, tomcat3 );
		InstanceHelpers.insertChild( tomcat3, war2 );

		compareInstances( graphs, Arrays.asList( vm, vm2, vm3 ), false );
	}


	@Test
	public void testOneInstanceWithComments() throws Exception {

		Graphs graphs = buildGraphs();
		Component vmComponent = ComponentHelpers.findComponent( graphs, "VM" );
		Assert.assertNotNull( vmComponent );

		Instance inst = new Instance( "inst" ).component( vmComponent );
		compareInstances( graphs, Arrays.asList( inst ), true );
	}


	private Graphs buildGraphs() {
		Graphs graphs = new Graphs();

		Component vmComponent = new Component( "VM" ).alias( "VM" ).installerName( "iaas" );
		graphs.getRootComponents().add( vmComponent );

		Component tomcatComponent = new Component( "Tomcat" ).alias( "Tomcat" ).installerName( "puppet" );
		tomcatComponent.getExportedVariables().put( "Tomcat.ip", null );
		tomcatComponent.getExportedVariables().put( "Tomcat.port", "8080" );
		ComponentHelpers.insertChild( vmComponent, tomcatComponent );

		Component warComponent = new Component( "WAR" ).alias( "A simple web application" ).installerName( "bash" );
		ComponentHelpers.insertChild( tomcatComponent, warComponent );

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
	private void compareInstances( Graphs graphs, List<Instance> rootInstances, boolean writeComments ) throws Exception {

		for( Instance rootInstance : rootInstances ) {
			List<Instance> allTheInstances = InstanceHelpers.buildHierarchicalList( rootInstance );
			Assert.assertEquals( 0, RuntimeModelValidator.validate( allTheInstances ).size());
		}

		File targetFile = this.testFolder.newFile( "roboconf_test.instances" );
		FileDefinition defToWrite = new FromInstances().buildFileDefinition( rootInstances, targetFile, writeComments );
		ParsingModelIo.saveRelationsFile( defToWrite, writeComments, System.getProperty( "line.separator" ));

		// Load the saved file
		FileDefinition def = ParsingModelIo.readConfigurationFile( targetFile, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());
		Assert.assertEquals( FileDefinition.INSTANCE, def.getFileType());

		Collection<ModelError> validationErrors = ParsingModelValidator.validate( def );
		Assert.assertEquals( 0, validationErrors.size());

		FromInstanceDefinition fromDef = new FromInstanceDefinition( def );
		Collection<Instance> newRootInstances = fromDef.buildInstances( graphs );
		Assert.assertEquals(  0, fromDef.getErrors().size());
		for( Instance rootInstance : newRootInstances ) {
			List<Instance> allTheInstances = InstanceHelpers.buildHierarchicalList( rootInstance );
			Assert.assertEquals(  0, RuntimeModelValidator.validate( allTheInstances ).size());
		}

		// Compare the Instances
		Assert.assertEquals( rootInstances.size(), newRootInstances.size());
		for( Instance rootInstance : rootInstances ) {

			// We have the same number of instances
			Application tempApp = new Application();
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

				Assert.assertEquals( instance.getChannel(), newInstance.getChannel());
				Assert.assertEquals( instance.getComponent(), newInstance.getComponent());
				Assert.assertEquals( instance.getStatus(), newInstance.getStatus());
				Assert.assertEquals( instance.getChildren().size(), newInstance.getChildren().size());
				Assert.assertEquals( instance.getOverriddenExports().size(), newInstance.getOverriddenExports().size());

				for( Map.Entry<String,String> entry : instance.getOverriddenExports().entrySet()) {
					Assert.assertTrue( instance.getName(), newInstance.getOverriddenExports().containsKey( entry.getKey()));
					String value = newInstance.getOverriddenExports().get( entry.getKey());
					Assert.assertEquals( instance.getName(), entry.getValue(), value );
				}
			}
		}
	}
}
