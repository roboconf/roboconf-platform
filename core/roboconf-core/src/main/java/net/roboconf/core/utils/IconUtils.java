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

import java.io.File;
import java.io.FileFilter;
import java.util.AbstractMap;
import java.util.Map;

import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.AbstractApplication;
import net.roboconf.core.model.beans.ApplicationTemplate;

/**
 * Utilities related to icons.
 * <p>
 * Because it is used in the web administration,
 * and in the REST services, this class must be exported by the bundle.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public final class IconUtils {

	public static final String MIME_JPG = "image/jpeg";
	public static final String MIME_PNG = "image/png";
	public static final String MIME_SVG = "image/svg+xml";
	public static final String MIME_GIF = "image/gif";
	public static final String MIME_BINARY = "application/octet-stream";


	/**
	 * Private empty constructor.
	 */
	private IconUtils() {
		// nothing
	}


	/**
	 * <i>Encodes</i> the URL for an application or a template.
	 * <p>
	 * For an application, it looks like <code>/app/icon.jpg</code>.<br>
	 * For a template, it looks like <code>/app/version/icon.jpg</code>.
	 * </p>
	 * <p>
	 * Notice the returned image is not always a JPG file.
	 * The MIME type will be used by the browser.
	 * </p>
	 *
	 * @param name the application or template name
	 * @param version the template version, or null for an application
	 * @param iconFile the icon file associated with the application or the template
	 * @return the empty string if iconFile is null, or a longer path otherwise
	 */
	public static String encodeIconUrl( String name, String version, File iconFile ) {

		StringBuilder sb = new StringBuilder();
		if( iconFile != null ) {
			sb.append( "/" );
			sb.append( name );
			if( ! Utils.isEmptyOrWhitespaces( version ))
				sb.append( "/" + version );

			sb.append( "/" );
			sb.append( iconFile.getName());
		}

		return sb.toString();
	}


	/**
	 * Decodes an URL path to extract a name and a potential version.
	 * @param path an URL path
	 * @return a non-null map entry (key = name, value = version)
	 */
	public static Map.Entry<String,String> decodeIconUrl( String path ) {

		if( path.startsWith( "/" ))
			path = path.substring( 1 );

		String name = null, version = null;
		String[] parts = path.split( "/" );
		switch( parts.length ) {
		case 2:
			name = parts[ 0 ];
			break;

		case 3:
			name = parts[ 0 ];
			version = parts[ 1 ];
			break;

		default:
			break;
		}

		return new AbstractMap.SimpleEntry<>( name, version );
	}


	/**
	 * Finds the icon associated with an application or application template.
	 * @param app an application (not null)
	 * @return an existing file, or null if no icon was found
	 */
	public static File findIcon( AbstractApplication app ) {
		return findIcon( app.getDirectory());
	}


	/**
	 * Finds the URL to access the icon of an application or templates.
	 * @param app an application or an application template
	 * @return the empty string if no icon was found, a string otherwise
	 */
	public static String findIconUrl( AbstractApplication app ) {

		StringBuilder sb = new StringBuilder();
		File iconFile = findIcon( app );
		if( iconFile != null ) {
			sb.append( "/" );
			sb.append( app.getName());

			if( app instanceof ApplicationTemplate ) {
				sb.append( "/" );
				sb.append(((ApplicationTemplate) app).getVersion());
			}

			sb.append( "/" );
			sb.append( iconFile.getName());
		}

		return sb.toString();
	}


	/**
	 * Finds the MIMe type associated with an image (based on the file extension).
	 * @param imgFile an image file (not null)
	 * @return a non-null string, matching a MIME type
	 */
	public static String findMimeType( File imgFile ) {

		String result;
		String name = imgFile.getName().toLowerCase();
		if( name.endsWith( ".jpg" ) || name.endsWith( ".jpeg" ))
			result = MIME_JPG;
		else if( name.endsWith( ".gif" ))
			result = MIME_GIF;
		else if( name.endsWith( ".png" ))
			result = MIME_PNG;
		else if( name.endsWith( ".svg" ))
			result = MIME_SVG;
		else
			result = MIME_BINARY;

		return result;
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	private static class ImgeFileFilter implements FileFilter {
		@Override
		public boolean accept( File f ) {
			return f.isFile() && f.getName().matches( "(?i:.*\\.(png|gif|svg|jpg|jpeg)$)" );
		}
	}


	/**
	 * Finds an icon from a root directory (for an application and/or template).
	 * @param rootDirectory a root directory
	 * @return an existing file, or null if no icon was found
	 */
	public static File findIcon( File rootDirectory ) {

		File result = null;
		File[] imageFiles = new File( rootDirectory, Constants.PROJECT_DIR_DESC ).listFiles( new ImgeFileFilter());
		if( imageFiles != null ) {

			// A single image? Take it...
			if( imageFiles.length == 1 ) {
				result = imageFiles[ 0 ];
			}

			// Otherwise, find the "application." file
			else for( File f : imageFiles ) {
				if( f.getName().toLowerCase().startsWith( "application." ))
					result = f;
			}
		}

		return result;
	}
}
