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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.roboconf.core.Constants;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.internal.model.parsing.FileDefinitionParser;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.model.parsing.AbstractBlock;
import net.roboconf.core.model.parsing.AbstractBlockHolder;
import net.roboconf.core.model.parsing.BlockImport;
import net.roboconf.core.model.parsing.BlockInstanceOf;
import net.roboconf.core.model.parsing.BlockProperty;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.model.runtime.Graphs;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.core.utils.ModelUtils;

/**
 * To build a collection of {@link Instance} from a {@link FileDefinition}.
 * @author Vincent Zurczak - Linagora
 */
public class FromInstanceDefinition {

	private static final String INST_COUNT = "count";

	private final FileDefinition definition;
	private final Collection<ModelError> errors = new ArrayList<ModelError> ();

	private Map<BlockInstanceOf,Instance> allBlocksToInstances;
	private Map<String,BlockImport> importUriToImportDeclaration;
	private Set<String> alreadyProcessedUris;
	private Graphs graphs;


	/**
	 * Constructor.
	 * @param definition its type must be either {@link FileDefinition#INSTANCE} or {@link FileDefinition#AGGREGATOR}
	 */
	public FromInstanceDefinition( FileDefinition definition ) {
		if( definition.getFileType() != FileDefinition.AGGREGATOR
				&& definition.getFileType() != FileDefinition.INSTANCE )
			throw new IllegalArgumentException( "File must be of type INSTANCE or AGGREGATOR." );

		this.definition = definition;
	}


	/**
	 * @return the errors (never null)
	 */
	public Collection<ModelError> getErrors() {
		return this.errors;
	}


	/**
	 * @param graphs the graph(s)
	 * @return a non-null collection of root rootInstances wrapped in machines
	 * <p>
	 * The result is not significant if there are errors.<br />
	 * Conversion errors are available by using {@link #getErrors()}.
	 * </p>
	 */
	public Collection<Instance> buildInstances( Graphs graphs ) {

		// Initialize collections
		this.importUriToImportDeclaration = new HashMap<String,BlockImport> ();
		this.allBlocksToInstances = new LinkedHashMap<BlockInstanceOf,Instance> ();
		this.alreadyProcessedUris = new HashSet<String> ();
		this.graphs = graphs;
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

		// Check uniqueness
		if( this.errors.isEmpty())
			checkUnicity();

		// Find the root instances
		Collection<Instance> rootInstances = new HashSet<Instance> ();
		for( Instance instance : this.allBlocksToInstances.values()) {
			if( instance.getParent() == null )
				rootInstances.add( instance );
		}

		// So far, we have found all the instance definitions.
		if( this.errors.isEmpty()) {

			// What we have to do, is to duplicate those whose "count" is higher than 1.
			List<Instance> newRootInstances = new ArrayList<Instance> ();
			for( Instance rootInstance : rootInstances )
				newRootInstances.addAll( duplicateInstancesFrom( rootInstance ));

			// At this level, there may be new naming conflicts...
			List<Instance> tempNewRootInstances = new ArrayList<Instance>( newRootInstances );
			tempNewRootInstances.retainAll( rootInstances );
			for( Instance instance : tempNewRootInstances ) {
				ModelError error = new ModelError( ErrorCode.CO_CONFLICTING_INFERRED_INSTANCE, -1 );
				error.setDetails( "Instance path: " + InstanceHelpers.computeInstancePath( instance ));
				this.errors.add( error );
			}

			rootInstances.addAll( newRootInstances );
		}

		return rootInstances;
	}


	private void processInstructions( FileDefinition definition ) {

		for( AbstractBlock block : this.definition.getBlocks()) {
			switch( block.getInstructionType()) {
			case AbstractBlock.INSTANCEOF:
				processInstance((BlockInstanceOf) block, definition.getFileLocation());
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


	private void processInstance( BlockInstanceOf block, URI processedUri ) {

		// Process the rootInstances
		Map<BlockInstanceOf,Instance> blockToInstance = new LinkedHashMap<BlockInstanceOf,Instance> ();
		blockToInstance.put( block, new Instance());
		this.allBlocksToInstances.putAll( blockToInstance );

		// We rely on a different collection than just Instance#getChildren().
		// This is because getChildren() uses a hash set.
		// But here, at parsing time, we need a list (if there are duplicates, we need to know them).
		Map<Instance,List<Instance>> instanceToChildrenInstances = new HashMap<Instance,List<Instance>> ();

		while( ! blockToInstance.isEmpty()) {

			// The current one to process won't be processed again
			Map.Entry<BlockInstanceOf,Instance> entry = blockToInstance.entrySet().iterator().next();
			blockToInstance.remove( entry.getKey());

			// Process the current
			BlockInstanceOf currentBlock = entry.getKey();

			// Do we need to create several instances?
			String countAsString = ModelUtils.getPropertyValue( currentBlock, Constants.PROPERTY_INSTANCE_COUNT );

			// Process the instance
			Instance instance = entry.getValue();
			instance.setName( ModelUtils.getPropertyValue( currentBlock, Constants.PROPERTY_INSTANCE_NAME ));
			instance.setChannel( ModelUtils.getPropertyValue( currentBlock, Constants.PROPERTY_INSTANCE_CHANNEL ));
			instance.setComponent( ComponentHelpers.findComponent( this.graphs, currentBlock.getName()));

			// Runtime data
			String state = ModelUtils.getPropertyValue( currentBlock, Constants.PROPERTY_INSTANCE_STATE );
			if( state != null )
				instance.setStatus( InstanceStatus.wichStatus( state ));

			for( Map.Entry<String,String> dataEntry : ModelUtils.getData( currentBlock ).entrySet()) {
				instance.getData().put( dataEntry.getKey(), dataEntry.getValue());
			}

			// Since instance hash changes when we update their parent, we cannot rely on hash map
			// to store the count for a given instance. So, we will temporarily use instance#getData().
			instance.getData().put( INST_COUNT, countAsString );

			for( AbstractBlock innerBlock : currentBlock.getInnerBlocks()) {

				// Check overridden exports
				if( innerBlock.getInstructionType() == AbstractBlock.PROPERTY ) {
					String pName = ((BlockProperty) innerBlock).getName();
					if( Constants.PROPERTY_INSTANCE_NAME.equals( pName )
							|| Constants.PROPERTY_INSTANCE_CHANNEL.equals( pName )
							|| Constants.PROPERTY_INSTANCE_DATA.equals( pName )
							|| Constants.PROPERTY_INSTANCE_STATE.equals( pName )
							|| Constants.PROPERTY_INSTANCE_COUNT.equals( pName ))
						continue;

					String pValue = ((BlockProperty) innerBlock).getValue();
					this.errors.addAll( analyzeOverriddenExport( innerBlock.getLine(), instance, pName, pValue ));
					continue;
				}

				// Initialize children rootInstances
				if( innerBlock.getInstructionType() != AbstractBlock.INSTANCEOF )
					continue;

				List<Instance> childrenInstances = instanceToChildrenInstances.get( instance );
				if( childrenInstances == null )
					childrenInstances = new ArrayList<Instance> ();

				Instance newInstance = new Instance();
				childrenInstances.add( newInstance );
				instanceToChildrenInstances.put( instance, childrenInstances );

				blockToInstance.put((BlockInstanceOf) innerBlock, newInstance );
				this.allBlocksToInstances.put((BlockInstanceOf) innerBlock, newInstance );
			}
		}

		// Associate instances with their children.
		// Since we change the path, we also change the hash computing.
		for( Map.Entry<Instance,List<Instance>> entry : instanceToChildrenInstances.entrySet()) {
			for( Instance childInstance : entry.getValue())
				InstanceHelpers.insertChild( entry.getKey(), childInstance );
		}
	}


	private Collection<Instance> duplicateInstancesFrom( Instance rootInstance ) {
		Collection<Instance> newRootInstances = new ArrayList<Instance> ();

		// Begin with the duplicates of the deepest instances.
		List<Instance> orderedInstances = InstanceHelpers.buildHierarchicalList( rootInstance );
		Collections.reverse( orderedInstances );

		for( Instance instance : orderedInstances ) {
			String countAsString = instance.getData().remove( INST_COUNT );
			Integer count = 1;
			try {
				count = Integer.parseInt( countAsString );
			} catch( NumberFormatException e ) {
				// ignore, the validator for the parsing model should handle this
			}

			if( count <= 1 )
				continue;

			String format = "%0" + String.valueOf( count ).length() + "d";
			for( int i=2; i<=count; i++ ) {
				Instance copy = InstanceHelpers.duplicateInstance( instance );
				copy.name( copy.getName() + String.format( format, i ));

				if( instance.getParent() != null )
					InstanceHelpers.insertChild( instance.getParent(), copy );
				else
					newRootInstances.add( copy );
			}

			// Update the first one
			instance.name( instance.getName() + String.format( format, 1 ));
		}

		return newRootInstances;
	}


	private void checkUnicity() {

		// "allBlocksToInstances" associates blocks and instances.
		// Unlike instances children which consider instances with the same path as the same,
		// "allBlocksToInstances" allows to find instances with the same path.
		Map<String,List<BlockInstanceOf>> instancePathToBlocks = new HashMap<String,List<BlockInstanceOf>> ();
		for( Map.Entry<BlockInstanceOf,Instance> entry : this.allBlocksToInstances.entrySet()) {
			String instancePath = InstanceHelpers.computeInstancePath( entry.getValue());
			List<BlockInstanceOf> blocks = instancePathToBlocks.get( instancePath );
			if( blocks == null )
				blocks = new ArrayList<BlockInstanceOf> ();

			blocks.add( entry.getKey());
			instancePathToBlocks.put( instancePath, blocks );
		}

		// Let's now find the duplicate declarations
		for( Map.Entry<String,List<BlockInstanceOf>> entry : instancePathToBlocks.entrySet()) {
			if( entry.getValue().size() == 1 )
				continue;

			StringBuilder sb = new StringBuilder();
			sb.append( "Instance " );
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
				ModelError error = new ModelError( ErrorCode.CO_ALREADY_DEFINED_INSTANCE, holder.getLine());
				error.setDetails( sb.toString());
				this.errors.add( error );
			}
		}
	}


	/**
	 * Analyzes an overridden export (i.e. an export defined in an instance).
	 * @param holderLine the line where this property was declared
	 * @param instance the instance
	 * @param varName the variable / property name
	 * @param varValue the variable value
	 * @return non-null list of errors
	 */
	static List<ModelError> analyzeOverriddenExport( int holderLine, Instance instance, String varName, String varValue ) {
		List<ModelError> result = new ArrayList<ModelError> ();

		// Component variables are prefixed by a component or a facet name.
		// Instance variables may not be prefixed (user-friendly).
		// This is an initial processing
		Set<String> ambiguousNames = new HashSet<String> ();
		for( String componentVarName : instance.getComponent().getExportedVariables().keySet()) {

			// If variables have the same name (by ignoring the prefixing component or facet name)...
			// ... then we have an ambiguity.
			if( varName.equals( VariableHelpers.parseVariableName( componentVarName ).getValue()))
				ambiguousNames.add( componentVarName );
		}

		// If the variable name is prefixed correctly
		if( instance.getComponent().getExportedVariables().containsKey( varName ))
			instance.getOverriddenExports().put( varName, varValue );

		// No name? Show a warning and it
		else if( ambiguousNames.isEmpty()) {
			ModelError error = new ModelError( ErrorCode.CO_NOT_OVERRIDING, holderLine );
			error.setDetails( "Variable name: " + varName );
			result.add( error );
		}

		// A single name: mark it as resolved
		else if( ambiguousNames.size() == 1 )
			instance.getOverriddenExports().put( ambiguousNames.iterator().next(), varValue );

		// Several names? Mark an ambiguity we cannot solve.
		else {
			StringBuilder sb = new StringBuilder();
			sb.append( "Variable " );
			sb.append( varName );
			sb.append( " could mean " );

			for( Iterator<String> it = ambiguousNames.iterator(); it.hasNext(); ) {
				sb.append( it.next());
				if( it.hasNext())
					sb.append( ", " );
			}

			ModelError error = new ModelError( ErrorCode.CO_AMBIGUOUS_OVERRIDING, holderLine );
			error.setDetails( sb.toString());
			result.add( error );
		}

		return result;
	}
}
