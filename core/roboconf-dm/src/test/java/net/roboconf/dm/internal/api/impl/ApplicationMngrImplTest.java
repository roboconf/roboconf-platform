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

package net.roboconf.dm.internal.api.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import net.roboconf.core.Constants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.RoboconfError;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestApplicationTemplate;
import net.roboconf.core.model.ApplicationDescriptor;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.api.IRandomMngr;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.api.IApplicationTemplateMngr;
import net.roboconf.dm.management.api.IAutonomicMngr;
import net.roboconf.dm.management.api.IConfigurationMngr;
import net.roboconf.dm.management.api.IMessagingMngr;
import net.roboconf.dm.management.api.INotificationMngr;
import net.roboconf.dm.management.api.ITargetsMngr;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.messaging.api.business.IDmClient;
import net.roboconf.messaging.api.business.ListenerCommand;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeBinding;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ApplicationMngrImplTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private ApplicationMngrImpl mngr;
	private IMessagingMngr messagingMngr;
	private IConfigurationMngr configurationMngr;
	private IApplicationTemplateMngr applicationTemplateMngr;
	private IAutonomicMngr autonomicMngr;

	private File dmDirectory;
	private IDmClient dmClientMock;



	@Before
	public void prepareTemplateManager() throws Exception {

		INotificationMngr notificationMngr = Mockito.mock( INotificationMngr.class );
		ITargetsMngr targetsMngr = Mockito.mock( ITargetsMngr.class );
		IRandomMngr randomMngr = Mockito.mock( IRandomMngr.class );

		this.messagingMngr = Mockito.mock( IMessagingMngr.class );
		this.dmClientMock = Mockito.mock( IDmClient.class );
		Mockito.when( this.messagingMngr.getMessagingClient()).thenReturn( this.dmClientMock );

		this.autonomicMngr = Mockito.mock( IAutonomicMngr.class );
		this.configurationMngr = Mockito.mock( IConfigurationMngr.class );
		this.applicationTemplateMngr = Mockito.mock( IApplicationTemplateMngr.class );
		this.mngr = new ApplicationMngrImpl(
				notificationMngr, this.configurationMngr,
				targetsMngr, this.messagingMngr,
				randomMngr, this.autonomicMngr );

		this.mngr.setApplicationTemplateMngr( this.applicationTemplateMngr );

		this.dmDirectory = this.folder.newFolder();
		Mockito.when( this.configurationMngr.getWorkingDirectory()).thenReturn( this.dmDirectory );
	}


	@Test
	public void testIsTemplateUsed() throws Exception {

		ApplicationTemplate tpl = new ApplicationTemplate( "lamp" );
		Assert.assertFalse( this.mngr.isTemplateUsed( tpl ));

		ManagedApplication ma = new ManagedApplication( new Application( "app", tpl ));
		ma.getApplication().setDirectory( this.folder.newFolder());
		TestManagerWrapper.addManagedApplication( this.mngr, ma );
		Assert.assertTrue( this.mngr.isTemplateUsed( tpl ));

		ApplicationTemplate tpl2 = new ApplicationTemplate( "lamp" ).version( "v2" );
		Assert.assertFalse( this.mngr.isTemplateUsed( tpl2 ));
	}


	@Test( expected = IOException.class )
	public void testCreateApplication_invalidApplicationName_1() throws Exception {
		this.mngr.createApplication( null, "desc", new ApplicationTemplate());
	}


	@Test( expected = IOException.class )
	public void testCreateApplication_invalidApplicationName_2() throws Exception {
		this.mngr.createApplication( "", "desc", new ApplicationTemplate());
	}


	@Test( expected = IOException.class )
	public void testCreateApplication_invalidApplicationName_3() throws Exception {
		this.mngr.createApplication( "app#", "desc", new ApplicationTemplate());
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testDeleteApplication_unauthorized() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		TestManagerWrapper.addManagedApplication( this.mngr, ma );
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.mngr.deleteApplication( ma );
	}


	@Test
	public void testDeleteApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		TestManagerWrapper.addManagedApplication( this.mngr, ma );
		Assert.assertEquals( 1, this.mngr.getManagedApplications().size());
		this.mngr.deleteApplication( ma );
		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());

		Mockito.verify( this.autonomicMngr, Mockito.times( 1 )).unloadApplicationRules( ma.getApplication());
	}


	@Test
	public void testCreateApplication_success() throws Exception {

		TestApplicationTemplate tpl = new TestApplicationTemplate();
		tpl.setDirectory( this.folder.newFolder());

		Mockito.verifyZeroInteractions( this.applicationTemplateMngr );
		Mockito.when( this.applicationTemplateMngr.findTemplate( tpl.getName(), tpl.getVersion())).thenReturn( tpl );

		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());
		ManagedApplication ma = this.mngr.createApplication( "toto", "desc", tpl.getName(), tpl.getVersion());
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, TestManagerWrapper.getNameToManagedApplication( this.mngr ).size());

		Assert.assertEquals( ma.getDirectory().getName(), ma.getName());
		Assert.assertEquals( "toto", ma.getName());

		File expected = new File( this.configurationMngr.getWorkingDirectory(), ConfigurationUtils.APPLICATIONS );
		Assert.assertEquals( expected, ma.getDirectory().getParentFile());

		Mockito.verify( this.autonomicMngr, Mockito.times( 1 )).loadApplicationRules( ma.getApplication());
	}


	@Test
	public void testCreateApplication_success_withSpecialName() throws Exception {

		TestApplicationTemplate tpl = new TestApplicationTemplate();
		tpl.setDirectory( this.folder.newFolder());

		Mockito.verifyZeroInteractions( this.applicationTemplateMngr );
		Mockito.when( this.applicationTemplateMngr.findTemplate( tpl.getName(), tpl.getVersion())).thenReturn( tpl );

		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());
		ManagedApplication ma = this.mngr.createApplication( "ça débute", "desc", tpl.getName(), tpl.getVersion());
		Assert.assertNotNull( ma );
		Assert.assertEquals( "ca debute", ma.getName());
		Assert.assertEquals( "ça débute", ma.getApplication().getDisplayName());
		Assert.assertEquals( 1, TestManagerWrapper.getNameToManagedApplication( this.mngr ).size());
		Assert.assertEquals( ma.getDirectory().getName(), ma.getName());

		// Important
		Assert.assertNull( this.mngr.findManagedApplicationByName( "ça débute" ));
		Assert.assertNull( this.mngr.findApplicationByName( "ça débute" ));

		Assert.assertNotNull( this.mngr.findManagedApplicationByName( "ca debute" ));
		Assert.assertNotNull( this.mngr.findApplicationByName( "ca debute" ));
		// Important

		File expected = new File( this.configurationMngr.getWorkingDirectory(), ConfigurationUtils.APPLICATIONS );
		Assert.assertEquals( expected, ma.getDirectory().getParentFile());

		Mockito.verify( this.autonomicMngr, Mockito.times( 1 )).loadApplicationRules( ma.getApplication());
	}


	@Test( expected = AlreadyExistingException.class )
	public void testCreateApplication_conflict() throws Exception {

		TestApplicationTemplate tpl = new TestApplicationTemplate();
		tpl.setDirectory( this.folder.newFolder());

		Mockito.verifyZeroInteractions( this.applicationTemplateMngr );
		Mockito.when( this.applicationTemplateMngr.findTemplate( tpl.getName(), tpl.getVersion())).thenReturn( tpl );

		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());
		ManagedApplication ma = this.mngr.createApplication( "toto", "desc", tpl.getName(), tpl.getVersion());
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, TestManagerWrapper.getNameToManagedApplication( this.mngr ).size());

		this.mngr.createApplication( "toto", "desc", tpl );
	}


	@Test
	public void testUpdateApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());

		ManagedApplication ma = new ManagedApplication( app );
		TestManagerWrapper.addManagedApplication( this.mngr, ma );

		String newDesc = "new description";
		Assert.assertEquals( 0, app.getDirectory().listFiles().length );
		Assert.assertFalse( newDesc.equals( app.getDescription()));

		this.mngr.updateApplication( ma, newDesc );
		Assert.assertEquals( newDesc, app.getDescription());
		Assert.assertEquals( 1, app.getDirectory().listFiles().length );
	}


	@Test( expected = IOException.class )
	public void testUpdateApplication_saveFailure() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFile());

		ManagedApplication ma = new ManagedApplication( app );
		TestManagerWrapper.addManagedApplication( this.mngr, ma );

		String newDesc = "new description";
		this.mngr.updateApplication( ma, newDesc );
	}


	@Test( expected = InvalidApplicationException.class )
	public void testCreateApplication_invalidTemplate() throws Exception {

		this.mngr.createApplication( "toto", "desc", "whatever", null );
	}


	@Test
	public void testRestoreApplications_noTemplate_noDirectory() throws Exception {

		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());
		this.mngr.restoreApplications();
		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());
	}


	@Test
	public void testRestoreApplications_noTemplate_withTplDirectory() throws Exception {

		Assert.assertTrue( new File( this.dmDirectory, ConfigurationUtils.APPLICATIONS ).mkdir());
		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());
		this.mngr.restoreApplications();
		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());
	}


	@Test
	public void testRestoreApplications_withApp() throws Exception {

		File dir = new File( this.dmDirectory, ConfigurationUtils.APPLICATIONS + "/myApp/" + Constants.PROJECT_DIR_DESC );
		File descriptorFile = new File( dir, Constants.PROJECT_FILE_DESCRIPTOR );
		Assert.assertTrue( dir.mkdirs());

		ApplicationTemplate tpl = new ApplicationTemplate( "myTpl" ).version( "v1" );
		tpl.setDirectory( this.folder.newFolder());

		Application app = new Application( "myApp", tpl );
		ApplicationDescriptor.save( descriptorFile, app );

		Mockito.when( this.applicationTemplateMngr.findTemplate( tpl.getName(), tpl.getVersion())).thenReturn( tpl );

		Mockito.verifyZeroInteractions( this.dmClientMock );
		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());

		this.mngr.restoreApplications();

		Assert.assertEquals( 1, this.mngr.getManagedApplications().size());
		Mockito.verify( this.dmClientMock, Mockito.times( 1 )).listenToAgentMessages( app, ListenerCommand.START );

		ManagedApplication ma = this.mngr.getManagedApplications().iterator().next();
		Assert.assertEquals( app.getName(), ma.getName());
		Assert.assertEquals( tpl.getName(), app.getTemplate().getName());
		Assert.assertEquals( tpl.getVersion(), app.getTemplate().getVersion());
		Assert.assertEquals( dir.getParentFile(), ma.getDirectory());

		Mockito.verify( this.autonomicMngr, Mockito.times( 1 )).loadApplicationRules( ma.getApplication());
	}


	@Test
	public void testRestoreApplications_withApp_withSpecialName() throws Exception {

		File dir = new File( this.dmDirectory, ConfigurationUtils.APPLICATIONS + "/ca debute bien/" + Constants.PROJECT_DIR_DESC );
		File descriptorFile = new File( dir, Constants.PROJECT_FILE_DESCRIPTOR );
		Assert.assertTrue( dir.mkdirs());

		ApplicationTemplate tpl = new ApplicationTemplate( "myTpl" ).version( "v1" );
		tpl.setDirectory( this.folder.newFolder());

		Application app = new Application( "ça débute bien", tpl );
		ApplicationDescriptor.save( descriptorFile, app );

		Mockito.when( this.applicationTemplateMngr.findTemplate( tpl.getName(), tpl.getVersion())).thenReturn( tpl );

		Mockito.verifyZeroInteractions( this.dmClientMock );
		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());

		this.mngr.restoreApplications();

		Assert.assertEquals( 1, this.mngr.getManagedApplications().size());
		Mockito.verify( this.dmClientMock, Mockito.times( 1 )).listenToAgentMessages( app, ListenerCommand.START );

		ManagedApplication ma = this.mngr.getManagedApplications().iterator().next();
		Assert.assertEquals( "ca debute bien", ma.getName());
		Assert.assertEquals( "ça débute bien", ma.getApplication().getDisplayName());
		Assert.assertEquals( tpl.getName(), app.getTemplate().getName());
		Assert.assertEquals( tpl.getVersion(), app.getTemplate().getVersion());
		Assert.assertEquals( dir.getParentFile(), ma.getDirectory());

		Mockito.verify( this.autonomicMngr, Mockito.times( 1 )).loadApplicationRules( ma.getApplication());
	}


	@Test
	public void testRestoreApplications_withConflict() throws Exception {

		ApplicationTemplate tpl = new ApplicationTemplate( "myTpl" ).version( "v1" );
		tpl.setDirectory( this.folder.newFolder());
		Application app = new Application( "myApp", tpl );

		// Copy the application descriptor twice
		for( int i=0; i<2; i++ ) {
			File dir = new File( this.dmDirectory, ConfigurationUtils.APPLICATIONS + "/myApp" + i + "/" + Constants.PROJECT_DIR_DESC );
			File descriptorFile = new File( dir, Constants.PROJECT_FILE_DESCRIPTOR );
			Assert.assertTrue( dir.mkdirs());

			ApplicationDescriptor.save( descriptorFile, app );
		}

		Mockito.when( this.applicationTemplateMngr.findTemplate( tpl.getName(), tpl.getVersion())).thenReturn( tpl );

		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());
		this.mngr.restoreApplications();
		Assert.assertEquals( 1, this.mngr.getManagedApplications().size());

		ManagedApplication ma = this.mngr.getManagedApplications().iterator().next();
		Assert.assertEquals( app.getName(), ma.getName());
		Assert.assertEquals( tpl.getName(), app.getTemplate().getName());
		Assert.assertEquals( tpl.getVersion(), app.getTemplate().getVersion());

		Mockito.verify( this.autonomicMngr, Mockito.times( 1 )).loadApplicationRules( ma.getApplication());
	}


	@Test
	public void testRestoreApplications_withError() throws Exception {

		File tplDir = new File( this.dmDirectory, ConfigurationUtils.APPLICATIONS + "/Demo" );
		Assert.assertTrue( tplDir.mkdirs());

		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());
		this.mngr.restoreApplications();
		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());
	}


	@Test
	public void testRestoreApplications_invalidApplicationTemplate() throws Exception {

		File dir = new File( this.dmDirectory, ConfigurationUtils.APPLICATIONS + "/myApp/" + Constants.PROJECT_DIR_DESC );
		File descriptorFile = new File( dir, Constants.PROJECT_FILE_DESCRIPTOR );
		Assert.assertTrue( dir.mkdirs());

		Application app = new Application( "myApp", new ApplicationTemplate( "not" ).version( "valid" ));
		ApplicationDescriptor.save( descriptorFile, app );

		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());
		this.mngr.restoreApplications();
		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());
	}


	@Test
	public void testRestoreApplications_invalidApplicationDescriptor() throws Exception {

		File dir = new File( this.dmDirectory, ConfigurationUtils.APPLICATIONS + "/myApp/" + Constants.PROJECT_DIR_DESC );
		File descriptorFile = new File( dir, Constants.PROJECT_FILE_DESCRIPTOR );
		Assert.assertTrue( dir.mkdirs());

		Utils.writeStringInto( "invalid", descriptorFile );

		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());
		this.mngr.restoreApplications();
		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());
	}


	@Test
	public void testCheckErrors_noError() throws Exception {

		ApplicationMngrImpl.checkErrors( new ArrayList<RoboconfError>( 0 ), Logger.getLogger( getClass().getName()));
		// No exception
	}


	@Test( expected = InvalidApplicationException.class )
	public void testCheckErrors_withError() throws Exception {

		List<RoboconfError> errors = new ArrayList<> ();
		errors.add( new RoboconfError( ErrorCode.CO_CYCLE_IN_COMPONENTS_INHERITANCE ));
		ApplicationMngrImpl.checkErrors( errors, Logger.getLogger( getClass().getName()));
	}


	@Test
	public void testCheckErrors_withWarning() throws Exception {

		List<RoboconfError> errors = new ArrayList<> ();
		errors.add( new RoboconfError( ErrorCode.RM_MISSING_APPLICATION_DSL_ID ));
		ApplicationMngrImpl.checkErrors( errors, Logger.getLogger( getClass().getName()));
		// No exception
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testBindApplication_invalidApplication() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());

		ManagedApplication ma = new ManagedApplication( app );
		TestManagerWrapper.addManagedApplication( this.mngr, ma );

		this.mngr.bindOrUnbindApplication( ma, ma.getApplication().getTemplate().getName(), "invalid", true );
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testBindApplication_invalidTemplate() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma1 = new ManagedApplication( app );
		TestManagerWrapper.addManagedApplication( this.mngr, ma1 );

		app = new TestApplication();
		app.getTemplate().setName( "tpl-other" );
		app.setName( "app-other" );

		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma2 = new ManagedApplication( app );
		TestManagerWrapper.addManagedApplication( this.mngr, ma2 );

		// ma1 and ma2 do not have the same template name
		this.mngr.bindOrUnbindApplication( ma1, ma1.getApplication().getTemplate().getName(), ma2.getName(), true );
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testBindApplication_invalidTemplatePrefix() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		app.getTemplate().setExternalExportsPrefix( "prefix" );

		ManagedApplication ma1 = new ManagedApplication( app );
		TestManagerWrapper.addManagedApplication( this.mngr, ma1 );

		app = new TestApplication();
		app.setDirectory( this.folder.newFolder());

		ManagedApplication ma2 = new ManagedApplication( app );
		TestManagerWrapper.addManagedApplication( this.mngr, ma2 );

		// ma1 and ma2 do not have the same template name
		this.mngr.bindOrUnbindApplication( ma1, ma1.getApplication().getTemplate().getName(), ma2.getName(), true );
	}


	@Test
	public void testBindApplication_success() throws Exception {

		TestApplication app1 = new TestApplication();
		app1.setDirectory( this.folder.newFolder());
		app1.getTemplate().setExternalExportsPrefix( "prefix1" );

		ManagedApplication ma1 = new ManagedApplication( app1 );
		TestManagerWrapper.addManagedApplication( this.mngr, ma1 );

		TestApplication app2 = new TestApplication();
		app2.getTemplate().setName( "tpl-other" );
		app2.getTemplate().setExternalExportsPrefix( "tpl-other-prefix" );
		app2.getTemplate().setExternalExportsPrefix( "prefix2" );
		app2.setName( "app-other" );

		// Rename root instances in the second application.
		// This is to make sure messages are sent to the right instances in the right application.
		app2.getMySqlVm().setName( "other-mysql" );
		app2.getTomcatVm().setName( "other-tomcat" );

		app2.setDirectory( this.folder.newFolder());
		ManagedApplication ma2 = new ManagedApplication( app2 );
		TestManagerWrapper.addManagedApplication( this.mngr, ma2 );

		Assert.assertEquals( 0, ma1.getApplication().getApplicationBindings().size());
		String eep = ma2.getApplication().getTemplate().getExternalExportsPrefix();

		Mockito.verifyZeroInteractions( this.messagingMngr );
		this.mngr.bindOrUnbindApplication( ma1, eep, ma2.getName(), true );

		Assert.assertEquals( 1, ma1.getApplication().getApplicationBindings().size());
		Assert.assertTrue( ma1.getApplication().getApplicationBindings().get( eep ).contains( ma2.getName()));

		// Verify the messaging
		ArgumentCaptor<ManagedApplication> arg0 = ArgumentCaptor.forClass( ManagedApplication.class );
		ArgumentCaptor<Instance> arg1 = ArgumentCaptor.forClass( Instance.class );
		ArgumentCaptor<Message> arg2 = ArgumentCaptor.forClass( Message.class );
		Mockito.verify( this.messagingMngr, Mockito.times( 2 )).sendMessageSafely( arg0.capture(), arg1.capture(), arg2.capture());

		for( ManagedApplication s : arg0.getAllValues()) {
			Assert.assertEquals( ma1, s );
		}

		for( Message m : arg2.getAllValues()) {
			Assert.assertEquals( MsgCmdChangeBinding.class, m.getClass());

			MsgCmdChangeBinding msg = (MsgCmdChangeBinding) m;
			Assert.assertEquals( ma2.getApplication().getTemplate().getExternalExportsPrefix(), msg.getExternalExportsPrefix());
			Assert.assertNotNull( msg.getAppNames());
			Assert.assertEquals( 1, msg.getAppNames().size());
			Assert.assertTrue( msg.getAppNames().contains( ma2.getName()));
		}

		// Messages must be sent to ma1!
		List<Instance> instances = arg1.getAllValues();
		Assert.assertTrue( instances.contains( app1.getMySqlVm()));
		Assert.assertTrue( instances.contains( app1.getTomcatVm()));
	}


	@Test
	public void testUnbindApplication_success_withNotification() throws Exception {

		TestApplication app1 = new TestApplication();
		app1.setDirectory( this.folder.newFolder());
		app1.getTemplate().setExternalExportsPrefix( "prefix1" );

		ManagedApplication ma1 = new ManagedApplication( app1 );
		TestManagerWrapper.addManagedApplication( this.mngr, ma1 );

		TestApplication app2 = new TestApplication();
		app2.getTemplate().setName( "tpl-other" );
		app2.getTemplate().setExternalExportsPrefix( "tpl-other-prefix" );
		app2.getTemplate().setExternalExportsPrefix( "prefix2" );
		app2.setName( "app-other" );

		// Rename root instances in the second application.
		// This is to make sure messages are sent to the right instances in the right application.
		app2.getMySqlVm().setName( "other-mysql" );
		app2.getTomcatVm().setName( "other-tomcat" );

		app2.setDirectory( this.folder.newFolder());
		ManagedApplication ma2 = new ManagedApplication( app2 );
		TestManagerWrapper.addManagedApplication( this.mngr, ma2 );

		Assert.assertEquals( 0, ma1.getApplication().getApplicationBindings().size());
		String eep = ma2.getApplication().getTemplate().getExternalExportsPrefix();

		Mockito.verifyZeroInteractions( this.messagingMngr );
		this.mngr.bindOrUnbindApplication( ma1, eep, ma2.getName(), true );
		Assert.assertEquals( 1, ma1.getApplication().getApplicationBindings().size());
		Assert.assertTrue( ma1.getApplication().getApplicationBindings().get( eep ).contains( ma2.getName()));

		Mockito.reset( this.messagingMngr );

		// Unbind
		this.mngr.bindOrUnbindApplication( ma1, eep, ma2.getName(), false );
		Assert.assertEquals( 0, ma1.getApplication().getApplicationBindings().size());

		// Verify sent messages
		ArgumentCaptor<ManagedApplication> arg0 = ArgumentCaptor.forClass( ManagedApplication.class );
		ArgumentCaptor<Instance> arg1 = ArgumentCaptor.forClass( Instance.class );
		ArgumentCaptor<Message> arg2 = ArgumentCaptor.forClass( Message.class );
		Mockito.verify( this.messagingMngr, Mockito.times( 2 )).sendMessageSafely( arg0.capture(), arg1.capture(), arg2.capture());

		for( ManagedApplication s : arg0.getAllValues()) {
			Assert.assertEquals( ma1, s );
		}

		for( Message m : arg2.getAllValues()) {
			Assert.assertEquals( MsgCmdChangeBinding.class, m.getClass());

			MsgCmdChangeBinding msg = (MsgCmdChangeBinding) m;
			Assert.assertEquals( ma2.getApplication().getTemplate().getExternalExportsPrefix(), msg.getExternalExportsPrefix());
			Assert.assertNull( msg.getAppNames());
		}

		// Messages must be sent to ma1!
		List<Instance> instances = arg1.getAllValues();
		Assert.assertTrue( instances.contains( app1.getMySqlVm()));
		Assert.assertTrue( instances.contains( app1.getTomcatVm()));
	}


	@Test
	public void testUnbindApplication_success_withoutNotification() throws Exception {

		TestApplication app1 = new TestApplication();
		app1.setDirectory( this.folder.newFolder());
		app1.getTemplate().setExternalExportsPrefix( "prefix1" );

		ManagedApplication ma1 = new ManagedApplication( app1 );
		TestManagerWrapper.addManagedApplication( this.mngr, ma1 );

		TestApplication app2 = new TestApplication();
		app2.getTemplate().setName( "tpl-other" );
		app2.getTemplate().setExternalExportsPrefix( "tpl-other-prefix" );
		app2.getTemplate().setExternalExportsPrefix( "prefix2" );
		app2.setName( "app-other" );

		// Rename root instances in the second application.
		// This is to make sure messages are sent to the right instances in the right application.
		app2.getMySqlVm().setName( "other-mysql" );
		app2.getTomcatVm().setName( "other-tomcat" );

		app2.setDirectory( this.folder.newFolder());
		ManagedApplication ma2 = new ManagedApplication( app2 );
		TestManagerWrapper.addManagedApplication( this.mngr, ma2 );

		Assert.assertEquals( 0, ma1.getApplication().getApplicationBindings().size());
		String eep = ma2.getApplication().getTemplate().getExternalExportsPrefix();

		// Unbind an application that is not bound
		this.mngr.bindOrUnbindApplication( ma1, eep, ma2.getName(), false );

		// Nothing changed
		Assert.assertEquals( 0, ma1.getApplication().getApplicationBindings().size());

		// No message should have been sent when we tried to unbind
		Mockito.verifyZeroInteractions( this.messagingMngr );
	}


	@Test
	public void testReplaceApplicationBindings_success() throws Exception {

		TestApplication app1 = new TestApplication();
		app1.setDirectory( this.folder.newFolder());
		app1.getTemplate().setExternalExportsPrefix( "prefix1" );

		ManagedApplication ma1 = new ManagedApplication( app1 );
		TestManagerWrapper.addManagedApplication( this.mngr, ma1 );

		TestApplication app2 = new TestApplication();
		app2.setDirectory( this.folder.newFolder());
		app2.getTemplate().setName( "tpl-other" );
		app2.getTemplate().setExternalExportsPrefix( "tpl-other-prefix" );
		app2.getTemplate().setExternalExportsPrefix( "prefix2" );
		app2.setName( "app-other" );

		// Rename root instances in the second application.
		// This is to make sure messages are sent to the right instances in the right application.
		app2.getMySqlVm().setName( "other-mysql" );
		app2.getTomcatVm().setName( "other-tomcat" );

		ManagedApplication ma2 = new ManagedApplication( app2 );
		TestManagerWrapper.addManagedApplication( this.mngr, ma2 );

		Assert.assertEquals( 0, ma1.getApplication().getApplicationBindings().size());
		String eep = ma2.getApplication().getTemplate().getExternalExportsPrefix();

		Mockito.verifyZeroInteractions( this.messagingMngr );
		this.mngr.replaceApplicationBindings( ma1, eep, new HashSet<>( Arrays.asList( ma2.getName())));

		Assert.assertEquals( 1, ma1.getApplication().getApplicationBindings().size());
		Assert.assertTrue( ma1.getApplication().getApplicationBindings().get( eep ).contains( ma2.getName()));

		// Verify the messaging
		ArgumentCaptor<ManagedApplication> arg0 = ArgumentCaptor.forClass( ManagedApplication.class );
		ArgumentCaptor<Instance> arg1 = ArgumentCaptor.forClass( Instance.class );
		ArgumentCaptor<Message> arg2 = ArgumentCaptor.forClass( Message.class );
		Mockito.verify( this.messagingMngr, Mockito.times( 2 )).sendMessageSafely( arg0.capture(), arg1.capture(), arg2.capture());

		for( ManagedApplication s : arg0.getAllValues()) {
			Assert.assertEquals( ma1, s );
		}

		for( Message m : arg2.getAllValues()) {
			Assert.assertEquals( MsgCmdChangeBinding.class, m.getClass());

			MsgCmdChangeBinding msg = (MsgCmdChangeBinding) m;
			Assert.assertEquals( ma2.getApplication().getTemplate().getExternalExportsPrefix(), msg.getExternalExportsPrefix());
			Assert.assertNotNull( msg.getAppNames());
			Assert.assertEquals( 1, msg.getAppNames().size());
			Assert.assertTrue( msg.getAppNames().contains( ma2.getName()));
		}

		// Messages must be sent to ma1!
		List<Instance> instances = arg1.getAllValues();
		Assert.assertTrue( instances.contains( app1.getMySqlVm()));
		Assert.assertTrue( instances.contains( app1.getTomcatVm()));

		// Set the same bindings: no message should be sent
		Mockito.reset( this.messagingMngr );
		this.mngr.replaceApplicationBindings( ma1, eep, new HashSet<>( Arrays.asList( ma2.getName())));
		Mockito.verifyZeroInteractions( this.messagingMngr );

		// Change the binding to an empty list => there should be messages
		this.mngr.replaceApplicationBindings( ma1, eep, new HashSet<String>( 0 ));
		Assert.assertEquals( 0, ma1.getApplication().getApplicationBindings().size());

		// Verify the messaging
		arg0 = ArgumentCaptor.forClass( ManagedApplication.class );
		arg1 = ArgumentCaptor.forClass( Instance.class );
		arg2 = ArgumentCaptor.forClass( Message.class );
		Mockito.verify( this.messagingMngr, Mockito.times( 2 )).sendMessageSafely( arg0.capture(), arg1.capture(), arg2.capture());

		for( ManagedApplication s : arg0.getAllValues()) {
			Assert.assertEquals( ma1, s );
		}

		for( Message m : arg2.getAllValues()) {
			Assert.assertEquals( MsgCmdChangeBinding.class, m.getClass());

			MsgCmdChangeBinding msg = (MsgCmdChangeBinding) m;
			Assert.assertEquals( ma2.getApplication().getTemplate().getExternalExportsPrefix(), msg.getExternalExportsPrefix());
			Assert.assertNull( msg.getAppNames());
		}
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testReplaceApplicationBindings_failure_inexistingApplication() throws Exception {

		TestApplication app1 = new TestApplication();
		app1.setDirectory( this.folder.newFolder());
		app1.getTemplate().setExternalExportsPrefix( "prefix1" );

		ManagedApplication ma1 = new ManagedApplication( app1 );
		TestManagerWrapper.addManagedApplication( this.mngr, ma1 );

		Assert.assertEquals( 0, ma1.getApplication().getApplicationBindings().size());
		Mockito.verifyZeroInteractions( this.messagingMngr );
		this.mngr.replaceApplicationBindings( ma1, "prefix1", new HashSet<>( Arrays.asList( "inexisting" )));
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testReplaceApplicationBindings_failure_invalidTemplate() throws Exception {

		TestApplication app1 = new TestApplication();
		app1.setDirectory( this.folder.newFolder());
		app1.getTemplate().setExternalExportsPrefix( "prefix1" );

		ManagedApplication ma1 = new ManagedApplication( app1 );
		TestManagerWrapper.addManagedApplication( this.mngr, ma1 );

		TestApplication app2 = new TestApplication();
		app2.setDirectory( this.folder.newFolder());

		ManagedApplication ma2 = new ManagedApplication( app2 );
		TestManagerWrapper.addManagedApplication( this.mngr, ma2 );

		Assert.assertEquals( 0, ma1.getApplication().getApplicationBindings().size());
		Mockito.verifyZeroInteractions( this.messagingMngr );
		this.mngr.replaceApplicationBindings( ma1, "prefix1", new HashSet<>( Arrays.asList( ma2.getName())));
	}
}
