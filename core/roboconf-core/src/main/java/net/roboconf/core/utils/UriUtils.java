/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * A collection of helpers related to {@link URI}.
 * @author Vincent Zurczak - Linagora
 */
public final class UriUtils {

	/**
	 * Private empty constructor.
	 */
	private UriUtils() {
		// nothing
	}


	/**
	 * Builds an URI from an URL (with a handle for URLs not compliant with RFC 2396).
	 * @param url an URL
	 * @return an URI
	 * @throws URISyntaxException if the URI is invalid and could not be repaired
	 */
	public static URI urlToUri( URL url ) throws URISyntaxException {

		URI uri;
		try {
			// Possible failing step.
			uri = url.toURI();

		} catch( Exception e ) {
			// URL did not comply with RFC 2396 => illegal non-escaped characters.
			try {
				uri = new URI(
						url.getProtocol(),
						url.getUserInfo(),
						url.getHost(),
						url.getPort(),
						url.getPath(),
						url.getQuery(),
						url.getRef());

			} catch( Exception e1 ) {
				throw new URISyntaxException( String.valueOf( url ), "Broken URL." );
			}
		}

		uri = uri.normalize();
		return uri;
	}


	/**
	 * Builds an URI from an URL string (with an handle for URLs not compliant with RFC 2396).
	 * @param urlAsString an URL as a string
	 * @return an URI
	 * @throws URISyntaxException if the URI is invalid and could not be repaired
	 */
	public static URI urlToUri( String urlAsString ) throws URISyntaxException {

		URL url;
		try {
			url = new URL( urlAsString );

		} catch( Exception e ) {
			throw new URISyntaxException( urlAsString, "Invalid URL." );
		}

		return urlToUri( url );
	}


	/**
	 * Builds an URI from an URI and a suffix.
	 *
	 * <p>
	 * This suffix can be an absolute URL, or a relative path
	 * with respect to the first URI. In this case, the suffix is resolved
	 * with respect to the URI.
	 * </p>
	 * <p>
	 * If the suffix is already an URL, its is returned.<br>
	 * If the suffix is a relative file path and cannot be resolved, an exception is thrown.
	 * </p>
	 * <p>
	 * The returned URI is normalized.
	 * </p>
	 *
	 * @param referenceUri the reference URI (can be null)
	 * @param uriSuffix the URI suffix (not null)
	 * @return the new URI
	 * @throws URISyntaxException if the resolution failed
	 */
	public static URI buildNewURI( URI referenceUri, String uriSuffix ) throws URISyntaxException {

		if( uriSuffix == null )
			throw new IllegalArgumentException( "The URI suffix cannot be null." );

		uriSuffix = uriSuffix.replaceAll( "\\\\", "/" );
		URI importUri = null;
		try {
			// Absolute URL ?
			importUri = urlToUri( new URL( uriSuffix ));

		} catch( Exception e ) {
			try {
				// Relative URL ?
				if( ! referenceUri.toString().endsWith( "/" )
						&& ! uriSuffix.startsWith( "/" ))
					referenceUri = new URI( referenceUri.toString() + "/" );

				importUri = referenceUri.resolve( new URI( null, uriSuffix, null ));

			} catch( Exception e2 ) {
				String msg =
						"An URI could not be built from the URI " + referenceUri.toString()
						+ " and the suffix " + uriSuffix + ".";
				throw new URISyntaxException( msg, e2.getMessage());
			}
		}

		return importUri.normalize();
	}
}
