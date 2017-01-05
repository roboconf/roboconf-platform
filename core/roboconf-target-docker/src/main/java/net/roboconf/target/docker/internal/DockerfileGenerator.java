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

package net.roboconf.target.docker.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.ops4j.pax.url.mvn.MavenResolver;

import net.roboconf.core.utils.UriUtils;
import net.roboconf.core.utils.Utils;

/**
 * Generate a Dockerfile directory with all necessary stuff to setup a Roboconf agent.
 * @author Pierre-Yves Gibello - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class DockerfileGenerator {

	static final String RBCF_DIR = "/usr/local/roboconf-agent/";
	static final String BACKUP = "agent-extensions-backup/";

	private static final String DEPLOY = "deploy/";
	private static final String GENERATED_FEATURE_XML = "generated-feature.xml";

	private static final String[] RESOURCES_TO_COPY = { "start.sh", "rename.sh", "rc.local" };

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final String agentPackURL;

	private String packages = DockerHandler.AGENT_JRE_AND_PACKAGES_DEFAULT;
	private boolean isTar = true;
	private String baseImageName = "ubuntu:14.04";

	final List<String> deployList;
	final List<String> bundleUrls = new ArrayList<> ();
	MavenResolver mavenResolver;



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
	public DockerfileGenerator(
			String agentPackURL,
			String packages,
			List<String> deployList,
			String baseImageName ) {

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
	 * @param mavenResolver the mavenResolver to set
	 */
	public void setMavenResolver( MavenResolver mavenResolver ) {
		this.mavenResolver = mavenResolver;
	}


	/**
	 * Generates a dockerfile.
	 * @return path to a full-fledged temporary Dockerfile directory
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public File generateDockerfile() throws IOException, URISyntaxException {

		// Create temporary dockerfile directory
		Path dockerfile = Files.createTempDirectory("roboconf_");

		// Copy agent package in temporary dockerfile directory
		String agentFilename = findAgentFileName( this.agentPackURL, this.isTar );
		File tmpPack = new File( dockerfile.toFile(), agentFilename );
		tmpPack.setReadable( true );

		try {
			this.logger.fine( "Downloading the agent package from " + this.agentPackURL );
			DockerUtils.downloadRemotePackage( this.agentPackURL, tmpPack, this.mavenResolver );
			this.logger.fine( "The agent package was successfully downloaded." );

		} catch( Exception e ) {
			this.logger.fine( "The agent package could not be downloaded." );
			Utils.logException( this.logger, e );
			throw e instanceof IOException ? (IOException) e : new IOException( e );
		}

		// Generate the Docker instructions
		this.logger.fine( "Generating a Dockerfile." );
		File generated = new File( dockerfile.toFile(), "Dockerfile" );
		PrintWriter out = null;
		try {
			out = new PrintWriter( generated, "UTF-8" );
			out.println("FROM " + this.baseImageName);

			String actualPackages = this.packages;
			if( ! this.isTar )
				actualPackages = "unzip " + actualPackages;

			out.println("RUN apt-get update && apt-get install -y " + actualPackages + " && rm -rf /var/lib/apt/lists/*");

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
			out.println("COPY start.sh " + RBCF_DIR);

			// Additional agent extensions?
			if( this.deployList != null ) {
				out.println( "RUN mkdir -p " + RBCF_DIR + BACKUP );
				out.println( this.handleAdditionalDeployments( dockerfile.toFile()));
			}

			// Generated feature?
			// It must be named "feature.xml"!!!
			if( this.bundleUrls.size() > 0 )
				out.println( "COPY " + GENERATED_FEATURE_XML + " " + RBCF_DIR + DEPLOY + "feature.xml" );

		} finally {
			Utils.closeQuietly( out );
			this.logger.fine( "The Dockerfile was generated." );
		}

		// Copy essential resources in the Dockerfile
		for( final String s : RESOURCES_TO_COPY ) {

			this.logger.fine( "Copying " + s + "..." );
			generated = new File( dockerfile.toFile(), s );
			InputStream in = null;

			try {
				in = this.getClass().getResourceAsStream( "/" + s );
				Utils.copyStream( in, generated );
				generated.setExecutable( true, false );
				this.logger.fine( s + " was copied within the Dockerfile's directory." );

			} finally {
				Utils.closeQuietly( in );
			}
		}

		// Do we have bundles to deploy?
		// If some, generate a feature and save it in the Dockerfile resources.
		String karafFeature = prepareKarafFeature( this.bundleUrls );
		if( karafFeature != null ) {
			this.logger.fine( "Writing " + GENERATED_FEATURE_XML + "..." );
			File target = new File( dockerfile.toFile(), GENERATED_FEATURE_XML );
			Utils.writeStringInto( karafFeature, target );
			this.logger.fine( GENERATED_FEATURE_XML + " was copied within the Dockerfile's directory. (" + target + ")" );
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
		if( url.toLowerCase().startsWith( "mvn:" )
				|| agentFilename.contains( "?" )
				|| agentFilename.contains( "&" ))
			agentFilename = "roboconf-agent" + (isTar ? ".tar.gz" : ".zip");

		return agentFilename;
	}


	/**
	 * Get the file name from a "file:" URL.
	 * @param url the URL of the file.
	 * @return the file name.
	 */
	static String getFileNameFromFileUrl( final String url ) {

		String name = url.substring( url.lastIndexOf('/') + 1 );
		int index = name.lastIndexOf( '?' );
		if( index > 0 )
			name = name.substring( 0, index );
		else if( index == 0 )
			name = name.substring( 1 );

		return name.replaceAll( "[^\\w.-]", "_" );
	}


	/**
	 * @param bundleUrls a non-null list of URLs (relative to the Docker container)
	 * @return the content of a Karaf feature, or null if no feature has to be installed
	 * @throws IOException
	 */
	static String prepareKarafFeature( List<String> bundleUrls ) throws IOException {

		String result = null;
		if( ! bundleUrls.isEmpty()) {

			ByteArrayOutputStream os = new ByteArrayOutputStream();
			InputStream in = DockerfileGenerator.class.getResourceAsStream( "/feature-tpl.xml" );
			Utils.copyStreamSafely( in, os );

			StringBuilder sb = new StringBuilder();
			for( String url : bundleUrls ) {
				sb.append( "<bundle>" );
				sb.append( url );
				sb.append( "</bundle>\n" );
			}

			result = os.toString( "UTF-8" ).replace( "%CONTENT%", sb.toString());
		}

		return result;
	}


	/**
	 * This methods prepares the actions to perform for agent extensions.
	 * @return a string to append to the dockerfile (never null)
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	String handleAdditionalDeployments( File dockerFileLocation ) throws IOException, URISyntaxException {
		StringBuilder sb = new StringBuilder();

		// Run through the list...
		for( final String deployUrl : this.deployList ) {

			// Local, Maven or remote URL?
			// "ADD" supports remotes URLs. So, we must copy other kinds.
			File fileToCopy = null;
			if( deployUrl.toLowerCase().startsWith( "file:/" )) {
				fileToCopy = new File( UriUtils.urlToUri( deployUrl ));

			} else if( deployUrl.toLowerCase().startsWith( "mvn:" )) {
				this.logger.fine( "Resolving a Maven URL: " + deployUrl );
				fileToCopy = this.mavenResolver.resolve( deployUrl );
			}

			// Where to place the file in the image?
			// JAR file? We will generate a feature.xml file, so put it in "backup".
			// Otherwise, directly put it in Karaf's deploy directory
			boolean isJar = fileToCopy != null && fileToCopy.getName().toLowerCase().endsWith( ".jar" );
			isJar |= deployUrl.toLowerCase().endsWith( ".jar" );

			String dir = RBCF_DIR + (isJar ? BACKUP : DEPLOY);

			// Add whatever is necessary in the dockerfile
			String name;
			if( fileToCopy != null ) {
				this.logger.fine( "Copying " + deployUrl + "..." );
				Utils.copyStream( fileToCopy, new File( dockerFileLocation, fileToCopy.getName()));
				sb.append( "ADD " + fileToCopy.getName() + " " + dir );
				name = fileToCopy.getName();

			} else {
				sb.append( "ADD " + deployUrl + " " + dir );
				name = getFileNameFromFileUrl( deployUrl );
			}

			// What should be added to the generated feature?
			// Well, we keep the JAR files.
			if( isJar ) {
				if( fileToCopy != null )
					this.bundleUrls.add( "file://" + dir + name );
				else
					this.bundleUrls.add( deployUrl );
			}
		}

		return sb.toString();
	}
}
