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

package net.roboconf.integration.tests.dm.with.agents.in.memory;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.util.Collection;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.integration.tests.commons.AbstractIntegrationTest;
import net.roboconf.integration.tests.commons.ItConfigurationBean;
import net.roboconf.integration.tests.commons.internal.ItUtils;
import net.roboconf.integration.tests.commons.internal.parameterized.IMessagingConfiguration;
import net.roboconf.integration.tests.commons.internal.runners.RoboconfITConfiguration;
import net.roboconf.integration.tests.commons.internal.runners.RoboconfPaxRunner;
import net.roboconf.integration.tests.dm.with.agents.in.memory.internal.MyHandler;
import net.roboconf.integration.tests.dm.with.agents.in.memory.internal.MyTargetResolver;
import net.roboconf.integration.tests.dm.with.agents.in.memory.probes.DmWithAgentInMemoryTest;

/**
 * Verify agent in-memory are synchronized correctly when executing real recipes.
 * @author Vincent Zurczak - Linagora
 */
@RunWith( RoboconfPaxRunner.class )
@ExamReactorStrategy( PerMethod.class )
@RoboconfITConfiguration( withLinux = true )
public class AgentInMemoryWithRealRecipesTest extends DmWithAgentInMemoryTest {

	private static final String APP_LOCATION = "my.app.location";

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private final String outputFilePath = new File(
			System.getProperty( "java.io.tmpdir" ),
			UUID.randomUUID().toString()).getAbsolutePath();


	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		// We need to specify the classes we need
		// and that come from external modules.
		probe.addTest( DmWithAgentInMemoryTest.class );
		probe.addTest( TestUtils.class );

		probe.addTest( AbstractIntegrationTest.class );
		probe.addTest( IMessagingConfiguration.class );
		probe.addTest( ItConfigurationBean.class );

		probe.addTest( MyHandler.class );
		probe.addTest( MyTargetResolver.class );

		return probe;
	}


	@Override
	@Configuration
	public Option[] config() throws Exception {

		File resourcesDirectory = TestUtils.findApplicationDirectory( "lamp" );
		String roboconfVersion = ItUtils.findRoboconfVersion();
		return OptionUtils.combine(
				super.config(),
				systemProperty( APP_LOCATION ).value( resourcesDirectory.getAbsolutePath()),
				mavenBundle()
					.groupId( "net.roboconf" )
					.artifactId( "roboconf-plugin-script" )
					.version( roboconfVersion )
					.start());
	}


	@Test
	public void run() throws Exception {

		// We copy an application template and we add recipes
		File sourceAppDirectory = new File( System.getProperty( APP_LOCATION ));
		Assert.assertTrue( sourceAppDirectory.isDirectory());
		File appDirectory = this.folder.newFolder();
		Utils.copyDirectory( sourceAppDirectory, appDirectory );

		// Generate recipes
		ApplicationTemplate tpl = RuntimeModelIo.loadApplication( sourceAppDirectory ).getApplicationTemplate();
		Collection<Component> allComponents = ComponentHelpers.findAllComponents( tpl );
		Assert.assertEquals( 4, allComponents.size());

		String[] recipes = { "deploy", "start", "stop", "undeploy" };

		int cpt = 0;
		for( Instance i : InstanceHelpers.getAllInstances( tpl )) {
			if( ! "script".equalsIgnoreCase( i.getComponent().getInstallerName()))
				continue;

			cpt ++;
			File scriptDir = new File( appDirectory, Constants.PROJECT_DIR_GRAPH + "/" + i.getName() + "/scripts/" );
			Utils.createDirectory( scriptDir );
			for( String recipe : recipes ) {

				// We write the same string in a same file within a period of 1 second
				StringBuilder sb = new StringBuilder();
				sb.append( "#!/bin/sh\n" );
				sb.append( "echo \"${ROBOCONF_CLEAN_INSTANCE_PATH} - " );
				sb.append( recipe );
				sb.append( "\" >> " );
				sb.append( this.outputFilePath );

				sb.append( "\nsleep 1\n" );

				sb.append( "echo \"${ROBOCONF_CLEAN_INSTANCE_PATH} - " );
				sb.append( recipe );
				sb.append( "\" >> " );
				sb.append( this.outputFilePath );

				Utils.writeStringInto( sb.toString(), new File( scriptDir, recipe + ".sh" ));
			}
		}

		Assert.assertEquals( 3, cpt );

		// Perform the test
		testExecution( appDirectory, new File( this.outputFilePath ));
	}


	private void testExecution( File appDirectory, File expectedOutputFile ) throws Exception {
		Assert.assertFalse( expectedOutputFile.exists());

		ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( appDirectory );
		ManagedApplication ma = this.manager.applicationMngr().createApplication( "test", null, tpl );
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, this.manager.applicationMngr().getManagedApplications().size());

		// Associate a default target for this application
		String targetId = this.manager.targetsMngr().createTarget( "id:tid\nhandler: in-memory\nin-memory.execute-real-recipes: true" );
		this.manager.targetsMngr().associateTargetWith( targetId, ma.getApplication(), null );

		// Create several new instances of our Tomcat
		Component tomcat = ComponentHelpers.findComponent( tpl, "Tomcat" );
		Component vm = ComponentHelpers.findComponent( tpl, "VM" );
		for( int i=0; i<4; i++) {
			Instance newVm = new Instance( "vm " + i ).component( vm );
			ma.getApplication().getRootInstances().add( newVm );
			InstanceHelpers.insertChild( newVm, new Instance( "tomcat" ).component( tomcat ));
		}

		// So, we have 7 instances that execute scripts!

		// Start everything
		// Each deploy and start script sleeps one second => 7 x 2 = 14 seconds
		this.manager.instancesMngr().deployAndStartAll( ma, null );
		Thread.sleep( 14500 );

		for( Instance i : InstanceHelpers.getAllInstances( ma.getApplication()))
			Assert.assertEquals( i.getName(), InstanceStatus.DEPLOYED_STARTED, i.getStatus());

		// Stop everything
		// Each stop script sleeps one second => 7 seconds
		this.manager.instancesMngr().stopAll( ma, null );
		Thread.sleep( 7500 );

		for( Instance i : InstanceHelpers.getAllInstances( ma.getApplication())) {
			if( ! InstanceHelpers.isTarget( i ))
				Assert.assertEquals( i.getName(), InstanceStatus.DEPLOYED_STOPPED, i.getStatus());
		}

		// Undeploy everything
		// Each undeploy script sleeps one second => 7 seconds
		this.manager.instancesMngr().undeployAll( ma, null );
		Thread.sleep( 7500 );

		for( Instance i : InstanceHelpers.getAllInstances( ma.getApplication()))
			Assert.assertEquals( i.getName(), InstanceStatus.NOT_DEPLOYED, i.getStatus());

		// Verify that recipes were executed only once at a time
		Assert.assertTrue( expectedOutputFile.exists());
		String content = Utils.readFileContent( expectedOutputFile );
		Assert.assertNotEquals( "", content.trim());
		Assert.assertTrue( content.contains( " - deploy" ));

		// The file was built so that a same sentence is repeated twice.
		// If recipes were executed sequentially, then the repetitions must
		// be consecutive. No concurrent execution of the recipes.
		//
		// aa
		// aa
		// bb
		// bb
		// ...
		String lastLine = null;
		for( String line : content.split( "\n" )) {

			if( lastLine == null ) {
				lastLine = line;
			} else {
				Assert.assertEquals( lastLine, line );
				lastLine = null;
			}
		}
	}
}
