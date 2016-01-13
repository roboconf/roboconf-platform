/**
 * Copyright 2014-2016 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.rest.commons.json;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ExportedVariable;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.TargetAssociation;
import net.roboconf.core.model.runtime.TargetUsageItem;
import net.roboconf.core.model.runtime.TargetWrapperDescriptor;
import net.roboconf.dm.rest.commons.Diagnostic;
import net.roboconf.dm.rest.commons.Diagnostic.DependencyInformation;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Vincent Zurczak - Linagora
 */
public class JSonBindingUtilsTest {

	@Test
	public void testApplicationTemplateBinding_1() throws Exception {

		final String result = "{\"desc\":\"some text\",\"qualifier\":\"v1\",\"apps\":[]}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		ApplicationTemplate app = new ApplicationTemplate().description( "some text" ).qualifier( "v1" );
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		ApplicationTemplate readApp = mapper.readValue( result, ApplicationTemplate.class );
		Assert.assertEquals( app, readApp );
		Assert.assertEquals( app.getName(), readApp.getName());
		Assert.assertEquals( app.getDescription(), readApp.getDescription());
		Assert.assertEquals( app.getQualifier(), readApp.getQualifier());
		Assert.assertEquals( app.getExternalExportsPrefix(), readApp.getExternalExportsPrefix());
	}


	@Test
	public void testApplicationTemplateBinding_2() throws Exception {

		final String result = "{\"name\":\"my application\",\"qualifier\":\"v1-17.snapshot\",\"apps\":[\"a1\",\"a2\"]}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		ApplicationTemplate app = new ApplicationTemplate( "my application" ).qualifier( "v1-17.snapshot" );
		new Application( "a1", app );
		new Application( "a2", app );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		ApplicationTemplate readApp = mapper.readValue( result, ApplicationTemplate.class );
		Assert.assertEquals( app, readApp );
		Assert.assertEquals( app.getName(), readApp.getName());
		Assert.assertEquals( app.getDescription(), readApp.getDescription());
		Assert.assertEquals( app.getQualifier(), readApp.getQualifier());
		Assert.assertEquals( app.getExternalExportsPrefix(), readApp.getExternalExportsPrefix());
	}


	@Test
	public void testApplicationTemplateBinding_3() throws Exception {

		final String result = "null";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, null );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		ApplicationTemplate readApp = mapper.readValue( result, ApplicationTemplate.class );
		Assert.assertNull( readApp );
	}


	@Test
	public void testApplicationTemplateBinding_4() throws Exception {

		final String result = "{\"name\":\"my application\",\"apps\":[]}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		ApplicationTemplate app = new ApplicationTemplate( "my application" );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		ApplicationTemplate readApp = mapper.readValue( result, ApplicationTemplate.class );
		Assert.assertEquals( app, readApp );
		Assert.assertEquals( app.getName(), readApp.getName());
		Assert.assertEquals( app.getDescription(), readApp.getDescription());
		Assert.assertEquals( app.getQualifier(), readApp.getQualifier());
		Assert.assertEquals( app.getExternalExportsPrefix(), readApp.getExternalExportsPrefix());
	}


	@Test
	public void testApplicationTemplateBinding_5() throws Exception {

		ApplicationTemplate app = new ApplicationTemplate( "my application" );
		app.setExternalExportsPrefix( "test" );

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();

		ApplicationTemplate readApp = mapper.readValue( s, ApplicationTemplate.class );
		Assert.assertEquals( app, readApp );
		Assert.assertEquals( app.getName(), readApp.getName());
		Assert.assertEquals( app.getDescription(), readApp.getDescription());
		Assert.assertEquals( app.getQualifier(), readApp.getQualifier());
		Assert.assertEquals( app.getExternalExportsPrefix(), readApp.getExternalExportsPrefix());
	}


	@Test
	public void testApplicationTemplateBinding_6() throws Exception {

		ApplicationTemplate app = new ApplicationTemplate( "my application" );
		app.externalExports.put( "k1", "v1" );
		app.externalExports.put( "k2", "v2" );

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();

		Assert.assertEquals( "{\"name\":\"my application\",\"extVars\":{\"k1\":\"v1\",\"k2\":\"v2\"},\"apps\":[]}", s );
	}


	@Test
	public void testApplicationTemplateBinding_7() throws Exception {

		// Initial binding
		ApplicationTemplate tpl = new ApplicationTemplate( "my tpl" );
		Application app = new Application( "app", tpl );

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, tpl );

		Assert.assertEquals( "{\"name\":\"my tpl\",\"apps\":[\"app\"]}", writer.toString());

		// After we remove the association with the application
		app.removeAssociationWithTemplate();

		writer = new StringWriter();
		mapper.writeValue( writer, tpl );

		Assert.assertEquals( "{\"name\":\"my tpl\",\"apps\":[]}", writer.toString());
	}


	@Test
	public void testApplicationBinding_1() throws Exception {

		final String result = "{\"name\":\"app1\",\"desc\":\"some text\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Application app = new Application( "app1", null ).description( "some text" );
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Application readApp = mapper.readValue( result, Application.class );
		Assert.assertEquals( app, readApp );
		Assert.assertEquals( app.getName(), readApp.getName());
		Assert.assertEquals( app.getDescription(), readApp.getDescription());
	}


	@Test
	public void testApplicationBinding_2() throws Exception {

		final String result = "{\"tplName\":\"oops\",\"tplQualifier\":\"hello!\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		ApplicationTemplate tpl = new ApplicationTemplate( "oops" ).qualifier( "hello!" );
		Application app = new Application( tpl );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Application readApp = mapper.readValue( result, Application.class );
		Assert.assertEquals( app, readApp );
		Assert.assertEquals( app.getName(), readApp.getName());
		Assert.assertEquals( app.getDescription(), readApp.getDescription());
		Assert.assertEquals( app.getTemplate(), readApp.getTemplate());
	}


	@Test
	public void testApplicationBinding_3() throws Exception {

		final String result = "null";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, null );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Application readApp = mapper.readValue( result, Application.class );
		Assert.assertNull( readApp );
	}


	@Test
	public void testApplicationBinding_4() throws Exception {

		Application app = new Application( "test", null );
		app.getRootInstances().add( new Instance( "r" ).status( InstanceStatus.PROBLEM ));

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );

		Assert.assertEquals( "{\"name\":\"test\",\"info\":\"warn\"}", writer.toString());
	}


	@Test
	public void testApplicationBinding_5() throws Exception {

		Application app = new Application( "test", null ).description( "hi!" );
		app.getRootInstances().add( new Instance( "r" ).status( InstanceStatus.DEPLOYED_STARTED ));

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );

		Assert.assertEquals( "{\"name\":\"test\",\"desc\":\"hi!\",\"info\":\"ok\"}", writer.toString());
	}


	@Test
	public void testApplicationBinding_6() throws Exception {

		final String result = "{\"name\":\"app1\",\"tplName\":\"oops\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		ApplicationTemplate tpl = new ApplicationTemplate( "oops" );
		Application app = new Application( "app1", tpl );
		app.getRootInstances().add( new Instance( "r" ));

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Application readApp = mapper.readValue( result, Application.class );
		Assert.assertEquals( app, readApp );
		Assert.assertEquals( app.getName(), readApp.getName());
		Assert.assertEquals( app.getDescription(), readApp.getDescription());
		Assert.assertEquals( app.getTemplate(), readApp.getTemplate());
		Assert.assertEquals( app.getTemplate().getExternalExportsPrefix(), readApp.getTemplate().getExternalExportsPrefix());
	}


	@Test
	public void testApplicationBinding_7() throws Exception {

		final String result = "{\"name\":\"app1\",\"tplName\":\"\",\"tplQualifier\":\"oops\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		ApplicationTemplate tpl = new ApplicationTemplate( "" ).qualifier( "oops" );
		Application app = new Application( "app1", tpl );
		app.getRootInstances().add( new Instance( "r" ));

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Application readApp = mapper.readValue( result, Application.class );
		Assert.assertEquals( app, readApp );
		Assert.assertEquals( app.getName(), readApp.getName());
		Assert.assertEquals( app.getDescription(), readApp.getDescription());
		Assert.assertEquals( app.getTemplate(), readApp.getTemplate());
		Assert.assertEquals( app.getTemplate().getExternalExportsPrefix(), readApp.getTemplate().getExternalExportsPrefix());
	}


	@Test
	public void testApplicationBinding_8() throws Exception {

		ApplicationTemplate tpl = new ApplicationTemplate( "" ).qualifier( "oops" );
		tpl.externalExports.put( "k1", "v1" );
		tpl.externalExports.put( "k2", "v2" );

		Application app = new Application( "app1", tpl );
		app.getRootInstances().add( new Instance( "r" ));

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();

		Assert.assertEquals( "{\"name\":\"app1\",\"tplName\":\"\",\"tplQualifier\":\"oops\",\"extVars\":{\"k1\":\"v1\",\"k2\":\"v2\"}}", s );
	}


	@Test
	public void testApplicationBinding_9() throws Exception {

		ApplicationTemplate tpl = new ApplicationTemplate( "" ).qualifier( "oops" );
		tpl.setExternalExportsPrefix( "toto" );

		Application app = new Application( "app1", tpl );
		app.getRootInstances().add( new Instance( "r" ));

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();

		Assert.assertEquals( "{\"name\":\"app1\",\"tplName\":\"\",\"tplQualifier\":\"oops\",\"tplEep\":\"toto\"}", s );

		Application readApp = mapper.readValue( s, Application.class );
		Assert.assertEquals( app, readApp );
		Assert.assertEquals( app.getName(), readApp.getName());
		Assert.assertEquals( app.getDescription(), readApp.getDescription());
		Assert.assertEquals( app.getTemplate(), readApp.getTemplate());
		Assert.assertEquals( app.getTemplate().getExternalExportsPrefix(), readApp.getTemplate().getExternalExportsPrefix());
	}


	@Test
	public void testComponentBinding_1() throws Exception {

		final String result = "{\"name\":\"component 1\",\"installer\":\"target\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Component comp = new Component( "component 1" ).installerName( "target" );
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, comp );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Component readComp = mapper.readValue( result, Component.class );
		Assert.assertEquals( comp, readComp );
		Assert.assertEquals( comp.getName(), readComp.getName());
		Assert.assertEquals( comp.getInstallerName(), readComp.getInstallerName());
	}


	@Test
	public void testComponentBinding_2() throws Exception {

		final String result = "{}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Component comp = new Component();
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, comp );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Component readComp = mapper.readValue( result, Component.class );
		Assert.assertEquals( comp, readComp );
		Assert.assertEquals( comp.getName(), readComp.getName());
		Assert.assertEquals( comp.getInstallerName(), readComp.getInstallerName());
	}


	@Test
	public void testInstanceBinding_1() throws Exception {

		final String result = "{\"name\":\"instance\",\"path\":\"/instance\",\"status\":\"NOT_DEPLOYED\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Instance inst = new Instance( "instance" );
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, inst );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Instance readInst = mapper.readValue( result, Instance.class );
		Assert.assertEquals( inst, readInst );
		Assert.assertEquals( inst.getName(), readInst.getName());
		Assert.assertEquals( inst.getStatus(), readInst.getStatus());
	}


	@Test
	public void testInstanceBinding_2() throws Exception {

		final String result = "{\"name\":\"server\",\"path\":\"/server\",\"status\":\"STARTING\",\"channels\":[\"channel4\",\"hop\"]}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Instance inst = new Instance( "server" ).channel( "channel4" ).channel( "hop" ).status( InstanceStatus.STARTING );
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, inst );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Instance readInst = mapper.readValue( result, Instance.class );
		Assert.assertEquals( inst, readInst );
		Assert.assertEquals( inst.getName(), readInst.getName());
		Assert.assertEquals( inst.getStatus(), readInst.getStatus());
		Assert.assertEquals( inst.channels, readInst.channels );
	}


	@Test
	public void testInstanceBinding_3() throws Exception {

		final String result = "{\"name\":\"server\",\"path\":\"/vm/server\",\"status\":\"STARTING\",\"component\":{\"name\":\"server-component\"}}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Instance inst = new Instance( "server" ).status( InstanceStatus.STARTING ).component( new Component( "server-component" ));
		Instance parentInst = new Instance( "vm" );
		InstanceHelpers.insertChild( parentInst, inst );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, inst );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Instance readInst = mapper.readValue( result, Instance.class );
		Assert.assertEquals( inst.getName(), readInst.getName());
		Assert.assertEquals( inst.getStatus(), readInst.getStatus());
		Assert.assertEquals( inst.channels, readInst.channels );
		Assert.assertEquals( inst.getComponent().getName(), readInst.getComponent().getName());
	}


	@Test
	public void testInstanceBinding_4() throws Exception {

		final String result = "{\"name\":\"server\",\"path\":\"/server\",\"status\":\"STOPPING\",\"component\":{\"name\":\"server-component\"}}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Component comp = new Component( "server-component" );
		Instance inst = new Instance( "server" ).status( InstanceStatus.STOPPING ).component( comp );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, inst );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Instance readInst = mapper.readValue( result, Instance.class );
		Assert.assertEquals( inst, readInst );
		Assert.assertEquals( inst.getName(), readInst.getName());
		Assert.assertEquals( inst.getStatus(), readInst.getStatus());
		Assert.assertEquals( inst.channels, readInst.channels );
		Assert.assertEquals( inst.getComponent().getName(), readInst.getComponent().getName());
	}


	@Test
	public void testInstanceBinding_5() throws Exception {

		final String result = "{\"name\":\"instance\",\"path\":\"/instance\",\"status\":\"NOT_DEPLOYED\",\"data\":{\"ip\":\"127.0.0.1\",\"any field\":\"some value\"}}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Instance inst = new Instance( "instance" );
		inst.data.put( "ip", "127.0.0.1" );
		inst.data.put( "any field", "some value" );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, inst );
		String s = writer.toString();

		Assert.assertEquals( result, s );
	}


	@Test
	public void testInstanceBinding_6() throws Exception {

		final String result = "{\"component\":{\"name\":\"server-component\"}}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Instance inst = new Instance().component( new Component( "server-component" )).status( null );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, inst );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Instance readInst = mapper.readValue( result, Instance.class );
		Assert.assertEquals( inst.getName(), readInst.getName());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, readInst.getStatus());
		Assert.assertEquals( inst.channels, readInst.channels );
		Assert.assertEquals( inst.getComponent().getName(), readInst.getComponent().getName());
	}

	@Test
	public void testInstanceBinding_7() throws Exception {

		final String result = "{\"name\":\"instance\",\"path\":\"/instance\",\"status\":\"NOT_DEPLOYED\",\"component\":{\"name\":\"component1\"},\"exports\":{\"component1.ip\":\"127.0.0.1\",\"any field\":\"some value\",\"component1.test\":\"test\"}}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Component comp = new Component("component1");
		comp.addExportedVariable( new ExportedVariable( "test", "test" ));

		Instance inst = new Instance( "instance" ).component(comp);
		inst.overriddenExports.put("component1.ip", "127.0.0.1");
		inst.overriddenExports.put("any field", "some value");

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, inst );
		String s = writer.toString();

		// May this fail due to export fields ordering ? Works by now...
		// Just remove line below if it happens to fail.
		Assert.assertEquals( result, s );

		Instance readInst = mapper.readValue( result, Instance.class);
		Map<String, String> exports = InstanceHelpers.findAllExportedVariables(inst);
		Map<String, String> readExports = InstanceHelpers.findAllExportedVariables(readInst);
		Assert.assertEquals(exports, readExports); // Works on maps with non-mutable keys (here, String is fine)
	}

	@Test
	public void testDiagnosticBinding_1() throws Exception {

		final String result = "{\"path\":\"/vm\",\"dependencies\":[]}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Diagnostic diag = new Diagnostic( "/vm" );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, diag );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Diagnostic readDiag = mapper.readValue( result, Diagnostic.class );
		Assert.assertEquals( diag.getInstancePath(), readDiag.getInstancePath());
		Assert.assertEquals( diag.getDependenciesInformation(), readDiag.getDependenciesInformation());
	}


	@Test
	public void testDiagnosticBinding_2() throws Exception {

		final String result = "{\"path\":\"/vm\",\"dependencies\":[{\"name\":\"mysql\",\"optional\":\"true\",\"resolved\":\"false\"},"
		+ "{\"name\":\"mongo\",\"optional\":\"false\",\"resolved\":\"true\"}]}";

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		Diagnostic diag = new Diagnostic( "/vm" );
		diag.getDependenciesInformation().add( new DependencyInformation( "mysql", true, false ));
		diag.getDependenciesInformation().add( new DependencyInformation( "mongo", false, true ));

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, diag );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Diagnostic readDiag = mapper.readValue( result, Diagnostic.class );
		Assert.assertEquals( diag.getInstancePath(), readDiag.getInstancePath());
		Assert.assertEquals( diag.getDependenciesInformation().size(), readDiag.getDependenciesInformation().size());

		for( int i=0; i<diag.getDependenciesInformation().size(); i++ ) {
			DependencyInformation original = diag.getDependenciesInformation().get( i );
			DependencyInformation read = readDiag.getDependenciesInformation().get( i );

			Assert.assertEquals( original.getDependencyName(), read.getDependencyName());
			Assert.assertEquals( original.isOptional(), read.isOptional());
			Assert.assertEquals( original.isResolved(), read.isResolved());
		}
	}


	@Test
	public void testDiagnosticBinding_3() throws Exception {

		final String result = "{\"dependencies\":[]}";

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		Diagnostic diag = new Diagnostic();

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, diag );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Diagnostic readDiag = mapper.readValue( "{}", Diagnostic.class );
		Assert.assertEquals( diag.getInstancePath(), readDiag.getInstancePath());
		Assert.assertEquals( diag.getDependenciesInformation(), readDiag.getDependenciesInformation());
	}


	@Test
	public void testDependencyInformationBinding_1() throws Exception {

		final String result = "{\"name\":\"/vm\",\"optional\":\"true\",\"resolved\":\"false\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		DependencyInformation info = new DependencyInformation( "/vm", true, false );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, info );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		DependencyInformation readInfo = mapper.readValue( result, DependencyInformation.class );
		Assert.assertEquals( info.getDependencyName(), readInfo.getDependencyName());
		Assert.assertEquals( info.isOptional(), readInfo.isOptional());
		Assert.assertEquals( info.isResolved(), readInfo.isResolved());
	}


	@Test
	public void testDependencyInformationBinding_2() throws Exception {

		final String result = "{\"optional\":\"false\",\"resolved\":\"false\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		DependencyInformation info = new DependencyInformation();

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, info );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		DependencyInformation readInfo = mapper.readValue( "{}", DependencyInformation.class );
		Assert.assertEquals( info.getDependencyName(), readInfo.getDependencyName());
		Assert.assertEquals( info.isOptional(), readInfo.isOptional());
		Assert.assertEquals( info.isResolved(), readInfo.isResolved());
	}


	@Test
	public void testTargetWDBinding_1() throws Exception {

		final String result = "{}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		TargetWrapperDescriptor twd = new TargetWrapperDescriptor();
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, twd );
		String s = writer.toString();

		Assert.assertEquals( result, s );

		twd = mapper.readValue( result, TargetWrapperDescriptor.class );
		Assert.assertNull( twd.getId());
		Assert.assertNull( twd.getName());
		Assert.assertNull( twd.getDescription());
		Assert.assertNull( twd.getHandler());
		Assert.assertFalse( twd.isDefault());
	}


	@Test
	public void testTargetWDBinding_2() throws Exception {

		final String result = "{\"id\":\"1\",\"name\":\"target 1\",\"handler\":\"aws\",\"desc\":\"my target\",\"default\":\"true\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		TargetWrapperDescriptor twd = new TargetWrapperDescriptor();
		twd.setId( "1" );
		twd.setName( "target 1" );
		twd.setDescription( "my target" );
		twd.setHandler( "aws" );
		twd.setDefault( true );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, twd );
		String s = writer.toString();

		Assert.assertEquals( result, s );

		TargetWrapperDescriptor readTwd = mapper.readValue( result, TargetWrapperDescriptor.class );
		Assert.assertEquals( twd.getId(), readTwd.getId());
		Assert.assertEquals( twd.getName(), readTwd.getName());
		Assert.assertEquals( twd.getDescription(), readTwd.getDescription());
		Assert.assertEquals( twd.getHandler(), readTwd.getHandler());
	}


	@Test
	public void testTargetUsageItemBinding_1() throws Exception {

		TargetUsageItem item = new TargetUsageItem();

		StringWriter writer = new StringWriter();
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		mapper.writeValue( writer, item );
		String s = writer.toString();

		Assert.assertEquals( "{}", s );
	}


	@Test
	public void testTargetUsageItemBinding_2() throws Exception {

		TargetUsageItem item = new TargetUsageItem();
		item.setName( "app" );
		item.setQualifier( "v1" );
		item.setReferencing( true );

		StringWriter writer = new StringWriter();
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		mapper.writeValue( writer, item );
		String s = writer.toString();

		Assert.assertEquals( "{\"name\":\"app\",\"qualifier\":\"v1\",\"referencing\":\"true\"}", s );
	}


	@Test
	public void testTargetUsageItemBinding_3() throws Exception {

		TargetUsageItem item = new TargetUsageItem();
		item.setName( "app1" );
		item.setReferencing( true );
		item.setUsing( true );

		StringWriter writer = new StringWriter();
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		mapper.writeValue( writer, item );
		String s = writer.toString();

		Assert.assertEquals( "{\"name\":\"app1\",\"using\":\"true\",\"referencing\":\"true\"}", s );
	}



	@Test
	public void testStringWrapperBinding_1() throws Exception {

		StringWrapper obj = new StringWrapper( "test" );

		StringWriter writer = new StringWriter();
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		mapper.writeValue( writer, obj );
		String s = writer.toString();

		Assert.assertEquals( "{\"s\":\"test\"}", s );
		StringWrapper read = mapper.readValue( s, StringWrapper.class );
		Assert.assertEquals( obj.toString(), read.toString());
	}


	@Test
	public void testStringWrapperBinding_2() throws Exception {

		StringWrapper obj = new StringWrapper( null );

		StringWriter writer = new StringWriter();
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		mapper.writeValue( writer, obj );
		String s = writer.toString();

		Assert.assertEquals( "{}", s );
		StringWrapper read = mapper.readValue( s, StringWrapper.class );
		Assert.assertNull( read.toString());
	}


	@Test
	public void testMapWrapperBinding_1() throws Exception {

		Map<String,String> map = new LinkedHashMap<String,String>( 2 );
		map.put( "key1", "value1" );
		map.put( "key2", "value2" );
		map.put( "key3", null );
		map.put( null, "value4" );

		MapWrapper obj = new MapWrapper( map );

		StringWriter writer = new StringWriter();
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		mapper.writeValue( writer, obj );
		String s = writer.toString();

		Assert.assertEquals( "{\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":\"\",\"\":\"value4\"}", s );
		MapWrapper read = mapper.readValue( s, MapWrapper.class );

		Assert.assertEquals( "value1", read.getMap().get( "key1" ));
		Assert.assertEquals( "value2", read.getMap().get( "key2" ));
		Assert.assertEquals( "", read.getMap().get( "key3" ));
		Assert.assertEquals( "value4", read.getMap().get( "" ));
	}


	@Test
	public void testMapWrapperBinding_2() throws Exception {

		Map<String,String> map = new HashMap<String,String>( 0 );
		MapWrapper obj = new MapWrapper( map );

		StringWriter writer = new StringWriter();
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		mapper.writeValue( writer, obj );
		String s = writer.toString();

		Assert.assertEquals( "{}", s );
		MapWrapper read = mapper.readValue( s, MapWrapper.class );
		Assert.assertEquals( obj.getMap(), read.getMap());
	}


	@Test
	public void testTargetAssociationBinding() throws Exception {

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		TargetAssociation association = new TargetAssociation( null, null );
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, association );
		Assert.assertEquals( "{}", writer.toString());

		association = new TargetAssociation( "/my-path", null );
		writer = new StringWriter();
		mapper.writeValue( writer, association );
		Assert.assertEquals( "{\"path\":\"/my-path\"}", writer.toString());

		TargetWrapperDescriptor twd = new TargetWrapperDescriptor();
		association = new TargetAssociation( "/my-path", twd );
		writer = new StringWriter();
		mapper.writeValue( writer, association );
		Assert.assertEquals( "{\"path\":\"/my-path\",\"desc\":{}}", writer.toString());

		twd.setId( "54" );
		twd.setName( "toto" );
		writer = new StringWriter();
		mapper.writeValue( writer, association );
		Assert.assertEquals( "{\"path\":\"/my-path\",\"desc\":{\"id\":\"54\",\"name\":\"toto\"}}", writer.toString());
	}
}
