/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.target.ec2.internal;

import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

import net.roboconf.core.utils.Utils;
import net.roboconf.target.api.AbstractThreadedTargetHandler.MachineConfigurator;
import net.roboconf.target.api.TargetException;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Tag;

/**
 * A machine configurator for EC2.
 * @author Vincent Zurczak - Linagora
 */
public class Ec2MachineConfigurator implements MachineConfigurator {

	/**
	 * The steps of a workflow.
	 * <ul>
	 * <li>UNKNOWN_VM: the VM must be known by all the EC2 parts.</li>
	 * <li>TAG_VM: tag the VM.</li>
	 * <li>ASSOCIATE_ELASTIC_IP: an elastic IP has to be associated.</li>
	 * <li>RUNNING_VM: the VM must be running (started).</li>
	 * <li>ASSOCIATE_STORAGE: associate storage.</li>
	 * <li>COMPLETE: there is nothing to do anymore.</li>
	 * </ul>
	 *
	 * @author Vincent Zurczak - Linagora
	 */
	public static enum State {
		UNKNOWN_VM, TAG_VM, RUNNING_VM, ASSOCIATE_ELASTIC_IP, ASSOCIATE_STORAGE, COMPLETE;
	}

	private final String machineId, tagName;
	private final Map<String,String> targetProperties;
	private final Logger logger = Logger.getLogger( getClass().getName());

	private AmazonEC2 ec2Api;
	private State state = State.UNKNOWN_VM;



	/**
	 * Constructor.
	 * @param targetProperties
	 * @param machineId
	 * @param tagName
	 */
	public Ec2MachineConfigurator( Map<String,String> targetProperties, String machineId, String tagName  ) {
		this.machineId = machineId;
		this.targetProperties = targetProperties;
		this.tagName = tagName;
	}


	@Override
	public boolean configure() throws TargetException {

		if( this.ec2Api == null )
			this.ec2Api = Ec2IaasHandler.createEc2Client( this.targetProperties );

		if( this.state == State.UNKNOWN_VM )
			if( checkVmIsKnown())
				this.state = State.TAG_VM;

		if( this.state == State.TAG_VM )
			if( tagVm())
				this.state = State.RUNNING_VM;

		if( this.state == State.RUNNING_VM )
			if( checkVmIsStarted())
				this.state = State.ASSOCIATE_ELASTIC_IP;

		if( this.state == State.ASSOCIATE_ELASTIC_IP )
			if( associateElasticIp())
				this.state = State.ASSOCIATE_STORAGE;

		if( this.state == State.ASSOCIATE_STORAGE )
			if( associateStorage())
				this.state = State.COMPLETE;

		return this.state == State.COMPLETE;
	}


	/**
	 * Checks whether a VM is known (i.e. all the EC2 parts know it).
	 * @return true if the VM is know, false otherwise
	 */
	private boolean checkVmIsKnown() {

		DescribeInstancesRequest dis = new DescribeInstancesRequest();
		dis.setInstanceIds( Arrays.asList( this.machineId ));

		DescribeInstancesResult disresult = this.ec2Api.describeInstances(dis);
		return disresult.getReservations().size() > 0
				&& disresult.getReservations().get( 0 ).getInstances().size() > 0;
	}


	/**
	 * Tags the VM (basically, it gives it a name).
	 * @return true if the tag was done, false otherwise
	 */
	private boolean tagVm() {

		// We cannot tag directly after the VM creation. See #197.
		// We need it to be known by all the EC2 components.
		Tag tag = new Tag( "Name", this.tagName );
		CreateTagsRequest ctr = new CreateTagsRequest( Arrays.asList( this.machineId ), Arrays.asList( tag ));
		this.ec2Api.createTags( ctr );

		return true;
	}


	/**
	 * Associates an elastic IP with the VM.
	 * @return true if there is nothing more to do about elastic IP configuration, false otherwise
	 */
	private boolean associateElasticIp() {

		String elasticIp = this.targetProperties.get( Ec2Constants.ELASTIC_IP );
		if( ! Utils.isEmptyOrWhitespaces( elasticIp )) {
			this.logger.fine( "Associating an elastic IP with the instance. IP = " + elasticIp );
			AssociateAddressRequest associateAddressRequest = new AssociateAddressRequest( this.machineId, elasticIp );
			this.ec2Api.associateAddress( associateAddressRequest );
		}

		return true;
	}


	/**
	 * Checks whether a VM is started or not (which is stronger than {@link #checkVmIsKnown()}).
	 * @return true if the VM is started, false otherwise
	 */
	private boolean checkVmIsStarted() {

		DescribeInstancesRequest dis = new DescribeInstancesRequest();
		dis.setInstanceIds( Arrays.asList( this.machineId ));

		DescribeInstancesResult disresult = this.ec2Api.describeInstances( dis );
		return "running".equalsIgnoreCase( disresult.getReservations().get(0).getInstances().get(0).getState().getName());
	}


	/**
	 * Associates storage with the VM.
	 * @return true if there is nothing more to do about elastic IP configuration, false otherwise
	 */
	private boolean associateStorage() {

		String elasticIp = this.targetProperties.get( Ec2Constants.ELASTIC_IP );
		if( ! Utils.isEmptyOrWhitespaces( elasticIp )) {
			this.logger.fine( "Associating an elastic IP with the instance. IP = " + elasticIp );
			AssociateAddressRequest associateAddressRequest = new AssociateAddressRequest( this.machineId, elasticIp );
			this.ec2Api.associateAddress( associateAddressRequest );
		}

		return true;
	}
}
