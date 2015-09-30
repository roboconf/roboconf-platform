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
import java.util.logging.Logger;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestApplicationTemplate;
import net.roboconf.core.model.ApplicationDescriptor;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.api.IApplicationTemplateMngr;
import net.roboconf.dm.management.api.IConfigurationMngr;
import net.roboconf.dm.management.api.IMessagingMngr;
import net.roboconf.dm.management.api.INotificationMngr;
import net.roboconf.dm.management.api.ITargetsMngr;
import net.roboconf.dm.management.exceptions.AlreadyExistingException;
import net.roboconf.dm.management.exceptions.InvalidApplicationException;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;
import net.roboconf.messaging.api.client.IDmClient;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.messages.from_dm_to_agent.MsgCmdChangeBinding;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

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
	private File dmDirectory;


	@Before
	public void prepareTemplateManager() throws Exception {

		INotificationMngr notificationMngr = Mockito.mock( INotificationMngr.class );
		ITargetsMngr targetsMngr = Mockito.mock( ITargetsMngr.class );

		this.messagingMngr = Mockito.mock( IMessagingMngr.class );
		Mockito.when( this.messagingMngr.getMessagingClient()).thenReturn( Mockito.mock( IDmClient.class ));

		this.configurationMngr = Mockito.mock( IConfigurationMngr.class );
		this.applicationTemplateMngr = Mockito.mock( IApplicationTemplateMngr.class );
		this.mngr = new ApplicationMngrImpl( notificationMngr, this.configurationMngr, targetsMngr, this.messagingMngr );
		this.mngr.setApplicationTemplateMngr( this.applicationTemplateMngr );

		this.dmDirectory = this.folder.newFolder();
		Mockito.when( this.configurationMngr.getWorkingDirectory()).thenReturn( this.dmDirectory );
	}


	@Test
	public void testIsTemplateUsed() throws Exception {

		ApplicationTemplate tpl = new ApplicationTemplate( "lamp" );
		Assert.assertFalse( this.mngr.isTemplateUsed( tpl ));

		ManagedApplication ma = new ManagedApplication( new Application( "app", tpl ));
		TestManagerWrapper.getNameToManagedApplication( this.mngr ).put( "app", ma );
		Assert.assertTrue( this.mngr.isTemplateUsed( tpl ));

		ApplicationTemplate tpl2 = new ApplicationTemplate( "lamp" ).qualifier( "v2" );
		Assert.assertFalse( this.mngr.isTemplateUsed( tpl2 ));
	}


	@Test( expected = IOException.class )
	public void testCreateApplication_invalidApplicationName() throws Exception {
		this.mngr.createApplication( null, "desc", new ApplicationTemplate());
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testDeleteApplication_unauthorized() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		TestManagerWrapper.getNameToManagedApplication( this.mngr ).put( app.getName(), ma );
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.mngr.deleteApplication( ma );
	}


	@Test
	public void testDeleteApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma = new ManagedApplication( app );

		TestManagerWrapper.getNameToManagedApplication( this.mngr ).put( app.getName(), ma );
		Assert.assertEquals( 1, this.mngr.getManagedApplications().size());
		this.mngr.deleteApplication( ma );
		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());
	}


	@Test
	public void testCreateApplication_withTags() throws Exception {

		TestApplicationTemplate tpl = new TestApplicationTemplate();
		tpl.setDirectory( this.folder.newFolder());

		Mockito.verifyZeroInteractions( this.applicationTemplateMngr );
		Mockito.when( this.applicationTemplateMngr.findTemplate( tpl.getName(), tpl.getQualifier())).thenReturn( tpl );

		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());
		ManagedApplication ma = this.mngr.createApplication( "toto", "desc", tpl.getName(), tpl.getQualifier());
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, TestManagerWrapper.getNameToManagedApplication( this.mngr ).size());

		Assert.assertEquals( ma.getDirectory().getName(), ma.getName());
		Assert.assertEquals( "toto", ma.getName());

		File expected = new File( this.configurationMngr.getWorkingDirectory(), ConfigurationUtils.APPLICATIONS );
		Assert.assertEquals( expected, ma.getDirectory().getParentFile());
	}


	@Test( expected = AlreadyExistingException.class )
	public void testCreateApplication_conflict() throws Exception {

		TestApplicationTemplate tpl = new TestApplicationTemplate();
		tpl.setDirectory( this.folder.newFolder());

		Mockito.verifyZeroInteractions( this.applicationTemplateMngr );
		Mockito.when( this.applicationTemplateMngr.findTemplate( tpl.getName(), tpl.getQualifier())).thenReturn( tpl );

		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());
		ManagedApplication ma = this.mngr.createApplication( "toto", "desc", tpl.getName(), tpl.getQualifier());
		Assert.assertNotNull( ma );
		Assert.assertEquals( 1, TestManagerWrapper.getNameToManagedApplication( this.mngr ).size());

		this.mngr.createApplication( "toto", "desc", tpl );
	}


	@Test
	public void testUpdateApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());

		ManagedApplication ma = new ManagedApplication( app );
		TestManagerWrapper.getNameToManagedApplication( this.mngr ).put( app.getName(), ma );

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
		TestManagerWrapper.getNameToManagedApplication( this.mngr ).put( app.getName(), ma );

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

		ApplicationTemplate tpl = new ApplicationTemplate( "myTpl" ).qualifier( "v1" );
		tpl.setDirectory( this.folder.newFolder());

		Application app = new Application( "myApp", tpl );
		ApplicationDescriptor.save( descriptorFile, app );

		Mockito.when( this.applicationTemplateMngr.findTemplate( tpl.getName(), tpl.getQualifier())).thenReturn( tpl );

		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());
		this.mngr.restoreApplications();
		Assert.assertEquals( 1, this.mngr.getManagedApplications().size());

		ManagedApplication ma = this.mngr.getManagedApplications().iterator().next();
		Assert.assertEquals( app.getName(), ma.getName());
		Assert.assertEquals( tpl.getName(), app.getTemplate().getName());
		Assert.assertEquals( tpl.getQualifier(), app.getTemplate().getQualifier());
		Assert.assertEquals( dir.getParentFile(), ma.getDirectory());
	}


	@Test
	public void testRestoreApplications_withConflict() throws Exception {

		ApplicationTemplate tpl = new ApplicationTemplate( "myTpl" ).qualifier( "v1" );
		tpl.setDirectory( this.folder.newFolder());
		Application app = new Application( "myApp", tpl );

		// Copy the application descriptor twice
		for( int i=0; i<2; i++ ) {
			File dir = new File( this.dmDirectory, ConfigurationUtils.APPLICATIONS + "/myApp" + i + "/" + Constants.PROJECT_DIR_DESC );
			File descriptorFile = new File( dir, Constants.PROJECT_FILE_DESCRIPTOR );
			Assert.assertTrue( dir.mkdirs());

			ApplicationDescriptor.save( descriptorFile, app );
		}

		Mockito.when( this.applicationTemplateMngr.findTemplate( tpl.getName(), tpl.getQualifier())).thenReturn( tpl );

		Assert.assertEquals( 0, this.mngr.getManagedApplications().size());
		this.mngr.restoreApplications();
		Assert.assertEquals( 1, this.mngr.getManagedApplications().size());

		ManagedApplication ma = this.mngr.getManagedApplications().iterator().next();
		Assert.assertEquals( app.getName(), ma.getName());
		Assert.assertEquals( tpl.getName(), app.getTemplate().getName());
		Assert.assertEquals( tpl.getQualifier(), app.getTemplate().getQualifier());
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

		Application app = new Application( "myApp", new ApplicationTemplate( "not" ).qualifier( "valid" ));
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
		TestManagerWrapper.getNameToManagedApplication( this.mngr ).put( app.getName(), ma );

		this.mngr.bindApplication( ma, ma.getApplication().getTemplate().getName(), "invalid" );
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testBindApplication_invalidTemplate() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma1 = new ManagedApplication( app );
		TestManagerWrapper.getNameToManagedApplication( this.mngr ).put( ma1.getName(), ma1 );

		app = new TestApplication();
		app.getTemplate().setName( "tpl-other" );
		app.setName( "app-other" );

		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma2 = new ManagedApplication( app );
		TestManagerWrapper.getNameToManagedApplication( this.mngr ).put( ma2.getName(), ma2 );

		// ma1 and ma2 do not have the same template name
		this.mngr.bindApplication( ma1, ma1.getApplication().getTemplate().getName(), ma2.getName());
	}


	@Test
	public void testBindApplication_success() throws Exception {

		TestApplication app = new TestApplication();
		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma1 = new ManagedApplication( app );
		TestManagerWrapper.getNameToManagedApplication( this.mngr ).put( ma1.getName(), ma1 );

		app = new TestApplication();
		app.getTemplate().setName( "tpl-other" );
		app.setName( "app-other" );

		app.setDirectory( this.folder.newFolder());
		ManagedApplication ma2 = new ManagedApplication( app );
		TestManagerWrapper.getNameToManagedApplication( this.mngr ).put( ma2.getName(), ma2 );

		Mockito.verifyZeroInteractions( this.messagingMngr );
		this.mngr.bindApplication( ma1, ma2.getApplication().getTemplate().getName(), ma2.getName());

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
			Assert.assertEquals( ma2.getApplication().getTemplate().getName(), msg.getAppTempleName());
			Assert.assertEquals( ma2.getName(), msg.getAppName());
		}

		List<Instance> instances = arg1.getAllValues();
		Assert.assertTrue( instances.contains( app.getMySqlVm()));
		Assert.assertTrue( instances.contains( app.getTomcatVm()));
	}
}
