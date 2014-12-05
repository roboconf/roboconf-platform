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

package net.roboconf.core.dsl.converters;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.roboconf.core.ErrorCode;
import net.roboconf.core.dsl.ParsingConstants;
import net.roboconf.core.dsl.ParsingModelValidator;
import net.roboconf.core.dsl.parsing.AbstractBlock;
import net.roboconf.core.dsl.parsing.AbstractBlockHolder;
import net.roboconf.core.dsl.parsing.BlockComponent;
import net.roboconf.core.dsl.parsing.BlockFacet;
import net.roboconf.core.dsl.parsing.BlockImport;
import net.roboconf.core.dsl.parsing.FileDefinition;
import net.roboconf.core.internal.dsl.parsing.FileDefinitionParser;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.RoboconfErrorHelpers;
import net.roboconf.core.utils.ModelUtils;

/**
 * To build a {@link Graphs} from a {@link FileDefinition}.
 * @author Vincent Zurczak - Linagora
 */
public class FromGraphDefinition {

	private final File rootDirectory;
	private final Collection<ModelError> errors = new ArrayList<ModelError> ();

	private Map<String,List<BlockFacet>> facetNameToRelationFacets;
	private Map<String,List<BlockComponent>> componentNameToRelationComponents;
	private Map<String,Component> componentNameToComponent;
	private Map<String,Collection<String>> componentNameToComponentChildrenNames;
	private Map<String,String> componentNameToExtendedComponentName;

	private Set<File> importsToProcess, processedImports;


	/**
	 * Constructor.
	 * @param rootDirectory the root directory that contains the definition (used to resolve imports)
	 */
	public FromGraphDefinition( File rootDirectory ) {
		this.rootDirectory = rootDirectory;
	}


	/**
	 * @return the errors (never null)
	 */
	public Collection<ModelError> getErrors() {
		return this.errors;
	}


	/**
	 * @param file the initial file to parse
	 * @return an instance of {@link Graphs} built from a {@link FileRelations}
	 * <p>
	 * The result is not significant if there are errors.<br />
	 * Conversion errors are available by using {@link #getErrors()}.
	 * </p>
	 */
	public Graphs buildGraphs( File file ) {

		// Initialize collections
		this.facetNameToRelationFacets = new HashMap<String,List<BlockFacet>> ();
		this.componentNameToRelationComponents = new HashMap<String,List<BlockComponent>> ();
		this.componentNameToComponent = new HashMap<String,Component> ();
		this.componentNameToComponentChildrenNames = new HashMap<String,Collection<String>> ();
		this.componentNameToExtendedComponentName = new HashMap<String,String> ();

		this.importsToProcess = new HashSet<File> ();
		this.processedImports = new HashSet<File> ();
		this.errors.clear();

		// Process the file and its imports
		this.importsToProcess.add( file );
		while( ! this.importsToProcess.isEmpty()) {
			File importedFile = this.importsToProcess.iterator().next();
			this.importsToProcess.remove( importedFile );
			this.processedImports.add( importedFile );

			if( ! importedFile.exists()) {
				ModelError error = new ModelError( ErrorCode.CO_UNREACHABLE_FILE, 0 );
				error.setDetails( "Import location: " + importedFile );
				this.errors.add( error );
				continue;
			}

			// Load the file
			FileDefinition currentDefinition = new FileDefinitionParser( importedFile, true ).read();
			Collection<ModelError> currentErrors = new ArrayList<ModelError> ();
			currentErrors.addAll( currentDefinition.getParsingErrors());

			for( AbstractBlock block : currentDefinition.getBlocks())
				currentErrors.addAll( ParsingModelValidator.validate( block ));

			if( currentDefinition.getFileType() != FileDefinition.AGGREGATOR
					&& currentDefinition.getFileType() != FileDefinition.GRAPH ) {

				ModelError error = new ModelError( ErrorCode.CO_NOT_A_GRAPH, 0 );
				error.setDetails( "Imported file  " + importedFile + " is of type " + FileDefinition.fileTypeAsString( currentDefinition.getFileType()) + "." );
				currentErrors.add( error );
			}

			// Process the file
			this.errors.addAll( currentErrors );
			if( ! RoboconfErrorHelpers.containsCriticalErrors( currentErrors ))
				processInstructions( currentDefinition );
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

		// Eventually, deal with inheritance
		if( this.errors.isEmpty())
			updateComponentsWithInheritance();

		// Build the result
		return buildFinalGraphs();
	}


	private void processInstructions( FileDefinition definition ) {

		for( AbstractBlock block : definition.getBlocks()) {
			switch( block.getInstructionType()) {
			case AbstractBlock.COMPONENT:
				processComponent((BlockComponent) block );
				break;

			case AbstractBlock.FACET:
				processFacet((BlockFacet) block);
				break;

			case AbstractBlock.IMPORT:
				processImport((BlockImport) block );
				break;

			default:
				// nothing
				break;
			}
		}
	}


	private void processImport( BlockImport block ) {

		String uri = block.getUri().trim();
		File newDefFile = new File( this.rootDirectory, uri );
		if( ! this.processedImports.contains( newDefFile ))
			this.importsToProcess.add( newDefFile );
	}


	private void processFacet( BlockFacet block ) {

		List<BlockFacet> facets = this.facetNameToRelationFacets.get( block.getName());
		if( facets == null )
			facets = new ArrayList<BlockFacet> ();

		facets.add( block );
		this.facetNameToRelationFacets.put( block.getName(), facets );
	}


	private void processComponent( BlockComponent block ) {

		Component component = new Component();
		component.setName( block.getName());
		component.setInstallerName( ModelUtils.getPropertyValue( block, ParsingConstants.PROPERTY_COMPONENT_INSTALLER ));
		component.getMetadata().getFacetNames().addAll( ModelUtils.getPropertyValues( block, ParsingConstants.PROPERTY_COMPONENT_FACETS ));

		for( String s : ModelUtils.getPropertyValues( block, ParsingConstants.PROPERTY_COMPONENT_IMPORTS )) {
			Boolean optional = s.toLowerCase().endsWith( ParsingConstants.PROPERTY_COMPONENT_OPTIONAL_IMPORT );
			if( optional )
				s = s.substring( 0, s.length() - ParsingConstants.PROPERTY_COMPONENT_OPTIONAL_IMPORT.length()).trim();

			component.getImportedVariables().put( s, optional );
		}

		component.getExportedVariables().putAll( ModelUtils.getExportedVariables( block ));
		String extendedComponent = ModelUtils.getPropertyValue( block, ParsingConstants.PROPERTY_GRAPH_EXTENDS );
		if( extendedComponent != null )
			this.componentNameToExtendedComponentName.put( component.getName(), extendedComponent );

		// Children and ancestors will be resolved once all the components have been read, imports included
		this.componentNameToComponent.put( component.getName(), component );
		this.componentNameToComponentChildrenNames.put(
				component.getName(),
				ModelUtils.getPropertyValues( block, ParsingConstants.PROPERTY_GRAPH_CHILDREN ));

		List<BlockComponent> components = this.componentNameToRelationComponents.get( block.getName());
		if( components == null )
			components = new ArrayList<BlockComponent> ();

		components.add( block );
		this.componentNameToRelationComponents.put( component.getName(), components );
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
				sb.append( file.getEditedFile().getName());

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
			for( String facetName : c.getMetadata().getFacetNames()) {

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
					allFacets.remove( currentFacet );

					if( alreadyProcessedFacets.contains( currentFacet )) {
						ModelError error = new ModelError( ErrorCode.CO_CYCLE_IN_FACETS, 0 );
						error.setDetails( currentFacet + " -> ... -> " + currentFacet );
						this.errors.add( error );
						break;
					}

					alreadyProcessedFacets.add( currentFacet );
					List<BlockFacet> extendedFacets = this.facetNameToRelationFacets.get( currentFacet );
					if( extendedFacets == null ) {
						ModelError error = new ModelError( ErrorCode.CO_UNRESOLVED_FACET, 0 );
						error.setDetails( "Facet name: " + currentFacet );
						this.errors.add( error );
						break;
					}

					for( String extendedFacetName : ModelUtils.getPropertyValues( extendedFacets.get( 0 ), ParsingConstants.PROPERTY_GRAPH_EXTENDS ))
						allFacets.add( extendedFacetName );
				}

				additionalComponentFacets.addAll( alreadyProcessedFacets );
			}

			// Update the component with the inherited properties
			c.getMetadata().getFacetNames().addAll( additionalComponentFacets );
			for( String facetName : c.getMetadata().getFacetNames()) {
				List<BlockFacet> facets = this.facetNameToRelationFacets.get( facetName );
				if( facets == null
						|| facets.isEmpty())
					continue;

				BlockFacet facet = facets.get( 0 );
				c.getExportedVariables().putAll( ModelUtils.getExportedVariables( facet ));

				Collection<String> children = this.componentNameToComponentChildrenNames.get( c.getName());
				children.addAll( ModelUtils.getPropertyValues( facet, ParsingConstants.PROPERTY_GRAPH_CHILDREN ));
				this.componentNameToComponentChildrenNames.put( c.getName(), children );
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
							|| c.getMetadata().getFacetNames().contains( childName )) {
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


	private void updateComponentsWithInheritance() {

		// Resolve extended components
		for( Component c : this.componentNameToComponent.values()) {
			String ecn = this.componentNameToExtendedComponentName.get( c.getName());
			if( ecn == null )
				continue;

			Component ec = this.componentNameToComponent.get( ecn );
			if( ec != null ) {
				c.getMetadata().setExtendedComponent( ec );

			} else {
				ModelError error = new ModelError( ErrorCode.CO_INEXISTING_COMPONENT, 0 );
				error.setDetails( "Component name: " + ecn );
				this.errors.add( error );
			}
		}

		// Now, we want to process components in the right order.
		// An extended component must be processed BEFORE the component that extends it.
		Map<Integer,Set<Component>> levelToComponents = new TreeMap<Integer,Set<Component>> ();
		for( Component c : this.componentNameToComponent.values()) {
			int level = countParents( c );
			Set<Component> components = levelToComponents.get( level );
			if( components == null )
				components = new HashSet<Component> ();

			components.add( c );
			levelToComponents.put( level, components );
		}

		// Level -1 indicates an error
		levelToComponents.remove( -1 );

		// Process by levels.
		// Fact: a component CANNOT extend a component from the same level.
		// So, processing by level guarantees all the extended components will be complete.
		for( Set<Component> components : levelToComponents.values()) {
			for( Component c : components ) {

				// Inexisting components were already identified. So, no new error here.
				Component ec = c.getMetadata().getExtendedComponent();
				if( ec == null )
					continue;

				if( c.getInstallerName() == null )
					c.setInstallerName( ec.getInstallerName());

				c.getExportedVariables().putAll( ec.getExportedVariables());
				c.getMetadata().getFacetNames().addAll( ec.getMetadata().getFacetNames());
				c.getChildren().addAll( ec.getChildren());
				c.getAncestors().addAll( ec.getAncestors());

				for( Component ancestor : ec.getAncestors())
					ancestor.getChildren().add( c );

				// A component MAY decide to change the requirement about an import.
				// It means an optional import may be come mandatory. Why not...
				for( Map.Entry<String,Boolean> entry : ec.getImportedVariables().entrySet()) {
					if( ! c.getImportedVariables().containsKey( entry.getKey()))
						c.getImportedVariables().put( entry.getKey(), entry.getValue());
				}
			}
		}

		// This map is useless now
		this.componentNameToExtendedComponentName = null;
	}


	private Graphs buildFinalGraphs() {
		Graphs result = new Graphs();
		for( Component c : this.componentNameToComponent.values()) {
			if( c.getAncestors().isEmpty())
				result.getRootComponents().add( c );
		}

		return result;
	}


	int countParents( Component component ) {

		Set<String> alreadySeen = new HashSet<String> ();
		int count = 0;
		Component c = component;
		while(( c = c.getMetadata().getExtendedComponent()) != null ) {

			if( alreadySeen.contains( c.getName())) {
				ModelError error = new ModelError( ErrorCode.CO_CYCLE_IN_COMPONENTS_INHERITANCE, 0 );
				error.setDetails( c.getName() + " -> ... -> " + c.getName());
				this.errors.add( error );

				count = -1;
				break;

			} else {
				count ++;
				alreadySeen.add( c.getName());
			}
		}

		return count;
	}
}
