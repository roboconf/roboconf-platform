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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import net.roboconf.dm.rest.services.internal.RestApplication;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RestIndexer {

	public final List<RestOperationBean> restMethods = new ArrayList<> ();
	private final Logger logger = Logger.getLogger( getClass().getName());


	/**
	 * Constructor.
	 */
	public RestIndexer() {

		boolean duplicates = false;
		final Map<String,String> patternToResourceName = new HashMap<> ();
		for( Class<?> clazz : RestApplication.getResourceClasses()) {

			// Get the root path (set on the class itself)
			String basePath = "";
			if( clazz.isAnnotationPresent( Path.class )) {
				Path pathAnnotation = clazz.getAnnotation( Path.class );
				basePath = pathAnnotation.value();
			}

			// Other parts are declared in our super interfaces
			Class<?> superClass = clazz.getInterfaces()[ 0 ];
			for( Method m : superClass.getMethods()) {

				String restVerb = null;
				if( m.isAnnotationPresent( POST.class ))
					restVerb = "POST";
				else if( m.isAnnotationPresent( GET.class ))
					restVerb = "GET";
				else if( m.isAnnotationPresent( DELETE.class ))
					restVerb = "DELETE";
				else if( m.isAnnotationPresent( PUT.class ))
					restVerb = "PUT";

				if( restVerb == null )
					continue;

				//if( ! m.isAnnotationPresent( Auth.class ))
				//	continue;

				String subPath = "";
				if( m.isAnnotationPresent( Path.class )) {
					Path pathAnnotation = m.getAnnotation( Path.class );
					subPath = pathAnnotation.value();
				}

				RestOperationBean restOperationBean = new RestOperationBean();
				restOperationBean.methodName = m.getName();
				restOperationBean.restVerb = restVerb;
				restOperationBean.jerseyPath = basePath + subPath;
				restOperationBean.urlPattern = restOperationBean.jerseyPath.replaceAll( "\\{[^}]+\\}", "[^/]+" );
				this.restMethods.add( restOperationBean );

				// Duplicate URL patterns?
				String key = restVerb + "_" + restOperationBean.urlPattern;
				String assoc = patternToResourceName.get( key );
				if( assoc != null ) {
					duplicates = true;
					this.logger.severe( "Duplicate pattern: " + key + " = " + assoc );
				} else {
					patternToResourceName.put( key, clazz.getSimpleName() + "_" + m.getName());
				}
			}
		}

		// Throw an exception if duplicates were found, as our permission and audit
		// systems are based on this class.
		if( duplicates ) {
			// Inconsistencies are located in the code.
			// They should appear during tests and before we release.
			this.logger.severe( "REST methods contain inconsistencies. Audit and permissions checking are currently NOT reliable." );
			throw new RuntimeException( "Duplicates were found in URL patterns." );
		}
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static class RestOperationBean {
		private String methodName, urlPattern, jerseyPath, restVerb;

		/**
		 * @return the methodName
		 */
		public String getMethodName() {
			return this.methodName;
		}

		/**
		 * @return the urlPattern
		 */
		public String getUrlPattern() {
			return this.urlPattern;
		}

		/**
		 * @return the jerseyPath
		 */
		public String getJerseyPath() {
			return this.jerseyPath;
		}

		/**
		 * @return the restVerb
		 */
		public String getRestVerb() {
			return this.restVerb;
		}

		@Override
		public String toString() {
			return this.methodName;
		}
	}
}
