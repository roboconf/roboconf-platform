/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.rest.services.internal.audit;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A log record dedicated to audit security access to our REST API.
 * <p>
 * If the target resource is not defined, or if the access is not authorized,
 * the log level is {@value Level#WARNING}. If the user is unknown, it is
 * {@value Level#SEVERE}. Otherwise, it is {@value Level#INFO}.
 * </p>
 * <p>
 * At the beginning, it was thought we could just add custom fields and
 * format the output in the log configuration files. Unfortunately, it does
 * not work. Pax-Logging converts java.util.logs into Log4j logs and it does
 * not propagate everything. So, it is more simple for this class to format
 * the log message. To make things simple, the formatting is not configurable.
 * </p>
 * <p>
 * This class overrides the logger name and sets it to {@value #LOGGER_NAME}.
 * It has a specific configuration in the log4j properties and does not inherit
 * from the usual Roboconf loggers (net.roboconf).
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class AuditLogRecord extends LogRecord {

	public static final String LOGGER_NAME = "audit.roboconf.rest.services";
	private static final long serialVersionUID = -463999943841687911L;

	static final String SEPARATOR = " | ";
	static final String ANONYMOUS = "anonymous";
	static final String ALLOWED = "OK";
	static final String BLOCKED = "BLOCKED";

	private static final int PADDING_USER = 30;			// Rough Estimation
	private static final int PADDING_TARGET = 60;		// Rough estimation
	private static final int PADDING_VERB = 6;			// "DELETE".length()
	private static final int PADDING_AUTHORIZED = BLOCKED.length();
	private static final int PADDING_IP = 16;			// IPV4 => 15, IPV6 => 45


	/**
	 * Constructor.
	 * @param user the user name (null for anonymous)
	 * @param targetResource the REST path (as defined in Jersey annotations)
	 * @param targetPath the real URI path
	 * @param ipAddress the IP address of the client
	 * @param restVerb the REST verb (get, post, put, delete)
	 * @param userAgent the user agent
	 * @param authorized true if the access is authorized, false otherwise
	 */
	public AuditLogRecord(
			String user,
			String targetResource,
			String targetPath,
			String restVerb,
			String ipAddress,
			String userAgent,
			boolean authorized ) {
		super(
				findLevel( user, targetResource, authorized ),
				buildMessage( user, targetResource, targetPath, restVerb, ipAddress, userAgent, authorized ));

		setLoggerName( LOGGER_NAME );
		setSourceClassName( null );
		setSourceMethodName( null );
	}


	/**
	 * @param user
	 * @param targetResource
	 * @param restVerb
	 * @param ipAddress
	 * @param targetPath
	 * @param userAgent the user agent
	 * @param authorized
	 * @return a non-null and formatted message
	 */
	private static String buildMessage(
			String user,
			String targetResource,
			String targetPath,
			String restVerb,
			String ipAddress,
			String userAgent,
			boolean authorized ) {

		StringBuilder sb = new StringBuilder();
		sb.append( String.format( "%" + PADDING_IP + "s", ipAddress ));
		sb.append( SEPARATOR );
		sb.append( String.format( "%" + PADDING_AUTHORIZED + "s", authorized ? ALLOWED : BLOCKED ));
		sb.append( SEPARATOR );
		sb.append( String.format( "%" + PADDING_USER + "s", user == null ? ANONYMOUS : user ));
		sb.append( SEPARATOR );
		sb.append( String.format( "%" + PADDING_VERB + "s", restVerb ));
		sb.append( SEPARATOR );
		sb.append( String.format( "%" + PADDING_TARGET + "s", targetResource == null ? "-" : targetResource ));
		sb.append( SEPARATOR );

		// Left-aligned, no limit of size
		sb.append( targetPath );
		sb.append( SEPARATOR );
		sb.append( userAgent == null ? "-" : userAgent );

		return sb.toString();
	}


	/**
	 * @param user
	 * @param targetResource
	 * @param authorized
	 * @return a non-null log level
	 */
	private static Level findLevel( String user, String targetResource, boolean authorized ) {

		Level result = Level.INFO;
		if( user == null )
			result = Level.SEVERE;
		else if( ! authorized )
			result = Level.SEVERE;
		else if( targetResource == null )
			result = Level.WARNING;

		return result;
	}
}
