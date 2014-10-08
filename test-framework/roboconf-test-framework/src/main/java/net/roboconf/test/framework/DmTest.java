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

package net.roboconf.test.framework;

import java.util.List;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DmTest extends AbstractTest {

	@Override
	protected String getArtifactId() {
		return "roboconf-karaf-dist-dm";
	}

	@Override
	protected String getDirectorySuffix() {
		return "dm";
	}

	@Configuration
	public Option[] config() {
		int debugPort = -1;
		List<Option> options = getBaseOptions( debugPort );
		return options.toArray( new Option[ options.size()]);
	}
}
