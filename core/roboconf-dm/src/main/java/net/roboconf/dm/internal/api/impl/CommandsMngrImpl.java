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

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.api.ICommandsMngr;

/**
 * @author Amadou Diarra - Université Joseph Fourier
 */
public class CommandsMngrImpl implements ICommandsMngr{

	@Override
	public void createOrUpdateCommand( Application app, String commandName, String commandText ) throws IOException {

		File cmdDir = new File( app.getDirectory(),ConfigurationUtils.COMMANDS_DIR );
		Utils.createDirectory(cmdDir);

		File cmd = new File(cmdDir,commandName+ConfigurationUtils.COMMANDS_SUFFIX);
		Utils.writeStringInto(commandText, cmd);
	}


	@Override
	public void createCommand( Application app, String commandName, File commandFile ) throws IOException {

		File cmdDir = new File( app.getDirectory(), ConfigurationUtils.COMMANDS_DIR );
		Utils.createDirectory(cmdDir);

		File cmd = new File(cmdDir,commandName+ConfigurationUtils.COMMANDS_SUFFIX);
		Utils.copyStream(commandFile, cmd);
	}


	@Override
	public void deleteCommand( Application app, String commandName ) throws IOException {

		File cmdDir = new File( app.getDirectory(),ConfigurationUtils.COMMANDS_DIR );
		File cmdDel = new File(cmdDir,commandName+ConfigurationUtils.COMMANDS_SUFFIX);
		Utils.deleteFilesRecursively(cmdDel);
	}


	@Override
	public String getCommandInstructions( Application app, String commandName ) throws IOException {

		File cmdDir = new File( app.getDirectory(),ConfigurationUtils.COMMANDS_DIR );
		File cmd = new File(cmdDir,commandName+ConfigurationUtils.COMMANDS_SUFFIX);

		String result = "";
		if( cmd.exists())
			result = Utils.readFileContent(cmd);

		return result;
	}


	@Override
	public boolean validate( String commandText ){
		return false;
	}


	@Override
	public void execute( Application app, String commandName ) throws IOException {

	}
}
