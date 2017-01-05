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

package net.roboconf.tooling.core.autocompletion;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.Constants;
import net.roboconf.core.dsl.ParsingConstants;
import net.roboconf.core.dsl.converters.FromGraphDefinition;
import net.roboconf.core.model.beans.AbstractType;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.tooling.core.autocompletion.ICompletionProposer.RoboconfCompletionProposal;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class CompletionUtils {

	public static final String DEFAULT_VALUE = "Default value: ";
	public static final String SET_BY_ROBOCONF = "Value set dynamically by Roboconf";
	public static final String IMPORT_ALL_THE_VARIABLES = "Import all the variables";


	/**
	 * Private empty constructor.
	 */
	private CompletionUtils() {
		// nothing
	}


	/**
	 * Finds all the graph files that can be imported.
	 * @param appDirectory the application's directory
	 * @param editedFile the graph file that is being edited
	 * @param fileContent the file content (not null)
	 * @return a non-null set of (relative) file paths
	 */
	public static Set<String> findGraphFilesToImport( File appDirectory, File editedFile, String fileContent ) {

		File graphDir = new File( appDirectory, Constants.PROJECT_DIR_GRAPH );
		return findFilesToImport( graphDir, Constants.FILE_EXT_GRAPH, editedFile, fileContent );
	}


	/**
	 * Finds all the instances files that can be imported.
	 * @param appDirectory the application's directory
	 * @param editedFile the graph file that is being edited
	 * @param fileContent the file content (not null)
	 * @return a non-null set of (relative) file paths
	 */
	public static Set<String> findInstancesFilesToImport( File appDirectory, File editedFile, String fileContent ) {

		File instancesDir = new File( appDirectory, Constants.PROJECT_DIR_INSTANCES );
		return findFilesToImport( instancesDir, Constants.FILE_EXT_INSTANCES, editedFile, fileContent );
	}


	/**
	 * Finds all the files that can be imported.
	 * @param searchDirectory the search's directory
	 * @param fileExtension the file extension to search for
	 * @param editedFile the graph file that is being edited
	 * @param fileContent the file content (not null)
	 * @return a non-null set of (relative) file paths
	 */
	static Set<String> findFilesToImport(
			File searchDirectory,
			String fileExtension,
			File editedFile,
			String fileContent ) {

		// Find all the files
		Set<String> result = new TreeSet<> ();
		if( searchDirectory.exists()) {

			for( File f : Utils.listAllFiles( searchDirectory, fileExtension )) {
				if( f.equals( editedFile ))
					continue;

				String path = Utils.computeFileRelativeLocation( searchDirectory, f );
				result.add( path );
			}
		}

		// Remove those that are already imported
		Pattern importPattern = Pattern.compile( "\\b" + ParsingConstants.KEYWORD_IMPORT + "\\s+(.*)", Pattern.CASE_INSENSITIVE );
		Matcher m = importPattern.matcher( fileContent );
		while( m.find()) {
			String imp = m.group( 1 ).trim();
			if( imp.endsWith( ";" ))
				imp = imp.substring( 0, imp.length() - 1 );

			result.remove( imp.trim());
		}

		return result;
	}


	/**
	 * A convenience method to shorten the creation of a basic proposal.
	 * @param s
	 * @param lastWord
	 * @param trim
	 * @return a non-null proposal
	 */
	public static RoboconfCompletionProposal basicProposal( String s, String lastWord, boolean trim ) {
		return new RoboconfCompletionProposal( s, trim ? s.trim() : s, null, lastWord.length());
	}


	/**
	 * A convenience method to shorten the creation of a basic proposal.
	 * <p>
	 * Equivalent to <code>basicProposal( s, lastWord, false )</code>.
	 * </p>
	 *
	 * @param s
	 * @param lastWord
	 * @return a non-null proposal
	 */
	public static RoboconfCompletionProposal basicProposal( String s, String lastWord ) {
		return basicProposal( s, lastWord, false );
	}


	/**
	 * @param s (not null)
	 * @param prefix (not null)
	 * @return true if s starts with prefix (case insensitively)
	 */
	public static boolean startsWith( String s, String prefix ) {
		return s.toLowerCase().startsWith( prefix.toLowerCase());
	}


	/**
	 * Finds all the Roboconf types.
	 * @param appDirectory the application's directory (can be null)
	 * @return a non-null map of types (key = type name, value = type)
	 */
	public static Map<String,RoboconfTypeBean> findAllTypes( File appDirectory ) {

		List<File> graphFiles = new ArrayList<> ();
		File graphDirectory = appDirectory;
		if( graphDirectory != null
				&& graphDirectory.exists())
			graphFiles = Utils.listAllFiles( graphDirectory, Constants.FILE_EXT_GRAPH );

		Map<String,RoboconfTypeBean> result = new HashMap<> ();
		for( File f : graphFiles ) {
			try {
				FromGraphDefinition converter = new FromGraphDefinition( appDirectory, true );
				Graphs g = converter.buildGraphs( f );

				Collection<AbstractType> types = new ArrayList<> ();
				types.addAll( ComponentHelpers.findAllComponents( g ));
				types.addAll( g.getFacetNameToFacet().values());

				for( AbstractType type : types ) {
					RoboconfTypeBean bean = new RoboconfTypeBean(
							type.getName(),
							converter.getTypeAnnotations().get( type.getName()),
							type instanceof Facet );

					result.put( type.getName(), bean );
					for( Map.Entry<String,String> entry : ComponentHelpers.findAllExportedVariables( type ).entrySet()) {
						bean.exportedVariables.put( entry.getKey(), entry.getValue());
					}
				}

			} catch( Exception e ) {
				Logger logger = Logger.getLogger( CompletionUtils.class.getName());
				Utils.logException( logger, e );
			}
		}

		return result;
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public final static class RoboconfTypeBean {

		public final Map<String,String> exportedVariables = new TreeMap<> ();
		private final String name, description;
		private final boolean facet;


		/**
		 * Constructor.
		 * @param name
		 * @param description
		 * @param facet
		 */
		private RoboconfTypeBean( String name, String description, boolean facet ) {
			this.name = name;
			this.description =  description;
			this.facet = facet;
		}

		public String getName() {
			return this.name;
		}

		public String getDescription() {
			return this.description;
		}

		public boolean isFacet() {
			return this.facet;
		}
	}


	/**
	 * Resolves the description to show for an exported variable.
	 * @param variableName a non-null variable name
	 * @param defaultValue the default value (can be null)
	 * @return a description (can be null)
	 */
	public static String resolveStringDescription( String variableName, String defaultValue ) {

		String result = null;
		if( Constants.SPECIFIC_VARIABLE_IP.equalsIgnoreCase( variableName )
				|| variableName.toLowerCase().endsWith( "." + Constants.SPECIFIC_VARIABLE_IP ))
			result = SET_BY_ROBOCONF;
		else if( ! Utils.isEmptyOrWhitespaces( defaultValue ))
			result = DEFAULT_VALUE + defaultValue;

		return result;
	}


	/**
	 * Find exported variables from raw graph files (including malformed ones).
	 * * @param appDirectory the application's directory (can be null)
	 * @return a non-null map of exported variables (key = name, value = default value)
	 */
	public static Map<String,String> findAllExportedVariables( File appDirectory ) {

		// TreeMap: keys are sorted alphabetically.
		Map<String,String> result = new TreeMap<> ();
		for( RoboconfTypeBean type : findAllTypes( appDirectory ).values()) {
			if( type.exportedVariables.size() > 0 )
				result.put( type.getName() + ".*", IMPORT_ALL_THE_VARIABLES );

			for( Map.Entry<String,String> entry : type.exportedVariables.entrySet()) {
				String desc = resolveStringDescription( entry.getKey(), entry.getValue());
				result.put( entry.getKey(), desc );
			}
		}

		return result;
	}


	/**
	 * Find type names from raw graph files (including malformed ones).
	 * @param appDirectory the application's directory (can be null)
	 * @param includeFacets true to include facet names in the result
	 * @param includeComponents true to include component names in the result
	 * @return a non-null map (key = type name, value = description)
	 */
	public static Map<String,String> findTypeNames( File appDirectory, boolean includeFacets, boolean includeComponents ) {

		// TreeMap: keys are sorted alphabetically.
		Map<String,String> result = new TreeMap<> ();
		for( RoboconfTypeBean type : findAllTypes( appDirectory ).values()) {

			if( includeFacets && type.isFacet())
				result.put( type.getName(), type.getDescription());

			if( includeComponents && ! type.isFacet())
				result.put( type.getName(), type.getDescription());
		}

		return result;
	}


	/**
	 * Builds proposals from a map (key = replacement, value = description).
	 * @param viewer the viewer
	 * @param candidates a non-null list of candidates
	 * @param prefix a non-null prefix
	 * @param offset the viewer's offset
	 * @return a non-null list of proposals
	 */
	public static List<RoboconfCompletionProposal> buildProposalsFromMap( Map<String,String> candidates, String prefix ) {

		List<RoboconfCompletionProposal> result = new ArrayList<> ();
		for( Map.Entry<String,String> entry : candidates.entrySet()) {

			// Matching candidate?
			String candidate = entry.getKey();
			if( ! startsWith( candidate, prefix ))
				continue;

			// No description => basic proposal
			if( Utils.isEmptyOrWhitespaces( entry.getValue())) {
				result.add( basicProposal( candidate, prefix ));
			}

			// Otherwise, show the description
			else {
				result.add( new RoboconfCompletionProposal(
						candidate,
						candidate,
						entry.getValue(),
						prefix.length()));
			}
		}

		return result;
	}
}
