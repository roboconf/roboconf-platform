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

package net.roboconf.core.errors.i18n;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.RoboconfErrorHelpers;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TranslationBundle extends ListResourceBundle {

	public static final String DETAILS_SEPARATOR = "@details-separator@";
	public static final String EN = "en_EN";
	public static final String FR = "fr_FR";

	static final List<String> LANGUAGES = Arrays.asList( EN, FR );

	static final String ROW_PATTERN = "\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"";
	private final String lang;


	/**
	 * Constructor.
	 * <p>
	 * Also you can instantiate this class yourself, it is recommended
	 * to use {@link RoboconfErrorHelpers#formatErrors(java.util.Collection, String)}
	 * instead of directly using this class.
	 * </p>
	 */
	public TranslationBundle( String lang ) {
		this.lang = resolve( lang );
	}


	/**
	 * @return the lang
	 */
	public String getLang() {
		return this.lang;
	}


	@Override
	protected Object[][] getContents() {

		Map<String,String> map = loadContent( this.lang );
		Object[][] result = new Object[ map.size()][ 2 ];

		int i = 0;
		for( Map.Entry<String,String> entry : map.entrySet()) {
			result[ i ][ 0 ] = entry.getKey();
			result[ i ][ 1 ] = entry.getValue();
			i++;
		}

		return result;
	}


	/**
	 * Resolves a language to a supported translation.
	 * @param lang a string (e.g. "en_EN")
	 * @return a supported language
	 */
	public static String resolve( String lang ) {

		String result;
		if( ! LANGUAGES.contains( lang ))
			result = EN;
		else
			result = lang;

		return result;
	}


	/**
	 * Loads content from JSon files.
	 * @param lang the language to use
	 * @return a non-null map
	 */
	static Map<String,String> loadContent( String lang ) {

		Map<String,String> result = new LinkedHashMap<> ();
		try {
			InputStream in = TranslationBundle.class.getResourceAsStream( "/" + lang + ".json" );
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			Utils.copyStreamSafely( in, os );
			String content = os.toString( "UTF-8" );

			Matcher m = Pattern.compile( ROW_PATTERN ).matcher( content );
			while( m.find()) {

				// Get the value
				String s = m.group( 2 );

				// Replace i18n parameters here (when it makes sense)
				try {
					ErrorCode ec = ErrorCode.valueOf( m.group( 1 ));
					for( int i=0; i<ec.getI18nProperties().length; i++ ) {
						s = s.replace( "{" + i + "}", ec.getI18nProperties()[ i ]);
					}

				} catch( IllegalArgumentException e ) {
					// nothing
				}

				result.put( m.group( 1 ), s );
			}

		} catch( Exception e ) {
			final Logger logger = Logger.getLogger( TranslationBundle.class.getName());
			Utils.logException( logger, e );
		}

		return result;
	}
}
