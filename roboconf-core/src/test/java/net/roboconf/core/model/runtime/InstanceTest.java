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

package net.roboconf.core.model.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Assert;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class InstanceTest {

	@Test
	public void testWichStatus() {
		Assert.assertEquals( InstanceStatus.STARTING, InstanceStatus.wichStatus( "starting" ));
		Assert.assertEquals( InstanceStatus.STARTING, InstanceStatus.wichStatus( "startiNG" ));
		Assert.assertEquals( InstanceStatus.NOT_DEPLOYED, InstanceStatus.wichStatus( "start" ));
	}


	@Test
	public void testUpdateImports() {

		Map<String,Collection<Import>> prefixToImports = new HashMap<String,Collection<Import>> ();
		prefixToImports.put( "comp", Arrays.asList(
				new Import( "/root1" ),
				new Import( "/root2" )));

		Instance inst = new Instance( "inst" );
		Assert.assertEquals( 0, inst.getImports().size());

		inst.updateImports( prefixToImports );
		Assert.assertEquals( 1, inst.getImports().size());

		Iterator<Import> iterator = inst.getImports().get( "comp" ).iterator();
		Assert.assertEquals( "/root1", iterator.next().getInstancePath());
		Assert.assertEquals( "/root2", iterator.next().getInstancePath());
		Assert.assertFalse( iterator.hasNext());

		prefixToImports.put( "comp", Arrays.asList( new Import( "/root1" )));
		inst.updateImports( prefixToImports );
		Assert.assertEquals( 1, inst.getImports().size());

		iterator = inst.getImports().get( "comp" ).iterator();
		Assert.assertEquals( "/root1", iterator.next().getInstancePath());
		Assert.assertFalse( iterator.hasNext());
	}


	@Test
	public void testAddImport() {

		Map<String,Collection<Import>> prefixToImports = new HashMap<String,Collection<Import>> ();
		prefixToImports.put( "comp", new ArrayList<Import>( Arrays.asList(
				new Import( "/root1" ),
				new Import( "/root2" ))));

		Instance inst = new Instance( "inst" );
		inst.getImports().putAll( prefixToImports );
		Assert.assertEquals( 1, inst.getImports().keySet().size());
		Assert.assertTrue( inst.getImports().keySet().contains( "comp" ));

		inst.addImport( "wow", new Import( "/root" ));
		Assert.assertEquals( 2, inst.getImports().keySet().size());
		Assert.assertTrue( inst.getImports().keySet().contains( "comp" ));
		Assert.assertTrue( inst.getImports().keySet().contains( "wow" ));

		Assert.assertEquals( 2, inst.getImports().get( "comp" ).size());
		inst.addImport( "comp", new Import( "/root3" ));
		Assert.assertEquals( 3, inst.getImports().get( "comp" ).size());
		Assert.assertEquals( 2, inst.getImports().keySet().size());
	}


	@Test
	public void testChain() {

		Instance inst = new Instance().name( "ins" ).channel( "ch" ).status( InstanceStatus.DEPLOYING ).component( null ).parent( null );
		Assert.assertEquals( "ch", inst.getChannel());
		Assert.assertEquals( "ins", inst.getName());
		Assert.assertEquals( InstanceStatus.DEPLOYING, inst.getStatus());
		Assert.assertNull( inst.getComponent());
		Assert.assertNull( inst.getParent());
	}
}
