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

package net.roboconf.core.logging;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import junit.framework.Assert;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfLogFormatterTest {

	@Test
	public void testFindMaxLevelLength() {

		int max = RoboconfLogFormatter.findMaxLevelLength();
		Assert.assertTrue( max >= Level.ALL.toString().length());
		Assert.assertTrue( max >= Level.CONFIG.toString().length());
		Assert.assertTrue( max >= Level.FINE.toString().length());
		Assert.assertTrue( max >= Level.FINER.toString().length());
		Assert.assertTrue( max >= Level.FINEST.toString().length());
		Assert.assertTrue( max >= Level.INFO.toString().length());
		Assert.assertTrue( max >= Level.OFF.toString().length());
		Assert.assertTrue( max >= Level.SEVERE.toString().length());
		Assert.assertTrue( max >= Level.WARNING.toString().length());
	}


	@Test
	public void testFormat() {

		LogRecord record = new LogRecord( Level.FINER, "This is an error log message." );
		record.setLoggerName( "my.logger.name" );
		record.setSourceClassName( "my.package.Class" );
		record.setSourceMethodName( "myMethod" );
		record.setMillis( new Date().getTime());

		String s = new RoboconfLogFormatter().format( record );
		Assert.assertEquals( 2, s.split( "\n" ).length );
		Assert.assertTrue( s.contains( "\n" + record.getMessage()));
		Assert.assertTrue( s.startsWith( "[" ));

		String fullName = record.getSourceClassName() + "#" + record.getSourceMethodName() + "\n";
		Assert.assertTrue( s.contains( fullName ));
	}
}
