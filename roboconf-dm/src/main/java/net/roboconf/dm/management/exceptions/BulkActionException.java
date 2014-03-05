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

package net.roboconf.dm.management.exceptions;

import java.util.HashMap;
import java.util.Map;

import net.roboconf.core.internal.utils.Utils;
import net.roboconf.core.model.runtime.Instance;

/**
 * An exception that stores several exceptions.
 * <p>
 * This is useful when we execute bulk actions, i.e. when
 * we process a lot of instances at once.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class BulkActionException extends Exception {
	private static final long serialVersionUID = -599978625913629464L;

	private final Map<Instance,Exception> instancesToException;
	private final boolean create;


	/**
	 * Constructor.
	 */
	public BulkActionException( boolean create ) {
		super();
		this.create = create;
		this.instancesToException = new HashMap<Instance,Exception> ();
	}


	/**
	 * @return the instancesToException
	 */
	public Map<Instance,Exception> getInstancesToException() {
		return this.instancesToException;
	}


	@Override
	public String getMessage() {

		StringBuilder sb = new StringBuilder();
		sb.append( "Errors were encountered while " );
		sb.append( this.create ? "creating" : "terminating" );
		sb.append( " machines." );

		return sb.toString();
	}


	/**
	 * Writes a detailed message about the exceptions that were encountered.
	 * @param printDetails true to print details (finest), false for the minimal information (warning, severe...).
	 * @return a non-null string
	 */
	public String getLogMessage( boolean printDetails ) {

		StringBuilder sb = new StringBuilder();
		sb.append( toString());
		for( Map.Entry<Instance,Exception> entry : this.instancesToException.entrySet()) {
			sb.append( "\n\n- " );
			sb.append( entry.getKey().getName());
			sb.append( "\n" );

			if( printDetails )
				sb.append( Utils.writeException( entry.getValue()));
			else
				sb.append( entry.getValue().getMessage());
		}

		return sb.toString();
	}
}
