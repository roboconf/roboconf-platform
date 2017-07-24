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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A concurrent hash map that supports null values and keeps key ordering during iterations.
 * <p>
 * For the record, ConcurrentHashMaps do not accept null values.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 * @param <K> a comparable type
 * @param <V> a type
 */
public class RoboconfFlexMap<K extends Comparable<?>,V> implements Serializable, Map<K,V> {

	private static final long serialVersionUID = 7715241436637667834L;
	private final ConcurrentHashMap<K,V> map = new ConcurrentHashMap<> ();
	private final V nullValue;


	/**
	 * Constructor.
	 * @param nullValue
	 */
	public RoboconfFlexMap( V nullValue ) {
		this.nullValue = nullValue;
	}


	// Wrapper methods

	@Override
	public void clear() {
		this.map.clear();
	}

	@Override
	public boolean containsKey( Object key ) {
		return this.map.containsKey( key );
	}

	@Override
	public boolean isEmpty() {
		return this.map.isEmpty();
	}

	@Override
	public Set<K> keySet() {
		return new TreeSet<>( this.map.keySet());
	}

	@Override
	public int size() {
		return this.map.size();
	}

	@Override
	public String toString() {
		return this.map.toString();
	}

	@Override
	public int hashCode() {
		return this.map.hashCode();
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof RoboconfFlexMap<?,?>
			&& this.nullValue.equals(((RoboconfFlexMap<?,?>) obj).nullValue)
			&& this.map.equals(((RoboconfFlexMap<?,?>) obj).map);
	}


	// Workaround methods

	@Override
	public V get( Object key ) {
		return outFilter( this.map.get( key ));
	}

	@Override
	public boolean containsValue( Object value ) {
		return this.map.containsValue( rawInFilter( value ));
	}

	@Override
	public V put( K key, V value ) {
		return this.map.put( key, inFilter( value ));
	}

	@Override
	public V remove( Object key ) {
		return outFilter( this.map.remove( key ));
	}


	/**
	 * @return a snapshot view of the map, with keys being ordered
	 */
	@Override
	public Set<Entry<K,V>> entrySet() {

		TreeMap<K,V> tempMap = new TreeMap<> ();
		for( Entry<K,V> entry : this.map.entrySet()) {
			tempMap.put(
					entry.getKey(),
					outFilter( entry.getValue()));
		}

		return tempMap.entrySet();
	}


	@Override
	public void putAll( Map<? extends K,? extends V> m ) {

		Map<? extends K,? extends V> m2 = new LinkedHashMap<>( m );
		for( Map.Entry<? extends K,? extends V> entry : m2.entrySet())
			put( entry.getKey(), entry.getValue());
	}


	/**
	 * @return a snapshot view of the values contained in the map, with values ordered by keys
	 */
	@Override
	public Collection<V> values() {

		Collection<V> result = new ArrayList<>( this.map.size());
		for( Map.Entry<K,V> entry : entrySet())
			result.add( outFilter( entry.getValue()));

		return result;
	}


	// Private methods

	/**
	 * Returns the real value from what was stored in the map.
	 * @param value a value (that cannot be null)
	 * @return null if the value is <code>nullValue</code>, something else otherwise
	 */
	private V outFilter( V value ) {
		return Objects.equals( value, this.nullValue ) ? null : value;
	}


	/**
	 * Returns the value to store in the internal map.
	 * @param value a value (can be null)
	 * @return <code>nullValue</code> if the parameter was null, something else otherwise
	 */
	private V inFilter( V value ) {
		return value == null ? this.nullValue : value;
	}


	/**
	 * Returns the value to store in the internal map.
	 * @param value a value (can be null)
	 * @return <code>nullValue</code> if the parameter was null, something else otherwise
	 */
	private Object rawInFilter( Object value ) {
		return value == null ? this.nullValue : value;
	}
}
