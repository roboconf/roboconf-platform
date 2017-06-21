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

package net.roboconf.doc.generator;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.errors.RoboconfErrorHelpers;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.RuntimeModelIo;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.doc.generator.internal.nls.Messages;

/**
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractTestForRendererManager {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	protected File applicationDirectory;
	protected File outputDir;
	protected ApplicationLoadResult alr;
	protected RenderingManager rm;


	@Before
	public void before() throws Exception {

		this.applicationDirectory = TestUtils.findTestFile( "/lamp" );
		Assert.assertNotNull( this.applicationDirectory );
		Assert.assertTrue( this.applicationDirectory.exists());

		this.alr = RuntimeModelIo.loadApplication( this.applicationDirectory );
		Assert.assertFalse( RoboconfErrorHelpers.containsCriticalErrors( this.alr.getLoadErrors()));

		this.outputDir = this.folder.newFolder();
		this.rm = new RenderingManager();
	}


	protected void verifyContent( String content ) {

		Assert.assertFalse( content.contains( Messages.OOPS ));
		Assert.assertFalse( content.contains( "{0}" ));

		content = content.toLowerCase();
		Assert.assertFalse( content.contains( " null " ));
		Assert.assertFalse( content.contains( ">null " ));
		Assert.assertFalse( content.contains( " null<" ));
		Assert.assertFalse( content.contains( "*null " ));
		Assert.assertFalse( content.contains( " null*" ));
	}
}
