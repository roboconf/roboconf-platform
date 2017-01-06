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
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.Constants;
import net.roboconf.core.utils.IconUtils;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.api.IConfigurationMngr;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ConfigurationMngrImpl implements IConfigurationMngr {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private File workingDirectory;


	/**
	 * Constructor.
	 */
	public ConfigurationMngrImpl() {

		String karafData = System.getProperty( Constants.KARAF_DATA );
		if( Utils.isEmptyOrWhitespaces( karafData ))
			this.workingDirectory = new File( System.getProperty( "java.io.tmpdir" ), "roboconf-dm" );
		else
			this.workingDirectory = new File( karafData, "roboconf" );

		try {
			Utils.createDirectory( this.workingDirectory );

		} catch( IOException e ) {
			this.logger.severe( "The DM's configuration directory could not be found and/or created." );
			Utils.logException( this.logger, e );
		}
	}


	@Override
	public File getWorkingDirectory() {
		return this.workingDirectory;
	}


	@Override
	public void setWorkingDirectory( File workingDirectory ) {
		this.workingDirectory = workingDirectory;
	}


	@Override
	public File findIconFromPath( String urlPath ) {
		Map.Entry<String,String> entry = IconUtils.decodeIconUrl( urlPath );
		return ConfigurationUtils.findIcon( entry.getKey(), entry.getValue(), this.workingDirectory );
	}
}
