/**
 * Copyright 2013-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.http.internal.messages;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import net.roboconf.messaging.api.messages.Message;

/**
 * This class is a partial copy of SerializationUtils.
 * <p>
 * This really sucks. We cannot use SerializationUtils with HTTP's custom
 * message implementation because of class loading issues. When deserializing
 * a HTTP message roa subscription message, we get a ClassNotFound exception.
 * </p>
 * <p>
 * A workaround was suggested on Stack Overflow:
 * http://stackoverflow.com/questions/13861342/how-do-you-deserialize-an-object-from-bytes-in-osgi
 * </p>
 * <p>
 * However, it is not more simple. So, this bundle provides its own deserializing method.
 * </p>
 *
 * @author Noël - LIG
 */
public final class HttpSerializationUtils {

	/**
	 * Empty private constructor.
	 */
	private HttpSerializationUtils() {
		// nothing
	}


	/**
	 * Deserializes a message.
	 * @param bytes a non-null array of bytes
	 * @return the deserialized message, or null if it failed
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static Message deserializeObject( byte[] bytes )
	throws IOException, ClassNotFoundException {

		ByteArrayInputStream is = new ByteArrayInputStream( bytes );
		ObjectInputStream deserializer = new ObjectInputStream( is );
		return (Message) deserializer.readObject();
	}
}
