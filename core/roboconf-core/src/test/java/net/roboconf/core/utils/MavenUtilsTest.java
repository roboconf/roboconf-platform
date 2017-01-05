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

import org.junit.Assert;

import org.junit.Ignore;
import org.junit.Test;

/**
 * These tests are meant to be run by hand, for debug purpose.
 * <p>
 * By default, they are ignored by the build.
 * </p>
 * @author Vincent Zurczak - Linagora
 */
@Ignore
public class MavenUtilsTest {

	@Test
	public void testLocalRepository() throws Exception {
		String url = MavenUtils.findMavenUrlForRoboconf( "roboconf-core", "0.4-SNAPSHOT" );
		System.out.println( "Local repository => " + url );
		Assert.assertNotNull( url );
		Assert.assertTrue( url.startsWith( "file:/" ));
	}


	@Test
	public void testLocalRepository_caseSnapshot() throws Exception {
		String url = MavenUtils.findMavenUrlForRoboconf( "roboconf-core", "0.4-SNAPshot" );
		System.out.println( "Local repository => " + url );
		Assert.assertNotNull( url );
		Assert.assertTrue( url.startsWith( "file:/" ));
	}


	@Test
	public void testSonatypeSnapshot() throws Exception {
		String url = MavenUtils.findMavenUrlForRoboconf( "roboconf-core", "0.5-SNAPSHOT" );
		System.out.println( "Sonatype snapshot => " + url );
		Assert.assertNotNull( url );
		Assert.assertTrue( url.startsWith( "https://oss.sonatype.org" ));
		Assert.assertTrue( url.contains( "/snapshots/" ));
	}


	@Test
	public void testSonatypeRelease() throws Exception {
		String url = MavenUtils.findMavenUrlForRoboconf( "roboconf-core", "0.4" );
		System.out.println( "Sonatype release => " + url );
		Assert.assertNotNull( url );
		Assert.assertTrue( url.startsWith( "https://oss.sonatype.org" ));
		Assert.assertTrue( url.contains( "/releases/" ));
	}
}
