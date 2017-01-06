/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.dsl.parsing;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.roboconf.core.model.ParsingError;

/**
 * A bean that contains information about a configuration file.
 * @author Vincent Zurczak - Linagora
 */
public class FileDefinition {

	public static final int UNDETERMINED = 0;
	public static final int GRAPH = 1;
	public static final int INSTANCE = 2;
	public static final int AGGREGATOR = 3;
	public static final int EMPTY = 4;

	private File editedFile;
	private int fileType = UNDETERMINED;
	private final List<ParsingError> parsingErrors = new ArrayList<> ();
	private final List<AbstractBlock> blocks = new ArrayList<> ();


	/**
	 * Constructor.
	 * @param editedFile not null
	 */
	public FileDefinition( File editedFile ) {
		this.editedFile = editedFile;
	}

	/**
	 * @param editedFile the editedFile to set
	 */
	public void setEditedFile( File editedFile ) {
		this.editedFile = editedFile;
	}

	/**
	 * @return the editedFile
	 */
	public File getEditedFile() {
		return this.editedFile;
	}

	/**
	 * @return the parsingErrors
	 */
	public List<ParsingError> getParsingErrors() {
		return this.parsingErrors;
	}

	/**
	 * @return the blocks
	 */
	public List<AbstractBlock> getBlocks() {
		return this.blocks;
	}

	/**
	 * @return the file type, {@link FileDefinition#GRAPH} or {@link #INSTANCE}
	 */
	public int getFileType() {
		return this.fileType;
	}

	/**
	 * @param fileType the file type
	 * <p>
	 * {@link FileDefinition#GRAPH} or {@link #INSTANCE} or {@link #AGGREGATOR}
	 * </p>
	 */
	public void setFileType( int fileType ) {
		if( fileType != GRAPH
				&& fileType != INSTANCE
				&& fileType != AGGREGATOR
				&& fileType != EMPTY )
			throw new IllegalArgumentException( "The file type was expected to be GRAPH, INSTANCE, AGGREGATOR or EMPTY." );

		this.fileType = fileType;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		sb.append( this.editedFile.getName());
		sb.append( " with " );
		sb.append( this.blocks.size());
		sb.append( " block" );
		if( this.blocks.size() > 1 )
			sb.append( "s" );

		return sb.toString();
	}

	/**
	 * Transforms a file type into a string.
	 * @param fileType a file type
	 * @return a string version of the file type (never null)
	 */
	public static String fileTypeAsString( int fileType ) {

		String result;
		switch( fileType ) {
		case AGGREGATOR:
			result = "aggregator";
			break;

		case GRAPH:
			result = "graph";
			break;

		case INSTANCE:
			result = "intsnace";
			break;

		case UNDETERMINED:
			result = "undetermined";
			break;

		default:
			result = "unknown";
			break;
		}

		return result;
	}
}
