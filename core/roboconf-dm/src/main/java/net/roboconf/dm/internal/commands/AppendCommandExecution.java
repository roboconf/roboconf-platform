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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import net.roboconf.core.commands.AppendCommandInstruction;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
class AppendCommandExecution extends AbstractCommandExecution {

	private final AppendCommandInstruction instr;


	/**
	 * Constructor.
	 * @param instr
	 */
	public AppendCommandExecution( AppendCommandInstruction instr ) {
		this.instr = instr;
	}


	@Override
	public void execute() throws CommandException {

		OutputStreamWriter fw = null;
		try {
			File f = new File( this.instr.getFilePath());
			boolean append = f.exists() && f.length() > 0;

			fw = new OutputStreamWriter( new FileOutputStream( f, true ), StandardCharsets.UTF_8 );
			if( append )
				fw.append( "\n" );

			fw.append( this.instr.getContent());

		} catch( IOException e ) {
			throw new CommandException( e );

		} finally {
			Utils.closeQuietly( fw );
		}
	}
}
