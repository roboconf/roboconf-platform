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

package net.roboconf.target.docker.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;

/**
 * Generate a Dockerfile directory with all necessary stuff to setup a Roboconf agent.
 * @author Pierre-Yves Gibello - Linagora
 */
public class DockerfileGenerator {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final String agentPackURL;

	private String packages = "openjdk-7-jre-headless";
	private boolean isTar = true;
	private String baseImageName = "ubuntu";


	/**
	 * Constructor for docker file generator.
	 * @param agentPackURL URL or path to the agent tarball or zip (not null)
	 * @param packages packages to be installed using apt-get (including JRE)
	 * <p>
	 * Set to "openjdk-7-jre-headless" if null.
	 * </p>
	 *
	 * @param baseImageName the name of the base image used to create a new image
	 * <p>
	 * This parameter can be null.<br />
	 * In this case, "ubuntu" will be used as the
	 * base image name (<code>FROM ubuntu</code>).
	 * </p>
	 */
	public DockerfileGenerator( String agentPackURL, String packages, String baseImageName ) {
		File test = new File(agentPackURL);
		this.agentPackURL = (test.exists() ? "file://" : "") + agentPackURL;

		if( ! Utils.isEmptyOrWhitespaces( packages ))
			this.packages = packages;

		if( ! Utils.isEmptyOrWhitespaces( baseImageName ))
			this.baseImageName = baseImageName;

		if(agentPackURL.toLowerCase().endsWith("zip"))
			this.isTar = false;
	}


	/**
	 * Generates a dockerfile.
	 * @return path to a full-fledged temporary Dockerfile directory
	 * @throws IOException
	 */
	public File generateDockerfile() throws IOException {

		// Create temporary dockerfile directory
		Path dockerfile = Files.createTempDirectory("roboconf_");

		// Copy agent package in temp dockerfile directory
		String agentFilename = findAgentFileName( this.agentPackURL, this.isTar );
		File tmpPack = new File(dockerfile.toFile(), agentFilename);
		tmpPack.setReadable(true);

		URL u = new URL(this.agentPackURL);
		URLConnection uc = u.openConnection();
		InputStream in = null;
		try {
			in = new BufferedInputStream(uc.getInputStream());
			Utils.copyStream(in, tmpPack);
		} finally {
			Utils.closeQuietly(in);
		}

		this.logger.fine( "Generating a Dockerfile." );
		File generated = new File(dockerfile.toFile(), "Dockerfile");
		PrintWriter out = null;
		try {
			out = new PrintWriter( generated, "UTF-8" );
			out.println("FROM " + this.baseImageName);
			out.println("COPY " + agentFilename + " /usr/local/");
			out.println("RUN apt-get update");

			if( ! this.isTar )
				out.println("RUN apt-get -y install unzip");

			out.println("RUN apt-get -y install " + this.packages);
			out.println("RUN cd /usr/local; " + (this.isTar ? "tar xvzf " : "unzip ") + agentFilename);

			// Remove extension from file name (generally .zip or .tar.gz, possibly .tgz or .tar)
			String extractDir = tmpPack.getName();
			extractDir = extractDir.replace( ".tar", "" ).replace( ".gz", "" ).replace( ".tgz", "" ).replace( ".zip", "" );
			out.println("RUN ln -s /usr/local/" + extractDir + " /usr/local/roboconf-agent");

			// The rc.local and start.sh files will be generated as well!
			out.println("COPY rc.local /etc/");
			out.println("COPY start.sh /usr/local/roboconf-agent/");

		} finally {
			Utils.closeQuietly(out);
		}

		// Generate start.sh startup script for roboconf agent
		this.logger.fine( "Generating the start script for the Roboconf agent." );
		generated = new File(dockerfile.toFile(), "start.sh");
		try {
			in = this.getClass().getResourceAsStream( "/start.sh" );
			Utils.copyStream( in, generated );
			generated.setExecutable(true, false);

		} finally {
			Utils.closeQuietly( in );
		}

		// Generate rc.local script to launch roboconf agent at boot time
		this.logger.fine( "Generating a rc.local file." );
		generated = new File(dockerfile.toFile(), "rc.local");
		try {
			in = this.getClass().getResourceAsStream( "/rc.local" );
			Utils.copyStream( in, generated );
			generated.setExecutable(true, false);

		} finally {
			Utils.closeQuietly( in );
		}

		return dockerfile.toFile();
	}


	/**
	 * Retrieve packages list (for apt-get).
	 * @return The packages list
	 */
	public String getPackages() {
		return this.packages;
	}


	/**
	 * Determine if the agent package is a tarball (tar/tgz) file.
	 * @return true for a tarball, false otherwise
	 */
	public boolean isTar() {
		return this.isTar;
	}


	/**
	 * @return the baseImageName
	 */
	public String getBaseImageName() {
		return this.baseImageName;
	}


	/**
	 * Finds the name of the agent file.
	 * @param url the agent's URL (not null)
	 * @param isTar true if it is a TAR.GZ, false for a ZIP
	 * <p>
	 * This parameter is ignored unless the URL does not contain a valid file name.
	 * </p>
	 *
	 * @return a non-null string
	 */
	static String findAgentFileName( String url, boolean isTar ) {

		String agentFilename = url.substring( url.lastIndexOf('/') + 1 );
		if( agentFilename.contains( "?" )
				|| agentFilename.contains( "&" ))
			agentFilename = "roboconf-agent" + (isTar ? ".tar.gz" : ".zip");

		return agentFilename;
	}
}
