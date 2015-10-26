/**
 * Copyright 2013-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.utils.ProgramUtils;
import net.roboconf.core.utils.Utils;

/**
 * Program that parses a Dockerfile.
 * @author Amadou Diarra - Université Joseph Fourier
 */
public final class DockerfileParser {

	/**
	 * Empty constructor.
	 * @throws IOException
	 */
	private DockerfileParser() {
		// no thing
	}

	/**
	 * Parses a dockerfile to a list of commands.
	 * @throws IOException
	 * @param dockerfile a file
	 * @return a list of Docker commands
	 */
	public static List<DockerCommand> dockerfileToCommandList(File dockerfile) throws IOException {

		List<DockerCommand> result = new ArrayList<DockerCommand>();
		FileReader fr = new FileReader(dockerfile);
		Logger logger = Logger.getLogger( DockerfileParser.class.getName());
		BufferedReader br = null;

		try {
			br = new BufferedReader(fr);
			String line;;
			while((line = br.readLine()) != null) {

				DockerCommand cmd = DockerCommand.guess(line);
				if( cmd != null )
					result.add( cmd );
				else
					logger.fine("Ignoring unsupported Docker instruction: " + line );
			}

		} finally {
			Utils.closeQuietly(br);
		}
		return result;
	}

	/**
	 * Executes a docker command on the VM and prints on the console its output.
	 *
	 * @param logger
	 *            a logger (not null)
	 * @param workingDir
	 *            a file indicating the working directory
	 * @param environmentVars
	 *            a map containing environment variables (can be null)
	 * @param dc
	 *            a Docker command to execute (not null, not empty)
	 * @throws IOException
	 *             if a new process could not be created
	 * @throws InterruptedException
	 *             if the new process encountered a process
	 */

	public static int executeDockerCommand(final Logger logger, File workingDir,
			final Map<String, String> environmentVars, DockerCommand dc) throws IOException, InterruptedException {
		int r = 0;
		switch (dc.type) {
		case RUN:
			r = ProgramUtils.executeCommand(logger, Utils.splitNicely(dc.argument, " "), workingDir, environmentVars);
			break;
		case COPY:
			copyFiles(dc.argument);
			break;
		case ADD:
			// TODO
		default:
		}
		return r;
	}

	/**
	 * Copies files using a Docker command passed in parameter as a string.
	 *
	 * @param command
	 *            a string (not null)
	 * @throws IOException
	 *             if the file could not be created
	 */
	public static void copyFiles(String command) throws IOException {
		List<String> c1 = Utils.splitNicely(command, " ");
		File f1 = new File(c1.get(0));
		File f2 = new File(c1.get(1));
		if (f2.isDirectory()) {
			f2 = new File(f2.getAbsolutePath() + "/" + f1.getName());
		}
		Utils.copyStream(f1, f2);
	}

	/**
	 * @author Amadou Diarra - UJF
	 */
	static class DockerCommand {
		DockerCommandType type;
		String argument;


		/**
		 * @param line
		 * @return
		 */
		public static DockerCommand guess( String line ) {

			DockerCommand result = null;
			for( DockerCommandType type : DockerCommandType.values()) {
				if( line.startsWith( type.toString())) {
					result = new DockerCommand();
					result.type = type;
					result.argument = line.substring(type.toString().length()).trim();
					break;
				}
			}

			return result;
		}
	}

	/**
	 * Contains Docker commands type.
	 */
	enum DockerCommandType {
		RUN, COPY, ADD
	}
}
