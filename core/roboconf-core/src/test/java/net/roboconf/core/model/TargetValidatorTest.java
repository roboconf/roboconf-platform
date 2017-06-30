/**
 * Copyright 2016-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.model;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.Constants;
import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetValidatorTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testValidate_noId() throws Exception {

		TargetValidator tv = new TargetValidator( "" );
		tv.validate();

		Assert.assertEquals( 3, tv.getErrors().size());
		Assert.assertEquals( ErrorCode.REC_TARGET_NO_ID, tv.getErrors().get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.REC_TARGET_NO_HANDLER, tv.getErrors().get( 1 ).getErrorCode());
		Assert.assertEquals( ErrorCode.REC_TARGET_NO_NAME, tv.getErrors().get( 2 ).getErrorCode());
	}


	@Test
	public void testValidate_noHandler() throws Exception {

		TargetValidator tv = new TargetValidator( "id: tid" );
		tv.validate();

		Assert.assertEquals( 2, tv.getErrors().size());
		Assert.assertEquals( ErrorCode.REC_TARGET_NO_HANDLER, tv.getErrors().get( 0 ).getErrorCode());
		Assert.assertEquals( ErrorCode.REC_TARGET_NO_NAME, tv.getErrors().get( 1 ).getErrorCode());
	}


	@Test
	public void testValidate_noName() throws Exception {

		TargetValidator tv = new TargetValidator( "handler: in-memory\nid: tid" );
		tv.validate();

		Assert.assertEquals( 1, tv.getErrors().size());
		Assert.assertEquals( ErrorCode.REC_TARGET_NO_NAME, tv.getErrors().get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_ok() throws Exception {

		TargetValidator tv = new TargetValidator( "handler: in-memory\nid: tid\nname: some name" );
		tv.validate();

		Assert.assertEquals( 0, tv.getErrors().size());
	}


	@Test
	public void testValidate_ok_fromFile() throws Exception {

		File f = this.folder.newFile();
		Utils.writeStringInto( "handler: in-memory\nid: tid\nname: some name", f );

		TargetValidator tv = new TargetValidator( f );
		tv.validate();

		Assert.assertEquals( 0, tv.getErrors().size());
	}


	@Test
	public void testValidate_invalidFile() throws Exception {

		File f = this.folder.newFolder();
		TargetValidator tv = new TargetValidator( f );
		tv.validate();

		Assert.assertEquals( 1, tv.getErrors().size());
		Assert.assertEquals( ErrorCode.REC_TARGET_INVALID_FILE_OR_CONTENT, tv.getErrors().get( 0 ).getErrorCode());
	}


	@Test
	public void testValidate_invalidContent() throws Exception {

		TargetValidator tv = new TargetValidator((String) null);
		tv.validate();

		Assert.assertEquals( 1, tv.getErrors().size());
		Assert.assertEquals( ErrorCode.REC_TARGET_INVALID_FILE_OR_CONTENT, tv.getErrors().get( 0 ).getErrorCode());
	}


	@Test
	public void testParseDirectory_conflictingIds() throws Exception {

		File dir = this.folder.newFolder();
		Utils.writeStringInto( "id: tid\nhandler: in\nname: na", new File( dir, "t1.properties" ));
		Utils.writeStringInto( "id: tid\nhandler: in\nname: na", new File( dir, "t2.properties" ));

		List<ModelError> errors = TargetValidator.parseDirectory( dir );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.REC_TARGET_CONFLICTING_ID, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testParseDirectory_noProperties() throws Exception {

		File dir = this.folder.newFolder();

		List<ModelError> errors = TargetValidator.parseDirectory( dir );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.REC_TARGET_NO_PROPERTIES, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testParseDirectory_ok() throws Exception {

		File dir = this.folder.newFolder();
		Utils.writeStringInto( "id: tid1\nhandler: in\nname: na", new File( dir, "t1.properties" ));
		Utils.writeStringInto( "id: tid2\nhandler: in\nname: na", new File( dir, "t2.properties" ));

		List<ModelError> errors = TargetValidator.parseDirectory( dir );
		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testParseTargetProperties_ok() throws Exception {

		File dir = new File( this.folder.newFolder(), Constants.PROJECT_DIR_GRAPH + "/c" );
		Utils.createDirectory( dir );

		Utils.writeStringInto( "id: tid1\nhandler: in", new File( dir, "t1.properties" ));

		List<ModelError> errors = TargetValidator.parseTargetProperties( dir.getParentFile().getParentFile(), new Component( "c" ));
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( ErrorCode.REC_TARGET_NO_NAME, errors.get( 0 ).getErrorCode());
	}


	@Test
	public void testParseTargetProperties_inexistingDirectory() throws Exception {

		List<ModelError> errors = TargetValidator.parseTargetProperties( new File( "whatever" ), new Component( "c" ));
		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testParseTargetProperties_noFile() throws Exception {

		File dir = new File( this.folder.newFolder(), Constants.PROJECT_DIR_GRAPH + "/c" );
		Utils.createDirectory( dir );

		List<ModelError> errors = TargetValidator.parseTargetProperties( dir, new Component( "c" ));
		Assert.assertEquals( 0, errors.size());
	}


	@Test
	public void testParseTargetProperties_noProperties() throws Exception {

		File dir = new File( this.folder.newFolder(), Constants.PROJECT_DIR_GRAPH + "/c" );
		Utils.createDirectory( dir );

		Utils.writeStringInto( "id: tid1\nhandler: in", new File( dir, "t1.not-properties" ));

		List<ModelError> errors = TargetValidator.parseTargetProperties( dir, new Component( "c" ));
		Assert.assertEquals( 0, errors.size());
	}
}
