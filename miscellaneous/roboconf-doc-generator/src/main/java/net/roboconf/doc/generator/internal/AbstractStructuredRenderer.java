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

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.AbstractType;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.ImportedVariable;
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
	protected ApplicationTemplate applicationTemplate;
	protected File applicationDirectory;
	protected Map<String,String> options, typeAnnotations;
	protected Messages messages;
	protected String locale;


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static enum DiagramType {
		RUNTIME, HIERARCHY, INHERITANCE;
	}


	/**
	 * Constructor.
	 * @param outputDirectory
	 * @param applicationTemplate
	 * @param applicationDirectory
	 * @param typeAnnotations (can be null)
	 */
	public AbstractStructuredRenderer(
			File outputDirectory,
			ApplicationTemplate applicationTemplate,
			File applicationDirectory,
			Map<String,String> typeAnnotations ) {

		this.outputDirectory = outputDirectory;
		this.applicationTemplate = applicationTemplate;
		this.applicationDirectory = applicationDirectory;
		this.typeAnnotations = typeAnnotations != null ? typeAnnotations : new HashMap<String,String>( 0 );
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.doc.generator.internal.IRenderer
	 * #render(java.util.Map)
	 */
	@Override
	public void render( Map<String,String> options ) throws IOException {

		// Keep the options
		this.options = options;

		// Check the language
		this.locale = options.get( DocConstants.OPTION_LOCALE );
		if( this.locale != null )
			this.messages = new Messages( this.locale );
		else
			this.messages = new Messages();

		// What to render?
		if( options.containsKey( DocConstants.OPTION_RECIPE ))
			renderRecipe();
		else
			renderApplication();
	}


	/**
	 * Renders an applicationTemplate.
	 * @throws IOException
	 */
	private void renderApplication() throws IOException {
		StringBuilder sb = new StringBuilder();

		// First pages
		sb.append( renderDocumentTitle());
		sb.append( renderPageBreak());

		sb.append( renderParagraph( this.messages.get( "intro" ))); //$NON-NLS-1$
		sb.append( renderPageBreak());

		sb.append( renderDocumentIndex());
		sb.append( renderPageBreak());

		sb.append( startTable());
		sb.append( addTableLine( this.messages.get( "app.name" ), this.applicationTemplate.getName())); //$NON-NLS-1$
		sb.append( addTableLine( this.messages.get( "app.qualifier" ), this.applicationTemplate.getVersion())); //$NON-NLS-1$
		sb.append( endTable());

		sb.append( renderApplicationDescription());
		sb.append( renderPageBreak());
		sb.append( renderSections( new ArrayList<String>( 0 )));

		// Render information about components
		sb.append( renderComponents());

		// Render information about initial instances
		sb.append( renderInstances());

		writeFileContent( sb.toString());
	}


	/**
	 * Renders a recipe.
	 * @throws IOException
	 */
	private void renderRecipe() throws IOException {
		StringBuilder sb = new StringBuilder();

		// First pages
		if( ! Constants.GENERATED.equalsIgnoreCase( this.applicationTemplate.getName())) {
			sb.append( renderDocumentTitle());
			sb.append( renderPageBreak());

			sb.append( renderParagraph( this.messages.get( "intro" ))); //$NON-NLS-1$
			sb.append( renderPageBreak());

			sb.append( renderDocumentIndex());
			sb.append( renderPageBreak());

			sb.append( startTable());
			sb.append( addTableLine( this.messages.get( "app.name" ), this.applicationTemplate.getName())); //$NON-NLS-1$
			sb.append( addTableLine( this.messages.get( "app.qualifier" ), this.applicationTemplate.getVersion())); //$NON-NLS-1$
			sb.append( endTable());

			sb.append( renderApplicationDescription());
			sb.append( renderPageBreak());
			sb.append( renderSections( new ArrayList<String>( 0 )));

		} else {
			sb.append( renderDocumentIndex());
			sb.append( renderPageBreak());
		}

		// Render information about components
		sb.append( renderComponents());

		// Render information about facets
		sb.append( renderFacets());

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
	 * @return a string builder (never null)
	 * @throws IOException
	 */
	private StringBuilder renderComponents() throws IOException {

		StringBuilder sb = new StringBuilder();
		sb.append( renderTitle1( this.messages.get( "components" ))); //$NON-NLS-1$
		sb.append( renderParagraph( this.messages.get( "components.intro" ))); //$NON-NLS-1$

		List<String> sectionNames = new ArrayList<> ();
		List<Component> allComponents = ComponentHelpers.findAllComponents( this.applicationTemplate );
		Collections.sort( allComponents, new AbstractTypeComparator());
		for( Component comp : allComponents ) {

			// Start a new section
			final String sectionName = DocConstants.SECTION_COMPONENTS + comp.getName();
			StringBuilder section = startSection( sectionName );

			// Overview
			section.append( renderTitle2( comp.getName()));
			section.append( renderTitle3( this.messages.get( "overview" ))); //$NON-NLS-1$

			String customInfo = readCustomInformation( this.applicationDirectory, comp.getName(), DocConstants.COMP_SUMMARY );
			if( Utils.isEmptyOrWhitespaces( customInfo ))
				customInfo = this.typeAnnotations.get( comp.getName());

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
			Collection<AbstractType> ancestors = new ArrayList<>();
			ancestors.addAll( ComponentHelpers.findAllAncestors( comp ));

			Set<AbstractType> children = new HashSet<>();;
			children.addAll( ComponentHelpers.findAllChildren( comp ));

			// For recipes, ancestors and children should include facets
			if( this.options.containsKey( DocConstants.OPTION_RECIPE )) {
				for( AbstractType type : comp.getAncestors()) {
					if( type instanceof Facet ) {
						ancestors.add( type );
						ancestors.addAll( ComponentHelpers.findAllExtendingFacets((Facet) type));
					}
				}

				for( AbstractType type : comp.getChildren()) {
					if( type instanceof Facet ) {
						children.add( type );
						children.addAll( ComponentHelpers.findAllExtendingFacets((Facet) type));
					}
				}
			}

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
			Collection<ImportedVariable> imports = ComponentHelpers.findAllImportedVariables( comp ).values();
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
	 * Renders information about the facets.
	 * @return a string builder (never null)
	 * @throws IOException
	 */
	private StringBuilder renderFacets() throws IOException {

		StringBuilder sb = new StringBuilder();
		if( ! this.applicationTemplate.getGraphs().getFacetNameToFacet().isEmpty()) {

			sb.append( renderTitle1( this.messages.get( "facets" ))); //$NON-NLS-1$
			sb.append( renderParagraph( this.messages.get( "facets.intro" ))); //$NON-NLS-1$

			List<String> sectionNames = new ArrayList<> ();
			List<Facet> allFacets = new ArrayList<>( this.applicationTemplate.getGraphs().getFacetNameToFacet().values());
			Collections.sort( allFacets, new AbstractTypeComparator());

			for( Facet facet : allFacets ) {

				// Start a new section
				final String sectionName = DocConstants.SECTION_FACETS + facet.getName();
				StringBuilder section = startSection( sectionName );

				// Overview
				section.append( renderTitle2( facet.getName()));

				String customInfo = readCustomInformation( this.applicationDirectory, facet.getName(), DocConstants.FACET_DETAILS );
				if( Utils.isEmptyOrWhitespaces( customInfo ))
					customInfo = this.typeAnnotations.get( facet.getName());

				if( ! Utils.isEmptyOrWhitespaces( customInfo )) {
					section.append( renderTitle3( this.messages.get( "overview" ))); //$NON-NLS-1$
					section.append( renderParagraph( customInfo ));
				}

				// Exported variables
				Map<String,String> exportedVariables = ComponentHelpers.findAllExportedVariables( facet );
				section.append( renderTitle3( this.messages.get( "exports" ))); //$NON-NLS-1$
				if( exportedVariables.isEmpty()) {
					String msg = MessageFormat.format( this.messages.get( "facet.no.export" ), facet ); //$NON-NLS-1$
					section.append( renderParagraph( msg ));

				} else {
					String msg = MessageFormat.format( this.messages.get( "facet.exports" ), facet ); //$NON-NLS-1$
					section.append( renderParagraph( msg ));
					section.append( renderList( convertExports( exportedVariables )));
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
	 * Renders information about the instances.
	 * @return a string builder (never null)
	 */
	private StringBuilder renderInstances() {

		StringBuilder sb = new StringBuilder();
		sb.append( renderTitle1( this.messages.get( "instances" ))); //$NON-NLS-1$
		sb.append( renderParagraph( this.messages.get( "instances.intro" ))); //$NON-NLS-1$

		if( this.applicationTemplate.getRootInstances().isEmpty()) {
			sb.append( renderParagraph( this.messages.get( "instances.none" ))); //$NON-NLS-1$

		} else  {
			sb.append( renderParagraph( this.messages.get( "instances.sorting" ))); //$NON-NLS-1$

			// Split by root instance
			List<String> sectionNames = new ArrayList<> ();
			Set<Instance> sortedRootInstances = new TreeSet<>( new InstanceComparator());
			sortedRootInstances.addAll( this.applicationTemplate.getRootInstances());
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

				List<Instance> instances = new ArrayList<> ();
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
	 * Renders the application's description.
	 * @return a non-null string
	 * @throws IOException if something went wrong
	 */
	private Object renderApplicationDescription() throws IOException {

		// No locale? => Display the application's description.
		// Otherwise, read app.desc_fr_FR.txt or the required file for another locale.
		// If it does not exist, return the empty string.

		String s;
		if( this.locale == null
				&& ! Utils.isEmptyOrWhitespaces( this.applicationTemplate.getDescription()))
			s = this.applicationTemplate.getDescription();
		else
			s = readCustomInformation( this.applicationDirectory, DocConstants.APP_DESC_PREFIX, DocConstants.FILE_SUFFIX );

		String result = "";
		if( ! Utils.isEmptyOrWhitespaces( s ))
			result = renderParagraph( s );

		return result;
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
	 * @param prefix the prefix name
	 * @param suffix the file's suffix (see the DocConstants interface)
	 * @return the read information, as a string (never null)
	 * @throws IOException if the file could not be read
	 */
	private String readCustomInformation( File applicationDirectory, String prefix, String suffix )
	throws IOException {

		// Prepare the file name
		StringBuilder sb = new StringBuilder();
		sb.append( prefix );
		if( this.locale != null )
			sb.append( "_" + this.locale );

		sb.append( suffix );
		sb.insert( 0, "/" ); //$NON-NLS-1$
		sb.insert( 0, DocConstants.DOC_DIR );

		// Handle usual (doc) and Maven (src/main/doc) cases
		File f = new File( applicationDirectory, sb.toString());
		if( ! f.exists())
			f = new File( f.getParentFile().getParentFile(), sb.toString());

		String result = ""; //$NON-NLS-1$
		if( f.exists())
			result = Utils.readFileContent( f );

		return result;
	}


	/**
	 * Converts imports to a human-readable text.
	 * @param importedVariables a non-null set of imported variables
	 * @return a non-null list of string, one entry per import
	 */
	private List<String> convertImports( Collection<ImportedVariable> importedVariables ) {

		List<String> result = new ArrayList<> ();
		for( ImportedVariable var : importedVariables ) {
			String componentOrFacet = VariableHelpers.parseVariableName( var.getName()).getKey();
			String s = applyLink( var.getName(), componentOrFacet );
			s += var.isOptional() ? this.messages.get( "optional" ) : this.messages.get( "required" ); //$NON-NLS-1$ //$NON-NLS-2$
			if( var.isExternal())
				s += this.messages.get( "external" ); //$NON-NLS-1$

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

		List<String> result = new ArrayList<> ();
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

		List<String> result = new ArrayList<> ();
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

		List<String> newNames = new ArrayList<> ();
		for( String s : names )
			newNames.add( applyLink( s, s ));

		return renderList( newNames );
	}
}
