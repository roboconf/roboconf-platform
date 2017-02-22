/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.urlresolvers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.logging.Logger;

import net.roboconf.core.utils.UriUtils;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DefaultUrlResolver implements IUrlResolver {

	protected final Logger logger = Logger.getLogger( getClass().getName());


	@Override
	public ResolvedFile resolve( String url ) throws IOException {

		ResolvedFile resolvedFile;
		try {
			// Local file
			if( url.toLowerCase().startsWith( "file:/" )) {
				File f = new File( UriUtils.urlToUri( url ));
				resolvedFile = new ResolvedFile( f, true );
			}

			// Other ones are downloaded directly
			else {
				URI uri = UriUtils.urlToUri( url );
				InputStream in = uri.toURL().openStream();
				File f = File.createTempFile( "rbcf_download_", null );
				try {
					Utils.copyStream( in, f );

				} finally {
					Utils.closeQuietly( in );
				}

				resolvedFile = new ResolvedFile( f, false );
			}

		} catch( Exception e ) {
			throw new IOException( e );
		}

		return resolvedFile;
	}
}
