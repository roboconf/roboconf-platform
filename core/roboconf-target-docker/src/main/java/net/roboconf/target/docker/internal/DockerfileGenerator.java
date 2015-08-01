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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;

/**
 * Generate a Dockerfile directory with all necessary stuff to setup a Roboconf agent.
 * @author Pierre-Yves Gibello - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class DockerfileGenerator {

	private static final String[] RESOURCES_TO_COPY = { "start.sh", "rename.sh", "rc.local" };

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final String agentPackURL;

	private String packages = DockerHandler.AGENT_JRE_AND_PACKAGES_DEFAULT;
	private final List<String> deployList;
	private boolean isTar = true;
	private String baseImageName = "ubuntu";


	/**
	 * Constructor for docker file generator.
	 * @param agentPackURL URL or path to the agent tarball or zip (not null)
	 * @param packages packages to be installed using apt-get (including JRE)
	 * <p>
	 * Set to "openjdk-7-jre-headless" if null.
	 * </p>
	 * @param deployList a list of URLs of additional resources to deploy.
	 * @param baseImageName the name of the base image used to create a new image
	 * <p>
	 * This parameter can be null.<br>
	 * In this case, "ubuntu" will be used as the
	 * base image name (<code>FROM ubuntu</code>).
	 * </p>
	 */
	public DockerfileGenerator( String agentPackURL, String packages, List<String> deployList, String baseImageName ) {
		File test = new File(agentPackURL);
		this.agentPackURL = (test.exists() ? "file://" : "") + agentPackURL;

		if( ! Utils.isEmptyOrWhitespaces( packages ))
			this.packages = packages;

		if( ! Utils.isEmptyOrWhitespaces( baseImageName ))
			this.baseImageName = baseImageName;

		if(agentPackURL.toLowerCase().endsWith("zip"))
			this.isTar = false;

		this.deployList = deployList;
	}


	/**
	 * Generates a dockerfile.
	 * @return path to a full-fledged temporary Dockerfile directory
	 * @throws IOException
	 */
	public File generateDockerfile() throws IOException {

		// Create temporary dockerfile directory
		Path dockerfile = Files.createTempDirectory("roboconf_");

		// Copy agent package in temporary dockerfile directory
		String agentFilename = findAgentFileName( this.agentPackURL, this.isTar );
		File tmpPack = new File(dockerfile.toFile(), agentFilename);
		tmpPack.setReadable(true);

		URL u = new URL( this.agentPackURL );
		URLConnection uc = u.openConnection();
		InputStream in = null;
		try {
			this.logger.fine( "Downloading the agent package from " + this.agentPackURL );
			in = new BufferedInputStream( uc.getInputStream());
			Utils.copyStream(in, tmpPack);
			this.logger.fine( "The agent package was successfully downloaded." );

		} catch( IOException e ) {
			this.logger.fine( "The agent package could not be downloaded." );
			Utils.logException( this.logger, e );
			throw e;

		} finally {
			Utils.closeQuietly(in);
		}

		this.logger.fine( "Generating a Dockerfile." );
		File generated = new File(dockerfile.toFile(), "Dockerfile");
		PrintWriter out = null;
		final List<URL> localDeploys = new ArrayList<>();
		try {
			out = new PrintWriter( generated, "UTF-8" );
			out.println("FROM " + this.baseImageName);

			String actualPackages = this.packages;
			if( ! this.isTar )
				actualPackages = "unzip " + actualPackages;
			out.println("RUN apt-get update && apt-get install -y "+ actualPackages + " && rm -rf /var/lib/apt/lists/*");

			// Copy and unzip the agent archive.
			out.println("COPY " + agentFilename + " /usr/local/");
			out.println("RUN cd /usr/local; " + (this.isTar ? "tar xvzf " : "unzip ") + agentFilename);

			// We used to assume the name of the extracted directory was the name of the ZIP file
			// without any file extension. This is wrong. If one points to a snapshot version (e.g. hosted on Sonatype)
			// then the file name contains a qualifier while the inner directory contains the SNAPSHOT mention.
			// The only assumption we can do is that it starts with "roboconf-something-agent".
			// We will rename this directory to "roboconf-agent".
			out.println("COPY rename.sh /usr/local/");
			out.println("RUN cd /usr/local; ./rename.sh");

			// The rc.local and start.sh files will be generated as well!
			out.println("COPY rc.local /etc/");
			out.println("COPY start.sh /usr/local/roboconf-agent/");

			// Copy additional resources in the Karaf deploy directory if needed.
			// We use the Docker ADD command that accepts remote URLs.
			if ( this.deployList != null ) {
				for ( final String deploy : this.deployList ) {

					// We need to be a bit selective here, because:
					// - URL might be a local resource (file://)
					// - Docker's ADD command does not accept:
					//    - file URLs
					//    - path of resources outside the Docker context directory (where the Dockerfile is)
					// So if the URL is a "file:" URL, we add it in the "resourcesToCopy" list, that will be processed
					// just a bit later.
					final URI uri;
					try {
						uri = new URI(deploy);
						if (!"file".equals(uri.getScheme())) {
							// Remote resource URL. No special treatment.
							out.println("ADD " + deploy + " /usr/local/roboconf-agent/deploy/");

						} else {
							// Add the URL to the toCopy list, and add the modified ADD Docker command.
							final URL url = uri.toURL();
							localDeploys.add(url);
							out.println("ADD " + getFileNameFromFileUrl(url) + " /usr/local/roboconf-agent/deploy/");
						}
					} catch (URISyntaxException | IllegalArgumentException | MalformedURLException e) {
						this.logger.warning("Invalid deploy resource URL: " + deploy);
						Utils.logException( this.logger, e );
					}
				}
			}

		} finally {
			Utils.closeQuietly( out );
			this.logger.fine( "The Dockerfile was generated." );
		}

		// Copy essential resources in the Dockerfile
		for( final String s : RESOURCES_TO_COPY ) {
			this.logger.fine( "Copying " + s + "..." );
			generated = new File( dockerfile.toFile(), s );
			try {
				in = this.getClass().getResourceAsStream( "/" + s );
				Utils.copyStream( in, generated );
				generated.setExecutable( true, false );

			} finally {
				Utils.closeQuietly( in );
				this.logger.fine( s + " was copied within the Dockerfile's directory." );
			}
		}

		// Copy additional deploy resources in the Docker context directory.
		for (final URL s : localDeploys) {

			this.logger.fine( "Copying " + s + "..." );
			final File target = new File(dockerfile.toFile(), getFileNameFromFileUrl(s));
			try {
				in = s.openStream();
				Utils.copyStream(in, target);
			} finally {
				Utils.closeQuietly( in );
				this.logger.fine( s + " was copied within the Dockerfile's directory. (" + target + ")" );
			}
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
	 * @return the deployList
	 */
	public List<String> getDeployList() {
		return this.deployList;
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


	/**
	 * Get the file name from a "file:" URL.
	 * @param url the URL of the file.
	 * @return the file name.
	 */
	static String getFileNameFromFileUrl( final URL url ) {

		final String path = url.getPath();
		String name = path.substring( path.lastIndexOf('/') + 1 );
		if( Utils.isEmptyOrWhitespaces( name ))
			name = url.getQuery();

		return name.replaceAll( "[^\\w.-]", "_" );
	}
}
