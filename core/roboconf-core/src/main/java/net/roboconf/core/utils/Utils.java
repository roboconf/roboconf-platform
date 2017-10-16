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

package net.roboconf.core.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Normalizer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Various utilities.
 * @author Vincent Zurczak - Linagora
 * @author Amadou Diarra - UJF
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
	 * Capitalizes a string.
	 * @param s a string
	 * @return the capitalized string
	 */
	public static String capitalize( String s ) {

		String result = s;
		if( ! Utils.isEmptyOrWhitespaces( s ))
			result = Character.toUpperCase( s.charAt( 0 )) + s.substring( 1 ).toLowerCase();

		return result;
	}


	/**
	 * Removes the extension from a file name.
	 * @param filename a non-null file name
	 * @return a non-null string
	 */
	public static String removeFileExtension( String filename ) {

		String result = filename;
		int index = filename.lastIndexOf( '.' );
		if( index != -1 )
			result= filename.substring( 0, index );

		return result;
	}


	/**
	 * Splits a string and formats the result.
	 * @param toSplit the string to split (can be null)
	 * @param separator the separator (cannot be null or the empty string)
	 * @return a list of items (never null), with every item being trimmed
	 */
	public static List<String> splitNicely( String toSplit, String separator ) {

		if( separator == null || separator.isEmpty())
			throw new IllegalArgumentException( "The separator cannot be null or the empty string." );

		return splitNicelyWithPattern( toSplit, Pattern.quote( separator ));
	}


	/**
	 * Splits a string and formats the result.
	 * @param toSplit the string to split (can be null)
	 * @param patternSeparator the separator pattern (cannot be null or the empty string)
	 * @return a list of items (never null), with every item being trimmed
	 */
	public static List<String> splitNicelyWithPattern( String toSplit, String patternSeparator ) {

		if( patternSeparator == null || patternSeparator.isEmpty())
			throw new IllegalArgumentException( "The separator cannot be null or the empty string." );

		List<String> result = new ArrayList<> ();
		if( ! Utils.isEmptyOrWhitespaces( toSplit )) {
			for( String s : toSplit.split( patternSeparator ))
				result.add( s .trim());
		}

		return result;
	}


	/**
	 * Creates a new list and only keeps values that are not null or made up of white characters.
	 * @param values a non-null list of items (can contain null and "empty" values)
	 * @return a list of items (never null), with no null or "empty" values
	 */
	public static List<String> filterEmptyValues( List<String> values ) {

		List<String> result = new ArrayList<> ();
		for( String s : values ) {
			if( ! Utils.isEmptyOrWhitespaces( s ))
				result.add( s );
		}

		return result;
	}


	/**
	 * Formats a collection of elements as a string.
	 * @param items a non-null list of items
	 * @param separator a string to separate items
	 * @return a non-null string
	 */
	public static String format( Collection<String> items, String separator ) {

		StringBuilder sb = new StringBuilder();
		for( Iterator<String> it = items.iterator(); it.hasNext(); ) {
			sb.append( it.next());
			if( it.hasNext())
				sb.append( separator );
		}

		return sb.toString();
	}


	/**
	 * Expands a template, replacing each {{ param }} by the corresponding value.
	 * <p>
	 * Eg. "My name is {{ name }}" will result in "My name is Bond", provided that "params" contains "name=Bond".
	 * </p>
	 *
	 * @param s the template to expand
	 * @param params the parameters to be expanded in the template
	 * @return the expanded template
	 */
	public static String expandTemplate(String s, Properties params) {

		String result;
		if( params == null || params.size() < 1 ) {
			result = s;

		} else {
			StringBuffer sb = new StringBuffer();
			Pattern pattern = Pattern.compile( "\\{\\{\\s*\\S+\\s*\\}\\}" );
			Matcher m = pattern.matcher( s );

			while( m.find()) {
				String raw = m.group();
				String varName = m.group().replace('{', ' ').replace('}', ' ').trim();
				String val = params.getProperty(varName);
				val = (val == null ? raw : val.trim());

				m.appendReplacement(sb, val);
			}

			m.appendTail( sb );
			result = sb.toString();
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
	 * Neither <i>in</i> nor <i>os</i> are closed by this method.<br>
	 * They must be explicitly closed after this method is called.
	 * </p>
	 * <p>
	 * Be careful, this method should be avoided when possible.
	 * It was responsible for memory leaks. See #489.
	 * </p>
	 *
	 * @param in an input stream (not null)
	 * @param os an output stream (not null)
	 * @throws IOException if an error occurred
	 */
	public static void copyStreamUnsafelyUseWithCaution( InputStream in, OutputStream os ) throws IOException {

		byte[] buf = new byte[ 1024 ];
		int len;
		while((len = in.read( buf )) > 0) {
			os.write( buf, 0, len );
		}
	}


	/**
	 * Copies the content from in into os.
	 * <p>
	 * This method closes the input stream.
	 * <i>os</i> does not need to be closed.
	 * </p>
	 *
	 * @param in an input stream (not null)
	 * @param os an output stream (not null)
	 * @throws IOException if an error occurred
	 */
	public static void copyStreamSafely( InputStream in, ByteArrayOutputStream os ) throws IOException {

		try {
			copyStreamUnsafelyUseWithCaution( in, os );

		} finally {
			in.close();
		}
	}


	/**
	 * Copies the content from in into outputFile.
	 * <p>
	 * <i>in</i> is not closed by this method.<br>
	 * It must be explicitly closed after this method is called.
	 * </p>
	 *
	 * @param in an input stream (not null)
	 * @param outputFile will be created if it does not exist
	 * @throws IOException if the file could not be created
	 */
	public static void copyStream( InputStream in, File outputFile ) throws IOException {
		OutputStream os = new FileOutputStream( outputFile );
		try {
			copyStreamUnsafelyUseWithCaution( in, os );
		} finally {
			os.close ();
		}
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
		try {
			copyStream( is, outputFile );
		} finally {
			is.close();
		}
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
		try {
			copyStreamUnsafelyUseWithCaution( is, os );
		} finally {
			is.close();
		}
	}


	/**
	 * Writes a string into a file.
	 *
	 * @param s the string to write (not null)
	 * @param outputFile the file to write into
	 * @throws IOException if something went wrong
	 */
	public static void writeStringInto( String s, File outputFile ) throws IOException {
		InputStream in = new ByteArrayInputStream( s.getBytes( StandardCharsets.UTF_8 ));
		copyStream( in, outputFile );
	}


	/**
	 * Appends a string into a file.
	 *
	 * @param s the string to write (not null)
	 * @param outputFile the file to write into
	 * @throws IOException if something went wrong
	 */
	public static void appendStringInto( String s, File outputFile ) throws IOException {

		OutputStreamWriter fw = null;
		try {
			fw = new OutputStreamWriter( new FileOutputStream( outputFile, true ), StandardCharsets.UTF_8 );
			fw.append( s );

		} finally {
			Utils.closeQuietly( fw );
		}
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
	 * Reads a text file content and returns it as a string.
	 * <p>
	 * The file is tried to be read with UTF-8 encoding.
	 * If it fails, the default system encoding is used.
	 * </p>
	 *
	 * @param file the file whose content must be loaded (can be null)
	 * @param logger a logger (not null)
	 * @return the file content or the empty string if an error occurred
	 */
	public static String readFileContentQuietly( File file, Logger logger ) {

		String result = "";
		try {
			if( file != null && file.exists())
				result = readFileContent( file );

		} catch( Exception e ) {
			logger.severe( "File " + file + " could not be read." );
			logException( logger, e );
		}

		return result;
	}


	/**
	 * Reads the content of an URL (assumed to be text content).
	 * @param url an URL
	 * @return a non-null string
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static String readUrlContent( String url )
	throws IOException, URISyntaxException {

		InputStream in = null;
		try {
			URI uri = UriUtils.urlToUri( url );
			in = uri.toURL().openStream();
			ByteArrayOutputStream os = new ByteArrayOutputStream();

			Utils.copyStreamSafely( in, os );
			return os.toString( "UTF-8" );

		} finally {
			closeQuietly( in );
		}
	}


	/**
	 * Reads the content of an URL (assumed to be text content).
	 * @param url an URL
	 * @return a non-null string
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static String readUrlContentQuietly( String url, Logger logger ) {

		String result = "";
		try {
			result = readUrlContent( url );

		} catch( Exception e ) {
			logger.severe( "Content of URL " + url + " could not be read." );
			logException( logger, e );
		}

		return result;
	}


	/**
	 * Reads properties from a file.
	 * @param file a properties file
	 * @return a {@link Properties} instance
	 * @throws IOException if reading failed
	 */
	public static Properties readPropertiesFile( File file ) throws IOException {

		Properties result = new Properties();
		InputStream in = null;
		try {
			in = new FileInputStream( file );
			result.load( in );

		} finally {
			closeQuietly( in );
		}

		return result;
	}


	/**
	 * Reads properties from a file but does not throw any error in case of problem.
	 * @param file a properties file (can be null)
	 * @param logger a logger (not null)
	 * @return a {@link Properties} instance (never null)
	 */
	public static Properties readPropertiesFileQuietly( File file, Logger logger ) {

		Properties result = new Properties();
		try {
			if( file != null && file.exists())
				result = readPropertiesFile( file );

		} catch( Exception e ) {
			logger.severe( "Properties file " + file + " could not be read." );
			logException( logger, e );
		}

		return result;
	}


	/**
	 * Reads properties from a string.
	 * @param file a properties file
	 * @param logger a logger (not null)
	 * @return a {@link Properties} instance
	 */
	public static Properties readPropertiesQuietly( String fileContent, Logger logger ) {

		Properties result = new Properties();
		try {
			if( fileContent != null ) {
				InputStream in = new ByteArrayInputStream( fileContent.getBytes( StandardCharsets.UTF_8 ));
				result.load( in );
			}

		} catch( Exception e ) {
			logger.severe( "Properties could not be read from a string." );
			logException( logger, e );
		}

		return result;
	}


	/**
	 * Writes Java properties into a file.
	 * @param properties non-null properties
	 * @param file a properties file
	 * @throws IOException if writing failed
	 */
	public static void writePropertiesFile( Properties properties, File file ) throws IOException {

		OutputStream out = null;
		try {
			out = new FileOutputStream( file );
			properties.store( out, "" );

		} finally {
			closeQuietly( out );
		}
	}


	/**
	 * Replaces all the accents in a string.
	 * @param name a non-null string
	 * @return a non-null string, with their accents replaced by their neutral equivalent
	 */
	public static String cleanNameWithAccents( String name ) {

		String temp = Normalizer.normalize( name, Normalizer.Form.NFD );
		Pattern pattern = Pattern.compile( "\\p{InCombiningDiacriticalMarks}+" );
		return pattern.matcher( temp ).replaceAll( "" ).trim();
	}


	/**
	 * Creates a directory if it does not exist.
	 * @param directory the directory to create
	 * @throws IOException if it did not exist and that it could not be created
	 */
	public static void createDirectory( File directory ) throws IOException {

		if( ! directory.exists()
				&& ! directory.mkdirs())
			throw new IOException( "The directory " + directory + " could not be created." );
	}


	/**
	 * Equivalent to <code>listAllFiles( directory, false )</code>.
	 * @param directory an existing directory
	 * @return a non-null list of files
	 */
	public static List<File> listAllFiles( File directory ) {
		return listAllFiles( directory, false );
	}


	/**
	 * Updates string properties.
	 * @param propertiesContent the properties file as a string
	 * @param keyToNewValue the keys to update with their new values
	 * @return a non-null string
	 */
	public static String updateProperties( String propertiesContent, Map<String,String> keyToNewValue ) {

		for( Map.Entry<String,String> entry : keyToNewValue.entrySet()) {
			propertiesContent = propertiesContent.replaceFirst(
					"(?mi)^\\s*" + entry.getKey() + "\\s*[:=][^\n]*$",
					entry.getKey() + " = " + entry.getValue());
		}

		return propertiesContent;
	}


	/**
	 * Finds all the files (directly and indirectly) contained in a directory and with a given extension.
	 * <p>
	 * Search is case-insensitive.
	 * It means searching for properties or PROPERTIES extensions will give
	 * the same result.
	 * </p>
	 *
	 * @param directory an existing directory
	 * @param fileExtension a file extension (null will not filter extensions)
	 * <p>
	 * If it does not start with a dot, then one will be inserted at the first position.
	 * </p>
	 *
	 * @return a non-null list of files
	 */
	public static List<File> listAllFiles( File directory, String fileExtension ) {

		String ext = fileExtension;
		if( ext != null ) {
			ext = ext.toLowerCase();
			if( ! ext.startsWith( "." ))
				ext = "." + ext;
		}

		List<File> result = new ArrayList<> ();
		for( File f : listAllFiles( directory )) {
			if( ext == null || f.getName().toLowerCase().endsWith( ext ))
				result.add( f );
		}

		return result;
	}


	/**
	 * Finds all the files (direct and indirect) from a directory.
	 * <p>
	 * This method skips hidden files and files whose name starts
	 * with a dot.
	 * </p>
	 *
	 * @param directory an existing directory
	 * @param includeDirectories true to include directories, false to exclude them from the result
	 * @return a non-null list of files, sorted alphabetically by file names
	 */
	public static List<File> listAllFiles( File directory, boolean includeDirectories ) {

		if( ! directory.isDirectory())
			throw new IllegalArgumentException( directory.getAbsolutePath() + " does not exist or is not a directory." );

		List<File> result = new ArrayList<> ();
		List<File> directoriesToInspect = new ArrayList<> ();
		directoriesToInspect.add( directory );

		while( ! directoriesToInspect.isEmpty()) {
			File currentDirectory = directoriesToInspect.remove( 0 );
			if( includeDirectories )
				result.add( currentDirectory );

			File[] subFiles = currentDirectory.listFiles();
			if( subFiles == null )
				continue;

			for( File subFile : subFiles ) {
				if( subFile.isHidden()
						|| subFile.getName().startsWith( "." ))
					continue;

				if( subFile.isFile())
					result.add( subFile );
				else
					directoriesToInspect.add( subFile );
			}
		}

		Collections.sort( result, new FileNameComparator());
		return result;
	}


	/**
	 * Finds all the files directly contained in a directory and with a given extension.
	 * <p>
	 * Search is case-insensitive.
	 * It means searching for properties or PROPERTIES extensions will give
	 * the same result.
	 * </p>
	 *
	 * @param directory an existing directory
	 * @param fileExtension a file extension (null will not filter extensions)
	 * <p>
	 * If it does not start with a dot, then one will be inserted at the first position.
	 * </p>
	 *
	 * @return a non-null list of files
	 */
	public static List<File> listDirectFiles( File directory, String fileExtension ) {

		String ext = fileExtension;
		if( ext != null ) {
			ext = ext.toLowerCase();
			if( ! ext.startsWith( "." ))
				ext = "." + ext;
		}

		List<File> result = new ArrayList<> ();
		File[] files = directory.listFiles();
		if( files != null ) {
			for( File f : files ) {
				if( f.isFile() &&
						(ext == null || f.getName().toLowerCase().endsWith( ext ))) {
					result.add( f );
				}
			}
		}

		return result;
	}


	/**
	 * Lists directories located under a given file.
	 * @param root a file
	 * @return a non-null list of directories, sorted alphabetically by file names
	 */
	public static List<File> listDirectories( File root ) {

		List<File> result = new ArrayList<> ();
		File[] files = root.listFiles( new DirectoryFileFilter());
		if( files != null )
			result.addAll( Arrays.asList( files ));

		Collections.sort( result, new FileNameComparator());
		return result;
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	static final class FileNameComparator implements Serializable, Comparator<File> {
		private static final long serialVersionUID = -4671366958457961589L;

		@Override
		public int compare( File o1, File o2 ) {
			return o1.getName().compareTo( o2.getName());
		}
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	static class DirectoryFileFilter implements FileFilter {
		@Override
		public boolean accept( File f ) {
			return f.isDirectory();
		}
	}


	/**
	 * Stores the resources from a directory into a map.
	 * @param directory an existing directory
	 * @return a non-null map (key = the file location, relative to the directory, value = file content)
	 * @throws IOException if something went wrong while reading a file
	 */
	public static Map<String,byte[]> storeDirectoryResourcesAsBytes( File directory ) throws IOException {
		return storeDirectoryResourcesAsBytes( directory, new ArrayList<String>( 0 ));
	}


	/**
	 * Stores the resources from a directory into a map.
	 * @param directory an existing directory
	 * @param exclusionPatteners a non-null list of exclusion patterns for file names (e.g. ".*\\.properties")
	 * @return a non-null map (key = the file location, relative to the directory, value = file content)
	 * @throws IOException if something went wrong while reading a file
	 */
	public static Map<String,byte[]> storeDirectoryResourcesAsBytes( File directory, List<String> exclusionPatteners )
	throws IOException {

		if( ! directory.exists())
			throw new IllegalArgumentException( "The resource directory was not found. " + directory.getAbsolutePath());

		if( ! directory.isDirectory())
			throw new IllegalArgumentException( "The resource directory is not a valid directory. " + directory.getAbsolutePath());

		Map<String,byte[]> result = new HashMap<> ();
		List<File> resourceFiles = listAllFiles( directory, false );
		fileLoop: for( File file : resourceFiles ) {

			for( String exclusionPattern : exclusionPatteners ) {
				if( file.getName().matches( exclusionPattern ))
					continue fileLoop;
			}

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
		Map<String,String> result = new HashMap<>( map.size());
		for( Map.Entry<String,byte[]> entry : map.entrySet())
			result.put( entry.getKey(), new String( entry.getValue(), StandardCharsets.UTF_8 ));

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

		extractZipArchive( zipFile, targetDirectory, null, null );
	}


	/**
	 * Extracts a ZIP archive in a directory (with advanced options).
	 * <p>
	 * Imagine you have an archive that contains pom.xml, graph/main.graph and graph/vm/init.pp.
	 * Let's now suppose you want to extract only the files located under "graph". And let's suppose
	 * you do not want to create a "graph" sub-directory in your target. Then...
	 * </p>
	 * <code>extractZipArchive( your.zip, your.target.dir, "graph/.*\\.graph, "graph/" );</code>
	 * <p>
	 * ... will only create main.graph in your target directory (and not graph/main.graph).
	 * </p>
	 *
	 * @param zipFile a ZIP file (not null, must exist)
	 * @param targetDirectory the target directory (may not exist but must be a directory)
	 * @param entryPattern a pattern to only extract some entries (null for all entries)
	 * @param removedEntryPrefix an entry prefix to remove (null to ignore)
	 * @throws IOException if something went wrong
	 */
	public static void extractZipArchive( File zipFile, File targetDirectory, String entryPattern, String removedEntryPrefix )
	throws IOException {

		// Make some checks
		if( zipFile == null || targetDirectory == null )
			throw new IllegalArgumentException( "The ZIP file and the target directory cannot be null." );

		if( ! zipFile.isFile())
			throw new IllegalArgumentException( "ZIP file " + targetDirectory.getName() + " does not exist." );

		if( targetDirectory.exists()
				&& ! targetDirectory.isDirectory())
			throw new IllegalArgumentException( "Target directory " + targetDirectory.getName() + " is not a directory." );

		Utils.createDirectory( targetDirectory );

		// Load the ZIP file
		ZipFile theZipFile = new ZipFile( zipFile );
		Enumeration<? extends ZipEntry> entries = theZipFile.entries();

		// And start the copy
		try {
			while( entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String suffix = entry.getName();

				// Deal with extract options
				if( entryPattern != null
						&& ! suffix.matches( entryPattern ))
					continue;

				if( removedEntryPrefix != null
						&& suffix.startsWith( removedEntryPrefix ))
					suffix = suffix.substring( removedEntryPrefix.length());

				if( isEmptyOrWhitespaces( suffix ))
					continue;

				// Extract...
				File f = new File( targetDirectory, suffix );

				// Case 'directory': create it.
				// Case 'file': create its parents and copy the content.
				if( entry.isDirectory()) {
					Utils.createDirectory( f );

				} else {
					Utils.createDirectory( f.getParentFile());
					copyStream( theZipFile.getInputStream( entry ), f );
				}
			}

		} finally {
			// Close the stream
			theZipFile.close();
		}
	}


	/**
	 * Determines whether a directory contains a given file.
	 * @param ancestorCandidate the directory
	 * @param file the file
	 * @return true if the directory directly or indirectly contains the file
	 */
	public static boolean isAncestor( File ancestorCandidate, File file ) {

		String path = ancestorCandidate.getAbsolutePath();
		if( ! path.endsWith( "/" ))
			path += "/";

		return file.getAbsolutePath().startsWith( path );
	}


	/**
	 * Deletes files recursively.
	 * @param files the files to delete
	 * @throws IOException if a file could not be deleted
	 */
	public static void deleteFilesRecursively( File... files ) throws IOException {

		if( files == null )
			return;

		List<File> filesToDelete = new ArrayList<> ();
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
	 * Deletes files recursively and remains quiet even if an exception is thrown.
	 * @param files the files to delete
	 */
	public static void deleteFilesRecursivelyAndQuietly( File... files ) {

		try {
			deleteFilesRecursively( files );

		} catch( IOException e ) {
			Logger logger = Logger.getLogger( Utils.class.getName());
			logException( logger, e );
		}
	}


	/**
	 * Writes an exception's stack trace into a string.
	 * <p>
	 * {@link #logException(Logger, Exception)} has better performances
	 * and should be used for logging purpose.
	 * </p>
	 *
	 * @param t an exception or a throwable (not null)
	 * @return a string
	 */
	public static String writeExceptionButDoNotUseItForLogging( Throwable t ) {

		StringWriter sw = new StringWriter();
		t.printStackTrace( new PrintWriter( sw ));

		return sw.toString();
	}


	/**
	 * Logs an exception with the given logger and the given level.
	 * <p>
	 * Writing a stack trace may be time-consuming in some environments.
	 * To prevent useless computing, this method checks the current log level
	 * before trying to log anything.
	 * </p>
	 *
	 * @param logger the logger
	 * @param t an exception or a throwable
	 * @param logLevel the log level (see {@link Level})
	 * @param message a message to insert before the stack trace
	 */
	public static void logException( Logger logger, Level logLevel, Throwable t, String message ) {

		if( logger.isLoggable( logLevel )) {
			StringBuilder sb = new StringBuilder();
			if( message != null ) {
				sb.append( message );
				sb.append( "\n" );
			}

			sb.append( writeExceptionButDoNotUseItForLogging( t ));
			logger.log( logLevel, sb.toString());
		}
	}


	/**
	 * Logs an exception with the given logger and the given level.
	 * <p>
	 * Writing a stack trace may be time-consuming in some environments.
	 * To prevent useless computing, this method checks the current log level
	 * before trying to log anything.
	 * </p>
	 *
	 * @param logger the logger
	 * @param t an exception or a throwable
	 * @param logLevel the log level (see {@link Level})
	 */
	public static void logException( Logger logger, Level logLevel, Throwable t ) {
		logException( logger, logLevel, t, null );
	}


	/**
	 * Logs an exception with the given logger and the FINEST level.
	 * @param logger the logger
	 * @param t an exception or a throwable
	 */
	public static void logException( Logger logger, Throwable t ) {
		logException( logger, Level.FINEST, t );
	}


	/**
	 * Closes a prepared statement.
	 * @param ps
	 * @param logger
	 */
	public static void closeStatement( PreparedStatement ps, Logger logger ) {

		try {
			if( ps != null )
				ps.close();

		} catch( SQLException e ) {
			// Not important.
			Utils.logException( logger, e );
		}
	}


	/**
	 * Closes a statement.
	 * @param st
	 * @param logger
	 */
	public static void closeStatement( Statement st, Logger logger ) {

		try {
			if( st != null )
				st.close();

		} catch( SQLException e ) {
			// Not important.
			Utils.logException( logger, e );
		}
	}


	/**
	 * Closes a result set.
	 * @param st
	 * @param logger
	 */
	public static void closeResultSet( ResultSet resultSet, Logger logger ) {

		try {
			if( resultSet != null )
				resultSet.close();

		} catch( SQLException e ) {
			// Not important.
			Utils.logException( logger, e );
		}
	}


	/**
	 * Closes a connection to a database.
	 * @param conn
	 * @param logger
	 */
	public static void closeConnection( Connection conn, Logger logger ) {

		try {
			if( conn != null )
				conn.close();

		} catch( SQLException e ) {
			// Not important.
			Utils.logException( logger, e );
		}
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

		Utils.createDirectory( target );
		for( File sourceFile : listAllFiles( source, false )) {
			String path = computeFileRelativeLocation( source, sourceFile );
			File targetFile = new File( target, path );

			Utils.createDirectory( targetFile.getParentFile());
			copyStream( sourceFile, targetFile );
		}
	}


	/**
	 * Parses a raw URL and extracts the host and port.
	 * @param url a raw URL (not null)
	 * @return a non-null map entry (key = host URL without the port, value = the port, -1 if not specified)
	 */
	public static Map.Entry<String,Integer> findUrlAndPort( String url ) {

		Matcher m = Pattern.compile( ".*(:\\d+).*" ).matcher( url );
		String portAsString = m.find() ? m.group( 1 ).substring( 1 ) : null;
		Integer port = portAsString == null ? - 1 : Integer.parseInt( portAsString );
		String address = portAsString == null ? url : url.replace( m.group( 1 ), "" );

		return new AbstractMap.SimpleEntry<>( address, port );
	}


	/**
	 * Returns the value contained in a map of string if it exists using the key.
	 * @param map a map of string
	 * @param key a string
	 * @param defaultValue the default value
	 */
	public static String getValue(Map<String,String> map, String key, String defaultValue) {
		return map.containsKey( key ) ? map.get( key ) : defaultValue;
	}
}
