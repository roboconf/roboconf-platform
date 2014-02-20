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

package net.roboconf.dm.rest.client.delegates;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status.Family;

import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.dm.rest.RestUtils;
import net.roboconf.dm.rest.UrlConstants;
import net.roboconf.dm.rest.client.exceptions.ApplicationException;

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
	 * Lists all the instances for a given application.
	 * @param applicationName the application name
	 * @return a non-null list of instance paths
	 */
	public List<Instance> listAllInstances( String applicationName ) {
		this.logger.finer( "Listing instance paths for application " + applicationName + "..." );

		List<Instance> result = this.resource
				.path( UrlConstants.APP ).path( applicationName ).path( "all" )
				.accept( MediaType.APPLICATION_JSON )
				.get( new GenericType<List<Instance>> () {});

		if( result != null )
			this.logger.finer( result.size() + " instances were found for the application " + applicationName + "." );
		else
			this.logger.finer( "No instance was found for the application " + applicationName + "." );

		return result != null ? result : new ArrayList<Instance> ();
	}


	/**
	 * Lists all the root instances for a given application.
	 * @param applicationName the application name
	 * @return a non-null list of instance paths
	 */
	public List<Instance> listRootInstances( String applicationName ) {
		this.logger.finer( "Listing instance paths for application " + applicationName + "..." );

		List<Instance> result = this.resource
				.path( UrlConstants.APP ).path( applicationName ).path( "roots" )
				.accept( MediaType.APPLICATION_JSON )
				.get( new GenericType<List<Instance>> () {});

		if( result != null )
			this.logger.finer( result.size() + " root instances were found for the application " + applicationName + "." );
		else
			this.logger.finer( "No root instance was found for the application " + applicationName + "." );

		return result != null ? result : new ArrayList<Instance> ();
	}


	/**
	 * Lists all the children instances for a given application and instance path.
	 * @param applicationName the application name
	 * @param parentInstancePath the parent instance path (not null)
	 * @return a non-null list of instance paths
	 */
	public List<Instance> listChildrenInstances( String applicationName, String parentInstancePath ) {
		this.logger.finer( "Listing instance paths for application " + applicationName + "..." );

		String encodedPath = RestUtils.toRestfulPath( parentInstancePath );
		List<Instance> result = this.resource
				.path( UrlConstants.APP ).path( applicationName ).path( encodedPath ).path( "children" )
				.accept( MediaType.APPLICATION_JSON )
				.get( new GenericType<List<Instance>> () {});

		if( result != null )
			this.logger.finer( result.size() + " instances were found for the application " + applicationName + "." );
		else
			this.logger.finer( "No instance was found for the application " + applicationName + "." );

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

		// This method is a proxy for two operations (addRootInstance and addInstance).
		// These operations are available at different URL.
		String encodedPath = null;
		if( parentInstancePath != null )
			encodedPath = RestUtils.toRestfulPath( parentInstancePath );

		WebResource res = this.resource.path( UrlConstants.APP ).path( applicationName );
		if( encodedPath != null )
			res = res.path( encodedPath );

		ClientResponse response = res
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
	 * Gets an instance from a path.
	 * @param applicationName the application name
	 * @param instancePath the instance path
	 * @return the instance object
	 * @throws ApplicationException if a problem occurred with the instance management
	 */
	public Instance getInstance( String applicationName, String instancePath ) throws ApplicationException {
		this.logger.finer( "Getting an instance object for the application " + applicationName + "..." );

		String encodedPath = RestUtils.toRestfulPath( instancePath );
		ClientResponse response = this.resource
				.path( UrlConstants.APP ).path( applicationName ).path( encodedPath )
				.accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON )
				.get( ClientResponse.class );

		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new ApplicationException( response.getStatusInfo().getStatusCode(), value );
		}

		this.logger.finer( String.valueOf( response.getStatusInfo()));
		return response.getEntity( Instance.class );
	}


	/**
	 * Removes an instance from an application.
	 * @param applicationName the application name
	 * @param instancePath the instance path
	 * @throws ApplicationException if a problem occurred with the instance management
	 */
	public void removeInstance( String applicationName, String instancePath ) throws ApplicationException {
		this.logger.finer( "Removing an instance from the application " + applicationName + "..." );

		String encodedPath = RestUtils.toRestfulPath( instancePath );
		ClientResponse response = this.resource
				.path( UrlConstants.APP ).path( applicationName ).path( encodedPath )
				.delete( ClientResponse.class );

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
	 * @param instancePath an instance path
	 * @return a non-null list of component names
	 */
	public List<String> findPossibleComponentChildren( String applicationName, String instancePath ) {
		this.logger.finer( "Listing possible child components for instance " + instancePath + "..." );

		String encodedPath = RestUtils.toRestfulPath( instancePath );
		List<String> result = this.resource
				.path( UrlConstants.APP ).path( applicationName ).path( encodedPath ).path( "possibilities" )
				.accept( MediaType.APPLICATION_JSON ).get( new GenericType<List<String>> () {});

		if( result != null )
			this.logger.finer( result.size() + " possible children was or were found for " + instancePath + "." );
		else
			this.logger.finer( "No possible child was found for " + instancePath + "." );

		return result != null ? result : new ArrayList<String> ();
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
