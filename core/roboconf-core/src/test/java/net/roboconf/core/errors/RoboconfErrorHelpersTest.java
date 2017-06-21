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

package net.roboconf.core.errors;

import static net.roboconf.core.errors.ErrorCode.CMD_CANNOT_HAVE_ANY_PARENT;
import static net.roboconf.core.errors.ErrorCode.PM_DUPLICATE_PROPERTY;
import static net.roboconf.core.errors.ErrorCode.PM_MALFORMED_COMMENT;
import static net.roboconf.core.errors.ErrorCode.PROJ_NO_RESOURCE_DIRECTORY;
import static net.roboconf.core.errors.ErrorCode.REC_ARTIFACT_ID_IN_LOWER_CASE;
import static net.roboconf.core.errors.ErrorCode.REC_SCRIPT_NO_SCRIPTS_DIR;
import static net.roboconf.core.errors.ErrorCode.RM_INVALID_COMPONENT_INSTALLER;
import static net.roboconf.core.errors.ErrorCode.RM_INVALID_VARIABLE_NAME;
import static net.roboconf.core.errors.ErrorCode.RM_ORPHAN_FACET;
import static net.roboconf.core.errors.ErrorCode.RM_ORPHAN_FACET_WITH_CHILDREN;
import static net.roboconf.core.errors.ErrorCode.RM_ROOT_INSTALLER_MUST_BE_TARGET;
import static net.roboconf.core.errors.ErrorCode.RM_UNREACHABLE_COMPONENT;
import static net.roboconf.core.errors.ErrorCode.RM_UNRESOLVABLE_FACET_VARIABLE;
import static net.roboconf.core.errors.ErrorDetails.name;
import static net.roboconf.core.errors.ErrorDetails.unexpected;
import static net.roboconf.core.errors.ErrorDetails.value;
import static net.roboconf.core.errors.ErrorDetails.variable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.core.errors.i18n.TranslationBundle;
import net.roboconf.core.model.ModelError;
import net.roboconf.core.model.ParsingError;
import net.roboconf.core.model.RuntimeModelIo.ApplicationLoadResult;
import net.roboconf.core.model.SourceReference;
import net.roboconf.core.model.beans.Component;

/**
 * @author Vincent Zurczak - Linagora
 */
public class RoboconfErrorHelpersTest {

	@Test
	public void testContainsCriticalErrors() {

		List<RoboconfError> errors = new ArrayList<> ();
		Assert.assertFalse( RoboconfErrorHelpers.containsCriticalErrors( errors ));

		errors.add( new RoboconfError( PM_MALFORMED_COMMENT ));
		Assert.assertFalse( RoboconfErrorHelpers.containsCriticalErrors( errors ));

		errors.add( new RoboconfError( PM_DUPLICATE_PROPERTY ));
		Assert.assertTrue( RoboconfErrorHelpers.containsCriticalErrors( errors ));
	}


	@Test
	public void testFindWarnings() {

		Collection<RoboconfError> errors = new ArrayList<> ();
		Assert.assertEquals( 0, RoboconfErrorHelpers.findWarnings( errors ).size());

		errors.add( new RoboconfError( PM_DUPLICATE_PROPERTY ));
		Assert.assertEquals( 0, RoboconfErrorHelpers.findWarnings( errors ).size());

		errors.add( new RoboconfError( PROJ_NO_RESOURCE_DIRECTORY ));
		errors = RoboconfErrorHelpers.findWarnings( errors );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( PROJ_NO_RESOURCE_DIRECTORY, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testResolveErrorsWithLocation() {

		ApplicationLoadResult alr = new ApplicationLoadResult();
		Component c1 = new Component( "comp1" );
		Component c2 = new Component( "comp2" );

		// Empty case
		Assert.assertEquals( 0, RoboconfErrorHelpers.resolveErrorsWithLocation( alr ).size());

		// Try with an application that contains only a runtime error
		alr.getLoadErrors().add( new RoboconfError( REC_SCRIPT_NO_SCRIPTS_DIR ));
		alr.getLoadErrors().add( new ModelError( RM_INVALID_VARIABLE_NAME, c1 ));
		alr.getLoadErrors().add( new ModelError( RM_INVALID_COMPONENT_INSTALLER, c2 ));

		List<RoboconfError> errors = RoboconfErrorHelpers.resolveErrorsWithLocation( alr );
		Assert.assertEquals( alr.getLoadErrors().size(), errors.size());
		Assert.assertEquals( RoboconfError.class, errors.get( 0 ).getClass());
		Assert.assertEquals( ModelError.class, errors.get( 1 ).getClass());
		Assert.assertEquals( ModelError.class, errors.get( 2 ).getClass());

		// Add location in the context
		alr.getObjectToSource().put( c1, new SourceReference( c1, new File( "whatever" ), 5 ));
		errors = RoboconfErrorHelpers.resolveErrorsWithLocation( alr );

		Assert.assertEquals( alr.getLoadErrors().size(), errors.size());
		Assert.assertEquals( RoboconfError.class, errors.get( 0 ).getClass());
		Assert.assertEquals( ParsingError.class, errors.get( 1 ).getClass());
		Assert.assertEquals( RM_INVALID_VARIABLE_NAME, errors.get( 1 ).getErrorCode());
		Assert.assertEquals( 5, ((ParsingError) errors.get( 1 )).getLine());
		Assert.assertEquals( ModelError.class, errors.get( 2 ).getClass());
	}


	@Test
	public void testFilterErrorsForRecipes() {

		List<RoboconfError> errors = new ArrayList<> ();
		errors.add( new RoboconfError( CMD_CANNOT_HAVE_ANY_PARENT ));
		errors.add( new RoboconfError( RM_ROOT_INSTALLER_MUST_BE_TARGET ));
		errors.add( new RoboconfError( RM_ROOT_INSTALLER_MUST_BE_TARGET ));
		errors.add( new RoboconfError( RM_UNRESOLVABLE_FACET_VARIABLE ));
		errors.add( new RoboconfError( RM_UNREACHABLE_COMPONENT ));
		errors.add( new RoboconfError( RM_ORPHAN_FACET_WITH_CHILDREN ));
		errors.add( new RoboconfError( RM_ORPHAN_FACET ));

		RoboconfErrorHelpers.filterErrorsForRecipes( errors );
		Assert.assertEquals( 1, errors.size());
		Assert.assertEquals( CMD_CANNOT_HAVE_ANY_PARENT, errors.iterator().next().getErrorCode());
	}


	@Test
	public void testFilterErrors() {

		// Original list
		List<RoboconfError> errors = new ArrayList<> ();
		errors.add( new RoboconfError( CMD_CANNOT_HAVE_ANY_PARENT ));
		errors.add( new RoboconfError( RM_ROOT_INSTALLER_MUST_BE_TARGET ));
		errors.add( new RoboconfError( RM_ROOT_INSTALLER_MUST_BE_TARGET ));
		errors.add( new RoboconfError( RM_UNRESOLVABLE_FACET_VARIABLE ));
		errors.add( new RoboconfError( RM_UNREACHABLE_COMPONENT ));
		errors.add( new RoboconfError( RM_ORPHAN_FACET_WITH_CHILDREN ));
		errors.add( new RoboconfError( RM_ORPHAN_FACET ));

		// Remove nothing
		int size = errors.size();
		RoboconfErrorHelpers.filterErrors( errors );
		Assert.assertEquals( size, errors.size());

		// Remove orphan facets codes
		RoboconfErrorHelpers.filterErrors( errors, RM_ORPHAN_FACET );
		Assert.assertEquals( size - 1, errors.size());
		for( RoboconfError error : errors )
			Assert.assertNotEquals( RM_ORPHAN_FACET, error.getErrorCode());

		// Remove "root installer must be target" and "unreachable component" codes
		RoboconfErrorHelpers.filterErrors(
				errors,
				ErrorCode.RM_ROOT_INSTALLER_MUST_BE_TARGET,
				ErrorCode.RM_UNREACHABLE_COMPONENT );

		Assert.assertEquals( size - 4, errors.size());
		for( RoboconfError error : errors ) {
			Assert.assertNotEquals( RM_ORPHAN_FACET, error.getErrorCode());
			Assert.assertNotEquals( RM_ROOT_INSTALLER_MUST_BE_TARGET, error.getErrorCode());
			Assert.assertNotEquals( RM_UNREACHABLE_COMPONENT, error.getErrorCode());
		}

		// Remove something that was not here
		RoboconfErrorHelpers.filterErrors( errors, REC_ARTIFACT_ID_IN_LOWER_CASE );
		Assert.assertEquals( size - 4, errors.size());
	}


	@Test
	public void testFormatError_en() {

		final String msg = "This component cannot have any parent. It cannot be instantiated under this instance.";

		List<RoboconfError> errors = new ArrayList<> ();
		errors.add( new RoboconfError( CMD_CANNOT_HAVE_ANY_PARENT ));
		errors.add( new RoboconfError( CMD_CANNOT_HAVE_ANY_PARENT, name( "oops" )));
		errors.add( new RoboconfError( CMD_CANNOT_HAVE_ANY_PARENT, value( "oops" )));
		errors.add( new RoboconfError( CMD_CANNOT_HAVE_ANY_PARENT, unexpected( "oops." )));
		errors.add( new ParsingError( CMD_CANNOT_HAVE_ANY_PARENT, new File( "/test" ), 24 ));
		errors.add( new ParsingError( CMD_CANNOT_HAVE_ANY_PARENT, new File( "/test" ), 21, name( "you" ), variable( "v" )));

		List<String> errorMessages = new ArrayList<>( RoboconfErrorHelpers.formatErrors( errors, TranslationBundle.EN, true ).values());
		Assert.assertEquals( errors.size(), errorMessages.size());

		Assert.assertEquals(
				"[ " + CMD_CANNOT_HAVE_ANY_PARENT.getCategory().name().toLowerCase() + " ] " + msg,
				errorMessages.get( 0 ));

		Assert.assertEquals(
				"[ " + CMD_CANNOT_HAVE_ANY_PARENT.getCategory().name().toLowerCase() + " ] " + msg + " Name: oops",
				errorMessages.get( 1 ));

		Assert.assertEquals(
				"[ " + CMD_CANNOT_HAVE_ANY_PARENT.getCategory().name().toLowerCase() + " ] " + msg + " Value: oops",
				errorMessages.get( 2 ));

		Assert.assertEquals(
				"[ " + CMD_CANNOT_HAVE_ANY_PARENT.getCategory().name().toLowerCase() + " ] " + msg + " Unexpected: oops.",
				errorMessages.get( 3 ));

		Assert.assertEquals(
				"[ " + CMD_CANNOT_HAVE_ANY_PARENT.getCategory().name().toLowerCase() + " ] " + msg + " File: /test. Line number: 24",
				errorMessages.get( 4 ));

		Assert.assertEquals(
				"[ " + CMD_CANNOT_HAVE_ANY_PARENT.getCategory().name().toLowerCase() + " ] " + msg + " Name: you. Variable: v. File: /test. Line number: 21",
				errorMessages.get( 5 ));
	}


	@Test
	public void testFormatError_fr() {

		final String msg = "Ce composant ne peut pas avoir de parent. Il ne peut pas être instancié sous cette instance.";

		List<RoboconfError> errors = new ArrayList<> ();
		errors.add( new RoboconfError( CMD_CANNOT_HAVE_ANY_PARENT ));
		errors.add( new RoboconfError( CMD_CANNOT_HAVE_ANY_PARENT, name( "oops" )));
		errors.add( new RoboconfError( CMD_CANNOT_HAVE_ANY_PARENT, value( "oops" )));
		errors.add( new RoboconfError( CMD_CANNOT_HAVE_ANY_PARENT, unexpected( "oops." )));
		errors.add( new ParsingError( CMD_CANNOT_HAVE_ANY_PARENT, new File( "/test" ), 24 ));
		errors.add( new ParsingError( CMD_CANNOT_HAVE_ANY_PARENT, new File( "/test" ), 21, name( "you" ), variable( "v" )));

		List<String> errorMessages = new ArrayList<>( RoboconfErrorHelpers.formatErrors( errors, TranslationBundle.FR, false ).values());
		Assert.assertEquals( errors.size(), errorMessages.size());

		Assert.assertEquals( msg, errorMessages.get( 0 ));
		Assert.assertEquals( msg + " Nom : oops", errorMessages.get( 1 ));
		Assert.assertEquals( msg + " Valeur : oops", errorMessages.get( 2 ));
		Assert.assertEquals( msg + " Non-attendu : oops.", errorMessages.get( 3 ));
		Assert.assertEquals( msg + " Fichier : /test. N° de ligne : 24", errorMessages.get( 4 ));
		Assert.assertEquals( msg + " Nom : you. Variable : v. Fichier : /test. N° de ligne : 21", errorMessages.get( 5 ));
	}
}
