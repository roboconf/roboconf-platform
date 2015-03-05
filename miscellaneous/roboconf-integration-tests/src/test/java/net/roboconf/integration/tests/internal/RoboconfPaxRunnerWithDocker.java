/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.integration.tests.internal;

import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;
import net.roboconf.target.docker.internal.DockerTestUtils;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.ops4j.pax.exam.junit.PaxExam;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfPaxRunnerWithDocker extends PaxExam {

	private final Class<?> testClass;


	/**
	 * Constructor.
	 * @param klass
	 * @throws InitializationError
	 */
	public RoboconfPaxRunnerWithDocker( Class<?> klass ) throws InitializationError {
		super( klass );
		this.testClass = klass;
	}


	@Override
	public void run( RunNotifier notifier ) {

		// We need RabbitMQ
		if( ! IntegrationTestsUtils.rabbitMqIsRunning()) {
			Description description = Description.createSuiteDescription( this.testClass );
			notifier.fireTestAssumptionFailed( new Failure( description, new Exception( "RabbitMQ is not running." )));

		} else {
			// We also need Docker
			boolean dockerIsHere = false;
			try {
				DockerTestUtils.checkDockerIsInstalled();
				dockerIsHere = true;

			} catch( Exception e ) {
				Logger logger = Logger.getLogger( getClass().getName());
				Utils.logException( logger, e );
			}

			// If it is here, run the test
			if( dockerIsHere ) {
				super.run( notifier );

			} else {
				Description description = Description.createSuiteDescription( this.testClass );
				notifier.fireTestAssumptionFailed( new Failure( description, new Exception( "Docker is not installed or not configured correctly." )));
			}
		}
	}
}
