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

package net.roboconf.integration.tests.commons.internal.runners;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.ops4j.pax.exam.junit.PaxExam;

import com.github.dockerjava.api.DockerClient;

import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.rabbitmq.internal.utils.RabbitMqTestUtils;
import net.roboconf.target.docker.internal.DockerUtils;
import net.roboconf.target.docker.internal.test.DockerTestUtils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfPaxRunner extends PaxExam {

	public static final String RBCF_USER = "roboconf";
	private static final String IMG_NAME = "roboconf-it-test";

	private final Class<?> testClass;


	/**
	 * Constructor.
	 * @param klass
	 * @throws InitializationError
	 */
	public RoboconfPaxRunner( Class<?> klass ) throws InitializationError {
		super( klass );
		this.testClass = klass;
	}


	@Override
	public void run( RunNotifier notifier ) {

		boolean runTheTest = true;
		if( this.testClass.isAnnotationPresent( RoboconfITConfiguration.class )) {
			RoboconfITConfiguration annotation = this.testClass.getAnnotation( RoboconfITConfiguration.class );

			// Default RMQ settings
			if( annotation.withRabbitMq()
					&& ! RabbitMqTestUtils.checkRabbitMqIsRunning()) {
				Description description = Description.createSuiteDescription( this.testClass );
				notifier.fireTestAssumptionFailed( new Failure( description, new Exception( "RabbitMQ is not running." )));
				runTheTest = false;
			}

			// Advanced RMQ settings
			else if( annotation.withRabbitMq()
					&& ! RabbitMqTestUtils.checkRabbitMqIsRunning( "127.0.0.1", RBCF_USER, RBCF_USER )) {
				Description description = Description.createSuiteDescription( this.testClass );
				notifier.fireTestAssumptionFailed( new Failure( description, new Exception( "RabbitMQ is not running with the '" + RBCF_USER + "' user." )));
				runTheTest = false;
			}

			// Linux
			else if( annotation.withLinux()
					&& ! new File( "/tmp" ).exists()) {

				Description description = Description.createSuiteDescription( this.testClass );
				notifier.fireTestAssumptionFailed( new Failure( description, new Exception( "The test can only run on a Linux system." )));
				runTheTest = false;
			}

			// Docker
			else {
				boolean dockerIsHere = false;
				try {
					DockerTestUtils.checkDockerIsInstalled();
					dockerIsHere = true;

					Map<String,String> targetProperties = new HashMap<> ();
					targetProperties.put( "docker.endpoint", "http://localhost:" + DockerTestUtils.DOCKER_TCP_PORT );
					targetProperties.put( "docker.image", IMG_NAME );

					DockerClient client = DockerUtils.createDockerClient( targetProperties );
					DockerUtils.deleteImageIfItExists( IMG_NAME, client );

				} catch( Exception e ) {
					Logger logger = Logger.getLogger( getClass().getName());
					Utils.logException( logger, e );
				}

				// If it is here, run the test
				if( ! dockerIsHere ) {
					Description description = Description.createSuiteDescription( this.testClass );
					notifier.fireTestAssumptionFailed( new Failure( description, new Exception( "Docker is not installed or not configured correctly." )));
					runTheTest = false;
				}
			}
		}

		// Otherwise, we consider RMQ must be installed by default
		else if( ! RabbitMqTestUtils.checkRabbitMqIsRunning()) {
			Description description = Description.createSuiteDescription( this.testClass );
			notifier.fireTestAssumptionFailed( new Failure( description, new Exception( "RabbitMQ is not running." )));
			runTheTest = false;
		}

		// If everything is good, run the test
		if( runTheTest )
			super.run( notifier );
	}
}
