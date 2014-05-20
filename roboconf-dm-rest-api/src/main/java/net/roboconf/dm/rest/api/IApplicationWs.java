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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.roboconf.core.actions.ApplicationAction;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.dm.rest.UrlConstants;
import net.roboconf.dm.rest.json.MapHolder;

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
	 * Performs an action on an instance of an application.
	 * @param applicationName the application name
	 * @param action see {@link ApplicationAction}
	 * @param mapHolder a map holder
	 * <p>
	 * {@link MapHolder#INSTANCE_PATH}: the instance path property (if null, we consider the application as the root)<br />
	 * {@link MapHolder#APPLY_TO_CHILDREN}: true to apply this action to all the children too, false to apply it only to this instance.
	 * Only makes sense when instancePath is not null.
	 * </p>
	 *
	 * @return a response
	 */
	@POST
	@Path( "/{action}" )
	@Consumes( MediaType.APPLICATION_JSON )
	Response perform( @PathParam("name") String applicationName, @PathParam("action") String action, MapHolder mapHolder );


	/**
	 * Adds a new instance.
	 * @param applicationName the application name
	 * @param parentInstancePath the path of the parent instance (optional, null to consider the application as the root)
	 * @param instance the new instance
	 * @return a response
	 */
	@POST
	@Path( "/add" )
	@Consumes( MediaType.APPLICATION_JSON )
	Response addInstance( @PathParam("name") String applicationName, @QueryParam("instance-path") String parentInstancePath, Instance instance );


	/**
	 * Lists the paths of the children of an instance.
	 * @param applicationName the application name
	 * @param instancePath the instance path (if null, we consider the application as the root)
	 * @return a non-null list
	 */
	@GET
	@Path( "/children" )
	@Produces( MediaType.APPLICATION_JSON )
	List<Instance> listChildrenInstances( @PathParam("name") String applicationName, @QueryParam("instance-path") String instancePath );


	/**
	 * Lists the paths of the children of an instance.
	 * @param applicationName the application name
	 * @param instancePath the instance path (if null, we consider the application as the root)
	 * @return a non-null list
	 */
	@GET
	@Path( "/all-children" )
	@Produces( MediaType.APPLICATION_JSON )
	List<Instance> listAllChildrenInstances( @PathParam("name") String applicationName, @QueryParam("instance-path") String instancePath );


	/**
	 * Finds possible components under a given instance.
	 * <p>
	 * This method answers the question: what can we deploy on this instance?
	 * </p>
	 *
	 * @param applicationName the application name
	 * @param instancePath the instance path (if null, we consider the application as the root)
	 * @return a non-null list of components names
	 */
	@GET
	@Path( "/possibilities" )
	@Produces( MediaType.APPLICATION_JSON )
	List<Component> findPossibleComponentChildren( @PathParam("name") String applicationName, @QueryParam("instance-path") String instancePath );


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
	 * Finds possible parent instances for a given component.
	 * <p>
	 * This method answers the question: where could I deploy such a component?
	 * </p>
	 *
	 *
	 * @param applicationName the application name
	 * @return a non-null list of instances paths
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
