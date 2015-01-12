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

package net.roboconf.dm.internal.environment.target;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.ResourceUtils;
import net.roboconf.core.utils.Utils;

/**
 * Utilities related to deployment targets.
 * @author Vincent Zurczak - Linagora
 */
public final class TargetHelpers {

	/**
	 * Empty private constructor.
	 */
	private TargetHelpers() {
		// nothing
	}


	/**
	 * Loads the targetHandlers properties.
	 * @param applicationFilesDirectory the directory where application resources are stored
	 * @param instance the root instance to find the targetHandlers properties
	 * @return a non-null properties
	 * @throws IOException if the targetHandlers properties file was not found
	 */
	public static Map<String,String> loadTargetProperties( File applicationFilesDirectory, Instance instance ) throws IOException {

		Instance realRootInstance = InstanceHelpers.findRootInstance( instance );
		File f = ResourceUtils.findInstanceResourcesDirectory( applicationFilesDirectory, realRootInstance );
		f = new File( f, Constants.TARGET_PROPERTIES_FILE_NAME );

		Map<String, String> result = new HashMap<String, String>();
		InputStream in = null;
		try {
			Properties p = new Properties();
			in = new FileInputStream(f);
			p.load( in );
			for( Map.Entry<Object,Object> entry : p.entrySet()) {
				result.put( entry.getKey().toString(), entry.getValue().toString());
			}

		} finally {
			Utils.closeQuietly( in );
		}

		return result;
	}
}
