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

package net.roboconf.core.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import net.roboconf.core.internal.utils.Utils;

/**
 * The descriptor for an application to deploy with Roboconf.
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationDescriptor {

	private static final String APPLICATION_NAME = "application-name";
	private static final String APPLICATION_QUALIFIER = "application-qualifier";
	private static final String APPLICATION_DESCRIPTION = "application-description";
	private static final String APPLICATION_GRAPH_EP = "graph-entry-point";
	private static final String APPLICATION_INSTANCES_EP = "instance-entry-point";

	private String name, description, qualifier, graphEntryPoint, instanceEntryPoint;


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

		return result;
	}


	/**
	 * Loads an application descriptor.
	 * @param properties a properties object
	 * @return an application descriptor (not null)
	 * @throws IOException if the file could not be read
	 */
	public static ApplicationDescriptor load( File f ) throws IOException {

		Properties properties = new Properties();
		FileInputStream in = null;
		try {
			in = new FileInputStream( f );
			properties.load( in );

		} finally {
			Utils.closeQuietly( in );
		}

		return load( properties );
	}
}
