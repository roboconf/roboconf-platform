/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.plugin.script.internal.templating;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ImportHelpers;
import net.roboconf.core.utils.Utils;

/**
 * @author Graham Crosmarie - Linagora
 */
public class InstanceTemplatingTest {

	@Test
	public void testImportTemplate() throws Exception {

		Map<String, String> vars = new HashMap<>();
		vars.put("name1", "val1");
		vars.put("name2", "val2");
		vars.put("name3", "val3");
		Import impt = new Import( "/", "component1", vars );

		MustacheFactory mf = new DefaultMustacheFactory();
		File templateFile = TestUtils.findTestFile( "/importTemplate.mustache" );
		Mustache mustache = mf.compile( templateFile.getAbsolutePath());
		StringWriter writer = new StringWriter();
		mustache.execute(writer, new ImportBean(impt)).flush();

		String writtenString = writer.toString();
		for( Map.Entry<String,String> entry : vars.entrySet()) {
			Assert.assertTrue("Var was not displayed correctly", writtenString.contains( entry.getKey() + " : " + entry.getValue()));
		}
	}

	@Test
	public void testInstanceTemplate() throws Exception {

		Map<String, String> vars = new HashMap<>();
		vars.put("name1", "val1");
		vars.put("name2", "val2");
		vars.put("name3", "val3");
		Import impt1 = new Import( "/", "component1", vars );

		List<Import> imports = new ArrayList<>();
		imports.add(impt1);
		imports.add(impt1);

		Map<String, Collection<Import>> importsByPrefix = new HashMap<>();
		importsByPrefix.put("prefix1", imports);
		importsByPrefix.put("prefix2", imports);

		Instance instance = new Instance("testInstance");
		ImportHelpers.updateImports( instance, importsByPrefix );

		// First test templating into a String
		File templateFile = TestUtils.findTestFile( "/instanceTemplate.mustache" );
		StringWriter writer = new StringWriter();
		InstanceTemplateHelper.injectInstanceImports(instance, templateFile, writer);

		String writtenString = writer.toString();
		for(String prefix : importsByPrefix.keySet()) {
			Assert.assertTrue("Prefix was not displayed correctly", writtenString.contains("Prefix "+prefix));
		}

		for( Map.Entry<String,String> entry : vars.entrySet()) {
			Assert.assertTrue("Var was not displayed correctly", writtenString.contains( entry.getKey() + " -> " + entry.getValue()));
		}

		// Test templating into a new file
		File generated = File.createTempFile(instance.getName(), ".pipo");
		InstanceTemplateHelper.injectInstanceImports(instance, templateFile, generated);
		Assert.assertTrue(generated.exists() && generated.isFile());
		Assert.assertEquals( Utils.readFileContent( generated ), writtenString);
	}
}
