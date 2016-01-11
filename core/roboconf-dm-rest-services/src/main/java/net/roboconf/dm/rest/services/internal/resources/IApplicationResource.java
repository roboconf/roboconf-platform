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
import net.roboconf.core.model.runtime.TargetAssociation;
import net.roboconf.dm.rest.commons.UrlConstants;

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
	 */
	@POST
	@Path( "/change-state" )
	Response changeInstanceState( @PathParam("name") String applicationName, @QueryParam("new-state") String newState, @QueryParam("instance-path") String instancePath );


	/**
	 * Sets the description of the application.
	 * @param applicationName the application name
	 * @param desc the new description
	 * @return a response
	 */
	@POST
	@Path( "/description" )
	Response setDescription( @PathParam("name") String applicationName, String desc );


	/**
	 * Deploys and starts several instances at once.
	 * @param applicationName the application name
	 * @param instancePath the instance path (null to consider the whole application)
	 * @return a response
	 */
	@POST
	@Path( "/deploy-all" )
	Response deployAndStartAll( @PathParam("name") String applicationName, @QueryParam("instance-path") String instancePath );


	/**
	 * Stops several instances at once.
	 * @param applicationName the application name
	 * @param instancePath the instance path (null to consider the whole application)
	 * @return a response
	 */
	@POST
	@Path( "/stop-all" )
	Response stopAll( @PathParam("name") String applicationName, @QueryParam("instance-path") String instancePath );


	/**
	 * Undeploys several instances at once.
	 * @param applicationName the application name
	 * @param instancePath the instance path (null to consider the whole application)
	 * @return a response
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
	 */
	@DELETE
	@Path( "/instances" )
	Response removeInstance( @PathParam("name") String applicationName, @QueryParam("instance-path") String instancePath );


	/**
	 * Finds the associations between the application "scoped" instances and targets.
	 * @param applicationName the application name
	 * @return a response
	 */
	@GET
	@Path( "/targets" )
	List<TargetAssociation> findTargetAssociations( @PathParam("name") String applicationName );


	/**
	 * Resynchronizes all the instances / agents.
	 * @param applicationName the application name
	 * @return a response
	 */
	@POST
	@Path( "/resynchronize" )
	Response resynchronize( @PathParam("name") String applicationName );


	/**
	 * Binds an application for external exports.
	 * @param applicationName the application name
	 * @param boundTplName the template name (no qualifier as it does not make sense for external exports)
	 * @param boundApp the name of the application (instance of <code>tplName</code>)
	 * @return a response
	 */
	@POST
	@Path( "/bind" )
	Response bindApplication(
			@PathParam("name") String applicationName,
			@QueryParam("bound-tpl") String boundTplName,
			@QueryParam("bound-app") String boundApp );


	/**
	 * Gets the application bindings (as a map).
	 * @param applicationName the application name
	 * @return a response
	 */
	@GET
	@Path( "/bind" )
	Response getApplicationBindings( @PathParam("name") String applicationName );


	/**
	 * Lists the paths of the children of an instance.
	 * @param applicationName the application name
	 * @param instancePath the instance path (null to consider the whole application)
	 * @param allChildren true to get all the children, false to only get the direct children
	 * @return a non-null list
	 */
	@GET
	@Path( "/children" )
	@Produces( MediaType.APPLICATION_JSON )
	List<Instance> listChildrenInstances(
			@PathParam("name") String applicationName,
			@QueryParam("instance-path") String instancePath,
			@QueryParam("all-children") boolean allChildren );


	/**
	 * Lists the available components in this application.
	 * @param applicationName the application name
	 * @return a non-null list of components
	 */
	@GET
	@Path( "/components" )
	@Produces( MediaType.APPLICATION_JSON )
	List<Component> listComponents( @PathParam("name") String applicationName );


	/**
	 * Finds possible parent components for a given component.
	 * @param applicationName the application name
	 * @return a non-null list of instances paths
	 */
	@GET
	@Path("/components/ancestors")
	@Produces( MediaType.APPLICATION_JSON )
	List<Component> findComponentAncestors( @PathParam("name") String applicationName, @QueryParam("name") String componentName );


	/**
	 * Finds possible components under a given component.
	 * @param applicationName the application name
	 * @param componentName a component name (if not specified, returns all the root components)
	 * @return a non-null list of components names
	 */
	@GET
	@Path( "/components/children" )
	@Produces( MediaType.APPLICATION_JSON )
	List<Component> findComponentChildren( @PathParam("name") String applicationName, @QueryParam("name") String componentName );
}
