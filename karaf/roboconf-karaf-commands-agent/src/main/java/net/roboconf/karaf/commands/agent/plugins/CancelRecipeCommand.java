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

package net.roboconf.karaf.commands.agent.plugins;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import net.roboconf.agent.AgentMessagingInterface;
import net.roboconf.core.utils.ProcessStore;
import net.roboconf.core.utils.Utils;

/**
 * @author Pierre-Yves Gibello - Linagora
 */
@Command( scope = "roboconf", name = "cancel-recipe", description="Cancel a running recipe." )
@Service
public class CancelRecipeCommand implements Action {

	@Argument( index = 0, name = "applicationName", description = "The application name.", required = false, multiValued = false )
	String applicationName = null;

	@Argument( index = 1, name = "scopedInstancePath", description = "The root instance path.", required = false, multiValued = false )
	String scopedInstancePath = null;

	/*
	 * Possible configurations:
	 * (*) The DM is alone in its distribution.
	 * (*) An agent is alone in its distribution.
	 * (*) The DM and in-memory agents coexist in the same distribution.
	 *
	 * So, we need to inject all the available reconfigurables.
	 * No need to add complexity with parameters. Reconfigure everything.
	 */
	@Reference
	List<AgentMessagingInterface> agents;

	// Other fields
	PrintStream out = System.out;



	@Override
	public Object execute() throws Exception {

		if(this.agents != null) {
			boolean single = (this.agents.size() == 1);

			for(AgentMessagingInterface agent : this.agents) {
				this.out.println("Found agent with appname=" + agent.getApplicationName());

				if(single) {
					if(this.scopedInstancePath == null || this.scopedInstancePath.equals(agent.getScopedInstancePath())) {
						this.out.println("Single agent eligible to cancel recipe");
						cancelRecipe(agent.getApplicationName(), agent.getScopedInstancePath());
					}

				} else if(Objects.equals(agent.getApplicationName(), this.applicationName)) {
					if(this.scopedInstancePath == null || this.scopedInstancePath.equals(agent.getScopedInstancePath())) {
						// Found agent eligible to cancel recipe (if any)
						this.out.println("Agent is eligible to cancel recipe");
						cancelRecipe(this.applicationName, this.scopedInstancePath);
					}
				}

			}

		} else {
			this.out.println("No agent found, command not applicable");
		}

		return null;
	}


	/**
	 * Cancels running recipe, if any.
	 */
	private void cancelRecipe(String applicationName, String scopedInstancePath) {
		if(Utils.isEmptyOrWhitespaces(applicationName)) applicationName = "";
		if(Utils.isEmptyOrWhitespaces(scopedInstancePath)) scopedInstancePath = "";

		this.out.println("looking up [" + applicationName + "] [" + scopedInstancePath + "]");
		Process p = ProcessStore.getProcess(applicationName, scopedInstancePath);
		if(p != null) {
			p.destroy();
			ProcessStore.clearProcess(applicationName, scopedInstancePath);
			this.out.println("Recipe cancelled !");
		} else {
			this.out.println("No running recipe to cancel.");
		}
	}
}
