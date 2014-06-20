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

package net.roboconf.core.internal.model.parsing;

import java.io.File;

import junit.framework.Assert;
import net.roboconf.core.model.parsing.BlockBlank;
import net.roboconf.core.model.parsing.BlockComment;
import net.roboconf.core.model.parsing.FileDefinition;

import org.junit.Test;

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
