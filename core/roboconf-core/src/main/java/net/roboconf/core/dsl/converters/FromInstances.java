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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.roboconf.core.dsl.ParsingConstants;
import net.roboconf.core.dsl.parsing.AbstractBlock;
import net.roboconf.core.dsl.parsing.BlockBlank;
import net.roboconf.core.dsl.parsing.BlockComment;
import net.roboconf.core.dsl.parsing.BlockInstanceOf;
import net.roboconf.core.dsl.parsing.BlockProperty;
import net.roboconf.core.dsl.parsing.FileDefinition;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;

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
	 * @param saveRuntimeInformation true to save runtime information (such as IP...), false otherwise
	 * @return a non-null file definition
	 */
	public FileDefinition buildFileDefinition( Collection<Instance> rootInstances, File targetFile, boolean addComment, boolean saveRuntimeInformation ) {

		FileDefinition result = new FileDefinition( targetFile );
		result.setFileType( FileDefinition.INSTANCE );
		if( addComment ) {
			String s = "# File created from an in-memory model,\n# without a binding to existing files.";
			BlockComment initialComment = new BlockComment( result, s );
			result.getBlocks().add( initialComment );
			result.getBlocks().add( new BlockBlank( result, "\n" ));
		}

		for( Instance rootInstance : rootInstances )
			result.getBlocks().addAll( buildInstanceOf( result, rootInstance, addComment, saveRuntimeInformation ));

		return result;
	}


	private Collection<AbstractBlock> buildInstanceOf( FileDefinition file, Instance rootInstance, boolean addComment, boolean saveRuntimeInformation ) {
		Collection<AbstractBlock> result = new ArrayList<> ();

		// Process the root instance
		Map<Instance,BlockInstanceOf> instanceToBlock = new ConcurrentHashMap<> ();
		BlockInstanceOf rootBlock = new BlockInstanceOf( file );
		instanceToBlock.put( rootInstance, rootBlock );

		List<Instance> toProcess = new ArrayList<> ();
		toProcess.add( rootInstance );
		while( ! toProcess.isEmpty()) {

			// Process the current instance
			Instance instance = toProcess.remove( 0 );
			BlockInstanceOf currentBlock = instanceToBlock.get( instance );
			currentBlock.setName( instance.getComponent().getName());

			BlockProperty p;
			if( ! Utils.isEmptyOrWhitespaces( instance.getName())) {
				p = new BlockProperty( file, ParsingConstants.PROPERTY_INSTANCE_NAME, instance.getName());
				currentBlock.getInnerBlocks().add( p );
			}

			if( ! instance.channels.isEmpty()) {
				String s = Utils.format( instance.channels, ", " );
				p = new BlockProperty( file, ParsingConstants.PROPERTY_INSTANCE_CHANNELS, s );
				currentBlock.getInnerBlocks().add( p );
			}

			for( Map.Entry<String,String> export : instance.overriddenExports.entrySet()) {
				p = new BlockProperty( file, export.getKey(), export.getValue());
				currentBlock.getInnerBlocks().add( p );
			}

			// Runtime information
			if( saveRuntimeInformation ) {
				p = new BlockProperty( file, ParsingConstants.PROPERTY_INSTANCE_STATE, instance.getStatus().toString());
				currentBlock.getInnerBlocks().add( p );

				StringBuilder sb = new StringBuilder();
				for( Iterator<Map.Entry<String,String>> it = instance.data.entrySet().iterator(); it.hasNext(); ) {
					Map.Entry<String,String> entry = it.next();
					sb.append( entry.getKey());
					sb.append( " = " );
					sb.append( entry.getValue());

					if( it.hasNext())
						sb.append( ", " );
				}

				if( sb.length() > 0 ) {
					p = new BlockProperty( file, ParsingConstants.PROPERTY_INSTANCE_DATA, sb.toString());
					currentBlock.getInnerBlocks().add( p );
				}
			}

			// Update the parent
			BlockInstanceOf parentBlock = instance.getParent() == null ? null : instanceToBlock.get( instance.getParent());
			if( parentBlock != null ) {
				parentBlock.getInnerBlocks().add( new BlockBlank( file, "" ));
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
