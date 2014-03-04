/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.core.model.runtime;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * An Import is a list of var which is embedded into an ImportType.
 * <p>
 * It define a set of var which is exchanged on the network.
 * For example in the case of a loadbalancer and application servers, an Import would be the ip and port of a worker.
 * </p>
 *
 * TODO: review the javadoc, and maybe the structure
 * @author Noël - LIG
 */
public class Import implements Serializable {

	private static final long serialVersionUID = 1926254974053785327L;

	private final String instanceExportingVarsName;
	private Map<String, String>	 importedVars;

	public Import(String instanceExportingVarsName) {
		this.instanceExportingVarsName = instanceExportingVarsName;
		this.importedVars = new HashMap<String, String>();
	}

	public Import(String instanceExportingVarsName,
			Map<String, String> importedVars) {
		super();
		this.instanceExportingVarsName = instanceExportingVarsName;
		this.importedVars = importedVars;
	}

	// Getters
	public Set<String> getImportedVarsName() {
		return this.importedVars.keySet();
	}

	public Map<String, String> getImportedVars() {
		return this.importedVars;
	}

	public String getInstanceExportingVarsName() {
		return this.instanceExportingVarsName;
	}

	// Setters
	public void setImportedVarValue(String varName, String varValue) {
		this.importedVars.put(varName, varValue);
	}

	public void setImportedVars(Map<String, String> vars) {
		this.importedVars = vars;
	}

	// Methods
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (Entry<String, String> entry : this.importedVars.entrySet()) {
			sb.append(entry.getKey() + "=" + entry.getValue() + ",");
		}
		sb.append("]");
		return sb.toString();
	}


	// VZ: call super.clone()... Is it really useful?
//	@Override
//	public Import clone() {
//		Import clone = new Import(this.instanceExportingVarsName);
//		for (Entry<String, String> importedVarName : this.importedVars.entrySet()) {
//			clone.setImportedVarValue(importedVarName.getKey(), importedVarName.getValue());
//		}
//		return clone;
//	}

	/**
	 * An Import is considered empty if it contains no vars or if value of vars are null or equals empty string ("").
	 * @return true if empty, false if it contains at least one var that has a value
	 */
	public boolean isEmpty() {
		if (this.importedVars.isEmpty()) {
			return true;
		}
		for (Entry<String, String> entry : this.importedVars.entrySet()) {
			if (entry.getValue() == null) {
				continue;
			} else {
				if (entry.getValue().equals("")) {
					// Consider it empty
				} else {
					return false;
				}
			}
		}
		return true;
	}

}
