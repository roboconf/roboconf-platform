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

package net.roboconf.core.internal.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import junit.framework.Assert;
import net.roboconf.core.dsl.ParsingModelIoTest;
import net.roboconf.core.utils.Utils;

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
		return findTestFile( fileName, ParsingModelIoTest.class );
	}


	/**
	 * Finds a test file.
	 * @param fileName must start with '/'
	 * @param clazz a class to search the class path
	 * @return an existing file (never null)
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static File findTestFile( String fileName, Class<?> clazz ) throws IOException, URISyntaxException {

		URL url = clazz.getResource( fileName );
		File file;
		if( url == null
				|| ! (file = new File( url.toURI())).exists())
			throw new IOException( "Could not find the resource file." );

		return file;
	}


	/**
	 * Gets the content of an URI.
	 * @param uri an URI
	 * @return the content available at this address
	 * @throws IOException if something went wrong
	 */
	public static String readUriContent( URI uri ) throws IOException {

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		InputStream in = null;
		try {
			in = uri.toURL().openStream();
			Utils.copyStream( in, out );

		} catch( Exception e ) {
			// nothing

		} finally {
			Utils.closeQuietly( in );
		}

		return out.toString( "UTF-8" );
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


	/**
	 * @return a non-null map associated a ZIP entry name with its text content
	 */
	public static Map<String,String> buildZipContent() {

		Map<String,String> entryToContent = new LinkedHashMap<String,String> ();

		entryToContent.put( "readme.txt", "This is a readme file." );
		entryToContent.put( "graph/main.graph", "import facets.graph;\nimport components.graph;" );
		entryToContent.put( "graph/facets.graph", "# nothing yet" );
		entryToContent.put( "graph/components.graph", "# nothing here too" );
		entryToContent.put( "descriptor/application-descriptor.properties", "application-name = Unit Test" );
		entryToContent.put( "instances/initial-deployment.instances", "# No instance" );
		entryToContent.put( "some/very/low/folder/demo.txt", "Whatever..." );
		entryToContent.put( "graph/", null );
		entryToContent.put( "anotherdir/", null );
		entryToContent.put( "anotherdir/deeper/", null );

		return entryToContent;
	}


	/**
	 * Creates a ZIP file from the map.
	 * @param entryToContent a map (key = ZIP entry, value = entry content, null for a directory)
	 * @param targetZipFile
	 */
	public static void createZipFile( Map<String,String> entryToContent, File targetZipFile ) throws IOException {

		ZipOutputStream zos = null;
		try {
			zos = new ZipOutputStream( new FileOutputStream( targetZipFile ));
			for( Map.Entry<String,String> entry : entryToContent.entrySet()) {
				zos.putNextEntry( new ZipEntry( entry.getKey()));

				if( entry.getValue() != null ) {
					ByteArrayInputStream is = new ByteArrayInputStream( entry.getValue().getBytes( "UTF-8" ));
					Utils.copyStream( is, zos );
				}

				zos.closeEntry();
			}

		} finally {
			Utils.closeQuietly( zos );
		}
	}


	/**
	 * Compares an assumed ZIP file with a content described in a map.
	 * @param zipFile
	 * @param entryToContent
	 * @throws ZipException
	 * @throws IOException
	 */
	public static void compareZipContent( File zipFile, Map<String,String> entryToContent ) throws IOException {

		File tempDir = new File( System.getProperty( "java.io.tmpdir" ), UUID.randomUUID().toString());
		if( ! tempDir.mkdir())
			Assert.fail( "Failed to create a temporary directory." );

		try {
			Utils.extractZipArchive( zipFile, tempDir );
			compareUnzippedContent( tempDir, entryToContent );

		} finally {
			Utils.deleteFilesRecursively( tempDir );
		}
	}


	/**
	 * Compares an assumed ZIP file with a content described in a map.
	 * @param rootDirectory the root directory of the unzipped content
	 * @param entryToContent the map associating entries and content (null for directories)
	 * @throws IOException
	 */
	public static void compareUnzippedContent( File rootDirectory, Map<String,String> entryToContent ) throws IOException {

		for( Map.Entry<String,String> entry : entryToContent.entrySet()) {
			File extractedFile = new File( rootDirectory, entry.getKey());
			Assert.assertTrue( "Missing entry: " + entry.getKey(), extractedFile.exists());

			if( entry.getValue() == null ) {
				Assert.assertTrue( entry.getKey() + " was supposed to be a directory.", extractedFile.isDirectory());
				continue;
			}

			Assert.assertTrue( entry.getKey() + " was supposed to be a file.", extractedFile.isFile());
			String fileContent = TestUtils.readFileContent( extractedFile );
			Assert.assertEquals( entry.getValue(), fileContent );
		}
	}
}
