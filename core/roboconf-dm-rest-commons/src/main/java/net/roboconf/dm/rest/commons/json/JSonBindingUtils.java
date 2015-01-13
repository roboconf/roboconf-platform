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

import java.io.IOException;
import java.util.Map;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * A set of utilities to bind Roboconf's runtime model to JSon.
 * @author Vincent Zurczak - Linagora
 */
public final class JSonBindingUtils {

	private static final String APP_NAME = "name";
	private static final String APP_DESC = "desc";
	private static final String APP_QUALIFIER = "qualifier";
	private static final String APP_NAMESPACE = "namespace";

	private static final String INST_NAME = "name";
	private static final String INST_PATH = "path";
	private static final String INST_CHANNELS = "channels";
	private static final String INST_COMPONENT = "component";
	private static final String INST_STATUS = "status";
	private static final String INST_DATA = "data";

	private static final String COMP_NAME = "name";
	private static final String COMP_INSTALLER = "installer";


	/**
	 * Private constructor.
	 */
	private JSonBindingUtils() {
		// nothing
	}


	/**
	 * Creates a mapper with specific binding for Roboconf types.
	 * @return a non-null, configured mapper
	 */
	public static ObjectMapper createObjectMapper() {

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );

		SimpleModule module = new SimpleModule( "RoboconfModule", new Version( 1, 0, 0, null, null, null ));

		module.addSerializer( Instance.class, new InstanceSerializer());
		module.addDeserializer( Instance.class, new InstanceDeserializer());

		module.addSerializer( Application.class, new ApplicationSerializer());
		module.addDeserializer( Application.class, new ApplicationDeserializer());

		module.addSerializer( Component.class, new ComponentSerializer());
		module.addDeserializer( Component.class, new ComponentDeserializer());

		mapper.registerModule( module );
		return mapper;
	}


	/**
	 * A JSon serializer for applications.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class ApplicationSerializer extends JsonSerializer<Application> {

		@Override
		public void serialize(
				Application app,
				JsonGenerator generator,
				SerializerProvider provider )
		throws IOException {

			generator.writeStartObject();
			if( app.getName() != null )
				generator.writeStringField( APP_NAME, app.getName());

			if( app.getDescription() != null )
				generator.writeStringField( APP_DESC, app.getDescription());

			if( app.getQualifier() != null )
				generator.writeStringField( APP_QUALIFIER, app.getQualifier());

			if( app.getNamespace() != null )
				generator.writeStringField( APP_NAMESPACE, app.getNamespace());

			generator.writeEndObject();
		}
	}


	/**
	 * A JSon deserializer for applications.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class ApplicationDeserializer extends JsonDeserializer<Application> {

		@Override
		public Application deserialize( JsonParser parser, DeserializationContext context ) throws IOException {

			ObjectCodec oc = parser.getCodec();
	        JsonNode node = oc.readTree( parser );
	        Application application = new Application();

	        JsonNode n;
	        if(( n = node.get( APP_NAME )) != null )
	        	application.setName( n.textValue());

	        if(( n = node.get( APP_DESC )) != null )
	        	application.setDescription( n.textValue());

	        if(( n = node.get( APP_QUALIFIER )) != null )
	        	application.setQualifier( n.textValue());

	        if(( n = node.get( APP_NAMESPACE )) != null )
	        	application.setNamespace( n.textValue());

			return application;
		}
	}


	/**
	 * A JSon serializer for instances.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class InstanceSerializer extends JsonSerializer<Instance> {

		@Override
		public void serialize(
				Instance instance,
				JsonGenerator generator,
				SerializerProvider provider )
		throws IOException {

			generator.writeStartObject();
			if( instance.getName() != null ) {
				generator.writeStringField( INST_NAME, instance.getName());
				generator.writeStringField( INST_PATH, InstanceHelpers.computeInstancePath( instance ));
			}

			if( instance.getStatus() != null )
				generator.writeStringField( INST_STATUS, String.valueOf( instance.getStatus()));

			if( instance.getComponent() != null )
				generator.writeObjectField( INST_COMPONENT, instance.getComponent());

			if( ! instance.channels.isEmpty()) {
				generator.writeArrayFieldStart( INST_CHANNELS );
				for( String channel : instance.channels )
					generator.writeString( channel );

				generator.writeEndArray();
			}

			// Write some meta-data (useful for web clients).
			// De-serializing this information is useless for the moment.
			if( ! instance.data.isEmpty()) {

				generator.writeFieldName( INST_DATA );
				generator.writeStartObject();
				for( Map.Entry<String,String> entry : instance.data.entrySet())
					generator.writeObjectField( entry.getKey(), entry.getValue());

				generator.writeEndObject();
			}

			generator.writeEndObject();
		}
	}


	/**
	 * A JSon deserializer for instances.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class InstanceDeserializer extends JsonDeserializer<Instance> {

		@Override
		public Instance deserialize( JsonParser parser, DeserializationContext context ) throws IOException {

			ObjectCodec oc = parser.getCodec();
	        JsonNode node = oc.readTree( parser );
	        Instance instance = new Instance();

	        JsonNode n;
	        if(( n = node.get( INST_NAME )) != null )
	        	instance.setName( n.textValue());

	        if(( n = node.get( INST_STATUS )) != null )
	        	instance.setStatus( InstanceStatus.whichStatus( n.textValue()));

	        if(( n = node.get( INST_CHANNELS )) != null ) {
	        	for( JsonNode arrayNodeItem : n )
	        		instance.channels.add( arrayNodeItem.textValue());
	        }

	        if(( n = node.get( INST_COMPONENT )) != null ) {
	        	ObjectMapper mapper = createObjectMapper();
	        	Component instanceComponent = mapper.readValue( n.toString(), Component.class );
	        	instance.setComponent( instanceComponent );
	        }

			return instance;
		}
	}


	/**
	 * A JSon serializer for components.
	 * <p>
	 * Only the name and alias are serialized.
	 * </p>
	 *
	 * @author Vincent Zurczak - Linagora
	 */
	public static class ComponentSerializer extends JsonSerializer<Component> {

		@Override
		public void serialize(
				Component component,
				JsonGenerator generator,
				SerializerProvider provider )
		throws IOException {

			generator.writeStartObject();
			if( component.getName() != null )
				generator.writeStringField( COMP_NAME, component.getName());

			if( component.getInstallerName() != null )
				generator.writeStringField( COMP_INSTALLER, component.getInstallerName());

			generator.writeEndObject();
		}
	}


	/**
	 * A JSon deserializer for components.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class ComponentDeserializer extends JsonDeserializer<Component> {

		@Override
		public Component deserialize( JsonParser parser, DeserializationContext context ) throws IOException {

			ObjectCodec oc = parser.getCodec();
	        JsonNode node = oc.readTree( parser );
	        Component component = new Component();

	        JsonNode n;
	        if(( n = node.get( COMP_NAME )) != null )
	        	component.setName( n.textValue());

	        if(( n = node.get( COMP_INSTALLER )) != null )
	        	component.setInstallerName( n.textValue());

			return component;
		}
	}
}
