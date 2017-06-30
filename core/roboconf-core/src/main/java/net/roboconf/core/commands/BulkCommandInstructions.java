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

package net.roboconf.core.commands;

import static net.roboconf.core.errors.ErrorDetails.instance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.helpers.ComponentHelpers;

/**
 * @author Vincent Zurczak - Linagora
 */
public class BulkCommandInstructions extends AbstractCommandInstruction {

	private static final String ALL = "\\s+instances\\s+of";

	private String instancePath, componentName;
	private ChangeStateInstruction changeStateInstruction;


	/**
	 * Constructor.
	 * @param context
	 * @param changeStateInstruction
	 * @param line
	 */
	BulkCommandInstructions( Context context, String instruction, int line ) {
		super( context, instruction, line );

		Matcher m = getPatternForInstancePath().matcher( instruction );
		if( m.matches()) {
			this.syntaxicallyCorrect = true;
			this.instancePath = m.group( 2 ).trim();
			this.changeStateInstruction = ChangeStateInstruction.which( m.group( 1 ).trim());

		} else if(( m = getPatternForComponentName().matcher( instruction )).matches()) {
			this.syntaxicallyCorrect = true;
			this.componentName = m.group( 2 ).trim();
			this.changeStateInstruction = ChangeStateInstruction.which( m.group( 1 ).trim());
		}
	}


	/**
	 * @param line a non-null string
	 * @return true if it matches a supported changeStateInstruction, false otherwise
	 */
	public static boolean isBulkInstruction( String line ) {

		boolean match = false;
		Pattern[] patterns = {
				getPatternForInstancePath(),
				getPatternForComponentName()
		};

		for( Pattern p : patterns ) {
			Matcher m = p.matcher( line );
			if( m.matches()
					&& ChangeStateInstruction.which( m.group( 1 ).trim()) != null ) {
				match = true;
				break;
			}
		}

		return match;
	}


	/**
	 * @return a pattern to recognize instructions that update a given instance
	 */
	private static Pattern getPatternForInstancePath() {
		return Pattern.compile( "([^/]+)(/.*)", Pattern.CASE_INSENSITIVE );
	}


	/**
	 * @return a pattern to recognize instructions that update instances of a given component
	 */
	private static Pattern getPatternForComponentName() {
		return Pattern.compile( "(.+)" + ALL + "(.+)", Pattern.CASE_INSENSITIVE );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.core.commands.AbstractCommandInstruction#doValidate()
	 */
	@Override
	public List<ParsingError> doValidate() {

		List<ParsingError> result = new ArrayList<> ();
		if( this.changeStateInstruction == null )
			result.add( error( ErrorCode.CMD_UNRECOGNIZED_INSTRUCTION ));

		if( this.instancePath != null
				&& ! this.context.instanceExists( this.instancePath ))
			result.add( error( ErrorCode.CMD_NO_MATCHING_INSTANCE, instance( this.instancePath )));

		if( this.componentName != null
				&& ComponentHelpers.findComponent( this.context.getApp(), this.componentName ) == null )
			result.add( error( ErrorCode.CMD_INEXISTING_COMPONENT, instance( this.instancePath )));

		return result;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.core.commands.AbstractCommandInstruction#updateContext()
	 */
	@Override
	public void updateContext() {

		if( this.changeStateInstruction == ChangeStateInstruction.DELETE ) {

			// Find the path of the instances to remove
			Set<String> pathOfInstancesToRemove = new HashSet<> ();
			if( this.instancePath != null ) {
				pathOfInstancesToRemove.add( this.instancePath );

			} else for( Map.Entry<String,String> entry : this.context.instancePathToComponentName.entrySet()) {
				if( this.componentName.equals( entry.getValue()))
					pathOfInstancesToRemove.add( entry.getKey());
			}

			// Now, delete the instances that need to eb erased
			List<String> keys = new ArrayList<>( this.context.instancePathToComponentName.keySet());
			for( String path : keys ) {
				for( String pathToRemove : pathOfInstancesToRemove ) {

					if( path.equals( pathToRemove )
							|| path.startsWith( pathToRemove + "/" ))
						this.context.instancePathToComponentName.remove( path );
				}
			}
		}
	}


	/**
	 * @return the instancePath
	 */
	public String getInstancePath() {
		return this.instancePath;
	}


	/**
	 * @return the componentName
	 */
	public String getComponentName() {
		return this.componentName;
	}


	/**
	 * @return the changeStateInstruction
	 */
	public ChangeStateInstruction getChangeStateInstruction() {
		return this.changeStateInstruction;
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static enum ChangeStateInstruction {

		DELETE,
		DEPLOY_AND_START_ALL,
		STOP_ALL,
		UNDEPLOY_ALL;


		/*
		 * (non-Javadoc)
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return super.toString().replace( '_', ' ' ).toLowerCase();
		}


		/**
		 * @param s a string (can be null)
		 * @return null if no changeStateInstruction was recognized, or an changeStateInstruction otherwise
		 */
		public static ChangeStateInstruction which( String s ) {

			ChangeStateInstruction result = null;
			for( ChangeStateInstruction elt : ChangeStateInstruction.values()) {
				if( elt.toString().equalsIgnoreCase( s )) {
					result = elt;
					break;
				}

				// "Delete all" => "delete"
				if(( elt.toString() + " all" ).equalsIgnoreCase( s )) {
					result = elt;
					break;
				}
			}

			return result;
		}
	}
}
