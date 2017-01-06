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

import static net.roboconf.core.utils.ManifestUtils.BUNDLE_VERSION;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManifestUtilsTest {

	@Test
	public void testFindManifestProperty() {

		Properties props = new Properties();
		Assert.assertNull( ManifestUtils.findManifestProperty( props, BUNDLE_VERSION ));

		String version = "1.2";
		props.put( BUNDLE_VERSION, version );
		Assert.assertEquals( version, ManifestUtils.findManifestProperty( props, BUNDLE_VERSION ));
	}


	@Test
	public void testStuffThatOnlyWorksInMavenOrOsgi() {

		// We keep this test method for code coverage...
		Assert.assertNull( ManifestUtils.findBundleVersion());
		Assert.assertNull( ManifestUtils.findManifestProperty( BUNDLE_VERSION ));
	}


	@Test
	public void testFindMavenVersion() {

		Assert.assertEquals( "0.4", ManifestUtils.findMavenVersion( "0.4" ));
		Assert.assertEquals( "0.4", ManifestUtils.findMavenVersion( "0.4.0" ));
		Assert.assertEquals( "0.4.1", ManifestUtils.findMavenVersion( "0.4.1" ));
		Assert.assertEquals( "0.4-SNAPSHOT", ManifestUtils.findMavenVersion( "0.4.0-SNAPSHOT" ));
		Assert.assertEquals( "0.4.1-SNAPSHOT", ManifestUtils.findMavenVersion( "0.4.1-SNAPSHOT" ));

		Assert.assertEquals( "0.4-SNAPSHOT", ManifestUtils.findMavenVersion( "0.4.SNAPSHOT" ));
		Assert.assertEquals( "0.4-SNAPSHOT", ManifestUtils.findMavenVersion( "0.4.0.SNAPSHOT" ));
		Assert.assertEquals( "0.4.1-SNAPSHOT", ManifestUtils.findMavenVersion( "0.4.1.SNAPSHOT" ));

		Assert.assertEquals( "12.52", ManifestUtils.findMavenVersion( "12.52.0" ));
		Assert.assertEquals( "12.52-SNAPSHOT", ManifestUtils.findMavenVersion( "12.52.0-SNAPSHOT" ));
		Assert.assertEquals( "12.52.1-SNAPSHOT", ManifestUtils.findMavenVersion( "12.52.1-SNAPSHOT" ));
		Assert.assertEquals( "12.52.1-SNAPSHOT", ManifestUtils.findMavenVersion( "12.52.1-snapshoT" ));

		Assert.assertEquals( "0.4.0.1", ManifestUtils.findMavenVersion( "0.4.0.1" ));
		Assert.assertEquals( "whatever", ManifestUtils.findMavenVersion( "whatever" ));
		Assert.assertEquals( "", ManifestUtils.findMavenVersion( "" ));
		Assert.assertNull( ManifestUtils.findMavenVersion( null ));
	}
}
