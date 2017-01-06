/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.plugin.file.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.UriUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.plugin.api.PluginException;
import net.roboconf.plugin.api.PluginInterface;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PluginFile implements PluginInterface {

	public static final String PLUGIN_NAME = "file";
	static final String FILE_NAME = "instructions.properties";
	static final String TMP_FILE = "roboconf_tmp_file";

	private final Logger logger = Logger.getLogger( getClass().getName());
	private String agentId;



	@Override
	public String getPluginName() {
		return PLUGIN_NAME;
	}


	@Override
	public void setNames( String applicationName, String rootInstanceName ) {
		this.agentId = "'" + rootInstanceName + "' agent";
	}


	@Override
	public void initialize( Instance instance ) throws PluginException {
		this.logger.fine( this.agentId + " has nothing to initialize (file extension)." );
	}


	@Override
	public void deploy( Instance instance ) throws PluginException {
		this.logger.info( this.agentId + " is deploying instance " + instance + "." );
		execute( "deploy", instance );
	}


	@Override
	public void start( Instance instance ) throws PluginException {
		this.logger.info( this.agentId + " is starting instance " + instance + "." );
		execute( "start", instance );
	}


	@Override
	public void update( Instance instance, Import importChanged, InstanceStatus statusChanged ) throws PluginException {
		this.logger.info( this.agentId + " is updating instance " + instance + "." );
		execute( "update", instance );
	}


	@Override
	public void stop( Instance instance ) throws PluginException {
		this.logger.info( this.agentId + " is stopping instance " + instance + "." );
		execute( "stop", instance );
	}


	@Override
	public void undeploy( Instance instance ) throws PluginException {
		this.logger.info( this.agentId + " is undeploying instance " + instance + "." );
		execute( "undeploy", instance );
	}


	/**
	 *
	 * @param actionName
	 * @param instance
	 * @throws PluginException
	 */
	private void execute( String actionName, Instance instance ) throws PluginException {

		Properties props = readProperties( instance );
		for( Action action : findActions( actionName, props ))
			executeAction( action );
	}


	/**
	 * Reads the "instructions.properties" file.
	 * @param instance the instance
	 * @return a non-null properties object (potentially empty)
	 * @throws PluginException
	 */
	Properties readProperties( Instance instance ) throws PluginException {

		Properties result = null;
		File instanceDirectory = InstanceHelpers.findInstanceDirectoryOnAgent( instance );
		File file = new File( instanceDirectory, FILE_NAME );

		try {
			if( file.exists()) {
				result = Utils.readPropertiesFile( file );

			} else {
				this.logger.warning( file + " does not exist or is invalid. There is no instruction for the plugin." );
				result = new Properties();
			}

		} catch( IOException e ) {
			throw new PluginException( e );
		}

		return result;
	}


	/**
	 * Finds the actions to execute for a given step.
	 * @return a non-null list of actions (sorted in the right execution order)
	 */
	SortedSet<Action> findActions( String actionName, Properties properties ) {

		Pattern pattern = Pattern.compile( actionName + "\\.(\\d)+\\.(.*)", Pattern.CASE_INSENSITIVE );
		SortedSet<Action> result = new TreeSet<Action>( new ActionComparator());
		for( Map.Entry<Object,Object> entry : properties.entrySet()) {
			String key = String.valueOf( entry.getKey()).toLowerCase();
			Matcher m = pattern.matcher( key );
			if( ! m.matches())
				continue;

			int position = Integer.parseInt( m.group( 1 ));
			ActionType actionType = ActionType.which( m.group( 2 ));
			String parameter = String.valueOf( entry.getValue());
			result.add( new Action( position, actionType, parameter ));
		}

		return result;
	}


	/**
	 * Executes an action.
	 * @param action the action to execute
	 * @throws PluginException
	 */
	void executeAction( Action action ) throws PluginException {

		try {
			switch( action.actionType ) {
			case DELETE:
				this.logger.fine( "Deleting " + action.parameter + "..." );
				File f = new File( action.parameter );
				Utils.deleteFilesRecursively( f );
				break;

			case DOWNLOAD:
				this.logger.fine( "Downloading " + action.parameter + "..." );
				URI uri = UriUtils.urlToUri( action.parameter );
				File targetFile = new File( System.getProperty( "java.io.tmpdir" ), TMP_FILE );
				InputStream in = null;
				try {
					in = uri.toURL().openStream();
					Utils.copyStream( in, targetFile );

				} finally {
					Utils.closeQuietly( in );
				}

				break;

			case MOVE:
				List<String> parts = Utils.splitNicely( action.parameter, "->" );
				if( parts.size() != 2 ) {
					this.logger.warning( "Invalid syntax for 'move' action. " + action.parameter );
				} else {
					File source = new File( parts.get( 0 ));
					File target = new File( parts.get( 1 ));
					this.logger.fine( "Moving " + source + " to " + target + "..." );
					if( ! source.renameTo( target ))
						throw new IOException( source + " could not be moved to " + target );
				}

				break;

			case COPY:
				parts = Utils.splitNicely( action.parameter, "->" );
				if( parts.size() != 2 ) {
					this.logger.warning( "Invalid syntax for 'copy' action. " + action.parameter );
				} else {
					File source = new File( parts.get( 0 ));
					File target = new File( parts.get( 1 ));
					this.logger.fine( "Copying " + source + " to " + target + "..." );
					Utils.copyStream( source, target );
				}

				break;

			default:
				this.logger.fine( "Ignoring the action..." );
				break;
			}

		} catch( Exception e ) {
			throw new PluginException( e );
		}
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	static class Action {
		int position;
		String parameter;
		ActionType actionType;

		/**
		 * Constructor.
		 * @param position
		 * @param actionType
		 * @param parameter
		 */
		public Action( int position, ActionType actionType, String parameter ) {
			this.position = position;
			this.actionType = actionType;
			this.parameter = parameter;
		}


		@Override
		public boolean equals( Object obj ) {
			return obj instanceof Action
					&& this.position == ((Action) obj).position
					&& this.actionType == ((Action) obj).actionType;
		}


		@Override
		public int hashCode() {
			return this.actionType == null ? 23 : this.actionType.toString().hashCode();
		}
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	private static class ActionComparator implements Serializable, Comparator<Action> {
		private static final long serialVersionUID = 6157709801342723302L;

		@Override
		public int compare( Action a1, Action a2 ) {
			return a1.position - a2.position;
		}
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	static enum ActionType {
		NOTHING, DOWNLOAD, MOVE, COPY, DELETE;

		/**
		 * A case-insensitive version of {@link #valueOf(String)}.
		 * @param s any string
		 * @return an action type, {@value #NOTHING} by default
		 */
		public static ActionType which( String s ) {

			ActionType result = NOTHING;
			for( ActionType at : values()) {
				if( at.toString().equalsIgnoreCase( s )) {
					result = at;
					break;
				}
			}

			return result;
		}
	}
}
