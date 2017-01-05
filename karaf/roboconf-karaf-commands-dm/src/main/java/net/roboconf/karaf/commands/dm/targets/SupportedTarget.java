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

package net.roboconf.karaf.commands.dm.targets;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vincent Zurczak - Linagora
 */
public enum SupportedTarget {

	DOCKER, IN_MEMORY, OCCI, OPENSTACK, VMWARE, JCLOUDS, EC2, AZURE, EMBEDDED;


	/**
	 * Finds a list of string representations for this target.
	 * @return a non-null list
	 */
	public List<String> findString() {

		List<String> result = new ArrayList<> ();
		switch( this ) {
		case EC2:
			result.add( "ec2" );
			result.add( "aws" );
			result.add( "amazon-web-services" );
			break;

		case AZURE:
			result.add( "azure" );
			result.add( "microsoft-azure" );
			break;

		case IN_MEMORY:
			result.add( "in-memory" );
			break;

		default:
			result.add( super.toString().toLowerCase());
			break;
		}

		return result;
	}


	/**
	 * Finds all the string representations for all the targets.
	 * @return a non-null list
	 */
	public static List<String> allString() {

		List<String> result = new ArrayList<> ();
		for( SupportedTarget st : SupportedTarget.values())
			result.addAll( st.findString());

		return result;
	}


	/**
	 * Finds a supported target from a string value.
	 * @param search a string (may be null)
	 * @return the matching target, or null if none matched
	 */
	public static SupportedTarget which( String search ) {

		SupportedTarget result = null;
		for( SupportedTarget st : values()) {
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
	 * Finds the commands to execute for a given target.
	 * @param roboconfVersion Roboconf's version
	 * @return a non-null list of Karaf commands
	 */
	public List<String> findCommands( String roboconfVersion ) {

		List<String> result = new ArrayList<> ();
		switch( this ) {
		case OPENSTACK:
			result.add( "feature:install jclouds-for-roboconf" );
			result.add( "bundle:install --start mvn:net.roboconf/roboconf-target-iaas-openstack/" + roboconfVersion );
			break;

		case AZURE:
			result.add( "bundle:install --start mvn:net.roboconf/roboconf-target-iaas-azure/" + roboconfVersion );
			break;

		case DOCKER:
			result.add( "bundle:install --start mvn:net.roboconf/roboconf-target-docker/" + roboconfVersion );
			break;

		case EC2:
			result.add( "bundle:install --start mvn:net.roboconf/roboconf-target-iaas-ec2/" + roboconfVersion );
			break;

		case EMBEDDED:
			result.add( "bundle:install --start mvn:net.roboconf/roboconf-target-embedded/" + roboconfVersion );
			break;

		case IN_MEMORY:
			result.add( "bundle:install --start mvn:net.roboconf/roboconf-plugin-api/" + roboconfVersion );
			result.add( "bundle:install --start mvn:net.roboconf/roboconf-agent/" + roboconfVersion );
			result.add( "bundle:install --start mvn:net.roboconf/roboconf-target-in-memory/" + roboconfVersion );
			break;

		case JCLOUDS:
			result.add( "feature:install jclouds-for-roboconf" );
			result.add( "bundle:install --start mvn:net.roboconf/roboconf-target-jclouds/" + roboconfVersion );
			break;

		case VMWARE:
			result.add( "bundle:install --start mvn:net.roboconf/roboconf-target-iaas-vmware/" + roboconfVersion );
			break;

		case OCCI:
			result.add( "bundle:install --start mvn:net.roboconf/roboconf-target-iaas-occi/" + roboconfVersion );
			break;
		}

		return result;
	}
}
