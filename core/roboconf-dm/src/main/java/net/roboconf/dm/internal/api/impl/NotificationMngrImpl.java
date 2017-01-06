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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.runtime.EventType;
import net.roboconf.dm.management.api.INotificationMngr;
import net.roboconf.dm.management.events.IDmListener;

/**
 * @author Vincent Zurczak - Linagora
 */
public class NotificationMngrImpl implements INotificationMngr {

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final List<IDmListener> dmListeners = new ArrayList<> ();
	private final AtomicBoolean enableNotifications = new AtomicBoolean( false );


	@Override
	public String getId() {
		return "main";
	}


	@Override
	public void enableNotifications() {

		this.logger.info( "Notifications are being enabled for DM listeners..." );
		this.enableNotifications.set( true );
		synchronized( this.dmListeners ) {
			for( IDmListener listener : this.dmListeners )
				listener.enableNotifications();
		}
	}


	@Override
	public void disableNotifications() {

		this.logger.info( "Notifications are being disabled for DM listeners..." );
		this.enableNotifications.set( false );
		synchronized( this.dmListeners ) {
			for( IDmListener listener : this.dmListeners )
				listener.disableNotifications();
		}
	}


	/**
	 * Adds a listener.
	 * @param listener
	 */
	public void addListener( IDmListener listener ) {

		if( listener == null ) {
			this.logger.info( "An invalid DM listener failed to be added." );

		} else {
			synchronized( this.dmListeners ) {
				this.dmListeners.add( listener );
			}

			if( this.enableNotifications.get())
				listener.enableNotifications();

			this.logger.info( "The listener '" + listener.getId() + "' is now available in Roboconf's DM." );
		}

		listListeners();
	}


	/**
	 * Removes a listener.
	 * @param listener
	 */
	public void removeListener( IDmListener listener ) {

		if( listener != null ) {
			synchronized( this.dmListeners ) {
				this.dmListeners.remove( listener );
			}

			this.logger.info( "The listener '" + listener.getId() + "' is not available anymore in Roboconf's DM." );
		} else {
			this.logger.info( "An invalid DM listener was removed." );
		}

		listListeners();
	}


	/**
	 * @return the dmListeners
	 */
	public List<IDmListener> getDmListeners() {
		return Collections.unmodifiableList( this.dmListeners );
	}


	@Override
	public void application( Application application, EventType eventType ) {
		synchronized( this.dmListeners ) {
			for( IDmListener listener : this.dmListeners )
				listener.application( application, eventType );
		}
	}


	@Override
	public void applicationTemplate( ApplicationTemplate tpl, EventType eventType ) {
		synchronized( this.dmListeners ) {
			for( IDmListener listener : this.dmListeners )
				listener.applicationTemplate( tpl, eventType );
		}
	}


	@Override
	public void instance( Instance instance, Application application, EventType eventType ) {
		synchronized( this.dmListeners ) {
			for( IDmListener listener : this.dmListeners )
				listener.instance( instance, application, eventType );
		}
	}


	@Override
	public void raw( String message, Object... data ) {
		synchronized( this.dmListeners ) {
			for( IDmListener listener : this.dmListeners )
				listener.raw( message, data );
		}
	}


	private void listListeners() {

		List<IDmListener> dmListenersCopy = new ArrayList<> ();
		synchronized( this.dmListeners ) {
			dmListenersCopy.addAll( this.dmListeners );
		}

		if( dmListenersCopy.isEmpty()) {
			this.logger.info( "No listener was found in Roboconf's DM." );

		} else {
			StringBuilder sb = new StringBuilder( "Available listeners in Roboconf's DM: " );
			for( Iterator<IDmListener> it = dmListenersCopy.iterator(); it.hasNext(); ) {
				sb.append( it.next().getId());
				if( it.hasNext())
					sb.append( ", " );
			}

			sb.append( "." );
			this.logger.info( sb.toString());
		}
	}
}
