/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.integration.tests.commons;

import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;

import net.roboconf.integration.tests.commons.internal.ItUtils;

/**
 * A bean to configure Karaf options.
 * <p>
 * Maven-related properties indicate which Karaf distribution use.
 * The directory name is the name of the directory where the tests file lie.
 * All the Karaf logs can be hidden with the <code>hideLogs</code> field.
 * This parameter is ignored if the <code>roboconfLogsLevel</code> is set. By
 * default, it is null. When set, Roboconf logs will use this level. This level
 * should only be changed for debug purpose.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class ItConfigurationBean {

	private String groupId, artifactId, version, directoryName;
	private boolean hideLogs;
	private LogLevel roboconfLogsLevel;


	/**
	 * Constructor with default values.
	 * <p>
	 * <code>directory</code> remains <code>null</code>.<br>
	 * <code>artifactId</code> remains <code>null</code>.<br>
	 * <code>groupId</code> is "net.roboconf".<br>
	 * <code>version</code> is extracted from the manifest of "roboconf-core".<br>
	 * <code>hideLogs</code> is <code>true</code>.
	 * </p>
	 */
	public ItConfigurationBean() {
		this.groupId = "net.roboconf";
		this.version = ItUtils.findRoboconfVersion();
		this.hideLogs = true;
	}

	/**
	 * Constructor with default values and specified values for other fields.
	 * @param artifactId
	 * @param directoryName
	 */
	public ItConfigurationBean( String artifactId, String directoryName ) {
		this();
		this.artifactId = artifactId;
		this.directoryName = directoryName;
	}

	/**
	 * @return the groupId
	 */
	public String getGroupId() {
		return this.groupId;
	}

	/**
	 * @param groupId the groupId to set
	 */
	public ItConfigurationBean groupId( String groupId ) {
		this.groupId = groupId;
		return this;
	}

	/**
	 * @return the artifactId
	 */
	public String getArtifactId() {
		return this.artifactId;
	}

	/**
	 * @param artifactId the artifactId to set
	 */
	public ItConfigurationBean artifactId( String artifactId ) {
		this.artifactId = artifactId;
		return this;
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return this.version;
	}

	/**
	 * @param version the version to set
	 */
	public ItConfigurationBean version( String version ) {
		this.version = version;
		return this;
	}

	/**
	 * @return the hideLogs
	 */
	public boolean areLogsHidden() {
		return this.hideLogs;
	}

	/**
	 * @param hideLogs the hideLogs to set
	 * @return this
	 */
	public ItConfigurationBean hideLogs( boolean hideLogs ) {
		this.hideLogs = hideLogs;
		return this;
	}

	/**
	 * @return the directoryName
	 */
	public String getDirectoryName() {
		return this.directoryName;
	}

	/**
	 * @param directoryName the directoryName to set
	 */
	public ItConfigurationBean directoryName( String directoryName ) {
		this.directoryName = directoryName;
		return this;
	}

	/**
	 * @return the roboconfLogsLevel
	 */
	public LogLevel getRoboconfLogsLevel() {
		return this.roboconfLogsLevel;
	}

	/**
	 * @param roboconfLogsLevel the roboconfLogsLevel to set
	 */
	public void roboconfLogsLevel( LogLevel roboconfLogsLevel ) {
		this.roboconfLogsLevel = roboconfLogsLevel;
	}
}
