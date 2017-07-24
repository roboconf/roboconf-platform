/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.utils;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.internal.tests.TestApplication;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DockerAndScriptUtilsTest {

	@Test
	public void testCleanInstancePath() {

		Assert.assertEquals( "", DockerAndScriptUtils.cleanInstancePath( "" ));
		Assert.assertEquals( "root", DockerAndScriptUtils.cleanInstancePath( "/root" ));
		Assert.assertEquals( "root_web_server_app_1", DockerAndScriptUtils.cleanInstancePath( "/root/web server/app-1" ));
	}


	@Test
	public void testCleanReversedInstancePath() {

		Assert.assertEquals( "", DockerAndScriptUtils.cleanReversedInstancePath( "" ));
		Assert.assertEquals( "root", DockerAndScriptUtils.cleanReversedInstancePath( "/root" ));
		Assert.assertEquals( "app_1_web_server_root", DockerAndScriptUtils.cleanReversedInstancePath( "/root/web server/app-1" ));
	}


	@Test
	public void testBuildReferenceMap() {

		TestApplication app = new TestApplication();
		Map<String,String> map = DockerAndScriptUtils.buildReferenceMap( app.getMySqlVm());
		Assert.assertEquals( 5, map.size());
		Assert.assertEquals( app.getMySqlVm().getName(), map.get( DockerAndScriptUtils.ROBOCONF_INSTANCE_NAME ));
		Assert.assertEquals( "/" + app.getMySqlVm().getName(), map.get( DockerAndScriptUtils.ROBOCONF_INSTANCE_PATH ));
		Assert.assertEquals( app.getMySqlVm().getComponent().getName(), map.get( DockerAndScriptUtils.ROBOCONF_COMPONENT_NAME ));
		Assert.assertEquals( "mysql_vm", map.get( DockerAndScriptUtils.ROBOCONF_CLEAN_INSTANCE_PATH ));
		Assert.assertEquals( "mysql_vm", map.get( DockerAndScriptUtils.ROBOCONF_CLEAN_REVERSED_INSTANCE_PATH ));

		map = DockerAndScriptUtils.buildReferenceMap( app.getWar());
		Assert.assertEquals( 5, map.size());
		Assert.assertEquals( app.getWar().getName(), map.get( DockerAndScriptUtils.ROBOCONF_INSTANCE_NAME ));
		Assert.assertEquals( "/tomcat-vm/tomcat-server/hello-world", map.get( DockerAndScriptUtils.ROBOCONF_INSTANCE_PATH ));
		Assert.assertEquals( app.getWar().getComponent().getName(), map.get( DockerAndScriptUtils.ROBOCONF_COMPONENT_NAME ));
		Assert.assertEquals( "tomcat_vm_tomcat_server_hello_world", map.get( DockerAndScriptUtils.ROBOCONF_CLEAN_INSTANCE_PATH ));
		Assert.assertEquals( "hello_world_tomcat_server_tomcat_vm", map.get( DockerAndScriptUtils.ROBOCONF_CLEAN_REVERSED_INSTANCE_PATH ));
	}
}
