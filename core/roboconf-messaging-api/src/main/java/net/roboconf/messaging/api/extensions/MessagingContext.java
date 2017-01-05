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

package net.roboconf.messaging.api.extensions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.utils.Utils;

/**
 * A context that identifies messages recipients and topic names.
 * <p>
 * Three properties are part of this class.<br />
 * The kind identifies the message recipient. It can either be the DM,
 * or agents or inter-application exchanges. The application name indicates
 * an application scope for the listeners. And the topic name indicates more
 * precise bindings. For agents, the topic name can be defined from a component
 * or facet name and a "direction".
 * </p>
 * <p>
 * When using the {@link RecipientKind #DM} kind, the topic name is ignored. The application
 * name is kept to be reused as a topic name. In terms of messaging, there is only one DM.
 * Even if we had to deploy several DM (for any reason), DM messages should be routed to all of them.
 * </p>
 * <p>
 * Using the {@link RecipientKind #AGENTS} kind means we target a specific topic name
 * in a given application. Both the topic name and the application name matter. Generally,
 * we use an indirection to build the topic name. It is based on a component or facet name
 * and a "direction" (target those that import a variable, or those that export it).
 * </p>
 * <p>
 * Eventually, the {@link RecipientKind #INTER_APP} kind means exchanges between agents
 * but without application scope (it is ignored). Only the topic name matters. We could have
 * implemented this kind with the AGENTS one and a null application name. But having a distinct
 * value for this use case will simplify debug. Besides, inter-application dependencies are
 * a real Roboconf feature. It is not only a view of the messaging.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class MessagingContext implements Serializable {

	private static final long serialVersionUID = 5529159467155629784L;

	private final RecipientKind kind;
	private final String domain, componentOrFacetName, applicationName;
	private final ThoseThat thoseThat;


	/**
	 * Constructor.
	 * @param kind the recipient kind
	 * @param domain the domain
	 * @param applicationName an application name
	 */
	public MessagingContext( RecipientKind kind, String domain, String applicationName ) {
		this( kind, domain, null, null, applicationName );
	}


	/**
	 * Constructor.
	 * @param kind the recipient kind
	 * @param domain the domain
	 * @param topicName a topic name
	 * @param applicationName an application name
	 */
	public MessagingContext( RecipientKind kind, String domain, String topicName, String applicationName ) {
		this( kind, domain, topicName, null, applicationName );
	}


	/**
	 * Constructor.
	 * <p>
	 * This is a convenience constructor that builds a topic name
	 * from a component or facet name and an agent direction. This constructor
	 * should be used when sending messages to agents.
	 * </p>
	 *
	 * @param kind the recipient kind
	 * @param domain the domain
	 * @param componentOrFacetName the component or facet name
	 * @param thoseThat do we target "those that export" or "those that import" <code>componentOrFacetName</code>?
	 * @param applicationName an application name
	 */
	public MessagingContext( RecipientKind kind, String domain, String componentOrFacetName, ThoseThat thoseThat, String applicationName ) {
		this.kind = kind;
		this.domain = domain;

		if( kind == RecipientKind.DM ) {
			// The application name will be used to build a topic name.
			this.componentOrFacetName = null;
			this.applicationName = applicationName;
			this.thoseThat = null;

		} else if( kind == RecipientKind.INTER_APP ) {
			this.componentOrFacetName = componentOrFacetName;
			this.thoseThat = thoseThat;
			this.applicationName = null;

		} else {
			this.thoseThat = thoseThat;
			this.componentOrFacetName = componentOrFacetName;
			this.applicationName = applicationName;
		}
	}


	public RecipientKind getKind() {
		return this.kind;
	}


	public String getApplicationName() {
		return this.applicationName;
	}


	public String getComponentOrFacetName() {
		return this.componentOrFacetName;
	}


	public ThoseThat getAgentDirection() {
		return this.thoseThat;
	}


	public String getDomain() {
		return this.domain;
	}


	/**
	 * @return a topic name, or an empty string if none was specified
	 */
	public String getTopicName() {

		StringBuilder sb = new StringBuilder();
		if( this.kind == RecipientKind.DM ) {
			if( this.applicationName != null )
				sb.append( this.applicationName );

		} else {
			if( this.thoseThat != null )
				sb.append( this.thoseThat );

			if( this.componentOrFacetName != null )
				sb.append( this.componentOrFacetName );
		}

		return sb.toString();
	}


	@Override
	public int hashCode() {

		String topicName = getTopicName();
		int backup = this.kind.hashCode();
		return Utils.isEmptyOrWhitespaces( topicName ) ? backup : topicName.hashCode();
	}


	@Override
	public boolean equals( Object obj ) {
		return obj instanceof MessagingContext
				&& this.kind == ((MessagingContext ) obj).kind
				&& this.thoseThat == ((MessagingContext ) obj).thoseThat
				&& Objects.equals( this.componentOrFacetName, ((MessagingContext ) obj).componentOrFacetName )
				&& Objects.equals( this.applicationName, ((MessagingContext ) obj).applicationName );
	}


	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder( getTopicName());
		if( this.applicationName != null
				&& this.kind != RecipientKind.DM ) {
			sb.append( " @ " );
			sb.append( this.applicationName );
		}

		sb.append( " (" );
		sb.append( this.kind );
		sb.append( ")" );

		return sb.toString().trim();
	}


	/**
	 * Builds a list of messaging contexts.
	 * @param domain the domain
	 * @param applicationName the name of the agent's application
	 * @param instance the current instance
	 * @param thoseThat whether we target "those that import" or "those that export"
	 * @return a non-null list
	 */
	public static Collection<MessagingContext> forImportedVariables(
			String domain,
			String applicationName,
			Instance instance,
			ThoseThat thoseThat ) {

		Map<String,MessagingContext> result = new HashMap<> ();
		for( ImportedVariable var : ComponentHelpers.findAllImportedVariables( instance.getComponent()).values()) {
			String componentOrApplicationTemplateName = VariableHelpers.parseVariableName( var.getName()).getKey();
			if( result.containsKey( componentOrApplicationTemplateName ))
				continue;

			// When we import a variable, it is either internal or external, but not both!
			RecipientKind kind = var.isExternal() ? RecipientKind.INTER_APP : RecipientKind.AGENTS;
			MessagingContext ctx = new MessagingContext( kind, domain, componentOrApplicationTemplateName, thoseThat, applicationName );
			result.put( componentOrApplicationTemplateName, ctx );
		}

		return result.values();
	}


	/**
	 * Builds a list of messaging contexts.
	 * @param domain the domain
	 * @param applicationName the name of the agent's application
	 * @param instance the current instance
	 * @param externalExports a non-null map that associates internal exported variables with global ones
	 * @param thoseThat whether we target "those that import" or "those that export"
	 * @return a non-null list
	 */
	public static List<MessagingContext> forExportedVariables(
			String domain,
			String applicationName,
			Instance instance,
			Map<String,String> externalExports,
			ThoseThat thoseThat ) {

		List<MessagingContext> result = new ArrayList<> ();

		// For inter-app messages, the real question is about whether we need
		// to create a context for the application template.
		Set<String> externalExportPrefixes = new HashSet<> ();
		for( String varName : externalExports.keySet()) {
			String prefix = VariableHelpers.parseVariableName( varName ).getKey();
			externalExportPrefixes.add( prefix );
		}

		// Internal variables
		boolean publishExternal = false;
		for( String facetOrComponentName : VariableHelpers.findPrefixesForExportedVariables( instance )) {
			MessagingContext ctx = new MessagingContext( RecipientKind.AGENTS, domain, facetOrComponentName, thoseThat, applicationName );
			result.add( ctx );

			if( externalExportPrefixes.contains( facetOrComponentName ))
				publishExternal = true;
		}

		// External variables - they all have the same prefix, the application template's name
		if( publishExternal ) {
			String varName = externalExports.values().iterator().next();
			String prefix = VariableHelpers.parseVariableName( varName ).getKey();

			// We indicate the application name, but it will most likely not be used
			// for inter-application messages.
			MessagingContext ctx = new MessagingContext( RecipientKind.INTER_APP, domain, prefix, thoseThat, applicationName );
			result.add( ctx );
		}

		return result;
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static enum RecipientKind {
		INTER_APP, DM, AGENTS;
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static enum ThoseThat {
		EXPORT( "those.that.export." ),
		IMPORT( "those.that.import." );

		private String string;


		/**
		 * Constructor.
		 * @param string
		 */
		private ThoseThat( String string ) {
			this.string = string;
		}


		@Override
		public String toString() {
			return this.string;
		}
	}
}
