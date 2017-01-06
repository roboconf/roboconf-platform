/**
 * Copyright 2015-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.templating.internal.helpers;

import java.util.HashMap;

import org.junit.Assert;

import org.junit.Test;
import org.mockito.Mockito;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.TagType;
import com.github.jknack.handlebars.Template;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AllHelperTest {

	@Test
	public void testUnsupportedContext_unknownType() throws Exception {

		Context ctx = Context.newContext( new Object());
		Handlebars handlebars = Mockito.mock( Handlebars.class );
		Template tpl = Mockito.mock(  Template.class  );
		Options opts = new Options(
				handlebars, "helper", TagType.SECTION, ctx, tpl, tpl,
				new Object[ 0 ],
				new HashMap<String,Object>( 0 ));

		AllHelper helper = new AllHelper();
		Assert.assertEquals( "", helper.apply( null, opts ));
	}


	@Test
	public void testUnsupportedContext_nullContext() throws Exception {

		Context ctx = Context.newContext( null );
		Handlebars handlebars = Mockito.mock( Handlebars.class );
		Template tpl = Mockito.mock(  Template.class  );
		Options opts = new Options(
				handlebars, "helper", TagType.SECTION, ctx, tpl, tpl,
				new Object[ 0 ],
				new HashMap<String,Object>( 0 ));

		AllHelper helper = new AllHelper();
		Assert.assertEquals( "", helper.apply( null, opts ));
	}
}
