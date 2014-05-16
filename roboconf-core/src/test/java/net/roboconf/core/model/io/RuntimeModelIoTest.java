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

package net.roboconf.core.model.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.ApplicationDescriptor;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.io.RuntimeModelIo.LoadResult;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.utils.Utils;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RuntimeModelIoTest {

	@Test
	public void testLoadApplication_Lamp_Legacy_1() throws Exception {

		File directory = TestUtils.findTestFile( "/applications/valid/lamp-legacy-1" );
		LoadResult result = RuntimeModelIo.loadApplication( directory );
		Assert.assertNotNull( result );
		Assert.assertNotNull( result.application );
		Assert.assertEquals( 0, result.loadErrors.size());

		Assert.assertEquals( "Legacy LAMP", result.application.getName());
		Assert.assertEquals( "A sample LAMP application", result.application.getDescription());
		Assert.assertEquals( "sample", result.application.getQualifier());

		Assert.assertNotNull( result.application.getGraphs());
		Graphs g = result.application.getGraphs();
		Assert.assertEquals( 1, g.getRootComponents().size());

		Component vmComponent = g.getRootComponents().iterator().next();
		Assert.assertEquals( "VM", vmComponent.getName());
		Assert.assertEquals( "iaas", vmComponent.getInstallerName());
		Assert.assertEquals( "Virtual Machine", vmComponent.getAlias());
		Assert.assertEquals( 3, vmComponent.getChildren().size());

		for( Component childComponent : vmComponent.getChildren()) {
			if( "Tomcat".equals( childComponent.getName())) {
				Assert.assertEquals( "puppet", childComponent.getInstallerName());
				Assert.assertEquals( "Tomcat with Rubis", childComponent.getAlias());
				Assert.assertEquals( 0, childComponent.getChildren().size());

				Assert.assertEquals( 2, childComponent.getExportedVariables().size());
				Assert.assertTrue( childComponent.getExportedVariables().containsKey( "Tomcat.ip" ));
				Assert.assertEquals( "8009", childComponent.getExportedVariables().get( "Tomcat.portAJP" ));

				Assert.assertEquals( 2, childComponent.getImportedVariables().size());
				Assert.assertEquals( Boolean.FALSE, childComponent.getImportedVariables().get( "MySQL.ip" ));
				Assert.assertEquals( Boolean.FALSE, childComponent.getImportedVariables().get( "MySQL.port" ));

			} else if( "MySQL".equals( childComponent.getName())) {
				Assert.assertEquals( "puppet", childComponent.getInstallerName());
				Assert.assertEquals( "MySQL", childComponent.getAlias());
				Assert.assertEquals( 0, childComponent.getChildren().size());

				Assert.assertEquals( 2, childComponent.getExportedVariables().size());
				Assert.assertTrue( childComponent.getExportedVariables().containsKey( "MySQL.ip" ));
				Assert.assertEquals( "3306", childComponent.getExportedVariables().get( "MySQL.port" ));
				Assert.assertEquals( 0, childComponent.getImportedVariables().size());

			} else if( "Apache".equals( childComponent.getName())) {
				Assert.assertEquals( "puppet", childComponent.getInstallerName());
				Assert.assertEquals( "Apache Load Balancer", childComponent.getAlias());
				Assert.assertEquals( 0, childComponent.getChildren().size());
				Assert.assertEquals( 0, childComponent.getExportedVariables().size());

				Assert.assertEquals( 2, childComponent.getImportedVariables().size());
				Assert.assertEquals( Boolean.FALSE, childComponent.getImportedVariables().get( "Tomcat.ip" ));
				Assert.assertEquals( Boolean.FALSE, childComponent.getImportedVariables().get( "Tomcat.portAJP" ));

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

		File directory = TestUtils.findTestFile( "/applications/valid/lamp-legacy-2" );
		LoadResult result = RuntimeModelIo.loadApplication( directory );
		Assert.assertNotNull( result );
		Assert.assertNotNull( result.application );
		Assert.assertEquals( 0, result.loadErrors.size());

		Assert.assertEquals( "Legacy LAMP", result.application.getName());
		Assert.assertEquals( "A sample LAMP application", result.application.getDescription());
		Assert.assertEquals( "sample", result.application.getQualifier());

		Assert.assertNotNull( result.application.getGraphs());
		Graphs g = result.application.getGraphs();
		Assert.assertEquals( 1, g.getRootComponents().size());

		Component vmComponent = g.getRootComponents().iterator().next();
		Assert.assertEquals( "VM", vmComponent.getName());
		Assert.assertEquals( "iaas", vmComponent.getInstallerName());
		Assert.assertEquals( "Virtual Machine", vmComponent.getAlias());
		Assert.assertEquals( 1, vmComponent.getFacetNames().size());
		Assert.assertEquals( "VM", vmComponent.getFacetNames().iterator().next());
		Assert.assertEquals( 3, vmComponent.getChildren().size());

		for( Component childComponent : vmComponent.getChildren()) {
			if( "Tomcat".equals( childComponent.getName())) {
				Assert.assertEquals( "puppet", childComponent.getInstallerName());
				Assert.assertEquals( "Tomcat with Rubis", childComponent.getAlias());
				Assert.assertEquals( 0, childComponent.getChildren().size());

				Assert.assertEquals( 1, childComponent.getFacetNames().size());
				Assert.assertEquals( "deployable", childComponent.getFacetNames().iterator().next());

				Assert.assertEquals( 2, childComponent.getExportedVariables().size());
				Assert.assertTrue( childComponent.getExportedVariables().containsKey( "Tomcat.ip" ));
				Assert.assertEquals( "8009", childComponent.getExportedVariables().get( "Tomcat.portAJP" ));

				Assert.assertEquals( 2, childComponent.getImportedVariables().size());
				Assert.assertEquals( Boolean.FALSE, childComponent.getImportedVariables().get( "MySQL.ip" ));
				Assert.assertEquals( Boolean.FALSE, childComponent.getImportedVariables().get( "MySQL.port" ));

			} else if( "MySQL".equals( childComponent.getName())) {
				Assert.assertEquals( "puppet", childComponent.getInstallerName());
				Assert.assertEquals( "MySQL", childComponent.getAlias());
				Assert.assertEquals( 0, childComponent.getChildren().size());

				Assert.assertEquals( 1, childComponent.getFacetNames().size());
				Assert.assertEquals( "deployable", childComponent.getFacetNames().iterator().next());

				Assert.assertEquals( 2, childComponent.getExportedVariables().size());
				Assert.assertTrue( childComponent.getExportedVariables().containsKey( "MySQL.ip" ));
				Assert.assertEquals( "3306", childComponent.getExportedVariables().get( "MySQL.port" ));
				Assert.assertEquals( 0, childComponent.getImportedVariables().size());

			} else if( "Apache".equals( childComponent.getName())) {
				Assert.assertEquals( "puppet", childComponent.getInstallerName());
				Assert.assertEquals( "Apache Load Balancer", childComponent.getAlias());
				Assert.assertEquals( 0, childComponent.getChildren().size());
				Assert.assertEquals( 0, childComponent.getExportedVariables().size());

				Assert.assertEquals( 1, childComponent.getFacetNames().size());
				Assert.assertEquals( "deployable", childComponent.getFacetNames().iterator().next());

				Assert.assertEquals( 2, childComponent.getImportedVariables().size());
				Assert.assertEquals( Boolean.FALSE, childComponent.getImportedVariables().get( "Tomcat.ip" ));
				Assert.assertEquals( Boolean.FALSE, childComponent.getImportedVariables().get( "Tomcat.portAJP" ));

			} else {
				Assert.fail( "Unrecognized child." );
			}
		}
	}


	@Test
	public void testLoadApplication_Mongo() throws Exception {

		File directory = TestUtils.findTestFile( "/applications/valid/mongo" );
		LoadResult result = RuntimeModelIo.loadApplication( directory );
		Assert.assertNotNull( result );
		Assert.assertNotNull( result.application );
		Assert.assertEquals( 0, result.loadErrors.size());

		Assert.assertEquals( "Mongo", result.application.getName());
		Assert.assertNotNull( result.application.getGraphs());
		Graphs g = result.application.getGraphs();
		Assert.assertEquals( 1, g.getRootComponents().size());

		Component vmComponent = g.getRootComponents().iterator().next();
		Assert.assertEquals( "VM", vmComponent.getName());
		Assert.assertEquals( "iaas", vmComponent.getInstallerName());
		Assert.assertEquals( "Virtual Machine", vmComponent.getAlias());
		Assert.assertEquals( 0, vmComponent.getFacetNames().size());
		Assert.assertEquals( 1, vmComponent.getChildren().size());

		Component childComponent = vmComponent.getChildren().iterator().next();
		Assert.assertEquals( "puppet", childComponent.getInstallerName());
		Assert.assertEquals( "Mongo DB", childComponent.getAlias());
		Assert.assertEquals( 0, childComponent.getChildren().size());

		Assert.assertEquals( 2, childComponent.getExportedVariables().size());
		Assert.assertNull( childComponent.getExportedVariables().get( "Mongo.ip" ));
		Assert.assertEquals( "27017", childComponent.getExportedVariables().get( "Mongo.port" ));

		Assert.assertEquals( 2, childComponent.getImportedVariables().size());
		Assert.assertTrue( childComponent.getImportedVariables().containsKey( "Mongo.ip" ));
		Assert.assertTrue( childComponent.getImportedVariables().containsKey( "Mongo.port" ));
	}


	@Test
	public void testLoadApplicationErrors() throws Exception {

		File tempDirectory = new File( System.getProperty( "java.io.tmpdir" ), UUID.randomUUID().toString());
		try {
			if( ! tempDirectory.mkdir())
				throw new IOException( "Failed to create a temporary directory." );

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

			String instanceContent = "instanceof Toto {\n\tname: toto;\n}";
			Utils.copyStream( new ByteArrayInputStream( instanceContent.getBytes( "UTF-8" )), graphFile );

			iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
			Assert.assertEquals( ErrorCode.PROJ_NOT_A_GRAPH, iterator.next().getErrorCode());

			instanceContent = "facet MyFacet {\n}\n\nA {\n\talias: A;\n\tinstaller: bash;\n}";
			Utils.copyStream( new ByteArrayInputStream( instanceContent.getBytes( "UTF-8" )), graphFile );

			// Instances
			Assert.assertEquals( 0, RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.size());

			fos = new FileOutputStream( new File( appDir, Constants.PROJECT_FILE_DESCRIPTOR ));
			props.setProperty( ApplicationDescriptor.APPLICATION_INSTANCES_EP, "init.instances" );
			props.store( fos, null );
			Utils.closeQuietly( fos );

			File instDir = new File( tempDirectory, Constants.PROJECT_DIR_INSTANCES );
			if( ! instDir.mkdir())
				throw new IOException( "Failed to create the instances directory." );

			iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
			Assert.assertEquals( ErrorCode.PROJ_MISSING_INSTANCE_EP, iterator.next().getErrorCode());

			File instancesFile = new File( instDir, "init.instances" );
			instanceContent = "facet MyFacet {\n}\n\nA {\n\talias: A;\n\tinstaller: bash;\n}";
			Utils.copyStream( new ByteArrayInputStream( instanceContent.getBytes( "UTF-8" )), instancesFile );

			iterator = RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.iterator();
			Assert.assertEquals( ErrorCode.PROJ_NOT_AN_INSTANCE, iterator.next().getErrorCode());

			instanceContent = "instanceof A {\n\tname: toto;\n}";
			Utils.copyStream( new ByteArrayInputStream( instanceContent.getBytes( "UTF-8" )), instancesFile );
			Assert.assertEquals( 0, RuntimeModelIo.loadApplication( tempDirectory ).loadErrors.size());

		} finally {
			Utils.deleteFilesRecursively( tempDirectory );
		}
	}


	@Test
	public void testLoadApplication_KarafJoramJndi() throws Exception {

		File directory = TestUtils.findTestFile( "/applications/valid/karaf-joram-jndi" );
		LoadResult result = RuntimeModelIo.loadApplication( directory );
		Assert.assertNotNull( result );
		Assert.assertNotNull( result.application );
		Assert.assertEquals( 0, result.loadErrors.size());

		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( result.getApplication(), "/vmec2karaf" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( result.getApplication(), "/vmec2karaf/karafec21" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( result.getApplication(), "/vmec2karaf/karafec21/jndiec2" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( result.getApplication(), "/vmec2karaf/karafec22" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( result.getApplication(), "/vmec2karaf/karafec22/joramec2" ));
	}
}
