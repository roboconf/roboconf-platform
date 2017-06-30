/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.core.model;

import java.util.Objects;

import net.roboconf.core.errors.ErrorCode;
import net.roboconf.core.errors.ErrorDetails;
import net.roboconf.core.errors.RoboconfError;

/**
 * A model error instantiates and localizes an {@link ErrorCode}.
 * @author Vincent Zurczak - Linagora
 */
public class ModelError extends RoboconfError {
	private final Object modelObject;


	/**
	 * Constructor.
	 * @param errorCode an error code
	 * @param modelObject the model object that contain an error
	 */
	public ModelError( ErrorCode errorCode, Object modelObject ) {
		super( errorCode );
		this.modelObject = modelObject;
	}

	/**
	 * Constructor.
	 * @param errorCode an error code
	 * @param modelObject the model object that contain an error
	 * @param details the error details
	 */
	public ModelError( ErrorCode errorCode, Object modelObject, ErrorDetails... details ) {
		super( errorCode, details );
		this.modelObject = modelObject;
	}

	/**
	 * @return the model object
	 */
	public Object getModelObject() {
		return this.modelObject;
	}

	/* (non-Javadoc)
	 * @see net.roboconf.core.RoboconfError
	 * #equals(java.lang.Object)
	 */
	@Override
	public boolean equals( Object obj ) {
		return super.equals( obj )
				&& Objects.equals( this.modelObject, ((ModelError) obj).modelObject );
	}

	/* (non-Javadoc)
	 * @see net.roboconf.core.RoboconfError
	 * #hashCode()
	 */
	@Override
	public int hashCode() {
		// Keep for Findbugs.
		return super.hashCode();
	}
}
