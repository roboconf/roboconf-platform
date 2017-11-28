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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.model.runtime.CommandHistoryItem;
import net.roboconf.core.model.runtime.Preference;
import net.roboconf.core.model.runtime.Preference.PreferenceKeyCategory;
import net.roboconf.core.model.runtime.ScheduledJob;
import net.roboconf.core.model.runtime.TargetUsageItem;
import net.roboconf.core.model.runtime.TargetWrapperDescriptor;
import net.roboconf.core.utils.IconUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.rest.commons.Diagnostic;
import net.roboconf.dm.rest.commons.Diagnostic.DependencyInformation;
import net.roboconf.dm.rest.commons.beans.ApplicationBindings;
import net.roboconf.dm.rest.commons.beans.ApplicationBindings.ApplicationBindingItem;
import net.roboconf.dm.rest.commons.beans.TargetAssociation;
import net.roboconf.dm.rest.commons.beans.WebSocketMessage;

/**
 * A set of utilities to bind Roboconf's runtime model to JSon.
 * @author Vincent Zurczak - Linagora
 */
public final class JSonBindingUtils {

	// Annotations set by the deserializers
	public static final String AT_INSTANCE_PATH = "@instance_path";

	// We use global maps to verify some little things in tests.
	static final Map<Class<?>,JsonSerializer<?>> SERIALIZERS = new HashMap<> ();
	static final Map<Class<?>,JsonDeserializer<?>> DESERIALIZERS = new HashMap<> ();

	static {
		// Read - Write
		SERIALIZERS.put( Instance.class, new InstanceSerializer());
		DESERIALIZERS.put( Instance.class, new InstanceDeserializer());

		SERIALIZERS.put( ApplicationTemplate.class, new ApplicationTemplateSerializer());
		DESERIALIZERS.put( ApplicationTemplate.class, new ApplicationTemplateDeserializer());

		SERIALIZERS.put( Application.class, new ApplicationSerializer());
		DESERIALIZERS.put( Application.class, new ApplicationDeserializer());

		SERIALIZERS.put( Component.class, new ComponentSerializer());
		DESERIALIZERS.put( Component.class, new ComponentDeserializer());

		SERIALIZERS.put( Diagnostic.class, new DiagnosticSerializer());
		DESERIALIZERS.put( Diagnostic.class, new DiagnosticDeserializer());

		SERIALIZERS.put( DependencyInformation.class, new DependencyInformationSerializer());
		DESERIALIZERS.put( DependencyInformation.class, new DependencyInformationDeserializer());

		SERIALIZERS.put( TargetWrapperDescriptor.class, new TargetWDSerializer());
		DESERIALIZERS.put( TargetWrapperDescriptor.class, new TargetWDDeserializer());

		SERIALIZERS.put( StringWrapper.class, new StringWrapperSerializer());
		DESERIALIZERS.put( StringWrapper.class, new StringWrapperDeserializer());

		SERIALIZERS.put( MapWrapper.class, new MapWrapperSerializer());
		DESERIALIZERS.put( MapWrapper.class, new MapWrapperDeserializer());

		SERIALIZERS.put( ScheduledJob.class, new ScheduledJobSerializer());
		DESERIALIZERS.put( ScheduledJob.class, new ScheduledJobDeserializer());

		SERIALIZERS.put( Preference.class, new PreferenceSerializer());
		DESERIALIZERS.put( Preference.class, new PreferenceDeserializer());

		// Write ONLY
		SERIALIZERS.put( MappedCollectionWrapper.class, new MappedCollectionWrapperSerializer());
		SERIALIZERS.put( TargetUsageItem.class, new TargetUsageItemSerializer());
		SERIALIZERS.put( TargetAssociation.class, new TargetAssociationSerializer());
		SERIALIZERS.put( ApplicationBindings.class, new ApplicationBindingsSerializer());
		SERIALIZERS.put( WebSocketMessage.class, new WebSocketMessageSerializer());
		SERIALIZERS.put( CommandHistoryItem.class, new CommandHistoryItemSerializer());
	}


	/**
	 * @return the serializers as an unmodifiable map
	 */
	public static Map<Class<?>,? super JsonSerializer<?>> getSerializers() {
		return Collections.unmodifiableMap( SERIALIZERS );
	}


	private static final String NAME = "name";
	private static final String DISPLAY_NAME = "displayName";
	private static final String VERSION = "version";
	private static final String DESC = "desc";
	private static final String EEP = "eep";
	private static final String EXT_VARS = "extVars";
	private static final String EXT_DEP = "extDep";
	private static final String S = "s";
	private static final String PATH = "path";
	private static final String CRON = "cron";
	private static final String ID = "id";
	private static final String BOUND = "bound";

	private static final String APP_ICON = "icon";
	private static final String APP_INFO = "info";

	private static final String APP_INST_TPL_NAME = "tplName";
	private static final String APP_INST_TPL_VERSION = "tplVersion";
	private static final String APP_INST_TPL_EEP = "tplEep";

	private static final String APP_TPL_APPS = "apps";
	private static final String APP_TPL_TAGS = "tags";
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

	private static final String TARGET_HANDLER = "handler";
	private static final String TARGET_DEFAULT = "default";

	private static final String TARGET_STATS_USING = "using";
	private static final String TARGET_STATS_REFERENCING = "referencing";

	private static final String PREF_VALUE = "value";
	private static final String PREF_CATEGORY = "category";

	private static final String JOB_NAME = "job-name";
	private static final String JOB_APP_NAME = "app-name";
	private static final String JOB_CMD_NAME = "cmd-name";

	private static final String WS_EVENT = "event";
	private static final String WS_APP = "app";
	private static final String WS_TPL = "tpl";
	private static final String WS_INST = "inst";
	private static final String WS_MSG = "msg";


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
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static ObjectMapper createObjectMapper() {

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
		SimpleModule module = new SimpleModule( "RoboconfModule", new Version( 1, 0, 0, null, null, null ));

		for( Map.Entry<Class<?>,? super JsonSerializer<?>> entry : SERIALIZERS.entrySet())
			module.addSerializer((Class) entry.getKey(), (JsonSerializer) entry.getValue());

		for( Map.Entry<Class<?>,? super JsonDeserializer<?>> entry : DESERIALIZERS.entrySet())
			module.addDeserializer((Class) entry.getKey(), (JsonDeserializer) entry.getValue());

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

			if( item.getVersion() != null )
				generator.writeStringField( VERSION, item.getVersion());

			if( item.isUsing())
				generator.writeStringField( TARGET_STATS_USING, "true" );

			if( item.isReferencing())
				generator.writeStringField( TARGET_STATS_REFERENCING, "true" );

			generator.writeEndObject();
		}
	}


	/**
	 * A JSon serializer for a bean describing a scheduled job.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class ScheduledJobSerializer extends JsonSerializer<ScheduledJob> {

		@Override
		public void serialize(
				ScheduledJob job,
				JsonGenerator generator,
				SerializerProvider provider )
		throws IOException {

			generator.writeStartObject();
			if( job.getJobId() != null )
				generator.writeStringField( ID, job.getJobId());

			if( job.getAppName() != null )
				generator.writeStringField( JOB_APP_NAME, job.getAppName());

			if( job.getCmdName() != null )
				generator.writeStringField( JOB_CMD_NAME, job.getCmdName());

			if( job.getJobName() != null )
				generator.writeStringField( JOB_NAME, job.getJobName());

			if( job.getCron() != null )
				generator.writeStringField( CRON, job.getCron());

			generator.writeEndObject();
		}
	}


	/**
	 * A JSon deserializer for scheduled jobs.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class ScheduledJobDeserializer extends JsonDeserializer<ScheduledJob> {

		@Override
		public ScheduledJob deserialize( JsonParser parser, DeserializationContext context ) throws IOException {

			ObjectCodec oc = parser.getCodec();
			JsonNode node = oc.readTree( parser );
			ScheduledJob job = new ScheduledJob();

			JsonNode n;
			if(( n = node.get( ID )) != null )
				job.setJobId( n.textValue());

			if(( n = node.get( JOB_NAME )) != null )
				job.setJobName( n.textValue());

			if(( n = node.get( JOB_APP_NAME )) != null )
				job.setAppName( n.textValue());

			if(( n = node.get( JOB_CMD_NAME )) != null )
				job.setCmdName( n.textValue());

			if(( n = node.get( CRON )) != null )
				job.setCron( n.textValue());

			return job;
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
			if( item.getInstancePathOrComponentName() != null )
				generator.writeStringField( PATH, item.getInstancePathOrComponentName());

			if( item.getInstanceComponent() != null )
				generator.writeStringField( INST_COMPONENT, item.getInstanceComponent());

			if( item.getTargetDescriptor() != null )
				generator.writeObjectField( DESC, item.getTargetDescriptor());

			generator.writeEndObject();
		}
	}


	/**
	 * A JSon serializer for a bean describing a command execution.
	 * <p>
	 * No deserializer is provided, as it does not make sense for the REST API.
	 * </p>
	 *
	 * @author Vincent Zurczak - Linagora
	 */
	public static class CommandHistoryItemSerializer extends JsonSerializer<CommandHistoryItem> {

		@Override
		public void serialize(
				CommandHistoryItem item,
				JsonGenerator generator,
				SerializerProvider provider )
		throws IOException {

			generator.writeStartObject();
			if( item.getApplicationName() != null )
				generator.writeStringField( "app", item.getApplicationName());

			if( item.getCommandName() != null )
				generator.writeStringField( "cmd", item.getCommandName());

			if( item.getOriginDetails() != null )
				generator.writeStringField( "details", item.getOriginDetails());

			long startInMilliSec = item.getStart();
			long durationInMilliSec = TimeUnit.MILLISECONDS.convert( item.getDuration(), TimeUnit.NANOSECONDS );
			generator.writeNumberField( "start", startInMilliSec );
			generator.writeNumberField( "duration", durationInMilliSec );

			generator.writeNumberField( "result", item.getExecutionResult());
			generator.writeNumberField( "origin", item.getOrigin());
			generator.writeEndObject();
		}
	}


	/**
	 * A JSon serializer for Roboconf preferences.
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
	 * A JSon deserializer for Roboconf preferences.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class PreferenceDeserializer extends JsonDeserializer<Preference> {

		@Override
		public Preference deserialize( JsonParser parser, DeserializationContext context ) throws IOException {

			ObjectCodec oc = parser.getCodec();
			JsonNode node = oc.readTree( parser );
			String name = null, value = null, cat = null;

			JsonNode n;
			if(( n = node.get( NAME )) != null )
				name = n.textValue();

			if(( n = node.get( PREF_VALUE )) != null )
				value = n.textValue();

			if(( n = node.get( PREF_CATEGORY )) != null )
				cat = n.textValue();

			return new Preference( name, value, PreferenceKeyCategory.which( cat ));
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
				generator.writeStringField( ID, twd.getId());

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

			if(( n = node.get( ID )) != null )
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

			if( app.getDisplayName() != null )
				generator.writeStringField( DISPLAY_NAME, app.getDisplayName());

			if( app.getDescription() != null )
				generator.writeStringField( DESC, app.getDescription());

			if( app.getVersion() != null )
				generator.writeStringField( VERSION, app.getVersion());

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
			Set<String> prefixesForExternalImports = VariableHelpers.findPrefixesForExternalImports( app );
			if( ! prefixesForExternalImports.isEmpty()) {
				generator.writeArrayFieldStart( EXT_DEP );
				for( String prefix : prefixesForExternalImports )
					generator.writeString( prefix );

				generator.writeEndArray();
			}

			// Read-only information.
			// We do not expect it for deserialization
			generator.writeArrayFieldStart( APP_TPL_APPS );
			for( Application associatedApp : app.getAssociatedApplications()) {

				// #483 We do not know why, but after we delete an application
				// from the web console, the resulting JSon array sometimes contains null.
				// This prevents the deletion of a template that does not have applications anymore.
				// The "IF" is a WORKAROUND.
				if( associatedApp != null
						&& associatedApp.getName() != null ) {
					// end of WORKAROUND
					generator.writeString( associatedApp.getName());
				}
			}

			generator.writeEndArray();

			// Tags
			Set<String> tags = app.getTags();
			if( ! tags.isEmpty()) {
				generator.writeArrayFieldStart( APP_TPL_TAGS );
				for( String s : tags )
					generator.writeString( s );

				generator.writeEndArray();
			}

			generator.writeEndObject();
		}
	}


	/**
	 * A JSon serializer for mapped collection wrappers.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class MappedCollectionWrapperSerializer extends JsonSerializer<MappedCollectionWrapper> {

		@Override
		public void serialize(
				MappedCollectionWrapper m,
				JsonGenerator generator,
				SerializerProvider provider )
		throws IOException {

			generator.writeStartObject();
			for( Map.Entry<String,? extends Collection<String>> entry : m.getMap().entrySet()) {
				generator.writeArrayFieldStart( entry.getKey() == null ? "" : entry.getKey());

				if( entry.getValue() != null ) {
					for( String item : entry.getValue())
						generator.writeString( item );
				}

				generator.writeEndArray();
			}

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
			if(( n = node.get( DISPLAY_NAME )) != null )
				application.setName( n.textValue());
			else if(( n = node.get( NAME )) != null )
				application.setName( n.textValue());

			if(( n = node.get( DESC )) != null )
				application.setDescription( n.textValue());

			if(( n = node.get( VERSION )) != null )
				application.setVersion( n.textValue());

			if(( n = node.get( EEP )) != null )
				application.setExternalExportsPrefix( n.textValue());

			if(( n = node.get( APP_TPL_TAGS )) != null ) {
				for( JsonNode arrayNodeItem : n )
					application.addTag( arrayNodeItem.textValue());
			}

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
	 * A JSon serializer for application bindings.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class ApplicationBindingsSerializer extends JsonSerializer<ApplicationBindings> {

		@Override
		public void serialize(
				ApplicationBindings bindings,
				JsonGenerator generator,
				SerializerProvider provider )
		throws IOException {

			generator.writeStartObject();
			for( Map.Entry<String,List<ApplicationBindingItem>> entry : bindings.prefixToItems.entrySet()) {
				generator.writeArrayFieldStart( entry.getKey());

				for( ApplicationBindingItem item : entry.getValue()) {
					generator.writeStartObject();
					generator.writeStringField( NAME, item.getApplicationName());
					generator.writeBooleanField( BOUND, item.isBound());
					generator.writeEndObject();
				}

				generator.writeEndArray();
			}

			generator.writeEndObject();
		}
	}


	/**
	 * A JSon serializer for web socket messages.
	 * @author Vincent Zurczak - Linagora
	 */
	public static class WebSocketMessageSerializer extends JsonSerializer<WebSocketMessage> {

		@Override
		public void serialize(
				WebSocketMessage wsm,
				JsonGenerator generator,
				SerializerProvider provider )
		throws IOException {

			generator.writeStartObject();
			if( wsm.getEventType() != null )
				generator.writeStringField( WS_EVENT, wsm.getEventType().toString());

			if( wsm.getApplication() != null )
				generator.writeObjectField( WS_APP, wsm.getApplication());

			if( wsm.getApplicationTemplate() != null )
				generator.writeObjectField( WS_TPL, wsm.getApplicationTemplate());

			if( wsm.getInstance() != null )
				generator.writeObjectField( WS_INST, wsm.getInstance());

			if( wsm.getMessage() != null )
				generator.writeObjectField( WS_MSG, wsm.getMessage());

			generator.writeEndObject();
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

			if( app.getDisplayName() != null )
				generator.writeStringField( DISPLAY_NAME, app.getDisplayName());

			if( app.getDescription() != null )
				generator.writeStringField( DESC, app.getDescription());

			if( app.getTemplate() != null ) {
				generator.writeStringField( APP_INST_TPL_NAME, app.getTemplate().getName());
				if( app.getTemplate().getVersion() != null )
					generator.writeStringField( APP_INST_TPL_VERSION, app.getTemplate().getVersion());

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

			// Read-only information.
			// We do not expect it for deserialization
			Set<String> prefixesForExternalImports = VariableHelpers.findPrefixesForExternalImports( app );
			if( ! prefixesForExternalImports.isEmpty()) {
				generator.writeArrayFieldStart( EXT_DEP );
				for( String prefix : prefixesForExternalImports )
					generator.writeString( prefix );

				generator.writeEndArray();
			}

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

				n = node.get( APP_INST_TPL_VERSION );
				if( n != null )
					appTemplate.setVersion( n.textValue());

				n = node.get( APP_INST_TPL_EEP );
				if( n != null )
					appTemplate.setExternalExportsPrefix( n.textValue());

				application = new Application( appTemplate );

			} else {
				application = new Application( null );
			}

			if(( n = node.get( DISPLAY_NAME )) != null )
				application.setName( n.textValue());
			else if(( n = node.get( NAME )) != null )
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
			// (e.g. by comparison with component exports).
			if(( n = node.get( INST_EXPORTS )) != null ) {
				Map<String, String> exports = mapper.readValue( n.toString(), new TypeReference<HashMap<String,String>>(){});
				instance.overriddenExports.putAll(exports);
			}

			if(( n = node.get( INST_COMPONENT )) != null ) {
				Component instanceComponent = mapper.readValue( n.toString(), Component.class );
				instance.setComponent( instanceComponent );
			}

			// Add all the data (IP address, etc)
			if(( n = node.get( INST_DATA )) != null ) {
				for( Iterator<String> it = n.fieldNames(); it.hasNext(); ) {
					String fieldName = it.next();
					JsonNode dataNode = n.get( fieldName );
					instance.data.put( fieldName, dataNode.asText());
				}
			}

			// Add meta-data
			if(( n = node.get( PATH )) != null ) {
				instance.data.put( AT_INSTANCE_PATH, n.textValue());
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

			String installerName = ComponentHelpers.findComponentInstaller( component );
			if( installerName != null )
				generator.writeStringField( COMP_INSTALLER, installerName );

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
