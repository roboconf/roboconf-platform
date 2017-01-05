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

package net.roboconf.doc.generator.internal.nls;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class Messages {

	public static final String OOPS = "!oops!"; //$NON-NLS-1$
	private static final String BUNDLE_NAME = "net.roboconf.doc.generator.internal.nls.messages"; //$NON-NLS-1$

	private final ResourceBundle bundle;


	/**
	 * Empty constructor (with the system's locale).
	 */
	public Messages() {
		this.bundle = ResourceBundle.getBundle( BUNDLE_NAME );
	}


	/**
	 * Constructor.
	 * @param locale the locale (e.g. en_US, fr_FR)
	 */
	public Messages( String locale ) {
		String ietfLocale = locale.replace( '_', '-' );
		Locale loc = Locale.forLanguageTag( ietfLocale );
		this.bundle = ResourceBundle.getBundle( BUNDLE_NAME, loc );
	}


	/**
	 * Finds an internationalized string by key.
	 * @param key a non-null key
	 * @return a string (never null)
	 */
	public String get( String key ) {

		String result;
		try {
			result = this.bundle.getString( key );

		} catch( MissingResourceException e ) {
			result = OOPS + key + '!';
		}

		return result;
	}
}
