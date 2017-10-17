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

package net.roboconf.core.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.internal.tests.TestUtils.StringHandler;
import net.roboconf.core.utils.Utils.DirectoryFileFilter;
import net.roboconf.core.utils.Utils.FileNameComparator;

/**
 * @author Vincent Zurczak - Linagora
 */
public class UtilsTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testDeleteFilesRecursively() {

		File tmpDir = new File( System.getProperty( "java.io.tmpdir" ), UUID.randomUUID().toString());
		if( ! tmpDir.mkdir())
			Assert.fail( "Could not create a temporary directory." );

		String[] dirs = { "dir1", "dir1/dir2", "dir1/dir2/dir3", "dir1/dir2/dir4", "dir2", "dir1/dir54" };
		for( String dir : dirs ) {
			File f = new File( tmpDir, dir );
			if( ! f.mkdir())
				Assert.fail( "Could not create a sub-directory: " + dir );
		}

		String[] files = { "test.txt", "te.txt", "dir1/test.txt", "dir2/some.txt", "dir1/dir2/dir3/pol.txt" };
		try {
			for( String file : files ) {
				File f = new File( tmpDir, file );
				if( ! f.createNewFile())
					Assert.fail( "Could not create a file: " + file );
			}

		} catch( IOException e ) {
			Assert.fail( "Could not create a file. " + e.getMessage());
		}

		Assert.assertTrue( tmpDir.exists());
		try {
			Utils.deleteFilesRecursively( tmpDir );
			Assert.assertFalse( "Temp directory could not be deleted: " + tmpDir.getName(), tmpDir.exists());

		} catch( IOException e ) {
			Assert.fail( "Failed to delete the temporary directory." );
		}

		try {
			Utils.deleteFilesRecursively((File) null);

		} catch( IOException e ) {
			Assert.fail( "Null file must be supported" );
		}

		try {
			Utils.deleteFilesRecursively((File[]) null);

		} catch( IOException e ) {
			Assert.fail( "Null file array must be supported" );
		}

		try {
			File[] nullFiles = new File[] { null, null };
			Utils.deleteFilesRecursively( nullFiles );

		} catch( IOException e ) {
			Assert.fail( "Array of null files must be supported" );
		}

		try {
			Utils.deleteFilesRecursively( new File( "inexisting-file" ));

		} catch( IOException e ) {
			Assert.fail( "Inexisting files must be supported" );
		}

		Utils.deleteFilesRecursivelyAndQuietly( new File( "another-inexisting-file" ));
	}


	@Test
	public void testCopyStreamSafely() throws Exception {

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ByteArrayInputStream in = new ByteArrayInputStream( "toto".getBytes( "UTF-8" ));
		Utils.copyStreamSafely( in, os );

		Assert.assertEquals( "toto", os.toString( "UTF-8" ));
	}


	@Test
	public void testSplitNicely() {

		List<String> result = Utils.splitNicely( "once, upon, a , time   ", "," );
		Assert.assertEquals( 4, result.size());
		Assert.assertEquals( "once", result.get( 0 ));
		Assert.assertEquals( "upon", result.get( 1 ));
		Assert.assertEquals( "a", result.get( 2 ));
		Assert.assertEquals( "time", result.get( 3 ));

		result = Utils.splitNicely( "once \n\n, upon, a , time \n  ", "\n" );
		Assert.assertEquals( 4, result.size());
		Assert.assertEquals( "once", result.get( 0 ));
		Assert.assertEquals( "", result.get( 1 ).trim());
		Assert.assertEquals( ", upon, a , time", result.get( 2 ));
		Assert.assertEquals( "", result.get( 3 ).trim());

		result = Utils.splitNicely( "once $ $a$ $$ time", "$" );
		Assert.assertEquals( 6, result.size());
		Assert.assertEquals( "once", result.get( 0 ));
		Assert.assertEquals( "", result.get( 1 ).trim());
		Assert.assertEquals( "a", result.get( 2 ));
		Assert.assertEquals( "", result.get( 3 ).trim());
		Assert.assertEquals( "", result.get( 4 ).trim());
		Assert.assertEquals( "time", result.get( 5 ));
	}


	@Test( expected = IllegalArgumentException.class )
	public void testSplitNicely_illegalArgument_1() {
		Utils.splitNicely( "once, upon, a , time   ", "" );
	}


	@Test( expected = IllegalArgumentException.class )
	public void testSplitNicely_illegalArgument_2() {
		Utils.splitNicely( "once, upon, a , time   ", null );
	}


	@Test
	public void testSplitNicelyWithPattern() {

		List<String> result = Utils.splitNicelyWithPattern( "once upon a , time   ", ",|\\s" );
		Assert.assertEquals( 6, result.size());
		Assert.assertEquals( "once", result.get( 0 ));
		Assert.assertEquals( "upon", result.get( 1 ));
		Assert.assertEquals( "a", result.get( 2 ));
		Assert.assertEquals( "", result.get( 3 ));
		Assert.assertEquals( "", result.get( 4 ));
		Assert.assertEquals( "time", result.get( 5 ));
	}


	@Test( expected = IllegalArgumentException.class )
	public void testSplitNicelyWithPattern_illegalArgument_1() {
		Utils.splitNicelyWithPattern( "once, upon, a , time   ", "" );
	}


	@Test( expected = IllegalArgumentException.class )
	public void testSplitNicelyWithPattern_illegalArgument_2() {
		Utils.splitNicelyWithPattern( "once, upon, a , time   ", null );
	}


	@Test
	public void testFilterEmptyValues() {

		List<String> list = new ArrayList<> ();
		Assert.assertEquals( Collections.emptyList(), Utils.filterEmptyValues( list ));

		list.add( "1" );
		list.add( null );
		list.add( "1" );
		list.add( "  " );
		list.add( "2" );

		Assert.assertEquals( Arrays.asList( "1", "1", "2" ), Utils.filterEmptyValues( list ));
	}


	@Test
	public void testFormat() {

		List<String> list = Arrays.asList( "1", "2", "", "3", "4" );
		Assert.assertEquals( "1, 2, , 3, 4", Utils.format( list, ", " ));
		Assert.assertEquals( "1 - 2 -  - 3 - 4", Utils.format( list, " - " ));
	}


	@Test
	public void testWriteStringInto() throws Exception {

		File f = this.folder.newFile();
		String content = "whatever\n\thop   ";
		Utils.writeStringInto( content, f );

		Assert.assertEquals( content, Utils.readFileContent( f ));
	}


	@Test
	public void testAppendStringInto() throws Exception {

		File f = this.folder.newFile();
		Assert.assertTrue( f.delete());

		String content = "whatever\n\thop   ";
		Assert.assertFalse( f.exists());

		Utils.appendStringInto( content, f );
		Assert.assertTrue( f.exists());

		Assert.assertEquals( content, Utils.readFileContent( f ));
		Utils.appendStringInto( "\npop", f );
		Assert.assertEquals( content + "\npop", Utils.readFileContent( f ));
	}


	@Test
	public void testReadFileContentQuietly() throws Exception {

		String s = "this is\na\test\t";
		File output = this.folder.newFile();
		Utils.writeStringInto( s, output );

		String readS = Utils.readFileContentQuietly( output, Logger.getLogger( getClass().getName()));
		Assert.assertEquals( s, readS );
	}


	@Test
	public void testReadFileContentQuietly_exception() throws Exception {

		File output = this.folder.newFolder();
		String readS = Utils.readFileContentQuietly( output, Logger.getLogger( getClass().getName()));
		Assert.assertEquals( "", readS );
	}


	@Test
	public void testReadFileContentQuietly_inexistingFile() throws Exception {

		String readS = Utils.readFileContentQuietly( new File( "inexisting" ), Logger.getLogger( getClass().getName()));
		Assert.assertEquals( "", readS );
	}


	@Test
	public void testReadFileContentQuietly_nullFile() throws Exception {

		String readS = Utils.readFileContentQuietly( null, Logger.getLogger( getClass().getName()));
		Assert.assertEquals( "", readS );
	}


	@Test
	public void testIsEmptyOrWhitespaces() {

		Assert.assertTrue( Utils.isEmptyOrWhitespaces( null ));
		Assert.assertTrue( Utils.isEmptyOrWhitespaces( "" ));
		Assert.assertTrue( Utils.isEmptyOrWhitespaces( "   " ));
		Assert.assertTrue( Utils.isEmptyOrWhitespaces( " \n  \t" ));
		Assert.assertFalse( Utils.isEmptyOrWhitespaces( " a\n  \t" ));
		Assert.assertFalse( Utils.isEmptyOrWhitespaces( "b" ));
	}

	@Test
	public void testExpandString() {

		// Null properties
		Assert.assertEquals( "toto", Utils.expandTemplate( "toto", null ));

		// No property
		Properties params = new Properties();
		Assert.assertEquals( "toto", Utils.expandTemplate( "toto", params ));

		// With properties
		params.setProperty("firstname", "James");
		params.setProperty("lastname", "Bond");
		String tmpl = "My name is {{lastname}}, {{ firstname }} {{ lastname }}!";
		Assert.assertEquals(
				"My name is Bond, James Bond!",
				Utils.expandTemplate(tmpl, params));

		tmpl = "This is an {{ unknown }} parameter";
		Assert.assertEquals( tmpl, Utils.expandTemplate(tmpl, params));
	}


	@Test
	public void testExtractZipArchive() throws Exception {

		// Prepare the original ZIP
		File zipFile = this.folder.newFile( "roboconf_test.zip" );
		Map<String,String> entryToContent = TestUtils.buildZipContent();

		TestUtils.createZipFile( entryToContent, zipFile );
		TestUtils.compareZipContent( zipFile, entryToContent );

		// Prepare the output directory
		File existingDirectory = this.folder.newFolder( "roboconf_test" );
		Assert.assertTrue( existingDirectory.exists());
		Assert.assertEquals( 0, Utils.listAllFiles( existingDirectory ).size());

		// Extract
		Utils.extractZipArchive( zipFile, existingDirectory );

		// And compare
		Assert.assertNotSame( 0, Utils.listAllFiles( existingDirectory ).size());
		Map<String,String> fileToContent = Utils.storeDirectoryResourcesAsString( existingDirectory );
		for( Map.Entry<String,String> entry : fileToContent.entrySet()) {
			Assert.assertTrue( entryToContent.containsKey( entry.getKey()));
			String value = entryToContent.remove( entry.getKey());
			Assert.assertEquals( entry.getValue(), value );
		}

		// Only directories should remain
		for( Map.Entry<String,String> entry : entryToContent.entrySet()) {
			Assert.assertNull( entry.getKey(), entry.getValue());
		}
	}


	@Test
	public void testExtractZipArchive_withOptions() throws Exception {

		// Prepare the original ZIP
		File zipFile = this.folder.newFile( "roboconf_test.zip" );
		Map<String,String> entryToContent = TestUtils.buildZipContent();

		TestUtils.createZipFile( entryToContent, zipFile );
		TestUtils.compareZipContent( zipFile, entryToContent );

		// Prepare the output directory
		File existingDirectory = this.folder.newFolder( "roboconf_test" );
		Assert.assertTrue( existingDirectory.exists());
		Assert.assertEquals( 0, Utils.listAllFiles( existingDirectory ).size());

		// Extract
		final String pattern = "graph/.*";
		Utils.extractZipArchive( zipFile, existingDirectory, pattern, "graph/" );

		// And compare
		Assert.assertNotSame( 0, Utils.listAllFiles( existingDirectory ).size());
		Map<String,String> fileToContent = Utils.storeDirectoryResourcesAsString( existingDirectory );
		Assert.assertEquals( 3, fileToContent.size());

		for( Map.Entry<String,String> entry : fileToContent.entrySet()) {
			Assert.assertTrue( entryToContent.containsKey( "graph/" + entry.getKey()));
			String value = entryToContent.remove( "graph/" + entry.getKey());
			Assert.assertEquals( entry.getValue(), value );
		}
	}


	@Test
	public void testExtractZipArchive_withOptions_invalidPrefix() throws Exception {

		// Prepare the original ZIP
		File zipFile = this.folder.newFile( "roboconf_test.zip" );
		Map<String,String> entryToContent = TestUtils.buildZipContent();

		TestUtils.createZipFile( entryToContent, zipFile );
		TestUtils.compareZipContent( zipFile, entryToContent );

		// Prepare the output directory
		File existingDirectory = this.folder.newFolder( "roboconf_test" );
		Assert.assertTrue( existingDirectory.exists());
		Assert.assertEquals( 0, Utils.listAllFiles( existingDirectory ).size());

		// Extract
		final String pattern = "graph/.*";
		Utils.extractZipArchive( zipFile, existingDirectory, pattern, "invalid/" );

		// And compare
		Assert.assertEquals( 3, Utils.listAllFiles( existingDirectory ).size());
		Map<String,String> fileToContent = Utils.storeDirectoryResourcesAsString( existingDirectory );

		for( Map.Entry<String,String> entry : fileToContent.entrySet()) {
			Assert.assertTrue( entryToContent.containsKey( entry.getKey()));
			String value = entryToContent.remove( entry.getKey());
			Assert.assertEquals( entry.getValue(), value );
		}
	}


	@Test
	public void testExtractZipArchive_inexistingDirectory() throws Exception {

		// Prepare the original ZIP
		File zipFile = this.folder.newFile( "roboconf_test.zip" );
		Map<String,String> entryToContent = TestUtils.buildZipContent();
		TestUtils.createZipFile( entryToContent, zipFile );

		// Prepare the output directory
		File unexistingDirectory = this.folder.newFolder( "roboconf_test" );
		if( ! unexistingDirectory.delete())
			throw new IOException( "Failed to delete a directory." );

		Assert.assertFalse( unexistingDirectory.exists());

		// Extract
		Utils.extractZipArchive( zipFile, unexistingDirectory );
		Assert.assertTrue( unexistingDirectory.exists());

		// And compare
		Assert.assertNotSame( 0, Utils.listAllFiles( unexistingDirectory ).size());
		Map<String,String> fileToContent = Utils.storeDirectoryResourcesAsString( unexistingDirectory );
		for( Map.Entry<String,String> entry : fileToContent.entrySet()) {
			Assert.assertTrue( entryToContent.containsKey( entry.getKey()));
			String value = entryToContent.remove( entry.getKey());
			Assert.assertEquals( entry.getValue(), value );
		}

		// Only directories should remain
		for( Map.Entry<String,String> entry : entryToContent.entrySet()) {
			Assert.assertNull( entry.getKey(), entry.getValue());
		}
	}


	@Test( expected = IllegalArgumentException.class )
	public void testExtractZipArchive_illegalArgument_1() throws Exception {
		File existingFile = new File( System.getProperty( "java.io.tmpdir" ));
		Utils.extractZipArchive( new File( "file-that-does-not.exists" ), existingFile );
	}


	@Test( expected = IllegalArgumentException.class )
	public void testExtractZipArchive_illegalArgument_2() throws Exception {
		File existingFile = new File( System.getProperty( "java.io.tmpdir" ));
		Utils.extractZipArchive( null, existingFile );
	}


	@Test( expected = IllegalArgumentException.class )
	public void testExtractZipArchive_illegalArgument_3() throws Exception {
		File existingFile = new File( System.getProperty( "java.io.tmpdir" ));
		Utils.extractZipArchive( existingFile, null );
	}


	@Test( expected = IllegalArgumentException.class )
	public void testExtractZipArchive_illegalArgument_4() throws Exception {

		File existingFile = new File( System.getProperty( "java.io.tmpdir" ));
		File unexistingFile = new File( existingFile, UUID.randomUUID().toString());

		Assert.assertFalse( unexistingFile.exists());
		Utils.extractZipArchive( existingFile, unexistingFile );
	}


	@Test( expected = IllegalArgumentException.class )
	public void testExtractZipArchive_illegalArgument_5() throws Exception {

		File tempZip = this.folder.newFile( "roboconf_test_zip.zip" );
		File tempFile = this.folder.newFile( "roboconf_test.txt" );
		Utils.extractZipArchive( tempZip, tempFile );
	}


	@Test
	public void testCreateDirectory() throws Exception {

		File dir = this.folder.newFolder();
		File target = new File( dir, "toto/pom" );
		Utils.createDirectory( target );
		Assert.assertTrue( target.exists());
	}


	@Test( expected = IOException.class )
	public void testCreateDirectory_error() throws Exception {

		File dir = this.folder.newFolder();
		File target = new File( dir, "toto/pom" );
		Assert.assertTrue( target.getParentFile().createNewFile());

		Utils.createDirectory( target );
	}


	@Test
	public void testCloseQuietly() throws Exception {

		InputStream in = null;
		Utils.closeQuietly( in );

		in = new ByteArrayInputStream( new byte[ 0 ]);
		Utils.closeQuietly( in );

		OutputStream out = new ByteArrayOutputStream();
		Utils.closeQuietly( out );

		out = null;
		Utils.closeQuietly( out );

		Reader reader = null;
		Utils.closeQuietly( reader );

		reader = new CharArrayReader( new char[ 0 ]);
		Utils.closeQuietly( reader );

		Writer writer = null;
		Utils.closeQuietly( writer );

		writer = new StringWriter();
		Utils.closeQuietly( writer );
	}


	@Test
	public void testCloseQuietly_silentInput() throws Exception {

		InputStream in = new InputStream() {
			@Override
			public int read() throws IOException {
				return 0;
			}

			@Override
			public void close() throws IOException {
				throw new IOException();
			}
		};

		Utils.closeQuietly( in );
	}


	@Test
	public void testCloseQuietly_silentOutput() throws Exception {

		OutputStream out = new OutputStream() {
			@Override
			public void write( int b ) throws IOException {
				// nothing
			}

			@Override
			public void close() throws IOException {
				throw new IOException();
			}
		};

		Utils.closeQuietly( out );
	}


	@Test
	public void testCloseQuietly_silentReader() throws Exception {

		Reader reader = new Reader() {
			@Override
			public int read( char[] cbuf, int off, int len ) throws IOException {
				return 0;
			}

			@Override
			public void close() throws IOException {
				throw new IOException();
			}
		};

		Utils.closeQuietly( reader );
	}


	@Test
	public void testCloseQuietly_silentWriter() throws Exception {

		Writer writer = new Writer() {
			@Override
			public void write( char[] arg0, int arg1, int arg2 ) throws IOException {
				// nothing
			}

			@Override
			public void flush() throws IOException {
				// nothing
			}

			@Override
			public void close() throws IOException {
				throw new IOException();
			}
		};

		Utils.closeQuietly( writer );
	}


	@Test
	public void testWriteException() {

		String msg = "Hello from Roboconf.";
		String stackTrace = Utils.writeExceptionButDoNotUseItForLogging( new Exception( msg ));
		Assert.assertTrue( stackTrace.contains( msg ));
	}


	@Test( expected = IllegalArgumentException.class )
	public void testComputeFileRelativeLocation_failure_notASubFile() {

		final File rootDir = new File( System.getProperty( "java.io.tmpdir" ), "does-not-exist");
		Utils.computeFileRelativeLocation( rootDir, new File( "invalid-path" ));
	}


	@Test( expected = IllegalArgumentException.class )
	public void testComputeFileRelativeLocation_failure_sameFile() {

		final File rootDir = new File( System.getProperty( "java.io.tmpdir" ));
		Utils.computeFileRelativeLocation( rootDir, rootDir );
	}


	@Test
	public void testComputeFileRelativeLocation_success() {

		final File rootDir = new File( System.getProperty( "java.io.tmpdir" ));
		File directChildFile = new File( rootDir, "woo.txt" );
		Assert.assertEquals(
				directChildFile.getName(),
				Utils.computeFileRelativeLocation( rootDir, directChildFile ));

		String indirectChildPath = "dir1/dir2/script.sh";
		File indirectChildFile = new File( rootDir, indirectChildPath );
		Assert.assertEquals(
				indirectChildPath,
				Utils.computeFileRelativeLocation( rootDir, indirectChildFile ));
	}


	@Test
	public void testListAllFiles() throws Exception {

		final File tempDir = this.folder.newFolder( "roboconf_test" );
		String[] paths = new String[] { "dir1", "dir2", "dir1/dir3" };
		for( String path : paths ) {
			if( ! new File( tempDir, path ).mkdir())
				throw new IOException( "Failed to create " + path );
		}

		paths = new String[] { "dir1/toto.txt", "dir2/script.sh", "dir1/dir3/smart.png" };
		for( String path : paths ) {
			if( ! new File( tempDir, path ).createNewFile())
				throw new IOException( "Failed to create " + path );
		}

		List<File> files = Utils.listAllFiles( tempDir );
		Assert.assertEquals( 3, files.size());
		for( String path : paths )
			Assert.assertTrue( path, files.contains( new File( tempDir, path )));
	}


	@Test
	public void testListAllFilesAndDirectories() throws Exception {

		final File tempDir = this.folder.newFolder( "roboconf_test" );
		String[] paths = new String[] { "dir1", "dir2", "dir1/dir3", "dir4" };
		for( String path : paths ) {
			if( ! new File( tempDir, path ).mkdir())
				throw new IOException( "Failed to create " + path );
		}

		paths = new String[] { "dir1/toto.txt", "dir2/script.sh", "dir1/dir3/smart.png" };
		for( String path : paths ) {
			if( ! new File( tempDir, path ).createNewFile())
				throw new IOException( "Failed to create " + path );
		}

		List<File> files = Utils.listAllFiles( tempDir, true );

		// 7 files and directories, mentioned previously, plus the root directory.
		Assert.assertEquals( 8, files.size());
		for( String path : paths )
			Assert.assertTrue( path, files.contains( new File( tempDir, path )));
	}


	@Test( expected = IllegalArgumentException.class )
	public void testListAllFiles_inexistingFile() throws Exception {
		Utils.listAllFiles( new File( "not/existing/file" ));
	}


	@Test( expected = IllegalArgumentException.class )
	public void testListAllFiles_invalidParameter() throws Exception {
		Utils.listAllFiles( this.folder.newFile( "roboconf.txt" ));
	}


	@Test
	public void testFilesListingAreSortedAlphabetically() {

		List<File> list = new ArrayList<> ();
		list.add( new File( "a/toto" ));
		list.add( new File( "a/zorro" ));
		list.add( new File( "d/toto" ));
		list.add( new File( "c/arbitrary" ));

		Assert.assertEquals( "toto", list.get( 0 ).getName());
		Assert.assertEquals( "zorro", list.get( 1 ).getName());
		Assert.assertEquals( "toto", list.get( 2 ).getName());
		Assert.assertEquals( "arbitrary", list.get( 3 ).getName());

		Collections.sort( list, new FileNameComparator());

		Assert.assertEquals( "arbitrary", list.get( 0 ).getName());
		Assert.assertEquals( "toto", list.get( 1 ).getName());
		Assert.assertEquals( "toto", list.get( 2 ).getName());
		Assert.assertEquals( "zorro", list.get( 3 ).getName());
	}


	@Test( expected = IllegalArgumentException.class )
	public void testStoreDirectoryResourcesAsBytes_illegalArgument_1() throws Exception {
		Utils.storeDirectoryResourcesAsBytes( new File( "not/existing/file" ));
	}


	@Test( expected = IllegalArgumentException.class )
	public void testStoreDirectoryResourcesAsBytes_illegalArgument_2() throws Exception {
		Utils.storeDirectoryResourcesAsBytes( this.folder.newFile( "roboconf.txt" ));
	}


	@Test
	public void testStoreDirectoryResourcesAsBytes_withExclusionPatterns() throws Exception {

		File dir = this.folder.newFolder();
		Assert.assertTrue( new File( dir, "dir1/dir2" ).mkdirs());
		Assert.assertTrue( new File( dir, "dir1/dir2/t1.txt" ).createNewFile());
		Assert.assertTrue( new File( dir, "dir1/dir2/t2.toto" ).createNewFile());
		Assert.assertTrue( new File( dir, "t3.txt" ).createNewFile());

		Map<String,byte[]> map = Utils.storeDirectoryResourcesAsBytes( dir );
		Assert.assertEquals( 3, map.size());
		Assert.assertTrue( map.containsKey( "dir1/dir2/t1.txt" ));
		Assert.assertTrue( map.containsKey( "dir1/dir2/t2.toto" ));
		Assert.assertTrue( map.containsKey( "t3.txt" ));

		map = Utils.storeDirectoryResourcesAsBytes( dir, Arrays.asList( ".*\\.txt" ));
		Assert.assertEquals( 1, map.size());
		Assert.assertTrue( map.containsKey( "dir1/dir2/t2.toto" ));

		map = Utils.storeDirectoryResourcesAsBytes( dir, Arrays.asList( ".*toto.*" ));
		Assert.assertEquals( 2, map.size());
		Assert.assertTrue( map.containsKey( "dir1/dir2/t1.txt" ));
		Assert.assertTrue( map.containsKey( "t3.txt" ));

		map = Utils.storeDirectoryResourcesAsBytes( dir, Arrays.asList( "dir1" ));
		Assert.assertEquals( 3, map.size());
		Assert.assertTrue( map.containsKey( "dir1/dir2/t1.txt" ));
		Assert.assertTrue( map.containsKey( "dir1/dir2/t2.toto" ));
		Assert.assertTrue( map.containsKey( "t3.txt" ));
	}


	@Test
	public void testIsAncestorFile() throws Exception {

		File parent = new File( "home/toto/whatever" );
		Assert.assertTrue( Utils.isAncestorFile( parent, parent ));

		File comp = new File( "home/toto/whatever/" );
		Assert.assertTrue( Utils.isAncestorFile( parent, comp ));

		comp = new File( "home/toto/./whatever/" );
		Assert.assertTrue( Utils.isAncestorFile( parent, comp ));

		comp = new File( "home/toto/../toto/whatever/" );
		Assert.assertTrue( Utils.isAncestorFile( parent, comp ));

		comp = new File( "home/toto/whatever/some-file.txt" );
		Assert.assertTrue( Utils.isAncestorFile( parent, comp ));

		comp = new File( "home/toto/whatever/some/dir/some-file.txt" );
		Assert.assertTrue( Utils.isAncestorFile( parent, comp ));

		comp = new File( "home/toto/" );
		Assert.assertFalse( Utils.isAncestorFile( parent, comp ));

		comp = new File( "home/toto/whateve" );
		Assert.assertFalse( Utils.isAncestorFile( parent, comp ));

		comp = new File( "home/toto/whatevereeeeeee" );
		Assert.assertFalse( Utils.isAncestorFile( parent, comp ));
	}


	@Test
	public void testCopyDirectory_existingTarget() throws Exception {

		// Create a source
		File source = this.folder.newFolder();
		File dir1 = new File( source, "lol/whatever/sub" );
		Assert.assertTrue( dir1.mkdirs());
		File dir2 = new File( source, "sub" );
		Assert.assertTrue( dir2.mkdirs());

		Utils.copyStream( new ByteArrayInputStream( ",kklmsdff sdfl sdfkkl".getBytes( "UTF-8" )), new File( dir1, "f1" ));
		Utils.copyStream( new ByteArrayInputStream( "".getBytes( "UTF-8" )), new File( dir1, "f2" ));
		Utils.copyStream( new ByteArrayInputStream( "sd".getBytes( "UTF-8" )), new File( dir1, "f3" ));

		Utils.copyStream( new ByteArrayInputStream( "sd\ndsfg".getBytes( "UTF-8" )), new File( source, "f" ));

		Utils.copyStream( new ByteArrayInputStream( "sd\ndsfg".getBytes( "UTF-8" )), new File( dir2, "f1" ));
		Utils.copyStream( new ByteArrayInputStream( "sdf df fg".getBytes( "UTF-8" )), new File( dir2, "f45678" ));

		// Copy
		File target = this.folder.newFolder();
		Assert.assertEquals( 0, Utils.listAllFiles( target ).size());
		Utils.copyDirectory( source, target );
		Assert.assertEquals( 6, Utils.listAllFiles( target ).size());
	}


	@Test
	public void testCopyDirectory_inexistingTarget() throws Exception {

		// Create a source
		File source = this.folder.newFolder();
		File dir1 = new File( source, "lol/whatever/sub/many/more/" );
		Assert.assertTrue( dir1.mkdirs());
		File dir2 = new File( source, "sub" );
		Assert.assertTrue( dir2.mkdirs());

		Utils.copyStream( new ByteArrayInputStream( ",kklmsdff sdfl sdfkkl".getBytes( "UTF-8" )), new File( dir1, "f1" ));
		Utils.copyStream( new ByteArrayInputStream( "".getBytes( "UTF-8" )), new File( dir1, "f2" ));
		Utils.copyStream( new ByteArrayInputStream( "sd".getBytes( "UTF-8" )), new File( dir1, "f3" ));

		Utils.copyStream( new ByteArrayInputStream( "sd\ndsfg".getBytes( "UTF-8" )), new File( source, "f" ));

		Utils.copyStream( new ByteArrayInputStream( "sd\ndsfg".getBytes( "UTF-8" )), new File( dir2, "f1" ));
		Utils.copyStream( new ByteArrayInputStream( "".getBytes( "UTF-8" )), new File( dir2, "f4" ));
		Utils.copyStream( new ByteArrayInputStream( "sdf df fg".getBytes( "UTF-8" )), new File( dir2, "f45678" ));

		// Copy
		File target = new File( this.folder.newFolder(), "some" );
		Assert.assertFalse( target.exists());
		Utils.copyDirectory( source, target );
		Assert.assertTrue( target.exists());
		Assert.assertEquals( 7, Utils.listAllFiles( target ).size());
	}


	@Test
	public void testLogException() {

		final StringHandler logHandler = new StringHandler();

		// Prepare a logger
		Logger logger = Logger.getLogger( getClass().getName());
		LogManager.getLogManager().addLogger( logger );
		logger.setLevel( Level.FINEST );
		logger.addHandler( logHandler );

		// Run the first test
		Assert.assertEquals( "", logHandler.getLogs());
		Utils.logException( logger, new Exception( "boo!" ));
		Assert.assertTrue( logHandler.getLogs().startsWith( "java.lang.Exception: boo!" ));

		// Change the log level
		logger.setLevel( Level.INFO );
		Assert.assertFalse( logger.isLoggable( Level.FINEST ));
		Assert.assertTrue( logger.isLoggable( Level.INFO ));

		logHandler.getStringBuilder().setLength( 0 );
		Assert.assertEquals( "", logHandler.getLogs());
		Utils.logException( logger, new Exception( "boo!" ));
		Assert.assertEquals( "", logHandler.getLogs());

		Utils.logException( logger, Level.INFO, new Exception( "boo!" ));
		Assert.assertTrue( logHandler.getLogs().startsWith( "java.lang.Exception: boo!" ));

		// With an additional message
		logHandler.getStringBuilder().setLength( 0 );
		Assert.assertEquals( "", logHandler.getLogs());
		Utils.logException( logger, Level.INFO, new Exception( "nice try!" ), "again" );
		Assert.assertTrue( logHandler.getLogs().startsWith( "again\njava.lang.Exception: nice try!" ));
	}


	@Test
	public void testFindUrlAndPort() throws Exception {

		Map.Entry<String,Integer> entry = Utils.findUrlAndPort( "http://localhost" );
		Assert.assertEquals( "http://localhost", entry.getKey());
		Assert.assertEquals( -1, entry.getValue().intValue());

		entry = Utils.findUrlAndPort( "http://localhost:9989" );
		Assert.assertEquals( "http://localhost", entry.getKey());
		Assert.assertEquals( 9989, entry.getValue().intValue());

		entry = Utils.findUrlAndPort( "http://roboconf.net/some/arbitrary/path" );
		Assert.assertEquals( "http://roboconf.net/some/arbitrary/path", entry.getKey());
		Assert.assertEquals( -1, entry.getValue().intValue());

		entry = Utils.findUrlAndPort( "http://roboconf.net:2727/some/arbitrary/path" );
		Assert.assertEquals( "http://roboconf.net/some/arbitrary/path", entry.getKey());
		Assert.assertEquals( 2727, entry.getValue().intValue());

		File f = new File( System.getProperty( "java.io.tmpdir" ));
		entry = Utils.findUrlAndPort( f.toURI().toString());
		Assert.assertEquals( f.toURI(), new URI( entry.getKey()));
		Assert.assertEquals( -1, entry.getValue().intValue());

		entry = Utils.findUrlAndPort( "ftp://some.host.com:4811/path" );
		Assert.assertEquals( "ftp://some.host.com/path", entry.getKey());
		Assert.assertEquals( 4811, entry.getValue().intValue());
	}


	@Test
	public void testCapitalize() {

		Assert.assertEquals( "", Utils.capitalize( "" ));
		Assert.assertNull( Utils.capitalize( null ));

		Assert.assertEquals( "Toto", Utils.capitalize( "Toto" ));
		Assert.assertEquals( "Toto", Utils.capitalize( "tOTo" ));
		Assert.assertEquals( "Toto oops", Utils.capitalize( "tOTo oops" ));
	}


	@Test
	public void testDirectoryFileFilter() throws Exception {

		DirectoryFileFilter filter = new DirectoryFileFilter();
		Assert.assertTrue( filter.accept( this.folder.newFolder()));
		Assert.assertFalse( filter.accept( this.folder.newFile()));
		Assert.assertFalse( filter.accept( new File( "inexisting" )));
	}


	@Test
	public void testListDirectories() throws Exception {

		File root = new File( "inexisting" );
		Assert.assertEquals( 0, Utils.listDirectories( root ).size());

		root = this.folder.newFolder();
		Assert.assertEquals( 0, Utils.listDirectories( root ).size());

		Assert.assertTrue( new File( root, "toto.txt" ).createNewFile());
		Assert.assertTrue( new File( root, "dir" ).mkdir());
		Assert.assertTrue( new File( root, "dir/dir1" ).mkdir());

		List<File> directories = Utils.listDirectories( root );
		Assert.assertEquals( 1, directories.size());
		Assert.assertEquals( "dir", directories.get( 0 ).getName());
	}


	@Test
	public void testReadPropertiesFileQuietly() throws Exception {

		File f = this.folder.newFile();
		Utils.writeStringInto( "prop: op", f );
		Logger logger = Logger.getLogger( getClass().getName());

		// Normal
		Properties props = Utils.readPropertiesFileQuietly( f, logger );
		Assert.assertEquals( 1, props.size());
		Assert.assertEquals( "op", props.get( "prop" ));

		// Inexisting file
		props = Utils.readPropertiesFileQuietly( new File( "inexisting" ), logger );
		Assert.assertEquals( 0, props.size());

		// Null file
		props = Utils.readPropertiesFileQuietly( null, logger );
		Assert.assertEquals( 0, props.size());
	}


	@Test
	public void testReadProperties() throws Exception {

		// Normal
		Logger logger = Logger.getLogger( getClass().getName());
		String s = "\np1: value1 \\ok\np2 = value2\n\n";
		Properties props = Utils.readPropertiesQuietly( s, logger );

		Assert.assertEquals( 2, props.size());
		Assert.assertEquals( "value1 ok", props.get( "p1" ));
		Assert.assertEquals( "value2", props.get( "p2" ));

		// Null string
		props = Utils.readPropertiesQuietly( null, logger );
		Assert.assertEquals( 0, props.size());

		// Check exceptions
		s = "";
		props = Utils.readPropertiesQuietly( s, logger );
		Assert.assertEquals( 0, props.size());
	}


	@Test
	public void testGetValue() throws Exception {
		Map<String,String> map = new HashMap<>();
		for(int i=0;i<10;i++) {
			map.put(""+i, "toto"+i);
		}
		Assert.assertEquals(10, map.size());

		for(int i=0;i<10;i++) {
			Assert.assertEquals("toto"+i, Utils.getValue(map, ""+i, "tata"));
		}

		Assert.assertEquals("tata", Utils.getValue(map, "coucou", "tata"));
	}


	@Test
	public void testRemoveFileExtension() {

		Assert.assertEquals( "", Utils.removeFileExtension( "" ));
		Assert.assertEquals( "test", Utils.removeFileExtension( "test" ));
		Assert.assertEquals( "test", Utils.removeFileExtension( "test.txt" ));
		Assert.assertEquals( "test", Utils.removeFileExtension( "test.jpg" ));
		Assert.assertEquals( "test.jpg", Utils.removeFileExtension( "test.jpg.txt" ));
	}


	@Test
	public void testListAllFiles_withFileExtension() throws Exception {

		File dir = this.folder.newFolder();
		Assert.assertTrue( new File( dir, "f1.txt" ).createNewFile());
		Assert.assertTrue( new File( dir, "f2.jpg" ).createNewFile());
		Assert.assertTrue( new File( dir, "f3.txt" ).createNewFile());
		Assert.assertTrue( new File( dir, "f4.JPG" ).createNewFile());
		Assert.assertTrue( new File( dir, "f5.zip" ).createNewFile());

		Assert.assertEquals( 2, Utils.listAllFiles( dir, "jpg" ).size());
		Assert.assertEquals( 2, Utils.listAllFiles( dir, ".jpg" ).size());
		Assert.assertEquals( 2, Utils.listAllFiles( dir, "jPg" ).size());

		Assert.assertEquals( 2, Utils.listAllFiles( dir, "txt" ).size());
		Assert.assertEquals( 1, Utils.listAllFiles( dir, ".zip" ).size());
		Assert.assertEquals( 5, Utils.listAllFiles( dir, null ).size());
	}


	@Test
	public void testListDirectFiles_withFileExtension() throws Exception {

		File dir = this.folder.newFolder();
		Assert.assertTrue( new File( dir, "sub" ).mkdir());

		Assert.assertTrue( new File( dir, "f1.txt" ).createNewFile());
		Assert.assertTrue( new File( dir, "sub/f2.jpg" ).createNewFile());
		Assert.assertTrue( new File( dir, "f3.txt" ).createNewFile());
		Assert.assertTrue( new File( dir, "f4.JPG" ).createNewFile());
		Assert.assertTrue( new File( dir, "sub/f5.zip" ).createNewFile());

		Assert.assertEquals( 1, Utils.listDirectFiles( dir, "jpg" ).size());
		Assert.assertEquals( 1, Utils.listDirectFiles( dir, ".jpg" ).size());
		Assert.assertEquals( 1, Utils.listDirectFiles( dir, "jPg" ).size());

		Assert.assertEquals( 2, Utils.listDirectFiles( dir, "txt" ).size());
		Assert.assertEquals( 0, Utils.listDirectFiles( dir, ".zip" ).size());
		Assert.assertEquals( 3, Utils.listDirectFiles( dir, null ).size());

		Assert.assertEquals( 0, Utils.listDirectFiles( this.folder.newFile(), null ).size());
	}


	@Test
	public void testIsAncestor() {

		File dir = new File( "somewhere/over/the/rainbow" );
		Assert.assertTrue( Utils.isAncestor( dir, new File( dir, "test" )));
		Assert.assertTrue( Utils.isAncestor( dir, new File( dir, "test.txt" )));
		Assert.assertTrue( Utils.isAncestor( dir, new File( dir, "test/test/te" )));

		Assert.assertFalse( Utils.isAncestor( dir, dir ));
		Assert.assertFalse( Utils.isAncestor( dir, new File( "somewhere/else" )));
		Assert.assertFalse( Utils.isAncestor( dir, new File( "somewhere/over/the/rainbows" )));
		Assert.assertFalse( Utils.isAncestor( dir, new File( "somewhere/over/the/rainbo" )));
	}


	@Test
	public void testReadUrlContent() throws Exception {

		File f = this.folder.newFile();
		Utils.writeStringInto( "kikou", f );
		String readContent = Utils.readUrlContent( f.toURI().toURL().toString());
		Assert.assertEquals( "kikou", readContent );

		Utils.deleteFilesRecursively( f );
		Assert.assertFalse( f.exists());

		Logger logger = Logger.getLogger( getClass().getName());
		readContent = Utils.readUrlContentQuietly( f.toURI().toURL().toString(), logger );
		Assert.assertEquals( "", readContent );
	}


	@Test
	public void testCleanNameWithAccents() {

		Assert.assertEquals( "", Utils.cleanNameWithAccents( "" ));
		Assert.assertEquals( "a bc d", Utils.cleanNameWithAccents( "a bc d" ));
		Assert.assertEquals( "Ebe a Vuc", Utils.cleanNameWithAccents( "Ébé à Vuç" ));
	}


	@Test
	public void testUpdateProperties() throws Exception {

		File before = TestUtils.findTestFile( "/properties/before.properties" );
		String beforeTxt = Utils.readFileContent( before );
		File after = TestUtils.findTestFile( "/properties/after.properties" );
		String afterTxt = Utils.readFileContent( after );

		// One way
		Map<String,String> keyToNewValue = new HashMap<> ();
		keyToNewValue.put( "application-name", "app" );
		keyToNewValue.put( "scoped-instance-path", "/vm" );
		keyToNewValue.put( "parameters", "file:/something" );
		keyToNewValue.put( "messaging-type", "http" );

		String out = Utils.updateProperties( beforeTxt, keyToNewValue );
		Assert.assertEquals( afterTxt, out );

		// Other way
		keyToNewValue.clear();
		keyToNewValue.put( "application-name", "" );
		keyToNewValue.put( "scoped-instance-path", "" );
		keyToNewValue.put( "paraMeters", "" );
		keyToNewValue.put( "messaging-type", "idle" );

		out = Utils.updateProperties( afterTxt, keyToNewValue );
		Assert.assertEquals( beforeTxt, out );
	}


	@Test
	public void testCloseConnection() throws Exception {

		Logger logger = Logger.getLogger( UtilsTest.class.getName());
		Utils.closeConnection( null, logger );
		// No exception

		Connection conn = Mockito.mock( Connection.class );
		Mockito.doThrow( new SQLException( "for test" )).when( conn ).close();
		Utils.closeConnection( conn, logger );
		// No exception
	}


	@Test
	public void testClosePreparedStatement() throws Exception {

		Logger logger = Logger.getLogger( UtilsTest.class.getName());
		Utils.closeStatement((PreparedStatement) null, logger );
		// No exception

		PreparedStatement ps = Mockito.mock( PreparedStatement.class );
		Mockito.doThrow( new SQLException( "for test" )).when( ps ).close();
		Utils.closeStatement( ps, logger );
		// No exception
	}


	@Test
	public void testCloseStatement() throws Exception {

		Logger logger = Logger.getLogger( UtilsTest.class.getName());
		Utils.closeStatement((Statement) null, logger );
		// No exception

		Statement st = Mockito.mock( Statement.class );
		Mockito.doThrow( new SQLException( "for test" )).when( st ).close();
		Utils.closeStatement( st, logger );
		// No exception
	}


	@Test
	public void testResultSet() throws Exception {

		Logger logger = Logger.getLogger( UtilsTest.class.getName());
		Utils.closeResultSet( null, logger );
		// No exception

		ResultSet resultSet = Mockito.mock( ResultSet.class );
		Mockito.doThrow( new SQLException( "for test" )).when( resultSet ).close();
		Utils.closeResultSet( resultSet, logger );
		// No exception
	}
}
