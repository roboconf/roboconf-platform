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

package net.roboconf.dm.templating.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.runtime.EventType;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.events.IDmListener;
import net.roboconf.dm.templating.internal.templates.TemplateEntry;
import net.roboconf.dm.templating.internal.templates.TemplateUtils;
import net.roboconf.dm.templating.internal.templates.TemplateWatcher;

/**
 * @author Pierre Bourret - Université Joseph Fourier
 * @author Vincent Zurczak - Linagora
 */
public class TemplatingManager implements IDmListener {

	public static final String ID = "DM's Templating";

	// Locks
	private final Object watcherLock = new Object();
	private final Object generationLock = new Object();

	// Injected by iPojo
	private long pollInterval;
	Manager dm;

	// Fields
	private final Logger logger = Logger.getLogger( getClass().getName());
	File templatesDIR, outputDIR;
	TemplateWatcher templateWatcher;



	// Configuration


	/**
	 * Sets the poll interval for the template watcher.
	 * @param pollInterval the poll interval, in milliseconds, for the template watcher
	 */
	public void setPollInterval( long pollInterval ) {

		this.pollInterval = pollInterval;
		this.logger.fine( "Template watcher poll interval set to " + pollInterval );
		synchronized( this.watcherLock ) {
			if( this.templateWatcher != null )
				resetWatcher();
		}
	}


	/**
	 * Sets the templates directory.
	 * @param templatesDirectory the templates directory
	 */
	public void setTemplatesDirectory( String templatesDirectory ) {

		this.logger.fine( "Templates directory is now... " + templatesDirectory );
		this.templatesDIR = Utils.isEmptyOrWhitespaces( templatesDirectory ) ? null : new File( templatesDirectory );
		synchronized( this.watcherLock ) {
			if( this.templateWatcher != null )
				resetWatcher();
		}
	}


	/**
	 * Sets the output directory.
	 * @param outputDirectory the output directory
	 */
	public void setOutputDirectory( String outputDirectory ) {

		// Update the configuration
		this.logger.fine( "Output directory is now... " + outputDirectory );
		this.outputDIR = Utils.isEmptyOrWhitespaces( outputDirectory ) ? null : new File( outputDirectory );

		// Generate the files
		if( this.outputDIR != null && this.dm != null ) {
			for( ManagedApplication ma : this.dm.applicationMngr().getManagedApplications())
				application( ma.getApplication(), EventType.CHANGED );
		}
	}


	/**
	 * Binds the DM.
	 * @param dm
	 */
	public void bindManager( Manager dm ) {

		synchronized( this.generationLock ) {
			this.logger.fine( "The DM is now available in the templating manager." );
			this.dm = dm;
		}
	}


	/**
	 * Unbinds the DM.
	 * @param dm
	 */
	public void unbindManager( Manager dm ) {

		synchronized( this.generationLock ) {
			this.logger.fine( "The DM is NOT available anymore in the templating manager." );
			this.dm = null;
		}
	}


	// iPojo life cycle


	/**
	 * Starts the templating manager (invoked by iPojo).
	 */
	public void start() {

		synchronized( this.watcherLock ) {
			this.logger.config( "The templating manager is starting..." );
			resetWatcher();
		}
	}


	/**
	 * Stops the templating manager (invoked by iPojo).
	 */
	public void stop() {

		synchronized( this.watcherLock ) {
			this.logger.config( "The templating manager is stopping..." );
			stopWatcher();
		}
	}


	// Inherited methods


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.management.events.IDmListener#getId()
	 */
	@Override
	public String getId() {
		return ID;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.management.events.IDmListener
	 * #application(net.roboconf.core.model.beans.Application, net.roboconf.dm.management.events.EventType)
	 */
	@Override
	public void application( Application application, EventType eventType ) {

		if( this.outputDIR == null ) {
			this.logger.warning( "Generation from templates is skipped. Invalid output directory." );

		} else if( eventType == EventType.DELETED ) {
			synchronized( this.generationLock ) {
				TemplateUtils.deleteGeneratedFiles( application, this.outputDIR );
			}

		} else {
			generate( application );
		}
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.management.events.IDmListener
	 * #applicationTemplate(net.roboconf.core.model.beans.ApplicationTemplate, net.roboconf.dm.management.events.EventType)
	 */
	@Override
	public void applicationTemplate( ApplicationTemplate tpl, EventType eventType ) {
		// nothing
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.management.events.IDmListener
	 * #instance(net.roboconf.core.model.beans.Instance, net.roboconf.core.model.beans.Application, net.roboconf.dm.management.events.EventType)
	 */
	@Override
	public void instance( Instance instance, Application application, EventType eventType ) {
		application( application, EventType.CHANGED );
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.management.events.IDmListener
	 * #raw(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void raw( String message, Object... data ) {
		// nothing
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.management.events.IDmListener
	 * #enableNotifications()
	 */
	@Override
	public void enableNotifications() {
		// nothing
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.management.events.IDmListener
	 * #disableNotifications()
	 */
	@Override
	public void disableNotifications() {
		// nothing
	}


	// Public methods


	/**
	 * Processes new templates.
	 * <p>
	 * They are applied to all the applications, provided they match
	 * the criteria defined by the template files.
	 * </p>
	 *
	 * @param newTemplates a non-null list of templates
	 */
	public void processNewTemplates( Collection<TemplateEntry> newTemplates ) {

		if( this.dm != null ) {
			for( ManagedApplication ma : this.dm.applicationMngr().getManagedApplications()) {
				Collection<TemplateEntry> filteredTemplates = TemplateUtils.findTemplatesForApplication( ma.getName(), newTemplates );
				generate( ma.getApplication(), filteredTemplates );
			}
		}
	}


	// Private methods


	/**
	 * Updates the template watcher after a change in the templating manager configuration.
	 */
	private void resetWatcher() {

		// Stop the current watcher.
		stopWatcher();

		// Update the template & target directories, based on the provided configuration directory.
		if( this.templatesDIR == null ) {
			this.logger.warning( "The templates directory was not specified. DM templating is temporarily disabled." );

		} else if( this.outputDIR == null ) {
			this.logger.warning( "The templates directory was not specified. DM templating is temporarily disabled." );

		} else {
			this.templateWatcher = new TemplateWatcher( this, this.templatesDIR, this.pollInterval );
			this.templateWatcher.start();
		}
	}


	/**
	 * Stops the watcher, if any.
	 */
	private void stopWatcher() {

		if( this.templateWatcher != null ) {
			try {
				this.templateWatcher.stop();

			} finally {
				this.templateWatcher = null;
			}
		}
	}


	/**
	 * Generates files from templates for a given application.
	 * <p>
	 * Templates are retrieved from the watcher.
	 * </p>
	 *
	 * @param application an application (not null)
	 */
	void generate( Application application ) {

		// Get the templates
		Collection<TemplateEntry> templates = Collections.emptyList();
		synchronized( this.watcherLock ) {
			if( this.templateWatcher != null )
				templates = this.templateWatcher.findTemplatesForApplication( application.getName());
		}

		// Process them
		generate( application, templates );
	}


	/**
	 * Generates files from templates for a given application.
	 * <p>
	 * Templates are provided in parameters.
	 * </p>
	 *
	 * @param application an application (not null)
	 * @param templates a non-null collection of templates
	 */
	void generate( Application application, Collection<TemplateEntry> templates ) {

		try {
			synchronized( this.generationLock ) {
				TemplateUtils.generate( application, this.outputDIR, templates, this.logger );
			}

		} catch( IOException e ) {
			this.logger.warning( "An error occurred while generating files from templates for application " + application );
			Utils.logException( this.logger, e );

		} catch( Throwable e ) {
			this.logger.warning( "An unexpected error occurred while generating files from templates for application " + application );
			Utils.logException( this.logger, new UndeclaredThrowableException( e ));
		}
	}
}
