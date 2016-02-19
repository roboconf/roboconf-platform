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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Preference;
import net.roboconf.core.model.runtime.TargetAssociation;
import net.roboconf.core.model.runtime.TargetUsageItem;
import net.roboconf.core.model.runtime.TargetWrapperDescriptor;
import net.roboconf.core.utils.IconUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.rest.commons.Diagnostic;
import net.roboconf.dm.rest.commons.Diagnostic.DependencyInformation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
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

	private static final String NAME = "name";
	private static final String QUALIFIER = "qualifier";
	private static final String DESC = "desc";
	private static final String EEP = "eep";
	private static final String EXT_VARS = "extVars";
	private static final String S = "s";
	private static final String PATH = "path";

	private static final String APP_ICON = "icon";
	private static final String APP_INFO = "info";

	private static final String APP_INST_TPL_NAME = "tplName";
	private static final String APP_INST_TPL_QUALIFIER = "tplQualifier";
	private static final String APP_INST_TPL_EEP = "tplEep";

	private static final String APP_TPL_APPS = "apps";
	private static final String COMP_INSTALLER = "installer";

	private static final String INST_CHANNELS = "channels";
	private static final String INST_COMPONENT = "component";
	private static final String INST_STATUS = "status";
	private static final String INST_EXPORTS = "exports";
	private static final String INST_DATA = "data";

	private static final String DEP_OPTIONAL = "optional";
	private static final String DEP_RESOLVED = "resolved";

	private static final String DIAG_PATH = "path";
	private static final String DIAG_DEPENDENCIES = "dependencies";

	private static final String TARGET_ID = "id";
	private static final String TARGET_HANDLER = "handler";
	private static final String TARGET_DEFAULT = "default";

	private static final String TARGET_STATS_USING = "using";
	private static final String TARGET_STATS_REFERENCING = "referencing";

	private static final String PREF_VALUE = "value";
	private static final String PREF_CATEGORY = "category";


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

		module.addSerializer( ApplicationTemplate.class, new ApplicationTemplateSerializer());
		module.addDeserializer( ApplicationTemplate.class, new ApplicationTemplateDeserializer());

		module.addSerializer( Application.class, new ApplicationSerializer());
		module.addDeserializer( Application.class, new ApplicationDeserializer());

		module.addSerializer( Component.class, new ComponentSerializer());
		module.addDeserializer( Component.class, new ComponentDeserializer());

		module.addSerializer( Diagnostic.class, new DiagnosticSerializer());
		module.addDeserializer( Diagnostic.class, new DiagnosticDeserializer());

		module.addSerializer( DependencyInformation.class, new DependencyInformationSerializer());
		module.addDeserializer( DependencyInformation.class, new DependencyInformationDeserializer());

		module.addSerializer( TargetWrapperDescriptor.class, new TargetWDSerializer());
		module.addDeserializer( TargetWrapperDescriptor.class, new TargetWDDeserializer());

		module.addSerializer( StringWrapper.class, new StringWrapperSerializer());
		module.addDeserializer( StringWrapper.class, new StringWrapperDeserializer());

		module.addSerializer( MapWrapper.class, new MapWrapperSerializer());
		module.addDeserializer( MapWrapper.class, new MapWrapperDeserializer());

		module.addSerializer( TargetUsageItem.class, new TargetUsageItemSerializer());
		module.addSerializer( TargetAssociation.class, new TargetAssociationSerializer());
		module.addSerializer( Preference.class, new PreferenceSerializer());

		mapper.registerModule( module );
		return mapper;
	}


	/**
	 * A JSon serializer for a bean describing a target usage.
	 * <p>
	 * No deserializer is provided, as it does not make sense for the REST API.
	 * </p>
	 *
	 * @author Vincent Zurczak - Linagora
	 */
	public static class TargetUsageItemSerializer extends JsonSerializer<TargetUsageItem> {

		@Override
		public void serialize(
				TargetUsageItem item,
				JsonGenerator generator,
				SerializerProvider provider )
		throws IOException {

			generator.writeStartObject();
			if( item.getName() != null )
				generator.writeStringField( NAME, item.getName());

			if( item.getQualifier() != null )
				generator.writeStringField( QUALIFIER, item.getQualifier());

			if( item.isUsing())
				generator.writeStringField( TARGET_STATS_USING, "true" );

			if( item.isReferencing())
				generator.writeStringField( TARGET_STATS_REFERENCING, "true" );

			generator.writeEndObject();
		}
	}


	/**
	 * A JSon serializer for a bean describing a target association.
	 * <p>
	 * No deserializer is provided, as it does not make sense for the REST API.
	 * </p>
	 *
	 * @author Vincent Zurczak - Linagora
	 */
	public static class TargetAssociationSerializer extends JsonSerializer<TargetAssociation> {

		@Override
		public void serialize(
				TargetAssociation item,
				JsonGenerator generator,
				SerializerProvider provider )
		throws IOException {

			generator.writeStartObject();
			if( item.getInstancePath() != null )
				generator.writeStringField( PATH, item.getInstancePath());

			if( item.getTargetDescriptor() != null )
				generator.writeObjectField( DESC, item.getTargetDescriptor());

			generator.writeEndObject();
		}
	}


	/**
	 * A JSon serializer for Roboconf preferences.
	 * <p>
	 * No deserializer is provided, as it does not make sense for the REST API.
	 * </p>
	 *
	 * @author Vincent Zurczak - Linagora
	 */
	public static class PreferenceSerializer extends JsonSerializer<Preference> {

		@Override
		public void serialize(
				Preference item,
				JsonGenerator generator,
				SerializerProvider provider )
		throws IOException {

			generator.writeStartObject();
			if( item.getName() != null )
				generator.writeStringField( NAME, item.getName());

			if( item.getValue() != null )
				generator.writeObjectField( PREF_VALUE, item.getValue());

			if( item.getCategory() != null )
				generator.writeObjectField( PREF_CATEGORY, item.getCategory().toString().toLowerCase());

			generator.writeEndObject();
		}
	}


	/**
	 * A JSon serializer for target descriptors.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class TargetWDSerializer extends JsonSerializer<TargetWrapperDescriptor> {

		@Override
		public void serialize(
				TargetWrapperDescriptor twd,
				JsonGenerator generator,
				SerializerProvider provider )
		throws IOException {

			generator.writeStartObject();
			if( twd.getId() != null )
				generator.writeStringField( TARGET_ID, twd.getId());

			if( twd.getName() != null )
				generator.writeStringField( NAME, twd.getName());

			if( twd.getHandler() != null )
				generator.writeStringField( TARGET_HANDLER, twd.getHandler());

			if( twd.getDescription() != null )
				generator.writeStringField( DESC, twd.getDescription());

			if( twd.isDefault())
				generator.writeStringField( TARGET_DEFAULT, "true" );

			generator.writeEndObject();
		}
	}


	/**
	 * A JSon deserializer for target descriptors.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class TargetWDDeserializer extends JsonDeserializer<TargetWrapperDescriptor> {

		@Override
		public TargetWrapperDescriptor deserialize( JsonParser parser, DeserializationContext context ) throws IOException {

			ObjectCodec oc = parser.getCodec();
	        JsonNode node = oc.readTree( parser );
	        TargetWrapperDescriptor twd = new TargetWrapperDescriptor();

	        JsonNode n;
	        if(( n = node.get( DESC )) != null )
	        	twd.setDescription( n.textValue());

	        if(( n = node.get( TARGET_HANDLER )) != null )
	        	twd.setHandler( n.textValue());

	        if(( n = node.get( TARGET_ID )) != null )
	        	twd.setId( n.textValue());

	        if(( n = node.get( NAME )) != null )
	        	twd.setName( n.textValue());

			return twd;
		}
	}


	/**
	 * A JSon deserializer for string wrappers.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class StringWrapperDeserializer extends JsonDeserializer<StringWrapper> {

		@Override
		public StringWrapper deserialize( JsonParser parser, DeserializationContext context ) throws IOException {

			ObjectCodec oc = parser.getCodec();
	        JsonNode node = oc.readTree( parser );
	        String s = null;

	        JsonNode n;
	        if(( n = node.get( S )) != null )
	        	s = n.textValue();

			return new StringWrapper( s );
		}
	}


	/**
	 * A JSon serializer for string wrappers.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class StringWrapperSerializer extends JsonSerializer<StringWrapper> {

		@Override
		public void serialize(
				StringWrapper s,
				JsonGenerator generator,
				SerializerProvider provider )
		throws IOException {

			generator.writeStartObject();
			if( s.toString() != null )
				generator.writeStringField( S, s.toString());

			generator.writeEndObject();
		}
	}


	/**
	 * A JSon deserializer for map wrappers.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class MapWrapperDeserializer extends JsonDeserializer<MapWrapper> {

		@Override
		public MapWrapper deserialize( JsonParser parser, DeserializationContext context ) throws IOException {

			ObjectCodec oc = parser.getCodec();
	        JsonNode node = oc.readTree( parser );
	        Map<String,String> map = new HashMap<> ();

	        for( Iterator<Map.Entry<String,JsonNode>> it = node.fields(); it.hasNext(); ) {
	        	Map.Entry<String,JsonNode> entry = it.next();
	        	map.put( entry.getKey(), entry.getValue().textValue());
	        }

			return new MapWrapper( map );
		}
	}


	/**
	 * A JSon serializer for map wrappers.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class MapWrapperSerializer extends JsonSerializer<MapWrapper> {

		@Override
		public void serialize(
				MapWrapper m,
				JsonGenerator generator,
				SerializerProvider provider )
		throws IOException {

			generator.writeStartObject();
			for( Map.Entry<String,String> entry : m.getMap().entrySet()) {
				generator.writeStringField(
						entry.getKey() == null ? "" : entry.getKey(),
						entry.getValue() == null ? "" : entry.getValue());
			}

			generator.writeEndObject();
		}
	}


	/**
	 * A JSon serializer for application templates.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class ApplicationTemplateSerializer extends JsonSerializer<ApplicationTemplate> {

		@Override
		public void serialize(
				ApplicationTemplate app,
				JsonGenerator generator,
				SerializerProvider provider )
		throws IOException {

			generator.writeStartObject();
			if( app.getName() != null )
				generator.writeStringField( NAME, app.getName());

			if( app.getDescription() != null )
				generator.writeStringField( DESC, app.getDescription());

			if( app.getQualifier() != null )
				generator.writeStringField( QUALIFIER, app.getQualifier());

			if( app.getExternalExportsPrefix() != null )
				generator.writeStringField( EEP, app.getExternalExportsPrefix());

			// Read-only information.
			// We do not expect it for deserialization
			if( ! app.externalExports.isEmpty())
				generator.writeObjectField( EXT_VARS, new MapWrapper( app.externalExports ));

			// Read-only information.
			// We do not expect it for deserialization
			String iconLocation = IconUtils.findIconUrl( app );
			if( ! Utils.isEmptyOrWhitespaces( iconLocation ))
				generator.writeStringField( APP_ICON, iconLocation );

			// Read-only information.
			// We do not expect it for deserialization
			generator.writeArrayFieldStart( APP_TPL_APPS );
			for( Application associatedApp : app.getAssociatedApplications()) {

				// #483 We do not know why, but after we delete an application
				// from the web console, the resulting JSon array sometimes contain null.
				// This prevents the deletion of a template that does not have applications anymore.
				// The "IF" is a WORKAROUND.
				if( associatedApp != null
						&& associatedApp.getName() != null ) {
					// end of WORKAROUND
					generator.writeString( associatedApp.getName());
				}
			}

			generator.writeEndArray();
			generator.writeEndObject();
		}
	}


	/**
	 * A JSon deserializer for application templates.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class ApplicationTemplateDeserializer extends JsonDeserializer<ApplicationTemplate> {

		@Override
		public ApplicationTemplate deserialize( JsonParser parser, DeserializationContext context ) throws IOException {

			ObjectCodec oc = parser.getCodec();
			JsonNode node = oc.readTree( parser );
			ApplicationTemplate application = new ApplicationTemplate();

			JsonNode n;
			if(( n = node.get( NAME )) != null )
				application.setName( n.textValue());

			if(( n = node.get( DESC )) != null )
				application.setDescription( n.textValue());

			if(( n = node.get( QUALIFIER )) != null )
				application.setQualifier( n.textValue());

			if(( n = node.get( EEP )) != null )
				application.setExternalExportsPrefix( n.textValue());

			return application;
		}
	}


	/**
	 * A JSon serializer for diagnostics.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class DiagnosticSerializer extends JsonSerializer<Diagnostic> {

		@Override
		public void serialize(
				Diagnostic diag,
				JsonGenerator generator,
				SerializerProvider provider )
		throws IOException {

			generator.writeStartObject();
			if( diag.getInstancePath() != null )
				generator.writeStringField( DIAG_PATH, diag.getInstancePath());

			generator.writeArrayFieldStart( DIAG_DEPENDENCIES );
			for( DependencyInformation info : diag.getDependenciesInformation())
				generator.writeObject( info );
			generator.writeEndArray();

			generator.writeEndObject();
		}
	}


	/**
	 * A JSon deserializer for diagnostics.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class DiagnosticDeserializer extends JsonDeserializer<Diagnostic> {

		@Override
		public Diagnostic deserialize( JsonParser parser, DeserializationContext context ) throws IOException {

			ObjectCodec oc = parser.getCodec();
	        JsonNode node = oc.readTree( parser );
	        Diagnostic diag = new Diagnostic();

	        JsonNode n;
	        if(( n = node.get( DIAG_PATH )) != null )
	        	diag.setInstancePath( n.textValue());

	        if(( n = node.get( DIAG_DEPENDENCIES )) != null ) {
	        	for( JsonNode arrayNodeItem : n ) {
	        		ObjectMapper mapper = createObjectMapper();
		        	DependencyInformation info = mapper.readValue( arrayNodeItem.toString(), DependencyInformation.class );
	        		diag.getDependenciesInformation().add( info );
	        	}
	        }

			return diag;
		}
	}


	/**
	 * A JSon serializer for dependencies information.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class DependencyInformationSerializer extends JsonSerializer<DependencyInformation> {

		@Override
		public void serialize(
				DependencyInformation info,
				JsonGenerator generator,
				SerializerProvider provider )
		throws IOException {

			generator.writeStartObject();
			if( info.getDependencyName() != null )
				generator.writeStringField( NAME, info.getDependencyName());

			generator.writeStringField( DEP_OPTIONAL, String.valueOf( info.isOptional()));
			generator.writeStringField( DEP_RESOLVED, String.valueOf( info.isResolved()));

			generator.writeEndObject();
		}
	}


	/**
	 * A JSon deserializer for dependencies information.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class DependencyInformationDeserializer extends JsonDeserializer<DependencyInformation> {

		@Override
		public DependencyInformation deserialize( JsonParser parser, DeserializationContext context ) throws IOException {

			ObjectCodec oc = parser.getCodec();
	        JsonNode node = oc.readTree( parser );
	        DependencyInformation info = new DependencyInformation();

	        JsonNode n;
	        if(( n = node.get( NAME )) != null )
	        	info.setDependencyName( n.textValue());

	        if(( n = node.get( DEP_OPTIONAL )) != null )
	        	info.setOptional( Boolean.valueOf( n.textValue()));

	        if(( n = node.get( DEP_RESOLVED )) != null )
	        	info.setResolved( Boolean.valueOf( n.textValue()));

			return info;
		}
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
				generator.writeStringField( NAME, app.getName());

			if( app.getDescription() != null )
				generator.writeStringField( DESC, app.getDescription());

			if( app.getTemplate() != null ) {
				if( app.getTemplate().getName() != null )
					generator.writeStringField( APP_INST_TPL_NAME, app.getTemplate().getName());

				if( app.getTemplate().getQualifier() != null )
					generator.writeStringField( APP_INST_TPL_QUALIFIER, app.getTemplate().getQualifier());

				if( app.getTemplate().getExternalExportsPrefix() != null )
					generator.writeStringField( APP_INST_TPL_EEP, app.getTemplate().getExternalExportsPrefix());

				// Read-only information.
				// We do not expect it for deserialization
				Map<String,String> externalExports = app.getTemplate().externalExports;
				if( ! externalExports.isEmpty())
					generator.writeObjectField( EXT_VARS, new MapWrapper( externalExports ));
			}

			// Read-only information.
			// We do not expect it for deserialization
			String iconLocation = IconUtils.findIconUrl( app );
			if( ! Utils.isEmptyOrWhitespaces( iconLocation ))
				generator.writeStringField( APP_ICON, iconLocation );

			// #357 Add a state for applications in JSon objects
			String info = null;
			for( Instance rootInstance : app.getRootInstances()) {
				if( rootInstance.getStatus() == InstanceStatus.PROBLEM ) {
					info = "warn";
					break;
				}

				if( rootInstance.getStatus() != InstanceStatus.NOT_DEPLOYED ) {
					info = "ok";
					break;
				}
			}

	        if( info != null )
	        	generator.writeObjectField( APP_INFO, info );

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

	        Application application;
	        JsonNode n;
	        if(( n = node.get( APP_INST_TPL_NAME )) != null ) {
	        	ApplicationTemplate appTemplate = new ApplicationTemplate();
	        	appTemplate.setName( n.textValue());

	        	n = node.get( APP_INST_TPL_QUALIFIER );
	        	if( n != null )
	        		appTemplate.setQualifier( n.textValue());

	        	n = node.get( APP_INST_TPL_EEP );
	        	if( n != null )
	        		appTemplate.setExternalExportsPrefix( n.textValue());

	        	application = new Application( appTemplate );

	        } else {
	        	application = new Application( null );
	        }

	        if(( n = node.get( NAME )) != null )
	        	application.setName( n.textValue());

	        if(( n = node.get( DESC )) != null )
	        	application.setDescription( n.textValue());

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
				generator.writeStringField( NAME, instance.getName());
				generator.writeStringField( PATH, InstanceHelpers.computeInstancePath( instance ));
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

			// All exports are serialized in the same object (overridden or not).
			// Will be necessary in external apps, like web console (eg. to edit exports).
			Map<String, String> exports = InstanceHelpers.findAllExportedVariables(instance);
			if( ! exports.isEmpty()) {
				generator.writeFieldName( INST_EXPORTS );
				generator.writeStartObject();
				for( Map.Entry<String,String> entry : exports.entrySet())
					generator.writeObjectField( entry.getKey(), entry.getValue());

				generator.writeEndObject();
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
	        if(( n = node.get( NAME )) != null )
	        	instance.setName( n.textValue());

	        if(( n = node.get( INST_STATUS )) != null )
	        	instance.setStatus( InstanceStatus.whichStatus( n.textValue()));

	        if(( n = node.get( INST_CHANNELS )) != null ) {
	        	for( JsonNode arrayNodeItem : n )
	        		instance.channels.add( arrayNodeItem.textValue());
	        }

	        ObjectMapper mapper = createObjectMapper();

	        // Consider all exports as overridden. This will be fixed later
	        // (eg. by comparison with component exports).
	        if(( n = node.get( INST_EXPORTS )) != null ) {
	        	Map<String, String> exports = mapper.readValue(n.toString(), new TypeReference<HashMap<String,String>>(){});
	        	instance.overriddenExports.putAll(exports);
	        }

	        if(( n = node.get( INST_COMPONENT )) != null ) {
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
				generator.writeStringField( NAME, component.getName());

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
	        if(( n = node.get( NAME )) != null )
	        	component.setName( n.textValue());

	        if(( n = node.get( COMP_INSTALLER )) != null )
	        	component.setInstallerName( n.textValue());

			return component;
		}
	}
}
