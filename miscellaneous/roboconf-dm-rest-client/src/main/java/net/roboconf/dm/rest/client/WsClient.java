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

package net.roboconf.dm.rest.client;

import javax.ws.rs.core.Cookie;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import net.roboconf.dm.rest.client.delegates.ApplicationWsDelegate;
import net.roboconf.dm.rest.client.delegates.AuthenticationWsDelegate;
import net.roboconf.dm.rest.client.delegates.DebugWsDelegate;
import net.roboconf.dm.rest.client.delegates.ManagementWsDelegate;
import net.roboconf.dm.rest.client.delegates.PreferencesWsDelegate;
import net.roboconf.dm.rest.client.delegates.SchedulerWsDelegate;
import net.roboconf.dm.rest.client.delegates.TargetWsDelegate;
import net.roboconf.dm.rest.commons.UrlConstants;
import net.roboconf.dm.rest.commons.json.ObjectMapperProvider;

/**
 * A client for the REST API of the Deployment Manager.
 * <p>
 * This client is configured for the REST implementations that use the predefined
 * PATH constants. Users should call {@link #destroy()} to release the connection.
 * </p>
 * <p>
 * The following rules apply to the client implementation.
 * </p>
 * <ul>
 * 		<li>Methods that return a list do not throw exceptions.</li>
 * 		<li>Methods that return a list never return null.</li>
 * 		<li>Methods that return a list only expected 2xx responses from the server.</li>
 * 		<li>Methods that return an object throw an exception if they did not receive a 2xx response.</li>
 * 		<li>Methods that return an object return null if they did not receive a 200 response.</li>
 * 		<li>Methods that do not return anything throw an exception if they did not receive a 2xx response.</li>
 * </ul>
 * <p>
 * Thrown exceptions contain the error code and the error message.
 * </p>
 * <p>
 * About the logging policy...<br>
 * Every client method logs an entry when it is invoked.
 * It logs a second entry once the REST invocation has completed, and provided a runtime
 * exception was not thrown by the REST library.
 * </p>
 * <p>
 * The second log entry either logs the response code (success), or logs the response
 * code and the response message (error in the result).
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class WsClient {

	private final ApplicationWsDelegate applicationDelegate;
	private final ManagementWsDelegate managementDelegate;
	private final DebugWsDelegate debugDelegate;
	private final TargetWsDelegate targetWsDelegate;
	private final SchedulerWsDelegate schedulerDelegate;
	private final PreferencesWsDelegate preferencesWsDelegate;
	private final AuthenticationWsDelegate authenticationWsDelegate;

	private final Client client;
	private String sessionId;


	/**
	 * Constructor.
	 * @param rootUrl the root URL (example: http://192.168.1.18:9007/dm/)
	 */
	public WsClient( String rootUrl ) {

		ClientConfig cc = new DefaultClientConfig();
		cc.getClasses().add( JacksonJsonProvider.class );
		cc.getClasses().add( ObjectMapperProvider.class );

		this.client = Client.create( cc );
		this.client.setFollowRedirects( true );

		WebResource resource = this.client.resource( rootUrl );
		this.applicationDelegate = new ApplicationWsDelegate( resource, this );
		this.managementDelegate = new ManagementWsDelegate( resource, this );
		this.debugDelegate = new DebugWsDelegate( resource, this );
		this.targetWsDelegate = new TargetWsDelegate( resource, this );
		this.schedulerDelegate = new SchedulerWsDelegate( resource, this );
		this.preferencesWsDelegate = new PreferencesWsDelegate( resource, this );
		this.authenticationWsDelegate = new AuthenticationWsDelegate( resource, this );
	}


	/**
	 * Force the destruction of the JAX-RS client.
	 */
	public void destroy() {
		this.client.destroy();
	}


	/**
	 * @return the applicationDelegate
	 */
	public ApplicationWsDelegate getApplicationDelegate() {
		return this.applicationDelegate;
	}

	/**
	 * @return the appManagementDelegate
	 */
	public ManagementWsDelegate getManagementDelegate() {
		return this.managementDelegate;
	}

	/**
	 * @return the debugDelegate
	 */
	public DebugWsDelegate getDebugDelegate() {
		return this.debugDelegate;
	}

	/**
	 * @return the targetWsDelegate
	 */
	public TargetWsDelegate getTargetWsDelegate() {
		return this.targetWsDelegate;
	}

	/**
	 * @return the schedulerDelegate
	 */
	public SchedulerWsDelegate getSchedulerDelegate() {
		return this.schedulerDelegate;
	}

	/**
	 * @return the preferencesWsDelegate
	 */
	public PreferencesWsDelegate getPreferencesWsDelegate() {
		return this.preferencesWsDelegate;
	}

	/**
	 * @return the authenticationWsDelegate
	 */
	public AuthenticationWsDelegate getAuthenticationWsDelegate() {
		return this.authenticationWsDelegate;
	}

	/**
	 * @return the Jersey client (useful to configure it)
	 */
	public Client getJerseyClient() {
		return this.client;
	}

	/**
	 * Sets the session ID to use when authenticated.
	 * @param sessionId
	 */
	public void setSessionId( String sessionId ) {
		this.sessionId = sessionId;
	}

	/**
	 * @return the sessionId
	 */
	public String getSessionId() {
		return this.sessionId;
	}


	/**
	 * @param resource a web resource
	 * @return a builder with an optional cookie (for authentication)
	 */
	public WebResource.Builder createBuilder( WebResource resource ) {

		WebResource.Builder result;
		if( this.sessionId != null )
			result = resource.cookie( new Cookie( UrlConstants.SESSION_ID, this.sessionId ));
		else
			result = resource.getRequestBuilder();

		return result;
	}
}
