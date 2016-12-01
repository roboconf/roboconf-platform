/**
 * Copyright 2016 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.rest.services.swagger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.dm.rest.commons.beans.ApplicationBindings;
import net.roboconf.dm.rest.commons.beans.WebSocketMessage;
import net.roboconf.dm.rest.commons.json.JSonBindingUtils;
import net.roboconf.dm.rest.commons.json.MapWrapper;
import net.roboconf.dm.rest.commons.json.MappedCollectionWrapper;
import net.roboconf.dm.rest.commons.json.StringWrapper;
import net.roboconf.karaf.dist.dm.GeneratePreferencesFile;

/**
 * @author Vincent Zurczak - Linagora
 */
public class UpdateSwaggerJsonTest {

	@Test
	public void verifyProcessedClasses() throws Exception {

		UpdateSwaggerJson updater = new UpdateSwaggerJson();
		updater.prepareNewDefinitions();

		Set<Class<?>> classes = new HashSet<> ();
		classes.addAll( JSonBindingUtils.getSerializers().keySet());
		classes.removeAll( updater.processedClasses );

		// These classes are used within other ones.
		// No need to add them directly in the swagger.json file.
		classes.removeAll( Arrays.asList(
				StringWrapper.class,
				MapWrapper.class,
				MappedCollectionWrapper.class,
				ApplicationBindings.class,

				// FIXME: remove it from this list (see #732)
				WebSocketMessage.class
		));

		Assert.assertEquals( Collections.emptySet(), classes );
	}


	@Test( expected = RuntimeException.class )
	public void testGeneratedFile_invalidNumberOfArguments() throws Exception {

		new GeneratePreferencesFile().run( new String[] { "a", "b", "c" });
	}


	@Test( expected = RuntimeException.class )
	public void testGeneratedFile_invalidArguments() throws Exception {

		new GeneratePreferencesFile().run( new String[] { "invalid path" });
	}
}
