/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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

package net.roboconf.core.model.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import net.roboconf.core.internal.model.parsing.FileDefinitionParser;
import net.roboconf.core.internal.model.parsing.FileDefinitionSerializer;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.utils.Utils;

/**
 * An API set of helpers to read and write a parsing model.
 * @author Vincent Zurczak - Linagora
 */
public final class ParsingModelIo {

	/**
	 * Constructor.
	 */
	private ParsingModelIo() {
		// nothing
	}


	/**
	 * Reads a definition file.
	 * @param relationsFileUri the file URI
	 * @param ignoreComments true to ignore comments during parsing
	 * @return an instance of {@link FileDefinition} (never null)
	 * <p>
	 * Parsing errors are stored in the result.<br />
	 * See {@link FileRelations#getParingErrors()}.
	 * </p>
	 */
	public static FileDefinition readConfigurationFile( URI relationsFileUri, boolean ignoreComments ) {
		FileDefinitionParser parser = new FileDefinitionParser( relationsFileUri, ignoreComments );
		return parser.read();
	}


	/**
	 * Reads a definition file.
	 * @param relationsFile the relations file
	 * @param ignoreComments true to ignore comments during parsing
	 * @return an instance of {@link FileDefinition} (never null)
	 * <p>
	 * Parsing errors are stored in the result.<br />
	 * See {@link FileRelations#getParingErrors()}.
	 * </p>
	 */
	public static FileDefinition readConfigurationFile( File relationsFile, boolean ignoreComments ) {
		FileDefinitionParser parser = new FileDefinitionParser( relationsFile, ignoreComments );
		return parser.read();
	}


	/**
	 * Reads a definition file.
	 * @param relationsFileUri the file URI
	 * @param ignoreComments true to ignore comments during parsing
	 * @return an instance of {@link FileDefinition} (never null)
	 * <p>
	 * Parsing errors are stored in the result.<br />
	 * See {@link FileRelations#getParingErrors()}.
	 * </p>
	 *
	 * @throws URISyntaxException if the given string is not a valid URI
	 */
	public static FileDefinition readConfigurationFile( String relationsFileUri, boolean ignoreComments ) throws URISyntaxException {
		FileDefinitionParser parser = new FileDefinitionParser( relationsFileUri, ignoreComments );
		return parser.read();
	}


	/**
	 * Writes a model into a string.
	 * @param relationsFile the relations file
	 * @param writeComments true to write comments
	 * @param lineSeparator the line separator (if null, the OS' one is used)
	 * @return a string (never null)
	 */
	public static String writeConfigurationFile( FileDefinition relationsFile, boolean writeComments, String lineSeparator ) {
		return new FileDefinitionSerializer( lineSeparator ).write( relationsFile, writeComments );
	}


	/**
	 * Saves the model into the original file.
	 * @param relationsFile the relations file
	 * @param writeComments true to write comments
	 * @param lineSeparator the line separator (if null, the OS' one is used)
	 * @throws IOException if the file could not be saved
	 */
	public static void saveRelationsFile( FileDefinition relationsFile, boolean writeComments, String lineSeparator )
	throws IOException {

		if( relationsFile.getEditedFile() == null )
			throw new IOException( "Save operation could not be performed. The model was not loaded from a local file." );

		saveRelationsFileInto( relationsFile.getEditedFile(), relationsFile, writeComments, lineSeparator );
	}


	/**
	 * Saves the model into the original file.
	 * @param targetFile the target file (may not exist)
	 * @param relationsFile the relations file
	 * @param writeComments true to write comments
	 * @param lineSeparator the line separator (if null, the OS' one is used)
	 * @throws IOException if the file could not be saved
	 */
	public static void saveRelationsFileInto(
			File targetFile,
			FileDefinition relationsFile,
			boolean writeComments,
			String lineSeparator )
	throws IOException {

		String s = writeConfigurationFile( relationsFile, writeComments, lineSeparator );
		Utils.copyStream( new ByteArrayInputStream( s.getBytes( "UTF-8" )), targetFile );
	}
}
