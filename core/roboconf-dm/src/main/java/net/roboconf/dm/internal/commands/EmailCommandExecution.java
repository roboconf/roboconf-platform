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

package net.roboconf.dm.internal.commands;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import net.roboconf.core.commands.EmailCommandInstruction;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.api.IPreferencesMngr;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
class EmailCommandExecution extends AbstractCommandExecution {

	private final EmailCommandInstruction instr;
	private final Manager manager;


	/**
	 * Constructor.
	 * @param instr
	 * @param manager
	 */
	public EmailCommandExecution( EmailCommandInstruction instr, Manager manager ) {
		this.instr = instr;
		this.manager = manager;
	}


	@Override
	public void execute() throws CommandException {

		try {
			Message message = getMessageToSend();
			Transport.send( message );

		} catch( Exception e ) {
			throw new CommandException( e );
		}
	}


	/**
	 * @return the message to send
	 * @throws MessagingException
	 */
	Message getMessageToSend() throws MessagingException {

		// Subject and message
		Properties mailProperties = this.manager.preferencesMngr().getJavaxMailProperties();
		String subject = "Roboconf event";
		String data = this.instr.getMsg();

		final String emailSubjectPattern = "Subject: ([^\n]+)(\n|$)(.*)";
		Matcher m = Pattern.compile( emailSubjectPattern ).matcher( this.instr.getMsg());
		if( m.find()) {
			subject = m.group( 1 );
			data = m.group( 3 ).trim();
		}

		// Credentials
		String username = mailProperties.getProperty( IPreferencesMngr.JAVAX_MAIL_SMTP_USER, "" );
		String password = mailProperties.getProperty( IPreferencesMngr.JAVAX_MAIL_SMTP_PWD, "" );

		// Obtain mail session object
		Session session = null;
		if("true".equalsIgnoreCase( mailProperties.getProperty( IPreferencesMngr.JAVAX_MAIL_SMTP_AUTH )))
			session = Session.getInstance( mailProperties, new MailAuthenticator( username, password ));
		else
			session = Session.getDefaultInstance( mailProperties );

		// Create a default MimeMessage object
		MimeMessage message = new MimeMessage(session);

		// Set From: and To: header fields
		message.setFrom( new InternetAddress( mailProperties.getProperty( IPreferencesMngr.JAVAX_MAIL_FROM )));

		Set<String> tos = new LinkedHashSet<> ();
		tos.addAll( this.instr.getTos());

		String defaultRecipients = this.manager.preferencesMngr().get( IPreferencesMngr.EMAIL_DEFAULT_RECIPIENTS, "" );
		List<String> recipients = Utils.splitNicely( defaultRecipients, "," );
		tos.addAll( recipients );

		for( String to : tos )
			message.addRecipient( Message.RecipientType.TO, new InternetAddress( to ));

		message.setSubject( subject );
		message.setText( data.trim());

		return message;
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

		@Override
		protected PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(this.username, this.password);
		}
	}
}
