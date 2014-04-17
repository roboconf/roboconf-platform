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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.parsing.AbstractBlock;
import net.roboconf.core.model.parsing.BlockBlank;
import net.roboconf.core.model.parsing.BlockComment;
import net.roboconf.core.model.parsing.BlockInstanceOf;
import net.roboconf.core.model.parsing.BlockProperty;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.model.runtime.Instance;

/**
 * To dump a collection of root {@link Instance}s into a file.
 * @author Vincent Zurczak - Linagora
 */
public class FromInstances {

	/**
	 * Builds a file definition from a collection of instances.
	 * @param rootInstances the root instances (not null)
	 * @param targetFile the target file (will not be written)
	 * @param addComment true to insert generated comments
	 * @return a non-null file definition
	 */
	public FileDefinition buildFileDefinition( Collection<Instance> rootInstances, File targetFile, boolean addComment ) {

		FileDefinition result = new FileDefinition( targetFile );
		result.setFileType( FileDefinition.INSTANCE );
		if( addComment ) {
			String s = "# File created from an in-memory model,\n# without a binding to existing files.";
			BlockComment initialComment = new BlockComment( result, s );
			result.getBlocks().add( initialComment );
			result.getBlocks().add( new BlockBlank( result, "\n" ));
		}

		for( Instance rootInstance : rootInstances )
			result.getBlocks().addAll( buildInstanceOf( result, rootInstance, addComment ));

		return result;
	}


	private Collection<AbstractBlock> buildInstanceOf( FileDefinition file, Instance rootInstance, boolean addComment ) {
		Collection<AbstractBlock> result = new ArrayList<AbstractBlock> ();

		// Process the root instance
		Map<Instance,BlockInstanceOf> instanceToBlock = new LinkedHashMap<Instance,BlockInstanceOf> ();
		BlockInstanceOf rootBlock = new BlockInstanceOf( file );
		instanceToBlock.put( rootInstance, rootBlock );

		List<Instance> toProcess = new ArrayList<Instance> ();
		toProcess.add( rootInstance );
		while( ! toProcess.isEmpty()) {

			// Process the current instance
			Instance instance = toProcess.remove( 0 );
			BlockInstanceOf currentBlock = instanceToBlock.get( instance );
			currentBlock.setName( instance.getComponent().getName());

			BlockProperty p;
			if( ! Utils.isEmptyOrWhitespaces( instance.getName())) {
				p = new BlockProperty( file, Constants.PROPERTY_INSTANCE_NAME, instance.getName());
				currentBlock.getInnerBlocks().add( p );
			}

			if( ! Utils.isEmptyOrWhitespaces( instance.getChannel())) {
				p = new BlockProperty( file, Constants.PROPERTY_INSTANCE_CHANNEL, instance.getChannel());
				currentBlock.getInnerBlocks().add( p );
			}

			for( Map.Entry<String,String> export : instance.getOverriddenExports().entrySet()) {
				p = new BlockProperty( file, export.getKey(), export.getValue());
				currentBlock.getInnerBlocks().add( p );
			}

			// Update the parent
			BlockInstanceOf parentBlock = instanceToBlock.get( instance.getParent());
			if( parentBlock != null ) {
				parentBlock.getInnerBlocks().add( new BlockBlank( file, "\n" ));
				parentBlock.getInnerBlocks().add( currentBlock );
			}

			// Children
			for( Instance child : instance.getChildren()) {
				instanceToBlock.put( child, new BlockInstanceOf( file ));
				toProcess.add( child );
			}
		}

		// Add the root instance to the result
		result.add( rootBlock );
		result.add( new BlockBlank( file, "\n" ));

		return result;
	}
}
