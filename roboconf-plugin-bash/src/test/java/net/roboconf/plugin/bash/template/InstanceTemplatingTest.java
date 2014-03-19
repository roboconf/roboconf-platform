/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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

package net.roboconf.plugin.bash.template;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.roboconf.core.model.runtime.Import;
import net.roboconf.core.model.runtime.Instance;

import org.junit.Assert;
import org.junit.Test;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class InstanceTemplatingTest {

	@Test
	public void testImportTemplate() throws IOException {
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("name1", "val1");
		vars.put("name2", "val2");
		vars.put("name3", "val3");
		Import impt = new Import("/", vars);
		
		MustacheFactory mf = new DefaultMustacheFactory();
	    Mustache mustache = mf.compile("net/roboconf/plugin/bash/template/importTemplate.mustache");
	    StringWriter writer = new StringWriter();
	    mustache.execute(writer, new ImportBean(impt)).flush();
	    
	    String writtenString = writer.toString();
	    for(String name : vars.keySet()) {
	    	Assert.assertTrue("Var was not displayed correctly", writtenString.contains(name+" : "+vars.get(name)));
	    }
	}
	
	@Test
	public void testInstanceTemplate() throws IOException {
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("name1", "val1");
		vars.put("name2", "val2");
		vars.put("name3", "val3");
		Import impt1 = new Import("/", vars);
		
		List<Import> imports = new ArrayList<Import>();
		imports.add(impt1);
		imports.add(impt1);
		
		Map<String, Collection<Import>> importsByPrefix = new HashMap<String, Collection<Import>>();
		importsByPrefix.put("prefix1", imports);
		importsByPrefix.put("prefix2", imports);
		
		Instance instance = new Instance("testInstance"); 
		instance.updateImports(importsByPrefix);
		
		//First test templating into a String
		String template = "net/roboconf/plugin/bash/template/instanceTemplate.mustache";
		StringWriter writer = new StringWriter();
		InstanceTemplateHelper.injectInstanceImports(instance, template, writer);
	    
	    String writtenString = writer.toString();
	    for(String prefix : importsByPrefix.keySet()) {
	    	Assert.assertTrue("Prefix was not displayed correctly", writtenString.contains("Prefix "+prefix));
	    }
	    for(String name : vars.keySet()) {
	    	Assert.assertTrue("Var was not displayed correctly", writtenString.contains(name+" -> "+vars.get(name)));
	    }
	    
	    //Test templating into a new file
	    File generated = File.createTempFile(instance.getName(), ".pipo");
        InstanceTemplateHelper.injectInstanceImports(instance, template, generated);
        Assert.assertTrue(generated.exists() && generated.isFile());
        Assert.assertEquals(readFile(generated.getAbsolutePath()), writtenString);
	}
	
	
	
	private String readFile(String fileName) throws IOException {
	    BufferedReader br = new BufferedReader(new FileReader(fileName));
	    try {
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();

	        while (line != null) {
	            sb.append(line);
	            sb.append("\n");
	            line = br.readLine();
	        }
	        return sb.toString();
	    } finally {
	        br.close();
	    }
	}

	
}
