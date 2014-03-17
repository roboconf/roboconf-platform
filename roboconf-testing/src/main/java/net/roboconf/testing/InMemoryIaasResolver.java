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

package net.roboconf.testing;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.roboconf.core.model.runtime.Instance;
import net.roboconf.dm.environment.iaas.IaasResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.iaas.api.IaasInterface;
import net.roboconf.iaas.api.exceptions.IaasException;
import net.roboconf.plugin.api.ExecutionLevel;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InMemoryIaasResolver extends IaasResolver {

	private ExecutionLevel executionLevel;
	private File dumpDirectory;
	private final Map<String,InMemoryIaaS> appNameToIaasResolver = new ConcurrentHashMap<String,InMemoryIaaS> ();


	@Override
	public IaasInterface findIaasInterface( ManagedApplication ma, Instance instance )
	throws IaasException {

		InMemoryIaaS result = this.appNameToIaasResolver.get( ma.getApplication().getName());
		if( result == null ) {
			result = new InMemoryIaaS();
			this.appNameToIaasResolver.put( ma.getApplication().getName(), result );
		}

		result.setDumpDirectory( this.dumpDirectory );
		result.setExecutionLevel( this.executionLevel );

		return result;
	}


	/**
	 * @return the executionLevel
	 */
	public ExecutionLevel getExecutionLevel() {
		return this.executionLevel;
	}


	/**
	 * @param executionLevel the executionLevel to set
	 */
	public void setExecutionLevel( ExecutionLevel executionLevel ) {
		this.executionLevel = executionLevel;
	}


	/**
	 * @return the dumpDirectory
	 */
	public File getDumpDirectory() {
		return this.dumpDirectory;
	}


	/**
	 * @param dumpDirectory the dumpDirectory to set
	 */
	public void setDumpDirectory( File dumpDirectory ) {
		this.dumpDirectory = dumpDirectory;
	}
}
