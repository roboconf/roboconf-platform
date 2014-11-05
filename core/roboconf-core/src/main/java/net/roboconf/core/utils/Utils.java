/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of their joint LINAGORA -
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Various utilities.
 * @author Vincent Zurczak - Linagora
 */
public final class Utils {

	/**
	 * Private empty constructor.
	 */
	private Utils() {
		// nothing
	}


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
	 * Splits a string and formats the result.
	 * @param toSplit the string to split (can be null)
	 * @param separator the separator (cannot be null or the empty string)
	 * @return a list of items (never null)
	 */
	public static List<String> splitNicely( String toSplit, String separator ) {

		if( separator == null || separator.isEmpty())
			throw new IllegalArgumentException( "The separator cannot be null or the empty string." );

		List<String> result = new ArrayList<String> ();
		if( ! Utils.isEmptyOrWhitespaces( toSplit )) {
			for( String s : toSplit.split( Pattern.quote( separator )))
				result.add( s .trim());
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
	 * Closes a reader quietly.
	 * @param reader a reader (can be null)
	 */
	public static void closeQuietly( Reader reader ) {
		if( reader != null ) {
			try {
				reader.close();
			} catch( IOException e ) {
				// nothing
			}
		}
	}


	/**
	 * Closes a writer quietly.
	 * @param writer a writer (can be null)
	 */
	public static void closeQuietly( Writer writer ) {
		if( writer != null ) {
			try {
				writer.close();
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
	 * Finds all the files (direct and indirect) from a directory.
	 * <p>
	 * This method skips hidden files and files whose name starts
	 * with a dot.
	 * </p>
	 *
	 * @param directory an existing directory
	 * @return a non-null list of files
	 */
	public static List<File> listAllFiles( File directory ) {

		if( ! directory.exists()
				|| ! directory.isDirectory())
			throw new IllegalArgumentException( directory.getAbsolutePath() + " does not exist or is not a directory." );

		List<File> result = new ArrayList<File> ();
		List<File> directoriesToInspect = new ArrayList<File> ();
		directoriesToInspect.add( directory );

		while( ! directoriesToInspect.isEmpty()) {
			File[] subFiles = directoriesToInspect.remove( 0 ).listFiles();
			if( subFiles == null )
				continue;

			for( File subFile : subFiles ) {
				if(  subFile.isHidden()
						|| subFile.getName().startsWith( "." ))
					continue;

				if( subFile.isFile())
					result.add( subFile );
				else
					directoriesToInspect.add( subFile );
			}
		}

		return result;
	}


	/**
	 * Stores the resources from a directory into a map.
	 * @param directory an existing directory
	 * @return a non-null map (key = the file location, relative to the directory, value = file content)
	 * @throws IOException if something went wrong while reading a file
	 */
	public static Map<String,byte[]> storeDirectoryResourcesAsBytes( File directory ) throws IOException {

		if( ! directory.exists())
			throw new IllegalArgumentException( "The resource directory was not found. " + directory.getAbsolutePath());

		if( ! directory.isDirectory())
			throw new IllegalArgumentException( "The resource directory is not a valid directory. " + directory.getAbsolutePath());

		Map<String,byte[]> result = new HashMap<String,byte[]> ();
		List<File> resourceFiles = listAllFiles( directory );
		for( File file : resourceFiles ) {

			String key = computeFileRelativeLocation( directory, file );
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			Utils.copyStream( file, os );
			result.put( key, os.toByteArray());
		}

		return result;
	}


	/**
	 * Stores the resources from a directory into a map.
	 * @param directory an existing directory
	 * @return a non-null map (key = the file location, relative to the directory, value = file content)
	 * @throws IOException if something went wrong while reading a file
	 */
	public static Map<String,String> storeDirectoryResourcesAsString( File directory ) throws IOException {

		Map<String,byte[]> map = storeDirectoryResourcesAsBytes( directory );
		Map<String,String> result = new HashMap<String,String>( map.size());
		for( Map.Entry<String,byte[]> entry : map.entrySet())
			result.put( entry.getKey(), new String( entry.getValue(), "UTF-8" ));

		return result;
	}


	/**
	 * Computes the relative location of a file with respect to a root directory.
	 * @param rootDirectory a directory
	 * @param subFile a file contained (directly or indirectly) in the directory
	 * @return a non-null string
	 */
	public static String computeFileRelativeLocation( File rootDirectory, File subFile ) {

		String rootPath = rootDirectory.getAbsolutePath();
		String subPath = subFile.getAbsolutePath();
		if(  ! subPath.startsWith( rootPath ))
			throw new IllegalArgumentException( "The sub-file must be contained in the directory." );

		if(  rootDirectory.equals( subFile ))
			throw new IllegalArgumentException( "The sub-file must be different than the directory." );

		return subPath.substring( rootPath.length() + 1 ).replace( '\\', '/' );
	}


	/**
	 * Extracts a ZIP archive in a directory.
	 * @param zipFile a ZIP file (not null, must exist)
	 * @param targetDirectory the target directory (may not exist but must be a directory)
	 * @throws IOException if something went wrong
	 */
	public static void extractZipArchive( File zipFile, File targetDirectory )
	throws IOException {

		// Make some checks
		if( zipFile == null || targetDirectory == null )
			throw new IllegalArgumentException( "The ZIP file and the target directory cannot be null." );

		if( ! zipFile.exists()
				|| ! zipFile.isFile())
			throw new IllegalArgumentException( "ZIP file " + targetDirectory.getName() + " does not exist." );

		if( targetDirectory.exists()
				&& ! targetDirectory.isDirectory())
			throw new IllegalArgumentException( "Target directory " + targetDirectory.getName() + " is not a directory." );

		if( ! targetDirectory.exists()
				&& ! targetDirectory.mkdirs())
			throw new IOException( "Target directory " + targetDirectory.getName() + " could not be created." );

		// Load the ZIP file
		ZipFile theZipFile = new ZipFile( zipFile );
		Enumeration<? extends ZipEntry> entries = theZipFile.entries();

		// And start the copy
		try {
			while( entries.hasMoreElements()) {
				FileOutputStream os = null;
				try {
					ZipEntry entry = entries.nextElement();
					File f = new File( targetDirectory, entry.getName());

					// Case 'directory': create it.
					// Case 'file': create its parents and copy the content.
					if( entry.isDirectory()) {
						if( ! f.exists() && ! f.mkdirs())
							throw new IOException( "Failed to create directory for entry: " + entry.getName());

					} else if( ! f.getParentFile().exists() && ! f.getParentFile().mkdirs()) {
						throw new IOException( "Failed to create parent directory for entry: " + entry.getName());

					} else {
						os = new FileOutputStream( f );
						copyStream( theZipFile.getInputStream( entry ), os );
					}

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

		List<File> filesToDelete = new ArrayList<File> ();
		filesToDelete.addAll( Arrays.asList( files ));
		while( ! filesToDelete.isEmpty()) {
			File currentFile = filesToDelete.remove( 0 );
			if( currentFile == null
					|| ! currentFile.exists())
				continue;

			// Non-empty directory: add sub-files and reinsert the current directory after
			File[] subFiles = currentFile.listFiles();
			if( subFiles != null && subFiles.length > 0 ) {
				filesToDelete.add( 0, currentFile );
				filesToDelete.addAll( 0, Arrays.asList( subFiles ));
			}

			// Existing file or empty directory => delete it
			else if( ! currentFile.delete())
				throw new IOException( currentFile.getAbsolutePath() + " could not be deleted." );
		}
	}


	/**
	 * Writes an exception's stack trace into a string.
	 * @param e an exception (not null)
	 * @return a string
	 */
	public static String writeException( Exception e ) {

		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter( sw ));

		return sw.toString();
	}


	/**
	 * Determines whether a file is a parent of another file.
	 * <p>
	 * This method handles intermediate '.' and '..' segments.
	 * </p>
	 *
	 * @param potentialAncestor a file that may contain the other one
	 * @param file a file
	 * @return true if the path of 'file' starts with the path of 'potentialAncestor', false otherwise
	 * @throws IOException if the file location cannot be made canonical
	 */
	public static boolean isAncestorFile( File potentialAncestor, File file ) throws IOException {

		String ancestorPath = potentialAncestor.getCanonicalPath();
		String path = file.getCanonicalPath();

		boolean result = false;
		if( path.startsWith( ancestorPath )) {
			String s = path.substring( ancestorPath.length());
			result = s.isEmpty() || s.startsWith( System.getProperty( "file.separator" ));
		}

		return result;
	}


	/**
	 * Copies a directory.
	 * <p>
	 * This method copies the content of the source directory
	 * into the a target directory. This latter is created if necessary.
	 * </p>
	 *
	 * @param source the directory to copy
	 * @param target the target directory
	 * @throws IOException if a problem occurred during the copy
	 */
	public static void copyDirectory( File source, File target ) throws IOException {

		if( ! target.exists()
				&& ! target.mkdirs())
			throw new IOException( "The directory " + target + " could not be created." );

		for( File sourceFile : listAllFiles( source )) {
			String path = computeFileRelativeLocation( source, sourceFile );
			File targetFile = new File( target, path );

			if( ! targetFile.getParentFile().exists()
					&& ! targetFile.getParentFile().mkdirs())
				throw new IOException( "The directory " + targetFile.getParentFile() + " could not be created." );

			copyStream( sourceFile, targetFile );
		}
	}
}
