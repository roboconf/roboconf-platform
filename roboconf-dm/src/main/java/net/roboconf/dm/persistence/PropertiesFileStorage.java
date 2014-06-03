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

package net.roboconf.dm.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PropertiesFileStorage implements IDmStorage {

	public static final File STATE_FILE = new File(
			System.getProperty( "user.home" ),
			"roboconf.dm.state.properties" );

	static final String APPS = "apps";
	static final String DOT = ".";
	static final String DOT_DIRECTORY = ".directory";
	static final String DOT_ROOT_INSTANCES = ".roots";
	static final String DOT_INSTANCE_IP = ".ip";
	static final String DOT_INSTANCE_COMPONENT = ".component";
	static final String DOT_INSTANCE_MACHINE_ID = ".machine-id";
	static final String DOT_INSTANCE_STATUS = ".status";


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.persistence.IDmStorage
	 * #requiresAgentContact()
	 */
	@Override
	public boolean requiresAgentContact() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.persistence.IDmStorage
	 * #saveManagerState(net.roboconf.dm.persistence.IDmStorage.DmStorageBean)
	 */
	@Override
	public void saveManagerState( DmStorageBean managerState ) throws IOException {

		// Save information in properties
		Properties props = extractApplicationsProperties( managerState );

		// Save the properties
		FileOutputStream fos = null;
		try {
			String comment = props.stringPropertyNames().isEmpty() ? "No state, there was nothing to save" : null;
			fos = new FileOutputStream( STATE_FILE );
			props.store( fos, comment );

		} finally {
			Utils.closeQuietly( fos );
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.persistence.IDmStorage
	 * #restoreManagerState()
	 */
	@Override
	public DmStorageBean restoreManagerState() throws IOException {

		// Read the properties
		Properties props = new Properties();
		FileInputStream fis = null;
		try {
			if( STATE_FILE.exists()) {
				fis = new FileInputStream( STATE_FILE );
				props.load( fis );
			}

		} finally {
			Utils.closeQuietly( fis );
		}

		// Convert it to beans
		DmStorageBean result = new DmStorageBean();

		// Restore the applications
		for( String appName : props.getProperty( APPS, "" ).split( "," )) {
			appName = appName.trim();
			if( Utils.isEmptyOrWhitespaces( appName ))
				continue;

			DmStorageApplicationBean appBean = new DmStorageApplicationBean();
			result.getApplications().add( appBean );

			appBean.applicationName( appName );
			appBean.applicationDirectoryPath( props.getProperty( appName + DOT_DIRECTORY ));

			for( String rootInstanceName : props.getProperty( appName + DOT_ROOT_INSTANCES, "" ).split( "," )) {
				rootInstanceName = rootInstanceName.trim();
				if( Utils.isEmptyOrWhitespaces( rootInstanceName ))
					continue;

				DmStorageRootInstanceBean rootInstanceBean = new DmStorageRootInstanceBean();
				appBean.getRootInstances().add( rootInstanceBean );

				rootInstanceBean.rootInstanceName( rootInstanceName );
				rootInstanceBean.ipAddress( props.getProperty( appName + DOT + rootInstanceName + DOT_INSTANCE_IP ));
				rootInstanceBean.machineId( props.getProperty( appName + DOT + rootInstanceName + DOT_INSTANCE_MACHINE_ID ));
				rootInstanceBean.componentName( props.getProperty( appName + DOT + rootInstanceName + DOT_INSTANCE_COMPONENT ));
				rootInstanceBean.status( props.getProperty( appName + DOT + rootInstanceName + DOT_INSTANCE_STATUS ));
			}
		}

		return result;
	}


	/**
	 * Converts the data to store as a {@link Properties} object.
	 * @param managerState the manager's state
	 * @return Java properties
	 */
	static Properties extractApplicationsProperties( DmStorageBean managerState ) {

		Properties props = new Properties();
		StringBuilder sb = new StringBuilder();
		for( Iterator<DmStorageApplicationBean> itMa = managerState.getApplications().iterator(); itMa.hasNext(); ) {

			DmStorageApplicationBean bean = itMa.next();
			sb.append( bean.getApplicationName());
			if( itMa.hasNext())
				sb.append( ", " );

			// Root Instances
			StringBuilder roots = new StringBuilder();
			for( Iterator<DmStorageRootInstanceBean> it = bean.getRootInstances().iterator(); it.hasNext(); ) {
				DmStorageRootInstanceBean rootInstance = it.next();
				roots.append( rootInstance.getRootInstanceName());
				if( it.hasNext())
					roots.append( ", " );

				String prefix = bean.getApplicationName() + DOT + rootInstance.getRootInstanceName();
				if( rootInstance.getStatus() != null )
					props.setProperty( prefix + DOT_INSTANCE_STATUS, rootInstance.getStatus());

				if( ! Utils.isEmptyOrWhitespaces( rootInstance.getIpAddress()))
					props.setProperty( prefix + DOT_INSTANCE_IP, rootInstance.getIpAddress());

				if( ! Utils.isEmptyOrWhitespaces( rootInstance.getMachineId()))
					props.setProperty( prefix + DOT_INSTANCE_MACHINE_ID, rootInstance.getMachineId());

				if( ! Utils.isEmptyOrWhitespaces( rootInstance.getComponentName()))
					props.setProperty( prefix + DOT_INSTANCE_COMPONENT, rootInstance.getComponentName());
			}

			// Application Properties
			String rootsAsString = roots.toString();
			if( ! Utils.isEmptyOrWhitespaces( rootsAsString ))
				props.setProperty( bean.getApplicationName() + DOT_ROOT_INSTANCES, rootsAsString );

			if( ! Utils.isEmptyOrWhitespaces( bean.getApplicationDirectoryPath()))
				props.setProperty( bean.getApplicationName() + DOT_DIRECTORY, bean.getApplicationDirectoryPath());
		}

		// Applications List
		if( ! sb.toString().isEmpty())
			props.setProperty( APPS, sb.toString());

		return props;
	}
}
