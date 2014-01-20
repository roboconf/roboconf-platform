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

package net.roboconf.core.internal.tests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.io.ParsingModelIoTest;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TestUtils {

	/**
	 * Finds test files.
	 * @param dirName must start with '/'
	 * @return a non-null list
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static List<File> findTestFiles( String dirName ) throws IOException, URISyntaxException {

		URL url = ParsingModelIoTest.class.getResource( dirName );
		File dir = new File( url.toURI());
		if( ! dir.exists())
			throw new IOException( "Could not resolve the resource directory." );

		File[] resources = dir.listFiles();
		if( resources == null )
			throw new IOException( "Could not list the resource files." );

		return Arrays.asList( resources );
	}


	/**
	 * Finds a test file.
	 * @param fileName must start with '/'
	 * @return an existing file (never null)
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static File findTestFile( String fileName ) throws IOException, URISyntaxException {

		URL url = ParsingModelIoTest.class.getResource( fileName );
		File file = new File( url.toURI());
		if( ! file.exists())
			throw new IOException( "Could not find the resource file." );

		return file;
	}


	/**
	 * Reads a text file content and returns it as a string.
	 * <p>
	 * The file is tried to be read with UTF-8 encoding.
	 * If it fails, the default system encoding is used.
	 * </p>
	 *
	 * @param file the file whose content must be loaded
	 * @return the file content
	 * @throws IOException if the file content could not be read
	 */
	public static String readFileContent( File file ) throws IOException {

		String result = null;
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Utils.copyStream( file, os );
		result = os.toString( "UTF-8" );

		return result;
	}
}
