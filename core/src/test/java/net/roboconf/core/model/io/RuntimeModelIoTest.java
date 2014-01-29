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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import junit.framework.Assert;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.io.RuntimeModelIo.LoadResult;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;
import net.roboconf.core.model.runtime.Instance;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RuntimeModelIoTest {

	@Test
	public void testLoadApplication_Lamp_Legacy_1() {

		// Find the directory
		File directory;
		try {
			directory = TestUtils.findTestFile( "/applications/lamp-legacy-1" );

		} catch( IOException e ) {
			Assert.fail( "IO exception: " + e.getMessage());
			return;

		} catch( URISyntaxException e ) {
			Assert.fail( "URI syntax exception: " + e.getMessage());
			return;
		}

		// Load the application and check some assertions
		LoadResult result = RuntimeModelIo.loadApplication( directory );
		Assert.assertNotNull( result );
		Assert.assertNotNull( result.application );
		Assert.assertEquals( 4, result.loadErrors.size());
		for( RoboconfError error : result.loadErrors )
			Assert.assertEquals( ErrorCode.RM_UNEXISTING_COMPONENT_DIRECTORY, error.getErrorCode());

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

				Assert.assertEquals( 2, childComponent.getImportedVariableNames().size());
				Assert.assertTrue( childComponent.getImportedVariableNames().contains( "MySQL.ip" ));
				Assert.assertTrue( childComponent.getImportedVariableNames().contains( "MySQL.port" ));

			} else if( "MySQL".equals( childComponent.getName())) {
				Assert.assertEquals( "puppet", childComponent.getInstallerName());
				Assert.assertEquals( "MySQL", childComponent.getAlias());
				Assert.assertEquals( 0, childComponent.getChildren().size());

				Assert.assertEquals( 2, childComponent.getExportedVariables().size());
				Assert.assertTrue( childComponent.getExportedVariables().containsKey( "MySQL.ip" ));
				Assert.assertEquals( "3306", childComponent.getExportedVariables().get( "MySQL.port" ));
				Assert.assertEquals( 0, childComponent.getImportedVariableNames().size());

			} else if( "Apache".equals( childComponent.getName())) {
				Assert.assertEquals( "puppet", childComponent.getInstallerName());
				Assert.assertEquals( "Apache Load Balancer", childComponent.getAlias());
				Assert.assertEquals( 0, childComponent.getChildren().size());
				Assert.assertEquals( 0, childComponent.getExportedVariables().size());

				Assert.assertEquals( 2, childComponent.getImportedVariableNames().size());
				Assert.assertTrue( childComponent.getImportedVariableNames().contains( "Tomcat.ip" ));
				Assert.assertTrue( childComponent.getImportedVariableNames().contains( "Tomcat.portAJP" ));

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
	public void testLoadApplication_Lamp_Legacy_2() {

		// Find the directory
		File directory;
		try {
			directory = TestUtils.findTestFile( "/applications/lamp-legacy-2" );

		} catch( IOException e ) {
			Assert.fail( "IO exception: " + e.getMessage());
			return;

		} catch( URISyntaxException e ) {
			Assert.fail( "URI syntax exception: " + e.getMessage());
			return;
		}

		// Load the application and check some assertions
		LoadResult result = RuntimeModelIo.loadApplication( directory );
		Assert.assertNotNull( result );
		Assert.assertNotNull( result.application );
		Assert.assertEquals( 4, result.loadErrors.size());
		for( RoboconfError error : result.loadErrors )
			Assert.assertEquals( ErrorCode.RM_UNEXISTING_COMPONENT_DIRECTORY, error.getErrorCode());

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

				Assert.assertEquals( 2, childComponent.getImportedVariableNames().size());
				Assert.assertTrue( childComponent.getImportedVariableNames().contains( "MySQL.ip" ));
				Assert.assertTrue( childComponent.getImportedVariableNames().contains( "MySQL.port" ));

			} else if( "MySQL".equals( childComponent.getName())) {
				Assert.assertEquals( "puppet", childComponent.getInstallerName());
				Assert.assertEquals( "MySQL", childComponent.getAlias());
				Assert.assertEquals( 0, childComponent.getChildren().size());

				Assert.assertEquals( 1, childComponent.getFacetNames().size());
				Assert.assertEquals( "deployable", childComponent.getFacetNames().iterator().next());

				Assert.assertEquals( 2, childComponent.getExportedVariables().size());
				Assert.assertTrue( childComponent.getExportedVariables().containsKey( "MySQL.ip" ));
				Assert.assertEquals( "3306", childComponent.getExportedVariables().get( "MySQL.port" ));
				Assert.assertEquals( 0, childComponent.getImportedVariableNames().size());

			} else if( "Apache".equals( childComponent.getName())) {
				Assert.assertEquals( "puppet", childComponent.getInstallerName());
				Assert.assertEquals( "Apache Load Balancer", childComponent.getAlias());
				Assert.assertEquals( 0, childComponent.getChildren().size());
				Assert.assertEquals( 0, childComponent.getExportedVariables().size());

				Assert.assertEquals( 1, childComponent.getFacetNames().size());
				Assert.assertEquals( "deployable", childComponent.getFacetNames().iterator().next());

				Assert.assertEquals( 2, childComponent.getImportedVariableNames().size());
				Assert.assertTrue( childComponent.getImportedVariableNames().contains( "Tomcat.ip" ));
				Assert.assertTrue( childComponent.getImportedVariableNames().contains( "Tomcat.portAJP" ));

			} else {
				Assert.fail( "Unrecognized child." );
			}
		}
	}
}
