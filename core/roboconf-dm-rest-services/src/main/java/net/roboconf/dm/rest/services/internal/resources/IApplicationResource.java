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

import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.dm.rest.commons.UrlConstants;
import net.roboconf.dm.rest.commons.beans.TargetAssociation;

/**
 * The REST API to manipulate instances on the DM.
 * <p>
 * Implementing classes may have to redefine the "Path" annotation
 * on the class. This is not required on methods.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public interface IApplicationResource {

	String PATH = "/" + UrlConstants.APP + "/{name}";


	/**
	 * Changes the state of an instance for a given application.
	 * @param applicationName the application name
	 * @param newState the new state (see {@link InstanceStatus})
	 * @param instancePath the instance path (not null)
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 404 The application or the instance was not found.
	 * @HTTP 403 Invalid state or permission issue.
	 */
	@POST
	@Path( "/change-state" )
	Response changeInstanceState( @PathParam("name") String applicationName, @QueryParam("new-state") String newState, @QueryParam("instance-path") String instancePath );


	/**
	 * Sets the description of the application.
	 * @param applicationName the application name
	 * @param desc the new description
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 404 The application was not found.
	 * @HTTP 403 The application's description could not be updated.
	 */
	@POST
	@Path( "/description" )
	Response setDescription( @PathParam("name") String applicationName, String desc );


	/**
	 * Deploys and starts several instances at once.
	 * @param applicationName the application name
	 * @param instancePath the instance path (null to consider the whole application)
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 404 The application or the instance was not found.
	 * @HTTP 403 Invalid state or permission issue.
	 */
	@POST
	@Path( "/deploy-all" )
	Response deployAndStartAll( @PathParam("name") String applicationName, @QueryParam("instance-path") String instancePath );


	/**
	 * Stops several instances at once.
	 * @param applicationName the application name
	 * @param instancePath the instance path (null to consider the whole application)
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 404 The application or the instance was not found.
	 * @HTTP 403 Invalid state or permission issue.
	 */
	@POST
	@Path( "/stop-all" )
	Response stopAll( @PathParam("name") String applicationName, @QueryParam("instance-path") String instancePath );


	/**
	 * Undeploys several instances at once.
	 * @param applicationName the application name
	 * @param instancePath the instance path (null to consider the whole application)
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 404 The application or the instance was not found.
	 * @HTTP 403 Invalid state or permission issue.
	 */
	@POST
	@Path( "/undeploy-all" )
	Response undeployAll( @PathParam("name") String applicationName, @QueryParam("instance-path") String instancePath );


	/**
	 * Adds a new instance.
	 * @param applicationName the application name
	 * @param parentInstancePath the path of the parent instance (optional, null to consider the application as the root)
	 * @param instance the new instance
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 404 The application or the instance's component was not found.
	 * @HTTP 403 The insertion does not comply with the graph definition.
	 */
	@POST
	@Path( "/instances" )
	@Consumes( MediaType.APPLICATION_JSON )
	Response addInstance( @PathParam("name") String applicationName, @QueryParam("instance-path") String parentInstancePath, Instance instance );


	/**
	 * Removes an instance.
	 * @param applicationName the application name
	 * @param instancePath the path of the instance to remove
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 404 The application or the instance was not found.
	 * @HTTP 403 Invalid instance state.
	 * @HTTP 406 The request could not be handled.
	 */
	@DELETE
	@Path( "/instances" )
	Response removeInstance( @PathParam("name") String applicationName, @QueryParam("instance-path") String instancePath );


	/**
	 * Finds the associations between the application "scoped" instances and targets.
	 * @param applicationName the application name
	 * @return a non-null list of target associations
	 *
	 * @HTTP 200 Everything went fine.
	 */
	@GET
	@Path( "/targets" )
	List<TargetAssociation> findTargetAssociations( @PathParam("name") String applicationName );


	/**
	 * Resynchronizes all the instances / agents.
	 * @param applicationName the application name
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 404 The application was not found.
	 * @HTTP 406 The request could not be processed.
	 */
	@POST
	@Path( "/resynchronize" )
	Response resynchronize( @PathParam("name") String applicationName );


	/**
	 * Binds an application for external exports.
	 * @param applicationName the application name
	 * @param externalExportPrefix the name of the prefix for external variables
	 * @param boundApp the name of the application (instance of <code>boundTplName</code>)
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 404 The application was not found.
	 * @HTTP 403 Such a binding is not allowed.
	 */
	@POST
	@Path( "/bind" )
	Response bindApplication(
			@PathParam("name") String applicationName,
			@QueryParam("bound-tpl") String externalExportPrefix,
			@QueryParam("bound-app") String boundApp );


	/**
	 * Unbinds an application for external exports.
	 * @param applicationName the application name
	 * @param externalExportPrefix the name of the prefix for external variables
	 * @param boundApp the name of the application (instance of <code>boundTplName</code>)
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 404 The application was not found.
	 */
	@POST
	@Path( "/unbind" )
	Response unbindApplication(
			@PathParam("name") String applicationName,
			@QueryParam("bound-tpl") String externalExportPrefix,
			@QueryParam("bound-app") String boundApp );


	/**
	 * Replaces application bindings for external exports.
	 * @param applicationName the application name
	 * @param externalExportPrefix the name of the prefix for external variables
	 * @param boundApps the application names (instance of <code>boundTplName</code>)
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 404 The application was not found.
	 * @HTTP 403 Such a binding is not allowed.
	 */
	@POST
	@Path( "/bind-x" )
	Response replaceApplicationBindings(
			@PathParam("name") String applicationName,
			@QueryParam("bound-tpl") String externalExportPrefix,
			@QueryParam("app") List<String> boundApps );


	/**
	 * Gets the application bindings (as a map).
	 * @param applicationName the application name
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine (application bindings were returned too).
	 * @HTTP 404 The application was not found.
	 */
	@GET
	@Path( "/bind" )
	Response getApplicationBindings( @PathParam("name") String applicationName );


	/**
	 * Lists instances of a given application.
	 * @param applicationName the application name
	 * @param instancePath the instance path (null to consider the whole application)
	 * @param allChildren true to get all the children, false to only get the direct children
	 * @return a non-null list of instances
	 *
	 * @HTTP 200 Everything went fine.
	 */
	@GET
	@Path( "/instances" )
	@Produces( MediaType.APPLICATION_JSON )
	List<Instance> listChildrenInstances(
			@PathParam("name") String applicationName,
			@QueryParam("instance-path") String instancePath,
			@QueryParam("all-children") boolean allChildren );


	/**
	 * Lists the available components in this application.
	 * @param applicationName the application name
	 * @return a non-null list of components
	 *
	 * @HTTP 200 Everything went fine.
	 */
	@GET
	@Path( "/components" )
	@Produces( MediaType.APPLICATION_JSON )
	List<Component> listComponents( @PathParam("name") String applicationName );


	/**
	 * Finds possible parent components for a given component.
	 * @param applicationName the application name
	 * @return a non-null list of components
	 *
	 * @HTTP 200 Everything went fine.
	 */
	@GET
	@Path("/components/ancestors")
	@Produces( MediaType.APPLICATION_JSON )
	List<Component> findComponentAncestors( @PathParam("name") String applicationName, @QueryParam("component-name") String componentName );


	/**
	 * Finds possible components under a given component.
	 * @param applicationName the application name
	 * @param componentName a component name (if not specified, returns all the root components)
	 * @return a non-null list of components
	 *
	 * @HTTP 200 Everything went fine.
	 */
	@GET
	@Path( "/components/children" )
	@Produces( MediaType.APPLICATION_JSON )
	List<Component> findComponentChildren( @PathParam("name") String applicationName, @QueryParam("component-name") String componentName );


	/**
	 * Lists all the available commands for an application.
	 * @param app the associated application
	 * @return a non-null list of commands
	 *
	 * @HTTP 200 Everything went fine.
	 */
	@GET
	@Path( "/commands" )
	List<String> listCommands( @PathParam("name") String app );


	/**
	 * Gets the instructions contained by a command.
	 * @param app the associated application
	 * @param commandName the command name
	 * @return the commands content (never null)
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 204 No instruction was found.
	 * @HTTP 404 The application was not found.
	 */
	@GET
	@Path( "/commands/{command-name}" )
	Response getCommandInstructions( @PathParam("name") String app, @PathParam("command-name") String commandName );


	/**
	 * Executes a given command.
	 * @param app the associated application
	 * @param commandName the command name
	 * @return a response indicating if the command is about to be executed.
	 * <p>
	 * The result does not indicate whether the command was successfully executed.
	 * Commands may take time to be run. If the application and the command was found,
	 * then this operation is considered as successful.
	 * </p>
	 *
	 * @HTTP 200 The command was found and successfully executed.
	 * @HTTP 404 The application or the command was not found.
	 * @HTTP 409 If the command execution failed.
	 */
	@POST
	@Path( "/commands/execute" )
	Response executeCommand( @PathParam("name") String app, @QueryParam("command-name") String commandName );


	/**
	 * Replaces the tags for an application template.
	 * @param name the application name
	 * @param version the application version
	 * @param tags the tags
	 * @return a response
	 *
	 * @HTTP 200 Everything went fine.
	 * @HTTP 404 The application template was not found.
	 */
	@POST
	@Path( "/tags" )
	Response replaceTags(
			@PathParam("name") String name,
			@QueryParam("version") String version,
			@QueryParam("tags") List<String> tags );
}
