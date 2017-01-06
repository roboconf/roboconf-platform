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

package net.roboconf.integration.tests.dm;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureSecurity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.security.auth.Subject;

import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.integration.tests.commons.AbstractIntegrationTest;
import net.roboconf.integration.tests.commons.ItConfigurationBean;
import net.roboconf.integration.tests.commons.internal.ItUtils;
import net.roboconf.integration.tests.commons.internal.parameterized.IMessagingConfiguration;
import net.roboconf.integration.tests.commons.internal.runners.RoboconfPaxRunner;
import net.roboconf.integration.tests.dm.probes.DmTest;

/**
 * A set of tests for the agent's initialization.
 * <p>
 * We launch a Karaf installation with an agent in-memory. We load
 * an application and instantiates a root instance. The new agent
 * must send an initial message to the DM to indicate it is alive.
 * It must then receive its model from the DM.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
@RunWith( RoboconfPaxRunner.class )
@ExamReactorStrategy( PerMethod.class )
public class DelayedInstallationOfTargetHandlerTest extends DmTest {

	private static final String APP_LOCATION = "my.app.location";

	@Inject
	protected Manager manager;

	@Inject
	protected FeaturesService featuresService;

	@Inject
	protected SessionFactory sessionFactory;

	// Wait for all the boot features to be installed.
	@Inject
	protected BootFinished bootFinished;

	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

	private Session session;
	private final PrintStream printStream;
	private final PrintStream errStream;



	/**
	 * Constructor.
	 */
	public DelayedInstallationOfTargetHandlerTest() throws Exception {
		this.printStream = new PrintStream( this.byteArrayOutputStream, true, "UTF-8" );
		this.errStream = new PrintStream( this.byteArrayOutputStream, true, "UTF-8" );
	}


	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		// We need to specify the classes we need
		// and that come from external modules.
		probe.addTest( DmTest.class );
		probe.addTest( TestUtils.class );

		probe.addTest( AbstractIntegrationTest.class );
		probe.addTest( IMessagingConfiguration.class );
		probe.addTest( ItConfigurationBean.class );

		return probe;
	}


	@Before
	public void setUp() throws Exception {
		this.session = this.sessionFactory.create( System.in, this.printStream, this.errStream );
	}


	@Override
	@Configuration
	public Option[] config() throws Exception {

		List<Option> options = new ArrayList<> ();
		options.addAll( Arrays.asList( super.config()));

		// Store the application's location
		File resourcesDirectory = TestUtils.findApplicationDirectory( "lamp" );
		String appLocation = resourcesDirectory.getAbsolutePath();
		options.add( systemProperty( APP_LOCATION ).value( appLocation ));

		// Disable the JMX server. Not sure it is really useful...
		options.add( configureSecurity().disableKarafMBeanServerBuilder());

		// Pre-deploy the agent and the plug-in API.
		// "roboconf:target" would install them. But we get OSGi import errors
		// with the PAX probe (the bundle that is generated on the fly by PAX-Exam
		// and that wraps the test classes).
		//
		// To prevent it, we deploy them before running the test.
		String roboconfVersion = ItUtils.findRoboconfVersion();
		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-plugin-api" )
				.version( roboconfVersion )
				.start());

		options.add( mavenBundle()
				.groupId( "net.roboconf" )
				.artifactId( "roboconf-agent" )
				.version( roboconfVersion )
				.start());

		return options.toArray( new Option[ options.size()]);
	}


	@Test
	public void run() throws Exception {

		// 1. Load an application.
		// 2. Deploy its instances. It should fail as the target handler is not available.
		// 3. Deploy the target handler. Deploy the application. It should work.
		// 4. Undeploy everything.

		// Sleep for a while, to let the RabbitMQ client factory arrive.
		Thread.sleep( 2000 );

		// Load the application
		String appLocation = System.getProperty( APP_LOCATION );
		ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( new File( appLocation ));
		ManagedApplication ma = this.manager.applicationMngr().createApplication( "test", null, tpl );
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, this.manager.applicationMngr().getManagedApplications().size());

		// Associate a default target for this application
		String targetId = this.manager.targetsMngr().createTarget( "id:tid\nhandler: in-memory" );
		this.manager.targetsMngr().associateTargetWith( targetId, ma.getApplication(), null );

		// Instantiate a new root instance
		Instance rootInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/MySQL VM" );
		Assert.assertNotNull( rootInstance );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, rootInstance.getStatus());

		try {
			this.manager.instancesMngr().changeInstanceState( ma, rootInstance, InstanceStatus.DEPLOYED_STARTED );
			Assert.fail( "Deployment should have failed. The target handler is not supposed to be installed." );

		} catch( Exception e1 ) {
			// nothing
		}

		Thread.sleep( 800 );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, rootInstance.getStatus());

		// Deploy the target handler
		this.byteArrayOutputStream.flush();
		this.byteArrayOutputStream.reset();

		// What we want to execute...
		final Callable<String> commandCallable = new Callable<String> () {
			@Override
			public String call() throws Exception {

				try {
					DelayedInstallationOfTargetHandlerTest.this.session.execute( "roboconf:target in-memory" );

				} catch( Exception e ) {
					e.printStackTrace( System.err );
				}

				DelayedInstallationOfTargetHandlerTest.this.printStream.flush();
				DelayedInstallationOfTargetHandlerTest.this.errStream.flush();
				return DelayedInstallationOfTargetHandlerTest.this.byteArrayOutputStream.toString( "UTF-8" );
			}
		};

		// In directly, we will use "bundle:install", which requires some privileges.
		// So, we must enclose our invocation in a privileged action.
		String response;
		FutureTask<String> commandFuture = new FutureTask<>( new Callable<String>() {
			@Override
			public String call() {

				// FIXME: isn't there a better way? "admin"???
				// The question was asked on Karaf's mailing-list.
				Subject subject = new Subject();
				subject.getPrincipals().addAll( Arrays.asList( new RolePrincipal( "admin" )));
				try {
					return Subject.doAs( subject, new PrivilegedExceptionAction<String> () {
						@Override
						public String run() throws Exception {
							return commandCallable.call();
						}
					});

				} catch( PrivilegedActionException e ) {
					e.printStackTrace( System.err );
				}

				return null;
			}
		});

		// Execute our privileged action.
		try {
			this.executor.submit( commandFuture );

			// Give up to 30 seconds for the command to complete...
			response = commandFuture.get( 30L, TimeUnit.SECONDS );

		} catch( Exception e ) {
			e.printStackTrace( System.err );
			response = "SHELL COMMAND TIMED OUT: ";
		}

		System.err.println( response );

		// Wait a little bit so that the in-memory handler gets injected in the manager.
		Thread.sleep( 5000 );

		// Try to deploy again. It should work now.
		this.manager.instancesMngr().changeInstanceState( ma, rootInstance, InstanceStatus.DEPLOYED_STARTED );
		Thread.sleep( 800 );
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, rootInstance.getStatus());

		// Undeploy them all
		this.manager.instancesMngr().changeInstanceState( ma, rootInstance, InstanceStatus.NOT_DEPLOYED );
		Thread.sleep( 300 );
	}
}
