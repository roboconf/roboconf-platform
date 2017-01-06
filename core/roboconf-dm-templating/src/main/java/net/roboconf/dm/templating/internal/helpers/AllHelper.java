/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.templating.internal.helpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;
import net.roboconf.dm.templating.internal.contexts.ApplicationContextBean;
import net.roboconf.dm.templating.internal.contexts.InstanceContextBean;

import org.apache.commons.lang3.StringUtils;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

/**
 * Helper needed to select all the instances matching a given criterion.
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class AllHelper implements Helper<Object> {

	public static final String NAME = "all";
	private final Logger logger = Logger.getLogger( getClass().getName());


	/**
	 * Selects instances according to a selection path.
	 * <p>For example:</p>
	 * <pre>
	 * {{#all children path="/VM/Apache" installer="script"}}
	 *   {{path}}
	 * {{/all}}
	 * </pre>
	 */
	@Override
	public CharSequence apply( final Object computedContext, final Options options ) throws IOException {

		// Get parameters
		Object context = options.context.model();
		String componentPath = null;
		if( computedContext instanceof String )
			componentPath = (String) computedContext;

		if( Utils.isEmptyOrWhitespaces( componentPath ))
			componentPath = InstanceFilter.JOKER;

		// Process them
		CharSequence result = StringUtils.EMPTY;
		if (context instanceof ApplicationContextBean) {
			// Implicit: all instances of the application.
			result = safeApply(((ApplicationContextBean) context).getInstances(), options, componentPath );

		} else if (context instanceof InstanceContextBean) {
			// Implicit: all descendants of the instance.
			result = safeApply( descendantInstances((InstanceContextBean) context), options, componentPath );

		} else {
			this.logger.warning( "An unexpected context was received: " + (context == null ? null : context.getClass()));
		}

		return result;
	}


	/**
	 * Same as above, but with type-safe arguments.
	 *
	 * @param instances the instances to which this helper is applied.
	 * @param options   the options of this helper invocation.
	 * @return a string result.
	 * @throws IOException if a template cannot be loaded.
	 */
	private String safeApply( Collection<InstanceContextBean> instances, Options options, String componentPath )
	throws IOException {

		// Parse the filter.
		String installerName = (String) options.hash.get( "installer" );
		final InstanceFilter filter = InstanceFilter.createFilter( componentPath, installerName );

		// Apply the filter.
		final Collection<InstanceContextBean> selectedInstances = filter.apply( instances );

		// Apply the content template of the helper to each selected instance.
		final StringBuilder buffer = new StringBuilder();
		final Context parent = options.context;
		int index = 0;
		final int last = selectedInstances.size() - 1;

		for( final InstanceContextBean instance : selectedInstances ) {
			final Context current = Context.newBuilder( parent, instance )
					.combine( "@index", index )
					.combine( "@first", index == 0 ? "first" : "" )
					.combine( "@last", index == last ? "last" : "" )
					.combine( "@odd", index % 2 == 0 ? "" : "odd" )
					.combine( "@even", index % 2 == 0 ? "even" : "" )
					.build();

			index++;
			buffer.append( options.fn( current ));
		}

		return buffer.toString();
	}


	/**
	 * Returns all the descendant instances of the given instance.
	 * @param instance the instance which descendants must be retrieved.
	 * @return the descendants of the given instance.
	 */
	private static Collection<InstanceContextBean> descendantInstances( final InstanceContextBean instance ) {

		final Collection<InstanceContextBean> result = new ArrayList<InstanceContextBean>();
		for (final InstanceContextBean child : instance.getChildren()) {
			result.add( child );
			result.addAll( descendantInstances( child ) );
		}

		return result;
	}
}
