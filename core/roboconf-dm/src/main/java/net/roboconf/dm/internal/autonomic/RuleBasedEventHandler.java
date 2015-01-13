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

package net.roboconf.dm.internal.autonomic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.messaging.messages.from_agent_to_dm.MsgNotifAutonomic;

/**
 * An event handler to evaluate rules.
 * <p>
 * This handler can create new instances, including virtual machines.
 * It can also delete instances it has created.
 * </p>
 *
 * @author Pierre-Yves Gibello - Linagora
 */
public class RuleBasedEventHandler {

	static final String DELETE_SERVICE = "delete-service";
	static final String REPLICATE_SERVICE = "replicate-service";
	static final String MAIL = "mail";

	private final Manager manager;
	private final Logger logger = Logger.getLogger( getClass().getName());
	final Map<String,List<Instance>> componentNameToCreatedRootInstances;


	/**
	 * Create a new rule-based event handler.
	 * @param manager the application manager
	 */
	public RuleBasedEventHandler( Manager manager ) {
		this.componentNameToCreatedRootInstances = new HashMap<String,List<Instance>> ();
		this.manager = manager;
	}


	/**
	 * React upon autonomic monitoring message (aka "autonomic event").
	 * @param event The autonomic event message
	 * @throws Exception
	 */
	public void handleEvent( ManagedApplication ma, MsgNotifAutonomic event ) {

		try {
			Map<String,AutonomicRule> rules = RulesParser.parseRules( ma );
			AutonomicRule rule = rules.get( event.getEventId());

			if( rule == null )
				this.logger.fine( "No rule was found to handle events with the '" + event.getEventId() + "' ID." );

			// EVENT_ID ReplicateService ComponentTemplate
			else if( REPLICATE_SERVICE.equalsIgnoreCase( rule.getReactionId()))
				createInstances( ma, rule.getReactionInfo());

			// EVENT_ID StopService ComponentName
			else if( DELETE_SERVICE.equalsIgnoreCase(rule.getReactionId()))
				deleteInstances( ma, rule.getReactionInfo());

			// EVENT_ID Mail DestinationEmail
			else if( MAIL.equalsIgnoreCase(rule.getReactionId())) {
				sendEmail(ma, rule.getReactionInfo());

			// EVENT_ID Log LogMessage
			// And default behavior...
			} else {
				this.logger.info( "AUTONOMIC Monitoring event. Info = " + rule.getReactionInfo());
			}

		} catch( IOException e ) {
			this.logger.warning( "An autonomic event could not be handled. " + e.getMessage());
			Utils.logException( this.logger, e );
		}
	}

	/**
	 * Send an email, according to credentials and configuration retrieved
	 * from a properties file (rules.cfg.properties).
	 * @param ma the managed application
	 * @param emailData the email message (may start with "Subject: {subject}\n)
	 */
	void sendEmail(ManagedApplication ma, String emailData) {
		/*
		 * Sample properties:
		 * mail.from: me@mydomain.com
		 * mail.smtp.auth: true
		 * mail.user: me
		 * mail.password: mypassword
		 * mail.smtp.starttls.enable: true
		 * mail.smtp.host: my.mail.server
		 * mail.smtp.port: 1234
		 */
		Properties props = new Properties();
		props.put("mail.from", "dm@roboconf.net");
		props.put("mail.smtp.host", "localhost");

		File propFile = new File(ma.getApplicationFilesDirectory(), Constants.PROJECT_DIR_AUTONOMIC + "/" + Constants.FILE_RULES + ".properties");
		if( propFile.isFile()) {
			FileInputStream in = null;
			try {
				in = new FileInputStream(propFile);
				props.load(in);
			} catch (Exception e) {
				this.logger.warning("Configuration file for email settings access problem: " + e.getMessage());
				Utils.logException(this.logger, e);
			} finally {
				Utils.closeQuietly(in);
			}
		}

		String to = props.getProperty("mail.to");
		if(to == null) {
			this.logger.warning("Can\'t send email: no destination (mail.to) specified");
			return;
		}

		String s = props.getProperty("mail.user");
		String username = (s == null ? "" : s);
		s = props.getProperty("mail.password");
		String password = (s == null ? "" : s);

		// Trust mail server (should not be null, default value set to "localhost" above)
		props.put("mail.smtp.ssl.trust", props.get("mail.smtp.host"));

		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");

		// Obtain mail session object
		Session session = null;
		if("true".equals(props.getProperty("mail.smtp.auth"))) {
			session = Session.getInstance(props,
					new MailAuthenticator(username, password));
		} else {
			session = Session.getDefaultInstance(props);
		}

		try {
			// Create a default MimeMessage object
			MimeMessage message = new MimeMessage(session);

			// Set From: and To: header fields
			message.setFrom(new InternetAddress(props.getProperty("mail.from")));
			message.addRecipient(Message.RecipientType.TO,
					new InternetAddress(to));

			// Determine subject + message text
			String subject = "Roboconf event";
			String data = emailData;
			if(emailData.startsWith("Subject:")) {
				int pos = emailData.indexOf("\n");
				// 8 = "Subject:".length()
				subject += ": " + (pos > 0
						? emailData.substring(8, pos).trim()
						: emailData.substring(8).trim());
				if(pos > 0) data = emailData.substring(pos+1);
			}
			
			message.setSubject(subject);
			message.setText(data.trim());

			// Send email !
			Transport.send(message);

		} catch (MessagingException e) {
			this.logger.warning( "Failed sending email: " + e.getMessage());
			Utils.logException(this.logger, e);
		}
	}

	/**
	 * Instantiate a new VM with instances on it.
	 * @param ma the managed application
	 * @param componentTemplates the component names, separated by "/"
	 * <p>
	 * (e.g. VM_name/Software_container_name/App_artifact_name)
	 * </p>
	 */
	void createInstances( ManagedApplication ma, String componentTemplates ) {

		try {
			if( componentTemplates.startsWith( "/" ))
				componentTemplates = componentTemplates.substring( 1 );

			String templates[] = componentTemplates.split( "/" );

			// First check that all component to instantiate are valid and found...
			// Necessary, not to create a VM then try to instantiate a fake component there!
			for( String s : templates) {
				Component compToInstantiate = ComponentHelpers.findComponent( ma.getApplication().getGraphs(), s);
				if( compToInstantiate == null )
					throw new IOException( "Component " + s + " was not found in application " + ma.getApplication().getName());
			}

			// We register new instances in the model
			Instance previousInstance = null;
			for( String s : templates) {
				Component compToInstantiate = ComponentHelpers.findComponent( ma.getApplication().getGraphs(), s);
				// compToInstantiate should never be null (check done above).

				// All the root instances must have a different name. Others do not matter.
				String instanceName;
				if( previousInstance == null )
					instanceName = compToInstantiate.getName() + "_" + System.currentTimeMillis();
				else
					instanceName = compToInstantiate.getName().toLowerCase();

				Instance currentInstance = new Instance( instanceName ).component(compToInstantiate);
				this.manager.addInstance( ma, previousInstance, currentInstance );
				previousInstance = currentInstance;
			}

			// Now, deploy and start all
			Instance rootInstance = InstanceHelpers.findRootInstance( previousInstance );
			this.manager.deployAndStartAll( ma, rootInstance );

			// Remember the VM this class has created
			String componentName = previousInstance.getName();
			List<Instance> vmList = this.componentNameToCreatedRootInstances.get( componentName );
			if(vmList == null)
				vmList = new ArrayList<Instance>();

			vmList.add( rootInstance );
			this.componentNameToCreatedRootInstances.put( componentName, vmList );

		} catch( Exception e ) {
			this.logger.warning( "The creation of instances (autonomic context) failed. " + e.getMessage());
			Utils.logException( this.logger, e );
		}
	}


	/**
	 * Deletes an instance and the VM that hosts it.
	 * <p>
	 * Only instances that were created by this class can be undeployed and removed from the model.
	 * </p>
	 *
	 * @param ma the managed application
	 * @param componentName the component name of an instance to delete
	 */
	void deleteInstances( ManagedApplication ma, String componentName ) {

		try {
			List<Instance> vmList = this.componentNameToCreatedRootInstances.get( componentName );
			if( vmList != null ) {
				if( vmList.size() <= 1 )
					this.componentNameToCreatedRootInstances.remove( componentName );

				Instance vmInstance = vmList.remove( 0 );
				this.manager.undeployAll( ma, vmInstance );
				this.manager.removeInstance( ma, vmInstance );
			}

		} catch( Exception e ) {
			this.logger.warning( "The deletion of an instance (autonomic context) failed. " + e.getMessage());
			Utils.logException( this.logger, e );
		}
	}

	/**
	 * @author Pierre-Yves Gibello - Linagora
	 */
	static class MailAuthenticator extends javax.mail.Authenticator {
		String username = "";
		String password = "";

		public MailAuthenticator(String username, String password) {
			super();
			this.username = username;
			this.password = password;
		}

		protected PasswordAuthentication getPasswordAuthentication() {
  		  return new PasswordAuthentication(this.username, this.password);
  	  	}
	}

}
