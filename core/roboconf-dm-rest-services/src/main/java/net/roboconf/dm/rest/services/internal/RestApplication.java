/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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

import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.services.cors.ResponseCorsFilter;
import net.roboconf.dm.rest.services.internal.resources.IApplicationCommandsResource;
import net.roboconf.dm.rest.services.internal.resources.IApplicationResource;
import net.roboconf.dm.rest.services.internal.resources.IDebugResource;
import net.roboconf.dm.rest.services.internal.resources.IManagementResource;
import net.roboconf.dm.rest.services.internal.resources.IPreferencesResource;
import net.roboconf.dm.rest.services.internal.resources.ITargetResource;
import net.roboconf.dm.rest.services.internal.resources.impl.ApplicationCommandsResource;
import net.roboconf.dm.rest.services.internal.resources.impl.ApplicationResource;
import net.roboconf.dm.rest.services.internal.resources.impl.DebugResource;
import net.roboconf.dm.rest.services.internal.resources.impl.ManagementResource;
import net.roboconf.dm.rest.services.internal.resources.impl.PreferencesResource;
import net.roboconf.dm.rest.services.internal.resources.impl.SchedulerResource;
import net.roboconf.dm.rest.services.internal.resources.impl.TargetResource;
import net.roboconf.dm.scheduler.IScheduler;

import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RestApplication extends DefaultResourceConfig {

	private final IApplicationCommandsResource applicationCommandsResource;
	private final IApplicationResource applicationResource;
	private final IManagementResource managementResource;
	private final IDebugResource debugResource;
	private final ITargetResource targetResource;
	private final IPreferencesResource preferencesResource;
	private final SchedulerResource schedulerResource;


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
		this.applicationCommandsResource = new ApplicationCommandsResource( manager );
		this.schedulerResource = new SchedulerResource();

		getFeatures().put( "com.sun.jersey.api.json.POJOMappingFeature", Boolean.TRUE );
		getFeatures().put( ResourceConfig.FEATURE_DISABLE_WADL, Boolean.TRUE );

		getProperties().put( ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS, ResponseCorsFilter.class.getName());
	}


	@Override
	public Set<Class<?>> getClasses() {

		Set<Class<?>> result = new HashSet<Class<?>> ();
		result.add( net.roboconf.dm.rest.commons.json.ObjectMapperProvider.class );
		result.add( com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider.class );

		return result;
	}


	@Override
	public Set<Object> getSingletons() {

		HashSet<Object> set = new HashSet<Object> ();
		set.add( this.applicationResource );
		set.add( this.managementResource );
		set.add( this.debugResource );
		set.add( this.targetResource );
		set.add( this.preferencesResource );
		set.add( this.applicationCommandsResource );
		set.add( this.schedulerResource );

		return set;
	}


	/**
	 * Sets the scheduler.
	 * @param scheduler the scheduler (can be null)
	 */
	public void setScheduler( IScheduler scheduler ) {
		this.schedulerResource.setScheduler( scheduler );
	}
}
