/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.rest.services.internal.resources.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.jersey.core.header.FormDataContentDisposition;

import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestApplicationTemplate;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.TargetUsageItem;
import net.roboconf.core.model.runtime.TargetWrapperDescriptor;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.commons.json.StringWrapper;
import net.roboconf.dm.rest.services.internal.resources.ITargetResource;
import net.roboconf.messaging.api.MessagingConstants;


/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetResourceTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Manager manager;
	private TestManagerWrapper managerWrapper;
	private ITargetResource resource;


	@Before
	public void initializeDm() throws Exception {

		// Create the manager
		this.manager = new Manager();
		this.manager.setMessagingType(MessagingConstants.FACTORY_TEST);
		this.manager.setTargetResolver( new TestTargetResolver());
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
		this.manager.start();

		// Create the wrapper and complete configuration
		this.managerWrapper = new TestManagerWrapper( this.manager );
		this.managerWrapper.configureMessagingForTest();
		this.manager.reconfigure();

		// Register the REST resource
		this.resource = new TargetResource( this.manager );
	}


	@After
	public void stopDm() {
		if( this.manager != null )
			this.manager.stop();
	}


	@Test
	public void testCrudOperations() {

		Assert.assertEquals(
				Status.NOT_FOUND.getStatusCode(),
				this.resource.getTargetProperties( "whatever" ).getStatus());

		Response resp = this.resource.createOrUpdateTarget( "id: tid\nprop: ok\nhandler: h", null );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		String targetId = (String) resp.getEntity();
		Assert.assertEquals( "tid", targetId );

		resp = this.resource.createOrUpdateTarget( "id: t2\nok: ok\nhandler: h", null );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		String newTargetId = (String) resp.getEntity();
		Assert.assertEquals( "t2", newTargetId );

		resp = this.resource.getTargetProperties( targetId );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		StringWrapper props = (StringWrapper) resp.getEntity();
		Assert.assertEquals( "prop: ok\nhandler: h", props.toString());

		resp = this.resource.getTargetProperties( targetId );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		props = (StringWrapper) resp.getEntity();
		Assert.assertEquals( "prop: ok\nhandler: h", props.toString());

		this.resource.createOrUpdateTarget( "prop2: ko\nprop1: done\nhandler: my handler", targetId );
		resp = this.resource.getTargetProperties( targetId );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
		props = (StringWrapper) resp.getEntity();
		Assert.assertEquals( "prop2: ko\nprop1: done\nhandler: my handler", props.toString());

		this.resource.deleteTarget( targetId );
		Assert.assertEquals(
				Status.NOT_FOUND.getStatusCode(),
				this.resource.getTargetProperties( targetId ).getStatus());
	}


	@Test
	public void testCreateOrupdateTarget_updateInexistingTarget() {

		Response resp = this.resource.createOrUpdateTarget( "prop: ok\nhandler: my handler", "4" );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testDeleteTarget_inexistingTarget() throws Exception {

		Response resp = this.resource.deleteTarget( "oops" );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testDeleteTarget_targetIsUsed() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		String targetId = (String) this.resource.createOrUpdateTarget( "id: tid\nprop: ok\nhandler: h", null ).getEntity();
		this.managerWrapper.addManagedApplication( ma );

		this.resource.associateTarget( app.getName(), null, null, targetId, true );
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );

		Response resp = this.resource.deleteTarget( targetId );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testListTargets_all() throws Exception {

		Assert.assertEquals( 0, this.resource.listTargets( null, null ).size());
		this.resource.createOrUpdateTarget( "id: t1\nhandler: h", null ).getEntity();
		Assert.assertEquals( 1, this.resource.listTargets( null, null ).size());
		String t2 = (String) this.resource.createOrUpdateTarget( "id: t2\nhandler: h", null ).getEntity();
		Assert.assertEquals( 2, this.resource.listTargets( null, null ).size());
		this.resource.createOrUpdateTarget( "id: t3\nhandler: h", null ).getEntity();
		Assert.assertEquals( 3, this.resource.listTargets( null, null ).size());

		this.resource.deleteTarget( t2 );
		Assert.assertEquals( 2, this.resource.listTargets( null, null ).size());
	}


	@Test
	public void testHints_onTemplate() throws Exception {

		String t1 = (String) this.resource.createOrUpdateTarget( "id: t1\nprop: ok\nhandler: h", null ).getEntity();
		this.resource.createOrUpdateTarget( "id: t2\nprop: ok\nhandler: h", null ).getEntity();

		TestApplicationTemplate tpl1 = new TestApplicationTemplate();
		TestApplicationTemplate tpl2 = new TestApplicationTemplate();
		tpl2.name( "newTpl" );

		this.managerWrapper.getApplicationTemplates().put( tpl1, Boolean.TRUE );
		this.managerWrapper.getApplicationTemplates().put( tpl2, Boolean.TRUE );

		Assert.assertEquals( 2, this.resource.listTargets( tpl1.getName(), tpl1.getVersion()).size());
		Assert.assertEquals( 2, this.resource.listTargets( tpl2.getName(), tpl2.getVersion()).size());

		this.resource.updateHint( tpl1.getName(), tpl1.getVersion(), t1, true );
		Assert.assertEquals( 2, this.resource.listTargets( tpl1.getName(), tpl1.getVersion()).size());
		Assert.assertEquals( 1, this.resource.listTargets( tpl2.getName(), tpl2.getVersion()).size());

		this.resource.updateHint( tpl1.getName(), tpl1.getVersion(), t1, false );
		Assert.assertEquals( 2, this.resource.listTargets( tpl1.getName(), tpl1.getVersion()).size());
		Assert.assertEquals( 2, this.resource.listTargets( tpl2.getName(), tpl2.getVersion()).size());
	}


	@Test
	public void testHints_onApplication() throws Exception {

		this.resource.createOrUpdateTarget( "id: t1\nprop: ok\nhandler: h", null ).getEntity();
		String t2 = (String) this.resource.createOrUpdateTarget( "id: t2nprop: ok\nhandler: h", null ).getEntity();

		TestApplication app1 = new TestApplication();
		app1.setDirectory( this.folder.newFolder());
		TestApplication app2 = new TestApplication();
		app2.setDirectory( this.folder.newFolder());
		app2.name( "myApp2" );

		this.managerWrapper.addManagedApplication( new ManagedApplication( app1 ));
		this.managerWrapper.addManagedApplication( new ManagedApplication( app2 ));

		Assert.assertEquals( 2, this.resource.listTargets( app1.getName(), null ).size());
		Assert.assertEquals( 2, this.resource.listTargets( app2.getName(), null ).size());

		this.resource.updateHint( app2.getName(), null, t2, true );
		Assert.assertEquals( 1, this.resource.listTargets( app1.getName(), null ).size());
		Assert.assertEquals( 2, this.resource.listTargets( app2.getName(), null ).size());

		this.resource.updateHint( app2.getName(), null, t2, false );
		Assert.assertEquals( 2, this.resource.listTargets( app1.getName(), null ).size());
		Assert.assertEquals( 2, this.resource.listTargets( app2.getName(), null ).size());
	}


	@Test
	public void testHints_invalidApplication() throws Exception {

		this.resource.createOrUpdateTarget( "prop: ok", null );
		String t2 = (String) this.resource.createOrUpdateTarget( "prop: ok", null ).getEntity();

		TestApplication app1 = new TestApplication();
		app1.setDirectory( this.folder.newFolder());
		TestApplication app2 = new TestApplication();
		app2.setDirectory( this.folder.newFolder());
		app2.name( "myApp2" );

		this.managerWrapper.addManagedApplication( new ManagedApplication( app1 ));
		this.managerWrapper.addManagedApplication( new ManagedApplication( app2 ));

		Response resp = this.resource.updateHint( "invalid", null, t2, true );
		Assert.assertEquals( Status.BAD_REQUEST.getStatusCode(), resp.getStatus());

		resp = this.resource.updateHint( "invalid", null, t2, false );
		Assert.assertEquals( Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testAssociations_invalidApplication() throws Exception {

		String t2 = (String) this.resource.createOrUpdateTarget( "prop: ok", null ).getEntity();

		TestApplication app1 = new TestApplication();
		app1.setDirectory( this.folder.newFolder());
		TestApplication app2 = new TestApplication();
		app2.setDirectory( this.folder.newFolder());
		app2.name( "myApp2" );

		this.managerWrapper.addManagedApplication( new ManagedApplication( app1 ));
		this.managerWrapper.addManagedApplication( new ManagedApplication( app2 ));

		Response resp = this.resource.associateTarget( "invalid", null, null, t2, true );
		Assert.assertEquals( Status.BAD_REQUEST.getStatusCode(), resp.getStatus());

		resp = this.resource.associateTarget( "invalid", null, null, t2, false );
		Assert.assertEquals( Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testAssociations_onApplication_defaultTarget() throws Exception {

		String targetId = (String) this.resource.createOrUpdateTarget( "id: tid\nhandler: h\nprop: ok", null ).getEntity();
		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.addManagedApplication( ma );

		try {
			this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
			Assert.fail( "No target should have been found." );

		} catch( IOException e ) {
			// nothing
		}

		Response resp = this.resource.associateTarget( app.getName(), null, null, targetId, true );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYING, app.getMySqlVm().getStatus());
	}


	@Test
	public void testAssociations_onApplication_exactInstance() throws Exception {

		String targetId = (String) this.resource.createOrUpdateTarget( "id: tid\nhandler: h\nprop: ok", null ).getEntity();
		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.addManagedApplication( ma );

		// Initial check: no associated target
		try {
			this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
			Assert.fail( "No target should have been found." );

		} catch( IOException e ) {
			// nothing
		}

		// Associate one
		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		Response resp = this.resource.associateTarget( app.getName(), null, instancePath, targetId, true );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());

		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
		Assert.assertEquals( InstanceStatus.DEPLOYING, app.getMySqlVm().getStatus());

		// Dissociate it
		this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.NOT_DEPLOYED );
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, app.getMySqlVm().getStatus());

		resp = this.resource.associateTarget( app.getName(), null, instancePath, targetId, false );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());

		try {
			this.manager.instancesMngr().changeInstanceState( ma, app.getMySqlVm(), InstanceStatus.DEPLOYED_STARTED );
			Assert.fail( "No target should have been found." );

		} catch( IOException e ) {
			// nothing
		}
	}


	@Test
	public void testAssociations_instanceAlreadyDeployed() throws Exception {

		String targetId = (String) this.resource.createOrUpdateTarget( "prop: ok", null ).getEntity();
		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );
		this.managerWrapper.addManagedApplication( ma );

		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		Response resp = this.resource.associateTarget( app.getName(), null, instancePath, targetId, true );
		Assert.assertEquals( Status.FORBIDDEN.getStatusCode(), resp.getStatus());
	}


	@Test
	public void testFindTargetById() throws Exception {

		Response resp = this.resource.findTargetById( "2" );
		Assert.assertEquals( Status.NOT_FOUND.getStatusCode(), resp.getStatus());

		String t2 = (String) this.resource.createOrUpdateTarget( "id: tid\nhandler: h\ndescription: we do not care", null ).getEntity();
		resp = this.resource.findTargetById( t2 );
		Assert.assertEquals( Status.OK.getStatusCode(), resp.getStatus());

		TargetWrapperDescriptor twd = (TargetWrapperDescriptor) resp.getEntity();
		Assert.assertNotNull( twd );
		Assert.assertEquals( t2, twd.getId());
		Assert.assertEquals( "we do not care", twd.getDescription());
		Assert.assertNull( twd.getName());
	}


	@Test
	public void testFindUsageStatistics_inexistingTarget() throws Exception {

		List<TargetUsageItem> items = this.resource.findUsageStatistics( "4" );
		Assert.assertEquals( 0, items.size());
	}


	@Test
	public void testFindUsageStatistics() throws Exception {

		// Setup
		String t1 = (String) this.resource.createOrUpdateTarget( "id: t1\nhandler: h\nprop: ok", null ).getEntity();
		Assert.assertNotNull( t1 );

		String t2 = (String) this.resource.createOrUpdateTarget( "id: t2\nhandler: h\nprop: ok", null ).getEntity();
		Assert.assertNotNull( t2 );

		String t3 = (String) this.resource.createOrUpdateTarget( "id: t3\nhandler: h\nprop: ok", null ).getEntity();
		Assert.assertNotNull( t3 );

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		this.managerWrapper.addManagedApplication( new ManagedApplication( app ));

		this.resource.associateTarget( app.getName(), null, InstanceHelpers.computeInstancePath( app.getMySqlVm()), t1, true );
		this.resource.associateTarget( app.getName(), null, null, t2, true );

		// Checks
		List<TargetUsageItem> items = this.resource.findUsageStatistics( t1 );
		Assert.assertEquals( 1, items.size());

		TargetUsageItem item = items.get( 0 );
		Assert.assertEquals( app.getName(), item.getName());
		Assert.assertNull( item.getVersion());
		Assert.assertFalse( item.isUsing());
		Assert.assertTrue( item.isReferencing());

		items = this.resource.findUsageStatistics( t2 );
		Assert.assertEquals( 1, items.size());

		item = items.get( 0 );
		Assert.assertEquals( app.getName(), item.getName());
		Assert.assertNull( item.getVersion());
		Assert.assertFalse( item.isUsing());
		Assert.assertTrue( item.isReferencing());

		items = this.resource.findUsageStatistics( t3 );
		Assert.assertEquals( 0, items.size());

		// Mark one as used
		this.manager.targetsMngr().lockAndGetTarget( app, app.getTomcatVm());

		items = this.resource.findUsageStatistics( t1 );
		Assert.assertEquals( 1, items.size());

		item = items.get( 0 );
		Assert.assertEquals( app.getName(), item.getName());
		Assert.assertNull( item.getVersion());
		Assert.assertFalse( item.isUsing());
		Assert.assertTrue( item.isReferencing());

		items = this.resource.findUsageStatistics( t3 );
		Assert.assertEquals( 0, items.size());

		items = this.resource.findUsageStatistics( t2 );
		Assert.assertEquals( 1, items.size());

		item = items.get( 0 );
		Assert.assertEquals( app.getName(), item.getName());
		Assert.assertNull( item.getVersion());
		Assert.assertTrue( item.isReferencing());

		// The change is here!
		Assert.assertTrue( item.isUsing());

		// Release it
		this.manager.targetsMngr().unlockTarget( app, app.getTomcatVm());

		items = this.resource.findUsageStatistics( t1 );
		Assert.assertEquals( 1, items.size());

		item = items.get( 0 );
		Assert.assertEquals( app.getName(), item.getName());
		Assert.assertNull( item.getVersion());
		Assert.assertFalse( item.isUsing());
		Assert.assertTrue( item.isReferencing());

		items = this.resource.findUsageStatistics( t2 );
		Assert.assertEquals( 1, items.size());

		item = items.get( 0 );
		Assert.assertEquals( app.getName(), item.getName());
		Assert.assertNull( item.getVersion());
		Assert.assertFalse( item.isUsing());
		Assert.assertTrue( item.isReferencing());

		items = this.resource.findUsageStatistics( t3 );
		Assert.assertEquals( 0, items.size());

		// Remove the association for the named instance
		this.resource.associateTarget( app.getName(), null, InstanceHelpers.computeInstancePath( app.getMySqlVm()), null, false );

		items = this.resource.findUsageStatistics( t1 );
		Assert.assertEquals( 0, items.size());

		items = this.resource.findUsageStatistics( t2 );
		Assert.assertEquals( 1, items.size());

		item = items.get( 0 );
		Assert.assertEquals( app.getName(), item.getName());
		Assert.assertNull( item.getVersion());
		Assert.assertFalse( item.isUsing());
		Assert.assertTrue( item.isReferencing());

		items = this.resource.findUsageStatistics( t3 );
		Assert.assertEquals( 0, items.size());
	}


	@Test
	public void testLoadTargetArchive_ok() throws Exception {

		// Create a ZIP with valid properties
		Map<String,String> entryToContent = new HashMap<> ();
		entryToContent.put( "t1.properties", "id: tid-1\nhandler: h" );
		entryToContent.put( "t2.properties", "id: tid-2\nhandler: h\nname: my main target" );

		File targetFile = this.folder.newFile( "roboconf_targets.zip" );
		TestUtils.createZipFile( entryToContent, targetFile );
		Assert.assertTrue( targetFile.exists());

		// Preconditions
		Assert.assertEquals( 0, this.manager.targetsMngr().listAllTargets().size());

		// Upload it
		InputStream in = null;
		try {
			FormDataContentDisposition fd = FormDataContentDisposition
					.name( targetFile.getName())
					.fileName( targetFile.getName()).build();

			in = new FileInputStream( targetFile );
			Assert.assertEquals(
					Status.OK.getStatusCode(),
					this.resource.loadTargetArchive( in, fd ).getStatus());

		} finally {
			Utils.closeQuietly( in );
		}

		// Postconditions
		List<TargetWrapperDescriptor> targetIds = this.manager.targetsMngr().listAllTargets();
		Assert.assertEquals( 2, targetIds.size());
		Assert.assertEquals( "tid-1", targetIds.get( 0 ).getId());
		Assert.assertEquals( "h", targetIds.get( 0 ).getHandler());
		Assert.assertNull( targetIds.get( 0 ).getName());
		Assert.assertNull( targetIds.get( 0 ).getDescription());

		Assert.assertEquals( "tid-2", targetIds.get( 1 ).getId());
		Assert.assertEquals( "h", targetIds.get( 1 ).getHandler());
		Assert.assertEquals( "my main target", targetIds.get( 1 ).getName());
		Assert.assertNull( targetIds.get( 1 ).getDescription());
	}


	@Test
	public void testLoadTargetArchive_conflictingTarget_withRevert() throws Exception {

		// Create a ZIP with valid properties
		Map<String,String> entryToContent = new HashMap<> ();
		entryToContent.put( "t1.properties", "id: tid-1\nhandler: h" );
		entryToContent.put( "t2.properties", "id: tid-2\n\nhandler: h\nnname: my main target" );
		entryToContent.put( "t3.properties", "id: tid-3\nhandler: h\nname: my main target" );

		File targetFile = this.folder.newFile( "roboconf_targets.zip" );
		TestUtils.createZipFile( entryToContent, targetFile );
		Assert.assertTrue( targetFile.exists());

		// Preconditions
		Assert.assertNotNull( this.manager.targetsMngr().createTarget( "id: tid-2\nhandler: handler" ));
		Assert.assertEquals( 1, this.manager.targetsMngr().listAllTargets().size());

		// Upload it
		InputStream in = null;
		try {
			FormDataContentDisposition fd = FormDataContentDisposition
					.name( targetFile.getName())
					.fileName( targetFile.getName()).build();

			in = new FileInputStream( targetFile );
			Assert.assertEquals(
					Status.NOT_ACCEPTABLE.getStatusCode(),
					this.resource.loadTargetArchive( in, fd ).getStatus());

		} finally {
			Utils.closeQuietly( in );
		}

		// Postconditions
		List<TargetWrapperDescriptor> targetIds = this.manager.targetsMngr().listAllTargets();
		Assert.assertEquals( 1, targetIds.size());
	}


	@Test
	public void testLoadTargetArchive_invalidTarget() throws Exception {

		// Create a ZIP with valid properties
		Map<String,String> entryToContent = new HashMap<> ();
		entryToContent.put( "t1.properties", "id: tid-1\nhandler: h" );
		entryToContent.put( "t2.properties", "id: tid-2\n\nnname: my main target" );
		entryToContent.put( "t3.properties", "id: tid-3\nhandler: h\nname: my main target" );

		File targetFile = this.folder.newFile( "roboconf_targets.zip" );
		TestUtils.createZipFile( entryToContent, targetFile );
		Assert.assertTrue( targetFile.exists());

		// Preconditions
		Assert.assertEquals( 0, this.manager.targetsMngr().listAllTargets().size());

		// Upload it
		InputStream in = null;
		try {
			FormDataContentDisposition fd = FormDataContentDisposition
					.name( targetFile.getName())
					.fileName( targetFile.getName()).build();

			in = new FileInputStream( targetFile );
			Assert.assertEquals(
					Status.FORBIDDEN.getStatusCode(),
					this.resource.loadTargetArchive( in, fd ).getStatus());

		} finally {
			Utils.closeQuietly( in );
		}

		// Postconditions
		List<TargetWrapperDescriptor> targetIds = this.manager.targetsMngr().listAllTargets();
		Assert.assertEquals( 0, targetIds.size());
	}
}
