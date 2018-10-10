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

package net.roboconf.dm.rest.commons.json;

import static net.roboconf.core.model.runtime.CommandHistoryItem.EXECUTION_OK;
import static net.roboconf.core.model.runtime.CommandHistoryItem.EXECUTION_OK_WITH_SKIPPED;
import static net.roboconf.core.model.runtime.CommandHistoryItem.ORIGIN_REST_API;
import static net.roboconf.core.model.runtime.CommandHistoryItem.ORIGIN_SCHEDULER;
import static net.roboconf.dm.rest.commons.json.JSonBindingUtils.AT_INSTANCE_PATH;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestApplicationTemplate;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ExportedVariable;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.CommandHistoryItem;
import net.roboconf.core.model.runtime.EventType;
import net.roboconf.core.model.runtime.Preference;
import net.roboconf.core.model.runtime.Preference.PreferenceKeyCategory;
import net.roboconf.core.model.runtime.ScheduledJob;
import net.roboconf.core.model.runtime.TargetUsageItem;
import net.roboconf.core.model.runtime.TargetWrapperDescriptor;
import net.roboconf.dm.rest.commons.Diagnostic;
import net.roboconf.dm.rest.commons.Diagnostic.DependencyInformation;
import net.roboconf.dm.rest.commons.beans.ApplicationBindings;
import net.roboconf.dm.rest.commons.beans.ApplicationBindings.ApplicationBindingItem;
import net.roboconf.dm.rest.commons.beans.TargetAssociation;
import net.roboconf.dm.rest.commons.beans.WebSocketMessage;

/**
 * @author Vincent Zurczak - Linagora
 */
public class JSonBindingUtilsTest {

	@Test
	public void testApplicationTemplateBinding_1() throws Exception {

		final String result = "{\"desc\":\"some text\",\"version\":\"1.0.1\",\"apps\":[]}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		ApplicationTemplate app = new ApplicationTemplate().description( "some text" ).version( "1.0.1" );
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		ApplicationTemplate readApp = mapper.readValue( result, ApplicationTemplate.class );
		Assert.assertEquals( app, readApp );
		Assert.assertEquals( app.getName(), readApp.getName());
		Assert.assertEquals( app.getDescription(), readApp.getDescription());
		Assert.assertEquals( app.getVersion(), readApp.getVersion());
		Assert.assertEquals( app.getExternalExportsPrefix(), readApp.getExternalExportsPrefix());
		Assert.assertEquals( app.getTags(), readApp.getTags());
	}


	@Test
	public void testApplicationTemplateBinding_2() throws Exception {

		final String result = "{\"name\":\"my application\",\"displayName\":\"my application\",\"version\":\"v1-17.snapshot\",\"apps\":[\"a1\",\"a2\"]}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		ApplicationTemplate app = new ApplicationTemplate( "my application" ).version( "v1-17.snapshot" );
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
		Assert.assertEquals( app.getVersion(), readApp.getVersion());
		Assert.assertEquals( app.getExternalExportsPrefix(), readApp.getExternalExportsPrefix());
		Assert.assertEquals( app.getTags(), readApp.getTags());
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

		final String result = "{\"name\":\"my application\",\"displayName\":\"my application\",\"apps\":[]}";
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
		Assert.assertEquals( app.getVersion(), readApp.getVersion());
		Assert.assertEquals( app.getExternalExportsPrefix(), readApp.getExternalExportsPrefix());
		Assert.assertEquals( app.getTags(), readApp.getTags());
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
		Assert.assertEquals( app.getVersion(), readApp.getVersion());
		Assert.assertEquals( app.getExternalExportsPrefix(), readApp.getExternalExportsPrefix());
		Assert.assertEquals( app.getTags(), readApp.getTags());
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

		Assert.assertEquals( "{\"name\":\"my application\",\"displayName\":\"my application\",\"extVars\":{\"k1\":\"v1\",\"k2\":\"v2\"},\"apps\":[]}", s );
	}


	@Test
	public void testApplicationTemplateBinding_7() throws Exception {

		// Initial binding
		ApplicationTemplate tpl = new ApplicationTemplate( "my tpl" );
		Application app = new Application( "app", tpl );

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, tpl );

		Assert.assertEquals( "{\"name\":\"my tpl\",\"displayName\":\"my tpl\",\"apps\":[\"app\"]}", writer.toString());

		// After we remove the association with the application
		app.removeAssociationWithTemplate();

		writer = new StringWriter();
		mapper.writeValue( writer, tpl );

		Assert.assertEquals( "{\"name\":\"my tpl\",\"displayName\":\"my tpl\",\"apps\":[]}", writer.toString());
	}


	@Test
	public void testApplicationTemplateBinding_8() throws Exception {

		TestApplicationTemplate tpl = new TestApplicationTemplate();
		ImportedVariable var1 = new ImportedVariable( "something.else", true, true );
		ImportedVariable var2 = new ImportedVariable( "other.stuff", true, true );

		tpl.getWar().getComponent().importedVariables.put( var1.getName(),  var1 );
		tpl.getWar().getComponent().importedVariables.put( var2.getName(),  var2 );

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, tpl );
		String s = writer.toString();

		Assert.assertEquals(
				"{\"name\":\"" + tpl.getName()
						+ "\",\"displayName\":\""
						+ tpl.getName()
						+ "\",\"version\":\""
						+ tpl.getVersion()
						+ "\",\"extDep\":[\"other\",\"something\"],\"apps\":[]}",
				s );
	}


	@Test
	public void testApplicationTemplateBinding_9() throws Exception {

		final String result = "{\"name\":\"aeocu\",\"displayName\":\"àéoçù\",\"desc\":\"some text\",\"version\":\"v1\",\"apps\":[]}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		ApplicationTemplate app = new ApplicationTemplate( "àéoçù" ).description( "some text" ).version( "v1" );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();
		Assert.assertEquals( result, s );

		ApplicationTemplate readApp = mapper.readValue( result, ApplicationTemplate.class );
		Assert.assertEquals( app, readApp );
		Assert.assertEquals( app.getName(), readApp.getName());
		Assert.assertEquals( app.getDescription(), readApp.getDescription());
		Assert.assertEquals( app.getVersion(), readApp.getVersion());
		Assert.assertEquals( app.getExternalExportsPrefix(), readApp.getExternalExportsPrefix());
		Assert.assertEquals( app.getTags(), readApp.getTags());
	}


	@Test
	public void testApplicationTemplateBinding_10() throws Exception {

		final String result = "{\"displayName\":\"àéoçù\",\"desc\":\"some text\",\"version\":\"v1\",\"apps\":[]}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		ApplicationTemplate app = new ApplicationTemplate( "àéoçù" ).description( "some text" ).version( "v1" );

		ApplicationTemplate readApp = mapper.readValue( result, ApplicationTemplate.class );
		Assert.assertEquals( app, readApp );
		Assert.assertEquals( app.getName(), readApp.getName());
		Assert.assertEquals( app.getDescription(), readApp.getDescription());
		Assert.assertEquals( app.getVersion(), readApp.getVersion());
		Assert.assertEquals( app.getExternalExportsPrefix(), readApp.getExternalExportsPrefix());
		Assert.assertEquals( app.getTags(), readApp.getTags());
	}


	@Test
	public void testApplicationTemplateBinding_11() throws Exception {

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		final String result =
		"{\"name\":\"coucou\",\"displayName\":\"coucou\",\"desc\":\"some text\",\"version\":\"v1\",\"apps\":[],\"tags\":[\"t1\",\"t21\",\"t4\"]}";

		ApplicationTemplate app = new ApplicationTemplate( "coucou" ).description( "some text" ).version( "v1" );
		app.setTags( Arrays.asList( "t1", "t21", "t4" ));

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();
		Assert.assertEquals( result, s );

		ApplicationTemplate readApp = mapper.readValue( result, ApplicationTemplate.class );
		Assert.assertEquals( app, readApp );
		Assert.assertEquals( app.getName(), readApp.getName());
		Assert.assertEquals( app.getDescription(), readApp.getDescription());
		Assert.assertEquals( app.getVersion(), readApp.getVersion());
		Assert.assertEquals( app.getExternalExportsPrefix(), readApp.getExternalExportsPrefix());
		Assert.assertEquals( app.getTags(), readApp.getTags());
	}


	@Test
	public void testApplicationBindingsBinding() throws Exception {

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		// Empty object
		ApplicationBindings bindings = new ApplicationBindings();
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, bindings );
		Assert.assertEquals( "{}", writer.toString());

		// Single key
		List<ApplicationBindingItem> list = new ArrayList<> ();
		list.add( new ApplicationBindingItem( "app1", true ));
		bindings.prefixToItems.put( "prefix1", list );

		writer = new StringWriter();
		mapper.writeValue( writer, bindings );
		Assert.assertEquals( "{\"prefix1\":[{\"name\":\"app1\",\"bound\":true}]}", writer.toString());

		// Complex object
		list = new ArrayList<> ();
		list.add( new ApplicationBindingItem( "app2", false ));
		list.add( new ApplicationBindingItem( "app3", true ));
		bindings.prefixToItems.put( "prefix2", list );

		list = new ArrayList<> ();
		list.add( new ApplicationBindingItem( "app4", false ));
		bindings.prefixToItems.put( "prefix0", list );

		writer = new StringWriter();
		mapper.writeValue( writer, bindings );
		Assert.assertEquals(
				"{\"prefix0\":[{\"name\":\"app4\",\"bound\":false}],"
				+ "\"prefix1\":[{\"name\":\"app1\",\"bound\":true}],\"prefix2\":[{\"name\":\"app2\",\"bound\":false},"
				+ "{\"name\":\"app3\",\"bound\":true}]}",
				writer.toString());
	}


	@Test
	public void testApplicationBinding_1() throws Exception {

		final String result = "{\"name\":\"app1\",\"displayName\":\"app1\",\"desc\":\"some text\"}";
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

		final String result = "{\"tplName\":\"oops\",\"tplVersion\":\"hello!\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		ApplicationTemplate tpl = new ApplicationTemplate( "oops" ).version( "hello!" );
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

		Assert.assertEquals( "{\"name\":\"test\",\"displayName\":\"test\",\"info\":\"warn\"}", writer.toString());
	}


	@Test
	public void testApplicationBinding_5() throws Exception {

		Application app = new Application( "test", null ).description( "hi!" );
		app.getRootInstances().add( new Instance( "r" ).status( InstanceStatus.DEPLOYED_STARTED ));

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );

		Assert.assertEquals( "{\"name\":\"test\",\"displayName\":\"test\",\"desc\":\"hi!\",\"info\":\"ok\"}", writer.toString());
	}


	@Test
	public void testApplicationBinding_6() throws Exception {

		final String result = "{\"name\":\"app1\",\"displayName\":\"app1\",\"tplName\":\"oops\"}";
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

		final String result = "{\"name\":\"app1\",\"displayName\":\"app1\",\"tplName\":\"\",\"tplVersion\":\"oops\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		ApplicationTemplate tpl = new ApplicationTemplate( "" ).version( "oops" );
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

		ApplicationTemplate tpl = new ApplicationTemplate( "" ).version( "oops" );
		tpl.externalExports.put( "k1", "v1" );
		tpl.externalExports.put( "k2", "v2" );

		Application app = new Application( "app1", tpl );
		app.getRootInstances().add( new Instance( "r" ));

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();

		Assert.assertEquals(
				"{\"name\":\"app1\",\"displayName\":\"app1\",\"tplName\":\"\",\"tplVersion\":\"oops\","
				+ "\"extVars\":{\"k1\":\"v1\",\"k2\":\"v2\"}}", s );
	}


	@Test
	public void testApplicationBinding_9() throws Exception {

		ApplicationTemplate tpl = new ApplicationTemplate( "" ).version( "oops" );
		tpl.setExternalExportsPrefix( "toto" );

		Application app = new Application( "app1", tpl );
		app.getRootInstances().add( new Instance( "r" ));

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();

		Assert.assertEquals(
				"{\"name\":\"app1\",\"displayName\":\"app1\",\"tplName\":\"\","
				+ "\"tplVersion\":\"oops\",\"tplEep\":\"toto\"}", s );

		Application readApp = mapper.readValue( s, Application.class );
		Assert.assertEquals( app, readApp );
		Assert.assertEquals( app.getName(), readApp.getName());
		Assert.assertEquals( app.getDescription(), readApp.getDescription());
		Assert.assertEquals( app.getTemplate(), readApp.getTemplate());
		Assert.assertEquals( app.getTemplate().getExternalExportsPrefix(), readApp.getTemplate().getExternalExportsPrefix());
	}


	@Test
	public void testApplicationBinding_10() throws Exception {

		TestApplication app = new TestApplication();
		ImportedVariable var1 = new ImportedVariable( "something.else", true, true );
		ImportedVariable var2 = new ImportedVariable( "other.stuff", true, true );

		app.getWar().getComponent().importedVariables.put( var1.getName(),  var1 );
		app.getWar().getComponent().importedVariables.put( var2.getName(),  var2 );

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();

		Assert.assertEquals(
				"{\"name\":\"" + app.getName()
						+ "\",\"displayName\":\""
						+ app.getName()
						+ "\",\"tplName\":\""
						+ app.getTemplate().getName()
						+ "\",\"tplVersion\":\""
						+ app.getTemplate().getVersion()
						+ "\",\"extDep\":[\"other\",\"something\"]}",
				s );
	}


	@Test
	public void testApplicationBinding_11() throws Exception {

		final String result = "{\"name\":\"aeocu\",\"displayName\":\"àéoçù\",\"desc\":\"some text\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Application app = new Application( "àéoçù", null ).description( "some text" );
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
	public void testApplicationBinding_12() throws Exception {

		final String result = "{\"name\":\"aeocu\",\"displayName\":\"àéoçù\",\"desc\":\"some text\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Application app = new Application( "àéoçù", null ).description( "some text" );
		Application readApp = mapper.readValue( result, Application.class );
		Assert.assertEquals( app, readApp );
		Assert.assertEquals( app.getName(), readApp.getName());
		Assert.assertEquals( app.getDescription(), readApp.getDescription());
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
	public void testComponentBinding_3() throws Exception {

		Component c1 = new Component( "component 1" ).installerName( "target" );
		Component c2 = new Component( "component 2" );
		c2.extendComponent( c1 );

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, c1 );
		Assert.assertEquals( "{\"name\":\"component 1\",\"installer\":\"target\"}", writer.toString());

		writer = new StringWriter();
		mapper.writeValue( writer, c2 );
		Assert.assertEquals( "{\"name\":\"component 2\",\"installer\":\"target\"}", writer.toString());
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
		Assert.assertEquals( "/instance", readInst.data.get( AT_INSTANCE_PATH ));
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
		Assert.assertEquals( "/server", readInst.data.get( AT_INSTANCE_PATH ));
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
		Assert.assertEquals( "/vm/server", readInst.data.get( AT_INSTANCE_PATH ));
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
		Assert.assertEquals( "/server", readInst.data.get( AT_INSTANCE_PATH ));
	}


	@Test
	public void testInstanceBinding_5() throws Exception {

		final String result = "{\"name\":\"instance\",\"path\":\"/instance\",\"status\":\"NOT_DEPLOYED\",\"data\":{\"any field\":\"some value\",\"ip\":\"127.0.0.1\"}}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Instance inst = new Instance( "instance" );
		inst.data.put( "ip", "127.0.0.1" );
		inst.data.put( "any field", "some value" );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, inst );
		String s = writer.toString();
		Assert.assertEquals( result, s );

		Instance readInst = mapper.readValue( result, Instance.class );
		Assert.assertEquals( inst.getName(), readInst.getName());
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, readInst.getStatus());
		Assert.assertEquals( inst.channels, readInst.channels );
		Assert.assertEquals( "/instance", readInst.data.get( AT_INSTANCE_PATH ));

		readInst.data.remove( AT_INSTANCE_PATH );
		Assert.assertEquals( inst.data, readInst.data );
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
		Assert.assertNull( readInst.data.get( AT_INSTANCE_PATH ));
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
		Assert.assertEquals( "/instance", readInst.data.get( AT_INSTANCE_PATH ));
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
		item.setVersion( "v1" );
		item.setReferencing( true );

		StringWriter writer = new StringWriter();
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		mapper.writeValue( writer, item );
		String s = writer.toString();

		Assert.assertEquals( "{\"name\":\"app\",\"version\":\"v1\",\"referencing\":\"true\"}", s );
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

		Map<String,String> map = new LinkedHashMap<>( 2 );
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

		Map<String,String> map = new HashMap<>( 0 );
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
	public void testMappedCollectionWrapperBinding_1() throws Exception {

		Map<String,List<String>> map = new LinkedHashMap<>( 2 );
		map.put( "key1", Arrays.asList( "value11", "value12" ));
		map.put( "key2", Arrays.asList( "value2" ));
		map.put( "key3", null );
		map.put( "key4", new ArrayList<String>( 0 ));
		map.put( null, Arrays.asList( "value4" ));

		MappedCollectionWrapper obj = new MappedCollectionWrapper( map );

		StringWriter writer = new StringWriter();
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		mapper.writeValue( writer, obj );
		String s = writer.toString();

		Assert.assertEquals( "{\"key1\":[\"value11\",\"value12\"],\"key2\":[\"value2\"],\"key3\":[],\"key4\":[],\"\":[\"value4\"]}", s );
	}


	@Test
	public void testMappedCollectionWrapperBinding_2() throws Exception {

		Map<String,Set<String>> map = new HashMap<>( 0 );
		MappedCollectionWrapper obj = new MappedCollectionWrapper( map );

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

		TargetAssociation association = new TargetAssociation( null, null, null );
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, association );
		Assert.assertEquals( "{}", writer.toString());

		association = new TargetAssociation( "/my-path", "comp1", null );
		writer = new StringWriter();
		mapper.writeValue( writer, association );
		Assert.assertEquals( "{\"path\":\"/my-path\",\"component\":\"comp1\"}", writer.toString());

		TargetWrapperDescriptor twd = new TargetWrapperDescriptor();
		association = new TargetAssociation( "/my-path", "comp2", twd );
		writer = new StringWriter();
		mapper.writeValue( writer, association );
		Assert.assertEquals( "{\"path\":\"/my-path\",\"component\":\"comp2\",\"desc\":{}}", writer.toString());

		twd.setId( "54" );
		twd.setName( "toto" );
		writer = new StringWriter();
		mapper.writeValue( writer, association );
		Assert.assertEquals( "{\"path\":\"/my-path\",\"component\":\"comp2\",\"desc\":{\"id\":\"54\",\"name\":\"toto\"}}", writer.toString());
	}


	@Test
	public void testPreferenceBinding_1() throws Exception {

		final String result = "{}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Preference pref = new Preference( null, null, null );
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, pref );
		String s = writer.toString();

		Assert.assertEquals( result, s );
	}


	@Test
	public void testPreferenceBinding_2() throws Exception {

		final String result = "{\"name\":\"mail.toto\",\"value\":\"smtp.something\",\"category\":\"email\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		// Test the serializer
		Preference pref = new Preference( "mail.toto", "smtp.something", PreferenceKeyCategory.EMAIL );
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, pref );
		String s = writer.toString();

		Assert.assertEquals( result, s );

		// Test the deserializer
		Preference readPref = mapper.readValue( result, Preference.class );
		Assert.assertNotNull( readPref );
		Assert.assertEquals( "mail.toto", readPref.getName());
		Assert.assertEquals( "smtp.something", readPref.getValue());
		Assert.assertEquals( PreferenceKeyCategory.EMAIL, readPref.getCategory());
	}


	@Test
	public void testScheduledJobBinding_1() throws Exception {

		final String result = "{}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		// Test the serializer
		ScheduledJob job = new ScheduledJob( null );
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, job );
		String s = writer.toString();

		Assert.assertEquals( result, s );

		// Test the deserializer
		ScheduledJob readJob = mapper.readValue( result, ScheduledJob.class );
		Assert.assertNotNull( readJob );
		Assert.assertNull( readJob.getJobId());
		Assert.assertNull( readJob.getJobName());
		Assert.assertNull( readJob.getCmdName());
		Assert.assertNull( readJob.getAppName());
		Assert.assertNull( readJob.getCron());
	}


	@Test
	public void testScheduledJobBinding_2() throws Exception {

		final String result = "{\"id\":\"job id\",\"app-name\":\"app\",\"cmd-name\":\"cmd\",\"job-name\":\"job\",\"cron\":\"* * *\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		// Test the serializer
		ScheduledJob job = new ScheduledJob( "job id" );
		job.setAppName( "app" );
		job.setCmdName( "cmd" );
		job.setJobName( "job" );
		job.setCron( "* * *" );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, job );
		String s = writer.toString();

		Assert.assertEquals( result, s );

		// Test the deserializer
		ScheduledJob readJob = mapper.readValue( result, ScheduledJob.class );
		Assert.assertNotNull( readJob );
		Assert.assertEquals( job.getJobId(), readJob.getJobId());
		Assert.assertEquals( job.getJobName(), readJob.getJobName());
		Assert.assertEquals( job.getCmdName(), readJob.getCmdName());
		Assert.assertEquals( job.getAppName(), readJob.getAppName());
		Assert.assertEquals( job.getCron(), readJob.getCron());
	}


	@Test
	public void testWebSocketMessage_instance() throws Exception {

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		WebSocketMessage wsm = new WebSocketMessage( null, null, null );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, wsm );
		Assert.assertEquals( "{}", writer.toString());

		TestApplication app = new TestApplication();
		wsm = new WebSocketMessage( app.getMySql(), app, EventType.CREATED );
		writer = new StringWriter();
		mapper.writeValue( writer, wsm );

		String s = writer.toString();
		Assert.assertEquals( "{\"event\":\"CREATED\",\"app\":{\"name\":\"test\",\"displayName\":\"test\",\"tplName\":\"test-app\",\"tplVersion\":\"1.0.1\"},\"inst\":"
				+ "{\"name\":\"mysql-server\",\"path\":\"/mysql-vm/mysql-server\",\"status\":\"NOT_DEPLOYED\","
				+ "\"component\":{\"name\":\"mysql\",\"installer\":\"puppet\"},\"exports\":{\"mysql.port\":\"3306\",\"mysql.ip\":null}}}", s );
	}


	@Test
	public void testWebSocketMessage_application() throws Exception {

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		WebSocketMessage wsm = new WebSocketMessage((Application) null, null );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, wsm );
		Assert.assertEquals( "{}", writer.toString());

		TestApplication app = new TestApplication();
		wsm = new WebSocketMessage( app, EventType.DELETED );
		writer = new StringWriter();
		mapper.writeValue( writer, wsm );

		String s = writer.toString();
		Assert.assertEquals(
				"{\"event\":\"DELETED\",\"app\":{\"name\":\"test\",\"displayName\":\"test\","
				+ "\"tplName\":\"test-app\",\"tplVersion\":\"1.0.1\"}}", s );
	}


	@Test
	public void testWebSocketMessage_applicationTemplate() throws Exception {

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		WebSocketMessage wsm = new WebSocketMessage((ApplicationTemplate) null, null );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, wsm );
		Assert.assertEquals( "{}", writer.toString());

		TestApplicationTemplate tpl = new TestApplicationTemplate();
		wsm = new WebSocketMessage( tpl, EventType.CHANGED );
		writer = new StringWriter();
		mapper.writeValue( writer, wsm );

		String s = writer.toString();
		Assert.assertEquals( "{\"event\":\"CHANGED\",\"tpl\":{\"name\":\"test-app\",\"displayName\":\"test-app\",\"version\":\"1.0.1\",\"apps\":[]}}", s );
	}


	@Test
	public void testWebSocketMessage_message() throws Exception {

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		WebSocketMessage wsm = new WebSocketMessage( null );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, wsm );
		Assert.assertEquals( "{}", writer.toString());

		wsm = new WebSocketMessage( "my message" );
		writer = new StringWriter();
		mapper.writeValue( writer, wsm );

		String s = writer.toString();
		Assert.assertEquals( "{\"msg\":\"my message\"}", s );
	}


	@Test
	public void testCommandHistoryItem() throws Exception {

		// All fields set
		String expected =
				"{\"app\":\"test\",\"cmd\":\"scale\",\"details\":\"me\",\"start\":1542,\"duration\":21,\"result\":"
				+ EXECUTION_OK + ",\"origin\":" + ORIGIN_REST_API + "}";

		long duration = TimeUnit.NANOSECONDS.convert( 21, TimeUnit.MILLISECONDS );
		Assert.assertEquals( 21000000, duration );
		CommandHistoryItem item = new CommandHistoryItem( "test", "scale", ORIGIN_REST_API, "me", EXECUTION_OK, 1542, duration );

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, item );
		Assert.assertEquals( expected, writer.toString());

		// Only mandatory fields are set
		duration = TimeUnit.NANOSECONDS.convert( 1, TimeUnit.MILLISECONDS );
		item = new CommandHistoryItem( null, null, ORIGIN_SCHEDULER, null, EXECUTION_OK_WITH_SKIPPED, 21, duration );

		writer = new StringWriter();
		mapper.writeValue( writer, item );
		Assert.assertEquals(
				"{\"start\":21,\"duration\":1,\"result\":"
				+ EXECUTION_OK_WITH_SKIPPED + ",\"origin\":" + ORIGIN_SCHEDULER + "}", writer.toString());
	}
}
