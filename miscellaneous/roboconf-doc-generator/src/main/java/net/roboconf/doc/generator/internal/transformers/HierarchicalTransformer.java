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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.roboconf.core.model.beans.AbstractType;
import net.roboconf.core.model.beans.Component;
import net.roboconf.doc.generator.internal.GraphUtils;
import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.decorators.AbstractEdgeShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;

/**
 * A transformer to find vertex positions for Roboconf's hierarchical relations.
 * @author Vincent Zurczak - Linagora
 */
public class HierarchicalTransformer extends AbstractRoboconfTransformer {

	private static final int MIN_H_MARGIN = 100;
	private static final int H_PADDING = 50;
	private static final int V_PADDING = 2 * GraphUtils.SHAPE_HEIGHT;
	private static final int V_MARGIN = 50;

	private final Component component;
	private final Map<AbstractType,Point2D> typeToLocation;
	private final int maxPerLine;
	private final List<AbstractType> aloneOnRow;

	private final Graph<AbstractType,String> graph;
	private int currentWidth = MIN_H_MARGIN;
	private int currentHeigth = V_MARGIN;
	private int maxRowWidth, hMargin;


	/**
	 * Constructor.
	 * @param component the component whose hierarchy must be displayed
	 * @param ancestors its ancestors
	 * @param children its children
	 * @param maxPerLine the maximum number of vertices per line
	 */
	public HierarchicalTransformer(
			Component component,
			Collection<AbstractType> ancestors,
			Collection<AbstractType> children,
			int maxPerLine ) {

		// Store fields
		this.component = component;
		this.maxPerLine = maxPerLine;
		this.typeToLocation = new HashMap<AbstractType,Point2D> ();
		this.aloneOnRow = new ArrayList<AbstractType> ();

		// Compute the effective horizontal margin for this graph
		this.hMargin = computeHMargin( component );
		for( AbstractType t : ancestors )
			this.hMargin = Math.max( this.hMargin, computeHMargin( t ));

		for( AbstractType t : children )
			this.hMargin = Math.max( this.hMargin, computeHMargin( t ));

		// Builds the graph
		this.graph = new DirectedOrderedSparseMultigraph<AbstractType,String> ();
		int cpt = 1;

		for( AbstractType t : ancestors )
			this.graph.addVertex( t );

		this.graph.addVertex( component );
		for( AbstractType t : ancestors )
			this.graph.addEdge( "can contain" + cpt++, t, component );

		for( AbstractType t : children ) {
			this.graph.addVertex( t );
			this.graph.addEdge( "can contain" + cpt++, component, t );
		}

		// In these first steps, vertices are aligned on the left
		if( ! ancestors.isEmpty()) {
			dealWithOthers( ancestors );
			this.currentHeigth += V_PADDING;
		}

		dealWithMainComponent();

		if( ! children.isEmpty()) {
			this.currentHeigth += V_PADDING;
			dealWithOthers( children );
		}

		this.currentHeigth += V_MARGIN;

		// Center alone vertices
		for( AbstractType t : this.aloneOnRow ) {
			int width = GraphUtils.computeShapeWidth( t );
			int newX = (this.maxRowWidth - width) / 2;

			if( newX > this.hMargin ) {
				Point2D p = transform( t );
				p.setLocation( newX, p.getY());
			}
		}
	}


	/**
	 * Finds the position for the main component.
	 */
	private void dealWithMainComponent() {

		this.typeToLocation.put(
				this.component,
				new Point2D.Double( this.hMargin, this.currentHeigth ));

		int width = H_PADDING + GraphUtils.computeShapeWidth( this.component );
		this.maxRowWidth = Math.max( this.maxRowWidth, width );
		this.aloneOnRow.add( this.component );
	}


	/**
	 * Finds the position of other components.
	 * @param others a non-null list of components
	 */
	private void dealWithOthers( Collection<AbstractType> others ) {

		int col = 1;
		this.currentWidth = this.hMargin;
		for( AbstractType t : others ) {

			if( col > this.maxPerLine ) {
				col = 1;
				this.currentHeigth += V_PADDING + GraphUtils.SHAPE_HEIGHT;
				this.currentWidth = this.hMargin;
			}

			this.typeToLocation.put( t, new Point2D.Double( this.currentWidth, this.currentHeigth ));
			this.currentWidth += H_PADDING + GraphUtils.computeShapeWidth( t );
			col ++;

			this.maxRowWidth = Math.max( this.maxRowWidth, this.currentWidth );
		}

		if( others.size() == 1 )
			this.aloneOnRow.add( others.iterator().next());
	}


	/*
	 * (non-Javadoc)
	 * @see org.apache.commons.collections15.Transformer
	 * #transform(java.lang.Object)
	 */
	@Override
	public Point2D transform( AbstractType input ) {
		return this.typeToLocation.get( input );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.transformers.AbstractRoboconfTransformer
	 * #getGraphDimension()
	 */
	@Override
	public Dimension getGraphDimension() {
		return new Dimension( this.maxRowWidth, this.currentHeigth );
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
	 * #getEdgeShapeTransformer()
	 */
	@Override
	public AbstractEdgeShapeTransformer<AbstractType,String> getEdgeShapeTransformer() {
		return new EdgeShape.Line<AbstractType,String> ();
	}


	/**
	 * Computes an horizontal margin for a given node.
	 * <p>
	 * We used to have a static horizontal margin, but images were
	 * truncated for long names. See roboconf-platform#315
	 * </p>
	 *
	 * @param input a node (can be null)
	 * @return a positive integer
	 */
	private static int computeHMargin( AbstractType input ) {

		int basis = MIN_H_MARGIN;
		if( input != null
				&& input.getName().length() > 17 ) {
			// Beyond 17 characters, we give 3 pixels for every new characters.
			basis += 3 * (input.getName().length() - 17);
		}

		return basis;
	}
}
