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

import static net.roboconf.core.errors.ErrorCode.REST_DELETION_ERROR;
import static net.roboconf.core.errors.ErrorCode.REST_INEXISTING;
import static net.roboconf.core.errors.ErrorCode.REST_IO_ERROR;
import static net.roboconf.core.errors.ErrorCode.REST_MNGMT_APP_SHUTDOWN_ERROR;
import static net.roboconf.core.errors.ErrorCode.REST_MNGMT_CONFLICT;
import static net.roboconf.core.errors.ErrorCode.REST_MNGMT_INVALID_IMAGE;
import static net.roboconf.core.errors.ErrorCode.REST_MNGMT_INVALID_TPL;
import static net.roboconf.core.errors.ErrorCode.REST_MNGMT_INVALID_URL;
import static net.roboconf.core.errors.ErrorCode.REST_MNGMT_ZIP_ERROR;
import static net.roboconf.core.errors.ErrorCode.REST_SAVE_ERROR;
import static net.roboconf.core.errors.ErrorDetails.application;
import static net.roboconf.core.errors.ErrorDetails.applicationTpl;
import static net.roboconf.core.errors.ErrorDetails.name;
import static net.roboconf.core.errors.ErrorDetails.value;
import static net.roboconf.dm.rest.services.internal.utils.RestServicesUtils.handleError;
import static net.roboconf.dm.rest.services.internal.utils.RestServicesUtils.lang;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.ops4j.pax.url.mvn.MavenResolver;

import com.sun.jersey.core.header.FormDataContentDisposition;

import net.roboconf.core.Constants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.urlresolvers.DefaultUrlResolver;
import net.roboconf.core.urlresolvers.IUrlResolver;
import net.roboconf.core.urlresolvers.IUrlResolver.ResolvedFile;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.dm.rest.services.internal.errors.RestError;
import net.roboconf.dm.rest.services.internal.resources.IManagementResource;
import net.roboconf.dm.rest.services.internal.utils.MavenUrlResolver;
import net.roboconf.dm.rest.services.internal.utils.RestServicesUtils;

/**
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
@Path( IManagementResource.PATH )
public class ManagementResource implements IManagementResource {

	private static final Set<String> SUPPORTED_EXTENSIONS;
	static {
		final Set<String> ex = new HashSet<>();
		ex.add("jpg");
		ex.add("jpeg");
		ex.add("gif");
		ex.add("png");
		ex.add("svg");
		SUPPORTED_EXTENSIONS = Collections.unmodifiableSet(ex);
	}

	/**
	 * The maximum allowed image file size, in bytes (1MB).
	 */
	public static final long MAX_IMAGE_SIZE = 1024 * 1024;

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final Manager manager;
	private MavenResolver mavenResolver;


	/**
	 * Constructor.
	 * @param manager the manager
	 */
	public ManagementResource( Manager manager ) {
		this.manager = manager;
	}


	/**
	 * @param mavenResolver the mavenResolver to set
	 */
	public void setMavenResolver( MavenResolver mavenResolver ) {
		this.mavenResolver = mavenResolver;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IManagementResource
	 * #loadUploadedZippedApplicationTemplate(java.io.InputStream, com.sun.jersey.core.header.FormDataContentDisposition)
	 */
	@Override
	public Response loadUploadedZippedApplicationTemplate( InputStream uploadedInputStream, FormDataContentDisposition fileDetail ) {

		this.logger.fine( "Request: load application from an uploaded ZIP file (" + fileDetail.getFileName() + ")." );
		File tempZipFile = new File( System.getProperty( "java.io.tmpdir" ), fileDetail.getFileName());
		Response response;
		try {
			// Copy the uploaded ZIP file on the disk
			Utils.copyStream( uploadedInputStream, tempZipFile );

			// Load the application
			response = loadZippedApplicationTemplate( tempZipFile.toURI().toURL().toString());

		} catch( IOException e ) {
			response = handleError(
					Status.NOT_ACCEPTABLE,
					new RestError( REST_MNGMT_ZIP_ERROR, e ),
					lang( this.manager )).build();

		} finally {
			Utils.closeQuietly( uploadedInputStream );

			// We do not need the uploaded file anymore.
			// In case of success, it was copied in the DM's configuration.
			Utils.deleteFilesRecursivelyAndQuietly( tempZipFile );
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IManagementResource
	 * #loadZippedApplicationTemplate(java.lang.String)
	 */
	@Override
	public Response loadZippedApplicationTemplate( String url ) {

		this.logger.fine( "Request: load application from URL " + url + "." );
		ResolvedFile resolvedFile = null;
		File extractionDir = null;
		Response response;

		// Retrieve the file as a local one
		if( Utils.isEmptyOrWhitespaces( url )) {
			response = handleError(
					Status.NOT_ACCEPTABLE,
					new RestError( REST_MNGMT_INVALID_URL, value( url )),
					lang( this.manager )).build();

		} else try {
			// Get a resolver...
			IUrlResolver resolver = this.mavenResolver != null ? new MavenUrlResolver( this.mavenResolver ) : new DefaultUrlResolver();
			resolvedFile = resolver.resolve( url );

			// Extract the ZIP content
			extractionDir = new File( System.getProperty( "java.io.tmpdir" ), "roboconf/" + UUID.randomUUID().toString());
			Utils.extractZipArchive( resolvedFile.getFile(), extractionDir );

			// Load the application template
			response = loadUnzippedApplicationTemplate( extractionDir.getAbsolutePath());

		} catch( IOException e ) {
			response = handleError(
					Status.UNAUTHORIZED,
					new RestError( REST_SAVE_ERROR, e ),
					lang( this.manager )).build();

		} finally {
			// We do not need the extracted application anymore.
			// In case of success, it was copied in the DM's configuration.
			Utils.deleteFilesRecursivelyAndQuietly( extractionDir );

			// If the resolved file did not exist before, delete it as we do not need it anymore
			if( resolvedFile != null && ! resolvedFile.existedBefore())
				Utils.deleteFilesRecursivelyAndQuietly( resolvedFile.getFile());
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IManagementResource
	 * #loadUnzippedApplicationTemplate(java.lang.String)
	 */
	@Override
	public Response loadUnzippedApplicationTemplate( String localFilePath ) {

		if( localFilePath == null )
			localFilePath = "null";

		this.logger.fine( "Request: load application from a local file (" + localFilePath + ")." );
		Response response;
		try {
			ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( new File( localFilePath ));
			response = Response.ok().entity( tpl ).build();

		} catch( AlreadyExistingException e ) {
			response = handleError(
					Status.FORBIDDEN,
					new RestError( REST_MNGMT_CONFLICT, e ),
					lang( this.manager )).build();

		} catch( InvalidApplicationException e ) {
			response = handleError(
					Status.NOT_ACCEPTABLE,
					new RestError( REST_MNGMT_INVALID_TPL, e ),
					lang( this.manager )).build();

		} catch( IOException e ) {
			response = handleError(
					Status.UNAUTHORIZED,
					new RestError( REST_SAVE_ERROR, e ),
					lang( this.manager )).build();

		} catch( UnauthorizedActionException e ) {
			response = handleError(
					Status.CONFLICT,
					new RestError( REST_MNGMT_CONFLICT, e ),
					lang( this.manager )).build();
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IManagementResource
	 * #listApplicationTemplates(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public List<ApplicationTemplate> listApplicationTemplates( String exactName, String exactQualifier, String tag ) {

		// Log
		if( this.logger.isLoggable( Level.FINE )) {
			if( exactName == null && exactQualifier == null ) {
				this.logger.fine( "Request: list all the application templates." );

			} else {
				StringBuilder sb = new StringBuilder( "Request: list/find the application templates" );
				if( exactName != null ) {
					sb.append( " with name = " );
					sb.append( exactName );
				}

				if( exactQualifier != null ) {
					if( exactName != null )
						sb.append( " and" );

					sb.append( " qualifier = " );
					sb.append( exactQualifier );
				}

				sb.append( "." );
				this.logger.fine( sb.toString());
			}
		}

		// Search
		List<ApplicationTemplate> result = new ArrayList<> ();
		for( ApplicationTemplate tpl : this.manager.applicationTemplateMngr().getApplicationTemplates()) {
			// Equality is on the name, not on the display name
			if(( exactName == null || exactName.equals( tpl.getName()))
					&& (exactQualifier == null || exactQualifier.equals( tpl.getVersion())
					&& (tag == null || tpl.getTags().contains( tag ))))
				result.add( tpl );
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.internal.rest.client.exceptions.server.IApplicationWs
	 * #listApplicationTemplates()
	 */
	@Override
	public List<ApplicationTemplate> listApplicationTemplates() {
		return listApplicationTemplates( null, null, null );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IManagementResource
	 * #deleteApplicationTemplate(java.lang.String, java.lang.String)
	 */
	@Override
	public Response deleteApplicationTemplate( String tplName, String tplVersion ) {

		this.logger.fine( "Request: delete application template " + tplName + " (version = " + tplVersion + ")." );
		Response result = Response.ok().build();
		try {
			this.manager.applicationTemplateMngr().deleteApplicationTemplate( tplName, tplVersion );

		} catch( InvalidApplicationException e ) {
			result = handleError(
					Status.NOT_FOUND,
					new RestError( ErrorCode.REST_INEXISTING, e, applicationTpl( tplName, tplVersion )),
					lang( this.manager )).build();

		} catch( UnauthorizedActionException | IOException e ) {
			result = RestServicesUtils.handleError(
					Status.FORBIDDEN,
					new RestError( ErrorCode.REST_DELETION_ERROR, e, applicationTpl( tplName, tplVersion )),
					lang( this.manager )).build();
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IManagementResource
	 * #createApplication(net.roboconf.core.model.beans.Application)
	 */
	@Override
	public Response createApplication( Application app ) {

		this.logger.fine( "Request: create application " + app + "." );
		Response result;
		try {
			String tplName = app.getTemplate() == null ? null : app.getTemplate().getName();
			String tplQualifier = app.getTemplate() == null ? null : app.getTemplate().getVersion();
			String appName = app.getDisplayName() != null ? app.getDisplayName() : app.getName();
			ManagedApplication ma = this.manager.applicationMngr().createApplication( appName, app.getDescription(), tplName, tplQualifier );
			result = Response.ok().entity( ma.getApplication()).build();

		} catch( InvalidApplicationException e ) {
			result = handleError(
					Status.NOT_FOUND,
					new RestError( REST_MNGMT_INVALID_TPL, e, application( app )),
					lang( this.manager )).build();

		} catch( AlreadyExistingException e ) {
			result = handleError(
					Status.FORBIDDEN,
					new RestError( REST_MNGMT_CONFLICT, e, application( app )),
					lang( this.manager )).build();

		} catch( IOException e ) {
			result = handleError(
					Status.UNAUTHORIZED,
					new RestError( REST_SAVE_ERROR, e, application( app )),
					lang( this.manager )).build();
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IManagementResource
	 * #listApplications(java.lang.String)
	 */
	@Override
	public List<Application> listApplications( String exactName ) {

		if( exactName != null )
			this.logger.fine( "Request: list/find the application named " + exactName + "." );
		else
			this.logger.fine( "Request: list all the applications." );

		List<Application> result = new ArrayList<> ();
		for( ManagedApplication ma : this.manager.applicationMngr().getManagedApplications()) {
			// Equality is on the name, not on the display name
			if( exactName == null || exactName.equals( ma.getName()))
				result.add( ma.getApplication());
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IManagementResource
	 * #listApplications()
	 */
	@Override
	public List<Application> listApplications() {
		return listApplications( null );
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.internal.rest.client.exceptions.server.IApplicationWs
	 * #deleteApplication(java.lang.String)
	 */
	@Override
	public Response deleteApplication( String applicationName ) {

		this.logger.fine( "Request: delete application " + applicationName + "." );
		Response result = Response.ok().build();
		try {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			if( ma == null )
				result = handleError(
						Status.NOT_FOUND,
						new RestError( REST_INEXISTING, application( applicationName )),
						lang( this.manager )).build();
			else
				this.manager.applicationMngr().deleteApplication( ma );

		} catch( UnauthorizedActionException | IOException e ) {
			result = handleError(
					Status.FORBIDDEN,
					new RestError( REST_DELETION_ERROR, e, application( applicationName )),
					lang( this.manager )).build();
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.internal.rest.api.IManagementWs
	 * #shutdownApplication(java.lang.String)
	 */
	@Override
	public Response shutdownApplication( String applicationName ) {

		this.logger.fine( "Request: shutdown application " + applicationName + "." );
		Response result = Response.ok().build();
		try {
			ManagedApplication ma = this.manager.applicationMngr().findManagedApplicationByName( applicationName );
			if( ma == null ) {
				result = handleError(
						Status.NOT_FOUND,
						new RestError( REST_INEXISTING, application( applicationName )),
						lang( this.manager )).build();

			} else {
				this.manager.instancesMngr().undeployAll( ma, null );
			}

		} catch( Exception e ) {
			result = handleError(
					Status.FORBIDDEN,
					new RestError( REST_MNGMT_APP_SHUTDOWN_ERROR, e ),
					lang( this.manager )).build();
		}

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IManagementResource
	 * #setImage(java.lang.String, java.lang.String, java.io.InputStream, com.sun.jersey.core.header.FormDataContentDisposition)
	 */
	@Override
	public Response setImage(
			final String name,
			final String qualifier,
			final InputStream image,
			final FormDataContentDisposition fileDetail) {

		// Do set the image, and wrap exceptions in a HTTP response.
		Response response;
		String id = name + " (qualifier = " + qualifier + ")";
		try {
			doSetImage(name, qualifier, image, fileDetail);
			response = Response.noContent().build();

		} catch( IllegalArgumentException e ) {
			response = handleError(
					Status.BAD_REQUEST,
					new RestError( REST_MNGMT_INVALID_IMAGE, e ),
					lang( this.manager )).build();

		} catch( NoSuchElementException e ) {
			response = handleError(
					Status.BAD_REQUEST,
					new RestError( REST_INEXISTING, e, name( id )),
					lang( this.manager )).build();

		} catch( IOException e ) {
			response = handleError(
					Status.INTERNAL_SERVER_ERROR,
					new RestError( REST_IO_ERROR, e ),
					lang( this.manager )).build();
		}

		return response;
	}


	/**
	 * Upload an image for a template/application.
	 * <p>
	 * If an image was already set, it is overridden by the new one.
	 * </p>
	 * @param name the name of the template/application.
	 * @param qualifier the qualifier of the template, or {@code null} for an application.
	 * @param image the uploaded image.
	 * @param fileDetail the image details.
	 * @throws IllegalArgumentException if the image is too large, or is not supported.
	 * @throws NoSuchElementException if the application/template cannot be found.
	 * @throws IOException if the image cannot be stored.
	 */
	private void doSetImage(
			final String name,
			final String qualifier,
			final InputStream image,
			final FormDataContentDisposition fileDetail)
	throws IOException {

		// Check image size and extension.
		final long size = fileDetail.getSize();
		final String extension = getFileExtension(fileDetail.getFileName());
		if (size > MAX_IMAGE_SIZE)
			throw new IllegalArgumentException("Image is too large: " + size);

		if (!SUPPORTED_EXTENSIONS.contains(extension))
			throw new IllegalArgumentException("Unsupported image file extension: " + extension);

		// Get the target directory.
		File targetDir;
		if (qualifier != null) {
			this.logger.fine( "Request: set template image: " + name + '/' + qualifier + "." );
			final ApplicationTemplate template = this.manager.applicationTemplateMngr().findTemplate(name, qualifier);
			if (template == null)
				throw new NoSuchElementException("Cannot find template: " + name + '/' + qualifier);

			targetDir = new File(template.getDirectory(), Constants.PROJECT_DIR_DESC);

		} else {
			this.logger.fine( "Request: set application image: " + name + "." );
			final Application application = this.manager.applicationMngr().findApplicationByName( name );
			if (application == null)
				throw new NoSuchElementException("Cannot find application: " + name);

			targetDir = new File(application.getDirectory(), Constants.PROJECT_DIR_DESC);
		}

		// First clean the previous "application.*" images, as they may be chosen instead of the one we're uploading.
		for (final String ext : SUPPORTED_EXTENSIONS) {
			File f = new File(targetDir, "application." + ext);
			Utils.deleteFilesRecursivelyAndQuietly( f );
		}

		// Now store the image: rename it to application.X, so we get sure it is chosen as THE app/template icon.
		// (where X is the uploaded file extension).
		Utils.copyStream(image, new File(targetDir, "application." + extension));
	}


	/**
	 * Get the file name extension from its name.
	 * @param filename the filename.
	 * @return the file name extension.
	 */
	private static String getFileExtension(final String filename) {

		String extension = "";
		int i = filename.lastIndexOf('.');
		int p = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
		if (i > p)
			extension = filename.substring(i+1);

		return extension;
	}
}
