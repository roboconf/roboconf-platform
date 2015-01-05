/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.agent.monitoring.internal.file;

import java.io.File;

import junit.framework.Assert;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifAutonomic;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class FileHandlerTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private static final String EVENT_NAME = "whatever";
	private static final String APP_NAME = "app";
	private static final String ROOT_INSTANCE_NAME = "root";


	@Test
	public void testInexistingFile() throws Exception {

		File f = new File( this.folder.newFolder(), "inexisting-file" );
		Assert.assertFalse( f.exists());

		String content = f.getAbsolutePath();
		FileHandler handler = new FileHandler( EVENT_NAME, APP_NAME, ROOT_INSTANCE_NAME, content );
		Assert.assertEquals( content, handler.getFileLocation());
		Assert.assertFalse( handler.isDeleteIfExists());
		Assert.assertFalse( handler.isNotifyIfNotExists());
		Assert.assertNull( handler.process());
	}


	@Test
	public void testExistingFile_doNotDelete() throws Exception {

		File f = this.folder.newFile();
		Assert.assertTrue( f.exists());

		String content = f.getAbsolutePath();
		FileHandler handler = new FileHandler( EVENT_NAME, APP_NAME, ROOT_INSTANCE_NAME, content );
		Assert.assertEquals( content, handler.getFileLocation());
		Assert.assertFalse( handler.isDeleteIfExists());
		Assert.assertFalse( handler.isNotifyIfNotExists());

		MsgNotifAutonomic msg = handler.process();
		Assert.assertNotNull( msg );
		Assert.assertEquals( APP_NAME, msg.getApplicationName());
		Assert.assertEquals( EVENT_NAME, msg.getEventName());
		Assert.assertEquals( ROOT_INSTANCE_NAME, msg.getRootInstanceName());
		Assert.assertTrue( msg.getEventInfo().toLowerCase().contains( "checked" ));

		Assert.assertTrue( f.exists());
	}


	@Test
	public void testExistingFile_delete() throws Exception {

		File f = this.folder.newFile();
		Assert.assertTrue( f.exists());

		String content = FileHandler.DELETE_IF_EXISTS.toUpperCase() + " \t " + f.getAbsolutePath() + "\t\n";
		FileHandler handler = new FileHandler( EVENT_NAME, APP_NAME, ROOT_INSTANCE_NAME, content );
		Assert.assertEquals( f.getAbsolutePath(), handler.getFileLocation());
		Assert.assertTrue( handler.isDeleteIfExists());
		Assert.assertFalse( handler.isNotifyIfNotExists());

		MsgNotifAutonomic msg = handler.process();
		Assert.assertNotNull( msg );
		Assert.assertEquals( APP_NAME, msg.getApplicationName());
		Assert.assertEquals( EVENT_NAME, msg.getEventName());
		Assert.assertEquals( ROOT_INSTANCE_NAME, msg.getRootInstanceName());
		Assert.assertTrue( msg.getEventInfo().toLowerCase().contains( "deleted" ));

		Assert.assertFalse( f.exists());
	}


	@Test
	public void testExistingFolder_delete() throws Exception {

		File f = this.folder.newFolder();
		Assert.assertTrue( f.exists());

		String content = FileHandler.DELETE_IF_EXISTS.toUpperCase() + " \t " + f.getAbsolutePath() + "\t\n";
		FileHandler handler = new FileHandler( EVENT_NAME, APP_NAME, ROOT_INSTANCE_NAME, content );
		Assert.assertEquals( f.getAbsolutePath(), handler.getFileLocation());
		Assert.assertTrue( handler.isDeleteIfExists());
		Assert.assertFalse( handler.isNotifyIfNotExists());

		MsgNotifAutonomic msg = handler.process();
		Assert.assertNotNull( msg );
		Assert.assertEquals( APP_NAME, msg.getApplicationName());
		Assert.assertEquals( EVENT_NAME, msg.getEventName());
		Assert.assertEquals( ROOT_INSTANCE_NAME, msg.getRootInstanceName());
		Assert.assertTrue( msg.getEventInfo().toLowerCase().contains( "deleted" ));

		Assert.assertFalse( f.exists());
	}


	@Test
	public void testInvalidContent() throws Exception {

		String content = "it does not matter, since there are \n several lines \n here, no message will be produced";
		FileHandler handler = new FileHandler( EVENT_NAME, APP_NAME, ROOT_INSTANCE_NAME, content );
		Assert.assertNull( handler.getFileLocation());
		Assert.assertFalse( handler.isDeleteIfExists());
		Assert.assertFalse( handler.isNotifyIfNotExists());

		Assert.assertNull( handler.process());
	}


	@Test
	public void testNotifyIfNotExists_inexisting() throws Exception {

		File f = new File( this.folder.newFolder(), "inexisting.file" );
		Assert.assertFalse( f.exists());

		String content = FileHandler.NOTIFY_IF_NOT_EXISTS.toUpperCase() + " \t " + f.getAbsolutePath() + "\t\n";
		FileHandler handler = new FileHandler( EVENT_NAME, APP_NAME, ROOT_INSTANCE_NAME, content );
		Assert.assertEquals( f.getAbsolutePath(), handler.getFileLocation());
		Assert.assertFalse( handler.isDeleteIfExists());
		Assert.assertTrue( handler.isNotifyIfNotExists());

		MsgNotifAutonomic msg = handler.process();
		Assert.assertNotNull( msg );
		Assert.assertEquals( APP_NAME, msg.getApplicationName());
		Assert.assertEquals( EVENT_NAME, msg.getEventName());
		Assert.assertEquals( ROOT_INSTANCE_NAME, msg.getRootInstanceName());
		Assert.assertTrue( msg.getEventInfo().toLowerCase().contains( "does not exist" ));

		Assert.assertFalse( f.exists());
	}


	@Test
	public void testNotifyIfNotExists_existing() throws Exception {

		File f = this.folder.newFolder();
		Assert.assertTrue( f.exists());

		String content = FileHandler.NOTIFY_IF_NOT_EXISTS.toUpperCase() + " \t " + f.getAbsolutePath() + "\t\n";
		FileHandler handler = new FileHandler( EVENT_NAME, APP_NAME, ROOT_INSTANCE_NAME, content );
		Assert.assertEquals( f.getAbsolutePath(), handler.getFileLocation());
		Assert.assertFalse( handler.isDeleteIfExists());
		Assert.assertTrue( handler.isNotifyIfNotExists());

		Assert.assertNotNull( handler.process());
	}
}
