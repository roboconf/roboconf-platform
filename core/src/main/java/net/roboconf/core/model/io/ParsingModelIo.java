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

import net.roboconf.core.internal.model.parsing.RelationsParser;
import net.roboconf.core.internal.model.parsing.RelationsSerializer;
import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.parsing.FileRelations;

/**
 * An API set of helpers to read and write a parsing model.
 * @author Vincent Zurczak - Linagora
 */
public class ParsingModelIo {

	/**
	 * Reads a relation file.
	 * @param relationsFileUri the file URI
	 * @param ignoreComments true to ignore comments during parsing
	 * @return an instance of {@link FileRelations} (never null)
	 * <p>
	 * Parsing errors are stored in the result.<br />
	 * See {@link FileRelations#getParingErrors()}.
	 * </p>
	 */
	public static FileRelations readRelationsFile( URI relationsFileUri, boolean ignoreComments ) {
		RelationsParser parser = new RelationsParser( relationsFileUri, ignoreComments );
		return parser.read();
	}


	/**
	 * Reads a relation file.
	 * @param relationsFile the relations file
	 * @param ignoreComments true to ignore comments during parsing
	 * @return an instance of {@link FileRelations} (never null)
	 * <p>
	 * Parsing errors are stored in the result.<br />
	 * See {@link FileRelations#getParingErrors()}.
	 * </p>
	 */
	public static FileRelations readRelationsFile( File relationsFile, boolean ignoreComments ) {
		RelationsParser parser = new RelationsParser( relationsFile, ignoreComments );
		return parser.read();
	}


	/**
	 * Reads a relation file.
	 * @param relationsFileUri the file URI
	 * @param ignoreComments true to ignore comments during parsing
	 * @return an instance of {@link FileRelations} (never null)
	 * <p>
	 * Parsing errors are stored in the result.<br />
	 * See {@link FileRelations#getParingErrors()}.
	 * </p>
	 *
	 * @throws URISyntaxException if the given string is not a valid URI
	 */
	public static FileRelations readRelationsFile( String relationsFileUri, boolean ignoreComments ) throws URISyntaxException {
		RelationsParser parser = new RelationsParser( relationsFileUri, ignoreComments );
		return parser.read();
	}


	/**
	 * Writes the model into a string.
	 * @param relationsFile the relations file
	 * @param writeComments true to write comments
	 * @param lineSeparator the line separator (if null, the OS' one is used)
	 * @return a string (never null)
	 */
	public static String writeRelationsFile( FileRelations relationsFile, boolean writeComments, String lineSeparator ) {
		return new RelationsSerializer( lineSeparator ).write( relationsFile, writeComments );
	}


	/**
	 * Saves the model into the original file.
	 * @param relationsFile the relations file
	 * @param writeComments true to write comments
	 * @param lineSeparator the line separator (if null, the OS' one is used)
	 * @throws IOException
	 */
	public static void saveRelationsFile( FileRelations relationsFile, boolean writeComments, String lineSeparator )
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
	 * @throws IOException
	 */
	public static void saveRelationsFileInto(
			File targetFile,
			FileRelations relationsFile,
			boolean writeComments,
			String lineSeparator )
	throws IOException {

		String s = writeRelationsFile( relationsFile, writeComments, lineSeparator );
		Utils.copyStream( new ByteArrayInputStream( s.getBytes( "UTF-8" )), targetFile );
	}
}
