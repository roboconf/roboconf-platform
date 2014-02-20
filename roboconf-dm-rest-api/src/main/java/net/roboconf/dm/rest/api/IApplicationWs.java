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

package net.roboconf.dm.rest.api;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.dm.rest.UrlConstants;

/**
 * The REST API to manipulate instances on the DM.
 * <p>
 * Implementing classes may have to redefine the "Path" annotation
 * on the class. This is not required on methods.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public interface IApplicationWs {

	String PATH = "/" + UrlConstants.APP + "/{name}";


	/**
	 * Lists the instances paths.
	 * @param applicationName the application name
	 * @return a response
	 */
	@GET
	@Path( "/all" )
	@Produces( MediaType.APPLICATION_JSON )
	List<Instance> listAllInstances( @PathParam("name") String applicationName );

	/**
	 * Lists the paths of the root instances.
	 * @param applicationName the application name
	 * @return a response
	 */
	@GET
	@Path( "/roots" )
	@Produces( MediaType.APPLICATION_JSON )
	List<Instance> listRootInstances( @PathParam("name") String applicationName );

	/**
	 * Lists the available components in this application.
	 * @param applicationName the application name
	 * @return a non-null list of components
	 */
	@GET
	@Path("/components")
	@Produces( MediaType.APPLICATION_JSON )
	List<Component> listComponents( @PathParam("name") String applicationName );

	/**
	 * Adds a new root instance.
	 * @param applicationName the application name
	 * @param instance the new root instance
	 * @return a response
	 */
	@POST
	@Produces( MediaType.APPLICATION_JSON )
	Response addRootInstance( @PathParam("name") String applicationName, Instance instance );

	/**
	 * Adds a new instance.
	 * @param applicationName the application name
	 * @param parentInstancePath the path of the parent instance
	 * @param instance the new instance
	 * @return a response
	 */
	@POST
	@Path("/{instancePath}")
	@Produces( MediaType.APPLICATION_JSON )
	Response addInstance( @PathParam("name") String applicationName, @PathParam("instancePath") String parentInstancePath, Instance instance );

	/**
	 * Gets an instance.
	 * @param applicationName the application name
	 * @param instancePath the instance path (ID)
	 * @return an instance
	 */
	@GET
	@Path("/{instancePath}")
	@Produces( MediaType.APPLICATION_JSON )
	Instance getInstance( @PathParam("name") String applicationName, @PathParam("instancePath") String instancePath );

	/**
	 * Deletes an instance.
	 * @param applicationName the application name
	 * @param instancePath the instance path (ID)
	 * @return a response
	 */
	@DELETE
	@Path("/{instancePath}")
	@Produces( MediaType.APPLICATION_JSON )
	Response removeInstance( @PathParam("name") String applicationName, @PathParam("instancePath") String instancePath );

	/**
	 * Lists the paths of the children of an instance.
	 * @param applicationName the application name
	 * @return a response
	 */
	@GET
	@Path("/{instancePath}/children")
	@Produces( MediaType.APPLICATION_JSON )
	List<Instance> listChildrenInstances( @PathParam("name") String applicationName, @PathParam("instancePath") String instancePath );

	/**
	 * Finds possible components under a given instance.
	 * <p>
	 * This method answers the question: what can we deploy on this instance?
	 * </p>
	 *
	 * @param applicationName the application name
	 * @return a non-null list of components names
	 */
	@GET
	@Path("/{instancePath}/possibilities")
	@Produces( MediaType.APPLICATION_JSON )
	List<String> findPossibleComponentChildren( @PathParam("name") String applicationName, @PathParam("instancePath") String instancePath );

	/**
	 * Finds possible parent instances for a given component.
	 * <p>
	 * This method answers the question: where could I deploy such a component?
	 * </p>
	 *
	 *
	 * @param applicationName the application name
	 * @return a non-null list of instances names
	 */
	@GET
	@Path("/component/{componentName}")
	@Produces( MediaType.APPLICATION_JSON )
	List<String> findPossibleParentInstances( @PathParam("name") String applicationName, @PathParam("componentName") String componentName );

	/**
	 * Creates an instance from a component name.
	 * @param applicationName the application name
	 * @return a response
	 */
	@GET
	@Path("/component/{componentName}/new")
	@Produces( MediaType.APPLICATION_JSON )
	Instance createInstanceFromComponent( @PathParam("name") String applicationName, @PathParam("componentName") String componentName );
}
