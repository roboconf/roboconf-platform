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

package net.roboconf.dm.templating.internal.contexts;

import static net.roboconf.core.model.helpers.InstanceHelpers.computeInstancePath;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import net.roboconf.core.model.beans.AbstractType;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class ContextUtils {

	/**
	 * Constructor.
	 */
	private ContextUtils() {
		// nothing
	}


	/**
	 * @param app
	 * @return
	 */
	public static ApplicationContextBean toContext( Application app ) {

		final ApplicationContextBean context = new ApplicationContextBean();

		// Model timestamp.
		context.lastModified = new Date();

		// General application properties.
		context.name = app.getName();
		context.description = app.getDescription();

		// Components of the application.
		for (final Component component : ComponentHelpers.findAllComponents(app)) {
			context.components.add(component.getName());
		}

		// Get all the instances, indexed by path, and compute their initial context.
		final Map<String, InstanceContextBean> instancesByPath = new LinkedHashMap<String, InstanceContextBean>();
		final Collection<Instance> allInstances = InstanceHelpers.getAllInstances(app);
		for (final Instance instance : allInstances) {
			final InstanceContextBean instanceContext = new InstanceContextBean();
			final String path = computeInstancePath(instance);

			// General instance properties.
			instanceContext.path = path;
			instanceContext.name = instance.getName();
			instanceContext.status = instance.getStatus();
			instanceContext.statusIsStable = instance.getStatus().isStable();
			instanceContext.component = instance.getComponent().getName();
			instanceContext.types.addAll(findExtendedTypes(instance.getComponent()));
			instanceContext.ip = instance.data.get(Instance.IP_ADDRESS);
			instanceContext.installer = ComponentHelpers.findComponentInstaller(instance.getComponent());

			// Instance exports.
			for (final Map.Entry<String, String> exportEntry : InstanceHelpers.findAllExportedVariables(instance).entrySet()) {
				final VariableContextBean export = new VariableContextBean();
				export.name = exportEntry.getKey();
				export.value = exportEntry.getValue();
				instanceContext.exports.add(export);
			}

			// Instance data.
			for (final Map.Entry<String, String> dataEntry : instance.data.entrySet()) {
				final VariableContextBean data = new VariableContextBean();
				data.name = dataEntry.getKey();
				data.value = dataEntry.getValue();
				instanceContext.data.add(data);
			}

			// Finally add the context to the maps.
			instancesByPath.put(path, instanceContext);
			context.instances.add(instanceContext);
			for (final String typeName : instanceContext.types) {
				Set<InstanceContextBean> instancesOfType = context.instancesByType.get(typeName);
				if (instancesOfType == null) {
					instancesOfType = new LinkedHashSet<InstanceContextBean>();
					context.instancesByType.put(typeName, instancesOfType);
				}
				instancesOfType.add(instanceContext);
			}
		}

		// Once the contexts of all the instances have been created, we can add their relationships, which are:
		// - the parent instance, or null for a root instance.
		// - the children instances
		// - the instances from which variables are imported.
		for (final Instance instance : allInstances) {

			final InstanceContextBean instanceContext = instancesByPath.get(computeInstancePath(instance));
			final Instance parent = instance.getParent();
			if (parent != null)
				instanceContext.parent = instancesByPath.get(computeInstancePath(parent));

			final Collection<Instance> children = instance.getChildren();
			if (!children.isEmpty()) {
				for (final Instance child : children)
					instanceContext.children.add(instancesByPath.get(computeInstancePath(child)));
			}

			for (final Map.Entry<String, Collection<Import>> importedByType : instance.getImports().entrySet()) {
				for (final Import importedVariable : importedByType.getValue()) {

					final ImportContextBean importContext = new ImportContextBean();
					importContext.component = importedVariable.getComponentName();
					importContext.instance = instancesByPath.get(importedVariable.getInstancePath());

					for (Map.Entry<String, String> entry : importedVariable.getExportedVars().entrySet()) {
						final VariableContextBean variable = new VariableContextBean();
						variable.name = entry.getKey();
						variable.value = entry.getValue();
						importContext.variables.add(variable);
					}
					instanceContext.imports.add(importContext);
				}
			}
		}

		return context;
	}


	/**
	 * Finds all the components and the facets extended by the given component.
	 * <p>
	 * The component itself is included in the returned set.
	 * </p>
	 *
	 * @param baseComponent the component whose ancestors must be returned.
	 * @return the names of all the ancestor components and facets of the given component.
	 */
	private static Set<String> findExtendedTypes( final Component baseComponent ) {

		final LinkedList<AbstractType> types = new LinkedList<AbstractType>();
		types.addAll(ComponentHelpers.findAllExtendedComponents(baseComponent));
		types.addAll(ComponentHelpers.findAllFacets(baseComponent));

		final Set<String> typeNames = new LinkedHashSet<String>();
		for (final AbstractType type : types)
			typeNames.add(type.getName());

		return typeNames;
	}
}
