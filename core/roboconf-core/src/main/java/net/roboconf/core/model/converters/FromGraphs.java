/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.model.converters;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.roboconf.core.Constants;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.model.parsing.AbstractBlock;
import net.roboconf.core.model.parsing.BlockBlank;
import net.roboconf.core.model.parsing.BlockComment;
import net.roboconf.core.model.parsing.BlockComponent;
import net.roboconf.core.model.parsing.BlockFacet;
import net.roboconf.core.model.parsing.BlockProperty;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;
import net.roboconf.core.utils.Utils;

/**
 * To dump a {@link Graphs} into a file.
 * @author Vincent Zurczak - Linagora
 */
public class FromGraphs {

	private final Map<String,BlockFacet> facets = new HashMap<String,BlockFacet> ();


	/**
	 * Builds a file definition from a graph.
	 * @param graphs the graphs
	 * @param targetFile the target file (will not be written)
	 * @param addComment true to insert generated comments
	 * @return a non-null file definition
	 */
	public FileDefinition buildFileDefinition( Graphs graphs, File targetFile, boolean addComment ) {

		// Build the global structure
		FileDefinition result = new FileDefinition( targetFile );
		result.setFileType( FileDefinition.GRAPH );
		if( addComment ) {
			String s = "# File created from an in-memory model,\n# without a binding to existing files.";
			BlockComment initialComment = new BlockComment( result, s );
			result.getBlocks().add( initialComment );
			result.getBlocks().add( new BlockBlank( result, "\n" ));
		}

		// Process and serialize the components
		Set<String> alreadySerializedComponentNames = new HashSet<String> ();
		ArrayList<Component> toProcess = new ArrayList<Component> ();
		toProcess.addAll( graphs.getRootComponents());

		while( ! toProcess.isEmpty()) {
			Component c = toProcess.iterator().next();
			toProcess.remove( c );
			if( alreadySerializedComponentNames.contains( c.getName()))
				continue;

			result.getBlocks().addAll( buildComponent( result, c, addComment ));
			alreadySerializedComponentNames.add( c.getName());

			toProcess.addAll( c.getChildren());
		}

		// Write the facets
		for( BlockFacet facet : this.facets.values()) {
			result.getBlocks().add( new BlockComment( result, "# Facets" ));
			result.getBlocks().add( facet );
			result.getBlocks().add( new BlockBlank( result, "\n" ));
		}

		return result;
	}


	private Collection<AbstractBlock> buildComponent( FileDefinition file, Component component, boolean addComment ) {
		Collection<AbstractBlock> result = new ArrayList<AbstractBlock> ();

		// Add a comment
		if( addComment ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "# Component '" );
			sb.append( component.getName());
			sb.append( "'\n# Applied Facets:" );

			for( String facetName : component.getFacetNames() ) {
				sb.append( "\n# \t- " );
				sb.append( facetName );
			}

			result.add( new BlockComment( file, sb.toString()));
		}

		// Basic properties
		BlockComponent blockComponent = new BlockComponent( file );
		blockComponent.setName( component.getName());
		result.add( blockComponent );
		result.add( new BlockBlank( file, "\n" ));

		BlockProperty p;
		if( ! Utils.isEmptyOrWhitespaces( component.getIconLocation())) {
			p = new BlockProperty( file, Constants.PROPERTY_GRAPH_ICON_LOCATION, component.getIconLocation());
			blockComponent.getInnerBlocks().add( p );
		}

		if( ! Utils.isEmptyOrWhitespaces( component.getAlias())) {
			p = new BlockProperty( file, Constants.PROPERTY_COMPONENT_ALIAS, component.getAlias());
			blockComponent.getInnerBlocks().add( p );
		}

		if( ! Utils.isEmptyOrWhitespaces( component.getInstallerName())) {
			p = new BlockProperty( file, Constants.PROPERTY_GRAPH_INSTALLER, component.getInstallerName());
			blockComponent.getInnerBlocks().add( p );
		}

		// Facets
		StringBuilder sb = new StringBuilder();
		for( Iterator<String> it = component.getFacetNames().iterator(); it.hasNext(); ) {

			String facetName = it.next();
			sb.append( facetName );
			if( it.hasNext())
				sb.append( ", " );

			if( ! this.facets.containsKey( facetName )) {
				BlockFacet blockFacet = new BlockFacet( file );
				blockFacet.setName( facetName );
				this.facets.put( facetName, blockFacet );
			}
		}

		if( sb.length() > 0 ) {
			p = new BlockProperty( file, Constants.PROPERTY_COMPONENT_FACETS, sb.toString());
			blockComponent.getInnerBlocks().add( p );
		}

		// Imported Variables
		sb = new StringBuilder();
		for( Iterator<Map.Entry<String,Boolean>> it=component.getImportedVariables().entrySet().iterator(); it.hasNext(); ) {

			Map.Entry<String,Boolean> entry = it.next();
			sb.append( entry.getKey());
			if( entry.getValue()) {
				sb.append( " " );
				sb.append( Constants.PROPERTY_COMPONENT_OPTIONAL_IMPORT );
			}

			if( it.hasNext())
				sb.append( ", " );
		}

		p = new BlockProperty( file, Constants.PROPERTY_COMPONENT_IMPORTS, sb.toString());
		if( ! component.getImportedVariables().isEmpty())
			blockComponent.getInnerBlocks().add( p );

		// Exported variables
		sb = new StringBuilder();
		boolean first = true;
		for( Map.Entry<String,String> entry : component.getExportedVariables().entrySet()) {

			// If the variable is exported by a facet, do not change its name.
			// If it is exported by the component, remove the component prefix.
			Entry<String,String> varParts = VariableHelpers.parseVariableName( entry.getKey());

			// The variable is exported by the component (i.e. it is prefixed by the component name)
			if( component.getName().equals( varParts.getKey())) {
				if( first )
					first = false;
				else
					sb.append( ", " );

				sb.append( varParts.getValue());
				if( entry.getValue() != null ) {
					sb.append( "=" );
					sb.append( entry.getValue());
				}
			}

			// Or it is exported by a facet (not prefixed by the component name)
			else {
				processFacetVariable( file, varParts.getKey(), varParts.getValue(), entry.getValue());
			}
		}

		p = new BlockProperty( file, Constants.PROPERTY_GRAPH_EXPORTS, sb.toString());
		if( ! component.getExportedVariables().isEmpty())
			blockComponent.getInnerBlocks().add( p );

		// Children
		sb = new StringBuilder();
		for( Iterator<Component> it=component.getChildren().iterator(); it.hasNext(); ) {
			sb.append( it.next().getName());
			if( it.hasNext())
				sb.append( ", " );
		}

		p = new BlockProperty( file, Constants.PROPERTY_GRAPH_CHILDREN, sb.toString());
		if( ! component.getChildren().isEmpty())
			blockComponent.getInnerBlocks().add( p );

		return result;
	}


	private void processFacetVariable( FileDefinition file, String facetName, String exportedVariableName, String variableValue ) {

		BlockFacet blockFacet = this.facets.get( facetName );
		BlockProperty exportsProperty = blockFacet.findPropertyBlockByName( Constants.PROPERTY_GRAPH_EXPORTS );
		if( exportsProperty == null ) {
			exportsProperty = new BlockProperty( file );
			exportsProperty.setName( Constants.PROPERTY_GRAPH_EXPORTS );
			blockFacet.getInnerBlocks().add( exportsProperty );
		}

		String decl = exportedVariableName;
		if( ! Utils.isEmptyOrWhitespaces( variableValue ))
			decl += " = " + variableValue;

		if( Utils.isEmptyOrWhitespaces( exportsProperty.getValue()))
			exportsProperty.setValue( decl );
		else
			exportsProperty.setValue( exportsProperty.getValue() + ", " + decl );
	}
}
