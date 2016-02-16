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

package net.roboconf.dm.rest.services.internal.resources.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.dm.rest.services.internal.RestServicesUtils;
import net.roboconf.dm.rest.services.internal.resources.IManagementResource;

import com.sun.jersey.core.header.FormDataContentDisposition;

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


	/**
	 * Constructor.
	 * @param manager the manager
	 */
	public ManagementResource( Manager manager ) {
		this.manager = manager;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IManagementResource
	 * #loadApplicationTemplate(java.io.InputStream, com.sun.jersey.core.header.FormDataContentDisposition)
	 */
	@Override
	public Response loadApplicationTemplate( InputStream uploadedInputStream, FormDataContentDisposition fileDetail ) {

		this.logger.fine( "Request: load application from uploaded ZIP file (" + fileDetail.getFileName() + ")." );
		File tempZipFile = new File( System.getProperty( "java.io.tmpdir" ), fileDetail.getFileName());
		File dir = null;
		Response response;
		try {
			// Copy the uploaded ZIP file on the disk
			Utils.copyStream( uploadedInputStream, tempZipFile );

			// Extract the ZIP content
			String appName = fileDetail.getFileName().replace( ".zip", "" );
			dir = new File( System.getProperty( "java.io.tmpdir" ), "roboconf/" + appName );
			Utils.extractZipArchive( tempZipFile, dir );

			// Load the application
			response = loadApplicationTemplate( dir.getAbsolutePath());

		} catch( IOException e ) {
			response = RestServicesUtils.handleException( this.logger, Status.NOT_ACCEPTABLE, "A new application template could not be loaded.", e ).build();

		} finally {
			Utils.closeQuietly( uploadedInputStream );

			// We do not need the extracted application anymore.
			// In case of success, it was copied in the DM's configuration.
			Utils.deleteFilesRecursivelyAndQuietly( dir );
			Utils.deleteFilesRecursivelyAndQuietly( tempZipFile );
		}

		return response;
	}

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IManagementResource
	 * #loadApplicationTemplate(java.lang.String)
	 */
	@Override
	public Response loadApplicationTemplate( String localFilePath ) {

		if( localFilePath == null )
			localFilePath = "null";

		this.logger.fine( "Request: load application from " + localFilePath + "." );
		Response response;
		try {
			ApplicationTemplate tpl = this.manager.applicationTemplateMngr().loadApplicationTemplate( new File( localFilePath ));
			response = Response.ok().entity( tpl ).build();

		} catch( AlreadyExistingException e ) {
			response = RestServicesUtils.handleException( this.logger, Status.FORBIDDEN, "A new application template could not be loaded.", e ).build();

		} catch( InvalidApplicationException e ) {
			response = RestServicesUtils.handleException( this.logger, Status.NOT_ACCEPTABLE, "A new application template could not be loaded.", e ).build();

		} catch( IOException e ) {
			response = RestServicesUtils.handleException( this.logger, Status.UNAUTHORIZED, "A new application template could not be loaded.", e ).build();
		}

		return response;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.internal.rest.client.exceptions.server.IApplicationWs
	 * #listApplicationTemplatess()
	 */
	@Override
	public List<ApplicationTemplate> listApplicationTemplates() {
		this.logger.fine( "Request: list all the application templates." );

		List<ApplicationTemplate> result = new ArrayList<ApplicationTemplate> ();
		result.addAll( this.manager.applicationTemplateMngr().getApplicationTemplates());

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.rest.services.internal.resources.IManagementResource
	 * #deleteApplicationTemplate(java.lang.String, java.lang.String)
	 */
	@Override
	public Response deleteApplicationTemplate( String tplName, String tplQualifier ) {

		String id = tplName + " (qualifier = " + tplQualifier + ")";
		this.logger.fine( "Request: delete application template " + id + "." );
		Response result = Response.ok().build();
		try {
			this.manager.applicationTemplateMngr().deleteApplicationTemplate( tplName, tplQualifier );

		} catch( InvalidApplicationException e ) {
			result = RestServicesUtils.handleException( this.logger, Status.NOT_FOUND, "Application template " + id + " was not found.", e ).build();

		} catch( UnauthorizedActionException | IOException e ) {
			result = RestServicesUtils.handleException( this.logger, Status.FORBIDDEN, "Application template " + id + " could not be deleted.", e ).build();
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
			String tplQualifier = app.getTemplate() == null ? null : app.getTemplate().getQualifier();
			ManagedApplication ma = this.manager.applicationMngr().createApplication( app.getName(), app.getDescription(), tplName, tplQualifier );
			result = Response.ok().entity( ma.getApplication()).build();

		} catch( InvalidApplicationException e ) {
			result = RestServicesUtils.handleException( this.logger, Status.NOT_FOUND, "Application " + app + " references an invalid template.", e ).build();

		} catch( AlreadyExistingException e ) {
			result = RestServicesUtils.handleException( this.logger, Status.FORBIDDEN, "Application " + app + " already exists.", e ).build();

		} catch( IOException e ) {
			result = RestServicesUtils.handleException( this.logger, Status.UNAUTHORIZED, "Application " + app + " could not be created.", e ).build();
		}

		return result;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.internal.rest.client.exceptions.server.IApplicationWs
	 * #listApplications()
	 */
	@Override
	public List<Application> listApplications() {
		this.logger.fine( "Request: list all the applications." );

		List<Application> result = new ArrayList<Application> ();
		for( ManagedApplication ma : this.manager.applicationMngr().getManagedApplications())
			result.add( ma.getApplication());

		return result;
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
				result = Response.status( Status.NOT_FOUND ).entity( "Application " + applicationName + " was not found." ).build();
			else
				this.manager.applicationMngr().deleteApplication( ma );

		} catch( UnauthorizedActionException | IOException e ) {
			result = RestServicesUtils.handleException( this.logger, Status.FORBIDDEN, "Application " + applicationName + " could not be deleted.", e ).build();
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
			if( ma == null )
				result = Response.status( Status.NOT_FOUND ).entity( "Application " + applicationName + " was not found." ).build();
			else
				this.manager.instancesMngr().undeployAll( ma, null );

		} catch( Exception e ) {
			result = RestServicesUtils.handleException( this.logger, Status.FORBIDDEN, "Application " + applicationName + " could not be shutdown.", e ).build();
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
		try {
			doSetImage(name, qualifier, image, fileDetail);
			response = Response.noContent().build();

		} catch (final NoSuchElementException | IllegalArgumentException e) {
			response = RestServicesUtils.handleException(this.logger, Status.BAD_REQUEST, e.getMessage(), e).build();

		} catch (final IOException e) {
			response = RestServicesUtils.handleException(this.logger, Status.INTERNAL_SERVER_ERROR, e.getMessage(), e).build();
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
