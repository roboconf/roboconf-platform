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
import net.roboconf.core.dsl.ParsingConstants;

import org.junit.Test;

/**
 * @author Vincent Zurczak - Linagora
 */
public class BlockInstanceOfTest {

	@Test
	public void testToString() {

		FileDefinition def = new FileDefinition( new File( "toto.txt" ));
		BlockInstanceOf block = new BlockInstanceOf( def );
		Assert.assertNotNull( block.toString());

		block.setName( "my-component" );
		Assert.assertTrue( block.toString().contains( "my-component" ));

		block.getInnerBlocks().add( new BlockProperty( def, ParsingConstants.PROPERTY_INSTANCE_NAME, "foo" ));
		Assert.assertTrue( block.toString().contains( "my-component" ));
		Assert.assertTrue( block.toString().contains( "foo" ));
		Assert.assertTrue( block.toString().contains( " as " ));

		block.getInnerBlocks().clear();
		block.getInnerBlocks().add( new BlockProperty( def, ParsingConstants.PROPERTY_INSTANCE_NAME, "  " ));
		Assert.assertTrue( block.toString().contains( "my-component" ));
		Assert.assertFalse( block.toString().contains( " as " ));
	}
}
