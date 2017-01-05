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

package net.roboconf.agent.monitoring.internal.nagios;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;

/**
 * Live status client.
 * @author Pierre-Yves Gibello - Linagora
 */
public class LiveStatusClient {

	private static final String NAGIOS_COLUMNS = "columns:";
	static final String DEFAULT_HOST = "localhost";
	static final int DEFAULT_PORT = 50000;

	private final Logger logger = Logger.getLogger( getClass().getName());
	final String host;
	final int port;


	/**
	 * Constructor.
	 * @param host
	 * @param port
	 */
	public LiveStatusClient( String host, int port ) {
		this.host = Utils.isEmptyOrWhitespaces( host ) ? DEFAULT_HOST : host;
		this.port = port < 1 ? DEFAULT_PORT : port;
	}


	/**
	 * Queries a live status server.
	 * @param nagiosQuery the query to pass through a socket (not null)
	 * @return the response
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public String queryLivestatus( String nagiosQuery ) throws UnknownHostException, IOException {

		Socket liveStatusSocket = null;
		try {
			this.logger.fine( "About to open a connection through Live Status..." );
			liveStatusSocket = new Socket( this.host, this.port );
			this.logger.fine( "A connection was established through Live Status." );

			Writer osw = new OutputStreamWriter( liveStatusSocket.getOutputStream(), StandardCharsets.UTF_8 );
			PrintWriter printer = new PrintWriter( osw, false );

			printer.print( nagiosQuery );
			printer.flush();
			liveStatusSocket.shutdownOutput();

			ByteArrayOutputStream os = new ByteArrayOutputStream();
			Utils.copyStreamSafely( liveStatusSocket.getInputStream(), os );

			String result = os.toString( "UTF-8" );
			result = format( nagiosQuery, result );
			return result;

		} finally {
			if( liveStatusSocket != null )
				liveStatusSocket.close();

			this.logger.fine( "The Live Status connection was closed." );
		}
	}


	/**
	 * When columns are specified, Live Status omits the column names.
	 * <p>
	 * This method adds them.
	 * </p>
	 *
	 * @param liveStatusResponse the (non-null) response
	 * @return a non-null string
	 */
	String format( String request, String liveStatusResponse ) {

		String columnsDecl = null;
		for( String s : request.split( "\n" )) {
			s = s.trim();
			if( s.toLowerCase().startsWith( NAGIOS_COLUMNS )) {
				columnsDecl = s.substring( NAGIOS_COLUMNS.length()).trim();
				break;
			}
		}

		String result = liveStatusResponse;
		if( columnsDecl != null ) {
			columnsDecl = columnsDecl.replaceAll( "\\s+", ";" );
			result = columnsDecl + "\n" + result;
		}

		return result;
	}
}
