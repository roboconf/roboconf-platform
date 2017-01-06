/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.templating.internal.contexts;

import static net.roboconf.core.model.beans.Instance.InstanceStatus.NOT_DEPLOYED;
import static net.roboconf.dm.templating.testutils.TemplatingTestUtils.instancesByPath;
import static net.roboconf.dm.templating.testutils.TemplatingTestUtils.variableMapOf;
import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Assert;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ImportHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.templating.internal.TemplatingManager;

import org.junit.Before;
import org.junit.Test;

/**
 * Test the templating context computed by the {@link TemplatingManager}.
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class TemplatingContextTest {

	/**
	 * The templating context of the application being tested.
	 */
	private ApplicationContextBean context;

	/**
	 * The paths of the instances indexed by type.
	 */
	private final Map<String, Set<String>> instancesOf = new LinkedHashMap<String, Set<String>>();

	/**
	 * The templating context of the instances, indexed by path.
	 */
	private Map<String, InstanceContextBean> instanceContexts = new LinkedHashMap<String, InstanceContextBean>();



	@Before
	public void before() throws IOException, URISyntaxException {

		// Load the application from the test resources.
		File dir = TestUtils.findApplicationDirectory( "app-for-templates" );
		Assert.assertTrue( dir.exists());

		final ApplicationLoadResult result = RuntimeModelIo.loadApplication( dir );
		assertThat( result.getApplicationTemplate()).isNotNull();

		// Create and patch an application to verify contexts are correctly generated
		Application app = new Application( "test-app", result.getApplicationTemplate()).description( "An example application" );
		Instance apacheVm = InstanceHelpers.findInstanceByPath( app, "/ApacheVm" );
		assertThat( apacheVm ).isNotNull();

		apacheVm.overriddenExports.put( "apacheVm.extra", "bonus" );

		for( Instance rootInstance : app.getRootInstances()) {
			if( rootInstance.equals( apacheVm ))
				continue;

			rootInstance.data.put( Instance.APPLICATION_NAME, app.getName());
			rootInstance.data.put( Instance.MACHINE_ID, "ds4sd14sdsfkdf" );
			ImportHelpers.addImport( rootInstance, "test", new Import( apacheVm ));
		}

		// Create a context
		this.context = ContextUtils.toContext( app );
		this.instanceContexts = instancesByPath(this.context.getInstances());

		// We need to re-arrange the data for easier further access.
		for (final Entry<String, Set<InstanceContextBean>> entry : this.context.getInstancesByType().entrySet()) {
			final Set<String> instancesOfType = new LinkedHashSet<String>();
			for (final InstanceContextBean i : entry.getValue())
				instancesOfType.add(i.getPath());

			this.instancesOf.put(entry.getKey(), instancesOfType);
		}
	}


	@Test
	public void testName() {
		assertThat(this.context.getName()).isEqualTo("test-app");
	}


	@Test
	public void testDescription() {
		assertThat(this.context.getDescription()).isEqualTo("An example application");
	}


	@Test
	public void testComponents() {
		assertThat(this.context.getComponents()).containsOnly("Vm", "MySql", "Apache", "Tomcat", "War");
	}


	@Test
	public void testInstancesByType() {

		assertThat(this.instancesOf.get("Vm"))
				.containsOnly("/MySqlVm", "/ApacheVm", "/TomcatVm1", "/TomcatVm2");

		assertThat(this.instancesOf.get("Virtual"))
				.containsOnly("/MySqlVm", "/ApacheVm", "/TomcatVm1", "/TomcatVm2");

		assertThat(this.instancesOf.get("Machine"))
				.containsOnly("/MySqlVm", "/ApacheVm", "/TomcatVm1", "/TomcatVm2");

		assertThat(this.instancesOf.get("MySql")).containsOnly("/MySqlVm/MySql");
		assertThat(this.instancesOf.get("Apache")).containsOnly("/ApacheVm/Apache");
		assertThat(this.instancesOf.get("Tomcat")).containsOnly("/TomcatVm1/Tomcat", "/TomcatVm2/Tomcat");

		assertThat(this.instancesOf.get("Service"))
				.containsOnly("/MySqlVm/MySql", "/ApacheVm/Apache", "/TomcatVm1/Tomcat", "/TomcatVm2/Tomcat");

		assertThat(this.instancesOf.get("NetworkService"))
				.containsOnly("/MySqlVm/MySql", "/ApacheVm/Apache", "/TomcatVm1/Tomcat", "/TomcatVm2/Tomcat");

		assertThat(this.instancesOf.get("War"))
				.containsOnly("/TomcatVm1/Tomcat/WebApp", "/TomcatVm2/Tomcat/WebApp");

		assertThat(this.instancesOf.get("Application"))
				.containsOnly("/TomcatVm1/Tomcat/WebApp", "/TomcatVm2/Tomcat/WebApp");
	}


	@Test
	public void testMySqlVmInstance() {

		final InstanceContextBean instance = this.instanceContexts.get("/MySqlVm");
		assertThat(instance.getName()).isEqualTo("MySqlVm");
		assertThat(instance.getPath()).isEqualTo("/MySqlVm");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("Vm");
		assertThat(instance.getTypes()).containsOnly("Vm", "Virtual", "Machine", "VirtualMachine");
		assertThat(instance.getParent()).isNull();
		assertThat(instance.getChildren()).containsOnly(this.instanceContexts.get("/MySqlVm/MySql"));
		assertThat(instance.getIp()).isNull();
		assertThat(instance.getInstaller()).isEqualTo("target");
		assertThat(instance.getExports()).isEmpty();
		assertThat(instance.getImports()).isNotEmpty();
		assertThat(instance.getData()).isNotEmpty();
	}


	@Test
	public void testApacheVmInstance() {

		final InstanceContextBean instance = this.instanceContexts.get("/ApacheVm");
		assertThat(instance.getName()).isEqualTo("ApacheVm");
		assertThat(instance.getPath()).isEqualTo("/ApacheVm");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("Vm");
		assertThat(instance.getTypes()).containsOnly("Vm", "Virtual", "Machine", "VirtualMachine");
		assertThat(instance.getParent()).isNull();
		assertThat(instance.getChildren()).containsOnly(this.instanceContexts.get("/ApacheVm/Apache"));
		assertThat(instance.getIp()).isNull();
		assertThat(instance.getInstaller()).isEqualTo("target");
		assertThat(instance.getExports()).isNotEmpty();
		assertThat(instance.getImports()).isEmpty();
		assertThat(instance.getData()).isEmpty();
	}


	@Test
	public void testTomcatVmInstance1() {

		final InstanceContextBean instance = this.instanceContexts.get("/TomcatVm1");
		assertThat(instance.getName()).isEqualTo("TomcatVm1");
		assertThat(instance.getPath()).isEqualTo("/TomcatVm1");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("Vm");
		assertThat(instance.getTypes()).containsOnly("Vm", "Virtual", "Machine", "VirtualMachine");
		assertThat(instance.getParent()).isNull();
		assertThat(instance.getChildren()).containsOnly(this.instanceContexts.get("/TomcatVm1/Tomcat"));
		assertThat(instance.getIp()).isNull();
		assertThat(instance.getInstaller()).isEqualTo("target");
		assertThat(instance.getExports()).isEmpty();
		assertThat(instance.getImports()).isNotEmpty();
		assertThat(instance.getData()).isNotEmpty();
	}


	@Test
	public void testTomcatVmInstance2() {

		final InstanceContextBean instance = this.instanceContexts.get("/TomcatVm2");
		assertThat(instance.getName()).isEqualTo("TomcatVm2");
		assertThat(instance.getPath()).isEqualTo("/TomcatVm2");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("Vm");
		assertThat(instance.getTypes()).containsOnly("Vm", "Virtual", "Machine", "VirtualMachine");
		assertThat(instance.getParent()).isNull();
		assertThat(instance.getChildren()).containsOnly(this.instanceContexts.get("/TomcatVm2/Tomcat"));
		assertThat(instance.getIp()).isNull();
		assertThat(instance.getInstaller()).isEqualTo("target");
		assertThat(instance.getExports()).isEmpty();
		assertThat(instance.getImports()).isNotEmpty();
		assertThat(instance.getData()).isNotEmpty();
	}


	@Test
	public void testMySqlInstance() {

		final InstanceContextBean instance = this.instanceContexts.get("/MySqlVm/MySql");
		assertThat(instance.getName()).isEqualTo("MySql");
		assertThat(instance.getPath()).isEqualTo("/MySqlVm/MySql");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("MySql");
		assertThat(instance.getTypes()).containsOnly("MySql", "NetworkService", "Service");
		assertThat(instance.getParent()).isSameAs(this.instanceContexts.get("/MySqlVm"));
		assertThat(instance.getChildren()).isEmpty();
		assertThat(instance.getIp()).isNull();
		assertThat(instance.getInstaller()).isEqualTo("puppet");

		final Map<String, String> exports = variableMapOf(instance.getExports());
		assertThat(exports).hasSize(2);
		assertThat(exports.get("MySql.ip")).isNull();
		assertThat(exports.get("MySql.port")).isEqualTo("3306");
		assertThat(instance.getImports()).isEmpty();
		assertThat(instance.getData()).isEmpty();
	}


	@Test
	public void testApacheInstance() {

		final InstanceContextBean instance = this.instanceContexts.get("/ApacheVm/Apache");
		assertThat(instance.getName()).isEqualTo("Apache");
		assertThat(instance.getPath()).isEqualTo("/ApacheVm/Apache");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("Apache");
		assertThat(instance.getTypes()).containsOnly("Apache", "NetworkService", "Service");
		assertThat(instance.getParent()).isSameAs(this.instanceContexts.get("/ApacheVm"));
		assertThat(instance.getChildren()).isEmpty();
		assertThat(instance.getIp()).isNull();
		assertThat(instance.getInstaller()).isEqualTo("docker");
		assertThat(instance.getExports()).isEmpty();
		assertThat(instance.getImports()).isEmpty();
		assertThat(instance.getData()).isEmpty();
	}


	@Test
	public void testTomcatInstance1() {

		final InstanceContextBean instance = this.instanceContexts.get("/TomcatVm1/Tomcat");
		assertThat(instance.getName()).isEqualTo("Tomcat");
		assertThat(instance.getPath()).isEqualTo("/TomcatVm1/Tomcat");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("Tomcat");
		assertThat(instance.getTypes()).containsOnly("Tomcat", "NetworkService", "Service");
		assertThat(instance.getParent()).isSameAs(this.instanceContexts.get("/TomcatVm1"));
		assertThat(instance.getChildren()).containsOnly(this.instanceContexts.get("/TomcatVm1/Tomcat/WebApp"));
		assertThat(instance.getIp()).isNull();
		assertThat(instance.getInstaller()).isEqualTo("docker");

		final Map<String, String> exports = variableMapOf(instance.getExports());
		assertThat(exports).hasSize(2);
		assertThat(exports.get("Tomcat.ip")).isNull();
		assertThat(exports.get("Tomcat.ajpPort")).isEqualTo("9021");
		assertThat(instance.getImports()).isEmpty();
		assertThat(instance.getData()).isEmpty();
	}


	@Test
	public void testTomcatInstance2() {

		final InstanceContextBean instance = this.instanceContexts.get("/TomcatVm2/Tomcat");
		assertThat(instance.getName()).isEqualTo("Tomcat");
		assertThat(instance.getPath()).isEqualTo("/TomcatVm2/Tomcat");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("Tomcat");
		assertThat(instance.getTypes()).containsOnly("Tomcat", "NetworkService", "Service");
		assertThat(instance.getParent()).isSameAs(this.instanceContexts.get("/TomcatVm2"));
		assertThat(instance.getChildren()).containsOnly(this.instanceContexts.get("/TomcatVm2/Tomcat/WebApp"));
		assertThat(instance.getIp()).isNull();
		assertThat(instance.getInstaller()).isEqualTo("docker");

		final Map<String, String> exports = variableMapOf(instance.getExports());
		assertThat(exports).hasSize(2);
		assertThat(exports.get("Tomcat.ip")).isNull();
		assertThat(exports.get("Tomcat.ajpPort")).isEqualTo("9021");
		assertThat(instance.getImports()).isEmpty();
		assertThat(instance.getData()).isEmpty();
	}


	@Test
	public void testWebAppInstance1() {

		final InstanceContextBean instance = this.instanceContexts.get("/TomcatVm1/Tomcat/WebApp");
		assertThat(instance.getName()).isEqualTo("WebApp");
		assertThat(instance.getPath()).isEqualTo("/TomcatVm1/Tomcat/WebApp");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("War");
		assertThat(instance.getTypes()).containsOnly("War", "Application");
		assertThat(instance.getParent()).isSameAs(this.instanceContexts.get("/TomcatVm1/Tomcat"));
		assertThat(instance.getChildren()).isEmpty();
		assertThat(instance.getIp()).isNull();
		assertThat(instance.getInstaller()).isEqualTo("human");
		assertThat(instance.getExports()).isEmpty();
		assertThat(instance.getImports()).isEmpty();
		assertThat(instance.getData()).isEmpty();
	}


	@Test
	public void testWebAppInstance2() {

		final InstanceContextBean instance = this.instanceContexts.get("/TomcatVm2/Tomcat/WebApp");
		assertThat(instance.getName()).isEqualTo("WebApp");
		assertThat(instance.getPath()).isEqualTo("/TomcatVm2/Tomcat/WebApp");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("War");
		assertThat(instance.getTypes()).containsOnly("War", "Application");
		assertThat(instance.getParent()).isSameAs(this.instanceContexts.get("/TomcatVm2/Tomcat"));
		assertThat(instance.getChildren()).isEmpty();
		assertThat(instance.getIp()).isNull();
		assertThat(instance.getInstaller()).isEqualTo("human");
		assertThat(instance.getExports()).isEmpty();
		assertThat(instance.getImports()).isEmpty();
		assertThat(instance.getData()).isEmpty();
	}
}
