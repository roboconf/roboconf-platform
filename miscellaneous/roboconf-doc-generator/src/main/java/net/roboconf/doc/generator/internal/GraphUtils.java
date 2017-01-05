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

package net.roboconf.doc.generator.internal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.decorators.AbstractEdgeShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import net.roboconf.core.model.beans.AbstractType;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.utils.Utils;
import net.roboconf.doc.generator.DocConstants;

/**
 * @author Vincent Zurczak - Linagora
 */
public final class GraphUtils {

	public static final int SHAPE_HEIGHT = 80;


	/**
	 * Private empty constructor.
	 */
	private GraphUtils() {
		// nothing
	}


	/**
	 * Computes the width of a shape for a given component or facet.
	 * @param type a type
	 * @return the width it should take once displayed as a graph vertex
	 */
	public static int computeShapeWidth( AbstractType type ) {

		Font font = GraphUtils.getDefaultFont();
		BufferedImage img = new BufferedImage( 1, 1, BufferedImage.TYPE_INT_ARGB );
		FontMetrics fm = img.getGraphics().getFontMetrics( font );

		int width = fm.stringWidth( type.getName());
		width = Math.max( width, 80 ) + 20;
		return width;
	}


	/**
	 * @return the default font to use in graph diagrams
	 */
	public static Font getDefaultFont() {
		return new Font( "Helvetica Neue", Font.BOLD, 15 );
	}


	/**
	 * Writes a graph as a PNG image.
	 * @param outputFile the output file
	 * @param selectedComponent the component to highlight
	 * @param layout the layout
	 * @param graph the graph
	 * @param edgeShapeTransformer the transformer for edge shapes (straight line, curved line, etc)
	 * @throws IOException if something went wrong
	 */
	public static void writeGraph(
			File outputFile,
			Component selectedComponent,
			Layout<AbstractType,String> layout,
			Graph<AbstractType,String> graph ,
			AbstractEdgeShapeTransformer<AbstractType,String> edgeShapeTransformer,
			Map<String,String> options )
	throws IOException {

		VisualizationImageServer<AbstractType,String> vis =
				new VisualizationImageServer<AbstractType,String>( layout, layout.getSize());

		vis.setBackground( Color.WHITE );
		vis.getRenderContext().setEdgeLabelTransformer( new NoStringLabeller ());
		vis.getRenderContext().setEdgeShapeTransformer( edgeShapeTransformer );

		vis.getRenderContext().setVertexLabelTransformer( new ToStringLabeller<AbstractType> ());
		vis.getRenderContext().setVertexShapeTransformer( new VertexShape());
		vis.getRenderContext().setVertexFontTransformer( new VertexFont());

		Color defaultBgColor = decode( options.get( DocConstants.OPTION_IMG_BACKGROUND_COLOR ), DocConstants.DEFAULT_BACKGROUND_COLOR );
		Color highlightBgcolor = decode( options.get( DocConstants.OPTION_IMG_HIGHLIGHT_BG_COLOR ), DocConstants.DEFAULT_HIGHLIGHT_BG_COLOR );
		vis.getRenderContext().setVertexFillPaintTransformer( new VertexColor( selectedComponent, defaultBgColor, highlightBgcolor ));

		Color defaultFgColor = decode( options.get( DocConstants.OPTION_IMG_FOREGROUND_COLOR ), DocConstants.DEFAULT_FOREGROUND_COLOR );
		vis.getRenderContext().setVertexLabelRenderer( new MyVertexLabelRenderer( selectedComponent, defaultFgColor ));
		vis.getRenderer().getVertexLabelRenderer().setPosition( Position.CNTR );

		BufferedImage image = (BufferedImage) vis.getImage(
				new Point2D.Double(
						layout.getSize().getWidth() / 2,
						layout.getSize().getHeight() / 2),
				new Dimension( layout.getSize()));

		ImageIO.write( image, "png", outputFile );
	}


	/**
	 * Decodes an hexadecimal color and resolves it as an AWT color.
	 * @param value the value to decode
	 * @param defaultValue the default value to use if value is invalid
	 * @return a color (not null)
	 */
	static Color decode( String value, String defaultValue ) {

		Color result;
		try {
			result = Color.decode( value );

		} catch( NumberFormatException e ) {
			Logger logger = Logger.getLogger( GraphUtils.class.getName());
			logger.severe( "The specified color " + value + " could not be parsed. Back to default value: " + defaultValue );
			Utils.logException( logger, e );
			result = Color.decode( defaultValue );
		}

		return result;
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	static class VertexColor implements Transformer<AbstractType,Paint> {
		private final Component selectedComponent;
		private final Color defaultBgColor, highlightBgcolor;

		public VertexColor( Component selectedComponent, Color defaultBgColor, Color highlightBgcolor ) {
			this.selectedComponent = selectedComponent;
			this.defaultBgColor = defaultBgColor;
			this.highlightBgcolor = highlightBgcolor;
		}

		@Override
		public Paint transform( AbstractType type ) {
			return type.equals( this.selectedComponent ) ? this.highlightBgcolor : this.defaultBgColor;
		}
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	static class VertexFont implements Transformer<AbstractType,Font> {
		@Override
		public Font transform( AbstractType type ) {
			return GraphUtils.getDefaultFont();
		}
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	static class MyVertexLabelRenderer extends DefaultVertexLabelRenderer {
		private static final long serialVersionUID = -7669532897039301417L;
		private final Component selectedComponent;

		public MyVertexLabelRenderer( Component selectedComponent, Color defaultFgColor ) {
			super( defaultFgColor );
			this.selectedComponent = selectedComponent;
		}

		@Override
		public <T> java.awt.Component getVertexLabelRendererComponent(
				JComponent vv, Object value, Font font, boolean isSelected,
				T vertex ) {

			super.getVertexLabelRendererComponent( vv, value, font, isSelected, vertex );
			if( ! this.selectedComponent.equals( vertex ))
				super.setForeground( this.pickedVertexLabelColor );

			return this;
		}

	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	static class NoStringLabeller implements Transformer<String,String> {
		@Override
		public String transform( String v ) {
			return "";
		}
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	static class VertexShape implements Transformer<AbstractType,Shape> {
		@Override
		public Shape transform( AbstractType type ) {
			int width = computeShapeWidth( type );
			return new Ellipse2D.Double( -width / 2.0, -SHAPE_HEIGHT / 2.0, width, SHAPE_HEIGHT );
		}
	}
}
