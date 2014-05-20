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

package net.roboconf.dm.rest.json;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Vincent Zurczak - Linagora
 */
public class MapHolder {

	public static final String FILE_LOCAL_PATH = "file-local-path";
	public static final String INSTANCE_PATH = "instance-path";
	public static final String APPLY_TO_CHILDREN = "apply-to-children";

	private final Map<String,String> map = new LinkedHashMap<String,String> ();


	/**
	 * @return the map
	 */
	public Map<String, String> getMap() {
		return this.map;
	}
}
