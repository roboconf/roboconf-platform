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

package net.roboconf.dm.rest.json;

import java.io.StringWriter;

import junit.framework.Assert;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Application.ApplicationStatus;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Vincent Zurczak - Linagora
 */
public class JSonBindingUtilsTest {

	@Test
	public void testApplicationBinding_1() throws Exception {

		final String result = "{\"name\":\"app1\",\"status\":\"STOPPED\",\"desc\":\"some text\",\"qualifier\":\"v1\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Application app = new Application();
		app.setName( "app1" );
		app.setDescription( "some text" );
		app.setQualifier( "v1" );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Application readApp = mapper.readValue( result, Application.class );
		Assert.assertEquals( app, readApp );
	}


	@Test
	public void testApplicationBinding_2() throws Exception {

		final String result = "{\"name\":\"my application\",\"status\":\"STOPPED\",\"qualifier\":\"v1-17.snapshot\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Application app = new Application();
		app.setName( "my application" );
		app.setQualifier( "v1-17.snapshot" );
		app.setStatus( ApplicationStatus.STOPPED );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Application readApp = mapper.readValue( result, Application.class );
		Assert.assertEquals( app, readApp );
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
	public void testComponentBinding_1() throws Exception {

		final String result = "{\"name\":\"component 1\",\"alias\":\"A component\",\"installer\":\"iaas\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Component comp = new Component();
		comp.setName( "component 1" );
		comp.setAlias( "A component" );
		comp.setInstallerName( "iaas" );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, comp );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Component readComp = mapper.readValue( result, Component.class );
		Assert.assertEquals( comp, readComp );
	}


	@Test
	public void testComponentBinding_2() throws Exception {

		final String result = "{\"alias\":\"A 'special' component\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Component comp = new Component();
		comp.setAlias( "A \"special\" component" );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, comp );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Component readComp = mapper.readValue( result, Component.class );
		Assert.assertEquals( comp, readComp );
	}


	@Test
	public void testInstanceBinding_1() throws Exception {

		final String result = "{\"name\":\"instance\",\"path\":\"|instance\",\"status\":\"UNKNOWN\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Instance inst = new Instance();
		inst.setName( "instance" );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, inst );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Instance readInst = mapper.readValue( result, Instance.class );
		Assert.assertEquals( inst, readInst );
	}


	@Test
	public void testInstanceBinding_2() throws Exception {

		final String result = "{\"name\":\"server\",\"path\":\"|server\",\"status\":\"INSTANTIATING\",\"channel\":\"channel4\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Instance inst = new Instance();
		inst.setName( "server" );
		inst.setChannel( "channel4" );
		inst.setStatus( InstanceStatus.INSTANTIATING );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, inst );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Instance readInst = mapper.readValue( result, Instance.class );
		Assert.assertEquals( inst, readInst );
	}


	@Test
	public void testInstanceBinding_3() throws Exception {

		final String result = "{\"name\":\"server\",\"path\":\"|vm|server\",\"status\":\"INSTANTIATING\",\"component\":{\"name\":\"server-component\"}}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Instance inst = new Instance();
		inst.setName( "server" );
		inst.setStatus( InstanceStatus.INSTANTIATING );

		Component comp = new Component();
		comp.setName( "server-component" );
		inst.setComponent( comp );

		Instance parentInst = new Instance();
		parentInst.setName( "vm" );
		InstanceHelpers.insertChild( parentInst, inst );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, inst );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Instance readInst = mapper.readValue( result, Instance.class );
		Assert.assertEquals( inst.getName(), readInst.getName());
	}


	@Test
	public void testInstanceBinding_4() throws Exception {

		final String result = "{\"name\":\"server\",\"path\":\"|server\",\"status\":\"INSTANTIATING\",\"component\":{\"name\":\"server-component\",\"alias\":\"this is a server!\"}}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		Instance inst = new Instance();
		inst.setName( "server" );
		inst.setStatus( InstanceStatus.INSTANTIATING );

		Component comp = new Component();
		comp.setName( "server-component" );
		comp.setAlias( "this is a server!" );
		inst.setComponent( comp );

		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, inst );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		Instance readInst = mapper.readValue( result, Instance.class );
		Assert.assertEquals( inst, readInst );
	}
}
