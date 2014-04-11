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

package net.roboconf.core.model.parsing;

import java.io.File;

import junit.framework.Assert;

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
}
