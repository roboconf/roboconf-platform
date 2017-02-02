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

import static net.roboconf.core.model.beans.Instance.InstanceStatus.DEPLOYED_STARTED;
import static net.roboconf.core.model.beans.Instance.InstanceStatus.DEPLOYED_STOPPED;
import static net.roboconf.core.model.beans.Instance.InstanceStatus.DEPLOYING;
import static net.roboconf.core.model.beans.Instance.InstanceStatus.NOT_DEPLOYED;
import static net.roboconf.core.model.beans.Instance.InstanceStatus.STARTING;
import static net.roboconf.core.model.beans.Instance.InstanceStatus.STOPPING;
import static net.roboconf.core.model.beans.Instance.InstanceStatus.UNDEPLOYING;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.EventType;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.events.IDmListener;
import net.roboconf.integration.tests.commons.AbstractIntegrationTest;
import net.roboconf.integration.tests.commons.ItConfigurationBean;
import net.roboconf.integration.tests.commons.internal.parameterized.IMessagingConfiguration;
import net.roboconf.integration.tests.commons.internal.runners.RoboconfPaxRunner;
import net.roboconf.integration.tests.dm.with.agents.in.memory.internal.MyHandler;
import net.roboconf.integration.tests.dm.with.agents.in.memory.internal.MyTargetResolver;
import net.roboconf.integration.tests.dm.with.agents.in.memory.probes.DmWithAgentInMemoryTest;

/**
 * Verify notifications when instance statuses change.
 * @author Vincent Zurczak - Linagora
 */
@RunWith( RoboconfPaxRunner.class )
@ExamReactorStrategy( PerMethod.class )
public class DmNotificationsAboutInstancesTest extends DmWithAgentInMemoryTest {

	private static final String APP_LOCATION = "my.app.location";

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

		File resourcesDirectory = TestUtils.findApplicationDirectory( "simple" );
		String appLocation = resourcesDirectory.getAbsolutePath();
		return OptionUtils.combine(
				super.config(),
				systemProperty( APP_LOCATION ).value( appLocation ));
	}


	@Test
	public void run() throws Exception {

		// Update the manager
		configureManagerForInMemoryUsage();

		// Add a listener to intercept notifications
		NotificationCounter notif = new NotificationCounter();
		this.manager.listenerAppears( notif );

		// Load the application
		String appLocation = System.getProperty( APP_LOCATION );
		ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( new File( appLocation ));
		ManagedApplication ma = this.manager.applicationMngr().createApplication( "test", null, tpl );
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, this.manager.applicationMngr().getManagedApplications().size());

		// Associate a default target for this application
		String targetId = this.manager.targetsMngr().createTarget( "id: tid\nhandler: in-memory" );
		this.manager.targetsMngr().associateTargetWith( targetId, ma.getApplication(), null );

		// No notification yet
		Assert.assertEquals( 0, notif.instanceToStatusHistory.size());

		// Instantiate a new root instance
		Instance rootInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/MySQL VM" );
		Assert.assertNotNull( rootInstance );
		Assert.assertEquals( NOT_DEPLOYED, rootInstance.getStatus());

		this.manager.instancesMngr().changeInstanceState( ma, rootInstance, DEPLOYED_STARTED );
		Thread.sleep( 3000 );
		Assert.assertEquals( DEPLOYED_STARTED, rootInstance.getStatus());

		Assert.assertEquals( 1, notif.instanceToStatusHistory.size());
		List<InstanceStatus> statusHistory = notif.instanceToStatusHistory.get( rootInstance );
		Assert.assertNotNull( statusHistory );
		Assert.assertEquals( 3, statusHistory.size());
		Assert.assertEquals( DEPLOYING, statusHistory.get( 0 ));
		Assert.assertEquals( DEPLOYED_STARTED, statusHistory.get( 2 ));

		// Deploy, start and stop a child
		Instance childInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/MySQL VM/MySQL" );
		Assert.assertNotNull( childInstance );
		Assert.assertEquals( NOT_DEPLOYED, childInstance.getStatus());

		this.manager.instancesMngr().deployAndStartAll( ma, rootInstance );
		Thread.sleep( 3000 );
		Assert.assertEquals( DEPLOYED_STARTED, childInstance.getStatus());

		Assert.assertEquals( 2, notif.instanceToStatusHistory.size());
		statusHistory = notif.instanceToStatusHistory.get( rootInstance );
		Assert.assertNotNull( statusHistory );
		Assert.assertEquals( 3, statusHistory.size());
		Assert.assertEquals( DEPLOYING, statusHistory.get( 0 ));
		Assert.assertEquals( DEPLOYED_STARTED, statusHistory.get( 2 ));

		statusHistory = notif.instanceToStatusHistory.get( childInstance );
		Assert.assertNotNull( statusHistory );
		Assert.assertEquals( Arrays.asList( DEPLOYING, DEPLOYED_STOPPED, STARTING, DEPLOYED_STARTED ), statusHistory );

		this.manager.instancesMngr().changeInstanceState( ma, childInstance, DEPLOYED_STOPPED );
		Thread.sleep( 3000 );
		Assert.assertEquals( DEPLOYED_STOPPED, childInstance.getStatus());

		Assert.assertEquals( 2, notif.instanceToStatusHistory.size());
		statusHistory = notif.instanceToStatusHistory.get( rootInstance );
		Assert.assertNotNull( statusHistory );
		Assert.assertEquals( 3, statusHistory.size());
		Assert.assertEquals( DEPLOYING, statusHistory.get( 0 ));
		Assert.assertEquals( DEPLOYED_STARTED, statusHistory.get( 2 ));

		statusHistory = notif.instanceToStatusHistory.get( childInstance );
		Assert.assertNotNull( statusHistory );
		Assert.assertEquals( Arrays.asList( DEPLOYING, DEPLOYED_STOPPED, STARTING, DEPLOYED_STARTED, STOPPING, DEPLOYED_STOPPED ), statusHistory );
		statusHistory.clear();

		// Undeploy
		this.manager.instancesMngr().changeInstanceState( ma, rootInstance, NOT_DEPLOYED );
		Assert.assertEquals( 2, notif.instanceToStatusHistory.size());

		statusHistory = notif.instanceToStatusHistory.get( rootInstance );
		Assert.assertNotNull( statusHistory );
		Assert.assertEquals( 5, statusHistory.size());
		Assert.assertEquals( DEPLOYING, statusHistory.get( 0 ));
		Assert.assertEquals( DEPLOYED_STARTED, statusHistory.get( 2 ));
		Assert.assertEquals( UNDEPLOYING, statusHistory.get( 3 ));
		Assert.assertEquals( NOT_DEPLOYED, statusHistory.get( 4 ));

		statusHistory = notif.instanceToStatusHistory.get( childInstance );
		Assert.assertNotNull( statusHistory );
		Assert.assertEquals( Arrays.asList( NOT_DEPLOYED ), statusHistory );
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	private static class NotificationCounter implements IDmListener {
		private final Map<Instance,List<InstanceStatus>> instanceToStatusHistory = new ConcurrentHashMap<> ();

		@Override
		public String getId() {
			return null;
		}

		@Override
		public void enableNotifications() {
			// nothing
		}

		@Override
		public void disableNotifications() {
			// nothing
		}

		@Override
		public void application( Application application, EventType eventType ) {
			// nothing
		}

		@Override
		public void applicationTemplate( ApplicationTemplate tpl, EventType eventType ) {
			// nothing
		}

		@Override
		public void instance( Instance instance, Application application, EventType eventType ) {

			List<InstanceStatus> status = this.instanceToStatusHistory.get( instance );
			if( status == null ) {
				status = new ArrayList<> ();
				this.instanceToStatusHistory.put( instance, status );
			}

			status.add( instance.getStatus());
		}

		@Override
		public void raw( String message, Object... data ) {
			// nothing
		}
	}
}
