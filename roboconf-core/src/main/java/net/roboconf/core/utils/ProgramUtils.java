/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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
 * FIXME: this class is still messy. Waiting for the execution result should be done in another thread.
 * Use a Future or a call-back mechanism.
 */
public final class ProgramUtils {

	/**
	 * Private empty constructor.
	 */
	private ProgramUtils() {
		// nothing
	}


	/**
	 * Executes a command on the VM and prints on the console its output
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
		if( environmentVars != null )
			env.putAll( environmentVars );

		Process process = pb.start();
		new Thread( new OutputRunnable( process, true, logger )).start();
		new Thread( new OutputRunnable( process, false, logger )).start();

		int exitValue = process.waitFor();
		if( exitValue != 0 ) {
			logger.severe( "Command execution returned a failure code. Code:" + exitValue );
			throw new IOException( "Process execution failed. Exit code: " + exitValue );
		}

		return exitValue;
	}


	/**
	 * Executes a command on the VM and prints on the console its output
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
				br = new BufferedReader( new InputStreamReader( is ));

				for( String line = br.readLine(); line != null; line = br.readLine())
					this.logger.info( prefix + line );

			} catch( IOException e ) {
				this.logger.severe( Utils.writeException( e ));

			} finally {
				try {
					if( br != null )
						br.close();

				} catch( IOException e ) {
					this.logger.warning( "Minor error while closing a reader. " + e.getMessage());
					this.logger.finest( Utils.writeException( e ));
				}
			}
		}
	}
}
