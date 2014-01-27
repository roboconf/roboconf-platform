package net.roboconf.core;

import java.io.File;

import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.io.RuntimeModelIo;
import net.roboconf.core.model.io.RuntimeModelIo.LoadResult;
import net.roboconf.core.model.runtime.Application;
import net.roboconf.core.model.runtime.Instance;


public class Example {

	public static void main( String[] args ) {

		File someProjectFile = new File( "" );
		LoadResult result = RuntimeModelIo.loadApplication( someProjectFile );

		if( result.getLoadErrors().isEmpty()) {
			// TODO: dump errors;

		} else {
			Application app = result.getApplication();
			for( Instance inst : app.getRootInstances())
				processRootInstance( inst );
		}
	}


	private static void processRootInstance( Instance rootInstance ) {

		for( Instance inst : InstanceHelpers.buildHierarchicalList( rootInstance )) {

			String installerName = inst.getComponent().getInstallerName();
			RoboconfPlugin plugin = findPlugin( installerName );
			if( plugin == null )
				throw new RuntimeException( "Unknown plugin" );

			PluginStatus status = plugin.process( inst );
			if( status == PluginStatus.ERROR ) {
				// TODO: process the error
				break;
			}
		}
	}









	private static RoboconfPlugin findPlugin( String installerName ) {
		// TODO Auto-generated method stub
		return null;
	}



	public static class RoboconfPlugin {

		public PluginStatus process( Instance inst ) {
			return PluginStatus.OK;
		}

	}


	public static enum PluginStatus {
		OK, WARNING, ERROR;
	}
}
