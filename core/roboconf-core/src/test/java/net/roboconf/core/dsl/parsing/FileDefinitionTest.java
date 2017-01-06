/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

import java.io.File;

import org.junit.Assert;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class FileDefinitionTest {

	@Test
	public void testFileTypeAsString() {

		Assert.assertNotSame( "unknown", FileDefinition.fileTypeAsString( FileDefinition.AGGREGATOR ));
		Assert.assertNotSame( "unknown", FileDefinition.fileTypeAsString( FileDefinition.GRAPH ));
		Assert.assertNotSame( "unknown", FileDefinition.fileTypeAsString( FileDefinition.INSTANCE ));
		Assert.assertNotSame( "unknown", FileDefinition.fileTypeAsString( FileDefinition.UNDETERMINED ));
		Assert.assertEquals( "unknown", FileDefinition.fileTypeAsString( 54 ));
	}


	@Test
	public void testSetFileType_1() {
		FileDefinition def = new FileDefinition( new File( "toto.txt" ));
		def.setFileType( FileDefinition.INSTANCE );
	}



	@Test
	public void testSetFileType_2() {
		FileDefinition def = new FileDefinition( new File( "toto.txt" ));
		def.setFileType( FileDefinition.GRAPH );
	}


	@Test
	public void testSetFileType_3() {
		FileDefinition def = new FileDefinition( new File( "toto.txt" ));
		def.setFileType( FileDefinition.AGGREGATOR );
	}


	@Test( expected = IllegalArgumentException.class )
	public void testSetFileType_4() {
		FileDefinition def = new FileDefinition( new File( "toto.txt" ));
		def.setFileType( FileDefinition.UNDETERMINED );
	}


	@Test( expected = IllegalArgumentException.class )
	public void testSetFileType_5() {
		FileDefinition def = new FileDefinition( new File( "toto.txt" ));
		def.setFileType( 87 );
	}


	@Test
	public void testToString() {

		FileDefinition def = new FileDefinition( new File( "toto.txt" ));
		Assert.assertNotNull( def.toString());

		def = new FileDefinition( new File( "htpp://roboconf.net/toto.txt" ));
		Assert.assertNotNull( def.toString());
	}
}
