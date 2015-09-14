/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.roboconf.core.model.beans.AbstractApplication;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.api.IConfigurationMngr;
import net.roboconf.dm.management.api.ITargetsMngr;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetsMngrImpl implements ITargetsMngr {

	private final IConfigurationMngr configurationMngr;


	/**
	 * Constructor.
	 * @param configurationMngr
	 */
	public TargetsMngrImpl( IConfigurationMngr configurationMngr ) {
		this.configurationMngr = configurationMngr;
	}


	@Override
	public String createTarget( String targetContent ) throws IOException {
		File targetFile = findTargetFile( null );
		Utils.writeStringInto( targetContent, targetFile );
		return targetFile.getName();
	}


	@Override
	public String createTarget( File targetPropertiesFile ) throws IOException {
		File targetFile = findTargetFile( null );
		Utils.copyStream( targetPropertiesFile, targetFile );
		return targetFile.getName();
	}


	@Override
	public void updateTarget( String targetId, String newTargetContent ) throws IOException {
		File targetFile = findTargetFile( targetId );
		Utils.writeStringInto( newTargetContent, targetFile );
	}


	@Override
	public void deleteTarget( String targetId ) throws IOException {
		File targetFile = findTargetFile( targetId );
		Utils.deleteFilesRecursively( targetFile );
	}


	@Override
	public void associateTargetWithScopedInstance( String targetId, AbstractApplication app, String instancePath )
	throws IOException {

		File dir = findDirectory( app );
		saveAssociation( dir, targetId, instancePath, true );
	}


	@Override
	public void dissociateTargetFromScopedInstance( String targetId, AbstractApplication app, String instancePath )
	throws IOException {

		File dir = findDirectory( app );
		saveAssociation( dir, targetId, instancePath, false );
	}


	@Override
	public Map<String,String> findTargetProperties( AbstractApplication app, String instancePath ) {

		Map<String,String> result = new HashMap<String,String>( 0 );

		return result;
	}


	@Override
	public List<TargetBean> findTargets( AbstractApplication app ) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void addHint( String targetId, AbstractApplication app ) {
		// TODO Auto-generated method stub

	}


	@Override
	public void removeHint( String targetId, AbstractApplication app ) {
		// TODO Auto-generated method stub

	}


	@Override
	public void markTargetAs( String targetId, Application app,
			String instancePath, boolean used ) {
		// TODO Auto-generated method stub

	}


	private void saveAssociation( File appOrTplDirectory, String targetId, String instancePath, boolean add )
	throws IOException {

		Properties props = new Properties();
		File f = new File( appOrTplDirectory, ConfigurationUtils.TARGETS_ASSOC_FILE );
		if( f.exists())
			props = Utils.readPropertiesFile( f );

		if( add )
			props.setProperty( instancePath, String.valueOf( targetId ));
		else
			props.remove( instancePath );

		if( props.isEmpty())
			Utils.deleteFilesRecursivelyAndQuitely( f );
		else
			Utils.writePropertiesFile( props, f );
	}


	private File findDirectory( AbstractApplication app ) {

		File dir;
		if( app instanceof Application )
			dir = ConfigurationUtils.findApplicationDirectory( app.getName(), this.configurationMngr.getWorkingDirectory());
		else
			dir = ConfigurationUtils.findTemplateDirectory((ApplicationTemplate) app, this.configurationMngr.getWorkingDirectory());

		return dir;
	}


	private File findTargetFile( String targetId ) throws IOException {

		File dir = new File( this.configurationMngr.getWorkingDirectory(), ConfigurationUtils.TARGETS );
		Utils.createDirectory( dir );
		File result;
		if( targetId != null ) {
			result = new File( dir, String.valueOf( targetId ));

		} else for( int i=0; ; i++ ) {
			result = new File( dir, String.valueOf( i ));
			if( ! result.exists())
				break;
		}

		return result;
	}
}
