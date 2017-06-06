/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.swagger;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.ExportedVariable;
import net.roboconf.core.model.beans.Graphs;
import net.roboconf.core.model.beans.ImportedVariable;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Preference;
import net.roboconf.core.model.runtime.Preference.PreferenceKeyCategory;
import net.roboconf.core.model.runtime.ScheduledJob;
import net.roboconf.core.model.runtime.TargetUsageItem;
import net.roboconf.core.model.runtime.TargetWrapperDescriptor;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.rest.commons.Diagnostic;
import net.roboconf.dm.rest.commons.Diagnostic.DependencyInformation;
import net.roboconf.dm.rest.commons.beans.TargetAssociation;
import net.roboconf.dm.rest.commons.json.JSonBindingUtils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class UpdateSwaggerJson {

	final Set<Class<?>> processedClasses = new HashSet<> ();


	/**
	 * @param args
	 */
	public static void main( String[] args ) {

		try {
			new UpdateSwaggerJson().run( args );

		} catch( Exception e ) {
			e.printStackTrace();
			System.exit( 3 );
		}
	}


	/**
	 * The method that does the job.
	 * @param args
	 * @throws Exception
	 */
	public void run( String[] args ) throws Exception {

		// Check
		File baseDirectory = null;
		if( args.length != 1
				|| ! (baseDirectory = new File( args[ 0 ])).exists())
			throw new RuntimeException( "The path of the module's directory was expected as an argument." );

		// Update
		UpdateSwaggerJson updater = new UpdateSwaggerJson();
		JsonObject newDef = updater.prepareNewDefinitions();
		updater.updateSwaggerJson( baseDirectory, newDef );
	}


	/**
	 * Prepares the JSon object to inject as the new definitions in the swagger.json file.
	 * @return a non-null object
	 * @throws IOException if something failed
	 */
	public JsonObject prepareNewDefinitions() throws IOException {

		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		StringWriter writer = new StringWriter();
		JsonObject newDef = new JsonObject();

		// Copy the test application here as test-jars cannot be referenced
		// in compile scope.
		Application app = newTestApplication();

		// Create a model, as complete as possible
		app.bindWithApplication( "externalExportPrefix1", "application 1" );
		app.bindWithApplication( "externalExportPrefix1", "application 2" );
		app.bindWithApplication( "externalExportPrefix2", "application 3" );

		app.setName( "My Application with special chàràcters" );
		app.getTemplate().externalExports.put( "internalGraphVariable", "variableAlias" );
		app.getTemplate().setExternalExportsPrefix( "externalExportPrefix" );
		app.getTemplate().setDescription( "some description" );

		// Serialize things and generate the examples
		// (*) Applications
		writer = new StringWriter();
		mapper.writeValue( writer, app );
		String s = writer.toString();
		convertToTypes( s, Application.class, newDef );

		// (*) Application Templates
		writer = new StringWriter();
		mapper.writeValue( writer, app.getTemplate());
		s = writer.toString();
		convertToTypes( s, ApplicationTemplate.class, newDef );

		// (*) Component
		Instance war = InstanceHelpers.findInstanceByPath( app, "/tomcat-vm/tomcat-server/hello-world" );
		Objects.requireNonNull( war );

		writer = new StringWriter();
		mapper.writeValue( writer, war.getComponent());
		s = writer.toString();
		convertToTypes( s, Component.class, newDef );

		// (*) Instance
		writer = new StringWriter();
		mapper.writeValue( writer, war );
		s = writer.toString();
		convertToTypes( s, Instance.class, newDef );

		// (*) Diagnostics
		Diagnostic diag = new Diagnostic( "/vm1/server1" );
		DependencyInformation di = new DependencyInformation( "facetOrComponentName", true, false );
		diag.getDependenciesInformation().add( di );

		writer = new StringWriter();
		mapper.writeValue( writer, diag );
		s = writer.toString();
		convertToTypes( s, Diagnostic.class, newDef );

		writer = new StringWriter();
		mapper.writeValue( writer, di );
		s = writer.toString();
		convertToTypes( s, DependencyInformation.class, newDef );

		// (*) Target descriptors
		TargetWrapperDescriptor twd = new TargetWrapperDescriptor();
		twd.setId( "target-id" );
		twd.setDescription( "some description" );
		twd.setHandler( "iaas-ec2" );
		twd.setName( "target name" );
		twd.setDefault( true );

		writer = new StringWriter();
		mapper.writeValue( writer, twd );
		s = writer.toString();
		convertToTypes( s, TargetWrapperDescriptor.class, newDef );

		// (*) Target usage items
		TargetUsageItem tui = new TargetUsageItem();
		tui.setName( "app or template name" );
		tui.setVersion( "template qualifier (null for applications)" );
		tui.setReferencing( true );
		tui.setUsing( true );

		writer = new StringWriter();
		mapper.writeValue( writer, tui );
		s = writer.toString();
		convertToTypes( s, TargetUsageItem.class, newDef );

		// (*) Target associations
		TargetAssociation ta = new TargetAssociation( "/vm-1", "VM", twd );

		writer = new StringWriter();
		mapper.writeValue( writer, ta );
		s = writer.toString();
		convertToTypes( s, TargetAssociation.class, newDef );

		// (*) Preferences
		Preference pref = new Preference( "key", "value", PreferenceKeyCategory.AUTONOMIC );

		writer = new StringWriter();
		mapper.writeValue( writer, pref );
		s = writer.toString();
		convertToTypes( s, Preference.class, newDef );

		// (*) Scheduled jobs
		ScheduledJob job = new ScheduledJob();
		job.setAppName( "application name" );
		job.setCmdName( "command name" );
		job.setCron( "0 0 12 ? * WED" );
		job.setJobId( "job-id" );
		job.setJobName( "job name" );

		writer = new StringWriter();
		mapper.writeValue( writer, job );
		s = writer.toString();
		convertToTypes( s, ScheduledJob.class, newDef );

		return newDef;
	}


	/**
	 * @param newDef the new "definitions" object
	 * @throws IOException if something went wrong
	 */
	private void updateSwaggerJson( File baseDirectory, JsonObject newDef ) throws IOException {

		File f = new File( baseDirectory, "target/docs/apidocs/ui/swagger.json" );
		if( ! f.exists())
			throw new RuntimeException( "The swagger.json file was not found." );

		JsonParser jsonParser = new JsonParser();
		String content = Utils.readFileContent( f );

		// Hack: remove useless parts.
		// For some operations, Enunciate indicates the return type is "file", which is wrong.
		// Empty types, empty schemas and empty headers are useless too.
		content = content.replaceAll( "\"type\"\\s*:\\s*\"file\",?", "" );
		content = content.replaceAll( "\"type\"\\s*:\\s*\"\",?", "" );
		content = content.replaceAll( "\"headers\"\\s*:\\s*\\{\\},?", "" );
		content = content.replaceAll( "\"schema\": \\{\\s*\"description\"\\s*:\\s*\"\"\\s*\\s*},?", "" );
		content = content.replaceAll( ",\\s+(\n[ \t]+\\})", "$1" );
		// Hack

		// Hack: arrays of arrays are a non-sense (Enunciate bug?)
		StringBuilder sb = new StringBuilder();
		sb.append( "(\\s*\"items\": \\{\n)" );
		sb.append( "\\s*\"type\": \"array\",\n" );
		sb.append( "\\s*\"items\": \\{\n" );
		sb.append( "(\\s*\"\\$ref\": \"[^\"]+\"\n)" );
		sb.append( "\\s*\\}" );

		content = content.replaceAll( sb.toString(), "$1$2" );
		// Hack

		JsonElement jsonTree = jsonParser.parse( content );
		Set<String> currentTypes = new HashSet<> ();
		for( Map.Entry<String,JsonElement> entry : jsonTree.getAsJsonObject().get( "definitions" ).getAsJsonObject().entrySet()) {
			currentTypes.add( entry.getKey());
		}

		Set<String> newTypes = new HashSet<> ();
		for( Map.Entry<String,JsonElement> entry : newDef.entrySet()) {
			newTypes.add( entry.getKey());
		}

		currentTypes.removeAll( newTypes );
		for( String s : currentTypes ) {
			System.out.println( "Type not appearing in the updated swagger definitions: " + s );
		}

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		jsonTree.getAsJsonObject().add( "definitions", jsonParser.parse( gson.toJson( newDef )));
		String json = gson.toJson( jsonTree );
		Utils.writeStringInto( json, f );
	}


	/**
	 * Creates a JSon object from a serialization result.
	 * @param serialization the serialization result
	 * @param clazz the class for which this serialization was made
	 * @param newDef the new definition object to update
	 */
	public void convertToTypes( String serialization, Class<?> clazz, JsonObject newDef ) {
		convertToTypes( serialization, clazz.getSimpleName(), newDef );
		this.processedClasses.add( clazz );
	}


	/**
	 * Creates a JSon object from a serialization result.
	 * @param serialization the serialization result
	 * @param className a class or type name
	 * @param newDef the new definition object to update
	 */
	public static void convertToTypes( String serialization, String className, JsonObject newDef ) {

		JsonParser jsonParser = new JsonParser();
		JsonElement jsonTree = jsonParser.parse( serialization );

		// Creating the swagger definition
		JsonObject innerObject = new JsonObject();

		// Start adding basic properties
		innerObject.addProperty( "title", className );
		innerObject.addProperty( "definition", "" );
		innerObject.addProperty( "type", jsonTree.isJsonObject() ? "object" : jsonTree.isJsonArray() ? "array" : "string" );

		// Prevent errors with classic Swagger UI
		innerObject.addProperty( "properties", "" );

		// Inner properties
		innerObject.add( "example", jsonTree.getAsJsonObject());

		// Update our global definition
		newDef.add( "json_" + className, innerObject );
	}


	/**
	 * @return a new test application
	 */
	static Application newTestApplication() {

		ApplicationTemplate tpl = new ApplicationTemplate();
		tpl.setName( "test-app" );
		tpl.setVersion( "test" );

		// Root instances
		Component vmComponent = new Component( "vm" ).installerName( Constants.TARGET_INSTALLER );
		Instance tomcatVm = new Instance( "tomcat-vm" ).component( vmComponent );
		Instance mySqlVm = new Instance( "mysql-vm" ).component( vmComponent );

		// Children instances
		Component tomcatComponent = new Component( "tomcat" ).installerName( "puppet" );
		Instance tomcat = new Instance( "tomcat-server" ).component( tomcatComponent );

		Component mySqlComponent = new Component( "mysql" ).installerName( "puppet" );
		mySqlComponent.addExportedVariable( new ExportedVariable( "port", "3306" ));
		mySqlComponent.addExportedVariable( new ExportedVariable( "ip", null ));
		Instance mySql = new Instance( "mysql-server" ).component( mySqlComponent );

		Component warComponent = new Component( "war" ).installerName( "script" );
		warComponent.addExportedVariable( new ExportedVariable( "port", "8080" ));
		warComponent.addExportedVariable( new ExportedVariable( "ip", null ));
		warComponent.addImportedVariable( new ImportedVariable( "mysql.port", false, false ));
		warComponent.addImportedVariable( new ImportedVariable( "mysql.ip", false, false ));
		Instance war = new Instance( "hello-world" ).component( warComponent );

		// Make the glue
		InstanceHelpers.insertChild( tomcatVm, tomcat );
		InstanceHelpers.insertChild( tomcat, war );
		InstanceHelpers.insertChild( mySqlVm, mySql );

		vmComponent.addChild( mySqlComponent );
		vmComponent.addChild( tomcatComponent );
		tomcatComponent.addChild( warComponent );

		tpl.setGraphs( new Graphs());
		tpl.getGraphs().getRootComponents().add( vmComponent );
		tpl.getRootInstances().add( mySqlVm );
		tpl.getRootInstances().add( tomcatVm );

		// Create the application
		Application app = new Application( "test", tpl );
		return app;
	}
}
