/**
 * Copyright 2017 Linagora, Université Joseph Fourier, Floralis
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

import java.io.File;
import java.util.Objects;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Facet;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.utils.Utils;

/**
 * Contextual information for {@link ErrorCode}s.
 * <p>
 * Previously, error details were passed as string. The problem is
 * that these strings were not translatable. This class solves this issue,
 * as such errors may be used in both documentation generators and web tools.
 * </p>
 *
 * @author Vincent Zurczak - Linagora
 */
public class ErrorDetails {

	/**
	 * A translatable (i18n) enumeration of error kinds.
	 * @author Vincent Zurczak - Linagora
	 */
	public enum ErrorDetailsKind {
		INSTANCE,
		VARIABLE,
		COMPONENT,
		FACET,
		INSTRUCTION,
		FILE,
		LINE,
		CYCLE,
		APPLICATION,
		APPLICATION_TEMPLATE,
		DIRECTORY,
		UNRECOGNIZED,
		EXPECTED,
		UNEXPECTED,
		ALREADY_DEFINED,
		CONFLICTING,
		MALFORMED,
		EXCEPTION,
		EXCEPTION_NAME,
		LOG_REFERENCE,
		NAME,
		VALUE;
	}


	private final String elementName;
	private final ErrorDetailsKind errorDetailsKind;



	/**
	 * Constructor.
	 * @param elementName
	 * @param errorDetailsKind
	 */
	protected ErrorDetails( String elementName, ErrorDetailsKind errorDetailsKind ) {
		Objects.requireNonNull( errorDetailsKind );
		this.elementName = elementName;
		this.errorDetailsKind = errorDetailsKind;
	}


	/**
	 * @return the elementName
	 */
	public String getElementName() {
		return this.elementName;
	}


	/**
	 * @return the errorDetailsKind
	 */
	public ErrorDetailsKind getErrorDetailsKind() {
		return this.errorDetailsKind;
	}


	/**
	 * Used in {@link RoboconfErrorComparator}.
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.errorDetailsKind.name() + " " + this.elementName;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals( Object obj ) {
		return obj != null
				&& obj.getClass().equals( getClass())
				&& ((ErrorDetails) obj).errorDetailsKind == this.errorDetailsKind
				&& Objects.equals(((ErrorDetails) obj).elementName, this.elementName );
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.elementName == null ? 43 : this.elementName.hashCode();
	}


	// Static methods to shorten the creation of details


	/**
	 * Details for an instance.
	 * @param instanceName the instance name
	 * @return an object with error details
	 */
	public static ErrorDetails instance( String instanceName ) {
		return new ErrorDetails( instanceName, ErrorDetailsKind.INSTANCE );
	}


	/**
	 * Details for an instance.
	 * @param instance the instance
	 * @return an object with error details
	 */
	public static ErrorDetails instance( Instance instance ) {
		return new ErrorDetails( instance.getName(), ErrorDetailsKind.INSTANCE );
	}


	/**
	 * Details for a facet.
	 * @param facetName the facet name
	 * @return an object with error details
	 */
	public static ErrorDetails facet( String facetName ) {
		return new ErrorDetails( facetName, ErrorDetailsKind.FACET );
	}


	/**
	 * Details for a facet.
	 * @param facet the facet
	 * @return an object with error details
	 */
	public static ErrorDetails facet( Facet facet ) {
		return facet( facet.getName());
	}


	/**
	 * Details for a component.
	 * @param componentName the component name
	 * @return an object with error details
	 */
	public static ErrorDetails component( String componentName ) {
		return new ErrorDetails( componentName, ErrorDetailsKind.COMPONENT );
	}


	/**
	 * Details for a component.
	 * @param component the component
	 * @return an object with error details
	 */
	public static ErrorDetails component( Component component ) {
		return new ErrorDetails( component.getName(), ErrorDetailsKind.COMPONENT );
	}


	/**
	 * Details for an application.
	 * @param app the application
	 * @return an object with error details
	 */
	public static ErrorDetails application( Application app ) {
		return new ErrorDetails( app.getName(), ErrorDetailsKind.APPLICATION );
	}


	/**
	 * Details for an application.
	 * @param appName the application's name
	 * @return an object with error details
	 */
	public static ErrorDetails application( String appName ) {
		return new ErrorDetails( appName, ErrorDetailsKind.APPLICATION );
	}


	/**
	 * Details for an application template.
	 * @param tpl the application template
	 * @return an object with error details
	 */
	public static ErrorDetails applicationTpl( ApplicationTemplate tpl ) {
		return applicationTpl( tpl.getName(), tpl.getVersion());
	}


	/**
	 * Details for an application.
	 * @param tplName the template's name
	 * @param tplVersion the template's version
	 * @return an object with error details
	 */
	public static ErrorDetails applicationTpl( String tplName, String tplVersion ) {
		return new ErrorDetails( tplName + " (" + tplVersion + ")", ErrorDetailsKind.APPLICATION_TEMPLATE );
	}


	/**
	 * Details for a variable.
	 * @param variableName the variable name
	 * @return an object with error details
	 */
	public static ErrorDetails variable( String variableName ) {
		return new ErrorDetails( variableName, ErrorDetailsKind.VARIABLE );
	}


	/**
	 * Details for a file.
	 * @param file the file
	 * @return an object with error details
	 */
	public static ErrorDetails file( File file ) {
		return file( file.getAbsolutePath());
	}


	/**
	 * Details for a file.
	 * @param filePath the file path
	 * @return an object with error details
	 */
	public static ErrorDetails file( String filePath ) {
		return new ErrorDetails( filePath, ErrorDetailsKind.FILE );
	}


	/**
	 * Details for a directory.
	 * @param file the directory
	 * @return an object with error details
	 */
	public static ErrorDetails directory( File directory ) {
		return directory( directory.getAbsolutePath());
	}


	/**
	 * Details for a directory.
	 * @param filePath the directory path
	 * @return an object with error details
	 */
	public static ErrorDetails directory( String filePath ) {
		return new ErrorDetails( filePath, ErrorDetailsKind.DIRECTORY );
	}


	/**
	 * Details for an unrecognized element.
	 * @param unrecognizedName the element that was not recognized
	 * @return an object with error details
	 */
	public static ErrorDetails unrecognized( String unrecognizedName ) {
		return new ErrorDetails( unrecognizedName, ErrorDetailsKind.UNRECOGNIZED );
	}


	/**
	 * Details for something that was expected.
	 * @param expectedName the expected value
	 * @return an object with error details
	 */
	public static ErrorDetails expected( String expectedName ) {
		return new ErrorDetails( expectedName, ErrorDetailsKind.EXPECTED );
	}


	/**
	 * Details for something that was unexpected.
	 * @param unexpectedName the unexpectedName value
	 * @return an object with error details
	 */
	public static ErrorDetails unexpected( String unexpectedName ) {
		return new ErrorDetails( unexpectedName, ErrorDetailsKind.UNEXPECTED );
	}


	/**
	 * Details for something that was malformed.
	 * @param malformedName the malformed item
	 * @return an object with error details
	 */
	public static ErrorDetails malformed( String malformedName ) {
		return new ErrorDetails( malformedName, ErrorDetailsKind.MALFORMED );
	}


	/**
	 * Details for an element name.
	 * @param name a name
	 * @return an object with error details
	 */
	public static ErrorDetails name( String name ) {
		return new ErrorDetails( name, ErrorDetailsKind.NAME );
	}


	/**
	 * Details for an instruction.
	 * @param instruction an instruction
	 * @return an object with error details
	 */
	public static ErrorDetails instruction( String instruction ) {
		return new ErrorDetails( instruction, ErrorDetailsKind.INSTRUCTION );
	}


	/**
	 * Details for a cycle.
	 * @param cycle a cycle's description
	 * @return an object with error details
	 */
	public static ErrorDetails cycle( String cycle ) {
		return new ErrorDetails( cycle, ErrorDetailsKind.CYCLE );
	}


	/**
	 * Details for an exception.
	 * @param t a throwable
	 * @return an object with error details
	 */
	public static ErrorDetails exception( Throwable t ) {
		return new ErrorDetails( Utils.writeExceptionButDoNotUseItForLogging( t ), ErrorDetailsKind.EXCEPTION );
	}


	/**
	 * Details for an exception name.
	 * @param t a throwable
	 * @return an object with error details
	 */
	public static ErrorDetails exceptionName( Throwable t ) {
		return new ErrorDetails( t.getClass().getName(), ErrorDetailsKind.EXCEPTION_NAME );
	}


	/**
	 * Details for a reference in the logs.
	 * @param logReference something to search for in the logs (e.g. UUID)
	 * @return an object with error details
	 */
	public static ErrorDetails logReference( String logReference ) {
		return new ErrorDetails( logReference, ErrorDetailsKind.LOG_REFERENCE );
	}


	/**
	 * Details for an element that is already defined.
	 * @param name a name
	 * @return an object with error details
	 */
	public static ErrorDetails alreadyDefined( String name ) {
		return new ErrorDetails( name, ErrorDetailsKind.ALREADY_DEFINED );
	}


	/**
	 * Details for a conflicting element.
	 * @param name a name
	 * @return an object with error details
	 */
	public static ErrorDetails conflicting( String name ) {
		return new ErrorDetails( name, ErrorDetailsKind.CONFLICTING );
	}


	/**
	 * Details for a value.
	 * @param value a value
	 * @return an object with error details
	 */
	public static ErrorDetails value( String value ) {
		return new ErrorDetails( value, ErrorDetailsKind.VALUE );
	}


	/**
	 * Details for a line number.
	 * @param line a line number
	 * @return an object with error details
	 */
	public static ErrorDetails line( int line ) {
		return new ErrorDetails( String.valueOf( line ), ErrorDetailsKind.LINE );
	}
}
