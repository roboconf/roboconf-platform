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

package net.roboconf.core.userdata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class UserDataHelpersTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	public static final String MESSAGING_IP = "messaging.ip";
	public static final String MESSAGING_USERNAME = "messaging.username";
	public static final String MESSAGING_PASSWORD = "messaging.password";


	@Test
	public void testWriteAndRead() throws Exception {

		String rawProperties = UserDataHelpers.writeUserDataAsString( msgCfg("192.168.1.24", "user", "pwd"), "domain", "app", "/root" );
		Properties props = UserDataHelpers.readUserData( rawProperties, null );
		Assert.assertEquals( "app", props.getProperty( UserDataHelpers.APPLICATION_NAME ));
		Assert.assertEquals( "/root", props.getProperty( UserDataHelpers.SCOPED_INSTANCE_PATH ));
		Assert.assertEquals( "domain", props.getProperty( UserDataHelpers.DOMAIN ));
		Assert.assertEquals( "192.168.1.24", props.getProperty( MESSAGING_IP ));
		Assert.assertEquals( "pwd", props.getProperty( MESSAGING_PASSWORD ));
		Assert.assertEquals( "user", props.getProperty( MESSAGING_USERNAME ));

		rawProperties = UserDataHelpers.writeUserDataAsString( msgCfg(null, "user", "pwd"), "domain", "app", "/root" );
		props = UserDataHelpers.readUserData( rawProperties, null );
		Assert.assertEquals( "app", props.getProperty( UserDataHelpers.APPLICATION_NAME ));
		Assert.assertEquals( "/root", props.getProperty( UserDataHelpers.SCOPED_INSTANCE_PATH ));
		Assert.assertEquals( "domain", props.getProperty( UserDataHelpers.DOMAIN ));
		Assert.assertEquals( null, props.getProperty( MESSAGING_IP ));
		Assert.assertEquals( "pwd", props.getProperty( MESSAGING_PASSWORD ));
		Assert.assertEquals( "user", props.getProperty( MESSAGING_USERNAME ));

		rawProperties = UserDataHelpers.writeUserDataAsString( msgCfg("192.168.1.24", null, "pwd"), "domain4", "app", "/root" );
		props = UserDataHelpers.readUserData( rawProperties, null );
		Assert.assertEquals( "app", props.getProperty( UserDataHelpers.APPLICATION_NAME ));
		Assert.assertEquals( "/root", props.getProperty( UserDataHelpers.SCOPED_INSTANCE_PATH ));
		Assert.assertEquals( "domain4", props.getProperty( UserDataHelpers.DOMAIN ));
		Assert.assertEquals( "192.168.1.24", props.getProperty( MESSAGING_IP ));
		Assert.assertEquals( "pwd", props.getProperty( MESSAGING_PASSWORD ));
		Assert.assertEquals( null, props.getProperty( MESSAGING_USERNAME ));

		rawProperties = UserDataHelpers.writeUserDataAsString( msgCfg("192.168.1.24", "user", null), "domain", "app", "root" );
		props = UserDataHelpers.readUserData( rawProperties, null );
		Assert.assertEquals( "app", props.getProperty( UserDataHelpers.APPLICATION_NAME ));
		Assert.assertEquals( "root", props.getProperty( UserDataHelpers.SCOPED_INSTANCE_PATH ));
		Assert.assertEquals( "domain", props.getProperty( UserDataHelpers.DOMAIN ));
		Assert.assertEquals( "192.168.1.24", props.getProperty( MESSAGING_IP ));
		Assert.assertEquals( null, props.getProperty( MESSAGING_PASSWORD ));
		Assert.assertEquals( "user", props.getProperty( MESSAGING_USERNAME ));

		rawProperties = UserDataHelpers.writeUserDataAsString( msgCfg("192.168.1.24", "user", "pwd"), null, null, "root" );
		props = UserDataHelpers.readUserData( rawProperties, null );
		Assert.assertEquals( null, props.getProperty( UserDataHelpers.APPLICATION_NAME ));
		Assert.assertEquals( "root", props.getProperty( UserDataHelpers.SCOPED_INSTANCE_PATH ));
		Assert.assertEquals( "192.168.1.24", props.getProperty( MESSAGING_IP ));
		Assert.assertEquals( "pwd", props.getProperty( MESSAGING_PASSWORD ));
		Assert.assertEquals( "user", props.getProperty( MESSAGING_USERNAME ));

		rawProperties = UserDataHelpers.writeUserDataAsString( msgCfg("192.168.1.24", "user", "pwd"), "domain", "app", null );
		props = UserDataHelpers.readUserData( rawProperties, null );
		Assert.assertEquals( "app", props.getProperty( UserDataHelpers.APPLICATION_NAME ));
		Assert.assertEquals( null, props.getProperty( UserDataHelpers.SCOPED_INSTANCE_PATH ));
		Assert.assertEquals( "domain", props.getProperty( UserDataHelpers.DOMAIN ));
		Assert.assertEquals( "192.168.1.24", props.getProperty( MESSAGING_IP ));
		Assert.assertEquals( "pwd", props.getProperty( MESSAGING_PASSWORD ));
		Assert.assertEquals( "user", props.getProperty( MESSAGING_USERNAME ));

		rawProperties = UserDataHelpers.writeUserDataAsString( msgCfg("192.168.1.24:9120", "user", "pwd"), "domain", "app", null );
		props = UserDataHelpers.readUserData( rawProperties, null );
		Assert.assertEquals( "app", props.getProperty( UserDataHelpers.APPLICATION_NAME ));
		Assert.assertEquals( null, props.getProperty( UserDataHelpers.SCOPED_INSTANCE_PATH ));
		Assert.assertEquals( "domain", props.getProperty( UserDataHelpers.DOMAIN ));
		Assert.assertEquals( "192.168.1.24:9120", props.getProperty( MESSAGING_IP ));
		Assert.assertEquals( "pwd", props.getProperty( MESSAGING_PASSWORD ));
		Assert.assertEquals( "user", props.getProperty( MESSAGING_USERNAME ));

		rawProperties = UserDataHelpers.writeUserDataAsString( null, "domain", "app", null );
		props = UserDataHelpers.readUserData( rawProperties, null );
		Assert.assertEquals( "app", props.getProperty( UserDataHelpers.APPLICATION_NAME ));
		Assert.assertEquals( null, props.getProperty( UserDataHelpers.SCOPED_INSTANCE_PATH ));
		Assert.assertEquals( "domain", props.getProperty( UserDataHelpers.DOMAIN ));
	}


	@Test
	public void testEncodeAndDecode() {

		final byte[] binary = new byte[ 509 ];
		ThreadLocalRandom.current().nextBytes( binary );

		byte[][] bytes = new byte[][] {
			"hi!\n\tnice 2 see u".getBytes( StandardCharsets.UTF_8 ),
			"*:sdf:: !dfs\n\r48sdf[ )".getBytes( StandardCharsets.UTF_8 ),
			binary
		};

		int cpt = 0;
		for( byte[] b : bytes ) {
			String encoded = UserDataHelpers.encodeToBase64( b );
			byte[] decoded = UserDataHelpers.decodeFromBase64( encoded );
			Assert.assertArrayEquals( "Index " + cpt,  b, decoded );
			cpt ++;
		}
	}


	@Test
	public void testWriteAndRead_withFiles() throws Exception {

		final String s = "this is\na test!\t";
		File f = this.folder.newFile();
		Utils.writeStringInto( s, f );

		final String key1 = "key1.loc";
		final String key2 = "key2";
		final String key3 = "key3";

		Map<String,String> msgCfg = msgCfg( "192.168.1.24", "user", "pwd" );
		msgCfg.put( key1, f.getAbsolutePath());
		msgCfg.put( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + key1, "" );
		msgCfg.put( key2, f.getAbsolutePath());
		msgCfg.put( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + key2, "" );

		// Test a missing file
		msgCfg.put( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + key3, "" );

		Properties props = UserDataHelpers.writeUserDataAsProperties( msgCfg, "domain", "app", "/root" );
		Assert.assertEquals( msgCfg.size() + 3, props.size());
		Assert.assertTrue( props.getProperty( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + key1 ).length() > 1 );
		Assert.assertTrue( props.getProperty( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + key2 ).length() > 1 );

		String rawProperties = UserDataHelpers.writeUserDataAsString( msgCfg, "domain", "app", "/root" );

		File outputDirectory = this.folder.newFolder();
		props = UserDataHelpers.readUserData( rawProperties, outputDirectory );

		Assert.assertEquals( msgCfg.size() + 1, props.size());
		Assert.assertNull( props.get( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + key1 ));
		Assert.assertNull( props.get( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + key2 ));

		Assert.assertEquals( "app", props.getProperty( UserDataHelpers.APPLICATION_NAME ));
		Assert.assertEquals( "/root", props.getProperty( UserDataHelpers.SCOPED_INSTANCE_PATH ));
		Assert.assertEquals( "domain", props.getProperty( UserDataHelpers.DOMAIN ));
		Assert.assertEquals( "192.168.1.24", props.getProperty( MESSAGING_IP ));
		Assert.assertEquals( "pwd", props.getProperty( MESSAGING_PASSWORD ));
		Assert.assertEquals( "user", props.getProperty( MESSAGING_USERNAME ));

		String copiedFilePath = props.getProperty( key1 );
		Assert.assertNotNull( copiedFilePath );
		File f1 = new File( copiedFilePath );
		Assert.assertTrue( f1.isFile());

		copiedFilePath = props.getProperty( key2 );
		Assert.assertNotNull( copiedFilePath );
		File f2 = new File( copiedFilePath );
		Assert.assertTrue( f2.isFile());

		Assert.assertEquals( f1, f2 );
		String copiedContent = Utils.readFileContent( f1 );
		Assert.assertEquals( s, copiedContent );
	}


	@Test
	public void testWriteAndRead_withBinaryFiles() throws Exception {

		File f = this.folder.newFile();
		final byte[] bytes = new byte[200];
		ThreadLocalRandom.current().nextBytes( bytes );
		Utils.copyStream( new ByteArrayInputStream( bytes ), f );

		// Verify we wrote the bytes correctly
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Utils.copyStream( f, os );
		Assert.assertArrayEquals( bytes, os.toByteArray());

		// Good. Let's now handle the properties transfer
		final String key1 = "key1.loc";
		final String key2 = "key2";
		final String key3 = "key3";

		Map<String,String> msgCfg = msgCfg( "192.168.1.24", "user", "pwd" );
		msgCfg.put( key1, f.getAbsolutePath());
		msgCfg.put( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + key1, "" );
		msgCfg.put( key2, f.getAbsolutePath());
		msgCfg.put( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + key2, "" );

		// Test a missing file
		msgCfg.put( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + key3, "" );

		Properties props = UserDataHelpers.writeUserDataAsProperties( msgCfg, "domain", "app", "/root" );
		Assert.assertEquals( msgCfg.size() + 3, props.size());
		Assert.assertTrue( props.getProperty( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + key1 ).length() > 1 );
		Assert.assertTrue( props.getProperty( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + key2 ).length() > 1 );

		String rawProperties = UserDataHelpers.writeUserDataAsString( msgCfg, "domain", "app", "/root" );

		File outputDirectory = this.folder.newFolder();
		props = UserDataHelpers.readUserData( rawProperties, outputDirectory );

		Assert.assertEquals( msgCfg.size() + 1, props.size());
		Assert.assertNull( props.get( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + key1 ));
		Assert.assertNull( props.get( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + key2 ));

		Assert.assertEquals( "app", props.getProperty( UserDataHelpers.APPLICATION_NAME ));
		Assert.assertEquals( "/root", props.getProperty( UserDataHelpers.SCOPED_INSTANCE_PATH ));
		Assert.assertEquals( "domain", props.getProperty( UserDataHelpers.DOMAIN ));
		Assert.assertEquals( "192.168.1.24", props.getProperty( MESSAGING_IP ));
		Assert.assertEquals( "pwd", props.getProperty( MESSAGING_PASSWORD ));
		Assert.assertEquals( "user", props.getProperty( MESSAGING_USERNAME ));

		String copiedFilePath = props.getProperty( key1 );
		Assert.assertNotNull( copiedFilePath );
		File f1 = new File( copiedFilePath );
		Assert.assertTrue( f1.isFile());

		copiedFilePath = props.getProperty( key2 );
		Assert.assertNotNull( copiedFilePath );
		File f2 = new File( copiedFilePath );
		Assert.assertTrue( f2.isFile());

		// Same file, same content.
		// Let's compare it with the original array of bytes.
		Assert.assertEquals( f1, f2 );
		os = new ByteArrayOutputStream();
		Utils.copyStream( f1, os );
		Assert.assertArrayEquals( bytes, os.toByteArray());
	}


	@Test
	public void testInterceptWritingFiles_inexistingOutputDirectory() throws Exception {

		File outputDirectory = this.folder.newFolder();
		Utils.deleteFilesRecursively( outputDirectory );
		Assert.assertFalse( outputDirectory.isDirectory());

		UserDataHelpers.interceptWritingFiles( new Properties(), outputDirectory );
		Assert.assertFalse( outputDirectory.isDirectory());
	}


	@Test
	public void testInterceptWritingFiles_inexistingOutputDirectory_withFiles() throws Exception {

		File outputDirectory = this.folder.newFolder();
		Utils.deleteFilesRecursively( outputDirectory );
		Assert.assertFalse( outputDirectory.isDirectory());

		File file = this.folder.newFile();

		Properties props = new Properties();
		props.put( "loc", file.getAbsolutePath());
		props.put( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + "loc", "" );

		UserDataHelpers.interceptWritingFiles( props, outputDirectory );
		Assert.assertTrue( outputDirectory.isDirectory());
		Assert.assertTrue( file.isFile());
	}


	@Test( expected = IOException.class )
	public void testWriteUserDataAsProperties_inexistingFile() throws Exception {

		Map<String,String> msgCfg = msgCfg( "192.168.1.24", "user", "pwd" );
		msgCfg.put( "loc", "does/not/exist" );
		msgCfg.put( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + "loc", "" );

		UserDataHelpers.writeUserDataAsProperties( msgCfg, "domain", "app", "/root" );
	}


	@Test( expected = IOException.class )
	public void testInterceptLoadingFiles_inexistingFile() throws Exception {

		Properties props = new Properties();
		props.put( "loc", "does/not/exist" );
		props.put( UserDataHelpers.ENCODE_FILE_CONTENT_PREFIX + "loc", "" );

		UserDataHelpers.interceptLoadingFiles( props );
	}


	/**
	 * Creates a pseudo messaging configuration for the given IP and credentials.
	 * @param ip the pseudo IP address.
	 * @param user the pseudo user.
	 * @param pass the pseudo password.
	 * @return the pseudo messaging configuration.
	 */
	private static Map<String, String> msgCfg(String ip, String user, String pass) {

		Map<String, String> result = new LinkedHashMap<> ();
		result.put(MESSAGING_IP, ip);
		result.put(MESSAGING_USERNAME, user);
		result.put(MESSAGING_PASSWORD, pass);

		return result;
	}
}
