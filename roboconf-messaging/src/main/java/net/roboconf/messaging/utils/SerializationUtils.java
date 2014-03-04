/**
 * Copyright 2013-2014 Linagora, Université Joseph Fourier
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

package net.roboconf.messaging.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Logger;

import net.roboconf.core.internal.utils.Utils;
import net.roboconf.messaging.MessagingConstants;
import net.roboconf.messaging.messages.Message;

/**
 * @author Noël - LIG
 */
public final class SerializationUtils {

	private static final Logger LOGGER = Logger.getLogger( MessagingConstants.ROBOCONF_LOGGER_NAME );


	/**
	 * Empty private constructor.
	 */
	private SerializationUtils() {
		// nothing
	}


	/**
	 * Serializes an object.
	 * @param object a serializable object
	 * @return a non-null array of bytes
	 */
	public static <T extends Serializable> byte[] serializeObject( T object ) {

		byte[] result;
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream( os );
			out.writeObject( object );
			result = os.toByteArray();

		} catch( IOException e ) {
			LOGGER.severe( "An object could not be serialized. " + e.getMessage());
			LOGGER.finest( Utils.writeException( e ));
			result = new byte[ 0 ];
		}

		return result;
	}


	/**
	 * Deserializes an object.
	 * @param bytes a non-null array of bytes
	 * @param clazz the class of the expected object
	 * @return the deserialized object, or null if it failed
	 */
	public static <T extends Serializable> T deserializeObject( byte[] bytes, Class<T> clazz ) {

		T result = null;
		try {
			ByteArrayInputStream is = new ByteArrayInputStream( bytes );
			ObjectInputStream deserializer = new ObjectInputStream( is );
			result = clazz.cast( deserializer.readObject());

		} catch( IOException e ) {
			LOGGER.severe( "An object could not be serialized. " + e.getMessage());
			LOGGER.finest( Utils.writeException( e ));

		} catch( ClassNotFoundException e ) {
			LOGGER.severe( "An object could not be serialized. " + e.getMessage());
			LOGGER.finest( Utils.writeException( e ));
		}

		return result;
	}


	/**
	 * Deserializes a message.
	 * @param bytes a non-null array of bytes
	 * @return the deserialized message, or null if it failed
	 */
	public static Message deserializeObject( byte[] bytes ) {
		return deserializeObject( bytes, Message.class );
	}
}
