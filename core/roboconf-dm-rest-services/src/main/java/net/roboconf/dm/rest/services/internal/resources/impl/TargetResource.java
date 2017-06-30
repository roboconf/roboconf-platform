/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

import static net.roboconf.dm.rest.services.internal.utils.RestServicesUtils.handleError;
import static net.roboconf.dm.rest.services.internal.utils.RestServicesUtils.lang;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.sun.jersey.core.header.FormDataContentDisposition;

import net.roboconf.core.Constants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.ErrorDetails;
import net.roboconf.core.errors.RoboconfErrorHelpers;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.TargetValidator;
import net.roboconf.core.model.beans.AbstractApplication;
import net.roboconf.core.model.runtime.TargetUsageItem;
import net.roboconf.core.model.runtime.TargetWrapperDescriptor;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.ITargetsMngr.TargetProperties;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.dm.rest.commons.json.StringWrapper;
import net.roboconf.dm.rest.services.internal.errors.RestError;
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
		this.exceptionclassToErrorCode = new HashMap<>( 2 );
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
			response = handleError(
					status,
					new RestError( ErrorCode.REST_SAVE_ERROR, e, ErrorDetails.name( targetId )),
					lang( this.manager )).build();
		}

		return response;
	}


	@Override
	public Response loadTargetArchive( InputStream uploadedInputStream, FormDataContentDisposition fileDetail ) {

		this.logger.fine( "Request: load targets from an uploaded ZIP file (" + fileDetail.getFileName() + ")." );
		File tempZipFile = new File( System.getProperty( "java.io.tmpdir" ), fileDetail.getFileName());
		final Set<String> newTargetIds = new HashSet<> ();

		File dir = null;
		Response response;
		try {
			// Copy the uploaded ZIP file on the disk
			Utils.copyStream( uploadedInputStream, tempZipFile );

			// Extract the ZIP content
			String projectName = fileDetail.getFileName().replace( ".zip", "" );
			dir = new File( System.getProperty( "java.io.tmpdir" ), "roboconf/" + projectName );
			Utils.extractZipArchive( tempZipFile, dir );

			// Validate the content
			List<ModelError> errors = TargetValidator.parseDirectory( dir );
			if( RoboconfErrorHelpers.containsCriticalErrors( errors )) {
				response = handleError(
						Status.FORBIDDEN,
						new RestError( ErrorCode.REST_TARGET_CONTAINS_ERROR ),
						lang( this.manager )).build();
			}

			// Register the properties
			else {
				response = Response.ok().build();
				for( File f : Utils.listAllFiles( dir, Constants.FILE_EXT_PROPERTIES )) {
					String id = this.manager.targetsMngr().createTarget( f, null );
					newTargetIds.add( id );
				}
			}

		} catch( Exception e ) {
			response = handleError(
					Status.NOT_ACCEPTABLE,
					new RestError( ErrorCode.REST_IO_ERROR, e ),
					lang( this.manager )).build();

			// Unregister the targets
			this.logger.fine( "An error occurred while deploying targets. Unregistering those that were in the same archive." );
			for( String id : newTargetIds ) {
				try {
					this.manager.targetsMngr().deleteTarget( id );

				} catch( Exception e1 ) {
					Utils.logException( this.logger, e1 );
					this.logger.severe( "Target " + id + " could not be deleted." );
				}
			}

		} finally {
			Utils.closeQuietly( uploadedInputStream );

			// We do not need the extracted application anymore.
			// In case of success, it was copied in the DM's configuration.
			Utils.deleteFilesRecursivelyAndQuietly( dir );
			Utils.deleteFilesRecursivelyAndQuietly( tempZipFile );
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
			response = handleError(
					status,
					new RestError( ErrorCode.REST_DELETION_ERROR, e, ErrorDetails.name( targetId )),
					lang( this.manager )).build();
		}

		return response;
	}


	@Override
	public Response getTargetProperties( String targetId ) {

		this.logger.fine( "Request: get properties for target " + targetId + "." );
		TargetProperties props = this.manager.targetsMngr().findTargetProperties( targetId );
		Response response;
		if( props.getSourceFile() == null )
			response = Response.status( Status.NOT_FOUND ).build();
		else
			response = Response.ok().entity( new StringWrapper( props.asString())).build();

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
	public Response associateTarget(
			String name,
			String qualifier,
			String instancePathOrComponentName,
			String targetId,
			boolean bind ) {

		if( bind )
			this.logger.fine( "Request: associate " + instancePathOrComponentName + " with target " + targetId + "." );
		else
			this.logger.fine( "Request: dissociate " + instancePathOrComponentName + " from its target." );

		Response response = Response.ok().build();
		AbstractApplication app = findAbstractApplication( name, qualifier );
		try {
			if( app == null )
				response = Response.status( Status.BAD_REQUEST ).build();
			else if( bind )
				this.manager.targetsMngr().associateTargetWith( targetId, app, instancePathOrComponentName );
			else
				this.manager.targetsMngr().dissociateTargetFrom( app, instancePathOrComponentName );

		} catch( IOException | UnauthorizedActionException e ) {
			Status status = this.exceptionclassToErrorCode.get( e.getClass());
			response = handleError(
					status,
					new RestError( ErrorCode.REST_TARGET_ASSOCIATION_ERROR, e ),
					lang( this.manager )).build();
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
			response = handleError(
					Status.INTERNAL_SERVER_ERROR,
					new RestError( ErrorCode.REST_TARGET_HINT_ERROR, e ),
					lang( this.manager )).build();
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
