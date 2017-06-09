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

package net.roboconf.core.model;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.utils.Utils;

/**
 * The descriptor for an application to deploy with Roboconf.
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationDescriptor {

	public static final String APPLICATION_NAME = "application-name";
	public static final String APPLICATION_DESCRIPTION = "application-description";
	public static final String APPLICATION_TPL_NAME = "template-name";
	public static final String APPLICATION_TPL_VERSION = "template-version";

	private String name, description, templateName, templateVersion;


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
	 * @return the templateName
	 */
	public String getTemplateName() {
		return this.templateName;
	}

	/**
	 * @param templateName the templateName to set
	 */
	public void setTemplateName( String templateName ) {
		this.templateName = templateName;
	}

	/**
	 * @return the templateVersion
	 */
	public String getTemplateVersion() {
		return this.templateVersion;
	}

	/**
	 * @param templateVersion the templateVersion to set
	 */
	public void setTemplateVersion( String templateVersion ) {
		this.templateVersion = templateVersion;
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
		result.templateName = properties.getProperty( APPLICATION_TPL_NAME, null );
		result.templateVersion = properties.getProperty( APPLICATION_TPL_VERSION, null );

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
		return load( properties );
	}


	/**
	 * Saves an application descriptor.
	 * @param f the file where the properties will be saved
	 * @param app an application (not null)
	 * @throws IOException if the file could not be written
	 */
	public static void save( File f, Application app ) throws IOException {

		Properties properties = new Properties();
		if( ! Utils.isEmptyOrWhitespaces( app.getDisplayName()))
			properties.setProperty( APPLICATION_NAME, app.getDisplayName());

		if( ! Utils.isEmptyOrWhitespaces( app.getDescription()))
			properties.setProperty( APPLICATION_DESCRIPTION, app.getDescription());

		if( app.getTemplate() != null ) {
			if( ! Utils.isEmptyOrWhitespaces( app.getTemplate().getName()))
				properties.setProperty( APPLICATION_TPL_NAME, app.getTemplate().getName());

			if( ! Utils.isEmptyOrWhitespaces( app.getTemplate().getVersion()))
				properties.setProperty( APPLICATION_TPL_VERSION, app.getTemplate().getVersion());
		}

		Utils.writePropertiesFile( properties, f );
	}
}
