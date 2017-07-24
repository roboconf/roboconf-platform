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

package net.roboconf.core.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfFlexMapTest {

	@Test
	public void testBasics() {

		// First set of tests
		RoboconfFlexMap<String,String> map = new RoboconfFlexMap<>( "!!!!!" );

		map.put( "test1", "test 2" );
		map.put( "test2", "test 3" );
		map.put( "test3", null );

		Assert.assertEquals( "test 2", map.get( "test1" ));
		Assert.assertEquals( "test 3", map.get( "test2" ));
		Assert.assertNull( map.get( "test3" ));

		Assert.assertEquals( new TreeSet<>( Arrays.asList( "test1", "test2", "test3" )), map.keySet());
		Assert.assertEquals( Arrays.asList( "test 2", "test 3", null ), map.values());
		Assert.assertEquals( "test 3", map.remove( "test2" ));
		Assert.assertFalse( map.containsKey( "test2" ) );
		Assert.assertTrue( map.containsKey( "test3" ) );
		Assert.assertFalse( map.isEmpty());

		Assert.assertTrue( map.containsValue( "test 2" ));
		Assert.assertTrue( map.containsValue( null ));
		Assert.assertFalse( map.containsValue( "test 3" ));

		Assert.assertNull( map.remove( "test3" ));
		Assert.assertFalse( map.containsValue( null ));

		// Go on
		map.clear();
		Assert.assertEquals( 0, map.size());
		Assert.assertTrue( map.isEmpty());

		Map<String,String> otherMap = new HashMap<> ();
		otherMap.put( "1", null );
		otherMap.put( "2", null );
		otherMap.put( "3", "3" );

		map.putAll( otherMap );
		Assert.assertEquals( otherMap.size(), map.size());
		Assert.assertEquals( otherMap.size(), map.entrySet().size());

		Assert.assertNull( map.get( "1" ));
		Assert.assertNull( map.get( "2" ));
		Assert.assertEquals( "3", map.get( "3" ));

		Assert.assertNotNull( map.toString());
	}


	@Test
	public void testHashCodeAndEquals() {

		RoboconfFlexMap<String,String> map1 = new RoboconfFlexMap<>( "!!!!!" );
		RoboconfFlexMap<String,String> map2 = new RoboconfFlexMap<>( "!!!!!" );
		RoboconfFlexMap<String,String> map3 = new RoboconfFlexMap<>( "###" );
		RoboconfFlexMap<String,String> map4 = new RoboconfFlexMap<>( "!!!!!" );

		map1.put( "test1", "test 2" );
		map1.put( "test2", "test 3" );
		map1.put( "test3", null );

		map2.put( "test1", "test 2" );
		map2.put( "test2", "test 3" );
		map2.put( "test3", null );

		map3.put( "test1", "test 2" );
		map3.put( "test2", "test 3" );
		map3.put( "test3", null );

		map4.put( "test1", "test 2" );
		map4.put( "test2", "test 3" );

		Assert.assertEquals( map1, map2 );
		Assert.assertNotEquals( map1, map3 );
		Assert.assertNotEquals( map2, map3 );
		Assert.assertNotEquals( map2, map4 );
		Assert.assertNotEquals( map2, new HashMap<>( 0 ));

		Assert.assertEquals( map1.hashCode(), map2.hashCode());
		Assert.assertNotEquals( map1.hashCode(), map3.hashCode());
		Assert.assertNotEquals( map2.hashCode(), map3.hashCode());
		Assert.assertNotEquals( map2.hashCode(), map4.hashCode());
	}
}
