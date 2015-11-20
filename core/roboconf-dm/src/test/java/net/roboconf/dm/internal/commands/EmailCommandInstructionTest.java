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

package net.roboconf.dm.internal.commands;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.ErrorCode;
import net.roboconf.core.RoboconfError;
import net.roboconf.dm.internal.commands.EmailCommandInstruction.MailAuthenticator;
import net.roboconf.dm.management.Manager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
		this.manager.configurationMngr().setWorkingDirectory( this.folder.newFolder());
	}


	@Test
	public void testMailAuthenticatorTest() {
		MailAuthenticator ma = new MailAuthenticator( "user", "pwd" );
		Assert.assertNotNull( ma.getPasswordAuthentication());
	}


	@Test
	public void testValidate() {

		Map<String,ErrorCode> instructionToError = new HashMap<> ();
		instructionToError.put( "email toto@company.net with this message", null );
		instructionToError.put( "email toto1@company.net, toto2@company.net with this message", null );
		instructionToError.put( "email with this message", ErrorCode.EXEC_CMD_EMAIL_NO_RECIPIENTS );
		instructionToError.put( "email toto@company.net with ", ErrorCode.EXEC_CMD_EMAIL_NO_MESSAGE );

		for( Map.Entry<String,ErrorCode> entry : instructionToError.entrySet()) {

			EmailCommandInstruction instr = new EmailCommandInstruction( this.manager, entry.getKey());
			RoboconfError error = instr.validate();
			ErrorCode value = error == null ? null : error.getErrorCode();

			Assert.assertEquals( entry.getKey(), entry.getValue(), value );
		}
	}
}
