/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.api.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import net.roboconf.core.Constants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.RoboconfErrorHelpers;
import net.roboconf.core.model.TargetValidator;
import net.roboconf.core.model.beans.AbstractApplication;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.TargetUsageItem;
import net.roboconf.core.model.runtime.TargetWrapperDescriptor;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.api.impl.beans.InstanceContext;
import net.roboconf.dm.internal.api.impl.beans.TargetPropertiesImpl;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.internal.utils.TargetHelpers;
import net.roboconf.dm.management.api.IConfigurationMngr;
import net.roboconf.dm.management.api.ITargetsMngr;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;

/**
 * Here is the way this class stores information.
 * <p>
 * Each target has an ID, which is an integer. And each target has its
 * own directory, located under the DM's configuration directory.
 * </p>
 * <p>
 * Each target may contain...
 * </p>
 * <ul>
 * 		<li>target.properties (mandatory)</li>
 * 		<li>{@value #TARGETS_ASSOC_FILE} (mapping between this target and application instances)</li>
 * 		<li>{@value #TARGETS_HINTS_FILE} (user preferences to show or hide targets for a given application)</li>
 * 		<li>{@value #TARGETS_USAGE_FILE} (mapping indicating which instances have deployed things with this target)</li>
 * </ul>
 *
 * @author Vincent Zurczak - Linagora
 * @author Amadou Diarra   - UGA
 */
public class TargetsMngrImpl implements ITargetsMngr {

	private static final String TARGETS_ASSOC_FILE = "associations.properties";
	private static final String TARGETS_HINTS_FILE = "hints.properties";
	private static final String TARGETS_USAGE_FILE = "usage.properties";
	private static final String CREATED_BY = "created.from";

	private static final Object LOCK = new Object();

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final IConfigurationMngr configurationMngr;
	private final Map<InstanceContext,String> instanceToCachedId;

	final ConcurrentHashMap<String,Boolean> targetIds = new ConcurrentHashMap<> ();


	/**
	 * Constructor.
	 * @param configurationMngr
	 */
	public TargetsMngrImpl( IConfigurationMngr configurationMngr ) {
		this.configurationMngr = configurationMngr;
		this.instanceToCachedId = new ConcurrentHashMap<> ();

		// Restore the cache
		restoreAssociationsCache();
	}


	// CRUD operations on targets


	@Override
	public String createTarget( String targetContent ) throws IOException {

		// Get the target ID
		TargetValidator tv = new TargetValidator( targetContent );
		tv.validate();
		if( RoboconfErrorHelpers.containsCriticalErrors( tv.getErrors()))
			throw new IOException( "There are errors in the target definition." );

		// Critical section to insert a target.
		// Store the ID, it cannot be reused.
		String targetId = tv.getProperties().getProperty( Constants.TARGET_PROPERTY_ID );
		String creator = tv.getProperties().getProperty( CREATED_BY );
		if( this.targetIds.putIfAbsent( targetId, Boolean.TRUE ) != null ) {

			// No creator? Then there is a conflict.
			if( creator == null )
				throw new IOException( "ID " + targetId + " is already used." );

			// It cannot be reused, unless they were created by the same template.
			File createdByFile = new File( findTargetDirectory( targetId ), CREATED_BY );
			String storedCreator = null;
			if( createdByFile.exists())
				storedCreator = Utils.readFileContent( createdByFile );

			// If they do not match, throw an exception
			if( ! Objects.equals( creator, storedCreator ))
				throw new IOException( "ID " + targetId + " is already used." );

			// Otherwise, we can override the properties
		}

		// Rewrite the properties without the ID.
		// We do not want it to be modified later.
		// We do not serialize java.util.properties#store because it adds
		// a time stamp, removes user comments and looses the properties order.
		targetContent = targetContent.replaceAll( Constants.TARGET_PROPERTY_ID + "\\s*(:|=)[^\n]*(\n|$)", "" );

		// For the same reason, we remove the "created.by" property
		targetContent = targetContent.replaceAll( "\n\n" + Pattern.quote( CREATED_BY ) + "\\s*(:|=)[^\n]*(\n|$)", "" );

		// Write the properties
		File targetFile = new File( findTargetDirectory( targetId ), Constants.TARGET_PROPERTIES_FILE_NAME );
		Utils.createDirectory( targetFile.getParentFile());
		Utils.writeStringInto( targetContent, targetFile );

		// Write the creator, if any
		if( creator != null ) {
			File createdByFile = new File( findTargetDirectory( targetId ), CREATED_BY );
			Utils.writeStringInto( creator, createdByFile );
		}

		return targetId;
	}


	@Override
	public String createTarget( File targetPropertiesFile, ApplicationTemplate creator ) throws IOException {

		String fileContent = Utils.readFileContent( targetPropertiesFile );

		// So that application templates can redefine some targets (when they are redeployed),
		// we insert a custom property when
		StringBuilder sb = new StringBuilder( fileContent );
		if( creator != null ) {
			sb.append( "\n\n" );
			sb.append( CREATED_BY );
			sb.append( ": " );
			sb.append( creator.getName());
			sb.append( " - " );
			sb.append( creator.getVersion());
		}

		// Create a target
		String targetId = createTarget( sb.toString());

		// Copy script files in target directory
		String prefix = Utils.removeFileExtension( targetPropertiesFile.getName());
		File scriptsInDir = new File( targetPropertiesFile.getParentFile(), prefix );
		File scriptsOutDir = new File( findTargetDirectory( targetId ), Constants.PROJECT_SUB_DIR_SCRIPTS );

		List<File> scriptFiles;
		if( scriptsInDir.exists())
			scriptFiles = Utils.listAllFiles( scriptsInDir );
		else
			scriptFiles = Collections.emptyList();

		if( ! scriptFiles.isEmpty()) {
			Utils.createDirectory( scriptsOutDir );
			for( File inputFile : scriptFiles ) {
				String relativePath = Utils.computeFileRelativeLocation( scriptsInDir, inputFile );
				File outputFile = new File( scriptsOutDir, relativePath );
				Utils.createDirectory( outputFile.getParentFile());
				Utils.copyStream( inputFile, outputFile );
			}
		}

		return targetId;
	}


	@Override
	public void updateTarget( String targetId, String newTargetContent ) throws IOException, UnauthorizedActionException {

		// Validate the new content
		TargetValidator tv = new TargetValidator( newTargetContent );
		tv.validate();

		// We should ignore all the ID related errors as we do not care anymore
		// (only at the creation, not on updates)
		RoboconfErrorHelpers.filterErrors(
				tv.getErrors(),
				ErrorCode.REC_TARGET_NO_ID,
				ErrorCode.REC_TARGET_CONFLICTING_ID );

		if( RoboconfErrorHelpers.containsCriticalErrors( tv.getErrors()))
			throw new IOException( "There are errors in the target definition." );

		// Write it
		File targetFile = findTargetFile( targetId, Constants.TARGET_PROPERTIES_FILE_NAME );
		if( targetFile == null )
			throw new UnauthorizedActionException( "Target " + targetId + " does not exist." );

		Utils.writeStringInto( newTargetContent, targetFile );
	}


	@Override
	public void deleteTarget( String targetId ) throws IOException, UnauthorizedActionException {

		// No machine using this target can be running.
		boolean used = false;
		synchronized( LOCK ) {
			used = isTargetUsed( targetId );
		}

		if( used )
			throw new UnauthorizedActionException( "Deletion is not permitted." );

		// Delete the files related to this target
		this.targetIds.remove( targetId );
		File targetDirectory = findTargetDirectory( targetId );
		Utils.deleteFilesRecursively( targetDirectory );
	}


	// Associating targets and application instances / components


	@Override
	public void associateTargetWith( String targetId, AbstractApplication app, String instancePathOrComponentName )
	throws IOException, UnauthorizedActionException {

		boolean valid = false;
		if( instancePathOrComponentName == null ) {
			valid = true;

		} else if( instancePathOrComponentName.startsWith( "@" )) {
			Component comp = ComponentHelpers.findComponent( app, instancePathOrComponentName.substring( 1 ));
			valid = comp != null;

		} else {
			Instance instance = InstanceHelpers.findInstanceByPath( app, instancePathOrComponentName );
			valid = instance != null;
			if( instance != null ) {
				if( instance.getStatus() != InstanceStatus.NOT_DEPLOYED )
					throw new UnauthorizedActionException( "Operation not allowed: " + app + " :: " + instancePathOrComponentName + " should be not deployed." );

				if( ! InstanceHelpers.isTarget( instance ))
					throw new IllegalArgumentException( "Only scoped instances can be associated with targets. Path in error: " + instancePathOrComponentName );
			}
		}

		if( valid )
			saveAssociation( app, targetId, instancePathOrComponentName, true );
	}


	@Override
	public void dissociateTargetFrom( AbstractApplication app, String instancePathOrComponentName )
	throws IOException, UnauthorizedActionException {

		if( instancePathOrComponentName != null
				&& ! instancePathOrComponentName.startsWith( "@" )) {

			Instance instance = InstanceHelpers.findInstanceByPath( app, instancePathOrComponentName );
			if( instance != null ) {
				if( instance.getStatus() != InstanceStatus.NOT_DEPLOYED )
					throw new UnauthorizedActionException( "Operation not allowed: " + app + " :: " + instancePathOrComponentName + " should be not deployed." );

				if( ! InstanceHelpers.isTarget( instance ))
					throw new IllegalArgumentException( "Only scoped instances can be associated with targets. Path in error: " + instancePathOrComponentName );
			}
		}

		saveAssociation( app, null, instancePathOrComponentName, false );
	}


	@Override
	public void copyOriginalMapping( Application app ) throws IOException {

		List<InstanceContext> keys = new ArrayList<> ();

		// Null <=> The default for the application
		keys.add( new InstanceContext( app.getTemplate(), (String) null ));

		// We can search defaults only for the existing instances
		for( Instance scopedInstance : InstanceHelpers.findAllScopedInstances( app ))
			keys.add( new InstanceContext( app.getTemplate(), scopedInstance ));

		// Copy mappings for the components
		for( Component comp : ComponentHelpers.findAllComponents( app )) {
			if( ComponentHelpers.isTarget( comp ))
				keys.add( new InstanceContext( app.getTemplate(), "@" + comp.getName()));
		}

		// Copy the associations when they exist for the template
		for( InstanceContext key : keys ) {
			String targetId = this.instanceToCachedId.get( key );
			try {
				if( targetId != null )
					associateTargetWith( targetId, app, key.getInstancePathOrComponentName());

			} catch( UnauthorizedActionException e ) {

				// This method could be used to reset the mappings / associations.
				// However, this is not possible if the current instance is already deployed.
				// So, we just skip it.
				this.logger.severe( e.getMessage());
				Utils.logException( this.logger, e );
			}
		}
	}


	@Override
	public void applicationWasDeleted( AbstractApplication app ) throws IOException {

		String name = app.getName();
		String qualifier = app instanceof ApplicationTemplate ? ((ApplicationTemplate) app).getVersion() : null;

		List<InstanceContext> toClean = new ArrayList<> ();
		Set<String> targetIds = new HashSet<> ();

		// Find the mapping keys and the targets to update
		for( Map.Entry<InstanceContext,String> entry : this.instanceToCachedId.entrySet()) {

			if( Objects.equals( name, entry.getKey().getName())
					&& Objects.equals( qualifier, entry.getKey().getQualifier())) {

				targetIds.add( entry.getValue());
				toClean.add( entry.getKey());
			}
		}

		// Once we have the target IDs, update the list of entries to remove
		for( Instance scopedInstance : InstanceHelpers.findAllScopedInstances( app )) {
			InstanceContext ctx = new InstanceContext( app, scopedInstance );
			toClean.add( ctx );
		}

		// Update the target files
		for( String targetId : targetIds ) {
			File targetDirectory = findTargetDirectory( targetId );

			// Update the association and usage files
			File[] files = new File[] {
				new File( targetDirectory, TARGETS_ASSOC_FILE ),
				new File( targetDirectory, TARGETS_HINTS_FILE ),
				new File( targetDirectory, TARGETS_USAGE_FILE )
			};

			for( File f : files ) {
				for( InstanceContext key : toClean ) {
					Properties props = Utils.readPropertiesFileQuietly( f, this.logger );
					props.remove( key.toString());
					writeProperties( props, f );
				}
			}
		}

		// Update the cache
		for( InstanceContext key : toClean )
			this.instanceToCachedId.remove( key );
	}


	// Finding targets


	@Override
	public TargetProperties findTargetProperties( AbstractApplication app, String instancePath ) {

		String targetId = findTargetId( app, instancePath );
		return findTargetProperties( targetId );
	}


	@Override
	public TargetProperties findTargetProperties( String targetId ) {

		File file = findTargetFile( targetId, Constants.TARGET_PROPERTIES_FILE_NAME );
		String content = Utils.readFileContentQuietly( file, this.logger );

		Map<String,String> map = new HashMap<> ();
		for( Map.Entry<Object,Object> entry : Utils.readPropertiesQuietly( content, this.logger ).entrySet()) {
			map.put((String) entry.getKey(), (String) entry.getValue());
		}

		return new TargetPropertiesImpl( map, content, file );
	}


	@Override
	public String findTargetId( AbstractApplication app, String instancePath, boolean strict ) {

		// Specific association for this instance
		InstanceContext key = new InstanceContext( app, instancePath );
		String targetId = this.instanceToCachedId.get( key );

		// Non-scoped instances cannot be associated with targets.
		// Such a query is a coding error!
		Instance inst = InstanceHelpers.findInstanceByPath( app, instancePath );
		if( inst != null && ! InstanceHelpers.isTarget( inst ))
			throw new IllegalArgumentException( "Targets aimed at being queried for scoped instances only. Invalid path: " + instancePath );

		// Association inherited from the component
		if( targetId == null && ! strict ) {
			if( inst != null ) {
				key = new InstanceContext( app, "@" + inst.getComponent().getName());
				targetId = this.instanceToCachedId.get( key );
			}
		}

		// Default target for this application
		if( targetId == null && ! strict ) {
			key = new InstanceContext( app, (String) null );
			targetId = this.instanceToCachedId.get( key );
		}

		return targetId;
	}


	@Override
	public String findTargetId( AbstractApplication app, String instancePath ) {
		return findTargetId( app, instancePath, false );
	}


	// Finding script resources


	@Override
	public Map<String,byte[]> findScriptResourcesForAgent( String targetId ) throws IOException {

		Map<String,byte[]> result = new HashMap<>( 0 );
		File targetDir = new File( findTargetDirectory( targetId ), Constants.PROJECT_SUB_DIR_SCRIPTS );
		if( targetDir.isDirectory()){

			List<String> exclusionPatterns = Arrays.asList(
				"(?i).*" + Pattern.quote( Constants.LOCAL_RESOURCE_PREFIX ) + ".*"
			);

			result.putAll( Utils.storeDirectoryResourcesAsBytes( targetDir, exclusionPatterns ));
		}

		return result;
	}


	@Override
	public Map<String,byte[]> findScriptResourcesForAgent( AbstractApplication app, Instance scopedInstance ) throws IOException {

		Map<String,byte[]> result = new HashMap<> ();
		String targetId = findTargetId( app, InstanceHelpers.computeInstancePath( scopedInstance ));
		if( targetId != null )
			result = findScriptResourcesForAgent( targetId );

		return result;
	}


	@Override
	public File findScriptForDm( AbstractApplication app, Instance scopedInstance ) {

		File result = null;
		String targetId = findTargetId( app, InstanceHelpers.computeInstancePath( scopedInstance ));
		if( targetId != null ) {
			File targetDir = new File( findTargetDirectory( targetId ), Constants.PROJECT_SUB_DIR_SCRIPTS );
			if( targetDir.isDirectory()){
				for( File f : Utils.listAllFiles( targetDir )) {
					if( f.getName().toLowerCase().contains( Constants.SCOPED_SCRIPT_AT_DM_CONFIGURE_SUFFIX )) {
						result = f;
						break;
					}
				}
			}
		}

		return result;
	}


	@Override
	public List<TargetWrapperDescriptor> listAllTargets() {

		File dir = new File( this.configurationMngr.getWorkingDirectory(), ConfigurationUtils.TARGETS );
		List<File> targetDirectories = Utils.listDirectories( dir );

		return buildList( targetDirectories, null );
	}


	@Override
	public TargetWrapperDescriptor findTargetById( String targetId ) {

		File dir = new File( this.configurationMngr.getWorkingDirectory(), ConfigurationUtils.TARGETS );
		File targetDirectory = new File( dir, targetId );

		return build( targetDirectory );
	}


	// In relation with hints


	@Override
	public List<TargetWrapperDescriptor> listPossibleTargets( AbstractApplication app ) {

		// Find the matching targets based on registered hints
		String key = new InstanceContext( app ).toString();
		String tplKey = null;
		if( app instanceof Application )
			tplKey = new InstanceContext(((Application) app).getTemplate()).toString();

		List<File> targetDirectories = new ArrayList<> ();
		File dir = new File( this.configurationMngr.getWorkingDirectory(), ConfigurationUtils.TARGETS );
		for( File f : Utils.listDirectories( dir )) {

			// If there is no hint for this target, then it is global.
			// We can list it.
			File hintsFile = new File( f, TARGETS_HINTS_FILE );
			if( ! hintsFile.exists()) {
				targetDirectories.add( f );
				continue;
			}

			// Otherwise, the key must exist in the file
			Properties props = Utils.readPropertiesFileQuietly( hintsFile, this.logger );
			if( props.containsKey( key ))
				targetDirectories.add( f );
			else if( tplKey != null && props.containsKey( tplKey ))
				targetDirectories.add( f );
		}

		// Build the result
		return buildList( targetDirectories, app );
	}


	@Override
	public void addHint( String targetId, AbstractApplication app ) throws IOException {
		saveHint( targetId, app, true );
	}


	@Override
	public void removeHint( String targetId, AbstractApplication app ) throws IOException {
		saveHint( targetId, app, false );
	}


	// Atomic operations...


	@Override
	public TargetProperties lockAndGetTarget( Application app, Instance scopedInstance )
	throws IOException {

		String instancePath = InstanceHelpers.computeInstancePath( scopedInstance );
		String targetId = findTargetId( app, instancePath );
		if( targetId == null )
			throw new IOException( "No target was found for " + app + " :: " + instancePath );

		InstanceContext mappingKey = new InstanceContext( app, instancePath );
		synchronized( LOCK ) {
			saveUsage( mappingKey, targetId, true );
		}

		this.logger.fine( "Target " + targetId + "'s lock was acquired for " + instancePath );
		TargetProperties result = findTargetProperties( app, instancePath );
		Map<String,String> newTargetProperties = TargetHelpers.expandProperties( scopedInstance, result.asMap());

		result.asMap().clear();
		result.asMap().putAll( newTargetProperties );

		return result;
	}


	@Override
	public void unlockTarget( Application app, Instance scopedInstance )
	throws IOException {

		String instancePath = InstanceHelpers.computeInstancePath( scopedInstance );
		String targetId = findTargetId( app, instancePath );
		InstanceContext mappingKey = new InstanceContext( app, instancePath );

		synchronized( LOCK ) {
			saveUsage( mappingKey, targetId, false );
		}

		this.logger.fine( "Target " + targetId + "'s lock was released for " + instancePath );
	}


	// Diagnostics


	@Override
	public List<TargetUsageItem> findUsageStatistics( String targetId ) {

		// Get usage first
		List<String> appNames;
		synchronized( LOCK ) {
			appNames = applicationsThatUse( targetId );
		}

		// Now, let's build the result
		Set<TargetUsageItem> result = new HashSet<> ();
		for( Map.Entry<InstanceContext,String> entry : this.instanceToCachedId.entrySet()) {
			if( ! entry.getValue().equals( targetId ))
				continue;

			String appName = entry.getKey().getName();
			TargetUsageItem item = new TargetUsageItem();

			item.setName( appName );
			item.setVersion( entry.getKey().getQualifier());
			item.setReferencing( true );
			item.setUsing( appNames.contains( appName ));

			result.add( item );
		}

		return new ArrayList<>( result );
	}


	// Package methods


	List<TargetWrapperDescriptor> buildList( List<File> targetDirectories, AbstractApplication app ) {

		List<TargetWrapperDescriptor> result = new ArrayList<> ();
		for( File targetDirectory : targetDirectories ) {

			File associationFile = new File( targetDirectory, TARGETS_ASSOC_FILE );
			Properties props = Utils.readPropertiesFileQuietly( associationFile, this.logger );
			boolean isDefault = false;
			if( app != null )
				isDefault = props.containsKey( new InstanceContext( app ).toString());

			TargetWrapperDescriptor tb = build( targetDirectory );
			if( tb != null ) {
				tb.setDefault( isDefault );
				result.add( tb );
			}
		}

		return result;
	}


	TargetWrapperDescriptor build( File targetDirectory ) {

		TargetWrapperDescriptor tb = null;
		File targetPropertiesFile = new File( targetDirectory, Constants.TARGET_PROPERTIES_FILE_NAME );
		try {
			Properties props = Utils.readPropertiesFile( targetPropertiesFile );
			tb = new TargetWrapperDescriptor();

			tb.setId( targetDirectory.getName());
			tb.setName( props.getProperty( Constants.TARGET_PROPERTY_NAME ));
			tb.setDescription( props.getProperty( Constants.TARGET_PROPERTY_DESCRIPTION ));

			String handler = TargetHelpers.findTargetHandlerName( props );
			tb.setHandler( handler );

		} catch( IOException e ) {
			this.logger.severe( "Properties of the target #" + targetDirectory.getName() + " could not be read." );
			Utils.logException( this.logger, e );
		}

		return tb;
	}


	// Private methods


	private void saveAssociation( AbstractApplication app, String targetId, String instancePathOrComponentName, boolean add )
	throws IOException {

		// Association means an exact mapping between an application instance
		// and a target ID.
		InstanceContext key = new InstanceContext( app, instancePathOrComponentName );

		// Remove the old association, always.
		if( instancePathOrComponentName != null ) {
			String oldTargetId = this.instanceToCachedId.remove( key );
			if( oldTargetId != null ) {
				File associationFile = findTargetFile( oldTargetId, TARGETS_ASSOC_FILE );
				Properties props = Utils.readPropertiesFileQuietly( associationFile, this.logger );
				props.remove( key.toString());
				writeProperties( props, associationFile );
			}
		}

		// Register a potential new association and update the cache.
		if( add ) {
			File associationFile = findTargetFile( targetId, TARGETS_ASSOC_FILE );
			if( associationFile == null )
				throw new IOException( "Target " + targetId + " does not exist." );

			Properties props = Utils.readPropertiesFileQuietly( associationFile, this.logger );
			props.setProperty( key.toString(), "" );
			writeProperties( props, associationFile );

			this.instanceToCachedId.put( key, targetId );
		}
	}


	private void restoreAssociationsCache() {

		File dir = new File( this.configurationMngr.getWorkingDirectory(), ConfigurationUtils.TARGETS );
		for( File f : Utils.listDirectories( dir )) {

			// Store the ID
			this.targetIds.put( f.getName(), Boolean.TRUE );

			// Cache associations for quicker access
			File associationFile = new File( f, TARGETS_ASSOC_FILE );
			Properties props = Utils.readPropertiesFileQuietly( associationFile, this.logger );
			for( Map.Entry<Object,Object> entry : props.entrySet()) {

				InstanceContext key = InstanceContext.parse( entry.getKey().toString());
				this.instanceToCachedId.put( key, f.getName());
			}
		}
	}


	private void saveUsage( InstanceContext mappingKey, String targetId, boolean add )
	throws IOException {

		// Usage means the target has been used to create a real machine.
		File usageFile = findTargetFile( targetId, TARGETS_USAGE_FILE );
		Properties props = Utils.readPropertiesFileQuietly( usageFile, this.logger );

		String key = mappingKey.toString();
		if( add )
			props.setProperty( key, targetId );
		else
			props.remove( key );

		writeProperties( props, usageFile );
	}


	private boolean isTargetUsed( String targetId ) {

		File usageFile = findTargetFile( targetId, TARGETS_USAGE_FILE );
		Properties props = Utils.readPropertiesFileQuietly( usageFile, this.logger );

		boolean found = false;
		for( Iterator<Object> it = props.values().iterator(); it.hasNext() && ! found; ) {
			found = it.next().equals( targetId );
		}

		return found;
	}


	private List<String> applicationsThatUse( String targetId ) {

		File usageFile = findTargetFile( targetId, TARGETS_USAGE_FILE );
		Properties props = Utils.readPropertiesFileQuietly( usageFile, this.logger );

		List<String> result = new ArrayList<> ();
		for( Object o : props.keySet()) {
			InstanceContext key = InstanceContext.parse((String) o);
			result.add( key.getName());
		}

		return result;
	}


	private void saveHint( String targetId, AbstractApplication app, boolean add ) throws IOException {

		// A hint is just a preference (some kind of scope for a target).
		// If a hint is not respected, no exception will be thrown.
		File hintsFile = findTargetFile( targetId, TARGETS_HINTS_FILE );
		Properties props = Utils.readPropertiesFileQuietly( hintsFile, this.logger );

		String key = new InstanceContext( app ).toString();
		if( add ) {
			props.setProperty( key, "" );
		} else {
			props.remove( key );
		}

		writeProperties( props, hintsFile );
	}


	private void writeProperties( Properties props, File file ) throws IOException {

		if( props.isEmpty())
			Utils.deleteFilesRecursivelyAndQuietly( file );
		else
			Utils.writePropertiesFile( props, file );
	}


	private File findTargetDirectory( String targetId ) {

		File dir = new File( this.configurationMngr.getWorkingDirectory(), ConfigurationUtils.TARGETS );
		return new File( dir, targetId );
	}


	private File findTargetFile( String targetId, String fileName ) {

		File dir = null;
		File result = null;
		if( targetId != null
				&& (dir = findTargetDirectory( targetId )).exists())
			result = new File( dir, fileName );

		return result;
	}
}
