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

package net.roboconf.karaf.commands.dm.history;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
@Command( scope = "roboconf", name = "prune-history", description="Prune's the history" )
@Service
public class PruneHistoryCommand implements Action {

	@Argument( index = 0, name = "daysToKeep", required = false, description = "The number of days to keep in the history (1 by default)." )
	int daysToKeep = 1;

	// We cannot filter OSGi services with Karaf references
	@Reference
	BundleContext bundleContext;

	// Other fields
	private final Logger logger = Logger.getLogger( getClass().getName());
	PrintStream out = System.out;


	@Override
	public Object execute() throws Exception {

		// daysToKeep >= 0
		if( this.daysToKeep < 0 ) {
			this.out.println( "[ WARNING ] The daysToKeep argument must be equal or greater than 0. Operation cancelled." );
			return null;
		}

		// Get the service references
		Collection<ServiceReference<DataSource>> dataSources = this.bundleContext.getServiceReferences(
				DataSource.class,
				"(dataSourceName=roboconf-dm-db)" );

		// No data source found? Most likely a bug... or a wrong configuration.
		if( dataSources.isEmpty()) {
			this.out.println( "No data source was found to prune the commands history." );
		}

		// Otherwise, let's prune!
		else {
			this.out.println( "Pruning the commands history." );
			if( this.daysToKeep > 0 )
				this.out.println( "Only the last " + this.daysToKeep + " day" + (this.daysToKeep == 1 ? "" : "s") +" will be kept." );
			else
				this.out.println( "All the entries will be deleted." );

			ServiceReference<DataSource> sr = dataSources.iterator().next();
			DataSource dataSource = this.bundleContext.getService( sr );
			Connection conn = null;
			PreparedStatement ps = null;
			try {
				long startLimitInMilliSeconds = System.currentTimeMillis() - 1000L * 24 * 3600 * this.daysToKeep;

				StringBuilder sb = new StringBuilder();
				sb.append( "DELETE FROM commands_history WHERE start < ?" );

				conn = dataSource.getConnection();

				// Use PreparedStatement to prevent SQL injection
				ps = conn.prepareStatement( sb.toString());
				ps.setString( 1, String.valueOf( startLimitInMilliSeconds ));

				// Execute
				ps.executeUpdate();
				this.out.println( "Pruning done." );

			} catch( SQLException e ) {
				this.logger.severe( "An error occurred while pruning the commands history." );
				Utils.logException( this.logger, e );
				e.printStackTrace();

			} finally {
				// Close the SQL stuff
				Utils.closeStatement( ps, this.logger );
				Utils.closeConnection( conn, this.logger );

				// Release the service's usage (very important)
				this.bundleContext.ungetService( sr );
			}
		}

		return null;
	}
}
