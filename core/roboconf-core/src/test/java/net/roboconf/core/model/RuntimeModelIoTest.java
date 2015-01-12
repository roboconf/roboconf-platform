/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RuntimeModelIoTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testLoadApplication_Lamp_Legacy_1() throws Exception {

		File directory = TestUtils.findTestFile( "/applications/valid/lamp-legacy-with-only-components" );
		ApplicationLoadResult result = RuntimeModelIo.loadApplication( directory );
		Assert.assertNotNull( result );
		Assert.assertNotNull( result.application );
		Assert.assertEquals( 4, result.loadErrors.size());
		for( RoboconfError error : result.loadErrors )
			Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, error.getErrorCode());

		Assert.assertEquals( "Legacy LAMP", result.application.getName());
		Assert.assertEquals( "A sample LAMP application", result.application.getDescription());
		Assert.assertEquals( "sample", result.application.getQualifier());

		Assert.assertNotNull( result.application.getGraphs());
		Graphs g = result.application.getGraphs();
		Assert.assertEquals( 1, g.getRootComponents().size());

		Component vmComponent = g.getRootComponents().iterator().next();
		Assert.assertEquals( "VM", vmComponent.getName());
		Assert.assertEquals( "target", vmComponent.getInstallerName());
		Assert.assertEquals( 3, vmComponent.getChildren().size());

		for( Component childComponent : ComponentHelpers.findAllChildren( vmComponent )) {
			Map<String,String> exportedVariables = ComponentHelpers.findAllExportedVariables( childComponent );
			Map<String,Boolean> importedVariables = ComponentHelpers.findAllImportedVariables( childComponent );
			Collection<Component> children = ComponentHelpers.findAllChildren( childComponent );

			if( "Tomcat".equals( childComponent.getName())) {
				Assert.assertEquals( "puppet", childComponent.getInstallerName());
				Assert.assertEquals( 0, children.size());

				Assert.assertEquals( 2, exportedVariables.size());
				Assert.assertTrue( exportedVariables.containsKey( "Tomcat.ip" ));
				Assert.assertEquals( "8009", exportedVariables.get( "Tomcat.portAJP" ));

				Assert.assertEquals( 2, importedVariables.size());
				Assert.assertEquals( Boolean.FALSE, importedVariables.get( "MySQL.ip" ));
				Assert.assertEquals( Boolean.FALSE, importedVariables.get( "MySQL.port" ));

			} else if( "MySQL".equals( childComponent.getName())) {
				Assert.assertEquals( "puppet", childComponent.getInstallerName());
				Assert.assertEquals( 0, children.size());

				Assert.assertEquals( 2, exportedVariables.size());
				Assert.assertTrue( exportedVariables.containsKey( "MySQL.ip" ));
				Assert.assertEquals( "3306", exportedVariables.get( "MySQL.port" ));
				Assert.assertEquals( 0, importedVariables.size());

			} else if( "Apache".equals( childComponent.getName())) {
				Assert.assertEquals( "puppet", childComponent.getInstallerName());
				Assert.assertEquals( 0, children.size());
				Assert.assertEquals( 0, exportedVariables.size());

				Assert.assertEquals( 2, childComponent.importedVariables.size());
				Assert.assertEquals( Boolean.FALSE, importedVariables.get( "Tomcat.ip" ));
				Assert.assertEquals( Boolean.FALSE, importedVariables.get( "Tomcat.portAJP" ));

			} else {
				Assert.fail( "Unrecognized child." );
			}
		}

		// Instances
		Assert.assertEquals( 3, result.application.getRootInstances().size());
		for( Instance i : result.application.getRootInstances()) {
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

		File directory = TestUtils.findTestFile( "/applications/valid/lamp-legacy-with-facets-and-so-on" );
		ApplicationLoadResult result = RuntimeModelIo.loadApplication( directory );
		Assert.assertNotNull( result );
		Assert.assertNotNull( result.application );
		Assert.assertEquals( 3, result.loadErrors.size());
		for( RoboconfError error : result.loadErrors )
			Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, error.getErrorCode());

		// Test the graph and descriptor
		Assert.assertEquals( "Legacy LAMP", result.application.getName());
		Assert.assertEquals( "A sample LAMP application", result.application.getDescription());
		Assert.assertEquals( "sample", result.application.getQualifier());
		Assert.assertEquals( "net.roboconf", result.application.getNamespace());
		Assert.assertEquals( "roboconf-1.0", result.application.getDslId());

		Assert.assertNotNull( result.application.getGraphs());
		Graphs g = result.application.getGraphs();
		Assert.assertEquals( 3, g.getRootComponents().size());

		Set<String> rootComponentNames = new HashSet<String> ();
		for( Component c : g.getRootComponents()) {

			rootComponentNames.add( c.getName());
			Assert.assertEquals( Constants.TARGET_INSTALLER, ComponentHelpers.findComponentInstaller( c ));
			Collection<Facet> facets = ComponentHelpers.findAllFacets( c );
			Assert.assertEquals( 1, facets.size());
			Assert.assertEquals( "Virtual_Machine", facets.iterator().next().getName());
			Assert.assertEquals( 3, ComponentHelpers.findAllChildren( c ).size());

			for( Component childComponent : ComponentHelpers.findAllChildren( c )) {
				Map<String,String> exportedVariables = ComponentHelpers.findAllExportedVariables( childComponent );
				Map<String,Boolean> importedVariables = ComponentHelpers.findAllImportedVariables( childComponent );
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
					Assert.assertEquals( Boolean.FALSE, importedVariables.get( "MySQL.ip" ));
					Assert.assertEquals( Boolean.FALSE, importedVariables.get( "MySQL.port" ));

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
					Assert.assertEquals( Boolean.FALSE, importedVariables.get( "Tomcat.ip" ));
					Assert.assertEquals( Boolean.FALSE, importedVariables.get( "Tomcat.portAJP" ));

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
		Assert.assertEquals( 2, exportedVariables.size());
		Assert.assertEquals( "else", exportedVariables.get( "VM_Openstack.something" ));
		Assert.assertNull( exportedVariables.get( "Virtual_Machine.ip" ));
		Assert.assertTrue( exportedVariables.containsKey( "Virtual_Machine.ip" ));
		Assert.assertFalse( exportedVariables.containsKey( "VM_Openstack.ip" ));
		Assert.assertEquals( ComponentHelpers.findComponent( g, "VM" ), vmOpenstack.getExtendedComponent());
		Assert.assertNotNull( vmOpenstack.getExtendedComponent());

		// Test the instances
		Set<String> expectedPaths = new HashSet<String> ();
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

		Set<String> realPaths = new HashSet<String> ();
		for( Instance inst : InstanceHelpers.getAllInstances( result.getApplication()))
			realPaths.add( InstanceHelpers.computeInstancePath( inst ));

		Assert.assertEquals( expectedPaths.size(), realPaths.size());
		realPaths.removeAll( expectedPaths );
		Assert.assertEquals( 0, realPaths.size());

		Instance tomcat1 = InstanceHelpers.findInstanceByPath( result.getApplication(), "/Tomcat VM 1/Tomcat" );
		Instance tomcat2 = InstanceHelpers.findInstanceByPath( result.getApplication(), "/Tomcat VM 2/Tomcat" );
		Instance tomcat3 = InstanceHelpers.findInstanceByPath( result.getApplication(), "/Tomcat VM 3/Tomcat" );

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

		File directory = TestUtils.findTestFile( "/applications/valid/mongo" );
		ApplicationLoadResult result = RuntimeModelIo.loadApplication( directory );
		Assert.assertNotNull( result );
		Assert.assertNotNull( result.application );
		Assert.assertEquals( 2, result.loadErrors.size());
		for( RoboconfError error : result.loadErrors )
			Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, error.getErrorCode());

		Assert.assertEquals( "Mongo", result.application.getName());
		Assert.assertNotNull( result.application.getGraphs());
		Graphs g = result.application.getGraphs();
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
		props.setProperty( ApplicationDescriptor.APPLICATION_NAME, "app-name" );
		props.setProperty( ApplicationDescriptor.APPLICATION_QUALIFIER, "snapshot" );
		props.setProperty( ApplicationDescriptor.APPLICATION_NAMESPACE, "net.roboconf" );
		props.setProperty( ApplicationDescriptor.APPLICATION_DSL_ID, "roboconf-1.0" );
		props.setProperty( ApplicationDescriptor.APPLICATION_GRAPH_EP, "main.graph" );
		FileOutputStream fos = new FileOutputStream( new File( appDir, Constants.PROJECT_FILE_DESCRIPTOR ));
		props.store( fos, null );
		Utils.closeQuietly( fos );

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
		Assert.assertEquals( ErrorCode.P_NO_FILE_TYPE, iterator.next().getErrorCode());

		String fileContent = "instanceof Toto {\n\tname: toto;\n}";
		Utils.copyStream( new ByteArrayInputStream( fileContent.getBytes( "UTF-8" )), graphFile );
		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.PM_PROPERTY_NOT_APPLIABLE, iterator.next().getErrorCode());

		fileContent = "instance of Toto {\n\tname: toto;\n}";
		Utils.copyStream( new ByteArrayInputStream( fileContent.getBytes( "UTF-8" )), graphFile );
		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.CO_NOT_A_GRAPH, iterator.next().getErrorCode());

		fileContent = "facet MyFacet {\n}\n\nA {\n\tinstaller: target;\n}";
		Utils.copyStream( new ByteArrayInputStream( fileContent.getBytes( "UTF-8" )), graphFile );

		// Instances
		Collection<RoboconfError> errors = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors;
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, errors.iterator().next().getErrorCode());

		fos = new FileOutputStream( new File( appDir, Constants.PROJECT_FILE_DESCRIPTOR ));
		props.setProperty( ApplicationDescriptor.APPLICATION_INSTANCES_EP, "init.instances" );
		props.store( fos, null );
		Utils.closeQuietly( fos );

		File instDir = new File( tempDirectory, Constants.PROJECT_DIR_INSTANCES );
		if( ! instDir.mkdir())
			throw new IOException( "Failed to create the instances directory." );

		iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
		Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.PROJ_MISSING_INSTANCE_EP, iterator.next().getErrorCode());

		File instancesFile = new File( instDir, "init.instances" );
		fileContent = "facet MyFacet {\n}\n\nA {\n\tinstaller: bash;\n}";
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

		File directory = TestUtils.findTestFile( "/applications/valid/karaf-joram-jndi" );
		ApplicationLoadResult result = RuntimeModelIo.loadApplication( directory );
		Assert.assertNotNull( result );
		Assert.assertNotNull( result.application );
		Assert.assertEquals( 6, result.loadErrors.size());
		for( RoboconfError error : result.loadErrors )
			Assert.assertEquals( ErrorCode.PROJ_NO_RESOURCE_DIRECTORY, error.getErrorCode());

		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( result.getApplication(), "/vmec2karaf" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( result.getApplication(), "/vmec2karaf/karafec21" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( result.getApplication(), "/vmec2karaf/karafec21/jndiec2" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( result.getApplication(), "/vmec2karaf/karafec22" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( result.getApplication(), "/vmec2karaf/karafec22/joramec2" ));
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

		ApplicationDescriptor desc = new ApplicationDescriptor();
		desc.setName( "app name" );
		desc.setQualifier( "qualifier" );
		desc.setInstanceEntryPoint( "model.instances" );
		desc.setDslId( "roboconf-1.0" );
		desc.setNamespace( "net.roboconf" );

		ApplicationDescriptor.save( new File( dir, Constants.PROJECT_DIR_DESC + "/" + Constants.PROJECT_FILE_DESCRIPTOR ), desc );
		Iterator<RoboconfError> it = RuntimeModelIo.loadApplication( dir ).loadErrors.iterator();
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
		Assert.assertTrue( new File( dir, Constants.PROJECT_DIR_GRAPH + "/VM/target.properties" ).createNewFile());

		File graphFile = new File( dir, Constants.PROJECT_DIR_GRAPH + "/app.graph" );
		Assert.assertTrue( graphFile.createNewFile());

		ApplicationDescriptor desc = new ApplicationDescriptor();
		desc.setName( "app name" );
		desc.setQualifier( "qualifier" );
		desc.setGraphEntryPoint( "app.graph" );
		desc.setDslId( "roboconf-1.0" );
		desc.setNamespace( "net.roboconf" );
		ApplicationDescriptor.save( new File( dir, Constants.PROJECT_DIR_DESC + "/" + Constants.PROJECT_FILE_DESCRIPTOR ), desc );

		Utils.writeStringInto( "VM {\ninstaller:target;\n}", graphFile );
		Assert.assertEquals( 0, RuntimeModelIo.loadApplication( dir ).loadErrors.size());
	}
}
