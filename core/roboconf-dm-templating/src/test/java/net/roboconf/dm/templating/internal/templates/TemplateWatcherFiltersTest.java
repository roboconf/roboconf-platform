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

package net.roboconf.dm.templating.internal.templates;

import java.io.File;

import org.junit.Assert;
import net.roboconf.dm.templating.internal.templates.TemplateWatcher.TemplateDirectoryFileFilter;
import net.roboconf.dm.templating.internal.templates.TemplateWatcher.TemplateFileFilter;
import net.roboconf.dm.templating.internal.templates.TemplateWatcher.TemplateSubDirectoryFileFilter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TemplateWatcherFiltersTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testTemplateDirectoryFileFilter() throws Exception {

		File dir = this.folder.newFolder();
		TemplateDirectoryFileFilter filter = new TemplateDirectoryFileFilter( dir );

		Assert.assertTrue( filter.accept( dir ));
		Assert.assertTrue( filter.accept( new File( dir, "child" )));

		Assert.assertFalse( filter.accept( new File( dir, "child/another/child" )));
		Assert.assertFalse( filter.accept( dir.getParentFile()));
	}


	@Test
	public void testTemplateFileFilter() throws Exception {

		File dir = this.folder.newFolder();
		TemplateFileFilter filter = new TemplateFileFilter( dir );

		Assert.assertFalse( filter.accept( dir ));
		Assert.assertFalse( filter.accept( new File( dir, "child/another/child" )));
		Assert.assertFalse( filter.accept( dir.getParentFile()));

		Assert.assertTrue( filter.accept( new File( dir, "child" )));
		Assert.assertTrue( filter.accept( new File( dir, "dir/child" )));
	}


	@Test
	public void testTemplateSubDirectoryFileFilter() throws Exception {

		File dir = this.folder.newFolder();
		TemplateSubDirectoryFileFilter filter = new TemplateSubDirectoryFileFilter( dir );

		Assert.assertTrue( filter.accept( new File( dir, "child" )));

		Assert.assertFalse( filter.accept( new File( dir, "child/another/child" )));
		Assert.assertFalse( filter.accept( dir ));
		Assert.assertFalse( filter.accept( dir.getParentFile()));
	}
}
