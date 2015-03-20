/**
 * Copyright 2013-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.monitoring.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * An instance filter based on a component path and/or an installer name.
 * 
 * <p>
 * This filter is used in the {@linkplain AllHelper all} Handlebars template helper.
 * </p>
 *
 * @author Pierre Bourret - Université Joseph Fourier
 */
public final class InstanceFilter {

	/**
	 * The joker character, matching any component.
	 */
	public static final String JOKER = "*";

	/**
	 * The separator for instance filters.
	 */
	public static final String PATH_SEPARATOR = "/";

	/**
	 * The path of this filter, exactly as requested at creation time.
	 */
	private final String path;

	/**
	 * The root node of this filter.
	 */
	private final Node rootNode;

	/**
	 * Private constructor.
	 *
	 * @param path     component path.
	 * @param rootNode the root node.
	 */
	private InstanceFilter( String path, Node rootNode ) {
		this.path = path;
		this.rootNode = rootNode;
	}

	/**
	 * Factory for {@code Filter}s.
	 *
	 * @param path component path for the filter to be created.
	 * @return the created filter.
	 * @throws IllegalArgumentException if {@code path} is an illegal component path.
	 */
	public static InstanceFilter createFilter( final String path ) {
		// Split the path.
		final String[] elements = path.split( PATH_SEPARATOR, -1 );
		final int last = elements.length - 1;
		if (last == -1) {
			throw new IllegalArgumentException( "Empty component path" );
		}

		// Iterate in reverse order, as the instances we want are those on the right side of the path.
		AndNode rootNode = null;
		AndNode parentNode = null;
		for (int i = last; i >= 0; i--) {
			final String element = elements[i];

			// Sanity checks
			if (element.isEmpty() || element.contains( JOKER ) && element.length() != JOKER.length()) {
				throw new IllegalArgumentException( "Empty component path: " + path );
			}

			// Node for the current element.
			final AndNode currentNode = new AndNode();

			// Type name filter
			if (!JOKER.equals( element )) {
				currentNode.delegates.add( new TypeNode( element ) );
			}

			// Special handling if the path begins with a leading '/'.
			// It means that the instance must be a root instance.
			// In this case the first element is empty "", despite the component path remains valid.
			// The following element (at index 1) must match root instances only.
			if (i == 1 && elements[0].isEmpty()) {
				currentNode.delegates.add( new RootInstanceNode() );
				// The next element is an empty string.
				// If we don't shortcut it, it will make the next iteration fail.
				// Skip it by forcing the index to its ending bound.
				i = 0;
			}

			if (rootNode == null) {
				// The current node  is the root node. We have no parent node yet.
				rootNode = currentNode;
			} else {
				// Chain the current node with its parent node.
				parentNode.delegates.add( new ParentInstanceNode( currentNode ) );
			}

			// Shift and reiterate.
			parentNode = currentNode;
		}

		return new InstanceFilter( path, rootNode );
	}

	/**
	 * Apply this filter to the given instances.
	 *
	 * @param instances the collection of instances to which this filter has to be applied.
	 * @return the instances of the provided collection that matches this filter.
	 */
	public Collection<InstanceContextBean> apply( Collection<InstanceContextBean> instances ) {
		final ArrayList<InstanceContextBean> result = new ArrayList<InstanceContextBean>();
		for (InstanceContextBean instance : instances) {
			if (rootNode.isMatching( instance )) {
				result.add( instance );
			}
		}
		result.trimToSize();
		return Collections.unmodifiableCollection(result);
	}

	/**
	 * Get the path string of this filter.
	 * 
	 * @return the path string of this filter.
	 */
	public String getPath() {
		return this.path;
	}

	@Override
	public String toString() {
		return super.toString() + "[path=" + this.path + ']';
	}

	/**
	 * Basic element of an instance filter.
	 */
	private static abstract class Node {
		/**
		 * Test if an instance matches this instance filter node.
		 *
		 * @param instance the instance to test.
		 * @return {@code true} if and only if the given instance matches this instance filter node.
		 */
		abstract boolean isMatching( InstanceContextBean instance );
	}

	/**
	 * And-combination of several nodes.
	 * <p>
	 * As a corner-case property, an {@code AndNode} without delegates matches any instance. It is used to handle the
	 * special {@value #JOKER} path element.
	 * </p>
	 */
	private static class AndNode extends Node {
		/**
		 * The delegate nodes.
		 */
		final LinkedList<Node> delegates = new LinkedList<Node>();

		@Override
		boolean isMatching( final InstanceContextBean instance ) {
			boolean result = true;
			for (Node n : this.delegates) {
				if (!n.isMatching( instance )) {
					result = false;
					break;
				}
			}
			return result;
		}
	}

	/**
	 * A node that only matches instances of a given type.
	 */
	private static class TypeNode extends Node {
		/**
		 * The name of the matching component.
		 */
		final String typeName;

		/**
		 * Create a {@code TypeNode} that matches instances of the type with the given name.
		 *
		 * @param typeName the name of the type to match.
		 */
		TypeNode( final String typeName ) {
			this.typeName = typeName;
		}

		@Override
		boolean isMatching( final InstanceContextBean instance ) {
			return instance.types.contains( typeName );
		}
	}

	/**
	 * A node that matches instances based on their parent.
	 */
	private static class ParentInstanceNode extends Node {
		/**
		 * The node that test the parent instances.
		 */
		final Node parentInstanceNode;

		/**
		 * Create a {@code ParentInstanceNode} that matches instances whose parent matches the given parent instance
		 * node.
		 *
		 * @param parentInstanceNode the node that test the parent instances.
		 */
		ParentInstanceNode( final Node parentInstanceNode ) {
			this.parentInstanceNode = parentInstanceNode;
		}

		@Override
		boolean isMatching( final InstanceContextBean instance ) {
			return instance.parent != null && parentInstanceNode.isMatching( instance.parent );
		}

	}

	/**
	 * A node that only matches root instances.
	 */
	private static class RootInstanceNode extends Node {
		@Override
		boolean isMatching( final InstanceContextBean instance ) {
			return instance.parent == null;
		}
	}

}
