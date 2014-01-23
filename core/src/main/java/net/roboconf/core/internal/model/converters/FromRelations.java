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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.roboconf.core.internal.model.parsing.FileDefinitionParser;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.ErrorCode;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.model.parsing.AbstractPropertiesHolder;
import net.roboconf.core.model.parsing.AbstractRegion;
import net.roboconf.core.model.parsing.Constants;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.model.parsing.RegionComponent;
import net.roboconf.core.model.parsing.RegionFacet;
import net.roboconf.core.model.parsing.RegionImport;
import net.roboconf.core.model.parsing.RegionProperty;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;
import net.roboconf.core.model.runtime.impl.ComponentImpl;
import net.roboconf.core.model.runtime.impl.GraphsImpl;

/**
 * To build a {@link Graphs} from a {@link FileDefinition}.
 * @author Vincent Zurczak - Linagora
 */
public class FromRelations {

	private final FileDefinition relations;
	private final Collection<ModelError> errors = new ArrayList<ModelError> ();

	private Map<String,RegionImport> importUriToImportDeclaration;
	private Set<String> alreadyProcessedUris;
	private Map<String,List<RegionFacet>> facetNameToRelationFacets;
	private Map<String,List<RegionComponent>> componentNameToRelationComponents;
	private Map<String,Component> componentNameToComponent;
	private Map<String,Collection<String>> componentNameToComponentChildrenNames;


	/**
	 * Constructor.
	 * @param relations
	 */
	public FromRelations( FileDefinition relations ) {
		this.relations = relations;
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
		this.facetNameToRelationFacets = new HashMap<String,List<RegionFacet>> ();
		this.componentNameToRelationComponents = new HashMap<String,List<RegionComponent>> ();
		this.componentNameToComponent = new HashMap<String,Component> ();
		this.componentNameToComponentChildrenNames = new HashMap<String,Collection<String>> ();
		this.importUriToImportDeclaration = new HashMap<String,RegionImport> ();
		this.alreadyProcessedUris = new HashSet<String> ();
		this.errors.clear();

		// Process the file and its imports
		processInstructions( this.relations );
		this.alreadyProcessedUris.add( String.valueOf( this.relations.getFileLocation()));

		while( ! this.importUriToImportDeclaration.isEmpty()) {
			Entry<String,RegionImport> entry = this.importUriToImportDeclaration.entrySet().iterator().next();
			String uri = entry.getKey();
			if( this.alreadyProcessedUris.contains( uri ))
				continue;

			try {
				FileDefinition importedRelations = new FileDefinitionParser( uri, true ).read();
				processInstructions( importedRelations );

			} catch( URISyntaxException e ) {
				ModelError error = new ModelError( ErrorCode.CO_UNREACHABLE_FILE, 0 );
				error.setDetails( "Import location: " + uri );
				this.errors.add( error );
			}

			this.importUriToImportDeclaration.remove( uri );
			this.alreadyProcessedUris.add( uri );
		}

		// Check names uniqueness
		if( this.errors.isEmpty())
			checkUnicity( this.componentNameToRelationComponents, ErrorCode.CO_ALREADY_DEFINED_COMPONENT );

		if( this.errors.isEmpty())
			checkUnicity( this.facetNameToRelationFacets, ErrorCode.CO_ALREADY_DEFINED_FACET );

		// Apply facets to components
		if( this.errors.isEmpty())
			updateComponentsFromFacets();

		// Only then, search for children - ancestor relations
		if( this.errors.isEmpty())
			updateComponentsDependencies();

		// Build the result
		return buildFinalGraphs();
	}


	private void processInstructions( FileDefinition relations ) {

		for( AbstractRegion instr : this.relations.getInstructions()) {
			switch( instr.getInstructionType()) {
			case AbstractRegion.COMPONENT:
				processComponent((RegionComponent) instr, relations.getFileLocation());
				break;

			case AbstractRegion.FACET:
				processFacet((RegionFacet) instr);
				break;

			case AbstractRegion.IMPORT:
				processImport((RegionImport) instr, relations.getFileLocation());
				break;

			default:
				// nothing
				break;
			}
		}
	}


	private void processImport( RegionImport instr, URI processedUri ) {
		String uri = instr.getUri().trim();

		// FIXME: to deal with...
		// try {
		//	uri = UriHelper.buildNewURI( processedUri, uri ).toString();
			this.importUriToImportDeclaration.put( uri, instr );

//		} catch( URISyntaxException e ) {
//			throw new ConversionException( "An URI could not be built from the import definition.",  e, instr );
//		}
	}


	private void processFacet( RegionFacet instr ) {

		List<RegionFacet> facets = this.facetNameToRelationFacets.get( instr.getName());
		if( facets == null )
			facets = new ArrayList<RegionFacet> ();

		facets.add( instr );
		this.facetNameToRelationFacets.put( instr.getName(), facets );
	}


	private void processComponent( RegionComponent instr, URI processedUri ) {

		ComponentImpl component = new ComponentImpl();
		component.setName( instr.getName());
		component.setInstallerName( getPropertyValue( instr, Constants.PROPERTY_GRAPH_INSTALLER ));
		component.setAlias( getPropertyValue( instr, Constants.PROPERTY_COMPONENT_ALIAS ));
		component.getFacetNames().addAll( getPropertyValues( instr, Constants.PROPERTY_COMPONENT_FACETS ));
		component.getImportedVariableNames().addAll( getPropertyValues( instr, Constants.PROPERTY_COMPONENT_IMPORTS ));
		component.getExportedVariables().putAll( getExportedVariables( instr ));
		component.setIconLocation( getPropertyValue( instr, Constants.PROPERTY_GRAPH_ICON_LOCATION ));

		// Children and ancestors will be resolved once all the components have been read, imports included
		this.componentNameToComponent.put( instr.getName(), component );
		this.componentNameToComponentChildrenNames.put( instr.getName(), getPropertyValues( instr, Constants.PROPERTY_GRAPH_CHILDREN ));

		List<RegionComponent> components = this.componentNameToRelationComponents.get( instr.getName());
		if( components == null )
			components = new ArrayList<RegionComponent> ();

		components.add( instr );
		this.componentNameToRelationComponents.put( instr.getName(), components );
	}


	private <T extends AbstractPropertiesHolder> void checkUnicity( Map<String,List<T>> map, ErrorCode code ) {

		for( Map.Entry<String,List<T>> entry : map.entrySet()) {
			if( entry.getValue().size() == 1 )
				continue;

			StringBuilder sb = new StringBuilder();
			sb.append( entry.getKey());
			sb.append( " is defined in:\n" );
			for( AbstractPropertiesHolder holder : entry.getValue()) {
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

			for( AbstractPropertiesHolder holder : entry.getValue()) {
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
				RegionFacet facet = this.facetNameToRelationFacets.get( facetName ).get( 0 );
				if( facet == null ) {
					ModelError error = new ModelError( ErrorCode.CO_UNRESOLVED_FACET, 0 );
					error.setDetails( "Facet name: " + facetName );
					this.errors.add( error );
					continue;
				}

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
						continue;
					}

					RegionFacet extendedFacet = this.facetNameToRelationFacets.get( currentFacet ).get( 0 );
					if( extendedFacet == null ) {
						ModelError error = new ModelError( ErrorCode.CO_UNRESOLVED_FACET, 0 );
						error.setDetails( "Facet name: " + currentFacet );
						this.errors.add( error );
						continue;
					}

					for( String extendedFacetName : getPropertyValues( extendedFacet, Constants.PROPERTY_COMPONENT_FACETS ))
						allFacets.add( extendedFacetName );

					allFacets.remove( currentFacet );
					alreadyProcessedFacets.add( currentFacet );
				}

				additionalComponentFacets.addAll( alreadyProcessedFacets );
			}

			// Update the component with the inherited properties
			c.getFacetNames().addAll( additionalComponentFacets );
			for( String facetName : c.getFacetNames()) {
				RegionFacet facet = this.facetNameToRelationFacets.get( facetName ).get( 0 );

				c.getExportedVariables().putAll( getExportedVariables( facet ));
				c.getImportedVariableNames().addAll( getPropertyValues( facet, Constants.PROPERTY_COMPONENT_IMPORTS ));
				if( c.getInstallerName() == null )
					c.setInstallerName( getPropertyValue( facet, Constants.PROPERTY_GRAPH_INSTALLER ));

				if( c.getIconLocation() == null )
					c.setIconLocation( getPropertyValue( facet, Constants.PROPERTY_GRAPH_ICON_LOCATION ));

				Collection<String> children = this.componentNameToComponentChildrenNames.get( c.getName());
				children.addAll( getPropertyValues( facet, Constants.PROPERTY_GRAPH_CHILDREN ));
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
				for( Component c : this.componentNameToComponent.values()) {
					if( childName.equals( c.getName())) {
						component.getChildren().add( c );
						c.getAncestors().add( component );

					} else if( c.getFacetNames().contains( childName )) {
						component.getChildren().add( c );
						c.getAncestors().add( component );
					}
				}
			}
		}

		// This map is useless now
		this.componentNameToComponentChildrenNames = null;
	}


	private Graphs buildFinalGraphs() {
		Graphs result = new GraphsImpl();
		for( Component c : this.componentNameToComponent.values()) {
			if( c.getAncestors().isEmpty())
				result.getRootComponents().add( c );
		}

		return result;
	}


	private String getPropertyValue( AbstractPropertiesHolder holder, String propertyName ) {
		RegionProperty p = holder.findPropertyByName( propertyName );
		return p == null ? null : p.getValue();
	}


	private Collection<String> getPropertyValues( AbstractPropertiesHolder holder, String propertyName ) {
		RegionProperty p = holder.findPropertyByName( propertyName );
		String propertyValue = p == null ? null : p.getValue();
		return Utils.splitNicely( propertyValue, Constants.PROPERTY_SEPARATOR );
	}


	private Map<String,String> getExportedVariables( AbstractPropertiesHolder holder ) {
		RegionProperty p = holder.findPropertyByName( Constants.PROPERTY_GRAPH_EXPORTS );
		Map<String,String> result = new HashMap<String,String> ();

		String propertyValue = p == null ? null : p.getValue();
		for( String s : Utils.splitNicely( propertyValue, Constants.PROPERTY_SEPARATOR )) {
			Map.Entry<String,String> entry = VariableHelpers.parseExportedVariable( s );
			// Prefix with the facet or component name.
			result.put( holder.getName() + "." + entry.getKey(), entry.getValue());
		}

		return result;
	}
}
