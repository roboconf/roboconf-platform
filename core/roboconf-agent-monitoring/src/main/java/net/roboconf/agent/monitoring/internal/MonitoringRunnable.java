/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.agent.monitoring.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.agent.AgentMessagingInterface;
import net.roboconf.agent.monitoring.api.IMonitoringHandler;
import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.api.messages.Message;

/**
 * Runnable for periodic monitoring checks (polling).
 * @author Pierre-Yves Gibello - Linagora
 */
public class MonitoringRunnable implements Runnable {

	private static final String COMMENT_DELIMITER = "#";
	static final String RULE_BEGINNING = "[event";
	static final String EVENT_PATTERN = "\\" + RULE_BEGINNING + "\\s+(\\S+)\\s+(\\S+)\\s*\\]";

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final List<IMonitoringHandler> handlers;
	private final AgentMessagingInterface agentInterface;
	private final Pattern eventPattern;
	private boolean handlersWereInitialized = false;


	/**
	 * Constructor.
	 * @param agentInterface the interface to access the agent
	 * @param handlers the monitoring handlers
	 */
	public MonitoringRunnable( AgentMessagingInterface agentInterface, List<IMonitoringHandler> handlers ) {
		this.agentInterface = agentInterface;
		this.handlers = handlers;
		this.eventPattern = Pattern.compile( EVENT_PATTERN, Pattern.CASE_INSENSITIVE );
	}


	@Override
	public void run() {
		this.logger.fine( "Monitoring Task is being invoked." );

		// Root Instance may not yet have been injected: skip!
		if( this.agentInterface.getScopedInstance() == null) {
			this.logger.fine( "The agent's model has not yet been initialized. Monitoring cannot work yet." );
			return;
		}

		// Update the handlers
		if( ! this.handlersWereInitialized ) {
			for( IMonitoringHandler handler : this.handlers ) {
				String scopedInstancePath = InstanceHelpers.computeInstancePath( this.agentInterface.getScopedInstance());
				handler.setAgentId( this.agentInterface.getApplicationName(), scopedInstancePath );
			}

			this.handlersWereInitialized = true;
		}

		// Otherwise, check all the instances
		for( Instance inst : InstanceHelpers.buildHierarchicalList( this.agentInterface.getScopedInstance())) {

			// Non started ones are skipped
			if( inst.getStatus() != InstanceStatus.DEPLOYED_STARTED )
				continue;

			File dir = InstanceHelpers.findInstanceDirectoryOnAgent( inst );
			File measureFile = new File( dir, inst.getComponent().getName() + Constants.FILE_EXT_MEASURES );
			if( ! measureFile.exists())
				continue;

			// Read the file content
			this.logger.fine( "A file with measure rules was found for instance '" + inst + "'." );
			String fileContent;
			try {
				fileContent = Utils.readFileContent( measureFile );

			} catch( IOException e ) {
				this.logger.warning( "A problem occurred while reading the content for measure rules of instance '"+ inst + "'." );
				Utils.logException( this.logger, e );
				continue;
			}

			File paramFile;
			Properties params;
			try {
				paramFile = new File( dir, inst.getComponent().getName() + Constants.FILE_EXT_MEASURES + ".properties" );
				params = Utils.readPropertiesFile( paramFile );
				this.logger.fine( "A file with measure parameters (properties) was found for instance '" + inst + "'." );

			} catch( IOException e1 ) {
				params = null;
			}

			// Find the right handlers to process the rules
			for( MonitoringHandlerRun bean : extractRuleSections( measureFile, fileContent, params )) {
				try {
					IMonitoringHandler handler = findHandlerByName( bean.handlerName );
					if( handler == null ) {
						this.logger.warning( "No handler was found with the ID '" + bean.handlerName + "'. The rule is skipped." );
						continue;
					}

					handler.reset( inst, bean.eventId, bean.rawRulesText );
					Message msg = handler.process();
					if( msg != null )
						this.agentInterface.getMessagingClient().sendMessageToTheDm( msg );

				} catch( IOException e ) {
					this.logger.warning( "A problem occurred while the agent monitoring was sending a message to the DM. " + e.getMessage());
					Utils.logException( this.logger, e );
				}
			}
		}
	}


	/**
	 * Reads the file content, extracts rules sections and prepares the handler invocations.
	 * @param file
	 * @param fileContent
	 * @param params
	 * @return a non-null list of handler parameters (may be empty)
	 */
	List<MonitoringHandlerRun> extractRuleSections( File file, String fileContent, Properties params ) {

		// Find rules sections
		StringBuilder sb = new StringBuilder();
		List<String> sections = new ArrayList<String>();
		for( String s : Arrays.asList( fileContent.trim().split( "\n" ))) {
			s = s.trim();
			if( s.length() == 0
					|| s.startsWith( COMMENT_DELIMITER ))
				continue;

			if( s.toLowerCase().startsWith( RULE_BEGINNING )) {
				addSectionIfNotEmpty(sections, sb.toString());
				sb.setLength( 0 );
			}

			sb.append( Utils.expandTemplate(s, params) + "\n" );
		}

		addSectionIfNotEmpty(sections, sb.toString());

		// Now, prepare handler invocations
		List<MonitoringHandlerRun> result = new ArrayList<MonitoringHandlerRun> ();
		for( String s : sections ) {

			Matcher m = this.eventPattern.matcher( s );
			if( ! m.find())
				continue;

			s = s.substring( m.end()).trim();
			MonitoringHandlerRun bean = new MonitoringHandlerRun();
			bean.handlerName = m.group( 1 );
			bean.eventId = m.group( 2 );
			bean.rawRulesText = s;

			result.add( bean );
		}

		return result;
	}


	private void addSectionIfNotEmpty(List<String> sections, String section) {
		if( ! Utils.isEmptyOrWhitespaces( section ))
			sections.add(section);
	}


	private IMonitoringHandler findHandlerByName( String name ) {

		IMonitoringHandler result = null;
		for( Iterator<IMonitoringHandler> it = this.handlers.iterator(); it.hasNext() && result == null; ) {
			IMonitoringHandler curr = it.next();
			if( name.equalsIgnoreCase( curr.getName()))
				result = curr;
		}

		return result;
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	static class MonitoringHandlerRun {
		public String handlerName, eventId, rawRulesText;
	}
}
