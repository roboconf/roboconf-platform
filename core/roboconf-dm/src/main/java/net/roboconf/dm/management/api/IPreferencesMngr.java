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

package net.roboconf.dm.management.api;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.roboconf.core.model.runtime.Preference;
import net.roboconf.core.model.runtime.Preference.PreferenceKeyCategory;
import net.roboconf.dm.internal.api.impl.RandomMngrImpl;

/**
 * An API to store and retrieve DM preferences.
 * @author Vincent Zurczak - Linagora
 */
public interface IPreferencesMngr {

	// javax.mail keys.

	@PreferenceDescription( desc = "The e-mail address of the sender.\nThis is a javax.mail property." )
	String JAVAX_MAIL_FROM = "mail.from";

	@PreferenceDescription( desc = "The user name for the SMTP server.\nThis is a javax.mail property." )
	String JAVAX_MAIL_SMTP_USER = "mail.user";

	@PreferenceDescription( desc = "The password for the SMTP server.\nThis is a javax.mail property." )
	String JAVAX_MAIL_SMTP_PWD = "mail.password";

	@PreferenceDescription( desc = "Whether authentication is required or not.\nThis is a javax.mail property.", values={ "true", "false" })
	String JAVAX_MAIL_SMTP_AUTH = "mail.smtp.auth";

	@PreferenceDescription( desc = "The host name or IP address of the SMTP server.\nThis is a javax.mail property." )
	String JAVAX_MAIL_SMTP_HOST = "mail.smtp.host";

	@PreferenceDescription( desc = "The port used to reach the SMTP server.\nThis is a javax.mail property." )
	String JAVAX_MAIL_SMTP_PORT = "mail.smtp.port";

	@PreferenceDescription( desc = "Whether TLS is enabled or not.\nThis is a javax.mail property.", values={ "true", "false" })
	String JAVAX_MAIL_START_SSL_ENABLE = "mail.smtp.starttls.enable";

	@PreferenceDescription( desc = "A list of trusted host names or IP addresses for secured connections.\nThis is a javax.mail property." )
	String JAVAX_MAIL_SSL_TRUST = "mail.smtp.ssl.trust";

	// General keys.
	// These properties are specific to Roboconf.

	/**
	 * Default email recipients.
	 */
	@PreferenceDescription(
			desc =
			"A list of default recipients that will receive all the e-mails sent by Roboconf.\n"
			+ "These recipients come in addition to those configured somewhere else\n"
			+ "(e.g. within a command or through the autonomic)."
	)
	String EMAIL_DEFAULT_RECIPIENTS = "email.default.recipients";

	/**
	 * The ports that must be excluded from random port generation.
	 */
	@PreferenceDescription(
			desc =
			"Instead of forcing a given port, it is possible to let Roboconf pick up one randomly.\n"
			+ "Random ports are selected between " + RandomMngrImpl.PORT_MIN + " and " + RandomMngrImpl.PORT_MAX + ".\n"
			+ "This preference allows to exclude one or several ports from being chosen by Roboconf.\n"
			+ "Ports must be separated by a comma. Example: 11000, 11001, 11002"
	)
	String FORBIDDEN_RANDOM_PORTS = "forbidden.random.ports";

	/**
	 * The maximum number of VM the autonomic can create.
	 */
	@PreferenceDescription(
			desc =
			"The maximum number of VMs the autonomic can create.\nThis is a global maximum, the count is effctive for all the applications."
	)
	String AUTONOMIC_MAX_VM_NUMBER = "autonomic.maximum.vm.number";

	/**
	 * A boolean value indicating if the maximum number of VM must be strict or not.
	 * <p>
	 * When a given event is processed, one or several command scripts can be executed.
	 * If the maximum is reached before the execution starts, then it is dropped. Otherwise,
	 * it may happen that the scripts create several VMs. If this preference is set to true,
	 * then the script execution will be interrupted. Otherwise, it will continue, with the side
	 * effect that the autonomic may create more VM than the maximum.
	 * </p>
	 * <p>
	 * Said differently, when set to false, this property makes the maximum an ideal barrier.
	 * It thus prevents started scripts from interrupting, even if the maximum was reached during
	 * their execution.
	 * </p>
	 */
	@PreferenceDescription(
			values={ "true", "false" },
			desc =
			"Whether the maximum number of VMs is strict or if it could support some outbounds.\n"
			+ "Given all the concurrency in Roboconf, it may happen several autonomic VMs are created at once and\n"
			+ "may result in the maximum being a little overheaded.\n\n"
			+ "Set it to \"true\" to be strict. Setting it to \"false\" may result in the maximum being never reached."
	)
	String AUTONOMIC_STRICT_MAX_VM_NUMBER = "autonomic.strict.maximum.vm.number";

	/**
	 * The user language (example: the web console).
	 */
	@PreferenceDescription(
			values={ "EN (for English)", "FR (for French)" },
			desc = "The user language (e.g. for the web console)."
	)
	String USER_LANGUAGE = "user.language";

	/**
	 * List of web extensions (automatically fetched by the web console).
	 */
	@PreferenceDescription(
			desc = "List of web extensions (automatically fetched by the web console).\n"
			+ "This preference is generally populated by bundles and directly from the code. "
			+ "The web console only loads extensions located on the DM's web server. So, one could "
			+ "save (e.g.) 'http://google.com' in this preference. But the web console would not embed its content."
	)
	String WEB_EXTENSIONS = "web.extensions";



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
	 * <p>
	 * This method updates the working cache and the configuration files.
	 * </p>
	 *
	 * @param key a non-null key
	 * @param value a non-null value
	 * @throws IOException if something went wrong
	 */
	void save( String key, String value ) throws IOException;

	/**
	 * Adds a value to a list preference.
	 * <p>
	 * List preferences are considered to have their values separated by a comma.
	 * Values should thus not contain a comma.
	 * </p>
	 *
	 * @param key a non-null key
	 * @param value a value to add to a list value
	 * @throws IOException if something went wrong
	 */
	void addToList( String key, String value ) throws IOException;

	/**
	 * Removes a value from a list preference.
	 * <p>
	 * List preferences are considered to have their values separated by a comma.
	 * Values should thus not contain a comma.
	 * </p>
	 *
	 * @param key a non-null key
	 * @param value a value to remove from a list value
	 * @throws IOException if something went wrong
	 */
	void removeFromList( String key, String value ) throws IOException;

	/**
	 * Gets the preferences for a given key as a list.
	 * <p>
	 * List preferences are considered to have their values separated by a comma.
	 * Values should thus not contain a comma.
	 * </p>
	 *
	 * @param key a non-null key
	 * @return a non-null collection
	 */
	Collection<String> getAsCollection( String key );

	/**
	 * Update several properties at once.
	 * <p>
	 * Unlike {@link #save(String, String)}, this method only updates the working cache.
	 * Modifications are not propagated to configuration files.
	 * </p>
	 *
	 * @param properties the properties
	 */
	void updateProperties( Dictionary<?,?> properties );

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

			this.keyToCategory.put( AUTONOMIC_MAX_VM_NUMBER, PreferenceKeyCategory.AUTONOMIC );
			this.keyToCategory.put( AUTONOMIC_STRICT_MAX_VM_NUMBER, PreferenceKeyCategory.AUTONOMIC );

			this.keyToCategory.put( USER_LANGUAGE, PreferenceKeyCategory.WEB );
			this.keyToCategory.put( WEB_EXTENSIONS, PreferenceKeyCategory.WEB );
			this.keyToCategory.put( FORBIDDEN_RANDOM_PORTS, PreferenceKeyCategory.MISCELLANEOUS );

			// Define default values
			this.keyToDefaultValue.put( JAVAX_MAIL_FROM, "dm@roboconf.net" );
			this.keyToDefaultValue.put( JAVAX_MAIL_SMTP_AUTH, "true" );
			this.keyToDefaultValue.put( JAVAX_MAIL_SMTP_HOST, "smtp.gmail.com" );
			this.keyToDefaultValue.put( JAVAX_MAIL_SMTP_PORT, "587" );
			this.keyToDefaultValue.put( JAVAX_MAIL_SSL_TRUST, "smtp.gmail.com" );
			this.keyToDefaultValue.put( JAVAX_MAIL_START_SSL_ENABLE, "true" );
			this.keyToDefaultValue.put( USER_LANGUAGE, "EN" );
		}
	}


	/**
	 * An annotation that helps to generate the documentation for the "net.roboconf.dm.preferences" file.
	 * @author Vincent Zurczak - Linagora
	 */
	@Retention( RetentionPolicy.RUNTIME )
	@Target( ElementType.FIELD )
	public static @interface PreferenceDescription {

		String desc() default "";
		String[] values() default {};
	}
}
