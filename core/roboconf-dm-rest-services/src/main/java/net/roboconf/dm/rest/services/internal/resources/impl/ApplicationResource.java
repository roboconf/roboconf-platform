/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.rest.services.internal.resources.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.comparators.InstanceComparator;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.ImpossibleInsertionException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.dm.rest.services.internal.RestServicesUtils;
import net.roboconf.dm.rest.services.internal.resources.IApplicationResource;
import net.roboconf.target.api.TargetException;

/**
 * @author Vincent Zurczak - Linagora
 */
@Path( IApplicationResource.PATH )
public class ApplicationResource implements IApplicationResource {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final Manager manager;


	/**
	 * Constructor.
	 * @param manager
	 */
	public ApplicationResource( Manager manager ) {
		this.manager = manager;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.internal.rest.api.IApplicationWs
	 * #changeInstanceState(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public Response changeInstanceState( String applicationName, String newState, String instancePath ) {

		this.logger.fine( "Request: change state of " + instancePath + " to '" + newState + "' in " + applicationName + "." );
		Response response = Response.ok().build();
		try {
			ManagedApplication ma;
			Instance instance;
			if( ! InstanceStatus.isValidState( newState ))
				response = Response.status( Status.FORBIDDEN ).entity( "Status '" + newState + "' does not exist." ).build();

			else if(( ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName )) == null )
				response = Response.status( Status.NOT_FOUND ).entity( "Application " + applicationName + " does not exist." ).build();

			else if(( instance = InstanceHelpers.findInstanceByPath( ma.getApplication(), instancePath )) == null )
				response = Response.status( Status.NOT_FOUND ).entity( "Instance " + instancePath + " was not found." ).build();

			else
				this.manager.instancesMngr().changeInstanceState( ma, instance, InstanceStatus.whichStatus( newState ));

		} catch( IOException e ) {
			response = RestServicesUtils.handleException( this.logger, Status.FORBIDDEN, null, e ).build();

		} catch( TargetException e ) {
			response = RestServicesUtils.handleException( this.logger, Status.FORBIDDEN, null, e ).build();

		} catch( Exception e ) {
			response = RestServicesUtils.handleException( this.logger, Status.INTERNAL_SERVER_ERROR, null, e ).build();
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IApplicationResource
	 * #setDescription(java.lang.String, java.lang.String)
	 */
	@Override
	public Response setDescription( String applicationName, String desc ) {

		this.logger.fine( "Request: change the description of " + applicationName + "." );
		Response response = Response.ok().build();
		try {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			if( ma == null )
				response = Response.status( Status.NOT_FOUND ).entity( "Application " + applicationName + " does not exist." ).build();
			else
				this.manager.applicationMngr().updateApplication( ma, desc );

		} catch( IOException e ) {
			response = RestServicesUtils.handleException( this.logger, Status.FORBIDDEN, null, e ).build();
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.internal.rest.api.IApplicationWs
	 * #deployAndStartAll(java.lang.String, java.lang.String)
	 */
	@Override
	public Response deployAndStartAll( String applicationName, String instancePath ) {

		this.logger.fine( "Request: deploy and start instances in " + applicationName + ", from instance = " + instancePath + "." );
		Response response;
		try {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			if( ma == null ) {
				response = Response.status( Status.NOT_FOUND ).entity( "Application " + applicationName + " does not exist." ).build();
			} else {
				Instance instance = InstanceHelpers.findInstanceByPath( ma.getApplication(), instancePath );
				this.manager.instancesMngr().deployAndStartAll( ma, instance );
				response = Response.ok().build();
			}

		} catch( Exception e ) {
			response = RestServicesUtils.handleException( this.logger, Status.FORBIDDEN, null, e ).build();
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.internal.rest.api.IApplicationWs
	 * #stopAll(java.lang.String, java.lang.String)
	 */
	@Override
	public Response stopAll( String applicationName, String instancePath ) {

		this.logger.fine( "Request: stop instances in " + applicationName + ", from instance = " + instancePath + "." );
		Response response;
		try {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			if( ma == null ) {
				response = Response.status( Status.NOT_FOUND ).entity( "Application " + applicationName + " does not exist." ).build();
			} else {
				Instance instance = InstanceHelpers.findInstanceByPath( ma.getApplication(), instancePath );
				this.manager.instancesMngr().stopAll( ma, instance );
				response = Response.ok().build();
			}

		} catch( Exception e ) {
			response = RestServicesUtils.handleException( this.logger, Status.FORBIDDEN, null, e ).build();
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.internal.rest.api.IApplicationWs
	 * #undeployAll(java.lang.String, java.lang.String)
	 */
	@Override
	public Response undeployAll( String applicationName, String instancePath ) {

		this.logger.fine( "Request: deploy and start instances in " + applicationName + ", from instance = " + instancePath + "." );
		Response response;
		try {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			if( ma == null ) {
				response = Response.status( Status.NOT_FOUND ).entity( "Application " + applicationName + " does not exist." ).build();
			} else {
				Instance instance = InstanceHelpers.findInstanceByPath( ma.getApplication(), instancePath );
				this.manager.instancesMngr().undeployAll( ma, instance );
				response = Response.ok().build();
			}

		} catch( Exception e ) {
			response = RestServicesUtils.handleException( this.logger, Status.FORBIDDEN, null, e ).build();
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.internal.rest.api.IApplicationWs
	 * #listChildrenInstances(java.lang.String, java.lang.String, boolean)
	 */
	@Override
	public List<Instance> listChildrenInstances( String applicationName, String instancePath, boolean allChildren ) {

		List<Instance> result = new ArrayList<Instance> ();
		Application app = this.manager.applicationMngr().findApplicationByName( applicationName );

		// Log
		if( instancePath == null )
			this.logger.finer( "Request: list " + (allChildren ? "all" : "root") + " instances for " + applicationName + "." );
		else
			this.logger.fine( "Request: list " + (allChildren ? "all" : "direct") + " children instances for " + instancePath + " in " + applicationName + "." );

		// Find the instances
		Instance inst;
		if( app != null ) {
			if( instancePath == null ) {
				if( allChildren )
					result.addAll( InstanceHelpers.getAllInstances( app ));
				else
					result.addAll( app.getRootInstances());
			}

			else if(( inst = InstanceHelpers.findInstanceByPath( app, instancePath )) != null ) {
				if( allChildren ) {
					result.addAll( InstanceHelpers.buildHierarchicalList( inst ));
					result.remove( inst );
				} else {
					result.addAll( inst.getChildren());
				}
			}
		}

		// Bug #64: sort instance paths for the clients
		Collections.sort( result, new InstanceComparator());
		return result;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.internal.rest.client.exceptions.server.IInstanceWs
	 * #addInstance(java.lang.String, java.lang.String, net.roboconf.core.model.beans.Instance)
	 */
	@Override
	public Response addInstance( String applicationName, String parentInstancePath, Instance instance ) {

		if( parentInstancePath == null )
			this.logger.fine( "Request: add root instance " + instance.getName() + " in " + applicationName + "." );
		else
			this.logger.fine( "Request: add instance " + instance.getName() + " under " + parentInstancePath + " in " + applicationName + "." );

		Response response;
		try {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			if( ma == null ) {
				response = Response.status( Status.NOT_FOUND ).entity( "Application " + applicationName + " does not exist." ).build();

			} else {
				Graphs graphs = ma.getApplication().getTemplate().getGraphs();
				String componentName = null;
				if( instance.getComponent() != null )
					componentName = instance.getComponent().getName();

				// The deserialized instance is not the real one, but just hints
				// (eg. only the component name is pertinent, and all the exports - included overridden ones - are
				// serialized in the same map...)
				// Now let's make this "fictive" instance real (fix it) !
				Component realComponent;
				if( componentName == null ) {
					response = Response.status( Status.NOT_FOUND ).entity( "No component was specified for the instance." ).build();

				} else if((realComponent = ComponentHelpers.findComponent( graphs, componentName )) == null ) {
					response = Response.status( Status.NOT_FOUND ).entity( "Component " + componentName + " does not exist." ).build();

				} else {
					instance.setComponent( realComponent );
					InstanceHelpers.fixOverriddenExports( instance );

					Instance parentInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), parentInstancePath );
					this.manager.instancesMngr().addInstance( ma, parentInstance, instance );
					response = Response.ok().build();
				}
			}

		} catch( ImpossibleInsertionException e ) {
			response = RestServicesUtils.handleException( this.logger, Status.FORBIDDEN, null, e ).build();

		} catch( IOException e ) {
			response = RestServicesUtils.handleException( this.logger, Status.FORBIDDEN, null, e ).build();
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.internal.rest.api.IApplicationWs
	 * #removeInstance(java.lang.String, java.lang.String)
	 */
	@Override
	public Response removeInstance( String applicationName, String instancePath ) {

		this.logger.fine( "Request: remove " + instancePath + " in " + applicationName + "." );
		Response response = Response.ok().build();
		Instance instance;
		try {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			if( ma == null )
				response = Response.status( Status.NOT_FOUND ).entity( "Application " + applicationName + " does not exist." ).build();

			else if(( instance = InstanceHelpers.findInstanceByPath( ma.getApplication(), instancePath )) == null )
				response = Response.status( Status.NOT_FOUND ).entity( "Instance " + instancePath + " was not found." ).build();

			else
				this.manager.instancesMngr().removeInstance( ma, instance );

		} catch( UnauthorizedActionException e ) {
			response = RestServicesUtils.handleException( this.logger, Status.FORBIDDEN, null, e ).build();

		} catch( IOException e ) {
			response = RestServicesUtils.handleException( this.logger, Status.NOT_ACCEPTABLE, null, e ).build();
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.internal.rest.api.IApplicationWs
	 * #resynchronize(java.lang.String)
	 */
	@Override
	public Response resynchronize( String applicationName ) {

		this.logger.fine( "Request: resynchronize all the agents." );
		Response response = Response.ok().build();
		try {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			if( ma == null )
				response = Response.status( Status.NOT_FOUND ).entity( "Application " + applicationName + " does not exist." ).build();
			else
				this.manager.instancesMngr().resynchronizeAgents( ma );

		} catch( IOException e ) {
			response = RestServicesUtils.handleException( this.logger, Status.NOT_ACCEPTABLE, null, e ).build();
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.internal.rest.client.exceptions.server.IGraphWs
	 * #listComponents(java.lang.String)
	 */
	@Override
	public List<Component> listComponents( String applicationName ) {

		this.logger.fine( "Request: list components for the application " + applicationName + "." );
		List<Component> result = new ArrayList<Component> ();
		Application app = this.manager.applicationMngr().findApplicationByName( applicationName );
		if( app != null )
			result.addAll( ComponentHelpers.findAllComponents( app ));

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IApplicationResource
	 * #findComponentChildren(java.lang.String, java.lang.String)
	 */
	@Override
	public List<Component> findComponentChildren( String applicationName, String componentName ) {

		if( componentName == null )
			this.logger.fine( "Request: list root components in " + applicationName + "." );
		else
			this.logger.fine( "Request: find components that can be deployed under a " + componentName + " component in " + applicationName + "." );

		List<Component> result = new ArrayList<Component> ();
		Application app = this.manager.applicationMngr().findApplicationByName( applicationName );
		if( app != null ) {
			Component comp;
			if( componentName == null )
				result.addAll( app.getTemplate().getGraphs().getRootComponents());
			else if(( comp = ComponentHelpers.findComponent( app, componentName )) != null )
				result.addAll( ComponentHelpers.findAllChildren( comp ));
			else
				this.logger.fine( "No component called " + componentName + " was found in " + applicationName + "." );
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IApplicationResource
	 * #findComponentAncestors(java.lang.String, java.lang.String)
	 */
	@Override
	public List<Component> findComponentAncestors( String applicationName, String componentName ) {

		this.logger.fine( "Request: find components where a " + componentName + " component could be deployed on, in " + applicationName + "." );
		List<Component> result = new ArrayList<Component> ();

		Application app = this.manager.applicationMngr().findApplicationByName( applicationName );
		if( app != null ) {
			Component comp = ComponentHelpers.findComponent( app.getTemplate().getGraphs(), componentName );
			if( comp != null )
				result.addAll( ComponentHelpers.findAllAncestors( comp ));
			else
				this.logger.fine( "No component called " + componentName + " was found in " + applicationName + "." );
		}

		return result;
	}
}
