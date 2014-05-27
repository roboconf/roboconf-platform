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

package net.roboconf.dm.persistence;

import java.io.IOException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class NoStorage implements IDmStorage {

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.persistence.IDmStorage
	 * #saveManagerState(net.roboconf.dm.persistence.IDmStorage.DmStorageBean)
	 */
	@Override
	public void saveManagerState( DmStorageBean managerState )
	throws IOException {
		// nothing
	}

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.persistence.IDmStorage
	 * #restoreManagerState()
	 */
	@Override
	public DmStorageBean restoreManagerState() throws IOException {
		return new DmStorageBean();
	}

	/*
	 * (non-Javadoc)
	 * @see net.roboconf.dm.persistence.IDmStorage
	 * #requiresAgentContact()
	 */
	@Override
	public boolean requiresAgentContact() {
		return false;
	}
}
