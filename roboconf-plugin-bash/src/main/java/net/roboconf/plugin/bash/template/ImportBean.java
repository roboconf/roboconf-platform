package net.roboconf.plugin.bash.template;

import java.util.ArrayList;
import java.util.List;

import net.roboconf.core.model.runtime.Import;

/**
 * Bean used to inject an data into a {@link Import} template
 * 
 * @author gcrosmarie
 *
 */
public class ImportBean {

	private Import imprt;

	public ImportBean(Import imprt) {
		this.imprt = imprt;
	}
	
	public List<Var> getExportedVars() {
		List<Var> result = new ArrayList<ImportBean.Var>();
		for(String name : imprt.getExportedVars().keySet()) {
			result.add(new Var(name, imprt.getExportedVars().get(name)));
		}
		return result;
	}
	
	static class Var {
		public Var(String name, String value) {
			super();
			this.name = name;
			this.value = value;
		}
		String name,value;
	}
}
