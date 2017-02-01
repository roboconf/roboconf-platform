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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import io.swagger.models.Contact;
import io.swagger.models.Info;
import io.swagger.models.License;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.properties.RefProperty;
import io.swagger.util.Json;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.EventType;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.rest.commons.beans.WebSocketMessage;
import net.roboconf.dm.rest.commons.json.JSonBindingUtils;
import net.roboconf.dm.rest.services.internal.ServletRegistrationComponent;

/**
 * @author Vincent Zurczak - Linagora
 */
public class GenerateSwaggerJsonForWebSockets {

	private static final String INSTANCE = "instance";
	private static final String APPLICATION = "application";
	private static final String APPLICATION_TEMPLATE = "applicationTemplate";
	private static final String TEXT = "text";

	static final Map<String,String> NAME_TO_DESC = new TreeMap<> ();
	static final Map<String,String> NAME_TO_PARAM = new HashMap<> ();
	static {
		NAME_TO_DESC.put( INSTANCE, "Internal method invoked when an instance is created, deleted or modified." );
		NAME_TO_DESC.put( APPLICATION, "Internal method invoked when an application is created, deleted or modified." );
		NAME_TO_DESC.put( APPLICATION_TEMPLATE, "Internal method invoked when an application template is created, deleted or modified." );
		NAME_TO_DESC.put( TEXT, "Internal method invoked when a raw text message must be propagated." );

		StringBuilder sb = new StringBuilder();
		sb.append( "The instance that was created, deleted or modified, as well as the context.\n" );
		sb.append( "The context includes the application the instance belongs to.\n" );
		sb.append( "It also includes the event type: CREATED, DELETED, CHANGED.\n" );
		NAME_TO_PARAM.put( INSTANCE, sb.toString());

		sb = new StringBuilder();
		sb.append( "The application that was created, deleted or modified, as well as the context." );
		sb.append( "The context includes the event type: CREATED, DELETED, CHANGED.\n" );
		NAME_TO_PARAM.put( APPLICATION, sb.toString());

		sb = new StringBuilder();
		sb.append( "The application template that was created, deleted or modified, as well as the context.\n" );
		sb.append( "The context includes the event type: CREATED, DELETED, CHANGED.\n" );
		NAME_TO_PARAM.put( APPLICATION_TEMPLATE, sb.toString());

		sb = new StringBuilder();
		sb.append( "The message that was pushed directly from the DM.\n" );
		NAME_TO_PARAM.put( TEXT, sb.toString());
	}


	/**
	 * @param args
	 */
	public static void main( String[] args ) {

		try {
			new GenerateSwaggerJsonForWebSockets().run( args );

		} catch( Exception e ) {
			e.printStackTrace();
			System.exit( 5 );
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
		if( args.length != 2
				|| ! (baseDirectory = new File( args[ 0 ])).exists())
			throw new RuntimeException( "The path of the module's directory was expected as an argument." );

		// Generate
		File f = new File( baseDirectory, "target/docs/apidocs/ui/swagger-websocket.json" );
		generate( args[1], f );
	}


	void generate( String roboconfVersion, File jsonFile ) throws Exception {

		// Global model
		Swagger swagger = new Swagger();
		swagger.setBasePath( ServletRegistrationComponent.WEBSOCKET_CONTEXT );
		swagger.setHost( "localhost:8181" );

		Tag tag = new Tag(  );
		tag.setName( "FromServer" );
		swagger.setTags( Arrays.asList( tag ));

		Contact contact = new Contact();
		contact.setName( "the Roboconf team" );
		contact.setUrl( "http://roboconf.net" );

		License license = new License();
		license.setUrl( "http://www.apache.org/licenses/LICENSE-2.0.txt" );
		license.setName( "The Apache Software License, Version 2.0" );

		Info info = new Info();
		info.setContact( contact );
		info.setLicense( license );
		info.setVersion( roboconfVersion );
		info.setTitle( "Web Socket API" );
		info.setDescription( "The Web Socket API for Roboconf's Administration" );
		swagger.setInfo( info );

		// The operations and the type definitions
		Map<String,Path> paths = new HashMap<>( NAME_TO_DESC.size());
		swagger.setPaths( paths );

		for( Map.Entry<String,String> entry : NAME_TO_DESC.entrySet()) {

			ModelImpl model = new ModelImpl();
			model.setType( ModelImpl.OBJECT );
			model.setTitle( entry.getKey());
			swagger.addDefinition( "json_" + entry.getKey(), model );

			Operation operation = new Operation();
			operation.setDescription( entry.getValue());
			operation.setSummary( entry.getValue());
			operation.setTags( Arrays.asList( tag.getName()));
			operation.setOperationId( entry.getKey());
			operation.setProduces( Arrays.asList( MediaType.APPLICATION_JSON ));

			Response response = new Response();
			response.setDescription( NAME_TO_PARAM.get( entry.getKey()));

			RefProperty schema = new RefProperty();
			schema.set$ref( "json_" + entry.getKey());
			response.setSchema( schema );
			operation.addResponse( "default", response );

			Path path = new Path();
			path.setPost( operation );
			paths.put( entry.getKey(), path );
		}

		// Write the model
		String swaggerJson = Json.pretty().writeValueAsString( swagger );

		// Update the definition in JSon directly.
		// If we set the examples as strings, they are seen as a string
		// and not as a JSon object by Swagger UI.
		Map<String,String> definitions = prepareNewDefinitions();
		JsonParser jsonParser = new JsonParser();
		JsonElement jsonTree = jsonParser.parse( swaggerJson );

		for( Map.Entry<String,String> entry : NAME_TO_DESC.entrySet()) {
			String serialization = definitions.get( entry.getKey());
			JsonElement serializationAsJson = jsonParser.parse( serialization );
			jsonTree.getAsJsonObject()
					.get( "definitions" ).getAsJsonObject()
					.get( "json_" + entry.getKey()).getAsJsonObject()
					.add( "example", serializationAsJson );
		}

		// Back to a string
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		swaggerJson = gson.toJson( jsonTree );;

		// Write the model in a file
		Utils.writeStringInto( swaggerJson, jsonFile );
	}


	/**
	 * Prepares the JSon object to inject as the new definitions in the swagger.json file.
	 * @return a non-null map (key = operation ID, value = example)
	 * @throws IOException if something went wrong
	 */
	public Map<String,String> prepareNewDefinitions() throws IOException {

		Map<String,String> result = new HashMap<> ();
		ObjectMapper mapper = JSonBindingUtils.createObjectMapper();
		StringWriter writer = new StringWriter();

		// Create a model, as complete as possible
		Application app = UpdateSwaggerJson.newTestApplication();

		// Serialize things and generate the examples
		Instance tomcat = InstanceHelpers.findInstanceByPath( app, "/tomcat-vm/tomcat-server" );
		Objects.requireNonNull( tomcat );

		WebSocketMessage msg = new WebSocketMessage( tomcat, app, EventType.CHANGED );
		writer = new StringWriter();
		mapper.writeValue( writer, msg );
		result.put( INSTANCE, writer.toString());

		msg = new WebSocketMessage( app, EventType.CREATED );
		writer = new StringWriter();
		mapper.writeValue( writer, msg );
		result.put( APPLICATION, writer.toString());

		msg = new WebSocketMessage( app.getTemplate(), EventType.DELETED );
		writer = new StringWriter();
		mapper.writeValue( writer, msg );
		result.put( APPLICATION_TEMPLATE, writer.toString());

		msg = new WebSocketMessage( "A text message." );
		writer = new StringWriter();
		mapper.writeValue( writer, msg );
		result.put( TEXT, writer.toString());

		return result;
	}
}
