/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import net.roboconf.core.utils.Utils;

/**
 * The descriptor for an application to deploy with Roboconf.
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationDescriptor {

	public static final String APPLICATION_NAME = "application-name";
	public static final String APPLICATION_QUALIFIER = "application-qualifier";
	public static final String APPLICATION_DESCRIPTION = "application-description";
	public static final String APPLICATION_DSL_ID = "application-dsl-id";
	public static final String APPLICATION_GRAPH_EP = "graph-entry-point";
	public static final String APPLICATION_INSTANCES_EP = "instance-entry-point";

	private String name, description, qualifier, graphEntryPoint, instanceEntryPoint, dslId;


	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName( String name ) {
		this.name = name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription( String description ) {
		this.description = description;
	}

	/**
	 * @return the qualifier
	 */
	public String getQualifier() {
		return this.qualifier;
	}

	/**
	 * @param qualifier the qualifier to set
	 */
	public void setQualifier( String qualifier ) {
		this.qualifier = qualifier;
	}

	/**
	 * @return the graphEntryPoint
	 */
	public String getGraphEntryPoint() {
		return this.graphEntryPoint;
	}

	/**
	 * @param graphEntryPoint the graphEntryPoint to set
	 */
	public void setGraphEntryPoint( String graphEntryPoint ) {
		this.graphEntryPoint = graphEntryPoint;
	}

	/**
	 * @return the instanceEntryPoint
	 */
	public String getInstanceEntryPoint() {
		return this.instanceEntryPoint;
	}

	/**
	 * @param instanceEntryPoint the instanceEntryPoint to set
	 */
	public void setInstanceEntryPoint( String instanceEntryPoint ) {
		this.instanceEntryPoint = instanceEntryPoint;
	}


	/**
	 * @return the dslId
	 */
	public String getDslId() {
		return this.dslId;
	}

	/**
	 * @param dslId the dslId to set
	 */
	public void setDslId( String dslId ) {
		this.dslId = dslId;
	}

	/**
	 * Loads an application descriptor.
	 * @param properties a properties object
	 * @return an application descriptor (not null)
	 */
	public static ApplicationDescriptor load( Properties properties ) {

		ApplicationDescriptor result = new ApplicationDescriptor();
		result.name = properties.getProperty( APPLICATION_NAME, null );
		result.description = properties.getProperty( APPLICATION_DESCRIPTION, null );
		result.qualifier = properties.getProperty( APPLICATION_QUALIFIER, null );
		result.graphEntryPoint = properties.getProperty( APPLICATION_GRAPH_EP, null );
		result.instanceEntryPoint = properties.getProperty( APPLICATION_INSTANCES_EP, null );
		result.dslId = properties.getProperty( APPLICATION_DSL_ID, null );

		return result;
	}


	/**
	 * Loads an application descriptor.
	 * @param f a file
	 * @return an application descriptor (not null)
	 * @throws IOException if the file could not be read
	 */
	public static ApplicationDescriptor load( File f ) throws IOException {

		Properties properties = Utils.readPropertiesFile( f );
		if( properties.get( "fail.read" ) != null )
			throw new IOException( "This is for test purpose..." );

		return load( properties );
	}


	/**
	 * Saves an application descriptor.
	 * @param f the file where the properties will be saved
	 * @param descriptor an application descriptor (not null)
	 * @throws IOException if the file could not be written
	 */
	public static void save( File f, ApplicationDescriptor descriptor ) throws IOException {

		Properties properties = new Properties();
		if( descriptor.name != null )
			properties.setProperty( APPLICATION_NAME, descriptor.name );

		if( descriptor.qualifier != null )
			properties.setProperty( APPLICATION_QUALIFIER, descriptor.qualifier );

		if( descriptor.dslId != null )
			properties.setProperty( APPLICATION_DSL_ID, descriptor.dslId );

		if( descriptor.description != null )
			properties.setProperty( APPLICATION_DESCRIPTION, descriptor.description );

		if( descriptor.graphEntryPoint != null )
			properties.setProperty( APPLICATION_GRAPH_EP, descriptor.graphEntryPoint );

		if( descriptor.instanceEntryPoint != null )
			properties.setProperty( APPLICATION_INSTANCES_EP, descriptor.instanceEntryPoint );

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream( f );
			properties.store( fos, null );

		} finally {
			Utils.closeQuietly( fos );
		}
	}
}
