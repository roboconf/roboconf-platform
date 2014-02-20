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

package net.roboconf.dm.rest.client.mocks.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Application.ApplicationStatus;
import net.roboconf.core.model.runtime.Component;
import net.roboconf.core.model.runtime.Graphs;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;

/**
 * @author Vincent Zurczak - Linagora
 */
public class PropertyManager {

	public static final String APP_1 = "App1";
	public static final String APP_2 = "App2";
	public static final PropertyManager INSTANCE = new PropertyManager();

	public AtomicBoolean initialized = new AtomicBoolean();
	public List<Application> apps = new ArrayList<Application> ();
	public List<String> remoteFiles = new ArrayList<String> ();


	private PropertyManager() {
		// nothing
	}


	public void loadApplications() {
		this.apps.add( createApplication1());
		this.apps.add( createApplication2());
	}


	public void reset() {
		this.apps.clear();
		this.remoteFiles.clear();
	}


	private Application createApplication1() {

		Application app = new Application();
		app.setName( APP_1 );
		app.setDescription( "Some description" );
		app.setQualifier( "v1" );
		app.setStatus( ApplicationStatus.STOPPED );

		Component c = new Component();
		c.setName( "vm" );
		c.setInstallerName( "iaas" );
		c.setAlias( "A VM" );

		Graphs graphs = new Graphs();
		graphs.getRootComponents().add( c );
		app.setGraphs( graphs );

		return app;
	}


	private Application createApplication2() {

		Application app = createApplication1();
		app.setName( APP_2 );
		app.setDescription( null );

		Component c = new Component();
		c.setName( "server" );
		c.setInstallerName( "puppet" );
		c.setAlias( "A server" );

		Component vmComponent = app.getGraphs().getRootComponents().iterator().next();
		vmComponent.getChildren().add( c );
		c.getAncestors().add( vmComponent );
		app.getGraphs().getRootComponents().add( c );

		Instance i1 = new Instance();
		i1.setName( "theVm" );
		i1.setStatus( InstanceStatus.INSTANTIATING );
		i1.setComponent( vmComponent );

		Instance i2 = new Instance();
		i2.setName( "server_1" );
		i2.setStatus( InstanceStatus.STOPPED );
		i2.setComponent( c );

		app.getRootInstances().add( i1 );
		InstanceHelpers.insertChild( i1, i2 );

		return app;
	}
}
