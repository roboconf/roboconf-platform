/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.target.api;

import java.io.File;
import java.util.Map;

import net.roboconf.core.model.beans.Instance;

/**
 * A bean to wrap the many parameters necessary to create or configure a machine.
 * @author Vincent Zurczak - Linagora
 */
public class TargetHandlerParameters {

	private Map<String,String> targetProperties;
	private Map<String,String> messagingProperties;
	private String scopedInstancePath, applicationName, domain;
	private Instance scopedInstance;
	private File targetConfigurationScript, targetPropertiesDirectory;



	public Map<String,String> getTargetProperties() {
		return this.targetProperties;
	}

	public void setTargetProperties( Map<String,String> targetProperties ) {
		this.targetProperties = targetProperties;
	}

	public TargetHandlerParameters targetProperties( Map<String,String> targetProperties ) {
		this.targetProperties = targetProperties;
		return this;
	}


	public Map<String,String> getMessagingProperties() {
		return this.messagingProperties;
	}

	public void setMessagingProperties( Map<String,String> messagingProperties ) {
		this.messagingProperties = messagingProperties;
	}

	public TargetHandlerParameters messagingProperties( Map<String,String> messagingProperties ) {
		this.messagingProperties = messagingProperties;
		return this;
	}


	public String getScopedInstancePath() {
		return this.scopedInstancePath;
	}

	public void setScopedInstancePath( String scopedInstancePath ) {
		this.scopedInstancePath = scopedInstancePath;
	}

	public TargetHandlerParameters scopedInstancePath( String scopedInstancePath ) {
		this.scopedInstancePath = scopedInstancePath;
		return this;
	}


	public Instance getScopedInstance() {
		return this.scopedInstance;
	}

	public void setScopedInstance( Instance scopedInstance ) {
		this.scopedInstance = scopedInstance;
	}

	public TargetHandlerParameters scopedInstance( Instance scopedInstance ) {
		this.scopedInstance = scopedInstance;
		return this;
	}


	public String getApplicationName() {
		return this.applicationName;
	}

	public void setApplicationName( String applicationName ) {
		this.applicationName = applicationName;
	}

	public TargetHandlerParameters applicationName( String applicationName ) {
		this.applicationName = applicationName;
		return this;
	}


	public String getDomain() {
		return this.domain;
	}

	public void setDomain( String domain ) {
		this.domain = domain;
	}

	public TargetHandlerParameters domain( String domain ) {
		this.domain = domain;
		return this;
	}


	public File getTargetConfigurationScript() {
		return this.targetConfigurationScript;
	}

	public void setTargetConfigurationScript( File targetConfigurationScript ) {
		this.targetConfigurationScript = targetConfigurationScript;
	}

	public TargetHandlerParameters targetConfigurationScript( File targetConfigurationScript ) {
		this.targetConfigurationScript = targetConfigurationScript;
		return this;
	}


	public File getTargetPropertiesDirectory() {
		return this.targetPropertiesDirectory;
	}

	public void setTargetPropertiesDirectory( File targetPropertiesDirectory ) {
		this.targetPropertiesDirectory = targetPropertiesDirectory;
	}

	public TargetHandlerParameters targetPropertiesDirectory( File targetPropertiesDirectory ) {
		this.targetPropertiesDirectory = targetPropertiesDirectory;
		return this;
	}
}
