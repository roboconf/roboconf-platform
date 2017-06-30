/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.errors.i18n;

import static net.roboconf.core.errors.i18n.TranslationBundle.DETAILS_SEPARATOR;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.ErrorDetails.ErrorDetailsKind;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TranslationBundleTest {

	@Test
	public void testKeys() {

		for( String lang : TranslationBundle.LANGUAGES ) {
			ResourceBundle rb = new TranslationBundle( lang );

			// Error codes
			for( ErrorCode ec : ErrorCode.values()) {
				try {
					String s = rb.getString( ec.name());
					Assert.assertNotNull( lang, s );

				} catch( MissingResourceException e ) {
					Assert.fail( lang + " => " + ec.name());
				}
			}

			// Error details
			for( ErrorDetailsKind detailsKind : ErrorDetailsKind.values()) {
				try {
					String s = rb.getString( detailsKind.name());
					Assert.assertNotNull( lang, s );

				} catch( MissingResourceException e ) {
					Assert.fail( lang + " => " + detailsKind.name());
				}
			}

			// Details separator
			try {
				String s = rb.getString( DETAILS_SEPARATOR );
				Assert.assertNotNull( lang, s );

			} catch( MissingResourceException e ) {
				Assert.fail( lang + " => " + DETAILS_SEPARATOR );
			}

			// Right number of elements
			Assert.assertEquals(
					lang,
					ErrorDetailsKind.values().length + ErrorCode.values().length + 1,
					((TranslationBundle) rb).getContents().length );
		}
	}


	@Test
	public void testLoadContent_invalidLang() {

		Map<String,String> map = TranslationBundle.loadContent( "fish" );
		Assert.assertEquals( 0, map.size());
	}


	@Test
	public void testConstructor() {

		TranslationBundle tb = new TranslationBundle( "martian" );
		Assert.assertEquals( TranslationBundle.EN, tb.getLang());
	}


	@Test
	public void testSameNumberOfPropertiesInTranslations() throws Exception {

		Map<String,Integer> keyToCurlyBracketsCount = new HashMap<> ();
		for( String lang : TranslationBundle.LANGUAGES ) {
			URL url = TranslationBundle.class.getResource( "/" + lang + ".json" );
			File f = new File( url.toURI());

			String content = Utils.readFileContent( f );
			Matcher m = Pattern.compile( TranslationBundle.ROW_PATTERN ).matcher( content );
			while( m.find()) {

				// Basic coherence among translation files
				String s = m.group( 2 );
				int ocb = s.length() - s.replace( "{", "").length();
				int ccb = s.length() - s.replace( "}", "").length();
				Assert.assertEquals( "Not the same number of { and } in " + m.group( 1 ) + " (" + lang + ")", ocb, ccb );

				Integer expectedValue = keyToCurlyBracketsCount.get( m.group( 1 ));
				if( expectedValue != null )
					Assert.assertEquals( "Not the expected number of properties in " + m.group( 1 ) + " (" + lang + ")", expectedValue.intValue(), ocb );
				else
					keyToCurlyBracketsCount.put( m.group( 1 ), ocb );

				// Coherence with Java types
				try {
					ErrorCode ec = ErrorCode.valueOf( m.group( 1 ));
					Assert.assertEquals( "Not the right number of properties",  ec.getI18nProperties().length, ocb );

				} catch( IllegalArgumentException e ) {
					// nothing
				}
			}
		}
	}
}
