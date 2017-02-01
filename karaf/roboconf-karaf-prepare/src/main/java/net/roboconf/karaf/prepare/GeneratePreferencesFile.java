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

package net.roboconf.karaf.prepare;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import net.roboconf.core.model.runtime.Preference.PreferenceKeyCategory;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.api.IPreferencesMngr;
import net.roboconf.dm.management.api.IPreferencesMngr.Defaults;
import net.roboconf.dm.management.api.IPreferencesMngr.PreferenceDescription;

/**
 * A stand-alone class used to generate the preferences file for the DM's distribution.
 * <p>
 * This class is used during the Maven build of the "roboconf-karaf-dist-dm" module.
 * We put it in this bundle as it is a utility class and that it was complicated to make the
 * Karaf distribution module a Java project (the packaging type is "karaf-assembly" and it has some
 * weird dependencies that can only be managed by the Karaf Maven plugin).
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class GeneratePreferencesFile {

	/**
	 * @param args
	 */
	public static void main( String[] args ) {

		try {
			new GeneratePreferencesFile().run( args );

		} catch( Exception e ) {
			e.printStackTrace();
			System.exit( 2 );
		}
	}


	/**
	 * The real method that does the job.
	 * @param args
	 * @throws Exception
	 */
	public void run( String[] args ) throws Exception {

		// Check the argument
		if( args.length != 1 )
			throw new RuntimeException( "A file path was expected as an argument." );

		File targetFile = new File( args[ 0 ]);
		if( ! targetFile.isFile())
			throw new RuntimeException( "File " + targetFile + " does not exist." );

		// Prepare the comments for all the properties
		Field[] fields = IPreferencesMngr.class.getDeclaredFields();
		Map<String,String> propertyNameToComment = new HashMap<> ();
		for( Field field : fields ) {
			String constantName = (String) field.get( null );
			PreferenceDescription annotation = field.getAnnotation( PreferenceDescription.class );
			if( annotation == null )
				throw new RuntimeException( "Documentation was not written for the " + field.getName() + " constant in IPreferencesMngr.java." );

			StringBuilder sb = new StringBuilder();
			sb.append( annotation.desc());
			String[] values = annotation.values();
			if( values.length > 0 ) {
				sb.append( "\n\nPossible values:\n" );
				for( String value : values ) {
					sb.append( " - " );
					sb.append( value );
					sb.append( "\n" );
				}
			}

			propertyNameToComment.put( constantName, sb.toString());
		}

		// Prepare the content to generate
		Defaults def = new Defaults();
		StringBuilder sb = new StringBuilder();
		for( PreferenceKeyCategory cat : PreferenceKeyCategory.values()) {

			sb.append( "\n###\n# " );
			sb.append( cat.getDescription());
			sb.append( "\n###\n" );

			for( Map.Entry<String,PreferenceKeyCategory> entry : def.keyToCategory.entrySet()) {
				if( cat != entry.getValue())
					continue;

				String comment = propertyNameToComment.get( entry.getKey());
				comment = comment.replaceAll( "^", "# " ).replace( "\n", "\n# " );

				sb.append( "\n" );
				sb.append( comment );
				sb.append( "\n" );

				sb.append( entry.getKey());
				sb.append( " = " );
				String defaultValue = def.keyToDefaultValue.get( entry.getKey());
				if( ! Utils.isEmptyOrWhitespaces( defaultValue ))
					sb.append( defaultValue.trim());

				sb.append( "\n" );
			}

			sb.append( "\n" );
		}

		// Update the file
		Utils.appendStringInto( sb.toString(), targetFile );
	}
}
