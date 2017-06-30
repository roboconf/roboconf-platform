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

package net.roboconf.dm.internal.api.impl.beans;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.roboconf.dm.management.api.ITargetsMngr.TargetProperties;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetPropertiesImpl implements TargetProperties {

	private final Map<String,String> map;
	private final String content;
	private final File sourceFile;


	/**
	 * Constructor.
	 */
	public TargetPropertiesImpl() {
		this( new HashMap<String,String>( 0 ), "", null );
	}

	/**
	 * Constructor.
	 * @param map
	 * @param content
	 * @param sourceFile
	 */
	public TargetPropertiesImpl( Map<String,String> map, String content, File sourceFile ) {
		this.map = map;
		this.content = content;
		this.sourceFile = sourceFile;
	}

	@Override
	public Map<String,String> asMap() {
		return this.map;
	}

	@Override
	public String asString() {
		return this.content;
	}

	@Override
	public File getSourceFile() {
		return this.sourceFile;
	}
}
