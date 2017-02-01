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

package net.roboconf.swagger;

import java.io.File;

import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.SwaggerDeserializationResult;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ValidateSwaggerJsonFiles {

	/**
	 * @param args
	 */
	public static void main( String[] args ) {

		try {
			new UpdateSwaggerJson().run( args );

		} catch( Exception e ) {
			e.printStackTrace();
			System.exit( 4 );
		}
	}


	/**
	 * The method that does the job.
	 * @param args
	 * @throws Exception
	 */
	public void run( String[] args ) throws Exception {

		// Check
		File baseDirectory = null;
		if( args.length != 1
				|| ! (baseDirectory = new File( args[ 0 ])).exists())
			throw new RuntimeException( "The path of the module's directory was expected as an argument." );

		// Validate
		String[] filesToValidate = {
			"target/docs-rest-api/apidocs/ui/swagger.json",
			"target/docs-rest-api/apidocs/ui/swagger-websocket.json"
		};

		for( String path : filesToValidate ) {
			File f = new File( baseDirectory, path );
			if( ! f.exists())
				throw new RuntimeException( path + " was not found." );

			validate( f );
		}
	}


	/**
	 * Loads and validates (lightly) a Swagger definition.
	 * @param jsonFile the JSon file to parse
	 * @return the Swagger definition
	 * @throws Exception if something went wrong
	 */
	static Swagger validate( File jsonFile ) throws Exception {

		// Validate with the Swagger parser
		SwaggerDeserializationResult res = new SwaggerParser().readWithInfo( jsonFile.getAbsolutePath(), null, true );
		if( ! res.getMessages().isEmpty()) {
			for( String s : res.getMessages())
				System.out.println( s );

			throw new Exception( "Errors were found in a Swagger definition: " + jsonFile.getAbsolutePath());
		}

		return res.getSwagger();
	}
}
