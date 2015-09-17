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

package net.roboconf.dm.internal.api.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.api.IConfigurationMngr;
import net.roboconf.dm.management.api.ITargetsMngr;
import net.roboconf.dm.management.api.ITargetsMngr.TargetBean;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetsMngrImplTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private ITargetsMngr mngr;
	private IConfigurationMngr configurationMngr;


	@Before
	public void prepareMngr() throws Exception {

		this.configurationMngr = Mockito.mock( IConfigurationMngr.class );
		Mockito.when( this.configurationMngr.getWorkingDirectory()).thenReturn( this.folder.newFolder());

		this.mngr = new TargetsMngrImpl( this.configurationMngr );
	}


	@Test
	public void testNormalCrudScenarios() throws Exception {

		Assert.assertEquals( 0, this.mngr.findRawTargetProperties( "whatever" ).size());
		String targetId = this.mngr.createTarget( "prop: ok" );
		Assert.assertNotNull( targetId );

		String newTargetId = this.mngr.createTarget( "ok: ok" );
		Assert.assertNotNull( newTargetId );

		Map<String,String> props = this.mngr.findRawTargetProperties( targetId );
		Assert.assertEquals( 1, props.size());
		Assert.assertEquals( "ok", props.get( "prop" ));

		props = this.mngr.findRawTargetProperties( newTargetId );
		Assert.assertEquals( 1, props.size());
		Assert.assertEquals( "ok", props.get( "ok" ));

		this.mngr.updateTarget( targetId, "prop2: ko\nprop1: done" );
		props = this.mngr.findRawTargetProperties( targetId );
		Assert.assertEquals( 2, props.size());
		Assert.assertEquals( "done", props.get( "prop1" ));
		Assert.assertEquals( "ko", props.get( "prop2" ));

		this.mngr.deleteTarget( targetId );
		Assert.assertEquals( 0, this.mngr.findRawTargetProperties( targetId ).size());
	}


	@Test
	public void testCreateTargetFromFile() throws Exception {

		File f = this.folder.newFile();
		Utils.writeStringInto( "prop: value", f );

		String targetId = this.mngr.createTarget( f );
		Assert.assertNotNull( targetId );

		Map<String,String> props = this.mngr.findRawTargetProperties( targetId );
		Assert.assertEquals( 1, props.size());
		Assert.assertEquals( "value", props.get( "prop" ));

		this.mngr.deleteTarget( targetId );
		Assert.assertEquals( 0, this.mngr.findRawTargetProperties( targetId ).size());
	}


	@Test( expected = IOException.class )
	public void testUpdateTarget_whenTargetDoesNotExist() throws Exception {

		this.mngr.updateTarget( "inexisting", "prop: ok" );
	}


	@Test
	public void testAssociations() throws Exception {

		TestApplication app = new TestApplication();
		String mySqlPath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		String tomcatPath = InstanceHelpers.computeInstancePath( app.getTomcatVm());

		// Only MySQL has an associated target
		String targetId = this.mngr.createTarget( "prop: ok" );
		this.mngr.associateTargetWithScopedInstance( targetId, app, mySqlPath );

		String associatedId = this.mngr.findTargetId( app, mySqlPath );
		Assert.assertEquals( targetId, associatedId );

		// There is no value for Tomcat, nor default target for the application
		Assert.assertNull( this.mngr.findTargetId( app, tomcatPath ));

		// Let's define a default target for the whole application
		String defaultTargetId = this.mngr.createTarget( "prop: ok" );
		this.mngr.associateTargetWithScopedInstance( defaultTargetId, app, null );

		associatedId = this.mngr.findTargetId( app, mySqlPath );
		Assert.assertEquals( targetId, associatedId );

		associatedId = this.mngr.findTargetId( app, tomcatPath );
		Assert.assertEquals( defaultTargetId, associatedId );

		// Remove the custom association for MySQL
		this.mngr.dissociateTargetFromScopedInstance( app, mySqlPath );
		associatedId = this.mngr.findTargetId( app, mySqlPath );
		Assert.assertEquals( defaultTargetId, associatedId );

		// Make sure we cannot delete a default target
		this.mngr.dissociateTargetFromScopedInstance( app, null );
		associatedId = this.mngr.findTargetId( app, mySqlPath );
		Assert.assertEquals( defaultTargetId, associatedId );

		// Make sure we can override a default target
		this.mngr.associateTargetWithScopedInstance( targetId, app, null );
		associatedId = this.mngr.findTargetId( app, mySqlPath );
		Assert.assertEquals( targetId, associatedId );

		associatedId = this.mngr.findTargetId( app, tomcatPath );
		Assert.assertEquals( targetId, associatedId );
	}


	@Test
	public void testHints_noHint() throws Exception {

		TestApplication app1 = new TestApplication();
		app1.name( "app1" );
		TestApplication app2 = new TestApplication();
		app2.name( "app2" );

		String t1 = this.mngr.createTarget( "prop: ok\nname: target 1\ndescription: t1's target" );
		String t2 = this.mngr.createTarget( "prop: ok\nhandler: docker" );

		List<TargetBean> beans = this.mngr.listPossibleTargets( app1 );
		Assert.assertEquals( 2, beans.size());

		TargetBean b1 = beans.get( 0 );
		Assert.assertEquals( t1, b1.id );
		Assert.assertEquals( "target 1", b1.name );
		Assert.assertEquals( "t1's target", b1.description );
		Assert.assertNull( b1.handler );
		Assert.assertFalse( b1.isDefault );

		TargetBean b2 = beans.get( 1 );
		Assert.assertEquals( t2, b2.id );
		Assert.assertEquals( "docker", b2.handler );
		Assert.assertNull( b2.name );
		Assert.assertNull( b2.description );
		Assert.assertFalse( b2.isDefault );

		Assert.assertEquals( 2, this.mngr.listPossibleTargets( app2 ).size());
	}


	@Test
	public void testHints_hintOnApplication() throws Exception {

		TestApplication app1 = new TestApplication();
		app1.name( "app1" );
		TestApplication app2 = new TestApplication();
		app2.name( "app2" );

		String t1 = this.mngr.createTarget( "prop: ok\nname: target 1\ndescription: t1's target" );
		String t2 = this.mngr.createTarget( "prop: ok\nhandler: docker" );

		// Hint between app1 and t1.
		// t1 has now a scope, which includes app1.
		// Therefore, t1 should not be listed for app2 (not in the scope).
		this.mngr.addHint( t1, app1 );

		List<TargetBean> beans = this.mngr.listPossibleTargets( app1 );
		Assert.assertEquals( 2, beans.size());

		TargetBean b1 = beans.get( 0 );
		Assert.assertEquals( t1, b1.id );
		Assert.assertEquals( "target 1", b1.name );
		Assert.assertEquals( "t1's target", b1.description );
		Assert.assertNull( b1.handler );
		Assert.assertFalse( b1.isDefault );

		TargetBean b2 = beans.get( 1 );
		Assert.assertEquals( t2, b2.id );
		Assert.assertEquals( "docker", b2.handler );
		Assert.assertNull( b2.name );
		Assert.assertNull( b2.description );
		Assert.assertFalse( b2.isDefault );

		beans = this.mngr.listPossibleTargets( app2 );
		Assert.assertEquals( 1, beans.size());

		b2 = beans.get( 0 );
		Assert.assertEquals( t2, b2.id );
		Assert.assertEquals( "docker", b2.handler );
		Assert.assertNull( b2.name );
		Assert.assertNull( b2.description );
		Assert.assertFalse( b2.isDefault );
	}


	@Test
	public void testHints_removeHintOnApplication() throws Exception {

		TestApplication app1 = new TestApplication();
		app1.name( "app1" );
		TestApplication app2 = new TestApplication();
		app2.name( "app2" );

		String t1 = this.mngr.createTarget( "prop: ok\nname: target 1\ndescription: t1's target" );
		String t2 = this.mngr.createTarget( "prop: ok\nhandler: docker" );

		// Hint between app1 and t1.
		// t1 has now a scope, which includes app1.
		// Therefore, t1 should not be listed for app2 (not in the scope).
		this.mngr.addHint( t1, app1 );

		List<TargetBean> beans = this.mngr.listPossibleTargets( app1 );
		Assert.assertEquals( 2, beans.size());

		beans = this.mngr.listPossibleTargets( app2 );
		Assert.assertEquals( 1, beans.size());

		// Remove the hint on the WRONG application => nothing changes
		this.mngr.removeHint( t1, app2 );

		beans = this.mngr.listPossibleTargets( app1 );
		Assert.assertEquals( 2, beans.size());

		beans = this.mngr.listPossibleTargets( app2 );
		Assert.assertEquals( 1, beans.size());

		// Remove the hint on the WRONG application => nothing changes
		this.mngr.removeHint( t2, app1 );

		beans = this.mngr.listPossibleTargets( app1 );
		Assert.assertEquals( 2, beans.size());

		beans = this.mngr.listPossibleTargets( app2 );
		Assert.assertEquals( 1, beans.size());

		// Remove the hint on the one we used
		this.mngr.removeHint( t1, app1 );

		beans = this.mngr.listPossibleTargets( app1 );
		Assert.assertEquals( 2, beans.size());

		beans = this.mngr.listPossibleTargets( app2 );
		Assert.assertEquals( 2, beans.size());
	}


	@Test
	public void testHints_hintOnApplicationTemplate() throws Exception {

		TestApplication app1 = new TestApplication();
		app1.name( "app1" );
		TestApplication app2 = new TestApplication();
		app2.name( "app2" );
		Application app3 = new Application( "app3", new ApplicationTemplate( "tpl" ).qualifier( "v1" ));

		String t1 = this.mngr.createTarget( "prop: ok\nname: target 1\ndescription: t1's target" );
		String t2 = this.mngr.createTarget( "prop: ok\nhandler: docker" );

		// Hint between app1 and app2's template and t1.
		// t1 has now a scope, which includes (indirectly) app1 and app2.
		// Therefore, t1 should not be listed for app3 (not in the scope).
		this.mngr.addHint( t1, app1.getTemplate());

		List<TargetBean> beans = this.mngr.listPossibleTargets( app1 );
		Assert.assertEquals( 2, beans.size());

		TargetBean b1 = beans.get( 0 );
		Assert.assertEquals( t1, b1.id );
		Assert.assertEquals( "target 1", b1.name );
		Assert.assertEquals( "t1's target", b1.description );
		Assert.assertNull( b1.handler );
		Assert.assertFalse( b1.isDefault );

		TargetBean b2 = beans.get( 1 );
		Assert.assertEquals( t2, b2.id );
		Assert.assertEquals( "docker", b2.handler );
		Assert.assertNull( b2.name );
		Assert.assertNull( b2.description );
		Assert.assertFalse( b2.isDefault );

		List<TargetBean> otherBeans = this.mngr.listPossibleTargets( app2 );
		Assert.assertEquals( beans, otherBeans );

		otherBeans = this.mngr.listPossibleTargets( app2.getTemplate());
		Assert.assertEquals( beans, otherBeans );

		beans = this.mngr.listPossibleTargets( app3 );
		Assert.assertEquals( 1, beans.size());

		b2 = beans.get( 0 );
		Assert.assertEquals( t2, b2.id );
		Assert.assertEquals( "docker", b2.handler );
		Assert.assertNull( b2.name );
		Assert.assertNull( b2.description );
		Assert.assertFalse( b2.isDefault );
	}


	@Test
	public void testFindRawTargetProperties_noProperties() {
		Assert.assertEquals( 0, this.mngr.findRawTargetProperties( new TestApplication(), "/whatever" ).size());
	}


	@Test
	public void testFindRawTargetProperties_withProperties() throws Exception {

		TestApplication app = new TestApplication();
		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		String targetId = this.mngr.createTarget( "prop: ok" );
		this.mngr.associateTargetWithScopedInstance( targetId, app, instancePath );

		Map<String,String> props = this.mngr.findRawTargetProperties( app, instancePath );
		Assert.assertEquals( 1, props.size());
		Assert.assertEquals( "ok", props.get( "prop" ));
	}


	@Test
	public void testRestoreCache() throws Exception {

		this.mngr.createTarget( "prop: ok" );
		this.mngr.createTarget( "prop: ok" );
		this.mngr.createTarget( "prop: ok" );
		this.mngr.createTarget( "prop: ok" );

		// Next ID should be 5
		Assert.assertEquals( "5", this.mngr.createTarget( "prop: ok" ));

		// Delete two in the middle (let holes)
		this.mngr.deleteTarget( "3" );
		this.mngr.deleteTarget( "4" );

		// Create a new manager and check the next new target is "6"
		this.mngr = new TargetsMngrImpl( this.configurationMngr );
		Assert.assertEquals( "6", this.mngr.createTarget( "prop: ok" ));

		// Add associations and make sure it works
		TestApplication app = new TestApplication();
		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());

		Assert.assertNull( this.mngr.findTargetId( app, instancePath ));
		this.mngr.associateTargetWithScopedInstance( "6", app, instancePath );
		Assert.assertEquals( "6", this.mngr.findTargetId( app, instancePath ));

		// Create another manager
		this.mngr = new TargetsMngrImpl( this.configurationMngr );
		Assert.assertEquals( "7", this.mngr.createTarget( "prop: ok" ));
		Assert.assertEquals( "6", this.mngr.findTargetId( app, instancePath ));
	}


	@Test
	public void testLocking_ByOneInstance() throws Exception {

		TestApplication app = new TestApplication();
		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		Assert.assertEquals( 0, this.mngr.listAllTargets().size());

		String targetId = this.mngr.createTarget( "prop: ok" );
		this.mngr.associateTargetWithScopedInstance( targetId, app, instancePath );

		Map<String,String> props = this.mngr.lockAndGetTarget( app, app.getMySqlVm());
		Assert.assertEquals( 1, props.size());
		Assert.assertEquals( "ok", props.get( "prop" ));

		Assert.assertEquals( 1, this.mngr.listAllTargets().size());
		try {
			this.mngr.deleteTarget( targetId );
			Assert.fail( "A target is locked <=> We cannot delete it." );

		} catch( IOException e ) {
			// nothing
		}

		this.mngr.unlockTarget( app, app.getMySqlVm());
		Assert.assertEquals( 1, this.mngr.listAllTargets().size());

		this.mngr.deleteTarget( targetId );
		Assert.assertEquals( 0, this.mngr.listAllTargets().size());
	}


	@Test
	public void testLocking_ByTwoInstances() throws Exception {

		TestApplication app = new TestApplication();
		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		Assert.assertEquals( 0, this.mngr.listAllTargets().size());

		String targetId = this.mngr.createTarget( "prop: ok" );
		this.mngr.associateTargetWithScopedInstance( targetId, app, instancePath );
		this.mngr.associateTargetWithScopedInstance( targetId, app, null );

		Map<String,String> props = this.mngr.lockAndGetTarget( app, app.getMySqlVm());
		Assert.assertEquals( 1, props.size());
		Assert.assertEquals( "ok", props.get( "prop" ));

		props = this.mngr.lockAndGetTarget( app, app.getTomcatVm());
		Assert.assertEquals( 1, props.size());
		Assert.assertEquals( "ok", props.get( "prop" ));

		Assert.assertEquals( 1, this.mngr.listAllTargets().size());
		try {
			this.mngr.deleteTarget( targetId );
			Assert.fail( "A target is locked <=> We cannot delete it." );

		} catch( IOException e ) {
			// nothing
		}

		this.mngr.unlockTarget( app, app.getMySqlVm());
		Assert.assertEquals( 1, this.mngr.listAllTargets().size());

		try {
			this.mngr.deleteTarget( targetId );
			Assert.fail( "A target is locked <=> We cannot delete it." );

		} catch( IOException e ) {
			// nothing
		}

		this.mngr.unlockTarget( app, app.getTomcatVm());
		this.mngr.deleteTarget( targetId );
		Assert.assertEquals( 0, this.mngr.listAllTargets().size());
	}


	@Test
	public void testCopyOriginalMapping_onInstancePath() throws Exception {

		TestApplication app = new TestApplication();
		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		String t1 = this.mngr.createTarget( "prop: ok" );
		String t2 = this.mngr.createTarget( "prop: ok" );

		// Association is on the template AND the instance
		Assert.assertNull( this.mngr.findTargetId( app, instancePath ));
		this.mngr.associateTargetWithScopedInstance( t1, app.getTemplate(), instancePath );
		Assert.assertNull( this.mngr.findTargetId( app, instancePath ));

		this.mngr.copyOriginalMapping( app );
		Assert.assertEquals( t1, this.mngr.findTargetId( app, instancePath ));

		// We can override the association
		this.mngr.associateTargetWithScopedInstance( t2, app, instancePath );
		Assert.assertEquals( t2, this.mngr.findTargetId( app, instancePath ));
	}


	@Test
	public void testCopyOriginalMapping_onDefault() throws Exception {

		TestApplication app = new TestApplication();
		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		String t1 = this.mngr.createTarget( "prop: ok" );
		String t2 = this.mngr.createTarget( "prop: ok" );

		// Association is on the template and BY DEFAULT
		Assert.assertNull( this.mngr.findTargetId( app, instancePath ));
		this.mngr.associateTargetWithScopedInstance( t1, app.getTemplate(), null );
		Assert.assertNull( this.mngr.findTargetId( app, instancePath ));

		this.mngr.copyOriginalMapping( app );
		Assert.assertEquals( t1, this.mngr.findTargetId( app, instancePath ));

		// We can override the association
		this.mngr.associateTargetWithScopedInstance( t2, app, instancePath );
		Assert.assertEquals( t2, this.mngr.findTargetId( app, instancePath ));
	}


	@Test
	public void testBuildList_exception() throws Exception {

		// Two targets, but only one with valid properties.
		// The invalid one won't be listed.
		File dir1 = this.folder.newFolder();
		File dir2 = this.folder.newFolder();
		Utils.writeStringInto( "prop: done", new File( dir1, Constants.TARGET_PROPERTIES_FILE_NAME ));

		List<File> targetDirectories = new ArrayList<>( 2 );
		targetDirectories.add( dir1 );
		targetDirectories.add( dir2 );

		List<TargetBean> beans = ((TargetsMngrImpl) this.mngr).buildList( targetDirectories, null );
		Assert.assertEquals( 1, beans.size());
		Assert.assertEquals( dir1.getName(), beans.get( 0 ).id );
	}
}
