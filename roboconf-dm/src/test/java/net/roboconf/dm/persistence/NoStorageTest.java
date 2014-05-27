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

import junit.framework.Assert;
import net.roboconf.dm.persistence.IDmStorage.DmStorageBean;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class NoStorageTest {

	@Test
	public void testRestorationAndPersistence() throws Exception {

		DmStorageBean bean = new DmStorageBean().messagingServerIp( "something" );
		IDmStorage storage = new NoStorage();
		storage.saveManagerState( bean );

		DmStorageBean restoredBean = storage.restoreManagerState();
		Assert.assertNotNull( restoredBean );
		Assert.assertNull( restoredBean.getMessagingServerIp());
		Assert.assertEquals( 0, restoredBean.getApplications().size());
	}
}
