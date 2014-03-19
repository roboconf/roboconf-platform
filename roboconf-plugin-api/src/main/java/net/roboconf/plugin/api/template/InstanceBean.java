/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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

package net.roboconf.plugin.api.template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.roboconf.core.model.runtime.Import;
import net.roboconf.core.model.runtime.Instance;

/**
 * Bean used to inject an data into a {@link Instance} template.
 *
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
			for(Import imprt : this.instance.getImports().get(prefix)) {
				importbeans.add(new ImportBean(imprt));
			}
			result.add(new ImportListBean(prefix, importbeans));
		}
		return result;
	}

	static class ImportListBean {
		String prefix;
		Collection<ImportBean> imports;

		public ImportListBean(String prefix, Collection<ImportBean> imports) {
			super();
			this.prefix = prefix;
			this.imports = imports;
		}
	}
}
