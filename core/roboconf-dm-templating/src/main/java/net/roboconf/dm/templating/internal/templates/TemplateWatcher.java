/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.templating.internal.templates;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.CanReadFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.StringTemplateSource;

import net.roboconf.core.utils.Utils;
import net.roboconf.dm.templating.internal.TemplatingManager;
import net.roboconf.dm.templating.internal.helpers.AllHelper;
import net.roboconf.dm.templating.internal.helpers.IsKeyHelper;

/**
 * A file system watcher dedicated to the Roboconf application templates.
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class TemplateWatcher extends FileAlterationListenerAdaptor {

	private static final ThreadFactory THREAD_FACTORY = new WatcherThreadFactory();

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final AtomicBoolean alreadyStarted = new AtomicBoolean( false );

	private final ReadWriteLock lock = new ReentrantReadWriteLock( true );
	private final Handlebars handlebars = new Handlebars();

	// FIXME: should we REALLY keep pre-compiled templates in memory?
	private final Map<File,TemplateEntry> fileToTemplate = new HashMap<> ();

	private final TemplatingManager manager;
	private final File templateDir;
	private final FileAlterationMonitor monitor;


	/**
	 * Constructor.
	 * @param manager the templating manager, to which event handling is delegated.
	 * @param templateDir the templates directory to watch.
	 * @param pollInterval the poll interval.
	 * @throws IOException if there is a problem watching the template directory.
	 */
	public TemplateWatcher( final TemplatingManager manager, final File templateDir, final long pollInterval ) {
		this.templateDir = templateDir;

		// Register the custom helpers.
		this.handlebars.registerHelper( AllHelper.NAME, new AllHelper());
		this.handlebars.registerHelper( IsKeyHelper.NAME, new IsKeyHelper());

		// Pretty formatting
		this.handlebars.prettyPrint( true );

		// Create the observer, register this object as the event listener.
		FileFilter fileFilter = FileFilterUtils.or(
				FileFilterUtils.and(
						FileFilterUtils.fileFileFilter(),
						FileFilterUtils.suffixFileFilter( ".tpl" ),
						CanReadFileFilter.CAN_READ,
						new TemplateFileFilter(templateDir)),
				FileFilterUtils.and(
						FileFilterUtils.directoryFileFilter(),
						CanReadFileFilter.CAN_READ,
						new TemplateSubDirectoryFileFilter(templateDir))
		);

		FileAlterationObserver observer = new FileAlterationObserver( this.templateDir, fileFilter );
		observer.addListener( this );

		// Create the monitor.
		this.monitor = new FileAlterationMonitor( pollInterval, observer );
		this.monitor.setThreadFactory( THREAD_FACTORY );

		this.manager = manager;
		this.logger.fine( "Template watcher is watching "
				+ this.templateDir
				+ " with an interval of " + pollInterval + " ms." );
	}


	/**
	 * Starts this template watcher.
	 * GuardedBy this.manager.globalLock.writeLock()
	 */
	public void start() {
		try {
			this.monitor.start();

		} catch( final Exception e ) {
			this.logger.warning("Cannot start template watcher");
			Utils.logException(this.logger, e);
		}
	}


	/**
	 * Stops this template watcher.
	 * GuardedBy this.manager.globalLock.writeLock()
	 */
	public void stop() {
		try {
			this.monitor.stop();

		} catch( final Exception e ) {
			this.logger.warning("Cannot stop template watcher");
			Utils.logException(this.logger, e);
		}
	}


	/**
	 * Finds the templates that can apply to a given application.
	 * <p>The templates contained in the returned set may have been removed at the time they are accessed.</p>
	 *
	 * @param appName the name of the application, or {@code null} to only get the global templates
	 * @return a non-null list
	 */
	public Collection<TemplateEntry> findTemplatesForApplication( final String appName ) {

		final Collection<TemplateEntry> result = new ArrayList<> ();
		this.lock.readLock().lock();
		try {
			result.addAll( TemplateUtils.findTemplatesForApplication( appName, this.fileToTemplate.values()));

		} finally {
			this.lock.readLock().unlock();
		}

		return result;
	}

	//
	// FileAlterationListener methods.
	//

	@Override
	public void onStart( final FileAlterationObserver observer ) {

		if( this.alreadyStarted.getAndSet( true ))
			return;

		this.logger.fine("Initial provisioning of templates...");
		final Collection<File> templateFiles = FileUtils.listFiles(
				this.templateDir,

				// Find readable template files.
				FileFilterUtils.and(
						FileFilterUtils.suffixFileFilter( ".tpl" ),
						CanReadFileFilter.CAN_READ),

				// Directory filter: go through the root template directory and its direct children.
				new TemplateDirectoryFileFilter( this.templateDir ));

		process( templateFiles );
	}


	@Override
	public void onFileCreate( final File file ) {
		this.logger.fine( "Template file " + file + " has just been created. Generating files..." );
		process( Collections.singletonList( file ));
	}


	@Override
	public void onFileChange( final File file ) {
		this.logger.fine( "Template file " + file + " changed. Updating the generated files..." );
		process( Collections.singletonList( file ));
	}


	@Override
	public void onFileDelete( final File file ) {

		this.logger.fine( "Template file " + file + " was deleted. Generated files won't be removed automatically." );
		this.lock.writeLock().lock();
		try {
			this.fileToTemplate.remove( file );

		} finally {
			this.lock.writeLock().unlock();
		}

		// Since generated files are not removed automatically,
		// the manager does not need to be notified.
	}


	/**
	 * Compiles the given template file and create the associated template entry.
	 * <p>IO and compile errors are logged but not rethrown.</p>
	 *
	 * @param templateFile the template file to compile
	 * @return the created template entry, or {@code null} if any problem occurred
	 */
	public TemplateEntry compileTemplate( final File templateFile ) {

		TemplateEntry templateEntry = null;
		try {
			// Parse the template's content and find the (optional) output
			String templateFileContent = Utils.readFileContent( templateFile );
			Matcher m = Pattern.compile( "\\{\\{!\\s*roboconf-output:(.*)\\}\\}" ).matcher( templateFileContent.trim());

			String targetFilePath = null;
			if( m.find())
				targetFilePath = m.group( 1 ).trim();

			// Compile the template file
			final Template template = this.handlebars.compile(
					new StringTemplateSource(
							templateFile.toString(),
							templateFileContent ));

			// Create the entry
			templateEntry = new TemplateEntry(
					templateFile, targetFilePath, template,
					TemplateUtils.findApplicationName( this.templateDir, templateFile ));

		} catch( IOException | IllegalArgumentException | HandlebarsException e ) {
			this.logger.warning("Cannot compile template " + templateFile);
			Utils.logException(this.logger, e);
		}

		return templateEntry;
	}


	/**
	 * Processes (compiles and registers) a collection of template files.
	 * @param templateFiles a non-null collection
	 */
	private void process( Collection<File> templateFiles ) {

		// Compile them all
		Collection<TemplateEntry> templateEntries = new ArrayList<> ();
		for( File f : templateFiles ) {
			final TemplateEntry templateEntry = compileTemplate( f );
			if( templateEntry != null )
				templateEntries.add( templateEntry );
		}

		// Add all the template entries
		this.lock.writeLock().lock();
		try {
			for( final TemplateEntry te : templateEntries )
				this.fileToTemplate.put( te.getTemplateFile(), te );

		} finally {
			this.lock.writeLock().unlock();
		}

		// Notify the templating manager
		this.manager.processNewTemplates( templateEntries );
	}


	/**
	 * A file filter that only matches template directories.
	 * <p>
	 * The template directories include the root directory and its first-level
	 * children. It is guaranteed that this filter is only called with directory files.
	 * </p>
	 *
	 * @author Pierre Bourret - Université Joseph Fourier
	 */
	static class TemplateDirectoryFileFilter extends AbstractFileFilter {
		final File rootTemplateDir;

		/**
		 * Creates a template directory file filter.
		 * @param rootTemplateDir the root template directory
		 */
		TemplateDirectoryFileFilter( File rootTemplateDir ) {
			this.rootTemplateDir = rootTemplateDir;
		}

		@Override
		public boolean accept( File file ) {
			return this.rootTemplateDir.equals(file) || this.rootTemplateDir.equals( file.getParentFile());
		}
	}


	/**
	 * File filter that only selects sub-template directories.
	 * @author Pierre Bourret - Université Joseph Fourier
	 */
	static class TemplateSubDirectoryFileFilter extends AbstractFileFilter {
		private final File rootTemplateDir;

		/**
		 * Creates a template sub-directory file filter.
		 * @param rootTemplateDir the root template directory.
		 */
		TemplateSubDirectoryFileFilter( final File rootTemplateDir ) {
			this.rootTemplateDir = rootTemplateDir;
		}

		@Override
		public boolean accept( File file ) {
			return this.rootTemplateDir.equals(file.getParentFile());
		}
	}



	/**
	 * File filter that only selects template files that are in the root template directory, or in a first-level
	 * sub-directory.
	 * @author Pierre Bourret - Université Joseph Fourier
	 */
	static class TemplateFileFilter extends AbstractFileFilter {
		private final File rootTemplateDir;

		/**
		 * Creates a template file filter.
		 * @param rootTemplateDir the root template directory.
		 */
		TemplateFileFilter( final File rootTemplateDir ) {
			this.rootTemplateDir = rootTemplateDir;
		}

		@Override
		public boolean accept( File file ) {
			final File parentDir = file.getParentFile();
			return this.rootTemplateDir.equals( parentDir ) || this.rootTemplateDir.equals( parentDir.getParentFile());
		}
	}


	/**
	 * Factory for the watcher thread.
	 * @author Pierre Bourret - Université Joseph Fourier
	 */
	private static final class WatcherThreadFactory implements ThreadFactory {
		@Override
		public Thread newThread( final Runnable r ) {
			return new Thread( r, "Roboconf's Templates Watcher" );
		}
	}
}
