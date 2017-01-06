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

package net.roboconf.core.dsl.converters;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.roboconf.core.dsl.ParsingConstants;
import net.roboconf.core.dsl.parsing.AbstractBlock;
import net.roboconf.core.dsl.parsing.BlockBlank;
import net.roboconf.core.dsl.parsing.BlockComment;
import net.roboconf.core.dsl.parsing.BlockComponent;
import net.roboconf.core.dsl.parsing.BlockFacet;
import net.roboconf.core.dsl.parsing.BlockProperty;
import net.roboconf.core.dsl.parsing.FileDefinition;
import net.roboconf.core.model.beans.AbstractType;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ExportedVariable;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.utils.Utils;

/**
 * To dump a {@link Graphs} into a file.
 * @author Vincent Zurczak - Linagora
 */
public class FromGraphs {

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
		Set<String> alreadySerializedNames = new HashSet<> ();
		for( Component c : ComponentHelpers.findAllComponents( graphs )) {
			if( alreadySerializedNames.contains( c.getName()))
				continue;

			alreadySerializedNames.add( c.getName());
			result.getBlocks().addAll( buildComponent( result, c, addComment ));
			for( Facet f : ComponentHelpers.findAllFacets( c )) {
				if( alreadySerializedNames.contains( f.getName()))
					continue;

				alreadySerializedNames.add( f.getName());
				result.getBlocks().addAll( buildFacet( result, f, addComment ));
			}
		}

		// There may be orphan facets too
		for( Facet f : graphs.getFacetNameToFacet().values()) {
			if( alreadySerializedNames.contains( f.getName()))
				continue;

			alreadySerializedNames.add( f.getName());
			result.getBlocks().addAll( buildFacet( result, f, addComment ));
		}

		return result;
	}


	private Collection<AbstractBlock> buildComponent( FileDefinition file, Component component, boolean addComment ) {
		Collection<AbstractBlock> result = new ArrayList<> ();

		// Add a comment
		if( addComment ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "# Component '" );
			sb.append( component.getName());
			sb.append( "'" );
			result.add( new BlockComment( file, sb.toString()));
		}

		// Basic properties
		BlockComponent blockComponent = new BlockComponent( file );
		blockComponent.setName( component.getName());
		result.add( blockComponent );
		result.add( new BlockBlank( file, "\n" ));

		BlockProperty p;
		if( component.getExtendedComponent() != null ) {
			p = new BlockProperty( file, ParsingConstants.PROPERTY_GRAPH_EXTENDS, component.getExtendedComponent().getName());
			blockComponent.getInnerBlocks().add( p );
		}

		if( ! Utils.isEmptyOrWhitespaces( component.getInstallerName())) {
			p = new BlockProperty( file, ParsingConstants.PROPERTY_COMPONENT_INSTALLER, component.getInstallerName());
			blockComponent.getInnerBlocks().add( p );
		}

		// Facets
		String s = writeCollection( component.getFacets());
		if( ! Utils.isEmptyOrWhitespaces( s )) {
			p = new BlockProperty( file, ParsingConstants.PROPERTY_COMPONENT_FACETS, s );
			blockComponent.getInnerBlocks().add( p );
		}

		// Imported Variables
		StringBuilder sb = new StringBuilder();
		for( Iterator<ImportedVariable> it=component.importedVariables.values().iterator(); it.hasNext(); ) {

			ImportedVariable var = it.next();
			if( var.isExternal()) {
				sb.append( ParsingConstants.PROPERTY_COMPONENT_EXTERNAL_IMPORT );
				sb.append( " " );
			}

			sb.append( var.getName());
			if( var.isOptional()) {
				sb.append( " " );
				sb.append( ParsingConstants.PROPERTY_COMPONENT_OPTIONAL_IMPORT );
			}

			if( it.hasNext())
				sb.append( ", " );
		}

		if( ! Utils.isEmptyOrWhitespaces( sb.toString())) {
			p = new BlockProperty( file, ParsingConstants.PROPERTY_COMPONENT_IMPORTS, sb.toString());
			blockComponent.getInnerBlocks().add( p );
		}

		// Exported variables
		s = writeExportedVariables( component );
		if( ! Utils.isEmptyOrWhitespaces( s )) {
			p = new BlockProperty( file, ParsingConstants.PROPERTY_GRAPH_EXPORTS, s );
			blockComponent.getInnerBlocks().add( p );
		}

		// Children
		s = writeCollection( component.getChildren());
		if( ! Utils.isEmptyOrWhitespaces( s )) {
			p = new BlockProperty( file, ParsingConstants.PROPERTY_GRAPH_CHILDREN, s );
			blockComponent.getInnerBlocks().add( p );
		}

		return result;
	}


	private Collection<AbstractBlock> buildFacet( FileDefinition file, Facet facet, boolean addComment ) {
		Collection<AbstractBlock> result = new ArrayList<> ();

		// Add a comment
		if( addComment ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "# Facet '" );
			sb.append( facet.getName());
			sb.append( "'" );
			result.add( new BlockComment( file, sb.toString()));
		}

		// Basic properties
		BlockFacet blockFacet = new BlockFacet( file );
		blockFacet.setName( facet.getName());
		result.add( blockFacet );
		result.add( new BlockBlank( file, "\n" ));

		// Extended facets
		BlockProperty p;
		String s = writeCollection( facet.getExtendedFacets());
		if( ! Utils.isEmptyOrWhitespaces( s )) {
			p = new BlockProperty( file, ParsingConstants.PROPERTY_GRAPH_EXTENDS, s );
			blockFacet.getInnerBlocks().add( p );
		}

		// Exported variables
		s = writeExportedVariables( facet );
		if( ! Utils.isEmptyOrWhitespaces( s )) {
			p = new BlockProperty( file, ParsingConstants.PROPERTY_GRAPH_EXPORTS, s );
			blockFacet.getInnerBlocks().add( p );
		}

		// Children
		s = writeCollection( facet.getChildren());
		if( ! Utils.isEmptyOrWhitespaces( s )) {
			p = new BlockProperty( file, ParsingConstants.PROPERTY_GRAPH_CHILDREN, s );
			blockFacet.getInnerBlocks().add( p );
		}

		return result;
	}


	private String writeExportedVariables( AbstractType type ) {

		StringBuilder sb = new StringBuilder();
		for( Iterator<Map.Entry<String,ExportedVariable>> it=type.exportedVariables.entrySet().iterator(); it.hasNext(); ) {

			Map.Entry<String,ExportedVariable> entry = it.next();
			sb.append( entry.getKey());

			String variableValue = entry.getValue().getValue();
			if( ! Utils.isEmptyOrWhitespaces( variableValue )) {
				sb.append( "=\"" );
				sb.append( variableValue );
				sb.append( "\"" );
			}

			if( it.hasNext())
				sb.append( ", " );
		}

		return sb.toString();
	}


	private String writeCollection( Collection<? extends AbstractType> types ) {

		StringBuilder sb = new StringBuilder();
		for( Iterator<? extends AbstractType> it=types.iterator(); it.hasNext(); ) {
			sb.append( it.next().getName());
			if( it.hasNext())
				sb.append( ", " );
		}

		return sb.toString();
	}
}
