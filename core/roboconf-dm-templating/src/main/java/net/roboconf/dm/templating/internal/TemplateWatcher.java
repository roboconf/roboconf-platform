/**
 * Copyright 2013-2015 Linagora, Université Joseph Fourier, Floralis
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

import net.roboconf.core.utils.Utils;
import net.roboconf.dm.templating.TemplatingService;

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

/**
 * A file system watcher dedicated to the Roboconf application templates.
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class TemplateWatcher extends FileAlterationListenerAdaptor {

	/**
	 * The templating manager.
	 */
	private final TemplatingManager manager;

	/**
	 * The templates directory.
	 */
	private final File templateDir;

	/**
	 * The template alteration monitor.
	 */
	private final FileAlterationMonitor monitor;

	/**
	 * Flag indicating if this watcher has already tracked down the templates already present in the template
	 * directory.
	 */
	private final AtomicBoolean hasBeenProvisioned = new AtomicBoolean(false);

	/**
	 * The lock for keeping watched templates.
	 */
	private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

	/**
	 * The templates being watched, index by the name of the scoped application (or {@code null} for global templates)
	 * and then by the template identifier.
	 *
	 * @GuardedBy lock
	 */
	private final Map<String, Map<String, TemplateEntry>> templates = new HashMap<String, Map<String, TemplateEntry>>();

	/**
	 * The Handlebars template processor.
	 */
	private final Handlebars handlebars = new Handlebars();

	/**
	 * The logger.
	 */
	private final Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Factory for the watcher thread.
	 */
	private static final class WatcherThreadFactory implements ThreadFactory {
		@Override
		public Thread newThread( final Runnable r ) {
			return new Thread(r, "Roboconf.TemplateWatcher");
		}
	}

	/**
	 * The instance of the the watcher thread factory.
	 */
	private static final ThreadFactory THREAD_FACTORY = new WatcherThreadFactory();

	/**
	 * A file filter that only matches template directories, i.e. the root template directory and its first-level
	 * children. It is guaranteed that this filter is only called with directory files.
	 */
	private static class TemplateDirectoryFileFilter extends AbstractFileFilter {
		/**
		 * The root template directory.
		 */
		final File rootTemplateDir;

		/**
		 * Create a template directory file filter.
		 *
		 * @param rootTemplateDir the root template directory.
		 */
		TemplateDirectoryFileFilter( final File rootTemplateDir ) {
			this.rootTemplateDir = rootTemplateDir;
		}

		@Override
		public boolean accept( final File file ) {
			return this.rootTemplateDir.equals(file) || this.rootTemplateDir.equals(file.getParentFile());
		}
	}

	/**
	 * Create a template watcher.
	 *
	 * @param manager      the templating manager, to which event handling is delegated.
	 * @param templateDir  the templates directory to watch.
	 * @param pollInterval the poll interval.
	 * @throws IOException if there is a problem watching the template directory.
	 */
	public TemplateWatcher( final TemplatingManager manager, final File templateDir, final long pollInterval )
	throws IOException {
		this.manager = manager;

		// Register the custom helpers.
		this.handlebars.registerHelper(AllHelper.NAME, AllHelper.INSTANCE);

		// We need the canonical directory, in order to test relationship w/ templating candidates.
		this.templateDir = templateDir.getCanonicalFile();

		// Create the observer, register this object as the event listener.
		final FileAlterationObserver observer = new FileAlterationObserver(
				this.templateDir, TemplateEntry.getTemplateFileFilter(this.templateDir));
		observer.addListener(this);

		// Create the monitor.
		this.monitor = new FileAlterationMonitor(pollInterval, observer);
		this.monitor.setThreadFactory(THREAD_FACTORY);

		this.logger.fine("Template watcher configured with templateDir=" + this.templateDir.toString()
				+ " and pollInterval=" + pollInterval);
	}

	/**
	 * Start this template watcher.
	 *
	 * @GuardedBy this.manager.globalLock.writeLock()
	 */
	public void start() {
		try {
			this.monitor.start();
		} catch (final Exception e) {
			this.logger.warning("Cannot start template watcher");
			Utils.logException(this.logger, e);
		}
	}

	/**
	 * Stop this template watcher.
	 *
	 * @GuardedBy this.manager.globalLock.writeLock()
	 */
	public void stop() {
		try {
			this.monitor.stop();
		} catch (final Exception e) {
			this.logger.warning("Cannot stop template watcher");
			Utils.logException(this.logger, e);
		}
	}

	/**
	 * Get the list of the templates currently tracked, for the application with the given name.
	 *
	 * <p>The templates contained in the returned set may have been removed at the time they are accessed.</p>
	 *
	 * @param appName the name of the application, or {@code null} to only get the global templates.
	 * @return the templates scoping the application with the given name, <em>including</em> the global templates.
	 */
	public Collection<TemplateEntry> getTemplates( final String appName ) {
		this.lock.readLock().lock();
		try {
			final Collection<TemplateEntry> result = new ArrayList<TemplateEntry>();
			// Process the application-specific templates.
			if (appName != null) {
				Map<String, TemplateEntry> appSpecificTemplates = this.templates.get(appName);
				if (appSpecificTemplates != null) {
					result.addAll(appSpecificTemplates.values());
				}
			}
			// Process the global templates.
			Map<String, TemplateEntry> globalTemplates = this.templates.get(null);
			if (globalTemplates != null) {
				result.addAll(globalTemplates.values());
			}
			return Collections.unmodifiableCollection(result);
		} finally {
			this.lock.readLock().unlock();
		}
	}

	/**
	 * Compile the given template file and create the associated template entry.
	 *
	 * <p>IO and compile errors are logged but not rethrown.</p>
	 *
	 * @param templateFile the template file to compile.
	 * @return the created template entry, or {@code null} if any problem has occurred.
	 */
	private TemplateEntry compileTemplate( final File templateFile ) {
		TemplateEntry templateEntry = null;
		try {
			// Compile the template file.
			final Template template = this.handlebars.compile(
					new StringTemplateSource(
							templateFile.toString(),
							Utils.readFileContent(templateFile)));
			// Create the entry.
			templateEntry = new TemplateEntry(
					templateFile,
					TemplateEntry.getTemplateId(templateFile),
					TemplateEntry.getTemplateApplicationName(this.templateDir, templateFile),
					template);
		} catch (final IOException e) {
			this.logger.warning("Cannot compile template " + templateFile);
			Utils.logException(this.logger, e);
		} catch (final IllegalArgumentException e) {
			this.logger.warning("Cannot compile template " + templateFile);
			Utils.logException(this.logger, e);
		} catch (final HandlebarsException e) {
			this.logger.warning("Cannot compile template " + templateFile);
			Utils.logException(this.logger, e);
		}
		return templateEntry;
	}

	/**
	 * Add/update the given template entry in the {@code templates} map.
	 *
	 * @param templateEntry the template entry to add/update.
	 * @GuardedBy this.lock.writeLock()
	 */
	private void updateTemplateEntry( final TemplateEntry templateEntry ) {
		Map<String, TemplateEntry> specificTemplates = this.templates.get(templateEntry.appName);
		if (specificTemplates == null) {
			specificTemplates = new HashMap<String, TemplateEntry>();
			this.templates.put(templateEntry.appName, specificTemplates);
		}
		specificTemplates.put(templateEntry.id, templateEntry);
	}

	//
	// FileAlterationListener methods.
	//

	@Override
	public void onStart( final FileAlterationObserver observer ) {
		if (!this.hasBeenProvisioned.getAndSet(true)) {
			// We must list the templates already present in the directory, as they won't otherwise trigger any event,
			// and thus be ignored.
			this.logger.fine("Initial provisioning of templates...");

			// Find all the template files: the global *and* the specific ones.
			final Collection<File> templateFiles = FileUtils.listFiles(
					this.templateDir,
					// Regular file filter:
					// List readable files with the template extension.
					FileFilterUtils.and(
							FileFilterUtils.suffixFileFilter(TemplatingService.TEMPLATE_FILE_EXTENSION),
							CanReadFileFilter.CAN_READ),
					// Directory filter:
					// Go through the root template directory and its direct children.
					new TemplateDirectoryFileFilter(this.templateDir));

			// Compile all the template files, outside of the critical section.
			final Collection<TemplateEntry> templateEntries = new ArrayList<TemplateEntry>();
			for (final File templateFile : templateFiles) {
				final TemplateEntry templateEntry = compileTemplate(templateFile);
				if (templateEntry != null) {
					templateEntries.add(templateEntry);
					this.logger.finest("++ Added " + templateEntry);
				}
			}

			// Add all the template entries.
			this.lock.writeLock().lock();
			try {
				for (final TemplateEntry templateEntry : templateEntries) {
					updateTemplateEntry(templateEntry);
				}
			} finally {
				this.lock.writeLock().unlock();
			}
			// Notify the manager, outside of the lock.
			this.manager.processTemplates(templateEntries);
		}
	}

	@Override
	public void onFileCreate( final File file ) {
		// Compile the template file, outside of the critical section.
		final TemplateEntry templateEntry = compileTemplate(file);
		if (templateEntry != null) {

			// Update the template map.
			this.lock.writeLock().lock();
			try {
				updateTemplateEntry(templateEntry);
				this.logger.finest("++ Added " + templateEntry);
			} finally {
				this.lock.writeLock().unlock();
			}
			// Notify the manager, outside of the critical section.
			this.manager.processTemplates(Collections.singletonList(templateEntry));
		}
	}

	@Override
	public void onFileChange( final File file ) {
		// Compile the template file, outside of the critical section.
		final TemplateEntry templateEntry = compileTemplate(file);
		if (templateEntry != null) {

			// Update the template map.
			this.lock.writeLock().lock();
			try {
				updateTemplateEntry(templateEntry);
				this.logger.finest("!! Updated " + file);
			} finally {
				this.lock.writeLock().unlock();
			}
			// Notify the manager, outside of the critical section.
			this.manager.processTemplates(Collections.singletonList(templateEntry));
		}
	}

	@Override
	public void onFileDelete( final File file ) {
		final String appName = TemplateEntry.getTemplateApplicationName(this.templateDir, file);
		final String id = TemplateEntry.getTemplateId(file);
		this.lock.writeLock().lock();
		try {
			Map<String, TemplateEntry> specificTemplates = this.templates.get(appName);
			if (specificTemplates != null) {
				specificTemplates.remove(id);
				if (specificTemplates.isEmpty()) {
					this.templates.remove(appName);
				}
			}
			this.logger.finest("-- Removed " + file);
		} finally {
			this.lock.writeLock().unlock();
		}
		// The manager does not need to be notified.
	}

}
