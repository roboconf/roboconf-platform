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

package net.roboconf.dm.rest.client.delegates;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status.Family;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.ApplicationWsException;
import net.roboconf.dm.rest.commons.UrlConstants;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationWsDelegate {

	private final WebResource resource;
	private final Logger logger;
	private final WsClient wsClient;


	/**
	 * Constructor.
	 * @param resource a web resource
	 * @param the WS client
	 */
	public ApplicationWsDelegate( WebResource resource, WsClient wsClient ) {
		this.resource = resource;
		this.wsClient = wsClient;
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
	 * @throws ApplicationWsException if something went wrong
	 */
	public void changeInstanceState( String applicationName, InstanceStatus newStatus, String instancePath )
	throws ApplicationWsException {

		this.logger.finer( "Changing state of " + instancePath + " to '" + newStatus + "' in " + applicationName + "."  );

		WebResource path = this.resource.path( UrlConstants.APP ).path( applicationName ).path( "change-state" );
		if( instancePath != null )
			path = path.queryParam( "instance-path", instancePath );

		if( newStatus != null )
			path = path.queryParam( "new-state", newStatus.toString());

		ClientResponse response = this.wsClient.createBuilder( path )
					.accept( MediaType.APPLICATION_JSON )
					.post( ClientResponse.class );

		handleResponse( response );
		this.logger.finer( String.valueOf( response.getStatusInfo()));
	}


	/**
	 * Changes the description of an application.
	 * @param applicationName the application name
	 * @param newDesc the new description to set
	 * @throws ApplicationWsException if something went wrong
	 */
	public void setDescription( String applicationName, String newDesc )
	throws ApplicationWsException {

		this.logger.finer( "Updating the description of application " + applicationName + "." );

		WebResource path = this.resource.path( UrlConstants.APP ).path( applicationName ).path( "description" );
		ClientResponse response = this.wsClient.createBuilder( path )
					.accept( MediaType.TEXT_PLAIN )
					.post( ClientResponse.class, newDesc );

		handleResponse( response );
		this.logger.finer( String.valueOf( response.getStatusInfo()));
	}


	/**
	 * Deploys and starts several instances at once.
	 * @param applicationName the application name
	 * @param instancePath the instance path (null for all the application instances)
	 * @throws ApplicationWsException if something went wrong
	 */
	public void deployAndStartAll( String applicationName, String instancePath )
	throws ApplicationWsException {

		this.logger.finer( "Deploying and starting instances in " + applicationName + " from instance = " + instancePath  );

		WebResource path = this.resource.path( UrlConstants.APP ).path( applicationName ).path( "deploy-all" );
		if( instancePath != null )
			path = path.queryParam( "instance-path", instancePath );

		ClientResponse response = this.wsClient.createBuilder( path )
						.accept( MediaType.APPLICATION_JSON )
						.post( ClientResponse.class );

		handleResponse( response );
		this.logger.finer( String.valueOf( response.getStatusInfo()));
	}


	/**
	 * Stops several instances at once.
	 * @param applicationName the application name
	 * @param instancePath the path of the instance to stop (null for all the application instances)
	 * @throws ApplicationWsException if something went wrong
	 */
	public void stopAll( String applicationName, String instancePath )
	throws ApplicationWsException {

		this.logger.finer( "Stopping instances in " + applicationName + " from instance = " + instancePath  );

		WebResource path = this.resource.path( UrlConstants.APP ).path( applicationName ).path( "stop-all" );
		if( instancePath != null )
			path = path.queryParam( "instance-path", instancePath );

		ClientResponse response = this.wsClient.createBuilder( path )
						.accept( MediaType.APPLICATION_JSON )
						.post( ClientResponse.class );

		handleResponse( response );
		this.logger.finer( String.valueOf( response.getStatusInfo()));
	}


	/**
	 * Undeploys several instances at once.
	 * @param applicationName the application name
	 * @param instancePath the path of the instance to undeploy (null for all the application instances)
	 * @throws ApplicationWsException if something went wrong
	 */
	public void undeployAll( String applicationName, String instancePath )
	throws ApplicationWsException {

		this.logger.finer( "Undeploying instances in " + applicationName + " from instance = " + instancePath  );

		WebResource path = this.resource.path( UrlConstants.APP ).path( applicationName ).path( "undeploy-all" );
		if( instancePath != null )
			path = path.queryParam( "instance-path", instancePath );

		ClientResponse response = this.wsClient.createBuilder( path )
						.accept( MediaType.APPLICATION_JSON )
						.post( ClientResponse.class );

		handleResponse( response );
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
				.path( UrlConstants.APP ).path( applicationName ).path( "instances" )
				.queryParam( "all-children", String.valueOf( all ));

		if( instancePath != null )
			path = path.queryParam( "instance-path", instancePath );

		List<Instance> result =
				this.wsClient.createBuilder( path )
				.accept( MediaType.APPLICATION_JSON )
				.type( MediaType.APPLICATION_JSON )
				.get( new GenericType<List<Instance>> () {});

		if( result != null ) {
			this.logger.finer( result.size() + " children instances were found for " + instancePath + " in " + applicationName + "." );
		} else {
			this.logger.finer( "No child instance was found for " + instancePath + " in " + applicationName + "." );
			result = new ArrayList<>( 0 );
		}

		return result;
	}


	/**
	 * Adds an instance into an application.
	 * @param applicationName the application name
	 * @param parentInstancePath the path of the parent instance (null to create a root instance)
	 * @param instance the instance to add
	 * @throws ApplicationWsException if a problem occurred with the instance management
	 */
	public void addInstance( String applicationName, String parentInstancePath, Instance instance ) throws ApplicationWsException {
		this.logger.finer( "Adding an instance to the application " + applicationName + "..." );

		WebResource path = this.resource.path( UrlConstants.APP ).path( applicationName ).path( "instances" );
		if( parentInstancePath != null )
			path = path.queryParam( "instance-path", parentInstancePath );

		ClientResponse response = this.wsClient.createBuilder( path )
				.accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON )
				.post( ClientResponse.class, instance );

		handleResponse( response );
		this.logger.finer( String.valueOf( response.getStatusInfo()));
	}


	/**
	 * Removes an instance.
	 *
	 * @param applicationName the application name
	 * @param instancePath    the path of the instance to remove
	 */
	public void removeInstance( String applicationName, String instancePath ) {
		this.logger.finer( String.format( "Removing instance \"%s\" from application \"%s\"...",
				instancePath, applicationName ) );

		WebResource path = this.resource
				.path( UrlConstants.APP )
				.path( applicationName )
				.path( "instances" )
				.queryParam( "instance-path", instancePath );

		this.wsClient.createBuilder( path ).delete();
		this.logger.finer( String.format( "Instance \"%s\" has been removed from application \"%s\"",
				instancePath, applicationName ) );
	}


	/**
	 * Resynchronizes all the instances / agents.
	 *
	 * @param applicationName the application name
	 */
	public void resynchronize( String applicationName ) {
		this.logger.finer( String.format( "Resynchronizing application \"%s\"...", applicationName ) );

		WebResource path = this.resource
				.path( UrlConstants.APP )
				.path( applicationName )
				.path( "resynchronize" );

		this.wsClient.createBuilder( path ).post();
		this.logger.finer( String.format( "Application \"%s\" has been resynchronized", applicationName ) );
	}


	/**
	 * Lists all the components from a given application.
	 * @param applicationName the application name
	 * @return a non-null list of components
	 */
	public List<Component> listAllComponents( String applicationName ) {
		this.logger.finer( "Listing components for application " + applicationName + "..." );

		WebResource path = this.resource.path( UrlConstants.APP ).path( applicationName ).path( "components" );
		List<Component> result =
				this.wsClient.createBuilder( path )
				.accept( MediaType.APPLICATION_JSON )
				.get( new GenericType<List<Component>> () {});

		if( result != null ) {
			this.logger.finer( result.size() + " components were found for the application " + applicationName + "." );
		} else {
			this.logger.finer( "No component was found for the application " + applicationName + "." );
			result = new ArrayList<>( 0 );
		}

		return result;
	}


	/**
	 * Finds the component names you could instantiate and deploy on an existing instance.
	 * @param applicationName the application name
	 * @param componentName a component name (null to get root components)
	 * @return a non-null list of component names
	 */
	public List<Component> findComponentChildren( String applicationName, String componentName ) {
		this.logger.finer( "Listing possible children components for component " + componentName + "..." );

		WebResource path = this.resource.path( UrlConstants.APP ).path( applicationName ).path( "components/children" );
		if( componentName != null )
			path = path.queryParam( "component-name", componentName );

		List<Component> result = this.wsClient.createBuilder( path )
					.accept( MediaType.APPLICATION_JSON )
					.get( new GenericType<List<Component>> () {});

		if( result != null ) {
			this.logger.finer( result.size() + " possible children was or were found for " + componentName + "." );
		} else {
			this.logger.finer( "No possible child was found for " + componentName + "." );
			result = new ArrayList<>( 0 );
		}

		return result;
	}


	/**
	 * Finds the instances on which you could deploy a new instance of a component.
	 * @param applicationName the application name
	 * @param componentName a component name
	 * @return a non-null list of instance paths
	 */
	public List<Component> findComponentAncestors( String applicationName, String componentName ) {
		this.logger.finer( "Listing possible parent components for component " + componentName + "..." );

		WebResource path = this.resource.path( UrlConstants.APP ).path( applicationName ).path( "components/ancestors" );
		if( componentName != null )
			path = path.queryParam( "component-name", componentName );

		List<Component> result = this.wsClient.createBuilder( path )
						.accept( MediaType.APPLICATION_JSON )
						.get( new GenericType<List<Component>> () {});

		if( result != null ) {
			this.logger.finer( result.size() + " possible parents was or were found for " + componentName + "." );
		} else {
			this.logger.finer( "No possible parent was found for " + componentName + "." );
			result = new ArrayList<>( 0 );
		}

		return result;
	}


	/**
	 * Binds an application for external exports.
	 * @param applicationName the application name
	 * @param boundTplName the template name (no qualifier as it does not make sense for external exports)
	 * @param boundApp the name of the application (instance of <code>tplName</code>)
	 * @throws ApplicationWsException if something went wrong
	 */
	public void bindApplication( String applicationName, String boundTplName, String boundApp )
	throws ApplicationWsException {

		this.logger.finer( "Creating a binding for external exports in " + applicationName + "..." );

		WebResource path = this.resource.path( UrlConstants.APP )
				.path( applicationName ).path( "bind" )
				.queryParam( "bound-tpl", boundTplName )
				.queryParam( "bound-app", boundApp );

		ClientResponse response = this.wsClient.createBuilder( path ).post( ClientResponse.class );
		handleResponse( response );
	}


	/**
	 * Lists application commands.
	 * @param applicationName an application name
	 * @return a non-null list of command names
	 */
	public List<String> listAllCommands( String applicationName ) {

		this.logger.finer( "Listing commands in " + applicationName + "..." );

		WebResource path = this.resource.path( UrlConstants.APP )
				.path( applicationName ).path( "commands" );

		List<String> result = this.wsClient.createBuilder( path )
					.accept( MediaType.APPLICATION_JSON )
					.get( new GenericType<List<String>> () {});

		if( result != null ) {
			this.logger.finer( result.size() + " command(s) were found for " + applicationName + "." );
		} else {
			this.logger.finer( "No command was found for " + applicationName + "." );
			result = new ArrayList<>( 0 );
		}

		return result;
	}


	private void handleResponse( ClientResponse response ) throws ApplicationWsException {

		if( Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
			String value = response.getEntity( String.class );
			this.logger.finer( response.getStatusInfo() + ": " + value );
			throw new ApplicationWsException( response.getStatusInfo().getStatusCode(), value );
		}
	}
}
