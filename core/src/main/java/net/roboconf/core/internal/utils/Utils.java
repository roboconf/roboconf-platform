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

package net.roboconf.core.internal.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Various utilities.
 * @author Vincent Zurczak - Linagora
 */
public class Utils {

	/**
	 * @param s a string (can be null)
	 * @return true if the string is null or only made up of white spaces
	 */
	public static boolean isEmptyOrWhitespaces( String s ) {
		return s == null || s.trim().length() == 0;
	}


	/**
	 * @param o1 an object
	 * @param o2 another object
	 * @return true if both objects are null or if they are equal
	 */
	public static boolean areEqual( Object o1, Object o2 ) {
		return o1 == null ? o2 == null : o1.equals( o2 );
	}


	/**
	 * Splits a string, ignores invalid sequences and formats the result.
	 * @param toSplit the string to split (can be null)
	 * @param separator the separator (cannot be null or the empty string)
	 * @return a list of items (never null)
	 */
	public static Collection<String> splitNicely( String toSplit, String separator ) {

		if( separator == null || separator.isEmpty())
			throw new IllegalArgumentException( "The separator cannot be null or the empty string." );

		Collection<String> result = new ArrayList<String> ();
		if( toSplit != null ) {
			for( String s : toSplit.split( separator )) {
				if( ! Utils.isEmptyOrWhitespaces( s ))
					result.add( s .trim());
			}
		}

		return result;
	}


	/**
	 * Closes a stream quietly.
	 * @param in an input stream (can be null)
	 */
	public static void closeQuietly( InputStream in ) {
		if( in != null ) {
			try {
				in.close();
			} catch( IOException e ) {
				// nothing
			}
		}
	}


	/**
	 * Closes a stream quietly.
	 * @param out an output stream (can be null)
	 */
	public static void closeQuietly( OutputStream out ) {
		if( out != null ) {
			try {
				out.close();
			} catch( IOException e ) {
				// nothing
			}
		}
	}


	/**
	 * Copies the content from in into os.
	 * <p>
	 * Neither <i>in</i> nor <i>os</i> are closed by this method.<br />
	 * They must be explicitly closed after this method is called.
	 * </p>
	 *
	 * @param in an input stream (not null)
	 * @param os an output stream (not null)
	 * @throws IOException if an error occurred
	 */
	public static void copyStream( InputStream in, OutputStream os ) throws IOException {

		byte[] buf = new byte[ 1024 ];
		int len;
		while((len = in.read( buf )) > 0) {
			os.write( buf, 0, len );
		}
	}


	/**
	 * Copies the content from in into outputFile.
	 * <p>
	 * <i>in</i> is not closed by this method.<br />
	 * It must be explicitly closed after this method is called.
	 * </p>
	 *
	 * @param in an input stream (not null)
	 * @param outputFile will be created if it does not exist
	 * @throws IOException if the file could not be created
	 */
	public static void copyStream( InputStream in, File outputFile ) throws IOException {

		if( ! outputFile.exists() && ! outputFile.createNewFile())
			throw new IOException( "Failed to create " + outputFile.getAbsolutePath() + "." );

		OutputStream os = new FileOutputStream( outputFile );
		copyStream( in, os );
		os.close ();
	}


	/**
	 * Copies the content from inputFile into outputFile.
	 *
	 * @param inputFile an input file (must be a file and exist)
	 * @param outputFile will be created if it does not exist
	 * @throws IOException if something went wrong
	 */
	public static void copyStream( File inputFile, File outputFile ) throws IOException {
		InputStream is = new FileInputStream( inputFile );
		copyStream( is, outputFile );
		is.close();
	}


	/**
	 * Copies the content from inputFile into an output stream.
	 *
	 * @param inputFile an input file (must be a file and exist)
	 * @param os the output stream
	 * @throws IOException if something went wrong
	 */
	public static void copyStream( File inputFile, OutputStream os ) throws IOException {
		InputStream is = new FileInputStream( inputFile );
		copyStream( is, os );
		is.close();
	}


	/**
	 * Extracts a ZIP archive in a directory.
	 * @param zipFile a ZIP file (not null, must exist)
	 * @param targetDirectory the target directory (must exist and be a directory)
	 * @throws ZipException if something went wrong
	 * @throws IOException if something went wrong
	 */
	public static void extractZipArchive( File zipFile, File targetDirectory )
    throws ZipException, IOException {

		// Make some checks
		if( zipFile == null || targetDirectory == null )
			throw new IllegalArgumentException( "The ZIP file and the target directory cannot be null." );

		if( ! zipFile.exists())
			throw new IllegalArgumentException( "ZIP file " + targetDirectory.getName() + " does not exist." );

		if( ! targetDirectory.exists())
			throw new IllegalArgumentException( "Target directory " + targetDirectory.getName() + " does not exist." );

		if( ! targetDirectory.isDirectory())
			throw new IllegalArgumentException( "Target directory " + targetDirectory.getName() + " is not a directory." );

		// Load the ZIP file
		ZipFile theZipFile = new ZipFile( zipFile );
		Enumeration<? extends ZipEntry> entries = theZipFile.entries();

		// And start the copy
		try {
			while( entries.hasMoreElements()) {
				FileOutputStream os = null;
				try {
					ZipEntry entry = entries.nextElement();
					os = new FileOutputStream( new File( targetDirectory, entry.getName()));
					copyStream( theZipFile.getInputStream( entry ), os );

				} finally {
					closeQuietly( os );
				}
			}

		} finally {
			// Close the stream
			theZipFile.close();
		}
	}


	/**
	 * Deletes files recursively.
	 * @param files the files to delete
	 * @throws IOException if a file could not be deleted
	 */
	public static void deleteFilesRecursively( File... files ) throws IOException {

		if( files == null )
			return;

		for( File file : files ) {
			if( file.exists()) {
				if( file.isDirectory())
					deleteFilesRecursively( file.listFiles());

				if( ! file.delete())
					throw new IOException( file.getAbsolutePath() + " could not be deleted." );
			}
		}
	}
}
