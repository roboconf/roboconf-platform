/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

import java.io.File;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestApplicationTemplate;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;

/**
 * @author Vincent Zurczak - Linagora
 */
public class IconUtilsTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testFindMimeType() {

		Assert.assertEquals( IconUtils.MIME_BINARY, IconUtils.findMimeType( new File( "toto.mp4" )));
		Assert.assertEquals( IconUtils.MIME_GIF, IconUtils.findMimeType( new File( "toto.gif" )));
		Assert.assertEquals( IconUtils.MIME_JPG, IconUtils.findMimeType( new File( "toto.jpg" )));
		Assert.assertEquals( IconUtils.MIME_JPG, IconUtils.findMimeType( new File( "toto.jpEg" )));
		Assert.assertEquals( IconUtils.MIME_PNG, IconUtils.findMimeType( new File( "toto.png" )));
		Assert.assertEquals( IconUtils.MIME_SVG, IconUtils.findMimeType( new File( "toto.svg" )));
	}


	@Test
	public void testEncodeAndDecode() {

		String name = "app", version = null;
		Assert.assertEquals( "", IconUtils.encodeIconUrl( name, version, null ));

		File iconFile = new File( "whatever.jpg" );
		String path = IconUtils.encodeIconUrl( name, version, iconFile );
		Assert.assertEquals( "/app/whatever.jpg", path );

		Map.Entry<String,String> entry = IconUtils.decodeIconUrl( path );
		Assert.assertEquals( name, entry.getKey());
		Assert.assertNull( version, entry.getValue());

		name = "app2";
		version = "";
		path = IconUtils.encodeIconUrl( name, version, iconFile );
		entry = IconUtils.decodeIconUrl( path );
		Assert.assertEquals( name, entry.getKey());
		Assert.assertNull( version, entry.getValue());

		name = "app2";
		version = "v2";
		path = IconUtils.encodeIconUrl( name, version, iconFile );
		Assert.assertEquals( "/app2/v2/whatever.jpg", path );

		entry = IconUtils.decodeIconUrl( path );
		Assert.assertEquals( name, entry.getKey());
		Assert.assertEquals( version, entry.getValue());
	}


	@Test
	public void testDecodeUrl() {

		String path = "/app/version/img.jpg";
		Map.Entry<String,String> entry = IconUtils.decodeIconUrl( path );
		Assert.assertEquals( "app", entry.getKey());
		Assert.assertEquals( "version", entry.getValue());

		path = "app/version/img.jpg";
		entry = IconUtils.decodeIconUrl( path );
		Assert.assertEquals( "app", entry.getKey());
		Assert.assertEquals( "version", entry.getValue());

		path = "app/version/oops/";
		entry = IconUtils.decodeIconUrl( path );
		Assert.assertEquals( "app", entry.getKey());
		Assert.assertEquals( "version", entry.getValue());

		path = "/app/version/oops/";
		entry = IconUtils.decodeIconUrl( path );
		Assert.assertEquals( "app", entry.getKey());
		Assert.assertEquals( "version", entry.getValue());

		path = "app/version/oops.jpg/whatever";
		entry = IconUtils.decodeIconUrl( path );
		Assert.assertNull( entry.getKey());
		Assert.assertNull( entry.getValue());
	}


	@Test
	public void testFindIcon_app() throws Exception {

		File appDir = this.folder.newFolder();
		File descDir = new File( appDir, Constants.PROJECT_DIR_DESC );
		Assert.assertTrue( descDir.mkdirs());

		File trickFile = new File( descDir, "directory.jpg" );
		Assert.assertTrue( trickFile.mkdirs());
		Application app = new Application( "app", new TestApplicationTemplate()).directory( appDir );
		Assert.assertNull( IconUtils.findIcon( app ));

		File singleJpgFile = new File( descDir, "whatever.jpg" );
		Assert.assertTrue( singleJpgFile.createNewFile());
		Assert.assertEquals( singleJpgFile, IconUtils.findIcon( app ));

		File defaultFile = new File( descDir, "application.sVg" );
		Assert.assertTrue( defaultFile.createNewFile());
		Assert.assertEquals( defaultFile, IconUtils.findIcon( app ));
	}


	@Test
	public void testFindIcon_tpl() throws Exception {

		File appDir = this.folder.newFolder();
		ApplicationTemplate tpl = new ApplicationTemplate( "app" ).version( "v1" ).directory( appDir );

		File descDir = new File( appDir, Constants.PROJECT_DIR_DESC );
		Assert.assertTrue( descDir.mkdirs());

		File trickFile = new File( descDir, "file.txt" );
		Assert.assertTrue( trickFile.createNewFile());
		Assert.assertNull( IconUtils.findIcon( tpl ));

		File singleJpgFile = new File( descDir, "whatever.jpg" );
		Assert.assertTrue( singleJpgFile.createNewFile());
		Assert.assertEquals( singleJpgFile, IconUtils.findIcon( tpl ));

		File defaultFile = new File( descDir, "application.sVg" );
		Assert.assertTrue( defaultFile.createNewFile());
		Assert.assertEquals( defaultFile, IconUtils.findIcon( tpl ));

		Assert.assertNull( IconUtils.findIcon( new ApplicationTemplate( "app" ).version( "" )));
		Assert.assertNull( IconUtils.findIcon( new ApplicationTemplate( "app" ).version( "" )));
		Assert.assertNull( IconUtils.findIcon( new ApplicationTemplate( "app" ).version( "v2" )));
	}


	@Test
	public void testFindIconUrl() throws Exception {

		// Create fake icons
		ApplicationTemplate tpl = new ApplicationTemplate( "tpl" ).version( "v1" ).directory( this.folder.newFolder());
		File descDir = new File( tpl.getDirectory(), Constants.PROJECT_DIR_DESC );
		Assert.assertTrue( descDir.mkdirs());
		Assert.assertTrue( new File( descDir, "tp.jpg" ).createNewFile());

		Application app = new Application( "app", tpl ).directory( this.folder.newFolder());
		descDir = new File( app.getDirectory(), Constants.PROJECT_DIR_DESC );
		Assert.assertTrue( descDir.mkdirs());
		Assert.assertTrue( new File( descDir, "whatever.jpg" ).createNewFile());

		// Check the URLs
		Assert.assertEquals( "/tpl/v1/tp.jpg", IconUtils.findIconUrl( tpl ));
		Assert.assertEquals( "/app/whatever.jpg", IconUtils.findIconUrl( app ));

		// And we delete the icon
		Utils.deleteFilesRecursively( descDir );
		Assert.assertEquals( "", IconUtils.findIconUrl( app ));
	}


	@Test
	public void testFindIcon_nullConfigDirectory() throws Exception {

		// In case we try to get an icon while the DM is reconfigured
		ApplicationTemplate tpl = new ApplicationTemplate( "app" ).version( "v1" );
		Assert.assertNull( IconUtils.findIcon( tpl ));
	}
}
