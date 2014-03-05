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

package net.roboconf.dm.rest.client.test;

import net.roboconf.dm.rest.client.WsClient;

import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RestTestUtils {

	/**
	 * @return a configured WS client
	 */
	public static WsClient buildWsClient() {
		return new WsClient( "http://localhost:9998" );
	}


	/**
	 * @return a web descriptor for the tests
	 */
	public static AppDescriptor buildTestDescriptor() {

		return new WebAppDescriptor.Builder()
			.servletClass( ServletContainer.class )
			.initParam( PackagesResourceConfig.PROPERTY_PACKAGES, "net.roboconf.dm.server;net.roboconf.dm.rest.json" )
			.initParam( "com.sun.jersey.api.json.POJOMappingFeature", "true" )
			.initParam( "com.sun.jersey.spi.container.ContainerResponseFilters", "net.roboconf.dm.rest.cors.ResponseCorsFilter" )
			.initParam( "com.sun.jersey.config.feature.DisableWADL", "true" )
			.build();
	}
}
