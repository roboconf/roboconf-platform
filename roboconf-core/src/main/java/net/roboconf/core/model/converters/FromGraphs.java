/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import net.roboconf.core.Constants;
import net.roboconf.core.model.parsing.AbstractBlock;
import net.roboconf.core.model.parsing.BlockBlank;
import net.roboconf.core.model.parsing.BlockComment;
import net.roboconf.core.model.parsing.BlockComponent;
import net.roboconf.core.model.parsing.BlockProperty;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;

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

		FileDefinition result = new FileDefinition( targetFile );
		result.setFileType( FileDefinition.GRAPH );
		if( addComment ) {
			String s = "# File created from an in-memory model,\n# without a binding to existing files.";
			BlockComment initialComment = new BlockComment( result, s );
			result.getBlocks().add( initialComment );
			result.getBlocks().add( new BlockBlank( result, "\n" ));
		}

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

		BlockProperty p = new BlockProperty( file, Constants.PROPERTY_GRAPH_ICON_LOCATION, component.getIconLocation());
		blockComponent.getInnerBlocks().add( p );

		p = new BlockProperty( file, Constants.PROPERTY_COMPONENT_ALIAS, component.getAlias());
		blockComponent.getInnerBlocks().add( p );

		p = new BlockProperty( file, Constants.PROPERTY_GRAPH_INSTALLER, component.getInstallerName());
		blockComponent.getInnerBlocks().add( p );

		// Imported Variables
		StringBuilder sb = new StringBuilder();
		for( Iterator<String> it=component.getImportedVariableNames().iterator(); it.hasNext(); ) {
			sb.append( it.next());
			if( it.hasNext())
				sb.append( ", " );
		}

		p = new BlockProperty( file, Constants.PROPERTY_COMPONENT_IMPORTS, sb.toString());
		blockComponent.getInnerBlocks().add( p );

		// Exported variables
		sb = new StringBuilder();
		for( Iterator<Entry<String,String>> it=component.getExportedVariables().entrySet().iterator(); it.hasNext(); ) {
			Entry<String,String> entry = it.next();
			sb.append( entry.getKey());
			if( entry.getValue() != null ) {
				sb.append( "=" );
				sb.append( entry.getValue());
			}

			if( it.hasNext())
				sb.append( ", " );
		}

		p = new BlockProperty( file, Constants.PROPERTY_GRAPH_EXPORTS, sb.toString());
		blockComponent.getInnerBlocks().add( p );

		// Children
		sb = new StringBuilder();
		for( Iterator<Component> it=component.getChildren().iterator(); it.hasNext(); ) {
			sb.append( it.next().getName());
			if( it.hasNext())
				sb.append( ", " );
		}

		p = new BlockProperty( file, Constants.PROPERTY_GRAPH_CHILDREN, sb.toString());
		blockComponent.getInnerBlocks().add( p );

		return result;
	}
}
