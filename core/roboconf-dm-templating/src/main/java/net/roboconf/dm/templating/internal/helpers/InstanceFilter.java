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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;
import net.roboconf.dm.templating.internal.contexts.InstanceContextBean;

/**
 * An instance filter based on a component path and/or an installer name.
 * <p>
 * This filter is used in the {@linkplain AllHelper all} Handlebars template helper.
 * </p>
 *
 * @author Pierre Bourret - Université Joseph Fourier
 */
public final class InstanceFilter {

	public static final String JOKER = "*";
	public static final String PATH_SEPARATOR = "/";
	public static final String ALTERNATIVE = "|";

	private final String path;
	private final Node rootNode;
	private final String installerName;


	/**
	 * Private constructor.
	 * @param path component path
	 * @param rootNode the root node
	 * @param installerName the installer name (can be null)
	 */
	private InstanceFilter( String path, Node rootNode, String installerName ) {
		this.path = path;
		this.rootNode = rootNode;
		this.installerName = installerName;
	}


	/**
	 * Factory for {@code Filter}s.
	 *
	 * @param path component path for the filter to be created (neither null, nor empty)
	 * @param installerName
	 * @return the created filter
	 * @throws IllegalArgumentException if {@code path} is an illegal component path
	 */
	public static InstanceFilter createFilter( final String path, String installerName ) {

		List<String> types = Utils.splitNicely( path, ALTERNATIVE );
		OrNode rootNode = new OrNode();
		for( String type : types ) {
			Node node = buildNodeForPath( type );
			rootNode.delegates.add( node );
		}

		return new InstanceFilter( path, rootNode, installerName );
	}


	/**
	 * Applies this filter to the given instances.
	 *
	 * @param instances the collection of instances to which this filter has to be applied.
	 * @return the instances of the provided collection that matches this filter.
	 */
	public Collection<InstanceContextBean> apply( Collection<InstanceContextBean> instances ) {

		final ArrayList<InstanceContextBean> result = new ArrayList<InstanceContextBean> ();
		for( InstanceContextBean instance : instances ) {
			boolean installerMatches = this.installerName == null || this.installerName.equalsIgnoreCase( instance.getInstaller());
			if( installerMatches && this.rootNode.isMatching( instance ))
				result.add( instance );
		}

		result.trimToSize();
		return Collections.unmodifiableCollection(result);
	}


	/**
	 * Gets the path string of this filter.
	 * @return the path string of this filter.
	 */
	public String getPath() {
		return this.path;
	}


	/**
	 * Builds a node for a single type (meaning no '|' in the path).
	 * @param path a non-null type
	 * @return a non-null node
	 */
	private static Node buildNodeForPath( String path ) {

		// The path cannot be empty, guaranteed by AllHelper
		final String[] elements = path.split(PATH_SEPARATOR, -1);
		final int last = elements.length - 1;

		// Iterate in reverse order, as the instances we want are those on the right side of the path.
		AndNode rootNode = null;
		AndNode parentNode = null;
		for (int i = last; i >= 0; i--) {
			final String element = elements[i];

			// Sanity checks
			if( element.isEmpty()) {
				Logger logger = Logger.getLogger( InstanceFilter.class.getName());
				logger.warning( "An invalid component path was found in templates. Wrong part is: '" + path + "'." );

				rootNode = null;
				break;
			}

			// Node for the current element.
			final AndNode currentNode = new AndNode();

			// Type name filter
			if (!JOKER.equals(element))
				currentNode.delegates.add(new TypeNode(element));

			// Special handling if the path begins with a leading '/'.
			// It means that the instance must be a root instance.
			// In this case the first element is empty "", despite the component path remains valid.
			// The following element (at index 1) must match root instances only.
			if (i == 1 && elements[0].isEmpty()) {
				currentNode.delegates.add(new RootInstanceNode());
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
				parentNode.delegates.add(new ParentInstanceNode(currentNode));
			}

			// Shift and reiterate.
			parentNode = currentNode;
		}

		return rootNode != null ? rootNode : new ErrorNode();
	}


	/**
	 * Basic element of an instance filter.
	 */
	private static abstract class Node {

		/**
		 * Tests if an instance matches this instance filter node.
		 *
		 * @param instance the instance to test.
		 * @return {@code true} if and only if the given instance matches this instance filter node.
		 */
		abstract boolean isMatching( InstanceContextBean instance );
	}



	/**
	 * A node indicating an error was encountered while processing this path.
	 */
	private static class ErrorNode extends Node {

		@Override
		boolean isMatching( InstanceContextBean instance ) {
			return false;
		}
	}


	/**
	 * And-combination of several nodes.
	 * <p>
	 * As a corner-case property, an {@code AndNode} without delegates matches any instance. It is used to handle the
	 * special {@value #JOKER} in a path element.
	 * </p>
	 */
	private static class AndNode extends Node {
		final LinkedList<Node> delegates = new LinkedList<Node>();

		@Override
		boolean isMatching( final InstanceContextBean instance ) {

			boolean matching = true;
			for( Iterator<Node> it = this.delegates.iterator(); it.hasNext() && matching; )
				matching = it.next().isMatching( instance );

			return matching;
		}
	}


	/**
	 * Or-combination of several nodes.
	 * <p>
	 * As a corner-case property, an {@code OrNode} is used to handle the
	 * special {@value #ALTERNATIVE} in a path element.
	 * </p>
	 */
	private static class OrNode extends Node {
		final LinkedList<Node> delegates = new LinkedList<Node>();

		@Override
		boolean isMatching( final InstanceContextBean instance ) {

			boolean matching = false;
			for( Iterator<Node> it = this.delegates.iterator(); it.hasNext() && ! matching; )
				matching = it.next().isMatching( instance );

			return matching;
		}
	}


	/**
	 * A node that only matches instances of a given type.
	 */
	private static class TypeNode extends Node {
		final String typeName;

		/**
		 * Constructor.
		 * @param typeName the type name
		 */
		TypeNode( String typeName ) {
			this.typeName = typeName;
		}

		@Override
		boolean isMatching( InstanceContextBean instance ) {

			// Find the exact reference.
			boolean matching = instance.getTypes().contains( this.typeName );

			// Or maybe it is a pattern...
			if( ! matching && this.typeName.contains( JOKER )) {
				String pattern = this.typeName.replace( JOKER, ".*" );
				for( String type : instance.getTypes()) {

					if( type.matches( pattern )) {
						matching = true;
						break;
					}
				}
			}

			return matching;
		}
	}


	/**
	 * A node that matches instances based on their parent.
	 */
	private static class ParentInstanceNode extends Node {
		final Node parentInstanceNode;

		/**
		 * Constructor.
		 * @param parentInstanceNode the node that test the parent instances.
		 */
		ParentInstanceNode( final Node parentInstanceNode ) {
			this.parentInstanceNode = parentInstanceNode;
		}

		@Override
		boolean isMatching( final InstanceContextBean instance ) {
			return instance.getParent() != null
					&& this.parentInstanceNode.isMatching(instance.getParent());
		}
	}


	/**
	 * A node that only matches root instances.
	 */
	private static class RootInstanceNode extends Node {
		@Override
		boolean isMatching( final InstanceContextBean instance ) {
			return instance.getParent() == null;
		}
	}
}
