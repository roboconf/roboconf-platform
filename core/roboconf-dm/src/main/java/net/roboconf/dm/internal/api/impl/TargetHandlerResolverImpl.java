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

package net.roboconf.dm.internal.api.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.dm.internal.utils.TargetHelpers;
import net.roboconf.dm.management.api.ITargetHandlerResolver;
import net.roboconf.target.api.TargetException;
import net.roboconf.target.api.TargetHandler;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetHandlerResolverImpl implements ITargetHandlerResolver {

	private final List<TargetHandler> targetHandlers = new ArrayList<> ();
	private final Logger logger = Logger.getLogger( getClass().getName());


	/**
	 * Adds a new target handler.
	 * @param targetItf a target handler
	 */
	public void addTargetHandler( TargetHandler targetItf ) {

		if( targetItf != null ) {
			this.logger.info( "Target handler '" + targetItf.getTargetId() + "' is now available in Roboconf's DM." );

			synchronized( this.targetHandlers ) {
				this.targetHandlers.add( targetItf );
			}

			listTargets( this.targetHandlers, this.logger );
		}
	}


	/**
	 * Removes a target handler.
	 * @param targetItf a target handler
	 */
	public void removeTargetHandler( TargetHandler targetItf ) {

		// May happen if a target could not be instantiated
		// (iPojo uses proxies). In this case, it results in a NPE here.
		if( targetItf == null ) {
			this.logger.info( "An invalid target handler is removed." );

		} else {
			synchronized( this.targetHandlers ) {
				this.targetHandlers.remove( targetItf );
			}

			this.logger.info( "Target handler '" + targetItf.getTargetId() + "' is not available anymore in Roboconf's DM." );
		}

		listTargets( this.targetHandlers, this.logger );
	}


	@Override
	public TargetHandler findTargetHandler( Map<String,String> targetProperties )
	throws TargetException {

		String targetId = TargetHelpers.findTargetHandlerName( targetProperties );
		TargetHandler result = null;
		if( targetId != null ) {
			for( TargetHandler itf : this.targetHandlers ) {
				if( targetId.equalsIgnoreCase( itf.getTargetId())) {
					result = itf;
					break;
				}
			}
		}

		if( result == null )
			throw new TargetException( "No deployment handler was found for handler named " + targetId );

		return result;
	}


	/**
	 * @return a snapshot of the target handlers
	 */
	public List<TargetHandler> getTargetHandlersSnapshot() {

		List<TargetHandler> snapshot;
		synchronized( this.targetHandlers ) {
			snapshot = new ArrayList<>( this.targetHandlers );
		}

		return snapshot;
	}


	/**
	 * This method lists the available target and logs them.
	 */
	public static void listTargets( List<TargetHandler> targetHandlers, Logger logger ) {

		if( targetHandlers.isEmpty()) {
			logger.info( "No target was found for Roboconf's DM." );

		} else {
			StringBuilder sb = new StringBuilder( "Available target in Roboconf's DM: " );
			for( Iterator<TargetHandler> it = targetHandlers.iterator(); it.hasNext(); ) {
				sb.append( it.next().getTargetId());
				if( it.hasNext())
					sb.append( ", " );
			}

			sb.append( "." );
			logger.info( sb.toString());
		}
	}
}
