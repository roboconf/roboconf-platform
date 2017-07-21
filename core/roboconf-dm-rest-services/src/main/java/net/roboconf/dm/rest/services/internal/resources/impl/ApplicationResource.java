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

package net.roboconf.dm.rest.services.internal.resources.impl;

import static net.roboconf.core.errors.ErrorCode.REST_INEXISTING;
import static net.roboconf.core.errors.ErrorCode.REST_MISSING_PROPERTY;
import static net.roboconf.core.errors.ErrorDetails.application;
import static net.roboconf.core.errors.ErrorDetails.component;
import static net.roboconf.core.errors.ErrorDetails.instance;
import static net.roboconf.core.errors.ErrorDetails.name;
import static net.roboconf.core.errors.ErrorDetails.value;
import static net.roboconf.dm.rest.services.internal.utils.RestServicesUtils.handleError;
import static net.roboconf.dm.rest.services.internal.utils.RestServicesUtils.lang;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.ErrorDetails;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.comparators.InstanceComparator;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.model.runtime.CommandHistoryItem;
import net.roboconf.core.model.runtime.TargetWrapperDescriptor;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.CommandException;
import net.roboconf.dm.management.exceptions.ImpossibleInsertionException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.dm.rest.commons.beans.ApplicationBindings;
import net.roboconf.dm.rest.commons.beans.ApplicationBindings.ApplicationBindingItem;
import net.roboconf.dm.rest.commons.beans.TargetAssociation;
import net.roboconf.dm.rest.services.internal.errors.RestError;
import net.roboconf.dm.rest.services.internal.resources.IApplicationResource;
import net.roboconf.dm.rest.services.internal.utils.RestServicesUtils;
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
	 * @param manager the manager
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
		String lang = lang( this.manager );
		try {
			ManagedApplication ma;
			Instance instance;
			if( ! InstanceStatus.isValidState( newState ))
				response = handleError( Status.FORBIDDEN, new RestError( REST_INEXISTING, name( newState )), lang ).build();

			else if(( ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName )) == null )
				response = handleError( Status.NOT_FOUND, new RestError( REST_INEXISTING, application( applicationName )), lang ).build();

			else if(( instance = InstanceHelpers.findInstanceByPath( ma.getApplication(), instancePath )) == null )
				response = handleError( Status.NOT_FOUND, new RestError( REST_INEXISTING, instance( instancePath ), application( applicationName )), lang ).build();

			else
				this.manager.instancesMngr().changeInstanceState( ma, instance, InstanceStatus.whichStatus( newState ));

		} catch( IOException | TargetException e ) {
			response = RestServicesUtils.handleError(
					Status.FORBIDDEN,
					new RestError( ErrorCode.REST_UNDETAILED_ERROR, e ),
					lang ).build();

		} catch( Exception e ) {
			response = RestServicesUtils.handleError(
					Status.INTERNAL_SERVER_ERROR,
					new RestError( ErrorCode.REST_UNDETAILED_ERROR, e ),
					lang ).build();
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
		String lang = lang( this.manager );
		try {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			if( ma == null ) {
				response = handleError(
						Status.NOT_FOUND,
						new RestError( REST_INEXISTING, application( applicationName )),
						lang ).build();

			} else {
				this.manager.applicationMngr().updateApplication( ma, desc );
			}

		} catch( IOException e ) {
			response = RestServicesUtils.handleError(
					Status.FORBIDDEN,
					new RestError( ErrorCode.REST_UNDETAILED_ERROR, e ),
					lang ).build();
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
		String lang = lang( this.manager );
		Response response;
		try {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			Instance instance = null;
			if( ma == null ) {
				response = handleError(
						Status.NOT_FOUND,
						new RestError( REST_INEXISTING, application( applicationName )),
						lang ).build();

			} else if( instancePath != null &&
					(instance = InstanceHelpers.findInstanceByPath( ma.getApplication(), instancePath )) == null ) {
				response = handleError(
						Status.NOT_FOUND,
						new RestError( REST_INEXISTING, instance( instancePath ), application( applicationName )),
						lang ).build();

			} else {
				this.manager.instancesMngr().deployAndStartAll( ma, instance );
				response = Response.ok().build();
			}

		} catch( Exception e ) {
			response = RestServicesUtils.handleError(
					Status.FORBIDDEN,
					new RestError( ErrorCode.REST_UNDETAILED_ERROR, e ),
					lang ).build();
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
		String lang = lang( this.manager );
		Response response;
		try {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			Instance instance = null;
			if( ma == null ) {
				response = handleError(
						Status.NOT_FOUND,
						new RestError( REST_INEXISTING, application( applicationName )),
						lang ).build();

			} else if( instancePath != null &&
					(instance = InstanceHelpers.findInstanceByPath( ma.getApplication(), instancePath )) == null ) {
				response = handleError(
						Status.NOT_FOUND,
						new RestError( REST_INEXISTING, instance( instancePath ), application( applicationName )),
						lang ).build();

			} else {
				this.manager.instancesMngr().stopAll( ma, instance );
				response = Response.ok().build();
			}

		} catch( Exception e ) {
			response = RestServicesUtils.handleError(
					Status.FORBIDDEN,
					new RestError( ErrorCode.REST_UNDETAILED_ERROR, e ),
					lang ).build();
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
		String lang = lang( this.manager );
		Response response;
		try {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			Instance instance = null;
			if( ma == null ) {
				response = handleError(
						Status.NOT_FOUND,
						new RestError( REST_INEXISTING, application( applicationName )),
						lang ).build();

			} else if( instancePath != null &&
					(instance = InstanceHelpers.findInstanceByPath( ma.getApplication(), instancePath )) == null ) {
				response = handleError(
						Status.NOT_FOUND,
						new RestError( REST_INEXISTING, instance( instancePath ), application( applicationName )),
						lang ).build();

			} else {
				this.manager.instancesMngr().undeployAll( ma, instance );
				response = Response.ok().build();
			}

		} catch( Exception e ) {
			response = RestServicesUtils.handleError(
					Status.FORBIDDEN,
					new RestError( ErrorCode.REST_UNDETAILED_ERROR, e ),
					lang ).build();
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

		List<Instance> result = new ArrayList<> ();
		Application app = this.manager.applicationMngr().findApplicationByName( applicationName );

		// Log
		if( instancePath == null )
			this.logger.fine( "Request: list " + (allChildren ? "all" : "root") + " instances for " + applicationName + "." );
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


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IApplicationResource
	 * #bindApplication(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public Response bindApplication( String applicationName, String externalExportPrefix, String boundApp ) {

		this.logger.fine( "Binding " + boundApp  + " to the " + externalExportPrefix + " prefix in application " + applicationName + "." );
		String lang = lang( this.manager );
		Response response;
		try {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			if( ma == null ) {
				response = handleError(
						Status.NOT_FOUND,
						new RestError( REST_INEXISTING, application( applicationName )),
						lang ).build();

			} else {
				this.manager.applicationMngr().bindOrUnbindApplication( ma, externalExportPrefix, boundApp, true );
				response = Response.ok().build();
			}

		} catch( UnauthorizedActionException | IOException e ) {
			response = RestServicesUtils.handleError(
					Status.FORBIDDEN,
					new RestError( ErrorCode.REST_UNDETAILED_ERROR, e ),
					lang ).build();
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IApplicationResource
	 * #unbindApplication(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public Response unbindApplication( String applicationName, String externalExportPrefix, String boundApp ) {

		this.logger.fine( "Unbinding " + boundApp  + " from the " + externalExportPrefix + " prefix in application " + applicationName + "." );
		String lang = lang( this.manager );
		Response response;
		try {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			if( ma == null ) {
				response = handleError(
						Status.NOT_FOUND,
						new RestError( REST_INEXISTING, application( applicationName )),
						lang ).build();

			} else {
				this.manager.applicationMngr().bindOrUnbindApplication( ma, externalExportPrefix, boundApp, false );
				response = Response.ok().build();
			}

		} catch( UnauthorizedActionException | IOException e ) {
			response = RestServicesUtils.handleError(
					Status.FORBIDDEN,
					new RestError( ErrorCode.REST_UNDETAILED_ERROR, e ),
					lang ).build();
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IApplicationResource
	 * #replaceApplicationBindings(java.lang.String, java.lang.String, java.util.List)
	 */
	@Override
	public Response replaceApplicationBindings( String applicationName, String externalExportPrefix, List<String> boundApps ) {

		if( this.logger.isLoggable( Level.FINE )) {
			StringBuilder sb = new StringBuilder();
			sb.append( "Replacing the bindings for the " );
			sb.append( externalExportPrefix );
			sb.append( " prefix with " );
			sb.append( Arrays.toString( boundApps.toArray()));
			sb.append( " in application " );
			sb.append( applicationName );
			sb.append( "." );

			this.logger.fine( sb.toString());
		}

		String lang = lang( this.manager );
		Response response;
		try {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			if( ma == null ) {
				response = RestServicesUtils.handleError(
						Status.NOT_FOUND,
						new RestError( ErrorCode.REST_INEXISTING, application( applicationName )),
						lang ).build();

			} else {
				Set<String> apps = new TreeSet<> ();
				if( boundApps != null )
					apps.addAll( boundApps );

				this.manager.applicationMngr().replaceApplicationBindings( ma, externalExportPrefix, apps );
				response = Response.ok().build();
			}

		} catch( UnauthorizedActionException | IOException e ) {
			response = RestServicesUtils.handleError(
					Status.FORBIDDEN,
					new RestError( ErrorCode.REST_UNDETAILED_ERROR, e ),
					lang ).build();
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IApplicationResource
	 * #getApplicationBindings(java.lang.String)
	 */
	@Override
	public Response getApplicationBindings( String applicationName ) {

		Response response;
		ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
		if( ma == null ) {
			response = handleError(
					Status.NOT_FOUND,
					new RestError( REST_INEXISTING, application( applicationName )),
					lang( this.manager )).build();

		} else {
			ApplicationBindings bindings = new ApplicationBindings();

			// Find all the external prefixes to resolve
			for( Component c : ComponentHelpers.findAllComponents( ma.getApplication())) {
				for( ImportedVariable var : ComponentHelpers.findAllImportedVariables( c ).values()) {
					if( ! var.isExternal())
						continue;

					String prefix = VariableHelpers.parseVariableName( var.getName()).getKey();
					bindings.prefixToItems.put( prefix, new ArrayList<ApplicationBindingItem> ());
				}
			}

			// Find all the applications that match a given prefix
			for( ManagedApplication managedApp : this.manager.applicationMngr().getManagedApplications()) {

				// Any potential bindings with this application?
				// There should be AT MOST 1 matching template for a given prefix.
				// One template means there can be several applications.
				String prefix = managedApp.getApplication().getTemplate().getExternalExportsPrefix();
				if( prefix == null || ! bindings.prefixToItems.containsKey( prefix ))
					continue;

				// There is a potential match. Is there an effective bound?
				Set<String> boundApps = ma.getApplication().getApplicationBindings().get( prefix );
				boolean bound = boundApps != null && boundApps.contains( managedApp.getName());
				ApplicationBindingItem item = new ApplicationBindingItem( managedApp.getName(), bound );
				bindings.prefixToItems.get( prefix ).add( item );
			}

			response = Response.ok().entity( bindings ).build();
		}

		return response;
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

		String lang = lang( this.manager );
		Response response;
		try {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			if( ma == null ) {
				response = handleError(
						Status.NOT_FOUND,
						new RestError( REST_INEXISTING, application( applicationName )),
						lang ).build();

			} else {
				Graphs graphs = ma.getApplication().getTemplate().getGraphs();
				String componentName = null;
				if( instance.getComponent() != null )
					componentName = instance.getComponent().getName();

				// The deserialized instance is not the real one, but just hints
				// (e.g. only the component name is pertinent, and all the exports - included overridden ones - are
				// serialized in the same map...). Now let's make this "fictional" instance real (fix it)!
				Component realComponent;
				if( componentName == null ) {
					response = handleError(
							Status.NOT_FOUND,
							new RestError( REST_MISSING_PROPERTY, value( "component" )),
							lang ).build();

				} else if((realComponent = ComponentHelpers.findComponent( graphs, componentName )) == null ) {
					response = handleError(
							Status.NOT_FOUND,
							new RestError( REST_INEXISTING, component( componentName )),
							lang ).build();

				} else {
					instance.setComponent( realComponent );
					InstanceHelpers.fixOverriddenExports( instance );

					Instance parentInstance = InstanceHelpers.findInstanceByPath( ma.getApplication(), parentInstancePath );
					this.manager.instancesMngr().addInstance( ma, parentInstance, instance );
					response = Response.ok().build();
				}
			}

		} catch( ImpossibleInsertionException | IOException e ) {
			response = RestServicesUtils.handleError(
					Status.FORBIDDEN,
					new RestError( ErrorCode.REST_UNDETAILED_ERROR, e ),
					lang ).build();
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
		String lang = lang( this.manager );
		Response response = Response.ok().build();
		Instance instance;
		try {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			if( ma == null ) {
				response = handleError(
						Status.NOT_FOUND,
						new RestError( REST_INEXISTING, application( applicationName )),
						lang ).build();

			} else if(( instance = InstanceHelpers.findInstanceByPath( ma.getApplication(), instancePath )) == null ) {
				response = handleError(
						Status.NOT_FOUND,
						new RestError( REST_INEXISTING, instance( instancePath ), application( applicationName )),
						lang ).build();

			} else {
				this.manager.instancesMngr().removeInstance( ma, instance );
			}

		} catch( UnauthorizedActionException e ) {
			response = RestServicesUtils.handleError(
					Status.FORBIDDEN,
					new RestError( ErrorCode.REST_UNDETAILED_ERROR, e ),
					lang ).build();

		} catch( IOException e ) {
			response = RestServicesUtils.handleError(
					Status.NOT_ACCEPTABLE,
					new RestError( ErrorCode.REST_UNDETAILED_ERROR, e ),
					lang ).build();
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IApplicationResource
	 * #findTargetAssociations(java.lang.String)
	 */
	@Override
	public List<TargetAssociation> findTargetAssociations( String applicationName ) {

		this.logger.fine( "Request: find target associations in " + applicationName + "." );
		List<TargetAssociation> result = new ArrayList<> ();

		Application app = this.manager.applicationMngr().findApplicationByName( applicationName );
		if( app != null ) {

			// Default target for the application.
			// It must be in first position.
			String defaultTargetId = this.manager.targetsMngr().findTargetId( app, null );
			TargetWrapperDescriptor twd = null;
			if( defaultTargetId != null )
				twd = this.manager.targetsMngr().findTargetById( defaultTargetId );

			result.add( new TargetAssociation( "", null, twd ));

			// Then, show the scoped instances.
			// List them, even if they do not have an associated target.
			for( Instance inst : InstanceHelpers.findAllScopedInstances( app )) {
				String instancePath = InstanceHelpers.computeInstancePath( inst );
				String targetId = this.manager.targetsMngr().findTargetId( app, instancePath, true );

				twd = null;
				if( targetId != null )
					twd = this.manager.targetsMngr().findTargetById( targetId );

				result.add( new TargetAssociation( instancePath, inst.getComponent().getName(), twd ));
			}

			// Deal with components.
			for( Component comp : ComponentHelpers.findAllComponents( app )) {
				if( ! ComponentHelpers.isTarget( comp ))
					continue;

				String key = "@" + comp.getName();
				String targetId = this.manager.targetsMngr().findTargetId( app, key, true );

				twd = null;
				if( targetId != null )
					twd = this.manager.targetsMngr().findTargetById( targetId );

				result.add( new TargetAssociation( key, null, twd ));
			}
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.internal.rest.api.IApplicationWs
	 * #resynchronize(java.lang.String)
	 */
	@Override
	public Response resynchronize( String applicationName ) {

		this.logger.fine( "Request: resynchronize all the agents." );
		String lang = lang( this.manager );
		Response response = Response.ok().build();
		try {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			if( ma == null )
				response = handleError( Status.NOT_FOUND, new RestError( REST_INEXISTING, application( applicationName )), lang ).build();
			else
				this.manager.instancesMngr().resynchronizeAgents( ma );

		} catch( IOException e ) {
			response = RestServicesUtils.handleError(
					Status.NOT_ACCEPTABLE,
					new RestError( ErrorCode.REST_UNDETAILED_ERROR, e ),
					lang ).build();
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
		List<Component> result = new ArrayList<> ();
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

		List<Component> result = new ArrayList<> ();
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
		List<Component> result = new ArrayList<> ();

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


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IApplicationResource
	 * #listCommands(java.lang.String)
	 */
	@Override
	public List<String> listCommands( String app ) {

		this.logger.fine("Request: list all the commands in the " + app + " application.");
		List<String> result;
		Application application = this.manager.applicationMngr().findApplicationByName( app );
		if( application == null )
			result = new ArrayList<>( 0 );
		else
			result = this.manager.commandsMngr().listCommands( application );

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IApplicationResource
	 * #executeCommand(java.lang.String, java.lang.String)
	 */
	@Override
	public Response executeCommand( String app, String commandName ) {

		this.logger.fine("Request: execute command " + commandName + " in the " + app + " application.");
		String lang = lang( this.manager );
		Response response = Response.ok().build();
		try {
			Application application = this.manager.applicationMngr().findApplicationByName( app );
			if( application == null )
				response = handleError( Status.NOT_FOUND, new RestError( REST_INEXISTING, application( app )), lang ).build();
			else
				this.manager.commandsMngr().execute( application, commandName, CommandHistoryItem.ORIGIN_REST_API, null );

		} catch( NoSuchFileException e ) {
			response = RestServicesUtils.handleError(
					Status.NOT_FOUND,
					new RestError( ErrorCode.REST_INEXISTING, e, ErrorDetails.name( commandName )),
					lang ).build();

		} catch( CommandException e ) {
			response = RestServicesUtils.handleError(
					Status.CONFLICT,
					new RestError( ErrorCode.REST_APP_EXEC_ERROR, e, ErrorDetails.name( commandName )),
					lang ).build();

		} catch( Exception e ) {
			response = RestServicesUtils.handleError(
					Status.INTERNAL_SERVER_ERROR,
					new RestError( ErrorCode.REST_UNDETAILED_ERROR, e ),
					lang ).build();
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IApplicationResource
	 * #getCommandInstructions(java.lang.String, java.lang.String)
	 */
	@Override
	public Response getCommandInstructions( String appName, String commandName ) {

		this.logger.fine("Request: get instructions from " + commandName + " in the " + appName + " application.");
		Response response;
		try {
			Application app = this.manager.applicationMngr().findApplicationByName( appName );
			String res;
			if( app == null )
				response = Response.status( Status.NOT_FOUND ).build();
			else if( Utils.isEmptyOrWhitespaces( res = this.manager.commandsMngr().getCommandInstructions( app, commandName )))
				response = Response.status( Status.NO_CONTENT ).build();
			else
				response = Response.ok( res ).build();

		} catch( IOException e ) {
			response = RestServicesUtils.handleError(
					Status.INTERNAL_SERVER_ERROR,
					new RestError( ErrorCode.REST_UNDETAILED_ERROR, e ),
					lang( this.manager )).build();
		}

		return response;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IApplicationResource
	 * #replaceTags(java.lang.String, java.lang.String, java.util.List)
	 */
	@Override
	public Response replaceTags( String name, String version, List<String> tags ) {

		this.logger.fine("Request: replace tags for template " + name + " (version " + version + ").");
		Response response = Response.ok().build();

		ApplicationTemplate tpl = this.manager.applicationTemplateMngr().findTemplate( name, version );
		if( tpl == null )
			response = Response.status( Status.NOT_FOUND ).build();
		else
			tpl.setTags( tags );

		return response;
	}
}
