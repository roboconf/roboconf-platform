/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.core.model.converters;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.io.ParsingModelIo;
import net.roboconf.core.model.parsing.FileDefinition;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;
import net.roboconf.core.model.runtime.Instance;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class FromInstanceDefinitionTest {

	@Test( expected = IllegalArgumentException.class )
	public void testIllegalArgument() {

		FileDefinition def = new FileDefinition( new File( "whatever.txt" ));
		def.setFileType( FileDefinition.GRAPH );
		new FromInstanceDefinition( def );
	}


	@Test
	public void testAnalyzeOverriddenExport() {

		Component tomcatComponent = new Component( "Tomcat" ).alias( "App Server" ).installerName( "puppet" );
		tomcatComponent.getExportedVariables().put( "tomcat.ip", null );
		tomcatComponent.getExportedVariables().put( "tomcat.port", "8080" );

		Instance tomcatInstance = new Instance( "tomcat" ).component( tomcatComponent );
		List<ModelError> errors = FromInstanceDefinition.analyzeOverriddenExport( 0, tomcatInstance, "unknown", "whatever" );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CO_NOT_OVERRIDING, errors.get( 0 ).getErrorCode());

		errors = FromInstanceDefinition.analyzeOverriddenExport( 0, tomcatInstance, "tomcat.port", "whatever" );
		Assert.assertEquals( 0, errors.size());

		errors = FromInstanceDefinition.analyzeOverriddenExport( 0, tomcatInstance, "port", "whatever" );
		Assert.assertEquals( 0, errors.size());

		tomcatComponent.getExportedVariables().put( "some-facet.port", "8081" );
		errors = FromInstanceDefinition.analyzeOverriddenExport( 0, tomcatInstance, "port", "whatever" );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.CO_AMBIGUOUS_OVERRIDING, errors.get( 0 ).getErrorCode());

		errors = FromInstanceDefinition.analyzeOverriddenExport( 0, tomcatInstance, "tomcat.port", "whatever" );
		Assert.assertEquals( 0, errors.size());

		errors = FromInstanceDefinition.analyzeOverriddenExport( 0, tomcatInstance, "some-facet.port", "whatever" );
		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testOverriddenExports() throws Exception {

		Component vmComponent = new Component( "VM" ).alias( "a VM" ).installerName( "iaas" );
		Component tomcatComponent = new Component( "Tomcat" ).alias( "App Server" ).installerName( "puppet" );
		tomcatComponent.getExportedVariables().put( "tomcat.ip", null );
		tomcatComponent.getExportedVariables().put( "tomcat.port", "8080" );

		ComponentHelpers.insertChild( vmComponent, tomcatComponent );
		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( vmComponent );

		File f = TestUtils.findTestFile( "/configurations/valid/instance-overridden-exports.instances" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		FromInstanceDefinition fromDef = new FromInstanceDefinition( def );
		Collection<Instance> rootInstances = fromDef.buildInstances( graphs );

		Iterator<ModelError> iterator = fromDef.getErrors().iterator();
		Assert.assertEquals( ErrorCode.CO_NOT_OVERRIDING, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());

		Assert.assertEquals( 1, rootInstances.size());
		Instance rootInstance = rootInstances.iterator().next();
		Assert.assertEquals( 2, InstanceHelpers.buildHierarchicalList( rootInstance ).size());
	}


	@Test
	public void testDuplicateInstance() throws Exception {

		Component vmComponent = new Component( "VM" ).alias( "a VM" ).installerName( "iaas" );
		Component tomcatComponent = new Component( "Tomcat" ).alias( "App Server" ).installerName( "puppet" );
		tomcatComponent.getExportedVariables().put( "tomcat.ip", null );
		tomcatComponent.getExportedVariables().put( "tomcat.port", "8080" );

		ComponentHelpers.insertChild( vmComponent, tomcatComponent );
		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( vmComponent );

		File f = TestUtils.findTestFile( "/configurations/invalid/duplicate-instance.instances" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		FromInstanceDefinition fromDef = new FromInstanceDefinition( def );
		fromDef.buildInstances( graphs );
		Iterator<ModelError> iterator = fromDef.getErrors().iterator();
		Assert.assertEquals( ErrorCode.CO_ALREADY_DEFINED_INSTANCE, iterator.next().getErrorCode());
		Assert.assertEquals( ErrorCode.CO_ALREADY_DEFINED_INSTANCE, iterator.next().getErrorCode());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testComplexInstances() throws Exception {

		// The graph
		Graphs graphs = new Graphs();
		Component vmComponent = new Component( "VM" ).alias( "VM" ).installerName( "iaas" );
		graphs.getRootComponents().add( vmComponent );

		Component tomcatComponent = new Component( "Tomcat" ).alias( "Tomcat" ).installerName( "puppet" );
		tomcatComponent.getExportedVariables().put( "Tomcat.ip", null );
		tomcatComponent.getExportedVariables().put( "Tomcat.port", "8080" );
		ComponentHelpers.insertChild( vmComponent, tomcatComponent );

		Component warComponent = new Component( "WAR" ).alias( "A simple web application" ).installerName( "bash" );
		ComponentHelpers.insertChild( tomcatComponent, warComponent );

		// The file to read
		File f = TestUtils.findTestFile( "/configurations/valid/complex-instances.instances" );
		FileDefinition def = ParsingModelIo.readConfigurationFile( f, true );
		Assert.assertEquals( 0, def.getParsingErrors().size());

		FromInstanceDefinition fromDef = new FromInstanceDefinition( def );
		Collection<Instance> rootInstances = fromDef.buildInstances( graphs );

		// The assertions
		Application app = new Application();
		app.getRootInstances().addAll( rootInstances );

		Assert.assertEquals( 3, rootInstances.size());
		Assert.assertEquals( 8, InstanceHelpers.getAllInstances( app ).size());
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-1" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-1/i-tomcat" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-1/i-tomcat/i-war" ));

		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-2" ));

		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-3" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-3/i-tomcat-1" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-3/i-tomcat-2" ));
		Assert.assertNotNull( InstanceHelpers.findInstanceByPath( app, "/i-vm-3/i-tomcat-2/i-war" ));
	}
}
