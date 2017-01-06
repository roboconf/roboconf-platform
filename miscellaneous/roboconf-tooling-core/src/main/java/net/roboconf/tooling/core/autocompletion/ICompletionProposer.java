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

package net.roboconf.tooling.core.autocompletion;

import java.util.ArrayList;
import java.util.List;

import net.roboconf.tooling.core.SelectionRange;

/**
 * @author Vincent Zurczak - Linagora
 */
public interface ICompletionProposer {

	/**
	 * Finds proposals from a given offset in a Roboconf file.
	 * @param text the file's content, trimmed until a given offset
	 * @param offset the offset (>= 0)
	 * @return a non-null list of proposals
	 */
	List<RoboconfCompletionProposal> findProposals( String text );


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static class RoboconfCompletionProposal {
		private final String proposalString, proposalName, proposalDescription;
		private final int replacementOffset;
		private final List<SelectionRange> selection = new ArrayList<> ();


		/**
		 * Constructor.
		 * @param proposalString
		 * @param proposalName
		 * @param proposalDescription
		 * @param replacementOffset
		 */
		public RoboconfCompletionProposal(
				String proposalString,
				String proposalName,
				String proposalDescription,
				int replacementOffset ) {

			this.proposalString = proposalString;
			this.proposalName = proposalName;
			this.proposalDescription = proposalDescription;
			this.replacementOffset = replacementOffset;
		}

		public String getProposalString() {
			return proposalString;
		}

		public String getProposalName() {
			return proposalName;
		}

		public String getProposalDescription() {
			return proposalDescription;
		}

		public int getReplacementOffset() {
			return replacementOffset;
		}

		public List<SelectionRange> getSelection() {
			return selection;
		}

		@Override
		public String toString() {
			return proposalName;
		}
	}
}
