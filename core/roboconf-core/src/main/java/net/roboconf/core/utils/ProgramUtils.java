/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
	 * Executes a command on the VM and retrieves all the result.
	 * <p>
	 * This includes the process's exit value, its normal output as well
	 * as the error flow.
	 * </p>
	 * @param logger a logger (not null)
	 * @param command a command to execute (not null, not empty)
	 * @param workingDir the working directory for the command
	 * @param environmentVars a map containing environment variables (can be null)
	 * @param applicationName the roboconf application name (null if not specified)
	 * @param scopedInstancePath the roboconf scoped instance path (null if not specified)
	 * @throws IOException if a new process could not be created
	 * @throws InterruptedException if the new process encountered a process
	 */
	public static ExecutionResult executeCommandWithResult(
			final Logger logger,
			final String[] command,
			final File workingDir,
			final Map<String,String> environmentVars,
			final String applicationName,
			final String scopedInstancePath)
	throws IOException, InterruptedException {

		logger.fine( "Executing command: " + Arrays.toString( command ));

		// Setup
		ProcessBuilder pb = new ProcessBuilder( command );
		if( workingDir != null )
			pb.directory(workingDir);

		Map<String,String> env = pb.environment();
		if( environmentVars != null && env != null ) {
			// No putAll() here: null key or value would cause NPE
			// (see ProcessBuilder.environment() javadoc).
			for( Map.Entry<String,String> entry : environmentVars.entrySet()) {
				if( entry.getKey() != null && entry.getValue() != null )
					env.put( entry.getKey(), entry.getValue());
			}
		}

		// Prepare the result
		StringBuilder normalOutput = new StringBuilder();
		StringBuilder errorOutput = new StringBuilder();
		int exitValue = -1;

		// Execute
		Process process = pb.start();

		// Store process in ThreadLocal, so it can be cancelled later (eg. if blocked)
		logger.fine("Storing process [" + applicationName + "] [" + scopedInstancePath + "]");
		ProcessStore.setProcess(applicationName, scopedInstancePath, process);

		try {
			new Thread( new OutputRunnable( process, true, errorOutput, logger )).start();
			new Thread( new OutputRunnable( process, false, normalOutput, logger )).start();

			exitValue = process.waitFor();
			if( exitValue != 0 )
				logger.warning( "Command execution returned a non-zero code. Code:" + exitValue );

		} finally {
			ProcessStore.clearProcess(applicationName, scopedInstancePath);
		}

		return new ExecutionResult(
				normalOutput.toString().trim(),
				errorOutput.toString().trim(),
				exitValue );
	}


	/**
	 * Executes a command on the VM and logs the output.
	 * @param logger a logger (not null)
	 * @param command a command to execute (not null, not empty)
	 * @param workingDir the working directory for the command
	 * @param environmentVars a map containing environment variables (can be null)
	 * @param applicationName the roboconf application name (null if not specified)
	 * @param scopedInstancePath the roboconf scoped instance path (null if not specified)
	 * @throws IOException if a new process could not be created
	 * @throws InterruptedException if the new process encountered a process
	 */
	public static int executeCommand(
			final Logger logger,
			final String[] command,
			final File workingDir,
			final Map<String,String> environmentVars,
			final String applicationName,
			final String scopedInstancePath)
	throws IOException, InterruptedException {

		ExecutionResult result = executeCommandWithResult( logger, command, workingDir, environmentVars, applicationName, scopedInstancePath);
		if( ! Utils.isEmptyOrWhitespaces( result.getNormalOutput()))
			logger.fine( result.getNormalOutput());

		if( ! Utils.isEmptyOrWhitespaces( result.getErrorOutput()))
			logger.warning( result.getErrorOutput());

		return result.getExitValue();
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
			final File workingDir,
			final Map<String,String> environmentVars,
			final String applicationName,
			final String scopedInstanceName)
	throws IOException, InterruptedException {

		return executeCommand( logger, command.toArray( new String[ 0 ]), workingDir, environmentVars, applicationName, scopedInstanceName);
	}


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static class ExecutionResult {

		private final String normalOutput, errorOutput;
		private final int exitValue;

		/**
		 * Constructor.
		 * @param normalOutput
		 * @param errorOutput
		 * @param exitValue
		 */
		public ExecutionResult( String normalOutput, String errorOutput, int exitValue ) {
			this.normalOutput = normalOutput;
			this.errorOutput = errorOutput;
			this.exitValue = exitValue;
		}

		public String getNormalOutput() {
			return this.normalOutput;
		}

		public String getErrorOutput() {
			return this.errorOutput;
		}

		public int getExitValue() {
			return this.exitValue;
		}
	}


	/**
	 * @author Noël - LIG
	 */
	private static class OutputRunnable implements Runnable {

		private final Process process;
		private final boolean errorLevel;
		private final Logger logger;
		private final StringBuilder sb;

		/**
		 * Constructor.
		 * @param process
		 * @param errorLevel
		 * @param sb
		 * @param logger
		 */
		public OutputRunnable( Process process, boolean errorLevel, StringBuilder sb, Logger logger ) {
			this.process = process;
			this.errorLevel = errorLevel;
			this.sb = sb;
			this.logger = logger;
		}

		@Override
		public void run() {

			final String prefix = this.errorLevel ? "-- ERROR --" : "";
			BufferedReader br = null;
			try {
				InputStream is = this.errorLevel ? this.process.getErrorStream() : this.process.getInputStream();
				br = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ));

				for( String line = br.readLine(); line != null; line = br.readLine())
					this.sb.append( prefix + line + "\n" );

			} catch( IOException e ) {
				this.logger.severe( Utils.writeExceptionButDoNotUseItForLogging( e ));

			} finally {
				Utils.closeQuietly( br );
			}
		}
	}
}
