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

package net.roboconf.dm.rest.services.internal;

import java.util.HashSet;
import java.util.Set;

import org.ops4j.pax.url.mvn.MavenResolver;

import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;

import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.commons.security.AuthenticationManager;
import net.roboconf.dm.rest.services.cors.ResponseCorsFilter;
import net.roboconf.dm.rest.services.internal.resources.IApplicationResource;
import net.roboconf.dm.rest.services.internal.resources.IDebugResource;
import net.roboconf.dm.rest.services.internal.resources.IHistoryResource;
import net.roboconf.dm.rest.services.internal.resources.IPreferencesResource;
import net.roboconf.dm.rest.services.internal.resources.ITargetResource;
import net.roboconf.dm.rest.services.internal.resources.impl.ApplicationResource;
import net.roboconf.dm.rest.services.internal.resources.impl.AuthenticationResource;
import net.roboconf.dm.rest.services.internal.resources.impl.DebugResource;
import net.roboconf.dm.rest.services.internal.resources.impl.HistoryResource;
import net.roboconf.dm.rest.services.internal.resources.impl.ManagementResource;
import net.roboconf.dm.rest.services.internal.resources.impl.PreferencesResource;
import net.roboconf.dm.rest.services.internal.resources.impl.SchedulerResource;
import net.roboconf.dm.rest.services.internal.resources.impl.TargetResource;
import net.roboconf.dm.scheduler.IScheduler;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RestApplication extends DefaultResourceConfig {

	private final IApplicationResource applicationResource;
	private final IDebugResource debugResource;
	private final ITargetResource targetResource;
	private final IPreferencesResource preferencesResource;
	private final IHistoryResource historyResource;

	private final ManagementResource managementResource;
	private final SchedulerResource schedulerResource;
	private final AuthenticationResource authenticationResource;


	/**
	 * Constructor.
	 * @param manager
	 */
	public RestApplication( Manager manager ) {
		super();

		this.applicationResource = new ApplicationResource( manager );
		this.managementResource = new ManagementResource( manager );
		this.debugResource = new DebugResource( manager );
		this.targetResource = new TargetResource( manager );
		this.preferencesResource = new PreferencesResource( manager );
		this.schedulerResource = new SchedulerResource( manager );
		this.authenticationResource = new AuthenticationResource( manager );
		this.historyResource = new HistoryResource( manager );

		getFeatures().put( "com.sun.jersey.api.json.POJOMappingFeature", Boolean.TRUE );
		getFeatures().put( ResourceConfig.FEATURE_DISABLE_WADL, Boolean.TRUE );
	}


	@Override
	public Set<Class<?>> getClasses() {

		Set<Class<?>> result = new HashSet<> ();
		result.add( net.roboconf.dm.rest.commons.json.ObjectMapperProvider.class );
		result.add( com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider.class );

		return result;
	}


	/**
	 * @return a non-null set with all the resource classes
	 */
	public static Set<Class<?>> getResourceClasses() {

		Set<Class<?>> result = new HashSet<> ();
		result.add( ApplicationResource.class );
		result.add( ManagementResource.class );
		result.add( DebugResource.class );
		result.add( TargetResource.class );
		result.add( PreferencesResource.class );
		result.add( SchedulerResource.class );
		result.add( AuthenticationResource.class );
		result.add( HistoryResource.class );

		return result;
	}


	@Override
	public Set<Object> getSingletons() {

		HashSet<Object> set = new HashSet<> ();
		set.add( this.applicationResource );
		set.add( this.managementResource );
		set.add( this.debugResource );
		set.add( this.targetResource );
		set.add( this.preferencesResource );
		set.add( this.schedulerResource );
		set.add( this.authenticationResource );
		set.add( this.historyResource );

		return set;
	}


	/**
	 * Sets the scheduler.
	 * @param scheduler the scheduler (can be null)
	 */
	public void setScheduler( IScheduler scheduler ) {
		this.schedulerResource.setScheduler( scheduler );
	}


	/**
	 * Sets the Maven resolver.
	 * @param mavenResolver the Maven resolver (can be null)
	 */
	public void setMavenResolver( MavenResolver mavenResolver ) {
		this.managementResource.setMavenResolver( mavenResolver );
	}


	/**
	 * Sets the authentication manager.
	 * @param authenticationMngr the authentication manager (can be null)
	 */
	public void setAuthenticationManager( AuthenticationManager authenticationMngr ) {
		this.authenticationResource.setAuthenticationManager( authenticationMngr );
	}


	/**
	 * Enables or disables CORS.
	 * @param enableCors true to enable it
	 */
	public void enableCors( boolean enableCors ) {

		if( enableCors )
			getProperties().put( ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS, ResponseCorsFilter.class.getName());
		else
			getProperties().remove( ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS );
	}
}
