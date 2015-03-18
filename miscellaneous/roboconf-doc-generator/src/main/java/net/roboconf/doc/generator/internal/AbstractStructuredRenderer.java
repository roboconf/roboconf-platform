/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.comparators.AbstractTypeComparator;
import net.roboconf.core.model.comparators.InstanceComparator;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.helpers.VariableHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.doc.generator.DocConstants;
import net.roboconf.doc.generator.internal.nls.Messages;
import net.roboconf.doc.generator.internal.transformers.AbstractRoboconfTransformer;
import net.roboconf.doc.generator.internal.transformers.HierarchicalTransformer;
import net.roboconf.doc.generator.internal.transformers.InheritanceTransformer;

/**
 * @author Vincent Zurczak - Linagora
 */
public abstract class AbstractStructuredRenderer implements IRenderer {

	protected File outputDirectory;
	protected Application application;
	protected File applicationDirectory;
	protected Map<String,String> options;
	protected Messages messages;


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static enum DiagramType {
		RUNTIME, HIERARCHY, INHERITANCE;
	}


	/**
	 * Constructor.
	 * @param outputDirectory
	 * @param application
	 * @param applicationDirectory
	 */
	public AbstractStructuredRenderer( File outputDirectory, Application application, File applicationDirectory ) {
		this.outputDirectory = outputDirectory;
		this.application = application;
		this.applicationDirectory = applicationDirectory;
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.IRenderer
	 * #render(java.util.Map)
	 */
	@Override
	public void render( Map<String,String> options ) throws IOException {

		this.options = options;
		StringBuilder sb = new StringBuilder();

		// Check the language
		String locale = options.get( DocConstants.OPTION_LOCALE );
		if( locale != null )
			this.messages = new Messages( locale );
		else
			this.messages = new Messages();

		// First pages
		sb.append( renderDocumentTitle());
		sb.append( renderPageBreak());

		sb.append( renderParagraph( this.messages.get( "intro" ))); //$NON-NLS-1$
		sb.append( renderPageBreak());

		sb.append( renderDocumentIndex());
		sb.append( renderPageBreak());

		sb.append( startTable());
		sb.append( addTableLine( this.messages.get( "app.name" ), this.application.getName())); //$NON-NLS-1$
		sb.append( addTableLine( this.messages.get( "app.ns" ), this.application.getNamespace())); //$NON-NLS-1$
		sb.append( addTableLine( this.messages.get( "app.qualifier" ), this.application.getQualifier())); //$NON-NLS-1$
		sb.append( endTable());

		sb.append( renderParagraph( this.application.getDescription()));
		sb.append( renderPageBreak());
		sb.append( renderSections( new ArrayList<String>( 0 )));

		// Render information about components
		sb.append( renderComponents());

		// Render information about initial instances
		sb.append( renderInstances());

		writeFileContent( sb.toString());
	}


	protected abstract String renderTitle1( String title );
	protected abstract String renderTitle2( String title );
	protected abstract String renderTitle3( String title );
	protected abstract String renderParagraph( String paragraph );
	protected abstract String renderList( Collection<String> listItems );
	protected abstract String renderPageBreak();
	protected abstract String indent();

	protected abstract String startTable();
	protected abstract String endTable();
	protected abstract String addTableHeader( String... headerEntries );
	protected abstract String addTableLine( String... lineEntries );

	protected abstract String renderDocumentTitle();
	protected abstract String renderDocumentIndex();

	protected abstract String renderImage( String componentName, DiagramType type, String relativeImagePath );

	protected abstract String applyBoldStyle( String text, String keyword );
	protected abstract String applyLink( String text, String linkId );

	protected abstract File writeFileContent( String fileContent ) throws IOException;
	protected abstract StringBuilder startSection( String sectionName );
	protected abstract StringBuilder endSection( String sectionName, StringBuilder sb );
	protected abstract String renderSections( List<String> sectionNames );


	/**
	 * Renders information about the components.
	 * @param application the application
	 * @param applicationDirectory the application's directory
	 * @return a string builder (never null)
	 * @throws IOException
	 */
	private StringBuilder renderComponents() throws IOException {

		StringBuilder sb = new StringBuilder();
		sb.append( renderTitle1( this.messages.get( "components" ))); //$NON-NLS-1$
		sb.append( renderParagraph( this.messages.get( "components.intro" ))); //$NON-NLS-1$

		List<String> sectionNames = new ArrayList<String> ();
		List<Component> allComponents = ComponentHelpers.findAllComponents( this.application );
		Collections.sort( allComponents, new AbstractTypeComparator());
		for( Component comp : allComponents ) {

			// Start a new section
			final String sectionName = DocConstants.SECTION_COMPONENTS + comp.getName();
			StringBuilder section = startSection( sectionName );

			// Overview
			section.append( renderTitle2( comp.getName()));
			section.append( renderTitle3( this.messages.get( "overview" ))); //$NON-NLS-1$

			String customInfo = readCustomInformation( this.applicationDirectory, comp.getName(), DocConstants.COMP_SUMMARY );
			if( ! Utils.isEmptyOrWhitespaces( customInfo ))
				section.append( renderParagraph( customInfo ));

			String installerName = Utils.capitalize( ComponentHelpers.findComponentInstaller( comp ));
			installerName = applyBoldStyle( installerName, installerName );
			String msg = MessageFormat.format( this.messages.get( "component.installer" ), installerName ); //$NON-NLS-1$
			section.append( renderParagraph( msg ));

			// Facets
			Collection<Facet> facets = ComponentHelpers.findAllFacets( comp );
			if( ! facets.isEmpty()) {
				section.append( renderTitle3( this.messages.get( "facets" ))); //$NON-NLS-1$
				msg = MessageFormat.format(this.messages.get( "component.inherits.facets" ), comp );

				section.append( renderParagraph( msg ));
				section.append( renderList( ComponentHelpers.extractNames( facets )));
			}

			// Inheritance
			List<Component> extendedComponents = ComponentHelpers.findAllExtendedComponents( comp );
			extendedComponents.remove( comp );
			Collection<Component> extendingComponents = ComponentHelpers.findAllExtendingComponents( comp );
			if( ! extendedComponents.isEmpty()
					|| ! extendingComponents.isEmpty()) {

				section.append( renderTitle3( this.messages.get( "inheritance" ))); //$NON-NLS-1$
				AbstractRoboconfTransformer transformer = new InheritanceTransformer( comp, comp.getExtendedComponent(), extendingComponents, 4 );
				saveImage( comp, DiagramType.INHERITANCE, transformer, section );
			}

			if( ! extendedComponents.isEmpty()) {
				msg = MessageFormat.format( this.messages.get( "component.inherits.properties" ), comp ); //$NON-NLS-1$
				section.append( renderParagraph( msg ));
				section.append( renderListAsLinks( ComponentHelpers.extractNames( extendedComponents )));
			}

			if( ! extendingComponents.isEmpty()) {
				msg = MessageFormat.format( this.messages.get( "component.is.extended.by" ), comp ); //$NON-NLS-1$
				section.append( renderParagraph( msg ));
				section.append( renderListAsLinks( ComponentHelpers.extractNames( extendingComponents )));
			}

			// Exported variables
			Map<String,String> exportedVariables = ComponentHelpers.findAllExportedVariables( comp );
			section.append( renderTitle3( this.messages.get( "exports" ))); //$NON-NLS-1$
			if( exportedVariables.isEmpty()) {
				msg = MessageFormat.format( this.messages.get( "component.no.export" ), comp ); //$NON-NLS-1$
				section.append( renderParagraph( msg ));

			} else {
				msg = MessageFormat.format( this.messages.get( "component.exports" ), comp ); //$NON-NLS-1$
				section.append( renderParagraph( msg ));
				section.append( renderList( convertExports( exportedVariables )));
			}

			// Hierarchy
			section.append( renderTitle3( this.messages.get( "hierarchy" ))); //$NON-NLS-1$
			Collection<Component> ancestors = ComponentHelpers.findAllAncestors( comp );
			Collection<Component> children = ComponentHelpers.findAllChildren( comp );
			if( ! ancestors.isEmpty() || ! children.isEmpty()) {
				AbstractRoboconfTransformer transformer = new HierarchicalTransformer( comp, ancestors, children, 4 );
				saveImage( comp, DiagramType.HIERARCHY, transformer, section );
			}

			if( ancestors.isEmpty()) {
				msg = MessageFormat.format( this.messages.get( "component.is.root" ), comp ); //$NON-NLS-1$
				section.append( renderParagraph( msg ));

			} else {
				msg = MessageFormat.format( this.messages.get( "component.over" ), comp ); //$NON-NLS-1$
				section.append( renderParagraph( msg ));
				section.append( renderListAsLinks( ComponentHelpers.extractNames( ancestors )));
			}

			if( ! children.isEmpty()) {
				msg = MessageFormat.format( this.messages.get( "component.children" ), comp ); //$NON-NLS-1$
				section.append( renderParagraph( msg ));
				section.append( renderListAsLinks( ComponentHelpers.extractNames( children )));
			}

			// Runtime
			section.append( renderTitle3( this.messages.get( "runtime" ))); //$NON-NLS-1$
			Map<String,Boolean> imports = ComponentHelpers.findAllImportedVariables( comp );
			if( imports.isEmpty()) {
				msg = MessageFormat.format( this.messages.get( "component.no.dep" ), comp ); //$NON-NLS-1$
				section.append( renderParagraph( msg ));

			} else {
				msg = MessageFormat.format( this.messages.get( "component.depends.on" ), comp ); //$NON-NLS-1$
				section.append( renderParagraph( msg ));
				section.append( renderList( getImportComponents( comp )));

				msg = MessageFormat.format( this.messages.get( "component.requires" ), comp ); //$NON-NLS-1$
				section.append( renderParagraph( msg ));
				section.append( renderList( convertImports( imports )));

			}

			// Extra
			String s = readCustomInformation( this.applicationDirectory, comp.getName(), DocConstants.COMP_EXTRA );
			if( ! Utils.isEmptyOrWhitespaces( s )) {
				section.append( renderTitle3( this.messages.get( "extra" ))); //$NON-NLS-1$
				section.append( renderParagraph( s ));
			}

			// End the section
			section = endSection( sectionName, section );
			sb.append( section );
			sectionNames.add( sectionName );
		}

		sb.append( renderSections( sectionNames ));
		return sb;
	}


	/**
	 * Renders information about the instances.
	 * @param application the application
	 * @param applicationDirectory the application's directory
	 * @return a string builder (never null)
	 */
	private StringBuilder renderInstances() {

		StringBuilder sb = new StringBuilder();
		sb.append( renderTitle1( this.messages.get( "instances" ))); //$NON-NLS-1$
		sb.append( renderParagraph( this.messages.get( "instances.intro" ))); //$NON-NLS-1$

		if( this.application.getRootInstances().isEmpty()) {
			sb.append( renderParagraph( this.messages.get( "instances.none" ))); //$NON-NLS-1$

		} else  {
			sb.append( renderParagraph( this.messages.get( "instances.sorting" ))); //$NON-NLS-1$

			// Split by root instance
			List<String> sectionNames = new ArrayList<String> ();
			Set<Instance> sortedRootInstances = new TreeSet<Instance>( new InstanceComparator());
			sortedRootInstances.addAll( this.application.getRootInstances());
			for( Instance inst : sortedRootInstances ) {

				// Start a section
				final String sectionName = DocConstants.SECTION_INSTANCES + inst.getName();
				StringBuilder section = startSection( sectionName );

				// Instances overview
				section.append( renderTitle2( inst.getName()));
				section.append( startTable());
				section.append( addTableHeader(
						this.messages.get( "instances.instance" ),		//$NON-NLS-1$
						this.messages.get( "instances.component" ),		//$NON-NLS-1$
						this.messages.get( "instances.installer" )));	//$NON-NLS-1$

				List<Instance> instances = new ArrayList<Instance> ();
				instances.addAll( InstanceHelpers.buildHierarchicalList( inst ));
				Collections.sort( instances, new InstanceComparator());

				for( Instance i : instances ) {
					StringBuilder content = new StringBuilder();
					String instancePath = InstanceHelpers.computeInstancePath( i );
					for( int j=1; j<InstanceHelpers.countInstances( instancePath ); j++ )
						content.insert( 0, indent());

					content.append( " " ); //$NON-NLS-1$
					content.append( i.getName());
					String componentName = i.getComponent().getName();
					String link = componentName;
					if( this.options.containsKey( DocConstants.OPTION_HTML_EXPLODED ))
						link = "../../" + DocConstants.SECTION_COMPONENTS + componentName; //$NON-NLS-1$

					String installer = ComponentHelpers.findComponentInstaller( i.getComponent());
					section.append( addTableLine(
							content.toString(),
							applyLink( componentName, link ),
							installer ));
				}

				section.append( endTable());

				// Additional variables?
				for( Instance i : instances ) {
					if( ! i.overriddenExports.isEmpty()) {
						String name = applyBoldStyle( i.getName(), i.getName());
						String msg = MessageFormat.format( this.messages.get( "instances.additional" ), name ); //$NON-NLS-1$

						section.append( renderParagraph( msg ));
						section.append( renderList( convertExports( i.overriddenExports )));
					}
				}

				// End the section
				section = endSection( sectionName, section );
				sb.append( section );
				sectionNames.add( sectionName );
			}

			sb.append( renderSections( sectionNames ));
		}

		return sb;
	}


	/**
	 * Generates and saves an image.
	 * @param comp the component to highlight in the image
	 * @param type the kind of relation to show in the diagram
	 * @param transformer a transformer for the graph generation
	 * @param sb the string builder to append the link to the generated image
	 * @throws IOException if something went wrong
	 */
	private void saveImage( final Component comp, DiagramType type, AbstractRoboconfTransformer transformer, StringBuilder sb )
	throws IOException {

		String baseName = comp.getName() + "_" + type; //$NON-NLS-1$
		String relativePath = "png/" + baseName + ".png"; //$NON-NLS-1$ //$NON-NLS-2$
		if( this.options.containsKey( DocConstants.OPTION_GEN_IMAGES_ONCE ))
			relativePath = "../" + relativePath;

		File pngFile = new File( this.outputDirectory, relativePath ).getCanonicalFile();
		if( ! pngFile.exists()) {

			Utils.createDirectory( pngFile.getParentFile());
			GraphUtils.writeGraph(
					pngFile,
					comp,
					transformer.getConfiguredLayout(),
					transformer.getGraph(),
					transformer.getEdgeShapeTransformer(),
					this.options );
		}

		sb.append( renderImage( comp.getName(), type, relativePath ));
	}


	/**
	 * Reads user-specified information from the project.
	 * @param applicationDirectory the application's directory
	 * @param componentName the component name
	 * @param suffix the file's suffix (see the DocConstants interface)
	 * @return the read information, as a string (never null)
	 * @throws IOException if the file could not be read
	 */
	private String readCustomInformation( File applicationDirectory, String componentName, String suffix )
	throws IOException {

		String result = ""; //$NON-NLS-1$
		File f = new File( applicationDirectory, DocConstants.DOC_DIR + "/" + componentName + suffix ); //$NON-NLS-1$
		if( f.exists())
			result = Utils.readFileContent( f );

		return result;
	}


	/**
	 * Converts imports to a human-readable text.
	 * @param imports a non-null map of imports
	 * @return a non-null list of string, one entry per import
	 */
	private List<String> convertImports( Map<String,Boolean> imports ) {

		List<String> result = new ArrayList<String> ();
		for( Map.Entry<String,Boolean> entry : imports.entrySet()) {
			String componentOrFacet = VariableHelpers.parseVariableName( entry.getKey()).getKey();
			String s = applyLink( entry.getKey(), componentOrFacet );
			s += entry.getValue() ? this.messages.get( "optional" ) : this.messages.get( "required" ); //$NON-NLS-1$ //$NON-NLS-2$
			result.add( s );
		}

		return result;
	}


	/**
	 * Converts exports to a human-readable text.
	 * @param exports a non-null map of exports
	 * @return a non-null list of string, one entry per exported variable
	 */
	private List<String> convertExports( Map<String,String> exports ) {

		List<String> result = new ArrayList<String> ();
		for( Map.Entry<String,String> entry : exports.entrySet()) {
			String componentOrFacet = VariableHelpers.parseVariableName( entry.getKey()).getKey();
			String s = Utils.isEmptyOrWhitespaces( componentOrFacet )
					? entry.getKey()
					: applyLink( entry.getKey(), componentOrFacet );

			if( ! Utils.isEmptyOrWhitespaces( entry.getValue()))
				s += MessageFormat.format( this.messages.get( "default" ), entry.getValue()); //$NON-NLS-1$

			if( entry.getKey().toLowerCase().endsWith( ".ip" )) //$NON-NLS-1$
				s += this.messages.get( "injected" ); //$NON-NLS-1$

			result.add( s );
		}

		return result;
	}


	/**
	 * Converts component dependencies to a human-readable text.
	 * @param component a component
	 * @return a non-null list of component names that match those this component needs
	 */
	private List<String> getImportComponents( Component component ) {

		List<String> result = new ArrayList<String> ();
		Map<String,Boolean> map = ComponentHelpers.findComponentDependenciesFor( component );
		for( Map.Entry<String,Boolean> entry : map.entrySet()) {
			String s = applyLink( entry.getKey(), entry.getKey());
			s += entry.getValue() ? this.messages.get( "optional" ) : this.messages.get( "required" ); //$NON-NLS-1$ //$NON-NLS-2$
			result.add( s );
		}

		return result;
	}


	/**
	 * Renders a list as a list of links.
	 * @param names a list of items
	 * @return a list of links that wrap the names
	 */
	private String renderListAsLinks( List<String> names ) {

		List<String> newNames = new ArrayList<String> ();
		for( String s : names )
			newNames.add( applyLink( s, s ));

		return renderList( newNames );
	}
}
