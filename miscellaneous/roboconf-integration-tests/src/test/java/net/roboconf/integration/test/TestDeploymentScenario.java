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

package net.roboconf.integration.test;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.util.List;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.pax.probe.AbstractTest;
import net.roboconf.pax.probe.DmWithAgentInMemoryTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

/**
 * @author Vincent Zurczak - Linagora
 */
@ExamReactorStrategy( PerSuite.class )
public class TestDeploymentScenario extends DmWithAgentInMemoryTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private static final String APP_LOCATION = "my.app.location";
	private WsClient client;


	@ProbeBuilder
	public TestProbeBuilder probeConfiguration( TestProbeBuilder probe ) {

		probe.addTest( AbstractTest.class );
		probe.addTest( DmWithAgentInMemoryTest.class );
		probe.addTest( TestUtils.class );

		return probe;
	}


	@Override
	@Configuration
	public Option[] config() {

		String appLocation = null;
		try {
			File resourcesDirectory = TestUtils.findTestFile( "/lamp", getClass());
			appLocation = resourcesDirectory.getAbsolutePath();

		} catch( Exception e ) {
			// nothing
		}

		return OptionUtils.combine(
				super.config(),

				mavenBundle()
					.groupId( "net.roboconf" )
					.artifactId( "roboconf-dm-rest-client" )
					.version( CURRENT_DEV_VERSION )
					.start(),

				systemProperty( APP_LOCATION )
					.value( appLocation ));
	}


	@Before
	public void resetManager() throws Exception {
		this.client = new WsClient( "http://localhost:8181/roboconf-dm" );
	}


	@After
	public void destroyClient() {
		if( this.client != null )
			this.client.destroy();
	}


	@Override
	public void run() {
		Assert.assertTrue( "Overridding the debug method to not block the tests.", true );
	}


	@Test
	public void testListApplications() throws Exception {

		String appLocation = System.getProperty( APP_LOCATION );
		Assert.assertNotNull( appLocation );
		Assert.assertTrue( new File( appLocation ).exists());

		Assert.assertEquals( 0, this.client.getManagementDelegate().listApplications().size());
		this.client.getManagementDelegate().loadApplication( appLocation );

		List<Application> apps = this.client.getManagementDelegate().listApplications();
		Assert.assertEquals( 1, apps.size());

		Application receivedApp = apps.get( 0 );
		Assert.assertEquals( "Legacy LAMP", receivedApp.getName());
		Assert.assertEquals( "sample", receivedApp.getQualifier());
	}
}
