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

package net.roboconf.doc.generator.internal.transformers;

import java.awt.Dimension;
import java.awt.geom.Point2D;

import net.roboconf.core.model.beans.AbstractType;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.decorators.AbstractEdgeShapeTransformer;

/**
 * A transformer to find vertex positions for Roboconf's relations.
 * <p>
 * It aims at being used with {@link StaticLayout}. But rather than creating your layout,
 * you should use {@link #getConfiguredLayout()} instead.
 * </p>
 * <p>
 * This is not just a transformer. Most of the layouts take a graph and a dimension
 * in arguments. And the layout tries to fit the graph in the given area. This transformer
 * works differently. It builds a graph and compute nodes positions on the fly. And only then,
 * it deduces the size of the graph for the layout.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractRoboconfTransformer implements Transformer<AbstractType,Point2D> {

	/**
	 * @return the graph dimension (based on the positions)
	 */
	public abstract Dimension getGraphDimension();


	/**
	 * @return the graph
	 */
	public abstract Graph<AbstractType,String> getGraph();


	/**
	 * @return a shape transformer for edges
	 */
	public abstract AbstractEdgeShapeTransformer<AbstractType,String> getEdgeShapeTransformer();


	/**
	 * @return a layout that use the computed positions
	 */
	public Layout<AbstractType,String> getConfiguredLayout() {
		return new StaticLayout<AbstractType,String>( getGraph(), this, getGraphDimension());
	}
}
