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

package net.roboconf.core.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Noël - LIG
 * @author Pierre-Yves Gibello - Linagora
 * @author Vincent Zurczak - Linagora
 */
public final class ProgramUtils {

	/**
	 * Private empty constructor.
	 */
	private ProgramUtils() {
		// nothing
	}


	/**
	 * Executes a command on the VM and prints on the console its output.
	 * @param command a command to execute (not null, not empty)
	 * @param environmentVars a map containing environment variables (can be null)
	 * @param logger a logger (not null)
	 * @throws IOException if a new process could not be created
	 * @throws InterruptedException if the new process encountered a process
	 */
	public static int executeCommand(
			final Logger logger,
			final String[] command,
			final Map<String,String> environmentVars )
	throws IOException, InterruptedException {

		logger.fine( "Executing command: " + Arrays.toString( command ));

		ProcessBuilder pb = new ProcessBuilder( command );
		Map<String,String> env = pb.environment();
		if( environmentVars != null && env != null ) {
			// No putAll() here: null key or value would cause NPE
			// (see ProcessBuilder.environment() javadoc).
			for( Map.Entry<String,String> entry : environmentVars.entrySet()) {
				if( entry.getKey() != null && entry.getValue() != null )
					env.put( entry.getKey(), entry.getValue());
			}
		}

		Process process = pb.start();
		new Thread( new OutputRunnable( process, true, logger )).start();
		new Thread( new OutputRunnable( process, false, logger )).start();

		int exitValue = process.waitFor();
		if( exitValue != 0 )
			logger.severe( "Command execution returned a failure code. Code:" + exitValue );

		return exitValue;
	}


	/**
	 * Executes a command on the VM and prints on the console its output.
	 * @param command a command to execute (not null, not empty)
	 * @param environmentVars a map containing environment variables (can be null)
	 * @param logger a logger (not null)
	 * @throws IOException if a new process could not be created
	 * @throws InterruptedException if the new process encountered a process
	 */
	public static int executeCommand(
			final Logger logger,
			final List<String> command,
			final Map<String,String> environmentVars )
	throws IOException, InterruptedException {

		return executeCommand( logger, command.toArray( new String[ 0 ]), environmentVars );
	}


	/**
	 * @author Noël - LIG
	 */
	private static class OutputRunnable implements Runnable {
		private final Process process;
		private final boolean errorLevel;
		private final Logger logger;

		public OutputRunnable( Process process, boolean errorLevel, Logger logger ) {
			this.process = process;
			this.errorLevel = errorLevel;
			this.logger = logger;
		}

		@Override
		public void run() {

			final String prefix = this.errorLevel ? "-- ERROR --" : "";
			BufferedReader br = null;
			try {
				InputStream is = this.errorLevel ? this.process.getErrorStream() : this.process.getInputStream();
				br = new BufferedReader( new InputStreamReader( is, "UTF-8" ));

				for( String line = br.readLine(); line != null; line = br.readLine())
					this.logger.info( prefix + line );

			} catch( IOException e ) {
				this.logger.severe( Utils.writeException( e ));

			} finally {
				Utils.closeQuietly( br );
			}
		}
	}
}
