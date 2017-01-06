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

package net.roboconf.dm.rest.services.internal.resources;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import net.roboconf.core.model.runtime.TargetUsageItem;
import net.roboconf.core.model.runtime.TargetWrapperDescriptor;
import net.roboconf.dm.rest.commons.UrlConstants;

/**
 * The REST API related to targets.
 * <p>
 * Implementing classes have to define the "Path" annotation
 * on the class. Use {@link #PATH}.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public interface ITargetResource {

	String PATH = "/" + UrlConstants.TARGETS;


	// Manage targets


	/**
	 * Lists targets.
	 * <p>
	 * This method can be used to list all the targets, or to filter those
	 * associated (as hints) with a given application or application template.
	 * </p>
	 *
	 * @param applicationName an optional application name
	 * @param qualifier an optional qualifier, if we want to list targets for a given application template
	 * @return a non-null list of target descriptions
	 *
	 * @HTTP 200 Everything went fine.
	 */
	@GET
	@Produces( MediaType.APPLICATION_JSON )
	List<TargetWrapperDescriptor> listTargets(
			@QueryParam("name") String applicationName,
			@QueryParam("qualifier") String qualifier );


	/**
	 * Creates or updates a target.
	 * @param rawProperties the target's properties
	 * @param targetId an optional target ID (not specified =&gt; will be created)
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 403 Invalid target ID or target still in use.
	 */
	@POST
	@Produces( MediaType.TEXT_PLAIN )
	Response createOrUpdateTarget( String rawProperties, @QueryParam("target-id") String targetId );


	/**
	 * Loads target properties from a ZIP file.
	 * @param uploadedInputStream the uploaded archive
	 * @param fileDetail the file details
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 403 Invalid target properties.
	 * @HTTP 406 The targets could not be registered.
	 */
	@POST
	@Path("/archive")
	@Consumes( MediaType.MULTIPART_FORM_DATA )
	@Produces( MediaType.APPLICATION_JSON )
	Response loadTargetArchive(
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail );


	/**
	 * Deletes a target.
	 * @param targetId a non-null target ID
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 403 The target is still in use.
	 */
	@DELETE
	@Path( "{target-id}" )
	Response deleteTarget( @PathParam("target-id") String targetId );


	/**
	 * Gets the target properties.
	 * @param targetId a non-null target ID
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine (with the properties).
	 * @HTTP 404 The target was not found.
	 */
	@GET
	@Path( "{target-id}" )
	Response getTargetProperties( @PathParam("target-id") String targetId );


	/**
	 * Gets general information about a target.
	 * @param targetId a non-null target ID
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine (with a description of the target).
	 * @HTTP 404 The target was not found.
	 */
	@GET
	@Path( "{target-id}/details" )
	Response findTargetById( @PathParam("target-id") String targetId );


	// Association targets with instances


	/**
	 * Associates a target with an instance application.
	 * @param name an application name
	 * @param qualifier a qualifier if the association implies an application template
	 * @param instancePathOrComponentName an instance path or a component name (can be null)
	 * @param targetId a target ID (useless when <code>bind</code> is false)
	 * @param bind true if we should create the association, false to delete it
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 400 No application or application template was found.
	 * @HTTP 403 The association could not be created.
	 */
	@Path( "{target-id}/associations" )
	@POST
	Response associateTarget(
			@QueryParam("name") String name,
			@QueryParam("qualifier") String qualifier,
			@QueryParam("elt") String instancePathOrComponentName,
			@PathParam( "target-id" ) String targetId,
			@QueryParam("bind") boolean bind );


	// Defining hints


	/**
	 * Updates a target's hints.
	 * @param name an application name
	 * @param qualifier a qualifier if the hint implies an application template
	 * @param targetId a target ID
	 * @param bind true if we should create the association, false to delete it
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 */
	@Path( "{target-id}/hints" )
	@POST
	Response updateHint(
			@QueryParam("name") String name,
			@QueryParam("qualifier") String qualifier,
			@PathParam( "target-id" ) String targetId,
			@QueryParam("bind") boolean bind );


	// Diagnostics

	/**
	 * Finds statistics about target usage.
	 * @param targetId a target ID
	 * @return a non-null list of target usage
	 *
	 * @HTTP 200 Everything went fine.
	 */
	@GET
	@Path( "{target-id}/usage" )
	@Produces( MediaType.APPLICATION_JSON )
	List<TargetUsageItem> findUsageStatistics( @PathParam("target-id") String targetId );
}
