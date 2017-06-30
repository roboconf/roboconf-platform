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

package net.roboconf.core.errors;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.errors.ErrorDetails.ErrorDetailsKind;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.internal.tests.TestApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.Instance;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ErrorDetailsTest {

	@Test
	public void testHashCode() {

		String elt = "kikou";
		ErrorDetails ed = ErrorDetails.component( elt );
		Assert.assertEquals( elt.hashCode(), ed.hashCode());
	}


	@Test
	public void testStaticMethods() {

		ErrorDetails ed = ErrorDetails.component( "" );
		Assert.assertEquals( ErrorDetailsKind.COMPONENT, ed.getErrorDetailsKind());
		Assert.assertEquals( "", ed.getElementName());

		ed = ErrorDetails.component( new Component( "" ));
		Assert.assertEquals( ErrorDetailsKind.COMPONENT, ed.getErrorDetailsKind());
		Assert.assertEquals( "", ed.getElementName());

		ed = ErrorDetails.facet( "" );
		Assert.assertEquals( ErrorDetailsKind.FACET, ed.getErrorDetailsKind());
		Assert.assertEquals( "", ed.getElementName());

		ed = ErrorDetails.facet( new Facet( "" ));
		Assert.assertEquals( ErrorDetailsKind.FACET, ed.getErrorDetailsKind());
		Assert.assertEquals( "", ed.getElementName());

		ed = ErrorDetails.application( "" );
		Assert.assertEquals( ErrorDetailsKind.APPLICATION, ed.getErrorDetailsKind());
		Assert.assertEquals( "", ed.getElementName());

		ed = ErrorDetails.application( new TestApplication());
		Assert.assertEquals( ErrorDetailsKind.APPLICATION, ed.getErrorDetailsKind());
		Assert.assertEquals( "test", ed.getElementName());

		ed = ErrorDetails.applicationTpl( "n", "v" );
		Assert.assertEquals( ErrorDetailsKind.APPLICATION_TEMPLATE, ed.getErrorDetailsKind());
		Assert.assertEquals( "n (v)", ed.getElementName());

		ed = ErrorDetails.applicationTpl( new TestApplicationTemplate());
		Assert.assertEquals( ErrorDetailsKind.APPLICATION_TEMPLATE, ed.getErrorDetailsKind());
		Assert.assertEquals( "test-app (1.0.1)", ed.getElementName());

		ed = ErrorDetails.cycle( "" );
		Assert.assertEquals( ErrorDetailsKind.CYCLE, ed.getErrorDetailsKind());
		Assert.assertEquals( "", ed.getElementName());

		ed = ErrorDetails.logReference( "abcd" );
		Assert.assertEquals( ErrorDetailsKind.LOG_REFERENCE, ed.getErrorDetailsKind());
		Assert.assertEquals( "abcd", ed.getElementName());

		ed = ErrorDetails.instance( "" );
		Assert.assertEquals( ErrorDetailsKind.INSTANCE, ed.getErrorDetailsKind());
		Assert.assertEquals( "", ed.getElementName());

		ed = ErrorDetails.instance( new Instance( "t" ));
		Assert.assertEquals( ErrorDetailsKind.INSTANCE, ed.getErrorDetailsKind());
		Assert.assertEquals( "t", ed.getElementName());

		ed = ErrorDetails.file( new File( "/test" ));
		Assert.assertEquals( ErrorDetailsKind.FILE, ed.getErrorDetailsKind());
		Assert.assertEquals( "/test", ed.getElementName());

		ed = ErrorDetails.file( "test" );
		Assert.assertEquals( ErrorDetailsKind.FILE, ed.getErrorDetailsKind());
		Assert.assertEquals( "test", ed.getElementName());

		ed = ErrorDetails.directory( new File( "/test" ));
		Assert.assertEquals( ErrorDetailsKind.DIRECTORY, ed.getErrorDetailsKind());
		Assert.assertEquals( "/test", ed.getElementName());

		ed = ErrorDetails.directory( "test" );
		Assert.assertEquals( ErrorDetailsKind.DIRECTORY, ed.getErrorDetailsKind());
		Assert.assertEquals( "test", ed.getElementName());

		ed = ErrorDetails.variable( "" );
		Assert.assertEquals( ErrorDetailsKind.VARIABLE, ed.getErrorDetailsKind());
		Assert.assertEquals( "", ed.getElementName());

		ed = ErrorDetails.unrecognized( "" );
		Assert.assertEquals( ErrorDetailsKind.UNRECOGNIZED, ed.getErrorDetailsKind());
		Assert.assertEquals( "", ed.getElementName());

		ed = ErrorDetails.expected( "" );
		Assert.assertEquals( ErrorDetailsKind.EXPECTED, ed.getErrorDetailsKind());
		Assert.assertEquals( "", ed.getElementName());

		ed = ErrorDetails.unexpected( "" );
		Assert.assertEquals( ErrorDetailsKind.UNEXPECTED, ed.getErrorDetailsKind());
		Assert.assertEquals( "", ed.getElementName());

		ed = ErrorDetails.malformed( "" );
		Assert.assertEquals( ErrorDetailsKind.MALFORMED, ed.getErrorDetailsKind());
		Assert.assertEquals( "", ed.getElementName());

		ed = ErrorDetails.name( "" );
		Assert.assertEquals( ErrorDetailsKind.NAME, ed.getErrorDetailsKind());
		Assert.assertEquals( "", ed.getElementName());

		ed = ErrorDetails.line( 24 );
		Assert.assertEquals( ErrorDetailsKind.LINE, ed.getErrorDetailsKind());
		Assert.assertEquals( "24", ed.getElementName());

		ed = ErrorDetails.instruction( "" );
		Assert.assertEquals( ErrorDetailsKind.INSTRUCTION, ed.getErrorDetailsKind());
		Assert.assertEquals( "", ed.getElementName());

		ed = ErrorDetails.alreadyDefined( "" );
		Assert.assertEquals( ErrorDetailsKind.ALREADY_DEFINED, ed.getErrorDetailsKind());
		Assert.assertEquals( "", ed.getElementName());

		ed = ErrorDetails.conflicting( "" );
		Assert.assertEquals( ErrorDetailsKind.CONFLICTING, ed.getErrorDetailsKind());
		Assert.assertEquals( "", ed.getElementName());

		ed = ErrorDetails.value( "" );
		Assert.assertEquals( ErrorDetailsKind.VALUE, ed.getErrorDetailsKind());
		Assert.assertEquals( "", ed.getElementName());

		ed = ErrorDetails.exception( new RuntimeException( "hi" ));
		Assert.assertEquals( ErrorDetailsKind.EXCEPTION, ed.getErrorDetailsKind());
		Assert.assertTrue( ed.getElementName().contains( "hi" ));

		ed = ErrorDetails.exceptionName( new RuntimeException( "hi" ));
		Assert.assertEquals( ErrorDetailsKind.EXCEPTION_NAME, ed.getErrorDetailsKind());
		Assert.assertEquals( RuntimeException.class.getName(), ed.getElementName());
	}


	@Test
	public void testEquals() {

		ErrorDetails ed1 = ErrorDetails.component( "c1" );
		ErrorDetails ed2 = ErrorDetails.instance( "c1" );
		ErrorDetails ed3 = ErrorDetails.component( "c2" );

		Assert.assertNotEquals( ed1, ed2 );
		Assert.assertNotEquals( ed3, ed2 );
		Assert.assertNotEquals( ed1, ed3 );
		Assert.assertEquals( ed1, ErrorDetails.component( "c1" ));

		Assert.assertNotEquals( ed1, null );
		Assert.assertNotEquals( ed1, new Object());
	}
}
