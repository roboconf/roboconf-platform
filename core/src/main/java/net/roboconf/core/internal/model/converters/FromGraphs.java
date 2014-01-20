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

package net.roboconf.core.internal.model.converters;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import net.roboconf.core.model.parsing.AbstractInstruction;
import net.roboconf.core.model.parsing.Constants;
import net.roboconf.core.model.parsing.FileRelations;
import net.roboconf.core.model.parsing.RelationBlank;
import net.roboconf.core.model.parsing.RelationComment;
import net.roboconf.core.model.parsing.RelationComponent;
import net.roboconf.core.model.parsing.RelationProperty;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;

/**
 * To dump a {@link Graphs} into a file.
 * @author Vincent Zurczak - Linagora
 */
public class FromGraphs {

	public FileRelations buildFileRelations( Graphs graphs, File targetFile, boolean addComment ) {

		FileRelations result = new FileRelations( targetFile );
		if( addComment ) {
			String s = "# File created from an in-memory model,\n# without a binding to existing files.";
			RelationComment initialComment = new RelationComment( result, s );
			result.getInstructions().add( initialComment );
			result.getInstructions().add( new RelationBlank( result, "\n" ));
		}

		Set<String> alreadySerializedComponentNames = new HashSet<String> ();
		ArrayList<Component> toProcess = new ArrayList<Component> ();
		toProcess.addAll( graphs.getRootComponents());

		while( ! toProcess.isEmpty()) {
			Component c = toProcess.iterator().next();
			toProcess.remove( c );
			if( alreadySerializedComponentNames.contains( c.getName()))
				continue;

			result.getInstructions().addAll( buildRelationComponent( result, c, addComment ));
			alreadySerializedComponentNames.add( c.getName());

			toProcess.addAll( c.getChildren());
		}

		return result;
	}


	private Collection<AbstractInstruction> buildRelationComponent( FileRelations file, Component component, boolean addComment ) {
		Collection<AbstractInstruction> result = new ArrayList<AbstractInstruction> ();

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

			result.add( new RelationComment( file, sb.toString()));
		}

		// Basic properties
		RelationComponent relationComponent = new RelationComponent( file );
		relationComponent.setName( component.getName());
		result.add( relationComponent );

		RelationProperty p = new RelationProperty( file, Constants.ICON_LOCATION, component.getIconLocation());
		relationComponent.getPropertyNameToProperty().put( Constants.ICON_LOCATION, p );

		p = new RelationProperty( file, Constants.COMPONENT_ALIAS, component.getAlias());
		relationComponent.getPropertyNameToProperty().put( Constants.COMPONENT_ALIAS, p );

		p = new RelationProperty( file, Constants.INSTALLER, component.getInstallerName());
		relationComponent.getPropertyNameToProperty().put( Constants.INSTALLER, p );

		// Imported Variables
		StringBuilder sb = new StringBuilder();
		for( Iterator<String> it=component.getImportedVariableNames().iterator(); it.hasNext(); ) {
			sb.append( it.next());
			if( it.hasNext())
				sb.append( ", " );
		}

		p = new RelationProperty( file, Constants.COMPONENT_IMPORTS, sb.toString());
		relationComponent.getPropertyNameToProperty().put( Constants.COMPONENT_IMPORTS, p );

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

		p = new RelationProperty( file, Constants.EXPORTS, sb.toString());
		relationComponent.getPropertyNameToProperty().put( Constants.EXPORTS, p );

		// Children
		sb = new StringBuilder();
		for( Iterator<Component> it=component.getChildren().iterator(); it.hasNext(); ) {
			sb.append( it.next().getName());
			if( it.hasNext())
				sb.append( ", " );
		}

		p = new RelationProperty( file, Constants.CHILDREN, sb.toString());
		relationComponent.getPropertyNameToProperty().put( Constants.CHILDREN, p );

		return result;
	}
}
