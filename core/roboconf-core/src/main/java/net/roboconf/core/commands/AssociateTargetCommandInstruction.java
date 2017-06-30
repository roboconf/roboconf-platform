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

import static net.roboconf.core.errors.ErrorDetails.instance;
import static net.roboconf.core.errors.ErrorDetails.value;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class AssociateTargetCommandInstruction extends AbstractCommandInstruction {

	static final String PREFIX = "associate";
	private String targetId, scopedInstancePath;


	/**
	 * Constructor.
	 * @param context
	 * @param instruction
	 * @param line
	 */
	AssociateTargetCommandInstruction( Context context, String instruction, int line ) {
		super( context, instruction, line );

		Pattern p = Pattern.compile( PREFIX + "\\s+(/.*)\\s+with\\b(.*)", Pattern.CASE_INSENSITIVE );
		Matcher m = p.matcher( instruction );
		if( m.matches()) {
			this.syntaxicallyCorrect = true;
			this.targetId = m.group( 2 ).trim();
			this.scopedInstancePath = m.group( 1 ).trim();
		}
	}


	/*
	 * (non-Javadoc)
	 * @see net.roboconf.core.commands.AbstractCommandInstruction#doValidate()
	 */
	@Override
	public List<ParsingError> doValidate() {

		List<ParsingError> result = new ArrayList<> ();
		if( Utils.isEmptyOrWhitespaces( this.targetId ))
			result.add( error( ErrorCode.CMD_INVALID_TARGET_ID, value( this.targetId )));

		Instance resolvedInstance;
		if(( resolvedInstance = this.context.resolveInstance( this.scopedInstancePath )) == null)
			result.add( error( ErrorCode.CMD_NO_MATCHING_INSTANCE, instance( this.scopedInstancePath )));
		else if( ! InstanceHelpers.isTarget( resolvedInstance ))
			result.add( error( ErrorCode.CMD_NOT_A_SCOPED_INSTANCE ));

		return result;
	}


	/**
	 * @return the targetId
	 */
	public String getTargetId() {
		return this.targetId;
	}


	/**
	 * @return the scopedInstancePath
	 */
	public String getScopedInstancePath() {
		return this.scopedInstancePath;
	}
}
