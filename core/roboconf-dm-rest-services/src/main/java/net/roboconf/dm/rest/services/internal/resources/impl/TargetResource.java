/**
 * Copyright 2015-2016 Linagora, Université Joseph Fourier, Floralis
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.roboconf.core.model.beans.AbstractApplication;
import net.roboconf.core.model.runtime.TargetUsageItem;
import net.roboconf.core.model.runtime.TargetWrapperDescriptor;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.dm.rest.commons.json.StringWrapper;
import net.roboconf.dm.rest.services.internal.RestServicesUtils;
import net.roboconf.dm.rest.services.internal.resources.ITargetResource;

/**
 * @author Vincent Zurczak - Linagora
 */
@Path( ITargetResource.PATH )
public class TargetResource implements ITargetResource {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final Manager manager;
	private final Map<Class<?>,Status> exceptionclassToErrorCode;


	/**
	 * Constructor.
	 * @param manager the manager
	 */
	public TargetResource( Manager manager ) {
		this.manager = manager;

		// This class allows to have several exception blocks.
		// It is more efficient for code coverage and guarantees a coherent
		// handling of exceptions.
		this.exceptionclassToErrorCode = new HashMap<Class<?>,Status>( 2 );
		this.exceptionclassToErrorCode.put( IOException.class, Status.INTERNAL_SERVER_ERROR );
		this.exceptionclassToErrorCode.put( UnauthorizedActionException.class, Status.FORBIDDEN );
	}


	@Override
	public List<TargetWrapperDescriptor> listTargets( String name, String qualifier ) {

		StringBuilder sb = new StringBuilder( "Request: list targets." );
		if( name != null ) {
			sb.append( " Filter names: " + name );
			if( qualifier != null )
				sb.append( ", " + qualifier );
		}

		this.logger.fine( sb.toString());
		AbstractApplication app = findAbstractApplication( name, qualifier );

		List<TargetWrapperDescriptor> result;
		if( app == null )
			result = this.manager.targetsMngr().listAllTargets();
		else
			result = this.manager.targetsMngr().listPossibleTargets( app );

		return result;
	}


	@Override
	public Response createOrUpdateTarget( String rawProperties, String targetId ) {

		if( targetId == null )
			this.logger.fine( "Request: create a new target." );
		else
			this.logger.fine( "Request: update target " + targetId + "." );

		Response response;
		String id = targetId;
		try {
			if( targetId == null )
				id = this.manager.targetsMngr().createTarget( rawProperties );
			else
				this.manager.targetsMngr().updateTarget( targetId, rawProperties );

			response = Response.ok().entity( id ).build();

		} catch( IOException | UnauthorizedActionException e ) {
			Status status = this.exceptionclassToErrorCode.get( e.getClass());
			response = RestServicesUtils.handleException( this.logger, status, null, e ).build();
		}

		return response;
	}


	@Override
	public Response deleteTarget( String targetId ) {

		this.logger.fine( "Request: delete target " + targetId + "." );
		Response response = Response.ok().build();
		try {
			this.manager.targetsMngr().deleteTarget( targetId );

		} catch( IOException | UnauthorizedActionException e ) {
			Status status = this.exceptionclassToErrorCode.get( e.getClass());
			response = RestServicesUtils.handleException( this.logger, status, null, e ).build();
		}

		return response;
	}


	@Override
	public Response getTargetProperties( String targetId ) {

		this.logger.fine( "Request: get properties for target " + targetId + "." );
		String content = this.manager.targetsMngr().findRawTargetProperties( targetId );
		Response response;
		if( content == null )
			response = Response.status( Status.NOT_FOUND ).build();
		else
			response = Response.ok().entity( new StringWrapper( content )).build();

		return response;
	}


	@Override
	public Response findTargetById( String targetId ) {

		this.logger.fine( "Request: get details about target " + targetId + "." );
		TargetWrapperDescriptor twb = this.manager.targetsMngr().findTargetById( targetId );
		Response response;
		if( twb == null )
			response = Response.status( Status.NOT_FOUND ).build();
		else
			response = Response.ok().entity( twb ).build();

		return response;
	}


	@Override
	public Response associateTarget( String name, String qualifier, String instancePath, String targetId, boolean bind ) {

		if( bind )
			this.logger.fine( "Request: associate instance " + instancePath + " with target " + targetId + "." );
		else
			this.logger.fine( "Request: dissociate instance " + instancePath + " with target " + targetId + "." );

		Response response = Response.ok().build();
		AbstractApplication app = findAbstractApplication( name, qualifier );
		try {
			if( app == null )
				response = Response.status( Status.BAD_REQUEST ).build();
			else if( bind )
				this.manager.targetsMngr().associateTargetWithScopedInstance( targetId, app, instancePath );
			else
				this.manager.targetsMngr().dissociateTargetFromScopedInstance( app, instancePath );

		} catch( IOException | UnauthorizedActionException e ) {
			Status status = this.exceptionclassToErrorCode.get( e.getClass());
			response = RestServicesUtils.handleException( this.logger, status, null, e ).build();
		}

		return response;
	}


	@Override
	public Response updateHint( String name, String qualifier, String targetId, boolean bind ) {

		String id = name;
		if( qualifier != null )
			id += " - " + qualifier;

		if( bind )
			this.logger.fine( "Request: add a hint between " + id + " and target " + targetId + "." );
		else
			this.logger.fine( "Request: remove a hint between " + id + " and target " + targetId + "." );

		Response response = Response.ok().build();
		AbstractApplication app = findAbstractApplication( name, qualifier );
		try {
			if( app == null )
				response = Response.status( Status.BAD_REQUEST ).build();
			else if( bind )
				this.manager.targetsMngr().addHint( targetId, app );
			else
				this.manager.targetsMngr().removeHint( targetId, app );

		} catch( IOException e ) {
			response = RestServicesUtils.handleException( this.logger, Status.INTERNAL_SERVER_ERROR, null, e ).build();
		}

		return response;
	}


	@Override
	public List<TargetUsageItem> findUsageStatistics( String targetId ) {

		this.logger.fine( "Request: list usage statistics for target " + targetId + "." );
		return this.manager.targetsMngr().findUsageStatistics( targetId );
	}


	private AbstractApplication findAbstractApplication( String name, String qualifier ) {

		AbstractApplication app = null;
		if( name != null ) {
			if( qualifier != null )
				app = this.manager.applicationTemplateMngr().findTemplate( name, qualifier );
			else
				app = this.manager.applicationMngr().findApplicationByName( name );
		}

		return app;
	}
}
