/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.autonomic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.environment.messaging.DmMessageProcessor;
import net.roboconf.dm.internal.test.TestManagerWrapper;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.messaging.api.MessagingConstants;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RuleBasedEventHandlerTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private ManagedApplication ma;
	private TestApplication app;

	private DmMessageProcessor processor;
	private Manager manager;
	private TestManagerWrapper managerWrapper;
	private RuleBasedEventHandler handler;


	@Before
	public void resetManager() throws Exception {

		// Create the manager
		File dir = this.folder.newFolder();

		this.manager = new Manager();
		this.manager.setTargetResolver( new TestTargetResolver());
		this.manager.setMessagingType(MessagingConstants.TEST_FACTORY_TYPE);
		this.manager.configurationMngr().setWorkingDirectory( dir );
		this.manager.start();

		// Create the wrapper and complete configuration
		this.managerWrapper = new TestManagerWrapper( this.manager );
		this.managerWrapper.configureMessagingForTest();
		this.manager.reconfigure();

		// Reset the processor
		if( this.processor != null )
			this.processor.stopProcessor();

		this.processor = (DmMessageProcessor) this.managerWrapper.getMessagingClient().getMessageProcessor();
		this.managerWrapper.getNameToManagedApplication().clear();

		// Create an application
		this.app = new TestApplication();
		File appDirectory = ConfigurationUtils.findApplicationDirectory( this.app.getName(), dir );
		Assert.assertTrue( appDirectory.mkdirs());
		this.app.setDirectory( appDirectory );

		this.ma = new ManagedApplication( this.app );
		this.managerWrapper.getNameToManagedApplication().put( this.app.getName(), this.ma );

		// Create the handler
		this.handler = new RuleBasedEventHandler( this.manager );
	}


	@After
	public void stopManager() {
		this.manager.stop();
	}


	@Test
	public void testDeleteInstance_noAutonomicInstance() {
		int instanceCount = InstanceHelpers.getAllInstances( this.ma.getApplication()).size();
		this.handler.deleteInstances( this.ma, this.app.getMySql().getComponent().getName());
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.ma.getApplication()).size());
	}


	@Test
	public void testDeleteInstance_invalidComponentName() {
		int instanceCount = InstanceHelpers.getAllInstances( this.ma.getApplication()).size();
		this.handler.deleteInstances( this.ma, "oops" );
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.ma.getApplication()).size());
	}


	@Test
	public void testCreateInstance_invalidComponentPath() {
		int instanceCount = InstanceHelpers.getAllInstances( this.ma.getApplication()).size();
		this.handler.createInstances( this.ma, "/whatever" );
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.ma.getApplication()).size());
	}


	@Test
	public void testReplicateInstance_invalidInstanceName() {
		int instanceCount = InstanceHelpers.getAllInstances( this.ma.getApplication()).size();
		this.handler.replicateInstance( this.ma, "/whatever" );
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.ma.getApplication()).size());
	}


	@Test
	public void testCreateAndDelete() {

		// Create...
		int instanceCount = InstanceHelpers.getAllInstances( this.ma.getApplication()).size();
		StringBuilder sb = new StringBuilder();
		sb.append( this.app.getTomcatVm().getComponent().getName());
		sb.append( "/" );
		sb.append( this.app.getTomcat().getComponent().getName());
		sb.append( "/" );
		sb.append( this.app.getWar().getComponent().getName());

		this.handler.createInstances( this.ma, sb.toString());
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.ma.getApplication()).size());

		Collection<Instance> rootInstances = new ArrayList<> ();
		rootInstances.addAll( this.ma.getApplication().getRootInstances());
		rootInstances.remove( this.app.getMySqlVm());
		rootInstances.remove( this.app.getTomcatVm());

		Assert.assertEquals( 1, rootInstances.size());
		Instance instance = rootInstances.iterator().next();
		Assert.assertTrue( instance.getName().startsWith( this.app.getTomcatVm().getComponent().getName() + "_" ));
		Assert.assertEquals( this.app.getTomcatVm().getComponent(), instance.getComponent());

		Assert.assertEquals( 1, instance.getChildren().size());
		instance = instance.getChildren().iterator().next();
		Assert.assertEquals( this.app.getTomcat().getComponent().getName().toLowerCase(), instance.getName());
		Assert.assertEquals( this.app.getTomcat().getComponent(), instance.getComponent());

		Assert.assertEquals( 1, instance.getChildren().size());
		instance = instance.getChildren().iterator().next();
		Assert.assertEquals( this.app.getWar().getComponent().getName().toLowerCase(), instance.getName());
		Assert.assertEquals( this.app.getWar().getComponent(), instance.getComponent());
		Assert.assertEquals( 0, instance.getChildren().size());

		// Create another time
		this.handler.createInstances( this.ma, sb.toString());
		Assert.assertEquals( instanceCount + 6, InstanceHelpers.getAllInstances( this.ma.getApplication()).size());

		// Delete once
		this.handler.deleteInstances( this.ma, this.app.getWar().getComponent().getName());
		Assert.assertEquals( instanceCount + 3, InstanceHelpers.getAllInstances( this.ma.getApplication()).size());

		// Delete again
		this.handler.deleteInstances( this.ma, this.app.getWar().getComponent().getName());
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.ma.getApplication()).size());

		// Delete again => nothing
		this.handler.deleteInstances( this.ma, this.app.getWar().getComponent().getName());
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.ma.getApplication()).size());
	}


	@Test
	public void testReplicateAndDelete() throws Exception {

		// Create...
		int instanceCount = InstanceHelpers.getAllInstances( this.ma.getApplication()).size();
		StringBuilder sb = new StringBuilder();
		sb.append( this.app.getTomcatVm().getComponent().getName());
		sb.append( "/" );
		sb.append( this.app.getTomcat().getComponent().getName());
		sb.append( "/" );
		sb.append( this.app.getWar().getComponent().getName());

		// Set a default target for the application
		String targetId = this.manager.targetsMngr().createTarget( "" );
		this.manager.targetsMngr().associateTargetWithScopedInstance( targetId, this.app, null );
		//

		this.handler.replicateInstance( this.ma, this.app.getMySqlVm().getName());
		Assert.assertEquals( instanceCount + 2, InstanceHelpers.getAllInstances( this.ma.getApplication()).size());

		Collection<Instance> rootInstances = new ArrayList<> ();
		rootInstances.addAll( this.ma.getApplication().getRootInstances());
		rootInstances.remove( this.app.getMySqlVm());
		rootInstances.remove( this.app.getTomcatVm());

		Assert.assertEquals( 1, rootInstances.size());
		Instance instance = rootInstances.iterator().next();
		Assert.assertTrue( instance.getName().startsWith( this.app.getMySqlVm().getComponent().getName() + "_" ));
		Assert.assertEquals( this.app.getMySqlVm().getComponent(), instance.getComponent());

		Assert.assertEquals( 1, instance.getChildren().size());
		instance = instance.getChildren().iterator().next();
		Assert.assertEquals( this.app.getMySql().getName().toLowerCase(), instance.getName());
		Assert.assertEquals( this.app.getMySql().getComponent(), instance.getComponent());

		Assert.assertEquals( 0, instance.getChildren().size());

		// Create another time
		this.handler.replicateInstance( this.ma, this.app.getMySqlVm().getName());
		Assert.assertEquals( instanceCount + 4, InstanceHelpers.getAllInstances( this.ma.getApplication()).size());

		// Delete once
		this.handler.deleteInstances( this.ma, this.app.getMySql().getComponent().getName());
		Assert.assertEquals( instanceCount + 2, InstanceHelpers.getAllInstances( this.ma.getApplication()).size());

		// Delete again
		this.handler.deleteInstances( this.ma, this.app.getMySql().getComponent().getName());
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.ma.getApplication()).size());

		// Delete again => nothing
		this.handler.deleteInstances( this.ma, this.app.getMySql().getComponent().getName());
		Assert.assertEquals( instanceCount, InstanceHelpers.getAllInstances( this.ma.getApplication()).size());
	}


	@Test( expected = IOException.class )
	public void testEmail_noProperties() throws Exception {

		this.handler.sendEmail( this.ma, "hello world!" );
	}


	@Test( expected = IOException.class )
	public void testEmail_missingMailTo() throws Exception {

		File propFile = new File(
				this.ma.getDirectory(),
				Constants.PROJECT_DIR_AUTONOMIC + "/" + Constants.FILE_RULES + ".properties" );

		Assert.assertTrue( propFile.getParentFile().mkdirs());
		Assert.assertTrue( propFile.createNewFile());

		this.handler.sendEmail( this.ma, "hello world!" );
	}


	@Test
	public void testEmail_withMailTo() throws Exception {

		File propFile = new File(
				this.ma.getDirectory(),
				Constants.PROJECT_DIR_AUTONOMIC + "/" + Constants.FILE_RULES + ".properties" );

		Assert.assertTrue( propFile.getParentFile().mkdirs());
		Properties props = new Properties();
		props.setProperty( "mail.to", "me@roboconf.net" );
		props.setProperty( "mail.from", "me-again@roboconf.net" );
		Utils.writePropertiesFile( props, propFile );

		// No mail server is configured, there will be error logs
		this.handler.sendEmail( this.ma, "hello world!" );
	}


	@Test
	public void testEmail_withMailTo_andCustomSubject() throws Exception {

		File propFile = new File(
				this.ma.getDirectory(),
				Constants.PROJECT_DIR_AUTONOMIC + "/" + Constants.FILE_RULES + ".properties" );

		Assert.assertTrue( propFile.getParentFile().mkdirs());
		Properties props = new Properties();
		props.setProperty( "mail.to", "me@roboconf.net" );
		props.setProperty( "mail.from", "me-again@roboconf.net" );
		Utils.writePropertiesFile( props, propFile );

		// No mail server is configured, there will be error logs
		this.handler.sendEmail( this.ma, "Subject: yo\nhello world!" );
	}


	@Test
	public void testEmail_withMailTo_andSubjectOnly() throws Exception {

		File propFile = new File(
				this.ma.getDirectory(),
				Constants.PROJECT_DIR_AUTONOMIC + "/" + Constants.FILE_RULES + ".properties" );

		Assert.assertTrue( propFile.getParentFile().mkdirs());
		Properties props = new Properties();
		props.setProperty( "mail.to", "me@roboconf.net" );
		props.setProperty( "mail.smtp.auth", "True" );
		props.setProperty( "mail.from", "me-again@roboconf.net" );
		Utils.writePropertiesFile( props, propFile );

		// No mail server is configured, there will be error logs
		this.handler.sendEmail( this.ma, "Subject: yo" );
	}
}
