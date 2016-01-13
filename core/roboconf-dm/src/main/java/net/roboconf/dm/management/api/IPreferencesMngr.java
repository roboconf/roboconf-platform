/**
 * Copyright 2015-2016 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.management.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.roboconf.core.model.runtime.Preference;
import net.roboconf.core.model.runtime.Preference.PreferenceKeyCategory;

/**
 * An API to store and retrieve DM preferences.
 * @author Vincent Zurczak - Linagora
 */
public interface IPreferencesMngr {

	// javax.mail keys.
	String JAVAX_MAIL_FROM = "mail.from";
	String JAVAX_MAIL_SMTP_USER = "mail.user";
	String JAVAX_MAIL_SMTP_PWD = "mail.password";
	String JAVAX_MAIL_SMTP_AUTH = "mail.smtp.auth";
	String JAVAX_MAIL_SMTP_HOST = "mail.smtp.host";
	String JAVAX_MAIL_SMTP_PORT = "mail.smtp.port";
	String JAVAX_MAIL_START_SSL_ENABLE = "mail.smtp.starttls.enable";
	String JAVAX_MAIL_SSL_TRUST = "mail.smtp.ssl.trust";

	// General keys.
	// These properties are specific to Roboconf.
	String EMAIL_DEFAULT_RECIPIENTS = "email.default.recipients";
	String FORBIDDEN_RANDOM_PORTS = "forbidden.random.ports";


	/**
	 * Loads the properties.
	 * <p>
	 * This method is not invoked in the constructor.
	 * It must be explicitly be run by the DM when it starts, or by any client (e.g. in tests).
	 * </p>
	 */
	void loadProperties();


	/**
	 * Gets a value by key.
	 * @param key a non-null key
	 * @return a value, potentially null
	 */
	String get( String key );

	/**
	 * Gets a value by key.
	 * @param key a non-null key
	 * @param defaultValue a default value in case where the key was not found
	 * @return a value, or the default value if the key was not found
	 */
	String get( String key, String defaultValue );

	/**
	 * Saves a key and its value.
	 * @param key a non-null key
	 * @param value a non-null value
	 * @throws IOException if something went wrong
	 */
	void save( String key, String value ) throws IOException;

	/**
	 * Deletes a key.
	 * @param key a non-null key
	 * @return the deleted value
	 */
	String delete( String key );

	/**
	 * A convenience method to gather javax.mail properties.
	 * @return a non-null properties with all the javax.mail keys.
	 */
	Properties getJavaxMailProperties();

	/**
	 * @return all the preferences that are editable / public (never null)
	 */
	List<Preference> getAllPreferences();


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static final class Defaults {

		public final Map<String,PreferenceKeyCategory> keyToCategory = new HashMap<> ();
		public final Map<String,String> keyToDefaultValue = new HashMap<> ();

		/**
		 * Constructor.
		 */
		public Defaults() {

			// Define categories.
			// Some keys may have no category.
			this.keyToCategory.put( JAVAX_MAIL_FROM, PreferenceKeyCategory.EMAIL );
			this.keyToCategory.put( JAVAX_MAIL_SMTP_USER, PreferenceKeyCategory.EMAIL );
			this.keyToCategory.put( JAVAX_MAIL_SMTP_PWD, PreferenceKeyCategory.EMAIL );
			this.keyToCategory.put( JAVAX_MAIL_SMTP_AUTH, PreferenceKeyCategory.EMAIL );
			this.keyToCategory.put( JAVAX_MAIL_SMTP_HOST, PreferenceKeyCategory.EMAIL );
			this.keyToCategory.put( JAVAX_MAIL_SMTP_PORT, PreferenceKeyCategory.EMAIL );
			this.keyToCategory.put( JAVAX_MAIL_SSL_TRUST, PreferenceKeyCategory.EMAIL );
			this.keyToCategory.put( JAVAX_MAIL_START_SSL_ENABLE, PreferenceKeyCategory.EMAIL );
			this.keyToCategory.put( EMAIL_DEFAULT_RECIPIENTS, PreferenceKeyCategory.EMAIL );

			// Define default values
			this.keyToDefaultValue.put( JAVAX_MAIL_FROM, "dm@roboconf.net" );
			this.keyToDefaultValue.put( JAVAX_MAIL_SMTP_AUTH, "true" );
			this.keyToDefaultValue.put( JAVAX_MAIL_SMTP_HOST, "smtp.gmail.com" );
			this.keyToDefaultValue.put( JAVAX_MAIL_SMTP_PORT, "587" );
			this.keyToDefaultValue.put( JAVAX_MAIL_SSL_TRUST, "smtp.gmail.com" );
			this.keyToDefaultValue.put( JAVAX_MAIL_START_SSL_ENABLE, "true" );
		}
	}
}
