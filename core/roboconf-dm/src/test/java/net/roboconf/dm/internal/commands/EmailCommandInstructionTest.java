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

package net.roboconf.dm.internal.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.mail.Address;
import javax.mail.Message;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.commands.CommandsParser;
import net.roboconf.core.commands.EmailCommandInstruction;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.dm.internal.api.impl.PreferencesMngrImpl;
import net.roboconf.dm.internal.commands.EmailCommandExecution.MailAuthenticator;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IPreferencesMngr;

/**
 * @author Vincent Zurczak - Linagora
 */
public class EmailCommandInstructionTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private Manager manager;


	@Before
	public void initialize() throws Exception {
		this.manager = new Manager();
		this.manager.setPreferencesMngr( new PreferencesMngrImpl());
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
	}


	@Test
	public void testMailAuthenticatorTest() {
		MailAuthenticator ma = new MailAuthenticator( "user", "pwd" );
		Assert.assertNotNull( ma.getPasswordAuthentication());
	}


	@Test
	public void testMessagesToSend() throws Exception {

		File f = TestUtils.findTestFile( "/commands/email-commands.txt" );
		CommandsParser parser = new CommandsParser( new TestApplication(), f );

		Assert.assertEquals( 0, parser.getParsingErrors().size());
		Assert.assertEquals( 2, parser.getInstructions().size());

		this.manager.preferencesMngr().save( "mail.from", "me@test.fr" );

		// First message
		Assert.assertEquals( EmailCommandInstruction.class, parser.getInstructions().get( 0 ).getClass());
		EmailCommandInstruction instr = (EmailCommandInstruction) parser.getInstructions().get( 0 );
		EmailCommandExecution executor = new EmailCommandExecution( instr, this.manager );
		Message message = executor.getMessageToSend();

		Assert.assertNotNull( message );
		Assert.assertEquals( "Alert!", message.getSubject());
		Assert.assertEquals( "This is an alert.", message.getContent());

		List<String> recipients = new ArrayList<> ();
		for( Address ad : message.getAllRecipients())
			recipients.add( ad.toString());

		Assert.assertEquals( 1, recipients.size());
		Assert.assertTrue( recipients.contains( "toto@company.net" ));

		// Second message

		// Just for code coverage (default value is "true")
		this.manager.preferencesMngr().save( IPreferencesMngr.JAVAX_MAIL_SMTP_AUTH, "false" );
		// End

		Assert.assertEquals( EmailCommandInstruction.class, parser.getInstructions().get( 1 ).getClass());
		instr = (EmailCommandInstruction) parser.getInstructions().get( 1 );
		executor = new EmailCommandExecution( instr, this.manager );
		message = executor.getMessageToSend();

		Assert.assertNotNull( message );
		Assert.assertEquals( "Roboconf event", message.getSubject());
		Assert.assertEquals( "This message is splitted over several lines!", message.getContent());

		recipients.clear();
		for( Address ad : message.getAllRecipients())
			recipients.add( ad.toString());

		Assert.assertEquals( 2, recipients.size());
		Assert.assertTrue( recipients.contains( "p1@c.com" ));
		Assert.assertTrue( recipients.contains( "p3@c.com" ));
	}
}
