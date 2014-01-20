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

package net.roboconf.core.model.parsing;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A bean that contains information about a 'relations' file.
 * @author Vincent Zurczak - Linagora
 */
public class FileRelations extends AbstractFile {

	private final List<AbstractInstruction> instructions = new ArrayList<AbstractInstruction> ();


	/**
	 * Constructor.
	 * @param editedFile
	 */
	public FileRelations( File editedFile ) {
		super( editedFile );
	}

	/**
	 * Constructor.
	 * @param fileLocation
	 */
	public FileRelations( URI fileLocation ) {
		super( fileLocation );
	}

	/**
	 * @return the instructions
	 */
	public Collection<AbstractInstruction> getInstructions() {
		return this.instructions;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		String name = getFileLocation().getPath();
		int index = name.lastIndexOf( '/' );
		if( index > 0 )
			name = name.substring( index );

		StringBuilder sb = new StringBuilder();
		sb.append( name );
		sb.append( " with " );
		sb.append( this.instructions.size());
		sb.append( " instructions" );

		return sb.toString();
	}
}
