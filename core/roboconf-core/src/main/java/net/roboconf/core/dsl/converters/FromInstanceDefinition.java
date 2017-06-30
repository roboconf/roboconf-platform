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

import static net.roboconf.core.errors.ErrorDetails.alreadyDefined;
import static net.roboconf.core.errors.ErrorDetails.component;
import static net.roboconf.core.errors.ErrorDetails.file;
import static net.roboconf.core.errors.ErrorDetails.instance;
import static net.roboconf.core.errors.ErrorDetails.line;
import static net.roboconf.core.errors.ErrorDetails.unrecognized;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.roboconf.core.dsl.ParsingConstants;
import net.roboconf.core.dsl.ParsingModelValidator;
import net.roboconf.core.dsl.parsing.AbstractBlock;
import net.roboconf.core.dsl.parsing.AbstractBlockHolder;
import net.roboconf.core.dsl.parsing.BlockImport;
import net.roboconf.core.dsl.parsing.BlockInstanceOf;
import net.roboconf.core.dsl.parsing.BlockProperty;
import net.roboconf.core.dsl.parsing.FileDefinition;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.ErrorDetails;
import net.roboconf.core.errors.RoboconfErrorHelpers;
import net.roboconf.core.internal.dsl.parsing.FileDefinitionParser;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.SourceReference;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.ModelUtils;

/**
 * To build a collection of {@link Instance} from a {@link FileDefinition}.
 * @author Vincent Zurczak - Linagora
 */
public class FromInstanceDefinition {

	private static final String INST_COUNT = "count";

	private final File rootDirectory;
	private Graphs graphs;
	private final Collection<ParsingError> errors = new ArrayList<> ();
	private final Map<Object,SourceReference> objectToSource = new HashMap<> ();

	private Map<BlockInstanceOf,Instance> allBlocksToInstances;
	private Set<File> importsToProcess, processedImports;


	/**
	 * Constructor.
	 * @param rootDirectory the root directory that contains the definition (used to resolve imports)
	 */
	public FromInstanceDefinition( File rootDirectory ) {
		this.rootDirectory = rootDirectory;
	}


	/**
	 * @return the errors (never null)
	 */
	public Collection<ParsingError> getErrors() {
		return this.errors;
	}


	/**
	 * @return the processedImports
	 */
	public Set<File> getProcessedImports() {
		return this.processedImports;
	}


	/**
	 * @return the objectToSource
	 */
	public Map<Object,SourceReference> getObjectToSource() {
		return this.objectToSource;
	}


	/**
	 * @param graphs the graph(s)
	 * @param file the file to parse
	 * @return a non-null collection of root rootInstances wrapped in machines
	 * <p>
	 * The result is not significant if there are errors.<br>
	 * Conversion errors are available by using {@link #getErrors()}.
	 * </p>
	 */
	public Collection<Instance> buildInstances( Graphs graphs, File file ) {

		// Initialize collections
		this.allBlocksToInstances = new LinkedHashMap<> ();
		this.graphs = graphs;

		this.importsToProcess = new HashSet<> ();
		this.processedImports = new HashSet<> ();
		this.errors.clear();

		// Process the file and its imports
		this.importsToProcess.add( file );
		while( ! this.importsToProcess.isEmpty()) {
			File importedFile = this.importsToProcess.iterator().next();
			this.importsToProcess.remove( importedFile );
			this.processedImports.add( importedFile );

			if( ! importedFile.exists()) {
				ParsingError error = new ParsingError( ErrorCode.CO_UNREACHABLE_FILE, file, 0 );
				error.setDetails( file( importedFile ));
				this.errors.add( error );
				continue;
			}

			// Load the file
			FileDefinition currentDefinition = new FileDefinitionParser( importedFile, true ).read();
			Collection<ParsingError> currentErrors = new ArrayList<> ();
			currentErrors.addAll( currentDefinition.getParsingErrors());

			for( AbstractBlock block : currentDefinition.getBlocks())
				currentErrors.addAll( ParsingModelValidator.validate( block ));

			if( currentDefinition.getFileType() != FileDefinition.INSTANCE
					&& currentDefinition.getFileType() != FileDefinition.AGGREGATOR
					&& currentDefinition.getFileType() != FileDefinition.EMPTY ) {

				ParsingError error = new ParsingError( ErrorCode.CO_NOT_INSTANCES, file, 0 );
				error.setDetails( unrecognized( FileDefinition.fileTypeAsString( currentDefinition.getFileType())));
				currentErrors.add( error );
			}

			// Process the file
			this.errors.addAll( currentErrors );
			if( ! RoboconfErrorHelpers.containsCriticalErrors( currentErrors ))
				processInstructions( currentDefinition );
		}

		// Check uniqueness
		if( this.errors.isEmpty())
			checkUnicity();

		// Find the root instances
		Collection<Instance> rootInstances = new HashSet<> ();
		for( Instance instance : this.allBlocksToInstances.values()) {
			if( instance.getParent() == null )
				rootInstances.add( instance );
		}

		// So far, we have found all the instance definitions.
		if( this.errors.isEmpty()) {

			// What we have to do, is to duplicate those whose "count" is higher than 1.
			List<Instance> newRootInstances = new ArrayList<> ();
			for( Instance rootInstance : rootInstances )
				newRootInstances.addAll( replicateInstancesFrom( rootInstance ));

			// At this level, there may be new naming conflicts...
			List<Instance> tempNewRootInstances = new ArrayList<>( newRootInstances );
			tempNewRootInstances.retainAll( rootInstances );
			for( Instance instance : tempNewRootInstances ) {
				ParsingError error = new ParsingError( ErrorCode.CO_CONFLICTING_INFERRED_INSTANCE, file, 1 );
				error.setDetails( instance( instance ));
				this.errors.add( error );
			}

			rootInstances.addAll( newRootInstances );
		}

		// No error? Backup source information for further validation.
		if( this.errors.isEmpty())
			backupSourceInformation();

		return rootInstances;
	}


	private void processInstructions( FileDefinition definition ) {

		for( AbstractBlock block : definition.getBlocks()) {
			switch( block.getInstructionType()) {
			case AbstractBlock.INSTANCEOF:
				processInstance((BlockInstanceOf) block );
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


	private void processInstance( BlockInstanceOf block ) {

		// Process the rootInstances
		Map<BlockInstanceOf,Instance> blockToInstance = new LinkedHashMap<> ();
		blockToInstance.put( block, new Instance());
		this.allBlocksToInstances.putAll( blockToInstance );

		// We rely on a different collection than just Instance#getChildren().
		// This is because getChildren() uses a hash set.
		// But here, at parsing time, we need a list (if there are duplicates, we need to know them).
		Map<Instance,List<Instance>> instanceToChildrenInstances = new HashMap<> ();

		while( ! blockToInstance.isEmpty()) {

			// The current one to process won't be processed again
			Map.Entry<BlockInstanceOf,Instance> entry = blockToInstance.entrySet().iterator().next();
			blockToInstance.remove( entry.getKey());

			// Process the current
			BlockInstanceOf currentBlock = entry.getKey();

			// Do we need to create several instances?
			String countAsString = ModelUtils.getPropertyValue( currentBlock, ParsingConstants.PROPERTY_INSTANCE_COUNT );

			// Process the instance
			Instance instance = entry.getValue();
			instance.setName( ModelUtils.getPropertyValue( currentBlock, ParsingConstants.PROPERTY_INSTANCE_NAME ));
			instance.channels.addAll( ModelUtils.getPropertyValues( currentBlock, ParsingConstants.PROPERTY_INSTANCE_CHANNELS ));

			instance.setComponent( ComponentHelpers.findComponent( this.graphs, currentBlock.getName()));
			if( instance.getComponent() == null ) {
				ParsingError error = new ParsingError( ErrorCode.CO_INEXISTING_COMPONENT, block.getDeclaringFile().getEditedFile(), currentBlock.getLine());
				error.setDetails( component( currentBlock.getName()));
				this.errors.add( error );
				continue;
			}

			// Runtime data
			String state = ModelUtils.getPropertyValue( currentBlock, ParsingConstants.PROPERTY_INSTANCE_STATE );
			if( state != null )
				instance.setStatus( InstanceStatus.whichStatus( state ));

			for( Map.Entry<String,String> dataEntry : ModelUtils.getData( currentBlock ).entrySet()) {
				instance.data.put( dataEntry.getKey(), dataEntry.getValue());
			}

			// Since instance hash changes when we update their parent, we cannot rely on hash map
			// to store the count for a given instance. So, we will temporarily use instance#getData().
			instance.data.put( INST_COUNT, countAsString );

			for( AbstractBlock innerBlock : currentBlock.getInnerBlocks()) {

				// Check overridden exports
				if( innerBlock.getInstructionType() == AbstractBlock.PROPERTY ) {
					String pName = ((BlockProperty) innerBlock).getName();
					if( ParsingConstants.PROPERTY_INSTANCE_NAME.equals( pName )
							|| ParsingConstants.PROPERTY_INSTANCE_CHANNELS.equals( pName )
							|| ParsingConstants.PROPERTY_INSTANCE_DATA.equals( pName )
							|| ParsingConstants.PROPERTY_INSTANCE_STATE.equals( pName )
							|| ParsingConstants.PROPERTY_INSTANCE_COUNT.equals( pName ))
						continue;

					String pValue = ((BlockProperty) innerBlock).getValue();
					if( pName.toLowerCase().startsWith( ParsingConstants.PROPERTY_INSTANCE_DATA_PREFIX ))
						instance.data.put( pName.substring( 5 ), pValue );
					else if( pValue.matches( "\\s*\".*\"\\s*" ))
						instance.overriddenExports.put( pName, pValue.trim().substring( 1, pValue.trim().length() -1 ));
					else
						instance.overriddenExports.put( pName, pValue );

					continue;
				}

				// Initialize children rootInstances
				if( innerBlock.getInstructionType() != AbstractBlock.INSTANCEOF )
					continue;

				List<Instance> childrenInstances = instanceToChildrenInstances.get( instance );
				if( childrenInstances == null )
					childrenInstances = new ArrayList<> ();

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


	private Collection<Instance> replicateInstancesFrom( Instance rootInstance ) {
		Collection<Instance> newRootInstances = new ArrayList<> ();

		// Begin with the duplicates of the deepest instances.
		List<Instance> orderedInstances = InstanceHelpers.buildHierarchicalList( rootInstance );
		Collections.reverse( orderedInstances );

		for( Instance instance : orderedInstances ) {
			String countAsString = instance.data.remove( INST_COUNT );
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
				Instance copy = InstanceHelpers.replicateInstance( instance );
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
		Map<String,List<BlockInstanceOf>> instancePathToBlocks = new HashMap<> ();
		for( Map.Entry<BlockInstanceOf,Instance> entry : this.allBlocksToInstances.entrySet()) {
			String instancePath = InstanceHelpers.computeInstancePath( entry.getValue());
			List<BlockInstanceOf> blocks = instancePathToBlocks.get( instancePath );
			if( blocks == null )
				blocks = new ArrayList<> ();

			blocks.add( entry.getKey());
			instancePathToBlocks.put( instancePath, blocks );
		}

		// Let's now find the duplicate declarations
		for( Map.Entry<String,List<BlockInstanceOf>> entry : instancePathToBlocks.entrySet()) {
			if( entry.getValue().size() == 1 )
				continue;

			for( AbstractBlockHolder holder : entry.getValue()) {
				List<ErrorDetails> details = new ArrayList<> ();

				details.add( instance( entry.getKey()));
				details.add( alreadyDefined( entry.getKey()));
				details.add( file( holder.getDeclaringFile().getEditedFile()));
				details.add( line( holder.getLine()));

				this.errors.add( new ParsingError(
						ErrorCode.CO_ALREADY_DEFINED_INSTANCE,
						holder.getFile(),
						holder.getLine(),
						details.toArray( new ErrorDetails[ details.size()])));
			}
		}
	}


	private void backupSourceInformation() {

		for( Map.Entry<BlockInstanceOf,Instance> entry : this.allBlocksToInstances.entrySet()) {
			AbstractBlockHolder holder = entry.getKey();
			SourceReference sr = new SourceReference( entry.getValue(), holder.getFile(), holder.getLine());
			this.objectToSource.put( sr.getModelObject(), sr );
		}
	}
}
