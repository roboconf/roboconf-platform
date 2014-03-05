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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import junit.framework.Assert;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class UriUtilsTest {

	@Test
	public void testUrlToUri_1() {

		try {
			URL url = new URL( "http://roboconf.net" );
			Assert.assertEquals( url.toURI(), UriUtils.urlToUri( url ));

		} catch( Exception e ) {
			Assert.fail( e.getMessage());
		}

		try {
			URL url = new URL( "http://url.com/some%20folder" );
			Assert.assertEquals( url.toURI(), UriUtils.urlToUri( url ));

		} catch( Exception e ) {
			Assert.fail( e.getMessage());
		}

		try {
			URL url = new URL( "http://url.com/some folder" );
			Assert.assertEquals( new URI( "http://url.com/some%20folder" ), UriUtils.urlToUri( url ));

		} catch( Exception e ) {
			Assert.fail( e.getMessage());
		}
	}


	@Test
	public void testUrlToUri_2() {

		try {
			String url = "http://roboconf.net";
			Assert.assertEquals( new URI( url ), UriUtils.urlToUri( url ));

		} catch( Exception e ) {
			Assert.fail( e.getMessage());
		}

		try {
			String url = "http://url.com/some%20folder";
			Assert.assertEquals( new URI( url ), UriUtils.urlToUri( url ));

		} catch( Exception e ) {
			Assert.fail( e.getMessage());
		}

		try {
			String url = "http://url.com/some folder";
			Assert.assertEquals( new URI( "http://url.com/some%20folder" ), UriUtils.urlToUri( url ));

		} catch( Exception e ) {
			Assert.fail( e.getMessage());
		}
	}


	@Test
	public void testBuildNewURI() {

		try {
			String suffix = "http://absolute-url.fr";
			Assert.assertEquals( new URI( suffix ), UriUtils.buildNewURI( null, suffix ));

		} catch( URISyntaxException e ) {
			Assert.fail( e.getMessage());
		}

		try {
			String url = "http://absolute-url.fr/";
			String suffix = "readme.txt";
			Assert.assertEquals( new URI( url + suffix ), UriUtils.buildNewURI( new URI( url ), suffix ));

		} catch( URISyntaxException e ) {
			Assert.fail( e.getMessage());
		}

		try {
			String url = "http://absolute-url.fr";
			String suffix = "readme.txt";
			Assert.assertEquals( new URI( url + "/" + suffix ), UriUtils.buildNewURI( new URI( url ), suffix ));

		} catch( URISyntaxException e ) {
			Assert.fail( e.getMessage());
		}

		try {
			String url = "http://absolute-url.fr/folder";
			String suffix = "readme.txt";
			Assert.assertEquals( new URI( url + "/" + suffix ), UriUtils.buildNewURI( new URI( url ), suffix ));

		} catch( URISyntaxException e ) {
			Assert.fail( e.getMessage());
		}

		try {
			String url = "http://absolute-url.fr/folder";
			String suffix = "../readme.txt";
			Assert.assertEquals( new URI( "http://absolute-url.fr/readme.txt" ), UriUtils.buildNewURI( new URI( url ), suffix ));

		} catch( URISyntaxException e ) {
			Assert.fail( e.getMessage());
		}

		try {
			String url = "http://absolute-url.fr/folder";
			String suffix = "./readme.txt";
			Assert.assertEquals( new URI( "http://absolute-url.fr/folder/readme.txt" ), UriUtils.buildNewURI( new URI( url ), suffix ));

		} catch( URISyntaxException e ) {
			Assert.fail( e.getMessage());
		}

		try {
			String url = "http://absolute-url.fr/folder";
			String suffix = "f1/f2/f3/readme.txt";
			Assert.assertEquals( new URI( "http://absolute-url.fr/folder/f1/f2/f3/readme.txt" ), UriUtils.buildNewURI( new URI( url ), suffix ));

		} catch( URISyntaxException e ) {
			Assert.fail( e.getMessage());
		}
	}
}
