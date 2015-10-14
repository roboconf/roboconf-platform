/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.rabbitmq.internal.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class MessagingContext {

	public static final String INTER_APP = "roboconf.inter-app";
	private final String exchangeName, routingKeySuffix;


	/**
	 * Private constructor.
	 * @param exchangeName
	 * @param routingKeySuffix
	 */
	private MessagingContext( String exchangeName, String routingKeySuffix ) {
		this.exchangeName = exchangeName;
		this.routingKeySuffix = routingKeySuffix;
	}

	/**
	 * @return the exchangeName
	 */
	public String getExchangeName() {
		return this.exchangeName == null ? INTER_APP : this.exchangeName;
	}

	/**
	 * @return the routingKeySuffix
	 */
	public String getRoutingKeySuffix() {
		return this.routingKeySuffix;
	}


	@Override
	public String toString() {

		String s = this.routingKeySuffix;
		if( this.exchangeName == null )
			s += " (external)";

		return s;
	}


	/**
	 * Builds a list of messaging contexts.
	 * @param applicationName the name of the agent's application
	 * @param instance the current instance
	 * @return a non-null list
	 */
	public static Collection<MessagingContext> forImportedVariables( String applicationName, Instance instance ) {

		Map<String,MessagingContext> result = new HashMap<> ();
		for( ImportedVariable var : ComponentHelpers.findAllImportedVariables( instance.getComponent()).values()) {
			String componentOrApplicationTemplateName = VariableHelpers.parseVariableName( var.getName()).getKey();
			if( result.containsKey( componentOrApplicationTemplateName ))
				continue;

			MessagingContext ctx;
			if( var.isExternal()) {
				String exchangeName = null;
				ctx = new MessagingContext( exchangeName, componentOrApplicationTemplateName );

			} else {
				String exchangeName = RabbitMqUtils.buildExchangeName( applicationName, false );
				ctx = new MessagingContext( exchangeName, componentOrApplicationTemplateName );
			}

			result.put( componentOrApplicationTemplateName, ctx );
		}

		return result.values();
	}


	/**
	 * Builds a list of messaging contexts.
	 * @param applicationName the name of the agent's application
	 * @param instance the current instance
	 * @param externalExports a non-null map that associates internal exported variables with global ones
	 * @return a non-null list
	 */
	public static List<MessagingContext> forExportedVariables( String applicationName, Instance instance, Map<String,String> externalExports ) {

		List<MessagingContext> result = new ArrayList<> ();

		// Internal variables
		for( String facetOrComponentName : VariableHelpers.findPrefixesForExportedVariables( instance )) {
			String exchangeName = RabbitMqUtils.buildExchangeName( applicationName, false );
			MessagingContext ctx = new MessagingContext( exchangeName, facetOrComponentName );
			result.add( ctx );
		}

		// External variables - they all have the same prefix, the application template's name
		if( ! externalExports.isEmpty()) {
			String varName = externalExports.values().iterator().next();
			String prefix = VariableHelpers.parseVariableName( varName ).getKey();

			MessagingContext ctx = new MessagingContext( null, prefix );
			result.add( ctx );
		}

		return result;
	}
}
