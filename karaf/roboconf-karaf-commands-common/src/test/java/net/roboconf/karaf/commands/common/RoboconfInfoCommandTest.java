/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.karaf.commands.common;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.ansi.SimpleAnsi;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import net.roboconf.core.Constants;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfInfoCommandTest {

	private RoboconfInfoCommand cmd;
	private BundleContext ctx;
	private Session session;
	private ByteArrayOutputStream os;


	@Before
	public void before() throws Exception {

		this.os = new ByteArrayOutputStream();
		this.ctx = Mockito.mock( BundleContext.class );
		this.session = Mockito.mock( Session.class );

		this.cmd = new RoboconfInfoCommand();
		this.cmd.out = new PrintStream( this.os, true, "UTF-8" );
		this.cmd.ctx = this.ctx;
		this.cmd.session = this.session;
	}


	@Test
	public void testNoRoboconf_shouldNotHappen() throws Exception {

		Mockito.when( this.ctx.getBundles()).thenReturn( new Bundle[ 0 ]);
		this.cmd.execute();

		String outputText = filterOutput( this.os.toString( "UTF-8" ));
		Assert.assertEquals( "Roboconf\n Roboconf version Undetermined", outputText );

		Mockito.verify( this.session, Mockito.only()).execute( RoboconfInfoCommand.KARAF_INFO );
		Mockito.verify( this.ctx, Mockito.only()).getBundles();
	}


	@Test
	public void testOneVersionOfRoboconf() throws Exception {

		Map<String,String> versionToBundle = new LinkedHashMap<> ();
		versionToBundle.put( "2.0", "whatever" );
		versionToBundle.put( "5.1", "bundle 1" );
		versionToBundle.put( "0.8", Constants.RBCF_CORE_SYMBOLIC_NAME );
		versionToBundle.put( "10", "oops" );
		versionToBundle.put( "36.5.1", "hey" );

		List<Bundle> bundles = new ArrayList<> ();
		for( Map.Entry<String,String> entry : versionToBundle.entrySet()) {
			Bundle bundleMock = Mockito.mock( Bundle.class );
			Mockito.when( bundleMock.getSymbolicName()).thenReturn( entry.getValue());

			Version v = Mockito.mock( Version.class );
			Mockito.when( v.toString()).thenReturn( entry.getKey());
			Mockito.when( bundleMock.getVersion()).thenReturn( v );

			bundles.add( bundleMock );
		}

		Mockito.when( this.ctx.getBundles()).thenReturn( bundles.toArray( new Bundle[ bundles.size()]));
		this.cmd.execute();

		String outputText = filterOutput( this.os.toString( "UTF-8" ));
		Assert.assertEquals( "Roboconf\n Roboconf version 0.8", outputText );

		Mockito.verify( this.session, Mockito.only()).execute( RoboconfInfoCommand.KARAF_INFO );
		Mockito.verify( this.ctx, Mockito.only()).getBundles();
	}


	@Test
	public void testSeveralVersionsOfRoboconf() throws Exception {

		Map<String,String> versionToBundle = new LinkedHashMap<> ();
		versionToBundle.put( "2.0", "whatever" );
		versionToBundle.put( "5.1", "bundle 1" );
		versionToBundle.put( "0.8", Constants.RBCF_CORE_SYMBOLIC_NAME );
		versionToBundle.put( "10", "oops" );
		versionToBundle.put( "0.9.1", Constants.RBCF_CORE_SYMBOLIC_NAME );
		versionToBundle.put( "36.5.1", "hey" );

		List<Bundle> bundles = new ArrayList<> ();
		for( Map.Entry<String,String> entry : versionToBundle.entrySet()) {
			Bundle bundleMock = Mockito.mock( Bundle.class );
			Mockito.when( bundleMock.getSymbolicName()).thenReturn( entry.getValue());

			Version v = Mockito.mock( Version.class );
			Mockito.when( v.toString()).thenReturn( entry.getKey());
			Mockito.when( bundleMock.getVersion()).thenReturn( v );

			bundles.add( bundleMock );
		}

		Mockito.when( this.ctx.getBundles()).thenReturn( bundles.toArray( new Bundle[ bundles.size()]));
		this.cmd.execute();

		String outputText = filterOutput( this.os.toString( "UTF-8" ));
		Assert.assertTrue( outputText.endsWith( "\n Roboconf versions 0.8\n 0.9.1" ));
		Assert.assertTrue( outputText.startsWith( "Roboconf\n\n[ WARNING ] " ));

		Mockito.verify( this.session, Mockito.only()).execute( RoboconfInfoCommand.KARAF_INFO );
		Mockito.verify( this.ctx, Mockito.only()).getBundles();
	}


	private String filterOutput( String output ) {

		String result = output.replace( "\r", "" );
		result = result.replace( SimpleAnsi.INTENSITY_BOLD, "" );
		result = result.replace( SimpleAnsi.INTENSITY_NORMAL, "" );
		result = result.replaceAll( " {2,}", " " ).trim();

		return result;
	}
}
