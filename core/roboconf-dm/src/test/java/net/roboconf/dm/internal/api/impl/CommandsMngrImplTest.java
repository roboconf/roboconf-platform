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

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.utils.Utils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Amadou Diarra - Université Joseph Fourier
 */
public class CommandsMngrImplTest{

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Application a1;
	private CommandsMngrImpl cmdMngr;


	@Before
	public void createMockObject() throws IOException {
		this.a1 = new TestApplication();
		this.cmdMngr = new CommandsMngrImpl();
		Assert.assertEquals( "", this.cmdMngr.getCommandInstructions( this.a1, ""));
	}


	@Test
	public void commandsTest() throws IOException {

		this.a1.directory( this.folder.newFolder());
		File f1 = this.folder.newFile();
		Utils.writeStringInto("Bonjour le monde cruel", f1);

		this.cmdMngr.createOrUpdateCommand(this.a1,"toto","This is a command");
		Assert.assertEquals("This is a command", this.cmdMngr.getCommandInstructions(this.a1, "toto"));
		this.cmdMngr.createCommand(this.a1,"tata",f1);
		Assert.assertEquals("Bonjour le monde cruel", this.cmdMngr.getCommandInstructions(this.a1, "tata"));

		this.cmdMngr.createOrUpdateCommand( this.a1, "toto", "Good command");
		Assert.assertEquals( "Good command", this.cmdMngr.getCommandInstructions(this.a1, "toto"));
		this.cmdMngr.deleteCommand( this.a1, "tata");
		Assert.assertEquals( "", this.cmdMngr.getCommandInstructions(this.a1, "tata"));
	}
}
