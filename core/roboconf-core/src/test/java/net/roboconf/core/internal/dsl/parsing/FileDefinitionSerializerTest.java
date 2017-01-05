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

package net.roboconf.core.internal.dsl.parsing;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.dsl.parsing.BlockBlank;
import net.roboconf.core.dsl.parsing.BlockComment;
import net.roboconf.core.dsl.parsing.FileDefinition;

/**
 * @author Vincent Zurczak - Linagora
 */
public class FileDefinitionSerializerTest {

	@Test
	public void testRepairedBlank() {

		FileDefinition def = new FileDefinition( new File( "whatever" ));
		BlockBlank block = new BlockBlank( def, "invalid blank block" );

		FileDefinitionSerializer serializer = new FileDefinitionSerializer();
		String s = serializer.write( block, true );
		Assert.assertEquals( System.getProperty( "line.separator" ), s );
	}


	@Test
	public void testRepairedComment() {

		FileDefinition def = new FileDefinition( new File( "whatever" ));
		BlockComment block = new BlockComment( def, "invalid comment \n\t # followed by a valid \n#comment" );

		FileDefinitionSerializer serializer = new FileDefinitionSerializer();
		String s = serializer.write( block, true );
		String expected = "# invalid comment \n\t # followed by a valid \n#comment\n".replace( "\n", System.getProperty( "line.separator" ));

		Assert.assertEquals( expected, s );
	}
}
