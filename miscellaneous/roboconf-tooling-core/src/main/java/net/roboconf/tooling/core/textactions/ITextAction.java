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

package net.roboconf.tooling.core.textactions;

/**
 * @author Vincent Zurczak - Linagora
 */
public interface ITextAction {

	/**
	 * Builds a new text from a text selection.
	 * @param text a non-null text
	 * @param selectionOffset the selection offset (-1 for the whole selection)
	 * @param selectionLength the selection length
	 * @return a non-null text
	 */
	String update( String text, int selectionOffset, int selectionLength );

	/**
	 * Gives the location of the cursor in the content returned by {@link #update(String, int, int)}.
	 * @return an integer in the [0, new text's length] range
	 */
	int getNewCursorPosition();
}
