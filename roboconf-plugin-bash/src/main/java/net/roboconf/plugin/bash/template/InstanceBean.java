package net.roboconf.plugin.bash.template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.roboconf.core.model.runtime.Import;
import net.roboconf.core.model.runtime.Instance;

/**
 * Bean used to inject an data into a {@link Instance} template
 * 
 * @author gcrosmarie
 *
 */
public class InstanceBean {

	private Instance instance;

	public InstanceBean(Instance instance) {
		this.instance = instance;
	}
	
	public List<ImportListBean> getImportLists() {
		List<ImportListBean> result = new ArrayList<InstanceBean.ImportListBean>();
		for(String prefix : instance.getImports().keySet()) {
			List<ImportBean> importbeans = new ArrayList<ImportBean>();
			for(Import imprt : instance.getImports().get(prefix)) {
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
