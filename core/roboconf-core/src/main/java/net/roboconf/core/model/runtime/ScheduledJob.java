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

package net.roboconf.core.model.runtime;

import java.util.Objects;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ScheduledJob implements Comparable<ScheduledJob> {

	private String jobId, jobName, appName, cmdName, cron;


	/**
	 * Constructor.
	 */
	public ScheduledJob() {
		// nothing
	}


	/**
	 * Constructor.
	 * @param jobId the job's ID
	 */
	public ScheduledJob( String jobId ) {
		this.jobId = jobId;
	}

	/**
	 * @param jobId the jobId to set
	 */
	public void setJobId( String jobId ) {
		this.jobId = jobId;
	}

	/**
	 * @return the jobId
	 */
	public String getJobId() {
		return this.jobId;
	}

	/**
	 * @return the jobName
	 */
	public String getJobName() {
		return this.jobName;
	}

	/**
	 * @param jobName the jobName to set
	 */
	public void setJobName( String jobName ) {
		this.jobName = jobName;
	}

	/**
	 * @return the appName
	 */
	public String getAppName() {
		return this.appName;
	}

	/**
	 * @param appName the appName to set
	 */
	public void setAppName( String appName ) {
		this.appName = appName;
	}

	/**
	 * @return the cmdName
	 */
	public String getCmdName() {
		return this.cmdName;
	}

	/**
	 * @param cmdName the cmdName to set
	 */
	public void setCmdName( String cmdName ) {
		this.cmdName = cmdName;
	}

	/**
	 * @return the cron
	 */
	public String getCron() {
		return this.cron;
	}

	/**
	 * @param cron the cron to set
	 */
	public void setCron( String cron ) {
		this.cron = cron;
	}


	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable
	 * #compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo( ScheduledJob job ) {

		int result;
		if( this.jobName == null && job.jobName == null )
			result = 0;
		else if( this.jobName == null )
			result = -1;
		else if( job.jobName == null )
			result = 1;
		else
			result = this.jobName.compareTo( job.jobName );

		return result;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals( Object obj ) {
		return obj instanceof ScheduledJob
				&& Objects.equals(((ScheduledJob) obj).jobName, this.jobName );
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.jobName == null ? 31 : this.jobName.hashCode();
	}
}
