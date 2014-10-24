package net.roboconf.integration.test;

import java.util.List;

import net.roboconf.core.model.runtime.Application;
import net.roboconf.dm.rest.client.WsClient;
import net.roboconf.dm.rest.client.exceptions.ManagementException;

public class Main {

	public static void main( String[] args ) {

		try {
			WsClient client = new WsClient( "http://localhost:8181/roboconf-dm" );
			List<Application> apps = client.getManagementDelegate().listApplications();
			for( Application a : apps )
				System.out.println( a );

			System.out.println( "ok" );

		} catch( ManagementException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
