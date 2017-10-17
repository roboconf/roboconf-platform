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

package net.roboconf.karaf.commands.common.plugins;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vincent Zurczak - Linagora
 */
public enum SupportedPlugin {

	SCRIPT, PUPPET, LOGGER, FILE;


	/**
	 * Finds a list of string representations for this plug-in.
	 * @return a non-null list
	 */
	public List<String> findString() {

		List<String> result = new ArrayList<> ();
		switch( this ) {
		case SCRIPT:
			result.add( "script" );
			break;

		case PUPPET:
			result.add( "puppet" );
			break;

		case LOGGER:
			result.add( "logger" );
			break;

		case FILE:
			result.add( "file" );
			break;

		default:
			result.add( super.toString().toLowerCase());
			break;
		}

		return result;
	}


	/**
	 * Finds all the string representations for all the plug-ins.
	 * @return a non-null list
	 */
	public static List<String> allString() {

		List<String> result = new ArrayList<> ();
		for( SupportedPlugin st : SupportedPlugin.values())
			result.addAll( st.findString());

		return result;
	}


	/**
	 * Finds a supported plug-in from a string value.
	 * @param search a string (may be null)
	 * @return the matching plug-in, or null if none matched
	 */
	public static SupportedPlugin which( String search ) {

		SupportedPlugin result = null;
		for( SupportedPlugin st : values()) {
			for( String s : st.findString()) {
				if( s.equalsIgnoreCase( search )) {
					result = st;
					break;
				}
			}
		}

		return result;
	}


	/**
	 * Finds the commands to execute for a given plug-in.
	 * @param roboconfVersion Roboconf's version
	 * @return a non-null list of Karaf commands
	 */
	public List<String> findCommands( String roboconfVersion ) {

		List<String> result = new ArrayList<> ();
		switch( this ) {
		case SCRIPT:
			result.add( "bundle:install --start mvn:net.roboconf/roboconf-plugin-script/" + roboconfVersion );
			break;

		case PUPPET:
			result.add( "bundle:install --start mvn:net.roboconf/roboconf-plugin-puppet/" + roboconfVersion );
			break;

		case LOGGER:
			result.add( "bundle:install --start mvn:net.roboconf/roboconf-plugin-logger/" + roboconfVersion );
			break;

		case FILE:
			result.add( "bundle:install --start mvn:net.roboconf/roboconf-plugin-file/" + roboconfVersion );
			break;
		}

		return result;
	}
}
