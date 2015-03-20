/**
 * Copyright 2013-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.monitoring.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Template;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.utils.Utils;
import org.fest.assertions.Condition;

/**
 * Utility methods dedicated to the testing of the Roboconf monitoring support.
 *
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class MonitoringTestUtils {

	// Prevent instantiations.
	private MonitoringTestUtils() {
	}

	/**
	 * Create a condition that matches {@code File}s with a content equal to the given value.
	 *
	 * @param expectedContent the expected content of the file.
	 * @return the created matcher.
	 */
	public static Condition<File> hasContent( final String expectedContent ) {
		return new Condition<File>( "file has content \"" + expectedContent + "\"" ) {
			@Override
			public boolean matches( File value ) {
				String actualContent;
				try {
					actualContent = Utils.readFileContent( value );
				} catch (final IOException e) {
					// Hum... Embarrassing!
					actualContent = null;
				}
				return Objects.equals( expectedContent, actualContent );
			}
		};
	}

	/**
	 * Add a template to the monitoring manager.
	 * <p>
	 * The template string content string is converted to an {@code InputStream} before being passed to the
	 * {@link net.roboconf.dm.monitoring.MonitoringService#addTemplate(Application, String, InputStream)} method.
	 * </p>
	 * @param monitoringManager the Roboconf monitoring manager being tested.
	 * @param application the application scope for the template to add, or {@code null} to add a global template.
	 * @param name        the name of the template to add.
	 * @param content     the string content of the template to add.
	 * @return {@code true} is the template was successfully added, {@code false} if there is already a template with
	 * the same name.
	 * 
	 * @throws IOException if the template cannot be added because of an IO error.
	 */
	public static boolean addStringTemplate( final MonitoringManager monitoringManager,
								   final Application application,
								   final String name,
								   final String content ) throws IOException {
		return monitoringManager.addTemplate(application, name, new ByteArrayInputStream( content.getBytes() ) );
	}

	/**
	 * Map the given instances by their path.
	 *
	 * @param instances the instances to map.
	 * @return the map of the given instances, indexed by path.
	 */
	public static Map<String, InstanceContextBean> instancesByPath( final Collection<InstanceContextBean> instances ) {
		final Map<String, InstanceContextBean> result = new LinkedHashMap<String, InstanceContextBean>();
		for (final InstanceContextBean instance : instances) {
			result.put( instance.getPath(), instance );
		}
		return result;
	}

	/**
	 * Transform the given set of variable contexts to a {@code string -> string} map.
	 * <p>The result of this method is left unspecified if the given set contains the same variable definition more
	 * than once.</p>
	 *
	 * @param variables the variables to map.
	 * @return the map with, for each provided variable, its name as key and its value as value.
	 */
	public static Map<String, String> variableMapOf( final Set<VariableContextBean> variables ) {
		final Map<String, String> result = new LinkedHashMap<String, String>();
		for (final VariableContextBean var : variables) {
			result.put( var.getName(), var.getValue() );
		}
		return result;
	}

	/**
	 * Process the given template, using the given monitoring context.
	 * <p>
	 * This method relies on the {@code MonitoringManager} and friends classes to process the template. So the result is
	 * exactly the same as the one that would be written in a monitoring report by the manager. 
	 * </p>
	 * 
	 * @param template the template to apply.
	 * @param context the monitoring context of an application.   
	 * @return the result of the template application to the given context.
	 */
	public static String processTemplate(final Template template, final ApplicationContextBean context) {
		// TODO
		return null;
	}

}
