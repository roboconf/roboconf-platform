/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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

package net.roboconf.core.model.parsing;

/**
 * The properties for facets and components.
 * @author Vincent Zurczak - Linagora
 */
public class RegionProperty extends AbstractRegion {

	private String name, value;


	/**
	 * Constructor.
	 * @param declaringFile
	 */
	public RegionProperty( FileDefinition declaringFile ) {
		super( declaringFile );
	}

	/**
	 * Constructor.
	 * @param declaringFile
	 * @param name
	 * @param value
	 */
	public RegionProperty( FileDefinition declaringFile, String name, String value ) {
		this( declaringFile );
		this.name = name;
		this.value = value;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName( String name ) {
		this.name = name;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue( String value ) {
		this.value = value;
	}

	/**
	 * @param name the name to set
	 * @param value the value to set
	 */
	public void setNameAndValue( String name, String value ) {
		this.name = name;
		this.value = value;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.name + " => " + this.value;
	}

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.core.model.parsing.AbstractRegion#getInstructionType()
	 */
	@Override
	public int getInstructionType() {
		return AbstractRegion.PROPERTY;
	}
}
