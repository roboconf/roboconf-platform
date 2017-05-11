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

package net.roboconf.messaging.api.utils;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.extensions.MessagingContext.RecipientKind;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class MessagingUtils {

	/**
	 * Constructor.
	 */
	private MessagingUtils() {
		// nothing
	}


	/**
	 * Builds the default topic name for an agent.
	 * @param instance an instance managed by the agent
	 * @return a non-null string
	 */
	public static String buildTopicNameForAgent( Instance instance ) {
		Instance scopedInstance = InstanceHelpers.findScopedInstance( instance );
		return buildTopicNameForAgent( InstanceHelpers.computeInstancePath( scopedInstance ));
	}


	/**
	 * Builds the default topic name for an agent.
	 * @param scopedInstancePath the path of the (scoped) instance associated with the agent
	 * @return a non-null string
	 */
	public static String buildTopicNameForAgent( String scopedInstancePath ) {
		return "machine." + escapeInstancePath( scopedInstancePath );
	}


	/**
	 * Removes unnecessary slashes and transforms the others into dots.
	 * @param instancePath an instance path
	 * @return a non-null string
	 */
	public static String escapeInstancePath( String instancePath ) {

		String result;
		if( Utils.isEmptyOrWhitespaces( instancePath ))
			result = "";
		else
			result = instancePath.replaceFirst( "^/*", "" ).replaceFirst( "/*$", "" ).replaceAll( "/+", "." );

		return result;
	}


	/**
	 * Builds a string identifying a messaging client.
	 * @param ownerKind {@link RecipientKind#DM} or {@link RecipientKind#AGENTS}
	 * @param domain the domain
	 * @param applicationName the application name (only makes sense for agents)
	 * @param scopedInstancePath the scoped instance path  (only makes sense for agents)
	 * @return a non-null string
	 */
	public static String buildId(
			RecipientKind ownerKind,
			String domain,
			String applicationName,
			String scopedInstancePath ) {

		StringBuilder sb = new StringBuilder();
		sb.append( "[ " );
		sb.append( domain );
		sb.append( " ] " );

		if( ownerKind ==  RecipientKind.DM ) {
			sb.append( "DM" );
		} else {
			sb.append( scopedInstancePath );
			sb.append( " @ " );
			sb.append( applicationName );
		}

		return sb.toString();
	}
}
