/**
 * Copyright 2013-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.monitoring.internal;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import org.junit.Before;
import org.junit.Test;

import static net.roboconf.core.model.beans.Instance.InstanceStatus.NOT_DEPLOYED;
import static net.roboconf.dm.monitoring.internal.MonitoringTestUtils.instancesByPath;
import static net.roboconf.dm.monitoring.internal.MonitoringTestUtils.variableMapOf;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;

/**
 * Test the monitoring context computed by the {@link MonitoringManager}.
 *
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class MonitoringContextTest {

	/**
	 * The monitoring context of the application being tested.
	 */
	private ApplicationContextBean context;

	/**
	 * The paths of the instances indexed by type.
	 */
	private Map<String, Set<String>> instancesOf = new LinkedHashMap<String, Set<String>>();

	/**
	 * The monitoring context of the instances, indexed by path.
	 */
	private Map<String, InstanceContextBean> instanceContexts = new LinkedHashMap<String, InstanceContextBean>();

	@Before
	public void before() throws IOException, URISyntaxException {
		// Load the application from the test resources.
		final ApplicationLoadResult result = RuntimeModelIo.loadApplication(TestUtils.findTestFile("/example-app"));
		assertThat(result.getApplication()).isNotNull();
		context = MonitoredApplication.applicationContext(result.getApplication());

		instanceContexts = instancesByPath(context.getInstances());

		// We need to re-arrange the data for easier further access.
		for (final Entry<String, Set<InstanceContextBean>> entry : context.getInstancesByType().entrySet()) {
			final Set<String> instancesOfType = new LinkedHashSet<String>();
			for (final InstanceContextBean i : entry.getValue()) {
				instancesOfType.add(i.getPath());
			}
			instancesOf.put(entry.getKey(), instancesOfType);
		}


	}

	@Test
	public void testName() {
		assertThat(context.getName()).isEqualTo("example-app");
	}

	@Test
	public void testDescription() {
		assertThat(context.getDescription()).isEqualTo("An example application");
	}

	@Test
	public void testComponents() {
		assertThat(context.getComponents()).containsOnly("Vm", "MySql", "Apache", "Tomcat", "War");
	}

	@Test
	public void testInstancesByType() {
		assertThat(instancesOf.get("Vm"))
				.containsOnly("/MySqlVm", "/ApacheVm", "/TomcatVm1", "/TomcatVm2");
		assertThat(instancesOf.get("Virtual"))
				.containsOnly("/MySqlVm", "/ApacheVm", "/TomcatVm1", "/TomcatVm2");
		assertThat(instancesOf.get("Machine"))
				.containsOnly("/MySqlVm", "/ApacheVm", "/TomcatVm1", "/TomcatVm2");

		assertThat(instancesOf.get("MySql"))
				.containsOnly("/MySqlVm/MySql");
		assertThat(instancesOf.get("Apache"))
				.containsOnly("/ApacheVm/Apache");
		assertThat(instancesOf.get("Tomcat"))
				.containsOnly("/TomcatVm1/Tomcat", "/TomcatVm2/Tomcat");
		assertThat(instancesOf.get("Service"))
				.containsOnly("/MySqlVm/MySql", "/ApacheVm/Apache", "/TomcatVm1/Tomcat", "/TomcatVm2/Tomcat");
		assertThat(instancesOf.get("NetworkService"))
				.containsOnly("/MySqlVm/MySql", "/ApacheVm/Apache", "/TomcatVm1/Tomcat", "/TomcatVm2/Tomcat");

		assertThat(instancesOf.get("War"))
				.containsOnly("/TomcatVm1/Tomcat/WebApp", "/TomcatVm2/Tomcat/WebApp");
		assertThat(instancesOf.get("Application"))
				.containsOnly("/TomcatVm1/Tomcat/WebApp", "/TomcatVm2/Tomcat/WebApp");
	}

	@Test
	public void testMySqlVmInstance() {
		final InstanceContextBean instance = instanceContexts.get("/MySqlVm");
		assertThat(instance.getName()).isEqualTo("MySqlVm");
		assertThat(instance.getPath()).isEqualTo("/MySqlVm");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("Vm");
		assertThat(instance.getTypes()).containsOnly("Vm", "Virtual", "Machine", "VirtualMachine");
		assertThat(instance.getParent()).isNull();
		assertThat(instance.getChildren()).containsOnly(instanceContexts.get("/MySqlVm/MySql"));
		assertThat(instance.getIp()).isNull();
		assertThat(instance.getInstaller()).isEqualTo("target");
		assertThat(instance.getExports()).isEmpty();
		assertThat(instance.getImports()).isEmpty();
		assertThat(variableMapOf(instance.getData()))
				.hasSize(1).includes(entry("application.name", "example-app"));
	}

	@Test
	public void testApacheVmInstance() {
		final InstanceContextBean instance = instanceContexts.get("/ApacheVm");
		assertThat(instance.getName()).isEqualTo("ApacheVm");
		assertThat(instance.getPath()).isEqualTo("/ApacheVm");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("Vm");
		assertThat(instance.getTypes()).containsOnly("Vm", "Virtual", "Machine", "VirtualMachine");
		assertThat(instance.getParent()).isNull();
		assertThat(instance.getChildren()).containsOnly(instanceContexts.get("/ApacheVm/Apache"));
		assertThat(instance.getIp()).isNull();
		assertThat(instance.getInstaller()).isEqualTo("target");
		assertThat(instance.getExports()).isEmpty();
		assertThat(instance.getImports()).isEmpty();
		assertThat(variableMapOf(instance.getData()))
				.hasSize(1).includes(entry("application.name", "example-app"));
	}

	@Test
	public void testTomcatVmInstance1() {
		final InstanceContextBean instance = instanceContexts.get("/TomcatVm1");
		assertThat(instance.getName()).isEqualTo("TomcatVm1");
		assertThat(instance.getPath()).isEqualTo("/TomcatVm1");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("Vm");
		assertThat(instance.getTypes()).containsOnly("Vm", "Virtual", "Machine", "VirtualMachine");
		assertThat(instance.getParent()).isNull();
		assertThat(instance.getChildren()).containsOnly(instanceContexts.get("/TomcatVm1/Tomcat"));
		assertThat(instance.getIp()).isNull();
		assertThat(instance.getInstaller()).isEqualTo("target");
		assertThat(instance.getExports()).isEmpty();
		assertThat(instance.getImports()).isEmpty();
		assertThat(variableMapOf(instance.getData()))
				.hasSize(1).includes(entry("application.name", "example-app"));
	}

	@Test
	public void testTomcatVmInstance2() {
		final InstanceContextBean instance = instanceContexts.get("/TomcatVm2");
		assertThat(instance.getName()).isEqualTo("TomcatVm2");
		assertThat(instance.getPath()).isEqualTo("/TomcatVm2");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("Vm");
		assertThat(instance.getTypes()).containsOnly("Vm", "Virtual", "Machine", "VirtualMachine");
		assertThat(instance.getParent()).isNull();
		assertThat(instance.getChildren()).containsOnly(instanceContexts.get("/TomcatVm2/Tomcat"));
		assertThat(instance.getIp()).isNull();
		assertThat(instance.getInstaller()).isEqualTo("target");
		assertThat(instance.getExports()).isEmpty();
		assertThat(instance.getImports()).isEmpty();
		assertThat(variableMapOf(instance.getData()))
				.hasSize(1).includes(entry("application.name", "example-app"));
	}

	@Test
	public void testMySqlInstance() {
		final InstanceContextBean instance = instanceContexts.get("/MySqlVm/MySql");
		assertThat(instance.getName()).isEqualTo("MySql");
		assertThat(instance.getPath()).isEqualTo("/MySqlVm/MySql");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("MySql");
		assertThat(instance.getTypes()).containsOnly("MySql", "NetworkService", "Service");
		assertThat(instance.getParent()).isSameAs(instanceContexts.get("/MySqlVm"));
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
		final InstanceContextBean instance = instanceContexts.get("/ApacheVm/Apache");
		assertThat(instance.getName()).isEqualTo("Apache");
		assertThat(instance.getPath()).isEqualTo("/ApacheVm/Apache");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("Apache");
		assertThat(instance.getTypes()).containsOnly("Apache", "NetworkService", "Service");
		assertThat(instance.getParent()).isSameAs(instanceContexts.get("/ApacheVm"));
		assertThat(instance.getChildren()).isEmpty();
		assertThat(instance.getIp()).isNull();
		assertThat(instance.getInstaller()).isEqualTo("script");
		assertThat(instance.getExports()).isEmpty();
		assertThat(instance.getImports()).isEmpty();
		assertThat(instance.getData()).isEmpty();
	}

	@Test
	public void testTomcatInstance1() {
		final InstanceContextBean instance = instanceContexts.get("/TomcatVm1/Tomcat");
		assertThat(instance.getName()).isEqualTo("Tomcat");
		assertThat(instance.getPath()).isEqualTo("/TomcatVm1/Tomcat");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("Tomcat");
		assertThat(instance.getTypes()).containsOnly("Tomcat", "NetworkService", "Service");
		assertThat(instance.getParent()).isSameAs(instanceContexts.get("/TomcatVm1"));
		assertThat(instance.getChildren()).containsOnly(instanceContexts.get("/TomcatVm1/Tomcat/WebApp"));
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
		final InstanceContextBean instance = instanceContexts.get("/TomcatVm2/Tomcat");
		assertThat(instance.getName()).isEqualTo("Tomcat");
		assertThat(instance.getPath()).isEqualTo("/TomcatVm2/Tomcat");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("Tomcat");
		assertThat(instance.getTypes()).containsOnly("Tomcat", "NetworkService", "Service");
		assertThat(instance.getParent()).isSameAs(instanceContexts.get("/TomcatVm2"));
		assertThat(instance.getChildren()).containsOnly(instanceContexts.get("/TomcatVm2/Tomcat/WebApp"));
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
		final InstanceContextBean instance = instanceContexts.get("/TomcatVm1/Tomcat/WebApp");
		assertThat(instance.getName()).isEqualTo("WebApp");
		assertThat(instance.getPath()).isEqualTo("/TomcatVm1/Tomcat/WebApp");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("War");
		assertThat(instance.getTypes()).containsOnly("War", "Application");
		assertThat(instance.getParent()).isSameAs(instanceContexts.get("/TomcatVm1/Tomcat"));
		assertThat(instance.getChildren()).isEmpty();
		assertThat(instance.getIp()).isNull();
		assertThat(instance.getInstaller()).isEqualTo("human");
		assertThat(instance.getExports()).isEmpty();
		assertThat(instance.getImports()).isEmpty();
		assertThat(instance.getData()).isEmpty();
	}

	@Test
	public void testWebAppInstance2() {
		final InstanceContextBean instance = instanceContexts.get("/TomcatVm2/Tomcat/WebApp");
		assertThat(instance.getName()).isEqualTo("WebApp");
		assertThat(instance.getPath()).isEqualTo("/TomcatVm2/Tomcat/WebApp");
		assertThat(instance.getStatus()).isEqualTo(NOT_DEPLOYED);
		assertThat(instance.getStatusIsStable()).isTrue();
		assertThat(instance.getComponent()).isEqualTo("War");
		assertThat(instance.getTypes()).containsOnly("War", "Application");
		assertThat(instance.getParent()).isSameAs(instanceContexts.get("/TomcatVm2/Tomcat"));
		assertThat(instance.getChildren()).isEmpty();
		assertThat(instance.getIp()).isNull();
		assertThat(instance.getInstaller()).isEqualTo("human");
		assertThat(instance.getExports()).isEmpty();
		assertThat(instance.getImports()).isEmpty();
		assertThat(instance.getData()).isEmpty();
	}

}
