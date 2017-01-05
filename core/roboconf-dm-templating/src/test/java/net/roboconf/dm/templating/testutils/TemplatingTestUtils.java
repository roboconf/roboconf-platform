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

package net.roboconf.dm.templating.testutils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.fest.assertions.Condition;

import net.roboconf.core.utils.Utils;
import net.roboconf.dm.templating.internal.contexts.InstanceContextBean;
import net.roboconf.dm.templating.internal.contexts.VariableContextBean;

/**
 * Utility methods dedicated to the testing of the Roboconf templating support.
 * @author Pierre Bourret - Université Joseph Fourier
 */
public final class TemplatingTestUtils {

	/**
	 * Constructor.
	 */
	private TemplatingTestUtils() {
		// nothing
	}


	/**
	 * Creates a condition that matches {@code File}s with a content equal to the given value.
	 * @param expectedContent the expected content of the file.
	 * @return the created matcher.
	 */
	public static Condition<File> hasContent( final String expectedContent ) {
		return new Condition<File>("file has content \"" + expectedContent + "\"") {
			@Override
			public boolean matches( File value ) {

				String actualContent;
				try {
					actualContent = Utils.readFileContent(value);

				} catch (final IOException e) {
					// Hum... Embarrassing!
					actualContent = null;
				}

				return Objects.equals(expectedContent, actualContent);
			}
		};
	}


	/**
	 * Maps the given instances by their path.
	 * @param instances the instances to map.
	 * @return the map of the given instances, indexed by path.
	 */
	public static Map<String, InstanceContextBean> instancesByPath( final Collection<InstanceContextBean> instances ) {

		final Map<String, InstanceContextBean> result = new LinkedHashMap<String, InstanceContextBean>();
		for (final InstanceContextBean instance : instances)
			result.put(instance.getPath(), instance);

		return result;
	}


	/**
	 * Transforms the given set of variable contexts to a {@code string -> string} map.
	 * <p>
	 * The result of this method is left unspecified if the given set contains the same variable definition more than
	 * once.
	 * </p>
	 *
	 * @param variables the variables to map.
	 * @return the map with, for each provided variable, its name as key and its value as value.
	 */
	public static Map<String, String> variableMapOf( final Set<VariableContextBean> variables ) {

		final Map<String, String> result = new LinkedHashMap<String, String>();
		for (final VariableContextBean var : variables)
			result.put(var.getName(), var.getValue());

		return result;
	}
}
