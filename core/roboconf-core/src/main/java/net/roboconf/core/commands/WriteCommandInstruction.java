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

package net.roboconf.core.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class WriteCommandInstruction extends AbstractCommandInstruction {

	static final String TMP = "%TMP%";
	static final String WRITE_PREFIX = "write";

	private String content, filePath;


	/**
	 * Constructor.
	 * @param context
	 * @param instruction
	 * @param line
	 */
	WriteCommandInstruction( Context context, String instruction, int line ) {
		super( context, instruction, line );

		Pattern p = Pattern.compile( getPrefix() + "\\s+(.*)\\s*into\\b(.*)", Pattern.CASE_INSENSITIVE );
		Matcher m = p.matcher( instruction );
		if( m.matches()) {
			this.syntaxicallyCorrect = true;
			this.content = m.group( 1 ).trim();
			this.filePath = m.group( 2 ).trim().replaceAll( TMP, findTemporaryDir());
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.core.commands.AbstractCommandInstruction#doValidate()
	 */
	@Override
	public List<ParsingError> doValidate() {

		List<ParsingError> result = new ArrayList<> ();
		if( Utils.isEmptyOrWhitespaces( this.filePath ))
			result.add( new ParsingError( ErrorCode.CMD_MISSING_TARGET_FILE, this.context.getCommandFile(), this.line ));

		return result;
	}


	/**
	 * @return the content
	 */
	public String getContent() {
		return this.content;
	}


	/**
	 * @return the filePath
	 */
	public String getFilePath() {
		return this.filePath;
	}


	/**
	 * @return the prefix to look for
	 */
	protected String getPrefix() {
		return WriteCommandInstruction.WRITE_PREFIX;
	}


	/**
	 * @return the location of the temporary directory WITHOUT a slash at the end
	 */
	static String findTemporaryDir() {

		String tmpDir = System.getProperty( "java.io.tmpdir" );
		tmpDir = tmpDir.replaceAll( "/$", "" );

		return tmpDir;
	}
}
