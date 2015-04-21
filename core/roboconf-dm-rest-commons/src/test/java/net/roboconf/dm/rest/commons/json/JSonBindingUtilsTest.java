/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

import junit.framework.Assert;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
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

		final String result = "{\"desc\":\"some text\",\"qualifier\":\"v1\"}";
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
	}


	@Test
	public void testApplicationTemplateBinding_2() throws Exception {

		final String result = "{\"name\":\"my application\",\"qualifier\":\"v1-17.snapshot\"}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		ApplicationTemplate app = new ApplicationTemplate( "my application" ).qualifier( "v1-17.snapshot" );
		StringWriter writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();

		Assert.assertEquals( result, s );
		ApplicationTemplate readApp = mapper.readValue( result, ApplicationTemplate.class );
		Assert.assertEquals( app, readApp );
		Assert.assertEquals( app.getName(), readApp.getName());
		Assert.assertEquals( app.getDescription(), readApp.getDescription());
		Assert.assertEquals( app.getQualifier(), readApp.getQualifier());
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

		final String result = "{\"tpl\":{\"name\":\"oops\",\"desc\":\"hello!\"}}";
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();

		ApplicationTemplate tpl = new ApplicationTemplate( "oops" ).description( "hello!" );
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
}
