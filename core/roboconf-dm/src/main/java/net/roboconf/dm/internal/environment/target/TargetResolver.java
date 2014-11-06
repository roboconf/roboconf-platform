/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import net.roboconf.core.Constants;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.dm.management.ITargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetResolver implements ITargetResolver {

	public static final String TARGET_ID = "target.id";


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.management.ITargetResolver
	 * #findTargetHandler(java.util.List, net.roboconf.dm.management.ManagedApplication, net.roboconf.core.model.runtime.Instance)
	 */
	@Override
	public TargetHandler findTargetHandler( List<TargetHandler> target, ManagedApplication ma, Instance instance )
	throws TargetException {

		TargetHandler targetHandler;
		try {
			String installerName = instance.getComponent().getInstallerName();
			if( ! Constants.TARGET_INSTALLER.equalsIgnoreCase( installerName ))
				throw new TargetException( "Unsupported installer name: " + installerName );

			Map<String,String> props = TargetHelpers.loadTargetProperties( ma.getApplicationFilesDirectory(), instance );
			String targetId = props.get( TARGET_ID );
			targetHandler = findTargetHandler( target, targetId );
			if( targetHandler == null )
				throw new TargetException( "No deployment handler was found for " + instance.getName() + "." );

			targetHandler.setTargetProperties( props );

		} catch( IOException e ) {
			throw new TargetException( e );
		}

		return targetHandler;
	}


	/**
	 * Finds the right targetHandlers handler.
	 * @param targetHandlers the list of available handlers (can be null)
	 * @param targetId the targetHandlers ID
	 * @return a handler for a deployment targetHandlers, or null if none matched
	 */
	protected TargetHandler findTargetHandler( List<TargetHandler> target, String targetId ) {

		TargetHandler result = null;
		if( target != null ) {
			for( TargetHandler itf : target ) {
				if( targetId.equalsIgnoreCase( itf.getTargetId())) {
					result = itf;
					break;
				}
			}
		}

		return result;
	}
}
