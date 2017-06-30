/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
	public static List<DockerCommand> dockerfileToCommandList( File dockerfile ) throws IOException {

		List<DockerCommand> result = new ArrayList<>();
		FileInputStream in = new FileInputStream( dockerfile );
		Logger logger = Logger.getLogger( DockerfileParser.class.getName());
		BufferedReader br = null;

		try {
			br = new BufferedReader( new InputStreamReader( in, StandardCharsets.UTF_8 ));
			String line;
			while((line = br.readLine()) != null) {

				DockerCommand cmd = DockerCommand.guess(line);
				if( cmd != null )
					result.add( cmd );
				else
					logger.fine("Ignoring unsupported Docker instruction: " + line );
			}

		} finally {
			Utils.closeQuietly( br );
			Utils.closeQuietly( in );
		}

		return result;
	}


	/**
	 * @author Amadou Diarra - UJF
	 */
	public static class DockerCommand {
		DockerCommandType type;
		String argument;


		/**
		 * @param line
		 * @return a Docker command, or null if none matched
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

		public DockerCommandType getType() {
			return this.type;
		}

		public String getArgument() {
			return this.argument;
		}
	}


	/**
	 * Contains Docker commands type.
	 */
	public enum DockerCommandType {
		RUN, COPY, ADD
	}
}
