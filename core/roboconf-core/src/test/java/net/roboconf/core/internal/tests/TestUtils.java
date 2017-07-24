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

package net.roboconf.core.internal.tests;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.junit.Assert;

import net.roboconf.core.dsl.ParsingModelIoTest;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TestUtils {

	/**
	 * @return true if the current OS is part of the Linux systems
	 */
	public static boolean isUnix() {
		String os = System.getProperty("os.name").toLowerCase();
		return os.contains( "nix" )  || os.contains( "nux" ) || os.contains( "aix" ) || os.contains( "freebsd" );
	}


	/**
	 * @return true if the current OS is part of the Windows systems
	 */
	public static boolean isWindows() {
		String os = System.getProperty("os.name").toLowerCase();
		return os.contains( "win" );
	}


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
	 * Finds the location of an application directory located in the "roboconf-core" module.
	 * @param currentDirectory the current directory (generally, <code>new File( "." );</code>).
	 * @param appName the application's name (not null)
	 * @return a non-null file (that may not exist
	 */
	public static File findApplicationDirectory( String appName ) throws IOException {

		// This method must support test execution from Maven and IDE (e.g. Eclipse).
		String suffix = "core/roboconf-core/src/test/resources/applications/" + appName;
		File result = new File( "../../" + suffix ).getCanonicalFile();

		return result;
	}


	/**
	 * Finds a test file.
	 * @param fileName must start with '/'
	 * @param clazz a class to search the class path
	 * @return an existing file (never null)
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static File findTestFile( String fileName, Class<?> clazz )
	throws IOException, URISyntaxException {

		URL url = clazz.getResource( fileName );
		File file;
		if( url == null
				|| ! (file = new File( url.toURI())).exists())
			throw new IOException( "Could not find the resource file." );

		return file;
	}


	/**
	 * @return a non-null map associated a ZIP entry name with its text content
	 */
	public static Map<String,String> buildZipContent() {

		Map<String,String> entryToContent = new LinkedHashMap<> ();

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
					try {
						Utils.copyStreamUnsafelyUseWithCaution( is, zos );

					} finally {
						Utils.closeQuietly( is );
					}
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
			String fileContent = Utils.readFileContent( extractedFile );
			Assert.assertEquals( entry.getValue(), fileContent );
		}
	}


	/**
	 * Gets the value of an internal field.
	 * <p>
	 * It is sometimes useful during tests to access a field that should remain
	 * private in a normal execution. This method requires permissions to access
	 * private fields.
	 * </p>
	 * <p>
	 * Super class are searched too.
	 * </p>
	 *
	 * @param o the object from which the field must be retrieved
	 * @param fieldName the field name
	 * @param clazz the class of the internal field
	 * @return the internal field's value or null if this field was not found
	 * @throws IllegalAccessException if the field could not be read
	 */
	public static <T> T getInternalField( Object o, String fieldName, Class<T> clazz )
	throws IllegalAccessException {

		Object fieldValue = null;
		for( Class<?> c = o.getClass(); c != null && fieldValue == null; c = c.getSuperclass()) {
			try {
				Field field = c.getDeclaredField( fieldName );
				field.setAccessible( true );
				fieldValue = field.get( o );

			} catch( NoSuchFieldException e ) {
				// nothing
			}
		}

		return clazz.cast( fieldValue );
	}


	/**
	 * A log handler that writes records in a string buffer.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class StringHandler extends Handler {
		private final StringBuilder sb = new StringBuilder();

		@Override
		public void close() throws SecurityException {
			// nothing
		}

		@Override
		public void flush() {
			// nothing
		}

		@Override
		public void publish( LogRecord rec ) {
			this.sb.append( rec.getMessage() + "\n" );
		}

		public String getLogs() {
			return this.sb.toString();
		}

		public StringBuilder getStringBuilder() {
			return this.sb;
		}
	}
}
