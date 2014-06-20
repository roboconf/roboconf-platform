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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.roboconf.core.Constants;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.internal.model.parsing.FileDefinitionParser;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.parsing.AbstractBlock;
import net.roboconf.core.model.parsing.AbstractBlockHolder;
import net.roboconf.core.model.parsing.BlockComponent;
import net.roboconf.core.model.parsing.BlockFacet;
import net.roboconf.core.model.parsing.BlockImport;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;
import net.roboconf.core.utils.ModelUtils;

/**
 * To build a {@link Graphs} from a {@link FileDefinition}.
 * @author Vincent Zurczak - Linagora
 */
public class FromGraphDefinition {

	private final FileDefinition definition;
	private final Collection<ModelError> errors = new ArrayList<ModelError> ();

	private Map<String,BlockImport> importUriToImportDeclaration;
	private Set<String> alreadyProcessedUris;
	private Map<String,List<BlockFacet>> facetNameToRelationFacets;
	private Map<String,List<BlockComponent>> componentNameToRelationComponents;
	private Map<String,Component> componentNameToComponent;
	private Map<String,Collection<String>> componentNameToComponentChildrenNames;


	/**
	 * Constructor.
	 * @param definition its type must be either {@link FileDefinition#GRAPH} or {@link FileDefinition#AGGREGATOR}
	 */
	public FromGraphDefinition( FileDefinition definition ) {
		if( definition.getFileType() != FileDefinition.AGGREGATOR
				&& definition.getFileType() != FileDefinition.GRAPH )
			throw new IllegalArgumentException( "File must be of type GRAPH or AGGREGATOR." );

		this.definition = definition;
	}


	/**
	 * @return the errors (never null)
	 */
	public Collection<ModelError> getErrors() {
		return this.errors;
	}


	/**
	 * @return an instance of {@link Graphs} built from a {@link FileRelations}
	 * <p>
	 * The result is not significant if there are errors.<br />
	 * Conversion errors are available by using {@link #getErrors()}.
	 * </p>
	 */
	public Graphs buildGraphs() {

		// Initialize collections
		this.facetNameToRelationFacets = new HashMap<String,List<BlockFacet>> ();
		this.componentNameToRelationComponents = new HashMap<String,List<BlockComponent>> ();
		this.componentNameToComponent = new HashMap<String,Component> ();
		this.componentNameToComponentChildrenNames = new HashMap<String,Collection<String>> ();
		this.importUriToImportDeclaration = new HashMap<String,BlockImport> ();
		this.alreadyProcessedUris = new HashSet<String> ();
		this.errors.clear();

		// Process the file and its imports
		processInstructions( this.definition );
		this.alreadyProcessedUris.add( String.valueOf( this.definition.getFileLocation()));

		while( ! this.importUriToImportDeclaration.isEmpty()) {
			Entry<String,BlockImport> entry = this.importUriToImportDeclaration.entrySet().iterator().next();
			String uri = entry.getKey();
			this.importUriToImportDeclaration.remove( uri );
			this.alreadyProcessedUris.add( uri );

			if( this.alreadyProcessedUris.contains( uri ))
				continue;

			// Load the file
			FileDefinition importedDefinition;
			try {
				importedDefinition = new FileDefinitionParser( uri, true ).read();

			} catch( URISyntaxException e ) {
				ModelError error = new ModelError( ErrorCode.CO_UNREACHABLE_FILE, 0 );
				error.setDetails( "Import location: " + uri );
				this.errors.add( error );
				continue;
			}

			// Check the file type
			if( this.definition.getFileType() != FileDefinition.AGGREGATOR
					&& this.definition.getFileType() != FileDefinition.GRAPH ) {

				ModelError error = new ModelError( ErrorCode.CO_NOT_A_GRAPH, 0 );
				error.setDetails( "Imported file  " + uri + " is of type " + FileDefinition.fileTypeAsString( this.definition.getFileType()) + "." );
				this.errors.add( error );
				continue;
			}

			// Process the file
			processInstructions( importedDefinition );
		}

		// Check names uniqueness
		if( this.errors.isEmpty())
			checkUnicity( this.componentNameToRelationComponents, ErrorCode.CO_ALREADY_DEFINED_COMPONENT );

		if( this.errors.isEmpty())
			checkUnicity( this.facetNameToRelationFacets, ErrorCode.CO_ALREADY_DEFINED_FACET );

		// Apply facets to components
		if( this.errors.isEmpty())
			updateComponentsFromFacets();

		// Only then, search for children - ancestor definition
		if( this.errors.isEmpty())
			updateComponentsDependencies();

		// Build the result
		return buildFinalGraphs();
	}


	private void processInstructions( FileDefinition definition ) {

		for( AbstractBlock block : this.definition.getBlocks()) {
			switch( block.getInstructionType()) {
			case AbstractBlock.COMPONENT:
				processComponent((BlockComponent) block, definition.getFileLocation());
				break;

			case AbstractBlock.FACET:
				processFacet((BlockFacet) block);
				break;

			case AbstractBlock.IMPORT:
				processImport((BlockImport) block, definition.getFileLocation());
				break;

			default:
				// nothing
				break;
			}
		}
	}


	private void processImport( BlockImport block, URI processedUri ) {
		String uri = block.getUri().trim();

		// FIXME: to deal with...
		// try {
		//	uri = UriUtils.buildNewURI( processedUri, uri ).toString();
			this.importUriToImportDeclaration.put( uri, block );

//		} catch( URISyntaxException e ) {
//			throw new ConversionException( "An URI could not be built from the import definition.",  e, block );
//		}
	}


	private void processFacet( BlockFacet block ) {

		List<BlockFacet> facets = this.facetNameToRelationFacets.get( block.getName());
		if( facets == null )
			facets = new ArrayList<BlockFacet> ();

		facets.add( block );
		this.facetNameToRelationFacets.put( block.getName(), facets );
	}


	private void processComponent( BlockComponent block, URI processedUri ) {

		Component component = new Component();
		component.setName( block.getName());
		component.setInstallerName( ModelUtils.getPropertyValue( block, Constants.PROPERTY_GRAPH_INSTALLER ));
		component.setAlias( ModelUtils.getPropertyValue( block, Constants.PROPERTY_COMPONENT_ALIAS ));
		component.getFacetNames().addAll( ModelUtils.getPropertyValues( block, Constants.PROPERTY_COMPONENT_FACETS ));

		for( String s : ModelUtils.getPropertyValues( block, Constants.PROPERTY_COMPONENT_IMPORTS )) {
			Boolean optional = s.toLowerCase().endsWith( Constants.PROPERTY_COMPONENT_OPTIONAL_IMPORT );
			if( optional )
				s = s.substring( 0, s.length() - Constants.PROPERTY_COMPONENT_OPTIONAL_IMPORT.length()).trim();

			component.getImportedVariables().put( s, optional );
		}

		component.getExportedVariables().putAll( ModelUtils.getExportedVariables( block ));
		component.setIconLocation( ModelUtils.getPropertyValue( block, Constants.PROPERTY_GRAPH_ICON_LOCATION ));

		// Children and ancestors will be resolved once all the components have been read, imports included
		this.componentNameToComponent.put( block.getName(), component );
		this.componentNameToComponentChildrenNames.put(
				block.getName(),
				ModelUtils.getPropertyValues( block, Constants.PROPERTY_GRAPH_CHILDREN ));

		List<BlockComponent> components = this.componentNameToRelationComponents.get( block.getName());
		if( components == null )
			components = new ArrayList<BlockComponent> ();

		components.add( block );
		this.componentNameToRelationComponents.put( block.getName(), components );
	}


	private <T extends AbstractBlockHolder> void checkUnicity( Map<String,List<T>> map, ErrorCode code ) {

		for( Map.Entry<String,List<T>> entry : map.entrySet()) {
			if( entry.getValue().size() == 1 )
				continue;

			StringBuilder sb = new StringBuilder();
			sb.append( entry.getKey());
			sb.append( " is defined in:\n" );
			for( AbstractBlockHolder holder : entry.getValue()) {
				sb.append( " - " );

				FileDefinition file = holder.getDeclaringFile();
				if( file.getEditedFile() != null )
					sb.append( file.getEditedFile().getName());
				else
					sb.append( file.getFileLocation());

				sb.append( " - line " );
				sb.append( holder.getLine());
				sb.append( "\n" );
			}

			for( AbstractBlockHolder holder : entry.getValue()) {
				ModelError error = new ModelError( code, holder.getLine());
				error.setDetails( sb.toString());
				this.errors.add( error );
			}
		}

		// The map is useless now
		map = null;
	}


	private void updateComponentsFromFacets() {

		for( Component c : this.componentNameToComponent.values()) {

			// Find all the facets this component inherits from
			Set<String> additionalComponentFacets = new HashSet<String> ();
			for( String facetName : c.getFacetNames()) {

				// Find the facet
				List<BlockFacet> facets = this.facetNameToRelationFacets.get( facetName );
				if( facets == null ) {
					ModelError error = new ModelError( ErrorCode.CO_UNRESOLVED_FACET, 0 );
					error.setDetails( "Facet name: " + facetName );
					this.errors.add( error );
					continue;
				}

				// No need to check whether this facet was defined several times,
				// it is done in #checkUnicity().

				// Find all the extended facets
				Set<String> allFacets = new HashSet<String> ();
				Set<String> alreadyProcessedFacets = new HashSet<String> ();
				allFacets.add( facetName );
				while( ! allFacets.isEmpty()) {
					String currentFacet = allFacets.iterator().next();
					if( alreadyProcessedFacets.contains( currentFacet )) {

						ModelError error = new ModelError( ErrorCode.CO_CYCLE_IN_FACETS, 0 );
						error.setDetails( currentFacet + " -> ... -> " + currentFacet );
						this.errors.add( error );
						break;
					}

					BlockFacet extendedFacet = this.facetNameToRelationFacets.get( currentFacet ).get( 0 );
					if( extendedFacet == null ) {
						ModelError error = new ModelError( ErrorCode.CO_UNRESOLVED_FACET, 0 );
						error.setDetails( "Facet name: " + currentFacet );
						this.errors.add( error );
						break;
					}

					for( String extendedFacetName : ModelUtils.getPropertyValues( extendedFacet, Constants.PROPERTY_FACET_EXTENDS ))
						allFacets.add( extendedFacetName );

					allFacets.remove( currentFacet );
					alreadyProcessedFacets.add( currentFacet );
				}

				additionalComponentFacets.addAll( alreadyProcessedFacets );
			}

			// Update the component with the inherited properties
			Set<String> installerNames = new HashSet<String> ();
			c.getFacetNames().addAll( additionalComponentFacets );
			for( String facetName : c.getFacetNames()) {
				List<BlockFacet> facets = this.facetNameToRelationFacets.get( facetName );
				if( facets == null
						|| facets.isEmpty())
					continue;

				BlockFacet facet = facets.get( 0 );
				c.getExportedVariables().putAll( ModelUtils.getExportedVariables( facet ));

				for( String s : ModelUtils.getPropertyValues( facet, Constants.PROPERTY_COMPONENT_IMPORTS )) {
					Boolean optional = s.toLowerCase().endsWith( Constants.PROPERTY_COMPONENT_OPTIONAL_IMPORT );
					if( optional )
						s = s.substring( 0, s.length() - Constants.PROPERTY_COMPONENT_OPTIONAL_IMPORT.length()).trim();

					c.getImportedVariables().put( s, optional );
				}

				installerNames.add( ModelUtils.getPropertyValue( facet, Constants.PROPERTY_GRAPH_INSTALLER ));
				if( c.getIconLocation() == null )
					c.setIconLocation( ModelUtils.getPropertyValue( facet, Constants.PROPERTY_GRAPH_ICON_LOCATION ));

				Collection<String> children = this.componentNameToComponentChildrenNames.get( c.getName());
				children.addAll( ModelUtils.getPropertyValues( facet, Constants.PROPERTY_GRAPH_CHILDREN ));
				this.componentNameToComponentChildrenNames.put( c.getName(), children );
			}

			// After the facets have been processed, check the installer name
			if( c.getInstallerName() == null ) {
				if( installerNames.size() == 1 ) {
					c.setInstallerName( installerNames.iterator().next());

				} else if( installerNames.size() > 1 ) {
					ModelError error = new ModelError( ErrorCode.CO_AMBIGUOUS_INSTALLER, 0 );
					error.setDetails( "Installer names: " + Arrays.toString( installerNames.toArray()));
					this.errors.add( error );
				}
			}
		}

		// This map is useless now
		this.facetNameToRelationFacets = null;
	}


	private void updateComponentsDependencies() {

		for( Map.Entry<String,Collection<String>> entry : this.componentNameToComponentChildrenNames.entrySet()) {
			Component component = this.componentNameToComponent.get( entry.getKey());
			for( String childName : entry.getValue()) {

				// Children can be determined by component name or by facet name
				int insertionCount = 0;
				for( Component c : this.componentNameToComponent.values()) {
					if( childName.equals( c.getName())
							|| c.getFacetNames().contains( childName )) {
						ComponentHelpers.insertChild( component, c );
						insertionCount ++;
					}
				}

				if( insertionCount == 0 ) {
					ModelError error = new ModelError( ErrorCode.CO_INEXISTING_CHILD, 0 );
					error.setDetails( "Child name: " + childName );
					this.errors.add( error );
				}
			}
		}

		// This map is useless now
		this.componentNameToComponentChildrenNames = null;
	}


	private Graphs buildFinalGraphs() {
		Graphs result = new Graphs();
		for( Component c : this.componentNameToComponent.values()) {
			if( c.getAncestors().isEmpty())
				result.getRootComponents().add( c );
		}

		return result;
	}
}
