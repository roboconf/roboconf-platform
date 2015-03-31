/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.SourceReference;
import net.roboconf.core.model.beans.AbstractType;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.RoboconfErrorHelpers;
import net.roboconf.core.utils.ModelUtils;
import net.roboconf.core.utils.Utils;

/**
 * To build a {@link Graphs} from a {@link FileDefinition}.
 * @author Vincent Zurczak - Linagora
 */
public class FromGraphDefinition {

	private final File rootDirectory;
	private final Collection<ParsingError> errors = new ArrayList<ParsingError> ();
	private final Map<Object,SourceReference> objectToSource = new HashMap<Object,SourceReference> ();

	private Map<String,ComponentData> componentNameToComponentData;
	private Map<String,FacetData> facetNameToFacetData;

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
	public Collection<ParsingError> getErrors() {
		return this.errors;
	}


	/**
	 * @return the objectToSource (never null)
	 */
	public Map<Object,SourceReference> getObjectToSource() {
		return this.objectToSource;
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
		this.componentNameToComponentData = new HashMap<String,ComponentData> ();
		this.facetNameToFacetData = new HashMap<String,FacetData> ();

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
				ParsingError error = new ParsingError( ErrorCode.CO_UNREACHABLE_FILE, file, 0 );
				error.setDetails( "File location: " + importedFile );
				this.errors.add( error );
				continue;
			}

			// Load the file
			FileDefinition currentDefinition = new FileDefinitionParser( importedFile, true ).read();
			Collection<ParsingError> currentErrors = new ArrayList<ParsingError> ();
			currentErrors.addAll( currentDefinition.getParsingErrors());

			for( AbstractBlock block : currentDefinition.getBlocks())
				currentErrors.addAll( ParsingModelValidator.validate( block ));

			if( currentDefinition.getFileType() != FileDefinition.AGGREGATOR
					&& currentDefinition.getFileType() != FileDefinition.GRAPH ) {

				ParsingError error = new ParsingError( ErrorCode.CO_NOT_A_GRAPH, file, 0 );
				error.setDetails( "Imported file  " + importedFile + " is of type " + FileDefinition.fileTypeAsString( currentDefinition.getFileType()) + "." );
				currentErrors.add( error );
			}

			// Process the file
			this.errors.addAll( currentErrors );
			if( ! RoboconfErrorHelpers.containsCriticalErrors( currentErrors ))
				processInstructions( currentDefinition );
		}

		// Check names collisions
		if( this.errors.isEmpty())
			checkNameCollisions();

		// Check uniqueness
		if( this.errors.isEmpty())
			checkUnicity();

		// Resolve all
		if( this.errors.isEmpty())
			resolveComponents();

		if( this.errors.isEmpty())
			resolveFacets();

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

		FacetData data = this.facetNameToFacetData.get( block.getName());
		if( data != null ) {
			data.blocks.add( block );

		} else {
			data = new FacetData();
			data.object = new Facet( block.getName());
			data.object.exportedVariables.putAll( ModelUtils.getExportedVariables( block ));
			data.childrenNames.addAll( ModelUtils.getPropertyValues( block, ParsingConstants.PROPERTY_GRAPH_CHILDREN ));
			data.extendedFacetNames.addAll( ModelUtils.getPropertyValues( block, ParsingConstants.PROPERTY_GRAPH_EXTENDS ));
			data.blocks.add( block );

			this.facetNameToFacetData.put( block.getName(), data );
		}
	}


	private void processComponent( BlockComponent block ) {

		ComponentData data = this.componentNameToComponentData.get( block.getName());
		if( data != null ) {
			data.blocks.add( block );

		} else {
			data = new ComponentData();
			data.object = new Component( block.getName());
			data.object.exportedVariables.putAll( ModelUtils.getExportedVariables( block ));
			data.object.setInstallerName( ModelUtils.getPropertyValue( block, ParsingConstants.PROPERTY_COMPONENT_INSTALLER ));

			for( String s : ModelUtils.getPropertyValues( block, ParsingConstants.PROPERTY_COMPONENT_IMPORTS )) {
				Boolean optional = s.toLowerCase().endsWith( ParsingConstants.PROPERTY_COMPONENT_OPTIONAL_IMPORT );
				if( optional )
					s = s.substring( 0, s.length() - ParsingConstants.PROPERTY_COMPONENT_OPTIONAL_IMPORT.length()).trim();

				data.object.importedVariables.put( s, optional );
			}

			data.extendedComponentName = ModelUtils.getPropertyValue( block, ParsingConstants.PROPERTY_GRAPH_EXTENDS );
			data.childrenNames.addAll( ModelUtils.getPropertyValues( block, ParsingConstants.PROPERTY_GRAPH_CHILDREN ));
			data.facetNames.addAll( ModelUtils.getPropertyValues( block, ParsingConstants.PROPERTY_COMPONENT_FACETS ));
			data.blocks.add( block );

			this.componentNameToComponentData.put( block.getName(), data );
		}
	}


	private void checkNameCollisions() {

		Collection<String> names = new HashSet<String> ();
		names.addAll( this.componentNameToComponentData.keySet());
		names.retainAll( this.facetNameToFacetData.keySet());

		for( String name : names ) {
			ComponentData cd = this.componentNameToComponentData.get( name );
			this.errors.addAll( cd.error( ErrorCode.CO_CONFLICTING_NAME, "Name: " + name ));

			FacetData fd = this.facetNameToFacetData.get( name );
			this.errors.addAll( fd.error( ErrorCode.CO_CONFLICTING_NAME, "Name: " + name ));
		}
	}


	private void checkUnicity() {

		// Components
		for( Data<?> data : this.componentNameToComponentData.values()) {
			if( data.blocks.size() > 1 )
				this.errors.addAll( data.error( ErrorCode.CO_ALREADY_DEFINED_COMPONENT, "Component name: " + data.object.getName()));
		}

		// Facets
		for( Data<?> data : this.facetNameToFacetData.values()) {
			if( data.blocks.size() > 1 )
				this.errors.addAll( data.error( ErrorCode.CO_ALREADY_DEFINED_FACET, "Facet name: " + data.object.getName()));
		}
	}


	private void resolveComponents() {

		for( ComponentData data : this.componentNameToComponentData.values()) {

			// Being here means we did not find conflicting names
			AbstractBlockHolder holder = data.blocks.get( 0 );
			SourceReference sr = new SourceReference( data.object, holder.getFile(), holder.getLine());
			this.objectToSource.put( data.object, sr );

			// The extended component name
			if( ! Utils.isEmptyOrWhitespaces( data.extendedComponentName )) {
				ComponentData extendedComponentData = this.componentNameToComponentData.get( data.extendedComponentName );
				if( extendedComponentData != null )
					data.object.extendComponent( extendedComponentData.object );
				else
					this.errors.addAll( data.error( ErrorCode.CO_INEXISTING_COMPONENT, "Component name: " + data.extendedComponentName ));
			}

			// The facets
			for( String s : data.facetNames ) {
				FacetData facetData = this.facetNameToFacetData.get( s );
				if( facetData != null )
					data.object.associateFacet( facetData.object );
				else
					this.errors.addAll( data.error( ErrorCode.CO_INEXISTING_FACET, "Facet name: " + s ));
			}

			// The children
			for( String s : data.childrenNames ) {
				ComponentData componentData = this.componentNameToComponentData.get( s );
				FacetData facetData = this.facetNameToFacetData.get( s );
				if( componentData != null )
					data.object.addChild( componentData.object );
				else if( facetData != null )
					data.object.addChild( facetData.object );
				else
					this.errors.addAll( data.error( ErrorCode.CO_INEXISTING_CHILD, "Name: " + s ));
			}
		}
	}


	private void resolveFacets() {

		for( FacetData data : this.facetNameToFacetData.values()) {

			// Being here means we did not find conflicting names
			AbstractBlockHolder holder = data.blocks.get( 0 );
			SourceReference sr = new SourceReference( data.object, holder.getFile(), holder.getLine());
			this.objectToSource.put( data.object, sr );

			// The extended facets
			for( String s : data.extendedFacetNames ) {
				FacetData facetData = this.facetNameToFacetData.get( s );
				if( facetData != null )
					data.object.extendFacet( facetData.object );
				else
					this.errors.addAll( data.error( ErrorCode.CO_INEXISTING_FACET, "Facet name: " + s ));
			}

			// The children
			for( String s : data.childrenNames ) {
				ComponentData componentData = this.componentNameToComponentData.get( s );
				FacetData facetData = this.facetNameToFacetData.get( s );
				if( componentData != null )
					data.object.addChild( componentData.object );
				else if( facetData != null )
					data.object.addChild( facetData.object );
				else
					this.errors.addAll( data.error( ErrorCode.CO_INEXISTING_CHILD, "Name: " + s ));
			}
		}
	}


	private Graphs buildFinalGraphs() {

		Graphs result = new Graphs();
		for( ComponentData cd : this.componentNameToComponentData.values()) {
			if( ComponentHelpers.findAllAncestors( cd.object ).isEmpty())
				result.getRootComponents().add( cd.object );
		}

		for( FacetData data : this.facetNameToFacetData.values())
			result.getFacetNameToFacet().put( data.object.getName(), data.object );

		return result;
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 * @param <T>
	 */
	private static class Data<T extends AbstractType> {

		T object;
		Collection<String> childrenNames = new HashSet<String> ();
		List<AbstractBlockHolder> blocks = new ArrayList<AbstractBlockHolder> ();

		List<ParsingError> error( ErrorCode code, String cause ) {

			List<ParsingError> errors = new ArrayList<ParsingError> ();
			for( AbstractBlockHolder block : this.blocks ) {
				ParsingError error = new ParsingError( code, block.getDeclaringFile().getEditedFile(), block.getLine());
				error.setDetails( cause );
				errors.add( error );
			}

			return errors;
		}
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	private static class ComponentData extends Data<Component> {
		String extendedComponentName;
		Collection<String> facetNames = new HashSet<String> ();
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	private static class FacetData extends Data<Facet> {
		Collection<String> extendedFacetNames = new HashSet<String> ();
	}
}
