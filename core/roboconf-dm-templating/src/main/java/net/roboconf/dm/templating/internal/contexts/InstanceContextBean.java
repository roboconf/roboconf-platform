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

package net.roboconf.dm.templating.internal.contexts;

import java.util.LinkedHashSet;
import java.util.Set;

import net.roboconf.core.model.beans.Instance;

/**
 * Context bean for a Roboconf instance.
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class InstanceContextBean {

	String name;
	String path;
	Instance.InstanceStatus status;
	boolean statusIsStable;
	String component;
	String ip;
	String installer;

	final Set<String> types = new LinkedHashSet<String>();
	final Set<InstanceContextBean> children = new LinkedHashSet<InstanceContextBean>();

	InstanceContextBean parent;

	final Set<VariableContextBean> exports = new LinkedHashSet<VariableContextBean>();
	final Set<ImportContextBean> imports = new LinkedHashSet<ImportContextBean>();
	final Set<VariableContextBean> data = new LinkedHashSet<VariableContextBean>();


	public String getName() {
		return this.name;
	}

	public String getPath() {
		return this.path;
	}

	public Instance.InstanceStatus getStatus() {
		return this.status;
	}

	public boolean getStatusIsStable() {
		return this.statusIsStable;
	}

	public String getComponent() {
		return this.component;
	}

	public Set<String> getTypes() {
		return this.types;
	}

	public InstanceContextBean getParent() {
		return this.parent;
	}

	public Set<InstanceContextBean> getChildren() {
		return this.children;
	}

	public String getIp() {
		return this.ip;
	}

	public Set<VariableContextBean> getExports() {
		return this.exports;
	}

	public String getInstaller() {
		return this.installer;
	}

	public Set<ImportContextBean> getImports() {
		return this.imports;
	}

	public Set<VariableContextBean> getData() {
		return this.data;
	}

	@Override
	public String toString() {
		return this.name;
	}
}
