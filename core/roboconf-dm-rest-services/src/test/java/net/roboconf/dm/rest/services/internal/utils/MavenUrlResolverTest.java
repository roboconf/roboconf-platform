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

package net.roboconf.dm.rest.services.internal.utils;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.ops4j.pax.url.mvn.MavenResolver;

import net.roboconf.core.urlresolvers.IUrlResolver.ResolvedFile;

/**
 * @author Vincent Zurczak - Linagora
 */
public class MavenUrlResolverTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testResolve_fileUrl() throws Exception {
		MavenUrlResolver mavenUrlResolver = new MavenUrlResolver( null );

		File f = this.folder.newFile();
		ResolvedFile resolvedFile = mavenUrlResolver.resolve( f.toURI().toURL().toString());
		Assert.assertEquals( f, resolvedFile.getFile());
		Assert.assertTrue( resolvedFile.existedBefore());
	}


	@Test
	public void testResolve_mavenUrl() throws Exception {

		MavenResolver mavenResolver = Mockito.mock( MavenResolver.class );
		MavenUrlResolver mavenUrlResolver = new MavenUrlResolver( mavenResolver );

		File f = new File( "test" );
		Mockito.when( mavenResolver.resolve( Mockito.anyString())).thenReturn( f );

		String url = "mvn:net.roboconf/my-app/1.0";
		ResolvedFile resolvedFile = mavenUrlResolver.resolve( url );
		Assert.assertEquals( f, resolvedFile.getFile());
		Assert.assertFalse( resolvedFile.existedBefore());

		Mockito.verify( mavenResolver ).resolve( url );
	}


	@Test( expected = IOException.class )
	public void testResolve_noMavenResolver() throws Exception {

		MavenUrlResolver mavenUrlResolver = new MavenUrlResolver( null );
		mavenUrlResolver.resolve( "mvn:net.roboconf/my-app/1.0" );
	}
}
