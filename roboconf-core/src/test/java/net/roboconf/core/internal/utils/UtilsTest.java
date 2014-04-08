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

package net.roboconf.core.internal.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestUtils;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class UtilsTest {

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
		Assert.assertEquals( 2, result.size());
		Assert.assertEquals( "once", result.get( 0 ));
		Assert.assertEquals( ", upon, a , time", result.get( 1 ));

		result = Utils.splitNicely( "once $ $a$ $$ time", "$" );
		Assert.assertEquals( 3, result.size());
		Assert.assertEquals( "once", result.get( 0 ));
		Assert.assertEquals( "a", result.get( 1 ));
		Assert.assertEquals( "time", result.get( 2 ));
	}


	@Test
	public void testAreEqual() {

		Assert.assertTrue( Utils.areEqual( null, null ));
		Assert.assertFalse( Utils.areEqual( null, new Object()));
		Assert.assertFalse( Utils.areEqual( new Object(), null ));
		Assert.assertFalse( Utils.areEqual( new Object(), new Object()));

		Object o = new Object();
		Assert.assertTrue( Utils.areEqual( o, o ));
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
	public void testExtractZipArchive() {

		File zipFile = new File( System.getProperty( "java.io.tmpdir" ), UUID.randomUUID().toString() + ".zip" );
		zipFile.deleteOnExit();

		try {
			Map<String,String> entryToContent = TestUtils.buildZipContent();
			TestUtils.createZipFile( entryToContent, zipFile );
			TestUtils.compareZipContent( zipFile, entryToContent );

		} catch( IOException e ) {
			Assert.fail( e.getMessage());
		}
	}


	@Test
	public void testCloseQuietly() {

		try {
			InputStream in = null;
			Utils.closeQuietly( in );
		} catch( Exception e ) {
			Assert.fail();
		}

		try {
			InputStream in = new ByteArrayInputStream( new byte[ 0 ]);
			Utils.closeQuietly( in );
		} catch( Exception e ) {
			Assert.fail();
		}

		try {
			OutputStream out = new ByteArrayOutputStream();
			Utils.closeQuietly( out );
		} catch( Exception e ) {
			Assert.fail();
		}

		try {
			OutputStream out = null;
			Utils.closeQuietly( out );
		} catch( Exception e ) {
			Assert.fail();
		}
	}


	@Test
	public void testWriteException() {

		String msg = "Hello from Roboconf.";
		String stackTrace = Utils.writeException( new Exception( msg ));
		Assert.assertTrue( stackTrace.contains( msg ));
	}
}
