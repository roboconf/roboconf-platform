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

package net.roboconf.plugin.script.internal.templating;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.roboconf.core.model.beans.Import;
import net.roboconf.core.model.beans.Instance;

/**
 * Bean used to inject an data into a {@link Instance} template.
 * @author gcrosmarie - Linagora
 */
public class InstanceBean {

	private final Instance instance;

	public InstanceBean(Instance instance) {
		this.instance = instance;
	}

	public List<ImportListBean> getImportLists() {

		List<ImportListBean> result = new ArrayList<InstanceBean.ImportListBean>();
		for(String prefix : this.instance.getImports().keySet()) {

			List<ImportBean> importbeans = new ArrayList<ImportBean>();
			for(Import imprt : this.instance.getImports().get(prefix))
				importbeans.add(new ImportBean(imprt));

			result.add(new ImportListBean(prefix, importbeans));
		}

		return result;
	}


	/**
	 * @author gcrosmarie - Linagora
	 */
	static class ImportListBean {
		private final String prefix;
		private final Collection<ImportBean> imports;

		ImportListBean(String prefix, Collection<ImportBean> imports) {
			this.prefix = prefix;
			this.imports = imports;
		}

		public String getPrefix() {
			return this.prefix;
		}

		public Collection<ImportBean> getImports() {
			return this.imports;
		}
	}
}
