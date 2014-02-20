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

package net.roboconf.dm.rest.client.mocks;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.roboconf.dm.rest.api.IInitWs;
import net.roboconf.dm.rest.client.mocks.helper.PropertyManager;

/**
 * @author Vincent Zurczak - Linagora
 */
@Path( IInitWs.PATH )
public class InitWsMock implements IInitWs {

	public static final String IP_OOPS = "oops";


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IInitWs#init(java.lang.String)
	 */
	@Override
	public Response init( String amqpIp ) {

		Response response;
		if( IP_OOPS.equals( amqpIp )) {
			response = Response.status( Status.NOT_FOUND ).build();
		} else {
			PropertyManager.INSTANCE.initialized.set( true );
			response = Response.ok().build();
		}

		return response;
	}


	/* (non-Javadoc)
	 * @see net.roboconf.dm.rest.client.exceptions.server.IInitWs#isInitialized()
	 */
	@Override
	public Response isInitialized() {
		String s = String.valueOf( PropertyManager.INSTANCE.initialized.get());
		return Response.ok( s ).build();
	}
}
