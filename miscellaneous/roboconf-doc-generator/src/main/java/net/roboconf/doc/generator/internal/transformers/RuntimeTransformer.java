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
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Component;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.decorators.AbstractEdgeShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RuntimeTransformer extends AbstractRoboconfTransformer {

	private Graph<AbstractType,String> graph;
	private final Component component;


	/**
	 * Constructor.
	 * @param component the component whose hierarchy must be displayed
	 */
	public RuntimeTransformer( Component component, Application app ) {

		// Store fields
		this.component = component;

	}


	@Override
	public Point2D transform( AbstractType input ) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.transformers.AbstractRoboconfTransformer
	 * #getGraphDimension()
	 */
	@Override
	public Dimension getGraphDimension() {
		return new Dimension( 50, 50 );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.transformers.AbstractRoboconfTransformer
	 * #getGraph()
	 */
	@Override
	public Graph<AbstractType,String> getGraph() {
		return this.graph;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.transformers.AbstractRoboconfTransformer
	 * #getConfiguredLayout()
	 */
	@Override
	public Layout<AbstractType,String> getConfiguredLayout() {
		return new StaticLayout<AbstractType,String>( this.graph, this, getGraphDimension());
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.transformers.AbstractRoboconfTransformer
	 * #getEdgeShapeTransformer()
	 */
	@Override
	public AbstractEdgeShapeTransformer<AbstractType,String> getEdgeShapeTransformer() {
		return new EdgeShape.Loop<AbstractType,String> ();
	}
}
