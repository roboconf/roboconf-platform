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

package net.roboconf.core.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class MavenUtils {

	/**
	 * Private empty constructor.
	 */
	private MavenUtils() {
		// nothing
	}


	/**
	 * Finds the URL of a Roboconf (JAR) Maven artifact.
	 * @param artifactId an artifact ID (not null)
	 * @param version a version (not null)
	 * @return an URL if the resolution worked, null if it was not found
	 * @throws IOException
	 */
	public static String findMavenUrlForRoboconf( String artifactId, String version )
	throws IOException {
		return findMavenUrl( "net.roboconf", artifactId, version, "jar" );
	}


	/**
	 * Finds the URL of a Maven artifact.
	 * <p>
	 * This method first tries to find the artifact locally (under ~/.m2).
	 * If it does not find the artifact, it then tries to get it from the Sonatype
	 * API.
	 * </p>
	 *
	 * @param groupId a group ID (not null)
	 * @param artifactId an artifact ID (not null)
	 * @param version a version (not null)
	 * @param extension an extension (not null)
	 * @return an URL if the resolution worked, null if it was not found
	 * @throws IOException
	 */
	public static String findMavenUrl(
			String groupId,
			String artifactId,
			String version,
			String extension )
	throws IOException {

		Logger logger = Logger.getLogger( MavenUtils.class.getName());
		String result = null;

		// 1. Search in the local Maven repository
		// We assume it is under ~/.m2/repository.
		// FIXME: we could extract the localRepository parameter...
		File defaultRepo = new File( System.getProperty( "user.home" ), ".m2/repository" );
		if( defaultRepo.exists()) {
			String fixedVersion = version.replaceAll( "(?i)-snapshot", "-SNAPSHOT" );

			StringBuilder sb = new StringBuilder();
			sb.append( groupId.replace( '.', '/' ));
			sb.append( "/" );
			sb.append( artifactId );
			sb.append( "/" );
			sb.append( fixedVersion );
			sb.append( "/" );
			sb.append( artifactId );
			sb.append( "-" );
			sb.append( fixedVersion );
			sb.append( "." );
			sb.append( extension );

			File localFile = new File( defaultRepo, sb.toString());
			if( localFile.exists())
				result = localFile.toURI().toString();
			else
				logger.fine( "Maven resolution: " + localFile + " does not exist (no local artifact)." );
		}

		// 2. Rely on Sonatype
		if( result == null ) {
			StringBuilder requestUrl = new StringBuilder();
			requestUrl.append( "https://oss.sonatype.org/service/local/artifact/maven/resolve?r=" );
			requestUrl.append( version.toLowerCase().endsWith( "-snapshot" ) ? "snapshots" : "releases" );
			requestUrl.append( "&g=" );
			requestUrl.append( groupId );
			requestUrl.append( "&a=" );
			requestUrl.append( artifactId );
			requestUrl.append( "&v=" );
			requestUrl.append( version );
			requestUrl.append( "&p=" );
			requestUrl.append( extension );

			logger.fine( "Maven resolution: requesting the address from " + requestUrl );

			URL url = new URL( requestUrl.toString());
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			Utils.copyStreamSafely( url.openStream(), os );

			Pattern p = Pattern.compile(
					"<repositoryPath>(.*)</repositoryPath>",
					Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL );

			Matcher m = p.matcher( os.toString( "UTF-8" ));
			if( m.find()) {
				StringBuilder sb = new StringBuilder();
				sb.append( "https://oss.sonatype.org/content/repositories/" );
				sb.append( version.toLowerCase().endsWith( "-snapshot" ) ? "snapshots/" : "releases/" );
				sb.append( m.group( 1 ));
				result = sb.toString();
			}
		}

		logger.fine( "Maven resolution: the final artifact location is " + result );
		return result;
	}
}
