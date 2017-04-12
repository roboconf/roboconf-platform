/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.rest.services.internal.annotations;

import java.lang.reflect.Method;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.dm.rest.services.internal.RestApplication;
import net.roboconf.dm.rest.services.internal.annotations.RestIndexer.RestOperationBean;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RestIndexerTest {

	@Test
	public void testMapBuilding() {
		RestIndexer indexer = new RestIndexer();

		// Checking that all the REST methods have been registered
		int cpt = 0;
		for( Class<?> clazz : RestApplication.getResourceClasses()) {

			Class<?> superClass = clazz.getInterfaces()[ 0 ];
			for( Method m : superClass.getMethods()) {

				// Only consider REST annotated classes
				if( ! m.isAnnotationPresent( POST.class )
						&& ! m.isAnnotationPresent( GET.class )
						&& ! m.isAnnotationPresent( DELETE.class )
						&& ! m.isAnnotationPresent( PUT.class ))
					continue;

				cpt ++;
				boolean found = false;
				for( RestOperationBean ab : indexer.restMethods ) {
					if( m.getName().equals( ab.getMethodName())) {
						found = true;
						break;
					}
				}

				Assert.assertTrue( clazz.getSimpleName() +  " - " + m.getName(), found );
			}
		}

		// Verify the number of methods
		Assert.assertEquals( cpt, indexer.restMethods.size());

		// Verify toString()
		RestOperationBean ab = indexer.restMethods.get( 0 );
		Assert.assertEquals( ab.getMethodName(), ab.toString());
	}


	@Test
	public void testUrlPatterns() throws Exception {

		RestIndexer indexer = new RestIndexer();
		for( RestOperationBean ab : indexer.restMethods ) {
			Assert.assertTrue( ab.getJerseyPath().matches( ab.getUrlPattern()));
		}
	}
}
