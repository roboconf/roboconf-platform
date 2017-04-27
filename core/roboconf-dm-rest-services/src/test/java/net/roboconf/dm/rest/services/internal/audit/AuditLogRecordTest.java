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

package net.roboconf.dm.rest.services.internal.audit;

import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AuditLogRecordTest {

	@Test
	public void testFormatting() {

		AuditLogRecord log = new AuditLogRecord( "me", "/target", "/roboconf-dm/target", "get", "127.0.0.1", "Gecko", true );
		Assert.assertEquals( Level.INFO, log.getLevel());
		Assert.assertEquals( AuditLogRecord.LOGGER_NAME, log.getLoggerName());
		Assert.assertTrue( log.getMessage().contains( " me | " ));
		Assert.assertTrue( log.getMessage().contains( " /target | " ));
		Assert.assertTrue( log.getMessage().contains( " | /roboconf-dm/target | " ));
		Assert.assertTrue( log.getMessage().contains( " 127.0.0.1 | " ));
		Assert.assertTrue( log.getMessage().contains( " |    get | " ));
		Assert.assertTrue( log.getMessage().contains( " |      " + AuditLogRecord.ALLOWED + " | " ));
		Assert.assertTrue( log.getMessage().contains( " | Gecko" ));

		log = new AuditLogRecord( null, "/target2", "/roboconf-dm/target2", "post", "192.168.1.1", "Gecko", false );
		Assert.assertEquals( Level.SEVERE, log.getLevel());
		Assert.assertEquals( AuditLogRecord.LOGGER_NAME, log.getLoggerName());
		Assert.assertTrue( log.getMessage().contains( " " + AuditLogRecord.ANONYMOUS + " | " ));
		Assert.assertTrue( log.getMessage().contains( " /target2 | " ));
		Assert.assertTrue( log.getMessage().contains( " | /roboconf-dm/target2 | " ));
		Assert.assertTrue( log.getMessage().contains( " 192.168.1.1 | " ));
		Assert.assertTrue( log.getMessage().contains( " |   post | " ));
		Assert.assertTrue( log.getMessage().contains( " | " + AuditLogRecord.BLOCKED + " | " ));
		Assert.assertTrue( log.getMessage().contains( " | Gecko" ));

		log = new AuditLogRecord( "me", null, "/roboconf-dm/target2", "delete", "255.255.255.255", null, true );
		Assert.assertEquals( Level.WARNING, log.getLevel());
		Assert.assertEquals( AuditLogRecord.LOGGER_NAME, log.getLoggerName());
		Assert.assertTrue( log.getMessage().contains( " me | " ));
		Assert.assertTrue( log.getMessage().contains( " - | " ));
		Assert.assertTrue( log.getMessage().contains( " | /roboconf-dm/target2 | " ));
		Assert.assertTrue( log.getMessage().contains( " 255.255.255.255 | " ));
		Assert.assertTrue( log.getMessage().contains( " | delete | " ));
		Assert.assertTrue( log.getMessage().contains( " |      " + AuditLogRecord.ALLOWED + " | " ));
		Assert.assertTrue( log.getMessage().contains( " | -" ));

		log = new AuditLogRecord( "me too", "/target", "/roboconf-dm/target", "get", "127.0.0.1", "Gecko", false );
		Assert.assertEquals( Level.SEVERE, log.getLevel());
		Assert.assertEquals( AuditLogRecord.LOGGER_NAME, log.getLoggerName());
		Assert.assertTrue( log.getMessage().contains( " me too | " ));
		Assert.assertTrue( log.getMessage().contains( " /target | " ));
		Assert.assertTrue( log.getMessage().contains( " | /roboconf-dm/target | " ));
		Assert.assertTrue( log.getMessage().contains( " 127.0.0.1 | " ));
		Assert.assertTrue( log.getMessage().contains( " |    get | " ));
		Assert.assertTrue( log.getMessage().contains( " | " + AuditLogRecord.BLOCKED + " | " ));
		Assert.assertTrue( log.getMessage().contains( " | Gecko" ));

		// SEVERE > WARNING
		log = new AuditLogRecord( "me", null, "/roboconf-dm/target2", "delete", "255.255.255.255", "Gecko2", false );
		Assert.assertEquals( Level.SEVERE, log.getLevel());
		Assert.assertEquals( AuditLogRecord.LOGGER_NAME, log.getLoggerName());
		Assert.assertTrue( log.getMessage().contains( " me | " ));
		Assert.assertTrue( log.getMessage().contains( " - | " ));
		Assert.assertTrue( log.getMessage().contains( " | /roboconf-dm/target2" ));
		Assert.assertTrue( log.getMessage().contains( " 255.255.255.255 | " ));
		Assert.assertTrue( log.getMessage().contains( " | delete | " ));
		Assert.assertTrue( log.getMessage().contains( " | " + AuditLogRecord.BLOCKED + " | " ));
		Assert.assertTrue( log.getMessage().contains( " | Gecko2" ));
	}
}
