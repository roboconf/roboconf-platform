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

package net.roboconf.dm.server;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.BulkActionException;
import net.roboconf.dm.management.exceptions.ImpossibleInsertionException;
import net.roboconf.dm.management.exceptions.InexistingException;
import net.roboconf.dm.management.exceptions.InvalidActionException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.dm.rest.RestUtils;
import net.roboconf.dm.rest.api.IApplicationWs;

/**
 * @author Vincent Zurczak - Linagora
 */
@Path( IApplicationWs.PATH )
public class ApplicationWs implements IApplicationWs {

	private final Logger logger = Logger.getLogger( ApplicationWs.class.getName());


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.api.IApplicationWs
	 * #perform(java.lang.String, java.lang.String, java.lang.String, boolean)
	 */
	@Override
	public Response perform( String applicationName, String actionAS, String instancePath, boolean applyToAllChildren ) {

		StringBuilder sb = new StringBuilder();
		sb.append( "Request: perform action '" );
		sb.append( actionAS );
		sb.append( "' in " );
		sb.append( applicationName );

		if( instancePath == null && applyToAllChildren ) {
			sb.append( " on all the instances." );

		} else if( instancePath != null ) {
			sb.append( ", instance " + instancePath );
			if( applyToAllChildren )
				sb.append( " and its children" );
			sb.append( "." );

		} else {
			sb.append( ". This request should result in an error." );
		}

		this.logger.fine( sb.toString());
		Response response;
		try {
			// Restore the real instance path
			String realInstancePath = null;
			if( ! Utils.isEmptyOrWhitespaces( instancePath )) {
				realInstancePath = instancePath.substring( IApplicationWs.INSTANCE_PATH_PREFIX.length());
				realInstancePath = RestUtils.fromRestfulPath( realInstancePath );
			}

			// Invoke the manager
			Manager.INSTANCE.perform( applicationName, actionAS, realInstancePath, applyToAllChildren );
			response = Response.ok().build();

		} catch( InexistingException e ) {
			response = Response.status( Status.NOT_FOUND ).entity( e.getMessage()).build();

		} catch( InvalidActionException e ) {
			response = Response.status( Status.BAD_REQUEST ).entity( e.getMessage()).build();

		} catch( UnauthorizedActionException e ) {
			response = Response.status( Status.FORBIDDEN ).entity( e.getMessage()).build();

		} catch( BulkActionException e ) {
			response = Response.status( Status.ACCEPTED ).entity( e.getMessage()).build();
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.api.IApplicationWs
	 * #listAllChildrenInstances(java.lang.String, java.lang.String)
	 */
	@Override
	public List<Instance> listAllChildrenInstances( String applicationName, String instancePath ) {

		// Restore the real instance path
		String realInstancePath = null;
		if( ! Utils.isEmptyOrWhitespaces( instancePath )) {
			realInstancePath = instancePath.substring( IApplicationWs.INSTANCE_PATH_PREFIX.length());
			realInstancePath = RestUtils.fromRestfulPath( realInstancePath );
		}

		// Invoke the manager
		if( realInstancePath == null )
			this.logger.fine( "Request: list all the instances for " + applicationName + "." );
		else
			this.logger.fine( "Request: list all the children instances for " + realInstancePath + " in " + applicationName + "." );

		List<Instance> result = new ArrayList<Instance> ();
		Application app = Manager.INSTANCE.findApplicationByName( applicationName );
		Instance inst = null;

		if( app != null ) {
			if( realInstancePath == null ) {
				result.addAll( InstanceHelpers.getAllInstances( app ));

			} else if(( inst = InstanceHelpers.findInstanceByPath( app, realInstancePath )) != null ) {
				result.addAll( InstanceHelpers.buildHierarchicalList( inst ));
				result.remove( inst );
			}
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.api.IApplicationWs
	 * #listChildrenInstances(java.lang.String, java.lang.String)
	 */
	@Override
	public List<Instance> listChildrenInstances( String applicationName, String instancePath ) {

		// Restore the real instance path
		String realInstancePath = null;
		if( ! Utils.isEmptyOrWhitespaces( instancePath )) {
			realInstancePath = instancePath.substring( IApplicationWs.INSTANCE_PATH_PREFIX.length());
			realInstancePath = RestUtils.fromRestfulPath( realInstancePath );
		}

		// Invoke the manager
		if( realInstancePath == null )
			this.logger.fine( "Request: list root instances for " + applicationName + "." );
		else
			this.logger.fine( "Request: list direct children instances for " + realInstancePath + " in " + applicationName + "." );

		List<Instance> result = new ArrayList<Instance> ();
		Application app = Manager.INSTANCE.findApplicationByName( applicationName );

		Instance inst;
		if( app != null ) {
			if( realInstancePath == null )
				result.addAll( app.getRootInstances());

			else if(( inst = InstanceHelpers.findInstanceByPath( app, realInstancePath )) != null )
				result.addAll( inst.getChildren());
		}

		return result;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IInstanceWs
	 * #addInstance(java.lang.String, java.lang.String, net.roboconf.core.model.runtime.Instance)
	 */
	@Override
	public Response addInstance( String applicationName, String parentInstancePath, Instance instance ) {

		// Restore the real instance path
		String realInstancePath = null;
		if( ! Utils.isEmptyOrWhitespaces( parentInstancePath )) {
			realInstancePath = parentInstancePath.substring( IApplicationWs.INSTANCE_PATH_PREFIX.length());
			realInstancePath = RestUtils.fromRestfulPath( realInstancePath );
		}

		// Invoke the manager
		if( realInstancePath == null )
			this.logger.fine( "Request: add root instance " + instance.getName() + " in " + applicationName + "." );
		else
			this.logger.fine( "Request: add instance " + instance.getName() + " under " + realInstancePath + " in " + applicationName + "." );

		Response response;
		try {
			Manager.INSTANCE.addInstance( applicationName, realInstancePath, instance );
			response = Response.ok().build();

		} catch( InexistingException e ) {
			response = Response.status( Status.NOT_FOUND ).entity( e.getMessage()).build();

		} catch( ImpossibleInsertionException e ) {
			response = Response.status( Status.NOT_ACCEPTABLE ).entity( e.getMessage()).build();
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IGraphWs
	 * #listComponents(java.lang.String)
	 */
	@Override
	public List<Component> listComponents( String applicationName ) {

		this.logger.fine( "Request: list components for the application " + applicationName + "." );
		List<Component> result = new ArrayList<Component> ();
		Application app = Manager.INSTANCE.findApplicationByName( applicationName );
		if( app != null )
			result.addAll( ComponentHelpers.findAllComponents( app ));

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IGraphWs
	 * #findPossibleComponentChildren(java.lang.String, java.lang.String)
	 */
	@Override
	public List<Component> findPossibleComponentChildren( String applicationName, String instancePath ) {

		// Restore the real instance path
		String realInstancePath = null;
		if( ! Utils.isEmptyOrWhitespaces( instancePath )) {
			realInstancePath = instancePath.substring( IApplicationWs.INSTANCE_PATH_PREFIX.length());
			realInstancePath = RestUtils.fromRestfulPath( realInstancePath );
		}

		// Invoke the manager
		if( realInstancePath == null )
			this.logger.fine( "Request: list possible root instances in " + applicationName + "." );
		else
			this.logger.fine( "Request: find components that can be deployed under " + realInstancePath + " in " + applicationName + "." );

		Application app = Manager.INSTANCE.findApplicationByName( applicationName );
		Instance instance = null;
		if( app != null
				&& realInstancePath != null )
			instance = InstanceHelpers.findInstanceByPath( app, realInstancePath );

		List<Component> result = new ArrayList<Component> ();
		if( instance != null )
			result.addAll( instance.getComponent().getChildren());

		else if( app != null
				&& realInstancePath == null )
			result.addAll( app.getGraphs().getRootComponents());

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IGraphWs
	 * #findPossibleParentInstances(java.lang.String, java.lang.String)
	 */
	@Override
	public List<String> findPossibleParentInstances( String applicationName, String componentName ) {

		this.logger.fine( "Request: find instances where a component " + componentName + " could be deployed on, in " + applicationName + "." );
		List<String> result = new ArrayList<String> ();
		Application app = Manager.INSTANCE.findApplicationByName( applicationName );

		// Run through all the instances.
		// See if their component can support a child "of type componentName".
		if( app != null ) {
			for( Instance instance : InstanceHelpers.getAllInstances( app )) {
				for( Component c : instance.getComponent().getChildren()) {
					if( componentName.equals( c.getName())) {
						String instancePath = InstanceHelpers.computeInstancePath( instance );
						result.add( instancePath );
					}
				}
			}
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IGraphWs
	 * #createInstanceFromComponent(java.lang.String, java.lang.String)
	 */
	@Override
	public Instance createInstanceFromComponent( String applicationName, String componentName ) {

		this.logger.fine( "Request: create a new instance for component " + componentName + " in " + applicationName + "." );
		Instance result = null;
		Component comp = null;

		Application app = Manager.INSTANCE.findApplicationByName( applicationName );
		if( app != null )
			comp = ComponentHelpers.findComponent( app.getGraphs(), componentName );

		// TODO: In the real implementation, properties should be set in the exports.
		// The interest of this operation is to display default export values
		// in a web console (as an example) and thus allow to override them if necessary.
		if( comp != null ) {
			result = new Instance( "new-instance" ).component( comp );
		}

		return result;
	}
}
