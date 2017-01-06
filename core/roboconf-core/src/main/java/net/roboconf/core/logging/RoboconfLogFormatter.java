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

package net.roboconf.core.logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfLogFormatter extends Formatter {

	private static final int LEVEL_SPAN = findMaxLevelLength();
	private final DateFormat df = new SimpleDateFormat( "dd/MM/yyyy hh:mm:ss.SSS" );


	/*
	 * (non-Javadoc)
	 * @see java.util.logging.Formatter
	 * #format(java.util.logging.LogRecord)
	 */
	@Override
	public String format( LogRecord record ) {

		// Center the level
		int totalSpaces = LEVEL_SPAN - record.getLevel().toString().length();
		StringBuilder sbLevel = new StringBuilder();
		for( int i=0; i<totalSpaces/2; i++ )
			sbLevel.append( " " );

		sbLevel.append( record.getLevel());
		for( int i=sbLevel.toString().length(); i<LEVEL_SPAN; i++ )
			sbLevel.append( " " );

		// Format the entire record
		StringBuilder sb = new StringBuilder();
		sb.append( "[ " ).append( this.df.format( new Date( record.getMillis()))).append(" ]" );
		sb.append( "[ ").append( sbLevel ).append( " ] " );
		sb.append( record.getSourceClassName()).append("#");
		sb.append( record.getSourceMethodName());

		sb.append( "\n" );
		sb.append( formatMessage( record ));
		sb.append( "\n" );

		return sb.toString();
	}


	/**
	 * @return the maximum length of a log level
	 */
	static int findMaxLevelLength() {

		Level[] levels = new Level[] {
				Level.ALL, Level.CONFIG, Level.FINE,
				Level.FINER, Level.FINEST, Level.INFO,
				Level.OFF, Level.SEVERE, Level.WARNING
		};

		int result = -1;
		for( Level level : levels )
			result = Math.max( result, level.toString().length());

		return result;
	}
}
