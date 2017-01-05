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

package net.roboconf.target.api.internal;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import net.roboconf.core.model.beans.Instance;
import net.roboconf.target.api.AbstractThreadedTargetHandler.MachineConfigurator;
import net.roboconf.target.api.TargetException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TestMachineConfigurator implements MachineConfigurator {

	private final AtomicInteger cpt;
	private final boolean failConfiguration;
	private final Instance scopedInstance;


	/**
	 * Constructor.
	 * @param scopedInstance
	 */
	public TestMachineConfigurator( AtomicInteger cpt, boolean failConfiguration, Instance scopedInstance  ) {
		this.cpt = cpt;
		this.scopedInstance = scopedInstance;
		this.failConfiguration = failConfiguration;
	}

	@Override
	public boolean configure() throws TargetException {

		if( this.failConfiguration
				&& this.cpt.get() == 1 )
			throw new TargetException( "This is for test purpose." );

		// We consider it is configured after 3 invocations.
		return this.cpt.incrementAndGet() == 3;
	}

	@Override
	public Instance getScopedInstance() {
		return this.scopedInstance;
	}

	@Override
	public void close() throws IOException {
		// nothing
	}
}
