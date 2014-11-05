/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of their joint LINAGORA -
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

package net.roboconf.dm.rest.client.delegates;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status.Family;

import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.dm.rest.client.exceptions.ApplicationException;
import net.roboconf.dm.rest.commons.UrlConstants;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationWsDelegate {

	private final WebResource resource;
	private final Logger logger;


	/**
	 * Constructor.
	 * @param resource a web resource
	 */
	public ApplicationWsDelegate( WebResource resource ) {
		this.resource = resource;
		this.logger = Logger.getLogger( getClass().getName());
	}


	/**
	 * Changes the state of an instance.
	 * <p>
	 * Notice that these actions, like of most of the others, are performed asynchronously.
	 * It means invoking these REST operations is equivalent to submitting a request. How it
	 * will be processed concretely will depend then on the agent.
	 * </p>
	 *
	 * @param applicationName the application name
	 * @param newStatus the new state of the instance
	 * @param instancePath the instance path (not null)
	 * @throws ApplicationException if something went wrong
	 */
	public void changeInstanceState( String applicationName, InstanceStatus newStatus, String instancePath )
	throws ApplicationException {

		this.logger.finer( "Changing state of " + instancePath + " to '" + newStatus + "' in " + applicationName + "."  );

		WebResource path = this.resource.path( UrlConstants.APP ).path( applicationName ).path( "change-state" );
		if( instancePath != null )
			path = path.queryParam( "instance-path", instancePath );

		if( newStatus != null )
			path = path.queryParam( "new-state", newStatus.toString());

		ClientResponse response = path.accept( MediaType.APPLICATION_JSON ).post( ClientResponse.class );
		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new ApplicationException( response.getStatusInfo().getStatusCode(), value );
		}

		this.logger.finer( String.valueOf( response.getStatusInfo()));
	}


	/**
	 * Deploys and starts several instances at once.
	 * @param applicationName the application name
	 * @param instancePath the instance path (null for all the application instances)
	 * @throws ApplicationException if something went wrong
	 */
	public void deployAndStartAll( String applicationName, String instancePath )
	throws ApplicationException {

		this.logger.finer( "Deploying and starting instances in " + applicationName + " from instance = " + instancePath  );

		WebResource path = this.resource.path( UrlConstants.APP ).path( applicationName ).path( "deploy-all" );
		if( instancePath != null )
			path = path.queryParam( "instance-path", instancePath );

		ClientResponse response = path.accept( MediaType.APPLICATION_JSON ).post( ClientResponse.class );
		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new ApplicationException( response.getStatusInfo().getStatusCode(), value );
		}

		this.logger.finer( String.valueOf( response.getStatusInfo()));
	}


	/**
	 * Stops several instances at once.
	 * @param applicationName the application name
	 * @param instancePath the path of the instance to stop (null for all the application instances)
	 * @throws ApplicationException if something went wrong
	 */
	public void stopAll( String applicationName, String instancePath )
	throws ApplicationException {

		this.logger.finer( "Stopping instances in " + applicationName + " from instance = " + instancePath  );

		WebResource path = this.resource.path( UrlConstants.APP ).path( applicationName ).path( "stop-all" );
		if( instancePath != null )
			path = path.queryParam( "instance-path", instancePath );

		ClientResponse response = path.accept( MediaType.APPLICATION_JSON ).post( ClientResponse.class );
		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new ApplicationException( response.getStatusInfo().getStatusCode(), value );
		}

		this.logger.finer( String.valueOf( response.getStatusInfo()));
	}


	/**
	 * Undeploys several instances at once.
	 * @param applicationName the application name
	 * @param instancePath the path of the instance to undeploy (null for all the application instances)
	 * @throws ApplicationException if something went wrong
	 */
	public void undeployAll( String applicationName, String instancePath )
	throws ApplicationException {

		this.logger.finer( "Undeploying instances in " + applicationName + " from instance = " + instancePath  );

		WebResource path = this.resource.path( UrlConstants.APP ).path( applicationName ).path( "undeploy-all" );
		if( instancePath != null )
			path = path.queryParam( "instance-path", instancePath );

		ClientResponse response = path.accept( MediaType.APPLICATION_JSON ).post( ClientResponse.class );
		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new ApplicationException( response.getStatusInfo().getStatusCode(), value );
		}

		this.logger.finer( String.valueOf( response.getStatusInfo()));
	}


	/**
	 * Lists all the children of an instance.
	 * @param applicationName the application name
	 * @param instancePath the instance path (null to get root instances)
	 * @param all true to list indirect children too, false to only list direct children
	 * @return a non-null list of instance paths
	 */
	public List<Instance> listChildrenInstances( String applicationName, String instancePath, boolean all ) {
		this.logger.finer( "Listing children instances for " + instancePath + " in " + applicationName + "." );

		WebResource path = this.resource
				.path( UrlConstants.APP ).path( applicationName ).path( "children" )
				.queryParam( "all-children", String.valueOf( all ));

		if( instancePath != null )
			path = path.queryParam( "instance-path", instancePath );

		List<Instance> result =
				path.accept( MediaType.APPLICATION_JSON )
				.type( MediaType.APPLICATION_JSON )
				.get( new GenericType<List<Instance>> () {});

		if( result != null )
			this.logger.finer( result.size() + " children instances were found for " + instancePath + " in " + applicationName + "." );
		else
			this.logger.finer( "No child instance was found for " + instancePath + " in " + applicationName + "." );

		return result != null ? result : new ArrayList<Instance> ();
	}


	/**
	 * Adds an instance into an application.
	 * @param applicationName the application name
	 * @param parentInstancePath the path of the parent instance (null to create a root instance)
	 * @param instance the instance to add
	 * @throws ApplicationException if a problem occurred with the instance management
	 */
	public void addInstance( String applicationName, String parentInstancePath, Instance instance ) throws ApplicationException {
		this.logger.finer( "Adding an instance to the application " + applicationName + "..." );

		WebResource path = this.resource.path( UrlConstants.APP ).path( applicationName ).path( "instances" );
		if( parentInstancePath != null )
			path = path.queryParam( "instance-path", parentInstancePath );

		ClientResponse response = path
				.accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON )
				.post( ClientResponse.class, instance );

		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new ApplicationException( response.getStatusInfo().getStatusCode(), value );
		}

		this.logger.finer( String.valueOf( response.getStatusInfo()));
	}


	/**
	 * Lists all the components from a given application.
	 * @param applicationName the application name
	 * @return a non-null list of components
	 */
	public List<Component> listAllComponents( String applicationName ) {
		this.logger.finer( "Listing components for application " + applicationName + "..." );

		List<Component> result = this.resource
				.path( UrlConstants.APP ).path( applicationName ).path( "components" )
				.accept( MediaType.APPLICATION_JSON )
				.get( new GenericType<List<Component>> () {});

		if( result != null )
			this.logger.finer( result.size() + " components were found for the application " + applicationName + "." );
		else
			this.logger.finer( "No component was found for the application " + applicationName + "." );

		return result != null ? result : new ArrayList<Component> ();
	}


	/**
	 * Finds the component names you could instantiate and deploy on an existing instance.
	 * @param applicationName the application name
	 * @param instancePath an instance path (null to get root components)
	 * @return a non-null list of component names
	 */
	public List<Component> findPossibleComponentChildren( String applicationName, String instancePath ) {
		this.logger.finer( "Listing possible child components for instance " + instancePath + "..." );

		WebResource path = this.resource.path( UrlConstants.APP ).path( applicationName ).path( "possibilities" );
		if( instancePath != null )
			path = path.queryParam( "instance-path", instancePath );

		List<Component> result = path.accept( MediaType.APPLICATION_JSON ).get( new GenericType<List<Component>> () {});
		if( result != null )
			this.logger.finer( result.size() + " possible children was or were found for " + instancePath + "." );
		else
			this.logger.finer( "No possible child was found for " + instancePath + "." );

		return result != null ? result : new ArrayList<Component> ();
	}


	/**
	 * Finds the instances on which you could deploy a new instance of a component.
	 * @param applicationName the application name
	 * @param componentName a component name
	 * @return a non-null list of instance paths
	 */
	public List<String> findPossibleParentInstances( String applicationName, String componentName ) {
		this.logger.finer( "Listing possible parent instances for component " + componentName + "..." );

		List<String> result = this.resource
				.path( UrlConstants.APP ).path( applicationName ).path( "component" ).path( componentName )
				.accept( MediaType.APPLICATION_JSON ).get( new GenericType<List<String>> () {});

		if( result != null )
			this.logger.finer( result.size() + " possible parents was or were found for " + componentName + "." );
		else
			this.logger.finer( "No possible parent was found for " + componentName + "." );

		return result != null ? result : new ArrayList<String> ();
	}


	/**
	 * Creates an instance from a component name.
	 * <p>
	 * The instance is not added into the application.<br />
	 * This method is like a factory.
	 * </p>
	 *
	 * @param applicationName the application name
	 * @param componentName a component name
	 * @return a new instance
	 * @throws ApplicationException if a problem occurred with the creation of an instance
	 */
	public Instance createInstanceFromComponent( String applicationName, String componentName ) throws ApplicationException {
		this.logger.finer( "Creating a new instance of component: " + componentName + "..." );

		ClientResponse response = this.resource
				.path( UrlConstants.APP ).path( applicationName ).path( "component" ).path( componentName ).path( "new" )
				.accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON )
				.get( ClientResponse.class );

		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new ApplicationException( response.getStatusInfo().getStatusCode(), value );
		}

		Instance result = null;
		this.logger.finer( String.valueOf( response.getStatusInfo()));
		if( Status.OK.getStatusCode() == response.getStatusInfo().getStatusCode())
			result = response.getEntity( Instance.class );

		return result;
	}
}
