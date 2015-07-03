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
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.TemplatingManagerService;
import net.roboconf.dm.templating.TemplatingService;

import org.apache.commons.io.filefilter.CanReadFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.HandlebarsException;


/**
 * Component dedicated to the generation of reports for Roboconf applications.
 * @author Pierre Bourret - Université Joseph Fourier
 */
@Component(name = "roboconf-templating-manager", managedservice = "net.roboconf.dm.templating.configuration", publicFactory = false)
@Provides(specifications = {TemplatingManagerService.class, TemplatingService.class})
@Instantiate(name = "Roboconf Templating Manager")
public class TemplatingManager implements TemplatingManagerService, TemplatingService {


	/**
	 * The big bad global lock.
	 */
	private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

	/**
	 * The Roboconf configuration directory.
	 * GuardedBy this.lock
	 * @see #startTemplating(File)
	 */
	private File configDir;

	/**
	 * The templates directory: {@code ${configDir}}/{@value #TEMPLATE_DIRECTORY}.
	 *
	 * GuardedBy this.lock
	 * @see #resetTemplateWatcher()
	 */
	private File templateDir;

	/**
	 * The target directory: {@code ${configDir}}/{@value #TARGET_DIRECTORY}.
	 *
	 * GuardedBy this.lock
	 * @see #resetTemplateWatcher()
	 */
	private File targetDir;

	/**
	 * The poll interval for the template watcher, or any negative value to disable.
	 *
	 * GuardedBy this.lock
	 * @see #setPollInterval(long)
	 */
	private long pollInterval;

	/**
	 * The monitored applications, indexed by name.
	 *
	 * GuardedBy this.lock
	 * @see #addApplication(Application)
	 * @see #removeApplication(Application)
	 * @see #stopTemplating()
	 */
	private final Map<String, MonitoredApplication> applications = new HashMap<String, MonitoredApplication>();

	/**
	 * The template watcher.
	 * GuardedBy this.lock
	 */
	private TemplateWatcher templateWatcher;

	/**
	 * The logger.
	 */
	private final Logger logger = Logger.getLogger(this.getClass().getName());


	/**
	 * Sets the poll interval for the template watcher.
	 * @param pollInterval the poll interval, in milliseconds, for the template watcher.
	 */
	@Property(name = "pollInterval", value = "1000")
	public void setPollInterval( final long pollInterval ) {
		this.lock.writeLock().lock();
		try {
			this.pollInterval = pollInterval;
			this.logger.config("Template watcher poll interval set to " + pollInterval);

			// The template watcher needs to be updated.
			resetTemplateWatcher();
		} finally {
			this.lock.writeLock().unlock();
		}
	}


	/**
	 * Starts this iPOJO component instance.
	 */
	@Validate
	public void start() {
		this.lock.writeLock().lock();
		try {
			this.logger.fine("Component instance is starting...");

			// Just trigger a reset of the template watcher, it should start it, if it is well-configured.
			resetTemplateWatcher();

			this.logger.fine("Component instance is started!");
		} finally {
			this.lock.writeLock().unlock();
		}
	}


	/**
	 * Stops this iPOJO component instance.
	 */
	@Invalidate
	public void stop() {
		this.lock.writeLock().lock();
		try {
			this.logger.fine("Component instance is stopping...");

			// Stop the template watcher, only if it was running.
			if (this.templateWatcher != null) {
				this.templateWatcher.stop();
				this.templateWatcher = null;
			}

			this.logger.fine("Component instance is stopped!");
		} finally {
			this.lock.writeLock().unlock();
		}
	}


	/**
	 * Updates the template watcher after a change in the templating manager configuration.
	 * GuardedBy this.lock.writeLock()
	 */
	private void resetTemplateWatcher() {
		// The template & poll interval may have changed. The template watcher must be updated because it is tracking
		// the templates at a wrong locations now. As hot-updates are *not* possible, and because the template watcher
		// may need to be stopped completely, we first shut it down, and start it again later, if really needed.
		if (this.templateWatcher != null) {
			this.templateWatcher.stop();
			this.templateWatcher = null;
		}

		// Update the template & target directories, based on the provided configuration directory.
		if (this.configDir != null) {
			this.logger.config("Reconfiguring template watcher...");

			this.templateDir = new File(this.configDir, TEMPLATE_DIRECTORY);
			try {
				Utils.createDirectory(this.templateDir);
			} catch (final IOException e) {
				this.logger.severe("Cannot access to template directory: " + this.templateDir);
				Utils.logException(this.logger, e);
			}

			this.targetDir = new File(this.configDir, TARGET_DIRECTORY);
			try {
				Utils.createDirectory(this.targetDir);
			} catch (final IOException e) {
				this.logger.severe("Cannot access to target directory: " + this.targetDir);
				Utils.logException(this.logger, e);
			}

			// Start the template watcher again.
			try {
				this.templateWatcher = new TemplateWatcher(this, this.templateDir, this.pollInterval);
				this.templateWatcher.start();
				// The initial provisioning of the template watcher will trigger a full generation of the all the
				// templating reports, for each monitored application. So the new target directory will be repopulated
				// promptly.
			} catch (final IOException e) {
				this.logger.warning("Cannot create template watcher");
				Utils.logException(this.logger, e);
			}
		} else {
			this.logger.config("Templating manager is disabled: no configuration directory set!");
			this.templateDir = null;
			this.targetDir = null;
			// We do *not* restart the template watcher here, as we wait for the configuration directory to be set.
		}
	}

	//
	// TemplatingManagerService methods
	//

	@Override
	public void startTemplating( final File configDir ) {
		Objects.requireNonNull(configDir, "configDir is null");
		this.lock.writeLock().lock();
		try {
			if (this.configDir == null) {
				this.logger.fine("Templating is starting...");
				this.configDir = configDir;
				this.logger.config("Configuration directory set to " + this.configDir);

				// The TemplatingManagerService method declaration clearly specifies that all the registered
				// applications are cleared. This is required since the DM, when reconfigured, stores, removes, and
				// restore all the applications. When restored, the DM loadApplication() method calls the
				// addApplication() of this class.
				this.logger.fine("Monitored applications have been removed: " + this.applications.keySet());
				this.applications.clear();

				// The template watcher is going to be started.
				resetTemplateWatcher();
				this.logger.fine("Templating is started!");
			}
		} finally {
			this.lock.writeLock().unlock();
		}
	}


	@Override
	public void stopTemplating() {
		this.lock.writeLock().lock();
		try {
			if (this.configDir != null) {
				this.logger.info("Templating is stopping...");
				this.configDir = null;
				this.applications.clear();

				// The template watcher is going to be stopped.
				resetTemplateWatcher();
				this.logger.info("Templating is stopped!");
			}
		} finally {
			this.lock.writeLock().unlock();
		}
	}


	/**
	 * Does nothing if templating is started, fail otherwise.
	 *
	 * @throws IllegalStateException if templating is not started.
	 * GuardedBy this.lock
	 */
	private void ensureTemplatingIsStarted() {
		if (this.configDir == null) {
			throw new IllegalStateException("templating not started");
		}
	}


	@Override
	public void addApplication( final Application app ) {

		// First create the monitored application entry.
		final String name = app.getName();
		final MonitoredApplication monitoredApp = new MonitoredApplication(app);
		final boolean added;

		this.lock.writeLock().lock();
		try {
			ensureTemplatingIsStarted();
			// Check if the application is already in the map.
			if (!this.applications.containsKey(name)) {
				// Add the monitored application entry.
				this.applications.put(name, monitoredApp);
				added = true;
				this.logger.info("Monitored application has been added: " + name);

			} else {
				added = false;
			}

		} finally {
			this.lock.writeLock().unlock();
		}

		// Refresh the application's context & generate reports, outside of the lock.
		if (added) {
			processApplication(monitoredApp);
		}
	}


	@Override
	public void updateApplication( final Application app ) {
		final String name = app.getName();
		final MonitoredApplication monitored;
		this.lock.readLock().lock();
		try {
			ensureTemplatingIsStarted();
			monitored = this.applications.get(name);
		} finally {
			this.lock.readLock().unlock();
		}
		// Refresh the application's context & generate reports, outside of the lock.
		if (monitored != null) {
			this.logger.info("Monitored application is being updated: " + name);
			processApplication(monitored);
		}
	}


	@Override
	public void removeApplication( final Application app ) {
		final String name = app.getName();
		this.lock.writeLock().lock();
		try {
			ensureTemplatingIsStarted();
			// Check if the application is already is the map.
			if (this.applications.containsKey(name)) {
				this.applications.remove(name);
				this.logger.info("Monitored application has been removed: " + name);
			}
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	//
	// Template processing methods.
	//

	/**
	 * Generates the reports for all the applications, for the given templates.
	 * <p>Called by the template watcher when templates appear or are modified.</p>
	 *
	 * @param templates the template to process.
	 */
	public void processTemplates( final Collection<TemplateEntry> templates ) {
		this.logger.info("Processing templates: " + templates + "...");

		// Get the templating contexts for each application.
		final Map<String, Context> contexts = new HashMap<String, Context>();
		final File targetDir;
		this.lock.readLock().lock();
		try {
			targetDir = this.targetDir;
			if (targetDir != null) {
				for (final MonitoredApplication app : this.applications.values()) {
					contexts.put(app.getModel().getName(), app.getCurrentContext());
				}
			} else {
				this.logger.warning("Target directory is not configured, cannot process templates!");
			}

		} finally {
			this.lock.readLock().unlock();
		}

		// For each template to process...
		for (final TemplateEntry templateEntry : templates) {
			if (templateEntry.appName != null) {

				// Application-specific template, only apply the scoped application context (if it exists).
				final Context appContext = contexts.get(templateEntry.appName);
				if (appContext != null) {
					process(templateEntry.appName, appContext, templateEntry, targetDir);

				} else {
					this.logger.finest("Cannot process template " + templateEntry + ": application " +
							templateEntry.appName + " is not monitored");
				}

			} else {
				// Global template, apply each one of the existing application contexts.
				for (final Entry<String, Context> contextEntry : contexts.entrySet()) {
					process(contextEntry.getKey(), contextEntry.getValue(), templateEntry, targetDir);
				}
			}
		}
	}


	/**
	 * Generates all the reports for the given application.
	 * <p>Called by the add/updateApplication methods.</p>
	 *
	 * @param app the application.
	 */
	private void processApplication( final MonitoredApplication app ) {

		final String name = app.getModel().getName();
		final Context context;
		final Collection<TemplateEntry> templates;
		final File targetDir;

		this.lock.readLock().lock();
		try {
			if (this.templateWatcher != null) {
				templates = this.templateWatcher.getTemplates(name);
				targetDir = this.targetDir;
			} else {
				templates = null;
				targetDir = null;
			}
			context = app.getCurrentContext();
		} finally {
			this.lock.readLock().unlock();
		}

		// Process outside of the lock.
		if (templates != null) {
			if (!templates.isEmpty()) {
				for (final TemplateEntry templateEntry : templates) {
					process(name, context, templateEntry, targetDir);
				}
			} else {
				this.logger.fine("No template to process for application " + name);
			}
		} else {
			this.logger.warning("Template watcher is disabled. No template can be retrieved for application " + name);
		}
	}


	/**
	 * Generates the reports for one application using one template.
	 *
	 * @param appName       the name of the application
	 * @param context       the templating context of the application.
	 * @param templateEntry the template entry.
	 * @param targetDir     the target directory.
	 */
	private void process( final String appName, final Context context, final TemplateEntry templateEntry,
						  final File targetDir ) {
		// Compute the target file location:
		// - targetDir/appName/templateId for app-specific templates,
		// - targetDir/appName.templateId for global templates.
		final File parentDir;
		final File targetFile;
		if (templateEntry.appName != null) {
			parentDir = new File(targetDir, appName);
			targetFile = new File(parentDir, templateEntry.id);
		} else {
			parentDir = targetDir;
			targetFile = new File(targetDir, appName + '.' + templateEntry.id);
		}

		try {
			this.logger.fine("Applying template " + templateEntry.id + " to application " + appName + "...");

			// Apply the context to the template.
			final String output = templateEntry.template.apply(context);
			// Create the parent directory, as it may not yet exist.
			Utils.createDirectory(parentDir);
			// Write the content to the target file.
			Utils.writeStringInto(output, targetFile);

		} catch (final IOException e) {
			this.logger.warning("Cannot apply template " + templateEntry.id + " to application " + appName);
			Utils.logException(this.logger, e);
		} catch (final HandlebarsException e) {
			this.logger.warning("Cannot apply template " + templateEntry.id + " to application " + appName);
			Utils.logException(this.logger, e);
		} catch (final Throwable e) {
			this.logger.warning("Cannot apply template " + templateEntry.id + " to application " + appName);
			Utils.logException(this.logger, new UndeclaredThrowableException(e));
		}
	}

	//
	// TemplatingService methods
	//

	@Override
	public File getTargetDirectory() {
		this.lock.readLock().lock();
		try {
			return this.targetDir;
		} finally {
			this.lock.readLock().unlock();
		}
	}


	/**
	 * Gets the template directory for the given application.
	 * @param application the application whose template directory must be retrieved, or {@code null} to get the global template directory.
	 * @return the template directory for the given application, or the global template directory if the
	 * {@code application} parameter is set to {@code null}.
	 */
	private File getTemplateDirectory( final Application application ) {
		// Get the current template directory.
		final File templateDir;
		this.lock.readLock().lock();
		try {
			templateDir = this.templateDir;
		} finally {
			this.lock.readLock().unlock();
		}
		// Append the application name, if not null.
		final File result;
		if (application != null) {
			result = new File(templateDir, application.getName());
		} else {
			result = templateDir;
		}
		return result;
	}


	@Override
	public boolean addTemplate( final Application application, final String name, final InputStream content )
			throws IOException {
		Objects.requireNonNull(name, "name is null");
		Objects.requireNonNull(content, "content is null");
		final File templateDir = getTemplateDirectory(application);
		// Directory may not yet exist, create it!
		Utils.createDirectory(templateDir);

		// Check if the template exists.
		final boolean added;
		final File template = new File(templateDir, name + TEMPLATE_FILE_EXTENSION);
		if (!template.exists()) {
			// Copy the template!
			Utils.copyStream(content, template);
			added = true;
		} else {
			added = false;
		}
		return added;
	}


	@Override
	public Set<String> listTemplates( final Application application ) {
		final File templateDir = getTemplateDirectory(application);

		// List the files, using the exact same filter the watcher uses/would use.
		final File[] templateFiles = templateDir.listFiles(
				(FileFilter) FileFilterUtils.and(
						FileFilterUtils.fileFileFilter(),
						FileFilterUtils.suffixFileFilter(TemplatingService.TEMPLATE_FILE_EXTENSION),
						CanReadFileFilter.CAN_READ));

		// Extract the identifiers from the templates.
		Set<String> templateNames;
		if (templateFiles != null && templateFiles.length > 0) {
			templateNames = new HashSet<String>();
			for (final File templateFile : templateFiles) {
				// Add the template file name, but remove the ".tpl" extension.
				final String name = templateFile.getName();
				templateNames.add(name.substring(0, name.length() - TEMPLATE_FILE_EXTENSION.length()));
			}

			templateNames = Collections.unmodifiableSet(templateNames);

		} else {
			templateNames = Collections.emptySet();
		}

		return templateNames;
	}


	@Override
	public boolean removeTemplate( final Application application, final String name ) throws IOException {
		Objects.requireNonNull(name, "name is null");
		final File template = new File(
				getTemplateDirectory(application),
				name + TEMPLATE_FILE_EXTENSION);

		return template.exists() && template.isFile() && template.delete();
	}
}
