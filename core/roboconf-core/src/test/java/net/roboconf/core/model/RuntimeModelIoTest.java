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

package net.roboconf.core.model;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.Constants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.ErrorCode.ErrorLevel;
import net.roboconf.core.errors.ErrorDetails.ErrorDetailsKind;
import net.roboconf.core.errors.RoboconfError;
import net.roboconf.core.errors.RoboconfErrorHelpers;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.RuntimeModelIo.GraphFileFilter;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ExportedVariable.RandomKind;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RuntimeModelIoTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testLoadApplication_Lamp_Legacy_1() throws Exception {

		File directory = TestUtils.findTestFile( "/applications/lamp-legacy-with-only-components" );
		ApplicationLoadResult result = RuntimeModelIo.loadApplication( directory );
		Assert.assertNotNull( result );
		Assert.assertNotNull( result.applicationTemplate );
		Assert.assertEquals( 4, result.loadErrors.size());
		for( RoboconfError error : result.loadErrors )
			Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, error.getErrorCode());

		Assert.assertEquals( "Legacy LAMP", result.applicationTemplate.getName());
		Assert.assertEquals( "A sample LAMP application", result.applicationTemplate.getDescription());
		Assert.assertEquals( "1.0.1-SNAPSHOT", result.applicationTemplate.getVersion());

		List<String> tags = Arrays.asList( "test", "lamp", "example" );
		Assert.assertEquals( new TreeSet<>( tags ), result.applicationTemplate.getTags());

		Assert.assertNotNull( result.applicationTemplate.getGraphs());
		Graphs g = result.applicationTemplate.getGraphs();
		Assert.assertEquals( 1, g.getRootComponents().size());

		Component vmComponent = g.getRootComponents().iterator().next();
		Assert.assertEquals( "VM", vmComponent.getName());
		Assert.assertEquals( "target", vmComponent.getInstallerName());
		Assert.assertEquals( 3, vmComponent.getChildren().size());

		for( Component childComponent : ComponentHelpers.findAllChildren( vmComponent )) {
			Map<String,String> exportedVariables = ComponentHelpers.findAllExportedVariables( childComponent );
			Map<String,ImportedVariable> importedVariables = ComponentHelpers.findAllImportedVariables( childComponent );
			Collection<Component> children = ComponentHelpers.findAllChildren( childComponent );

			if( "Tomcat".equals( childComponent.getName())) {
				Assert.assertEquals( "puppet", childComponent.getInstallerName());
				Assert.assertEquals( 0, children.size());

				Assert.assertEquals( 2, exportedVariables.size());
				Assert.assertTrue( exportedVariables.containsKey( "Tomcat.ip" ));
				Assert.assertEquals( "8009", exportedVariables.get( "Tomcat.portAJP" ));

				Assert.assertEquals( 2, importedVariables.size());
				Assert.assertFalse( importedVariables.get( "MySQL.ip" ).isOptional());
				Assert.assertFalse( importedVariables.get( "MySQL.ip" ).isExternal());
				Assert.assertFalse( importedVariables.get( "MySQL.port" ).isOptional());
				Assert.assertFalse( importedVariables.get( "MySQL.port" ).isExternal());

				SourceReference sr = result.getObjectToSource().get( childComponent );
				Assert.assertNotNull( sr );
				Assert.assertEquals( new File( directory, "graph/lamp.graph" ), sr.getSourceFile());
				Assert.assertEquals( 22, sr.getLine());

			} else if( "MySQL".equals( childComponent.getName())) {
				Assert.assertEquals( "puppet", childComponent.getInstallerName());
				Assert.assertEquals( 0, children.size());

				Assert.assertEquals( 2, exportedVariables.size());
				Assert.assertTrue( exportedVariables.containsKey( "MySQL.ip" ));
				Assert.assertNull( exportedVariables.get( "MySQL.port" ));
				Assert.assertEquals( 0, importedVariables.size());

				SourceReference sr = result.getObjectToSource().get( childComponent );
				Assert.assertNotNull( sr );
				Assert.assertEquals( new File( directory, "graph/lamp.graph" ), sr.getSourceFile());
				Assert.assertEquals( 16, sr.getLine());

			} else if( "Apache".equals( childComponent.getName())) {
				Assert.assertEquals( "puppet", childComponent.getInstallerName());
				Assert.assertEquals( 0, children.size());
				Assert.assertEquals( 0, exportedVariables.size());

				Assert.assertEquals( 2, childComponent.importedVariables.size());
				Assert.assertFalse( importedVariables.get( "Tomcat.ip" ).isOptional());
				Assert.assertFalse( importedVariables.get( "Tomcat.ip" ).isExternal());
				Assert.assertFalse( importedVariables.get( "Tomcat.portAJP" ).isOptional());
				Assert.assertFalse( importedVariables.get( "Tomcat.portAJP" ).isExternal());

				SourceReference sr = result.getObjectToSource().get( childComponent );
				Assert.assertNotNull( sr );
				Assert.assertEquals( new File( directory, "graph/lamp.graph" ), sr.getSourceFile());
				Assert.assertEquals( 30, sr.getLine());

			} else {
				Assert.fail( "Unrecognized child." );
			}
		}

		// Instances
		Assert.assertEquals( 3, result.applicationTemplate.getRootInstances().size());
		for( Instance i : result.applicationTemplate.getRootInstances()) {
			if( "Apache VM".equals( i.getName())) {
				Assert.assertEquals( "VM", i.getComponent().getName());
				Assert.assertEquals( 1, i.getChildren().size());

				Instance child = i.getChildren().iterator().next();
				Assert.assertEquals( 0, child.getChildren().size());
				Assert.assertEquals( i, child.getParent());
				Assert.assertEquals( "Apache", child.getName());
				Assert.assertEquals( "Apache", child.getComponent().getName());

			} else if( "MySQL VM".equals( i.getName())) {
				Assert.assertEquals( "VM", i.getComponent().getName());
				Assert.assertEquals( 1, i.getChildren().size());

				Instance child = i.getChildren().iterator().next();
				Assert.assertEquals( 0, child.getChildren().size());
				Assert.assertEquals( i, child.getParent());
				Assert.assertEquals( "MySQL", child.getName());
				Assert.assertEquals( "MySQL", child.getComponent().getName());

				Map<String,String> exportedVariables = InstanceHelpers.findAllExportedVariables( child );
				Assert.assertEquals( "3306", exportedVariables.get( "MySQL.port" ));
				Assert.assertNull( child.getComponent().exportedVariables.get( "MySQL.port" ));
				Assert.assertNotNull( child.getComponent().exportedVariables.get( "port" ));
				Assert.assertNull( child.getComponent().exportedVariables.get( "port" ).getValue());

			} else if( "Tomcat VM 1".equals( i.getName())) {
				Assert.assertEquals( "VM", i.getComponent().getName());
				Assert.assertEquals( 1, i.getChildren().size());

				Instance child = i.getChildren().iterator().next();
				Assert.assertEquals( 0, child.getChildren().size());
				Assert.assertEquals( i, child.getParent());
				Assert.assertEquals( "Tomcat", child.getName());
				Assert.assertEquals( "Tomcat", child.getComponent().getName());

			} else {
				Assert.fail( "Unrecognized instance." );
			}
		}
	}


	@Test
	public void testLoadApplication_Lamp_Legacy_2() throws Exception {

		File directory = TestUtils.findTestFile( "/applications/lamp-legacy-with-facets-and-so-on" );
		ApplicationLoadResult result = RuntimeModelIo.loadApplication( directory );
		Assert.assertNotNull( result );
		Assert.assertNotNull( result.applicationTemplate );
		Assert.assertEquals( 3, result.loadErrors.size());
		for( RoboconfError error : result.loadErrors )
			Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, error.getErrorCode());

		// Test the graph and descriptor
		Assert.assertEquals( "Legacy LAMP", result.applicationTemplate.getName());
		Assert.assertEquals( "A sample LAMP application", result.applicationTemplate.getDescription());
		Assert.assertEquals( "1.0.1-SNAPSHOT", result.applicationTemplate.getVersion());
		Assert.assertEquals( "roboconf-1.0", result.applicationTemplate.getDslId());

		Assert.assertNotNull( result.applicationTemplate.getGraphs());
		Graphs g = result.applicationTemplate.getGraphs();
		Assert.assertEquals( 3, g.getRootComponents().size());

		Set<String> rootComponentNames = new HashSet<> ();
		for( Component c : g.getRootComponents()) {

			rootComponentNames.add( c.getName());
			Assert.assertEquals( Constants.TARGET_INSTALLER, ComponentHelpers.findComponentInstaller( c ));
			Collection<Facet> facets = ComponentHelpers.findAllFacets( c );
			Assert.assertEquals( 1, facets.size());
			Assert.assertEquals( "Virtual_Machine", facets.iterator().next().getName());
			Assert.assertEquals( 3, ComponentHelpers.findAllChildren( c ).size());

			for( Component childComponent : ComponentHelpers.findAllChildren( c )) {
				Map<String,String> exportedVariables = ComponentHelpers.findAllExportedVariables( childComponent );
				Map<String,ImportedVariable> importedVariables = ComponentHelpers.findAllImportedVariables( childComponent );
				Collection<Component> children = ComponentHelpers.findAllChildren( childComponent );

				if( "Tomcat".equals( childComponent.getName())) {
					Assert.assertEquals( "puppet", childComponent.getInstallerName());
					Assert.assertEquals( 0, children.size());

					Assert.assertEquals( 1, childComponent.getFacets().size());
					Assert.assertEquals( "deployable", childComponent.getFacets().iterator().next().getName());

					Assert.assertEquals( 2, exportedVariables.size());
					Assert.assertTrue( exportedVariables.containsKey( "Tomcat.ip" ));
					Assert.assertEquals( "8009", exportedVariables.get( "Tomcat.portAJP" ));

					Assert.assertEquals( 2, importedVariables.size());
					Assert.assertFalse( importedVariables.get( "MySQL.ip" ).isOptional());
					Assert.assertFalse( importedVariables.get( "MySQL.ip" ).isExternal());
					Assert.assertFalse( importedVariables.get( "MySQL.port" ).isOptional());
					Assert.assertFalse( importedVariables.get( "MySQL.port" ).isExternal());

				} else if( "MySQL".equals( childComponent.getName())) {
					Assert.assertEquals( "puppet", childComponent.getInstallerName());
					Assert.assertEquals( 0, children.size());

					Assert.assertEquals( 1, childComponent.getFacets().size());
					Assert.assertEquals( "deployable", childComponent.getFacets().iterator().next().getName());

					Assert.assertEquals( 2, exportedVariables.size());
					Assert.assertTrue( exportedVariables.containsKey( "MySQL.ip" ));
					Assert.assertEquals( "3306", exportedVariables.get( "MySQL.port" ));
					Assert.assertEquals( 0, importedVariables.size());

				} else if( "Apache".equals( childComponent.getName())) {
					Assert.assertEquals( "puppet", childComponent.getInstallerName());
					Assert.assertEquals( 0, children.size());
					Assert.assertEquals( 0, exportedVariables.size());

					Assert.assertEquals( 1, childComponent.getFacets().size());
					Assert.assertEquals( "deployable", childComponent.getFacets().iterator().next().getName());

					Assert.assertEquals( 2, importedVariables.size());
					Assert.assertFalse( importedVariables.get( "Tomcat.ip" ).isOptional());
					Assert.assertFalse( importedVariables.get( "Tomcat.ip" ).isExternal());
					Assert.assertFalse( importedVariables.get( "Tomcat.portAJP" ).isOptional());
					Assert.assertFalse( importedVariables.get( "Tomcat.portAJP" ).isExternal());

				} else {
					Assert.fail( "Unrecognized child." );
				}
			}
		}

		// Check we got everything in the graph
		Assert.assertEquals( 3, rootComponentNames.size());
		Assert.assertTrue( rootComponentNames.contains( "VM" ));
		Assert.assertTrue( rootComponentNames.contains( "VM_EC2" ));
		Assert.assertTrue( rootComponentNames.contains( "VM_Openstack" ));

		Component vmOpenstack = ComponentHelpers.findComponent( g, "VM_Openstack" );
		Map<String,String> exportedVariables = ComponentHelpers.findAllExportedVariables( vmOpenstack );
		Assert.assertEquals( 4, exportedVariables.size());
		Assert.assertEquals( "else", exportedVariables.get( "VM_Openstack.something" ));
		Assert.assertNull( exportedVariables.get( "Virtual_Machine.ip" ));

		Assert.assertTrue( exportedVariables.containsKey( "Virtual_Machine.ip" ));
		Assert.assertTrue( exportedVariables.containsKey( "VM_Openstack.ip" ));
		Assert.assertTrue( exportedVariables.containsKey( "VM.ip" ));

		Assert.assertEquals( ComponentHelpers.findComponent( g, "VM" ), vmOpenstack.getExtendedComponent());
		Assert.assertNotNull( vmOpenstack.getExtendedComponent());

		// Test the instances
		Set<String> expectedPaths = new HashSet<> ();
		expectedPaths.add( "/Apache VM" );
		expectedPaths.add( "/Apache VM/Apache" );
		expectedPaths.add( "/MySQL VM" );
		expectedPaths.add( "/MySQL VM/MySQL" );
		expectedPaths.add( "/Tomcat VM 1" );
		expectedPaths.add( "/Tomcat VM 1/Tomcat" );
		expectedPaths.add( "/Tomcat VM 2" );
		expectedPaths.add( "/Tomcat VM 2/Tomcat" );
		expectedPaths.add( "/Tomcat VM 3" );
		expectedPaths.add( "/Tomcat VM 3/Tomcat" );

		Set<String> realPaths = new HashSet<> ();
		for( Instance inst : InstanceHelpers.getAllInstances( result.getApplicationTemplate()))
			realPaths.add( InstanceHelpers.computeInstancePath( inst ));

		Assert.assertEquals( expectedPaths.size(), realPaths.size());
		realPaths.removeAll( expectedPaths );
		Assert.assertEquals( 0, realPaths.size());

		Instance tomcat1 = InstanceHelpers.findInstanceByPath( result.getApplicationTemplate(), "/Tomcat VM 1/Tomcat" );
		Instance tomcat2 = InstanceHelpers.findInstanceByPath( result.getApplicationTemplate(), "/Tomcat VM 2/Tomcat" );
		Instance tomcat3 = InstanceHelpers.findInstanceByPath( result.getApplicationTemplate(), "/Tomcat VM 3/Tomcat" );

		Assert.assertEquals( tomcat1.getComponent(), tomcat2.getComponent());
		Assert.assertEquals( tomcat1.getComponent(), tomcat3.getComponent());

		Assert.assertEquals( tomcat1.channels, tomcat2.channels );
		Assert.assertEquals( tomcat1.channels, tomcat3.channels );

		Assert.assertEquals( 0, tomcat1.getChildren().size());
		Assert.assertEquals( 1, tomcat1.overriddenExports.size());
		Assert.assertEquals( "9021", tomcat1.overriddenExports.get( "portAJP" ));
		Assert.assertEquals( "9021", InstanceHelpers.findAllExportedVariables( tomcat1 ).get( "Tomcat.portAJP" ));

		Assert.assertEquals( 0, tomcat2.getChildren().size());
		Assert.assertEquals( 1, tomcat2.overriddenExports.size());
		Assert.assertEquals( "9021", tomcat2.overriddenExports.get( "portAJP" ));
		Assert.assertEquals( "9021", InstanceHelpers.findAllExportedVariables( tomcat2 ).get( "Tomcat.portAJP" ));

		Assert.assertEquals( 0, tomcat3.getChildren().size());
		Assert.assertEquals( 1, tomcat3.overriddenExports.size());
		Assert.assertEquals( "9021", tomcat3.overriddenExports.get( "portAJP" ));
		Assert.assertEquals( "9021", InstanceHelpers.findAllExportedVariables( tomcat3 ).get( "Tomcat.portAJP" ));
	}


	@Test
	public void testLoadApplication_Mongo() throws Exception {

		File directory = TestUtils.findTestFile( "/applications/mongo" );
		ApplicationLoadResult result = RuntimeModelIo.loadApplication( directory );
		Assert.assertNotNull( result );
		Assert.assertNotNull( result.applicationTemplate );
		Assert.assertEquals( 0, result.applicationTemplate.externalExports.size());

		Assert.assertEquals( 2, result.loadErrors.size());
		for( RoboconfError error : result.loadErrors )
			Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, error.getErrorCode());

		Assert.assertEquals( "Mongo", result.applicationTemplate.getName());
		Assert.assertNotNull( result.applicationTemplate.getGraphs());
		Graphs g = result.applicationTemplate.getGraphs();
		Assert.assertEquals( 1, g.getRootComponents().size());

		Component vmComponent = g.getRootComponents().iterator().next();
		Assert.assertEquals( "VM", vmComponent.getName());
		Assert.assertEquals( "target", vmComponent.getInstallerName());
		Assert.assertEquals( 0, vmComponent.getFacets().size());

		Collection<Component> children = ComponentHelpers.findAllChildren( vmComponent );
		Assert.assertEquals( 1, children.size());

		Component childComponent = children.iterator().next();
		Assert.assertEquals( "puppet", childComponent.getInstallerName());
		Assert.assertEquals( 0, childComponent.getChildren().size());

		Assert.assertEquals( 2, childComponent.exportedVariables.size());
		Assert.assertNull( childComponent.exportedVariables.get( "Mongo.ip" ));
		Assert.assertEquals( "27017", ComponentHelpers.findAllExportedVariables( childComponent ).get( "Mongo.port" ));

		Assert.assertEquals( 2, childComponent.importedVariables.size());
		Assert.assertTrue( childComponent.importedVariables.containsKey( "Mongo.ip" ));
		Assert.assertTrue( childComponent.importedVariables.containsKey( "Mongo.port" ));
	}


	@Test
	public void testLoadApplication_AppWithExternalDependencies() throws Exception {

		File directory = TestUtils.findTestFile( "/applications/app-with-external-dependencies" );
		ApplicationLoadResult result = RuntimeModelIo.loadApplication( directory );
		Assert.assertNotNull( result );
		Assert.assertNotNull( result.applicationTemplate );

		Assert.assertEquals( 2, result.applicationTemplate.externalExports.size());
		Assert.assertEquals( "DEP.c", result.applicationTemplate.externalExports.get( "VM.config" ));
		Assert.assertEquals( "DEP.ip", result.applicationTemplate.externalExports.get( "App.ip" ));

		Assert.assertEquals( 2, result.loadErrors.size());
		for( RoboconfError error : result.loadErrors )
			Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, error.getErrorCode());

		Assert.assertEquals( "app-with-dep", result.applicationTemplate.getName());
		Assert.assertNotNull( result.applicationTemplate.getGraphs());
		Graphs g = result.applicationTemplate.getGraphs();
		Assert.assertEquals( 1, g.getRootComponents().size());

		Component vmComponent = g.getRootComponents().iterator().next();
		Assert.assertEquals( "VM", vmComponent.getName());
		Assert.assertEquals( "target", vmComponent.getInstallerName());
		Assert.assertEquals( 0, vmComponent.getFacets().size());

		Collection<Component> children = ComponentHelpers.findAllChildren( vmComponent );
		Assert.assertEquals( 1, children.size());

		Component childComponent = children.iterator().next();
		Assert.assertEquals( "App", childComponent.getName());
		Assert.assertEquals( "logger", childComponent.getInstallerName());
		Assert.assertEquals( 0, childComponent.getChildren().size());

		Assert.assertEquals( 1, childComponent.exportedVariables.size());
		Assert.assertNull( childComponent.exportedVariables.get( "App.ip" ));

		Assert.assertEquals( 2, childComponent.importedVariables.size());
		Assert.assertTrue( childComponent.importedVariables.containsKey( "VM.config" ));
		Assert.assertFalse( childComponent.importedVariables.get( "VM.config" ).isOptional());
		Assert.assertFalse( childComponent.importedVariables.get( "VM.config" ).isExternal());

		Assert.assertTrue( childComponent.importedVariables.containsKey( "App1.test" ));
		Assert.assertFalse( childComponent.importedVariables.get( "App1.test" ).isOptional());
		Assert.assertTrue( childComponent.importedVariables.get( "App1.test" ).isExternal());

	}


	@Test
	public void testLoadApplicationErrors() throws Exception {
		File tempDirectory = this.folder.newFolder();

		// Descriptor
		Iterator<RoboconfError> iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.PROJ_NO_DESC_DIR, iterator.next().getErrorCode());

		File appDir = new File( tempDirectory, Constants.PROJECT_DIR_DESC );
		if( ! appDir.mkdir())
			throw new IOException( "Failed to create the descriptor directory." );

		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.PROJ_NO_DESC_FILE, iterator.next().getErrorCode());

		Properties props = new Properties();
		props.setProperty( ApplicationTemplateDescriptor.APPLICATION_NAME, "app-name" );
		props.setProperty( ApplicationTemplateDescriptor.APPLICATION_VERSION, "1.0" );
		props.setProperty( ApplicationTemplateDescriptor.APPLICATION_DSL_ID, "roboconf-1.0" );
		props.setProperty( ApplicationTemplateDescriptor.APPLICATION_GRAPH_EP, "main.graph" );
		Utils.writePropertiesFile( props, new File( appDir, Constants.PROJECT_FILE_DESCRIPTOR ));

		// Graph
		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.PROJ_NO_GRAPH_DIR, iterator.next().getErrorCode());

		File graphDir = new File( tempDirectory, Constants.PROJECT_DIR_GRAPH );
		if( ! graphDir.mkdir())
			throw new IOException( "Failed to create the graph directory." );

		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.PROJ_MISSING_GRAPH_EP, iterator.next().getErrorCode());

		File graphFile = new File( graphDir, "main.graph" );
		if( ! graphFile.createNewFile())
			throw new IOException( "Faild to create a graph file." );

		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.P_EMPTY_FILE, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_NO_ROOT_COMPONENT, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		Utils.writeStringInto( "   \n  \t\n\n ", graphFile );
		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.P_EMPTY_FILE, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.RM_NO_ROOT_COMPONENT, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		Utils.writeStringInto( "[ not supported by our parser ]", graphFile );
		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.P_UNRECOGNIZED_BLOCK, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.PM_INVALID_BLOCK_TYPE, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.CO_NOT_A_GRAPH, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		Utils.writeStringInto( "  \n\n [ not supported by our parser ] \n\n ", graphFile );
		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.P_UNRECOGNIZED_BLOCK, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.PM_INVALID_BLOCK_TYPE, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.CO_NOT_A_GRAPH, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		Utils.writeStringInto( "comp", graphFile );
		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.P_O_C_BRACKET_MISSING, iterator.next().getErrorCode());

		String fileContent = "my Toto {\n\tname: toto;\n}";
		Utils.copyStream( new ByteArrayInputStream( fileContent.getBytes( "UTF-8" )), graphFile );
		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, iterator.next().getErrorCode());

		fileContent = "instanceof Toto {\n\tname: toto;\n}";
		Utils.copyStream( new ByteArrayInputStream( fileContent.getBytes( "UTF-8" )), graphFile );
		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.PM_INVALID_NAME, iterator.next().getErrorCode());

		fileContent = "instance of Toto {\n\tname: toto;\n}";
		Utils.copyStream( new ByteArrayInputStream( fileContent.getBytes( "UTF-8" )), graphFile );
		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.CO_NOT_A_GRAPH, iterator.next().getErrorCode());

		fileContent = "facet MyFacet {\n}\n\nA {\n\tinstaller: target;\n\tfacets: MyFacet;\n}";
		Utils.copyStream( new ByteArrayInputStream( fileContent.getBytes( "UTF-8" )), graphFile );

		// Instances
		Collection<RoboconfError> errors = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors;
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.iterator().next().getErrorCode());

		props.setProperty( ApplicationTemplateDescriptor.APPLICATION_INSTANCES_EP, "init.instances" );
		Utils.writePropertiesFile( props, new File( appDir, Constants.PROJECT_FILE_DESCRIPTOR ));

		File instDir = new File( tempDirectory, Constants.PROJECT_DIR_INSTANCES );
		if( ! instDir.mkdir())
			throw new IOException( "Failed to create the instances directory." );

		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_MISSING_INSTANCE_EP, iterator.next().getErrorCode());

		File instancesFile = new File( instDir, "init.instances" );
		if( ! instancesFile.createNewFile())
			throw new IOException( "Failed to create " + instancesFile );

		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.P_EMPTY_FILE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		Utils.writeStringInto( "   \n  \t\n\n ", instancesFile );
		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.P_EMPTY_FILE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		Utils.writeStringInto( "[ not supported by our parser ]", instancesFile );
		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.P_UNRECOGNIZED_BLOCK, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.PM_INVALID_BLOCK_TYPE, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.CO_NOT_INSTANCES, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		Utils.writeStringInto( "  \n\n [ not supported by our parser ] \n\n ", instancesFile );
		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.P_UNRECOGNIZED_BLOCK, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.PM_INVALID_BLOCK_TYPE, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.CO_NOT_INSTANCES, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		Utils.writeStringInto( "  \n\n ip \n\n ", instancesFile );
		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.P_O_C_BRACKET_MISSING, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.PM_INVALID_BLOCK_TYPE, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.CO_NOT_INSTANCES, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		fileContent = "facet MyFacet {\n}\n\nA {\n\tinstaller: script;\n}";
		Utils.copyStream( new ByteArrayInputStream( fileContent.getBytes( "UTF-8" )), instancesFile );

		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.CO_NOT_INSTANCES, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		fileContent = "instance of A {\n\tname: toto;\n}";
		Utils.copyStream( new ByteArrayInputStream( fileContent.getBytes( "UTF-8" )), instancesFile );
		errors =  RuntimeModelIo.loadApplication( tempDirectory ).loadErrors;
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testLoadApplication_KarafJoramJndi() throws Exception {

		File directory = TestUtils.findTestFile( "/applications/karaf-joram-jndi" );
		ApplicationLoadResult result = RuntimeModelIo.loadApplication( directory );
		Assert.assertNotNull( result );
		Assert.assertNotNull( result.applicationTemplate );
		Assert.assertEquals( 6, result.loadErrors.size());
		for( RoboconfError error : result.loadErrors )
			Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, error.getErrorCode());

		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( result.getApplicationTemplate(), "/vmec2karaf" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( result.getApplicationTemplate(), "/vmec2karaf/karafec21" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( result.getApplicationTemplate(), "/vmec2karaf/karafec21/jndiec2" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( result.getApplicationTemplate(), "/vmec2karaf/karafec22" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( result.getApplicationTemplate(), "/vmec2karaf/karafec22/joramec2" ));
	}


	@Test
	public void testInvalidApplicationDescriptor() throws Exception {

		File appDirectory = this.folder.newFolder();
		File descDirectory = new File( appDirectory, Constants.PROJECT_DIR_DESC );
		if( ! descDirectory.mkdir())
			throw new IOException();

		File descFile = new File( descDirectory, Constants.PROJECT_FILE_DESCRIPTOR );
		if( ! descFile.createNewFile())
			throw new IOException();

		String content = "fail.read = true";
		Utils.copyStream( new ByteArrayInputStream( content.getBytes( "UTF-8" )), descFile );

		ApplicationLoadResult lr = RuntimeModelIo.loadApplication( appDirectory );
		Assert.assertTrue( lr.getLoadErrors().size() > 1 );
		Assert.assertEquals( ErrorCode.PROJ_READ_DESC_FILE, lr.getLoadErrors().iterator().next().getErrorCode());
	}


	@Test
	public void testWriteInstances_empty() throws Exception {

		File targetFile = this.folder.newFile();
		RuntimeModelIo.writeInstances( targetFile, new ArrayList<Instance>( 0 ));
		Assert.assertTrue( targetFile.exists());
		Assert.assertEquals( 0, targetFile.length());
	}


	@Test
	public void testWriteInstances_notEmpty() throws Exception {

		Component component = new Component( "comp" );
		Instance instance = new Instance( "inst" ).component( component ).status( InstanceStatus.DEPLOYING ).channel( "c" );
		instance.overriddenExports.put( "check", "true" );
		instance.data.put( "something", "else" );

		File targetFile = this.folder.newFile();
		RuntimeModelIo.writeInstances( targetFile, Arrays.asList( instance ));
		Assert.assertTrue( targetFile.exists());
		Assert.assertTrue( 0 < targetFile.length());
	}


	@Test
	public void testApplicationWithMissingGraph() throws Exception {

		File dir = this.folder.newFolder();
		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_DESC ).mkdir());
		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_GRAPH ).mkdir());
		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_INSTANCES ).mkdir());
		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_INSTANCES + "/model.instances" ).createNewFile());

		ApplicationTemplateDescriptor desc = new ApplicationTemplateDescriptor();
		desc.setName( "app name" );
		desc.setVersion( "3.0" );
		desc.setInstanceEntryPoint( "model.instances" );
		desc.setDslId( "roboconf-1.0" );

		ApplicationTemplateDescriptor.save( new File( dir, Constants.PROJECT_DIR_DESC + "/" + Constants.PROJECT_FILE_DESCRIPTOR ), desc );
		Iterator<RoboconfError> it = RuntimeModelIo.loadApplicationFlexibly( dir ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.RM_MISSING_APPLICATION_GEP, it.next().getErrorCode());
		Assert.assertEquals( ErrorCode.CO_GRAPH_COULD_NOT_BE_BUILT, it.next().getErrorCode());
	}


	@Test
	public void testApplicationWithoutInstances() throws Exception {

		File dir = this.folder.newFolder();
		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_DESC ).mkdir());
		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_GRAPH ).mkdir());
		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_INSTANCES ).mkdir());

		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_GRAPH + "/VM" ).mkdir());
		Utils.writeStringInto(
				"id: tid\nhandler: test\nname: n",
				new File( dir, Constants.PROJECT_DIR_GRAPH + "/VM/target.properties" ));

		File graphFile = new File( dir, Constants.PROJECT_DIR_GRAPH + "/app.graph" );
		Assert.assertTrue( graphFile.createNewFile());

		ApplicationTemplateDescriptor desc = new ApplicationTemplateDescriptor();
		desc.setName( "app name" );
		desc.setVersion( "3.0" );
		desc.setGraphEntryPoint( "app.graph" );
		desc.setDslId( "roboconf-1.0" );
		ApplicationTemplateDescriptor.save( new File( dir, Constants.PROJECT_DIR_DESC + "/" + Constants.PROJECT_FILE_DESCRIPTOR ), desc );

		Utils.writeStringInto( "VM {\ninstaller:target;\n}", graphFile );
		Assert.assertEquals( 0, RuntimeModelIo.loadApplication( dir ).loadErrors.size());
	}


	@Test
	public void testUnreachableFile() throws Exception {

		// Valid project
		File dir = this.folder.newFolder();
		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_DESC ).mkdir());
		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_GRAPH ).mkdir());
		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_INSTANCES ).mkdir());

		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_GRAPH + "/VM" ).mkdir());
		Utils.writeStringInto(
				"id: tid\nhandler: test\nname: n",
				new File( dir, Constants.PROJECT_DIR_GRAPH + "/VM/target.properties" ));

		ApplicationTemplateDescriptor desc = new ApplicationTemplateDescriptor();
		desc.setName( "app name" );
		desc.setVersion( "3.0" );
		desc.setGraphEntryPoint( "app.graph" );
		desc.setDslId( "roboconf-1.0" );
		ApplicationTemplateDescriptor.save( new File( dir, Constants.PROJECT_DIR_DESC + "/" + Constants.PROJECT_FILE_DESCRIPTOR ), desc );

		File graphFile = new File( dir, Constants.PROJECT_DIR_GRAPH + "/app.graph" );
		Utils.writeStringInto( "VM {\ninstaller:target;\n}", graphFile );

		// Unreachable files
		final int length = 3;
		File[] copies = new File[ length ];
		for( int i=0; i<length; i++ ) {
			copies[ i ] = new File( new File( dir, Constants.PROJECT_DIR_GRAPH ), "not-used-" + i + ".graph" );
			Utils.copyStream( graphFile, copies[ i ]);
		}

		// Verify
		List<RoboconfError> errors = new ArrayList<>( RuntimeModelIo.loadApplication( dir ).loadErrors );
		Assert.assertEquals( length, errors.size());
		for( int i=0; i<length; i++ ) {
			Assert.assertEquals( ParsingError.class, errors.get( i ).getClass());
			Assert.assertEquals( ErrorCode.PROJ_UNREACHABLE_FILE, errors.get( i ).getErrorCode());
			Assert.assertEquals( copies[ i ], ((ParsingError) errors.get( i )).getFile());
		}
	}


	@Test
	public void testApplicationWithInvalidTarget() throws Exception {

		File dir = this.folder.newFolder();
		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_DESC ).mkdir());
		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_GRAPH ).mkdir());
		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_INSTANCES ).mkdir());

		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_GRAPH + "/VM" ).mkdir());
		Utils.writeStringInto(
				"handler: test\nname: n",
				new File( dir, Constants.PROJECT_DIR_GRAPH + "/VM/target.properties" ));

		File graphFile = new File( dir, Constants.PROJECT_DIR_GRAPH + "/app.graph" );
		Assert.assertTrue( graphFile.createNewFile());

		ApplicationTemplateDescriptor desc = new ApplicationTemplateDescriptor();
		desc.setName( "app name" );
		desc.setVersion( "3.0" );
		desc.setGraphEntryPoint( "app.graph" );
		desc.setDslId( "roboconf-1.0" );
		ApplicationTemplateDescriptor.save( new File( dir, Constants.PROJECT_DIR_DESC + "/" + Constants.PROJECT_FILE_DESCRIPTOR ), desc );

		Utils.writeStringInto( "VM {\ninstaller:target;\n}", graphFile );
		Iterator<RoboconfError> it = RuntimeModelIo.loadApplication( dir ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.REC_TARGET_NO_ID, it.next().getErrorCode());
		Assert.assertFalse( it.hasNext());
	}


	@Test
	public void testInvalidFileLocation() throws Exception {

		// Valid project
		File dir = this.folder.newFolder();
		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_DESC ).mkdir());
		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_GRAPH ).mkdir());
		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_INSTANCES ).mkdir());

		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_GRAPH + "/VM" ).mkdir());
		Utils.writeStringInto(
				"id: tid\nhandler: test\nname: n",
				new File( dir, Constants.PROJECT_DIR_GRAPH + "/VM/target.properties" ));

		ApplicationTemplateDescriptor desc = new ApplicationTemplateDescriptor();
		desc.setName( "app name" );
		desc.setVersion( "3.0" );
		desc.setGraphEntryPoint( "app.graph" );
		desc.setDslId( "roboconf-1.0" );
		ApplicationTemplateDescriptor.save( new File( dir, Constants.PROJECT_DIR_DESC + "/" + Constants.PROJECT_FILE_DESCRIPTOR ), desc );

		File graphFile = new File( dir, Constants.PROJECT_DIR_GRAPH + "/app.graph" );
		Utils.writeStringInto( "VM {\ninstaller:target;\n}", graphFile );

		// File at an invalid location
		final int length = 3;
		File[] copies = new File[ length ];
		for( int i=0; i<length; i++ ) {
			copies[ i ] = new File( dir, "not-used-" + i + ".graph" );
			Utils.copyStream( graphFile, copies[ i ]);
		}

		// Such files are not validated
		File invalidInstFile = new File( dir, "invalid.instances" );
		Utils.writeStringInto( "inst of {", invalidInstFile );

		// Verify
		List<RoboconfError> errors = new ArrayList<>( RuntimeModelIo.loadApplication( dir ).loadErrors );
		Assert.assertEquals( length + 1, errors.size());
		for( int i=0; i<length; i++ ) {
			Assert.assertEquals( ParsingError.class, errors.get( i ).getClass());
			Assert.assertEquals( ErrorCode.PROJ_INVALID_FILE_LOCATION, errors.get( i ).getErrorCode());
			Assert.assertEquals( copies[ i ], ((ParsingError) errors.get( i )).getFile());
		}

		Assert.assertEquals( ParsingError.class, errors.get( length - 1 ).getClass());
		Assert.assertEquals( ErrorCode.PROJ_INVALID_FILE_LOCATION, errors.get( length - 1 ).getErrorCode());
		Assert.assertEquals( copies[ length - 1 ], ((ParsingError) errors.get( length - 1 )).getFile());
	}


	@Test
	public void testParsingWithRecipeProject() throws Exception {

		// Normal load
		File dir = TestUtils.findTestFile( "/reusable.recipe" );
		Assert.assertTrue( dir.exists());

		ApplicationLoadResult alr = RuntimeModelIo.loadApplication( dir );
		RoboconfErrorHelpers.filterErrorsForRecipes( alr );

		List<RoboconfError> errors = new ArrayList<>( alr.getLoadErrors());
		Assert.assertEquals( 2, errors.size());
		Assert.assertEquals( ErrorCode.PROJ_NO_DESC_DIR, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_UNREACHABLE_FILE, errors.get( 1 ).getErrorCode());

		// Flexible load
		alr = RuntimeModelIo.loadApplicationFlexibly( dir );
		RoboconfErrorHelpers.filterErrorsForRecipes( alr );
		Assert.assertEquals( 0, alr.getLoadErrors().size());
	}


	@Test
	public void testParsingWithInvalidRecipeProject_1() throws Exception {

		File dir = TestUtils.findTestFile( "/reusable.recipe.with.errors.1" );
		Assert.assertTrue( dir.exists());

		ApplicationLoadResult alr = RuntimeModelIo.loadApplicationFlexibly( dir );
		RoboconfErrorHelpers.filterErrorsForRecipes( alr );
		Assert.assertEquals( 1, alr.getLoadErrors().size());
		Assert.assertEquals( ErrorCode.RM_UNRESOLVABLE_VARIABLE, alr.getLoadErrors().iterator().next().getErrorCode());

		RoboconfError error = alr.getLoadErrors().iterator().next();
		Assert.assertEquals( 1, error.getDetails().length );
		Assert.assertEquals( ErrorDetailsKind.VARIABLE, error.getDetails()[ 0 ].getErrorDetailsKind());
		Assert.assertTrue( error.getDetails()[ 0 ].getElementName().contains( "f.*" ));
	}


	@Test
	public void testParsingWithInvalidRecipeProject_2() throws Exception {

		File dir = TestUtils.findTestFile( "/reusable.recipe.with.errors.2" );
		Assert.assertTrue( dir.exists());

		ApplicationLoadResult alr = RuntimeModelIo.loadApplicationFlexibly( dir );
		RoboconfErrorHelpers.filterErrorsForRecipes( alr );
		List<RoboconfError> errors = new ArrayList<>( alr.getLoadErrors());
		Assert.assertEquals( 2, errors.size());
		Assert.assertEquals( ErrorCode.RM_COMPONENT_IMPORTS_EXPORTS, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 1 ).getErrorCode());
	}


	@Test
	public void testGraphFileFilter() throws Exception {

		GraphFileFilter filter = new GraphFileFilter();
		Assert.assertFalse( filter.accept( new File( "inexisting" )));

		Assert.assertTrue( filter.accept( this.folder.newFile( "toto.graph" )));
		Assert.assertFalse( filter.accept( this.folder.newFile( "toto.txt" )));
		Assert.assertFalse( filter.accept( this.folder.newFolder( "sth.graph" )));
	}


	@Test
	public void testParsingWithRandomValues() throws Exception {

		File dir = TestUtils.findApplicationDirectory( "app-with-random-ports" );
		Assert.assertTrue( dir.isDirectory());

		ApplicationLoadResult alr = RuntimeModelIo.loadApplication( dir );
		Assert.assertFalse( RoboconfErrorHelpers.containsCriticalErrors( alr.getLoadErrors()));

		// Container 1
		Instance container1 = InstanceHelpers.findInstanceByPath( alr.getApplicationTemplate(), "/vm/container1" );
		Assert.assertNotNull( container1 );

		Assert.assertNull( container1.getComponent().exportedVariables.get( "httpPort" ).getValue());
		Assert.assertTrue( container1.getComponent().exportedVariables.get( "httpPort" ).isRandom());
		Assert.assertEquals( RandomKind.PORT, container1.getComponent().exportedVariables.get( "httpPort" ).getRandomKind());

		Assert.assertNull( container1.getComponent().exportedVariables.get( "ajpPort" ).getValue());
		Assert.assertTrue( container1.getComponent().exportedVariables.get( "ajpPort" ).isRandom());
		Assert.assertEquals( RandomKind.PORT, container1.getComponent().exportedVariables.get( "ajpPort" ).getRandomKind());

		Assert.assertEquals( "test", container1.getComponent().exportedVariables.get( "config" ).getValue());
		Assert.assertFalse( container1.getComponent().exportedVariables.get( "config" ).isRandom());
		Assert.assertNull( container1.getComponent().exportedVariables.get( "config" ).getRandomKind());

		Assert.assertNull( container1.getComponent().exportedVariables.get( "ip" ).getValue());
		Assert.assertFalse( container1.getComponent().exportedVariables.get( "ip" ).isRandom());
		Assert.assertNull( container1.getComponent().exportedVariables.get( "ip" ).getRandomKind());

		Map<String,String> exportedVariables = InstanceHelpers.findAllExportedVariables( container1 );
		Assert.assertEquals( "test", exportedVariables.get( "Container1.config" ));

		Assert.assertTrue( exportedVariables.containsKey( "Container1.ip" ));
		Assert.assertNull( exportedVariables.get( "Container1.ip" ));

		Assert.assertTrue( exportedVariables.containsKey( "Container1.httpPort" ));
		Assert.assertNull( exportedVariables.get( "Container1.httpPort" ));

		Assert.assertTrue( exportedVariables.containsKey( "Container1.ajpPort" ));
		Assert.assertNull( exportedVariables.get( "Container1.ajpPort" ));

		// Container 2
		Instance container2 = InstanceHelpers.findInstanceByPath( alr.getApplicationTemplate(), "/vm/container2" );
		Assert.assertNotNull( container2 );

		Assert.assertNull( container2.getComponent().exportedVariables.get( "port" ).getValue());
		Assert.assertTrue( container2.getComponent().exportedVariables.get( "port" ).isRandom());
		Assert.assertEquals( RandomKind.PORT, container2.getComponent().exportedVariables.get( "port" ).getRandomKind());

		Assert.assertNull( container2.getComponent().exportedVariables.get( "ip" ).getValue());
		Assert.assertFalse( container2.getComponent().exportedVariables.get( "ip" ).isRandom());
		Assert.assertNull( container2.getComponent().exportedVariables.get( "ip" ).getRandomKind());

		exportedVariables = InstanceHelpers.findAllExportedVariables( container2 );
		Assert.assertTrue( exportedVariables.containsKey( "Container2.ip" ));
		Assert.assertNull( exportedVariables.get( "Container2.ip" ));

		// This value is found in the instances.
		Assert.assertEquals( "45012", exportedVariables.get( "Container2.port" ));
	}


	@Test
	public void testParsingWithInvalidCommands() throws Exception {

		// Copy the application and update a command file
		File dir = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( dir.isDirectory());

		File newDir = this.folder.newFolder();
		Utils.copyDirectory( dir, newDir );

		File commandFile = new File( newDir, Constants.PROJECT_DIR_COMMANDS + "/scale.commands" );
		Assert.assertTrue( commandFile.isFile());

		File commandFileCopy = new File( newDir, Constants.PROJECT_DIR_COMMANDS + "/scale.invalid-extension" );
		Utils.copyStream( commandFile, commandFileCopy );
		Utils.writeStringInto( "this is an invalid command", commandFile );

		// Load it and verify it contains errors
		ApplicationLoadResult alr = RuntimeModelIo.loadApplication( newDir );
		List<RoboconfError> criticalErrors = new ArrayList<> ();

		for( RoboconfError error : alr.getLoadErrors()) {
			if( error.getErrorCode().getLevel() == ErrorLevel.SEVERE )
				criticalErrors.add( error );
		}

		Assert.assertEquals( 2, criticalErrors.size());
		Assert.assertEquals( ErrorCode.CMD_UNRECOGNIZED_INSTRUCTION, criticalErrors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_INVALID_COMMAND_EXT, criticalErrors.get( 1 ).getErrorCode());
	}


	@Test
	public void testParsingWithInvalidAutonomicRules() throws Exception {

		// Copy the application and update a command file
		File dir = TestUtils.findApplicationDirectory( "lamp" );
		Assert.assertTrue( dir.isDirectory());

		File newDir = this.folder.newFolder();
		Utils.copyDirectory( dir, newDir );

		File ruleFile = new File( newDir, Constants.PROJECT_DIR_RULES_AUTONOMIC + "/sample.drl" );
		Assert.assertTrue( ruleFile.isFile());

		File ruleFileCopy = new File( newDir, Constants.PROJECT_DIR_RULES_AUTONOMIC + "/sample.drl-invalid-extension" );
		Utils.copyStream( ruleFile, ruleFileCopy );

		String s = Utils.readFileContent( ruleFile );
		s = s.replace( "scale", "inexisting-command" );
		Utils.writeStringInto( s, ruleFile );

		// Load it and verify it contains errors
		ApplicationLoadResult alr = RuntimeModelIo.loadApplication( newDir );
		List<RoboconfError> criticalErrors = new ArrayList<> ();

		for( RoboconfError error : alr.getLoadErrors()) {
			if( error.getErrorCode().getLevel() == ErrorLevel.SEVERE )
				criticalErrors.add( error );
		}

		Assert.assertEquals( 2, criticalErrors.size());
		Assert.assertEquals( ErrorCode.RULE_UNKNOWN_COMMAND, criticalErrors.get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_INVALID_RULE_EXT, criticalErrors.get( 1 ).getErrorCode());
	}


	@Test
	public void testParsingWithInexistingDirectory() {

		ApplicationLoadResult alr = RuntimeModelIo.loadApplication( new File( "inexiting" ));
		Assert.assertNotNull( alr.getApplicationTemplate());
		Assert.assertNull( alr.getApplicationTemplate().getGraphs());
		Assert.assertNotSame( 0, alr.getLoadErrors().size());
	}


	@Test
	public void testParsingWithInstancesAndComponentExtensions() throws Exception {

		File dir = TestUtils.findApplicationDirectory( "component-extensions" );
		Assert.assertTrue( dir.isDirectory());

		ApplicationLoadResult alr = RuntimeModelIo.loadApplication( dir );
		Assert.assertFalse( RoboconfErrorHelpers.containsCriticalErrors( alr.getLoadErrors()));

		// Ambiguous overriding with multiple inheritance
		Instance ambiguous = InstanceHelpers.findInstanceByPath( alr.getApplicationTemplate(), "/container-bootstrap-vm/ambiguous" );
		Assert.assertNotNull( ambiguous );

		Map<String,String> exportedVariables = InstanceHelpers.findAllExportedVariables( ambiguous );
		Assert.assertEquals( 9, exportedVariables.size());

		Assert.assertEquals( "petals-sl-postgresql-9.4-1201-jdbc4", exportedVariables.get( "PetalsJBIComponent.componentId" ));
		Assert.assertEquals( "petals-sl-postgresql-9.4-1201-jdbc4", exportedVariables.get( "PetalsSL.componentId" ));
		Assert.assertEquals( "petals-sl-postgresql-9.4-1201-jdbc4", exportedVariables.get( "PetalsSLPostgreSQL.componentId" ));
		Assert.assertEquals( "petals-sl-postgresql-9.4-1201-jdbc4", exportedVariables.get( "componentId" ));

		Assert.assertEquals( "SL", exportedVariables.get( "PetalsSL.componentType" ));
		Assert.assertEquals( "SL", exportedVariables.get( "PetalsSLPostgreSQL.componentType" ));
		Assert.assertEquals( "true", exportedVariables.get( "PetalsSLPostgreSQL.present" ));

		Assert.assertEquals( "roboconf-demo", exportedVariables.get( "domainName" ));
		Assert.assertEquals( "roboconf-demo-1", exportedVariables.get( "subdomainName" ));

		// Specific overriding with multiple inheritance
		Instance specific = InstanceHelpers.findInstanceByPath( alr.getApplicationTemplate(), "/container-bootstrap-vm/specific" );
		Assert.assertNotNull( specific );

		exportedVariables = InstanceHelpers.findAllExportedVariables( specific );
		Assert.assertEquals( 7, exportedVariables.size());
		Assert.assertEquals( "comp", exportedVariables.get( "PetalsJBIComponent.componentId" ));
		Assert.assertEquals( "sl", exportedVariables.get( "PetalsSL.componentId" ));
		Assert.assertEquals( "last", exportedVariables.get( "PetalsSLPostgreSQL.componentId" ));
		Assert.assertNull( exportedVariables.get( "componentId" ));

		Assert.assertEquals( "SL", exportedVariables.get( "PetalsSL.componentType" ));
		Assert.assertEquals( "SL", exportedVariables.get( "PetalsSLPostgreSQL.componentType" ));
		Assert.assertEquals( "true", exportedVariables.get( "PetalsSLPostgreSQL.present" ));

		Assert.assertEquals( "roboconf-demo", exportedVariables.get( "domainName" ));
		Assert.assertNull( exportedVariables.get( "subdomainName" ));

		// Instance from a super component
		Instance superInstance = InstanceHelpers.findInstanceByPath( alr.getApplicationTemplate(), "/container-bootstrap-vm/superInstance" );
		Assert.assertNotNull( superInstance );

		exportedVariables = InstanceHelpers.findAllExportedVariables( superInstance );
		Assert.assertEquals( 4, exportedVariables.size());
		Assert.assertEquals( "my-sl", exportedVariables.get( "PetalsJBIComponent.componentId" ));
		Assert.assertEquals( "my-sl", exportedVariables.get( "PetalsSL.componentId" ));
		Assert.assertEquals( "my-sl", exportedVariables.get( "componentId" ));

		Assert.assertEquals( "SL", exportedVariables.get( "PetalsSL.componentType" ));

		Assert.assertNull( exportedVariables.get( "PetalsSLPostgreSQL.componentId" ));
		Assert.assertNull( exportedVariables.get( "domainName" ));
		Assert.assertNull( exportedVariables.get( "subdomainName" ));
	}


	@Test
	public void testLoadApplication_withCustomRoboconfVariables() throws Exception {

		File dir = TestUtils.findApplicationDirectory( "root-exporting-variables" );
		Assert.assertTrue( dir.isDirectory());

		ApplicationLoadResult alr = RuntimeModelIo.loadApplication( dir );
		Assert.assertFalse( RoboconfErrorHelpers.containsCriticalErrors( alr.getLoadErrors()));

		Component appComponent = ComponentHelpers.findComponent( alr.getApplicationTemplate(), "App" );
		Assert.assertNotNull( appComponent );

		Assert.assertEquals( 5, appComponent.exportedVariables.size());
		Assert.assertEquals( "$(ROBOCONF_INSTANCE_NAME)", appComponent.exportedVariables.get( "inst" ).getValue());
		Assert.assertEquals( "$(ROBOCONF_INSTANCE_PATH)", appComponent.exportedVariables.get( "path" ).getValue());
		Assert.assertEquals( "$(ROBOCONF_CLEAN_INSTANCE_PATH)", appComponent.exportedVariables.get( "clean_path" ).getValue());
		Assert.assertEquals( "kikou $(ROBOCONF_CLEAN_REVERSED_INSTANCE_PATH)", appComponent.exportedVariables.get( "rev" ).getValue());
		Assert.assertEquals( "$(ROBOCONF_COMPONENT_NAME)", appComponent.exportedVariables.get( "comp" ).getValue());

		Instance appInstance = InstanceHelpers.findInstanceByPath( alr.getApplicationTemplate(), "/vm1/app" );
		Assert.assertNotNull( appInstance );

		Map<String,String> resolvedExportedVariables = InstanceHelpers.findAllExportedVariables( appInstance );
		Assert.assertEquals( 5, appComponent.exportedVariables.size());
		Assert.assertEquals( "app", resolvedExportedVariables.get( "App.inst" ));
		Assert.assertEquals( "/vm1/app", resolvedExportedVariables.get( "App.path" ));
		Assert.assertEquals( "vm1_app", resolvedExportedVariables.get( "App.clean_path" ));
		Assert.assertEquals( "kikou app_vm1", resolvedExportedVariables.get( "App.rev" ));
		Assert.assertEquals( "App", resolvedExportedVariables.get( "App.comp" ));
	}


	@Test
	public void testLoadApplication_descriptorErrorsAreResolved() throws Exception {

		// Copy an application and overwrite the descriptor with an invalid one
		File sourceDirectory = TestUtils.findTestFile( "/applications/mongo" );
		File targetDirectory = this.folder.newFolder();
		Utils.copyDirectory( sourceDirectory, targetDirectory );

		File f = new File( targetDirectory, Constants.PROJECT_DIR_DESC + "/" + Constants.PROJECT_FILE_DESCRIPTOR );
		Assert.assertTrue( f.exists());

		String fContent = Utils.readFileContent( f );
		fContent = fContent.replaceAll( "(?i)application-version\\s*=.*", "application-version = @inval!d" );
		Utils.writeStringInto( fContent, f );

		// Parse it
		ApplicationLoadResult result = RuntimeModelIo.loadApplication( targetDirectory );
		Assert.assertNotNull( result );
		Assert.assertNotNull( result.applicationTemplate );
		Assert.assertEquals( 3, result.loadErrors.size());

		Iterator<RoboconfError> it = result.loadErrors.iterator();
		Assert.assertEquals( ErrorCode.RM_INVALID_APPLICATION_VERSION, it.next().getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, it.next().getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, it.next().getErrorCode());
		Assert.assertFalse( it.hasNext());

		// Resolve errors locations
		List<RoboconfError> errors = RoboconfErrorHelpers.resolveErrorsWithLocation( result );
		Assert.assertEquals( 3, errors.size());
		Assert.assertEquals( ErrorCode.RM_INVALID_APPLICATION_VERSION, errors.get( 0 ).getErrorCode());
		Assert.assertEquals( ParsingError.class, errors.get( 0 ).getClass());
		Assert.assertEquals( f, ((ParsingError) errors.get( 0 )).getFile());
		Assert.assertEquals( 3, ((ParsingError) errors.get( 0 )).getLine());

		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 1 ).getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.get( 2 ).getErrorCode());
	}
}
