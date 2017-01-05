/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.dsl.parsing;

/**
 * The properties for facets and components.
 * @author Vincent Zurczak - Linagora
 */
public class BlockProperty extends AbstractBlock {

	private String name, value;


	/**
	 * Constructor.
	 * @param declaringFile the definition file
	 */
	public BlockProperty( FileDefinition declaringFile ) {
		super( declaringFile );
	}

	/**
	 * Constructor.
	 * @param declaringFile the definition file
	 * @param name the property name
	 * @param value the property value
	 */
	public BlockProperty( FileDefinition declaringFile, String name, String value ) {
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
	 * @see net.roboconf.core.dsl.parsing.AbstractBlock#getInstructionType()
	 */
	@Override
	public int getInstructionType() {
		return AbstractBlock.PROPERTY;
	}
}
