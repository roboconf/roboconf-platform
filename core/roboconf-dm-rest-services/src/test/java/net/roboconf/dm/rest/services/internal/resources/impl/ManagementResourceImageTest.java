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

package net.roboconf.dm.rest.services.internal.resources.impl;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import com.sun.jersey.core.header.FormDataContentDisposition;
import junit.framework.Assert;
import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestUtils;
import net.roboconf.core.model.beans.AbstractApplication;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.dm.internal.test.TestTargetResolver;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.rest.services.internal.resources.IManagementResource;
import net.roboconf.messaging.api.MessagingConstants;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.internal.client.test.TestClientFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for the {@link IManagementResource#setImage(String, String, InputStream, FormDataContentDisposition)} method.
 *
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class ManagementResourceImageTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private static final File IMAGE_PNG;
	private static final File IMAGE_SVG;
	private static final File IMAGE_GIF;
	private static final File IMAGE_JPG;
	private static final File IMAGE_JPEG;
	private static final File IMAGE_UNSUPPORTED;
	private static final File IMAGE_TOO_BIG;

	private static final String TEMPLATE_NAME = "Legacy LAMP";
	private static final String TEMPLATE_QUALIFIER = "sample";
	private static final String APPLICATION_NAME = "app";

	private static final String[] SUPPORTED_EXTENSIONS = {"jpg", "jpeg", "gif", "png", "svg"};

	// Load the images.
	static {
		try {
			IMAGE_PNG = new File(ManagementResourceImageTest.class.getResource("/smiley.png").toURI());
			IMAGE_SVG = new File(ManagementResourceImageTest.class.getResource("/smiley.svg").toURI());
			IMAGE_GIF = new File(ManagementResourceImageTest.class.getResource("/smiley.gif").toURI());
			IMAGE_JPG = new File(ManagementResourceImageTest.class.getResource("/smiley.jpg").toURI());
			IMAGE_JPEG = new File(ManagementResourceImageTest.class.getResource("/smiley.jpeg").toURI());
			IMAGE_UNSUPPORTED = new File(ManagementResourceImageTest.class.getResource("/smiley.tif").toURI());
			IMAGE_TOO_BIG = new File(ManagementResourceImageTest.class.getResource("/fat.png").toURI());
		} catch (final URISyntaxException e) {
			throw new AssertionError(e);
		}
	}


	private Manager manager;
	private IManagementResource resource;
	private ApplicationTemplate template;
	private Application application;


	@Before
	public void before() throws Exception {

		// Create, configure & start the manager.
		this.manager = new Manager();
		this.manager.setMessagingType(MessagingConstants.TEST_FACTORY_TYPE);
		this.manager.setTargetResolver(new TestTargetResolver());
		this.manager.setConfigurationDirectoryLocation(this.folder.newFolder().getAbsolutePath());
		this.manager.start();

		// Reconfigure with the messaging client factory registry set.
		final MessagingClientFactoryRegistry registry = new MessagingClientFactoryRegistry();
		registry.addMessagingClientFactory(new TestClientFactory());
		this.manager.getMessagingClient().setRegistry(registry);
		this.manager.reconfigure();

		// Create the management resource.
		this.resource = new ManagementResource(this.manager);

		// Deploy an application template.
		this.resource.loadApplicationTemplate(TestUtils.findApplicationDirectory("lamp").getAbsolutePath());
		this.template = this.manager.findTemplate(TEMPLATE_NAME, TEMPLATE_QUALIFIER);

		// Create an application.
		this.resource.createApplication(new Application(APPLICATION_NAME, this.template));
		this.application = this.manager.findApplicationByName(APPLICATION_NAME);
	}


	@After
	public void after() {
		this.manager.stop();
	}


	private Response setImage( final String name, final String qualifier, final File image ) throws IOException {
		try (final InputStream stream = new FileInputStream(image)) {
			return this.resource.setImage(name, qualifier, stream,
					FormDataContentDisposition
							.name(image.getName())
							.fileName(image.getName())
							.size(image.length())
							.build());
		}
	}


	private File getApplicationIcon( final AbstractApplication app, final String extension ) {
		return new File(app.getDirectory(), Constants.PROJECT_DIR_DESC + "/application." + extension);
	}


	@Test
	public void testSetTemplateImage_png_success() throws Exception {
		final File icon = getApplicationIcon(this.template, "png");
		Assert.assertFalse(icon.exists());
		Assert.assertEquals(
				Status.NO_CONTENT.getStatusCode(),
				setImage(TEMPLATE_NAME, TEMPLATE_QUALIFIER, IMAGE_PNG).getStatus());
		Assert.assertTrue(icon.exists());
	}


	@Test
	public void testSetTemplateImage_svg_success() throws Exception {
		final File icon = getApplicationIcon(this.template, "svg");
		Assert.assertFalse(icon.exists());
		Assert.assertEquals(
				Status.NO_CONTENT.getStatusCode(),
				setImage(TEMPLATE_NAME, TEMPLATE_QUALIFIER, IMAGE_SVG).getStatus());
		Assert.assertTrue(icon.exists());
	}


	@Test
	public void testSetTemplateImage_jpg_success() throws Exception {
		final File icon = getApplicationIcon(this.template, "jpg");
		Assert.assertFalse(icon.exists());
		Assert.assertEquals(
				Status.NO_CONTENT.getStatusCode(),
				setImage(TEMPLATE_NAME, TEMPLATE_QUALIFIER, IMAGE_JPG).getStatus());
		Assert.assertTrue(icon.exists());
	}


	@Test
	public void testSetTemplateImage_jpeg_success() throws Exception {
		final File icon = getApplicationIcon(this.template, "jpeg");
		Assert.assertFalse(icon.exists());
		Assert.assertEquals(
				Status.NO_CONTENT.getStatusCode(),
				setImage(TEMPLATE_NAME, TEMPLATE_QUALIFIER, IMAGE_JPEG).getStatus());
		Assert.assertTrue(icon.exists());
	}


	@Test
	public void testSetTemplateImage_gif_success() throws Exception {
		final File icon = getApplicationIcon(this.template, "gif");
		Assert.assertFalse(icon.exists());
		Assert.assertEquals(
				Status.NO_CONTENT.getStatusCode(),
				setImage(TEMPLATE_NAME, TEMPLATE_QUALIFIER, IMAGE_GIF).getStatus());
		Assert.assertTrue(icon.exists());
	}


	@Test
	public void testSetTemplateImage_too_big() throws Exception {
		final File icon = getApplicationIcon(this.template, "png");
		Assert.assertFalse(icon.exists());
		Assert.assertEquals(
				Status.BAD_REQUEST.getStatusCode(),
				setImage(TEMPLATE_NAME, TEMPLATE_QUALIFIER, IMAGE_TOO_BIG).getStatus());
		Assert.assertFalse(icon.exists());
	}


	@Test
	public void testSetTemplateImage_unsupported() throws Exception {
		final File icon = getApplicationIcon(this.template, "tif");
		Assert.assertFalse(icon.exists());
		Assert.assertEquals(
				Status.BAD_REQUEST.getStatusCode(),
				setImage(TEMPLATE_NAME, TEMPLATE_QUALIFIER, IMAGE_UNSUPPORTED).getStatus());
		Assert.assertFalse(icon.exists());
	}


	@Test
	public void testSetApplicationImage_png() throws Exception {
		final File icon = getApplicationIcon(this.application, "png");
		Assert.assertFalse(icon.exists());
		Assert.assertEquals(
				Status.NO_CONTENT.getStatusCode(),
				setImage(APPLICATION_NAME, null, IMAGE_PNG).getStatus());
		Assert.assertTrue(icon.exists());
	}


	@Test
	public void testSetTemplateImage_delete_previous_images() throws Exception {

		// Create fake images.
		for (final String extension : SUPPORTED_EXTENSIONS) {
			Assert.assertTrue(getApplicationIcon(this.template, extension).createNewFile());
		}

		// Do set the application image.
		Assert.assertEquals(
				Status.NO_CONTENT.getStatusCode(),
				setImage(TEMPLATE_NAME, TEMPLATE_QUALIFIER, IMAGE_PNG).getStatus());

		// Check all images have been deleted, except the png which has been replaced.
		for (final String extension : SUPPORTED_EXTENSIONS) {
			if (!extension.equals("png")) {

				// Deleted
				Assert.assertFalse(getApplicationIcon(this.template, extension).exists());
			} else {

				// Replaced
				Assert.assertEquals(IMAGE_PNG.length(), getApplicationIcon(this.template, extension).length());
			}
		}
	}


	@Test
	public void testSetApplicationImage_delete_previous_images() throws Exception {

		// Create fake images.
		for (final String extension : SUPPORTED_EXTENSIONS) {
			Assert.assertTrue(getApplicationIcon(this.application, extension).createNewFile());
		}

		// Do set the application image.
		Assert.assertEquals(
				Status.NO_CONTENT.getStatusCode(),
				setImage(APPLICATION_NAME, null, IMAGE_PNG).getStatus());

		// Check all images have been deleted, except the png which has been replaced.
		for (final String extension : SUPPORTED_EXTENSIONS) {
			if (!extension.equals("png")) {

				// Deleted
				Assert.assertFalse(getApplicationIcon(this.application, extension).exists());
			} else {

				// Replaced
				Assert.assertEquals(IMAGE_PNG.length(), getApplicationIcon(this.application, extension).length());
			}
		}
	}


	@Test
	public void testSetTemplateImage_no_such_element() throws Exception {
		final ApplicationTemplate t = new ApplicationTemplate("foo").qualifier("bar");
		final File icon = getApplicationIcon(t, "png");
		Assert.assertFalse(icon.exists());
		Assert.assertEquals(
				Status.BAD_REQUEST.getStatusCode(),
				setImage("foo", "bar", IMAGE_PNG).getStatus());
		Assert.assertFalse(icon.exists());
	}


	@Test
	public void testSetApplicationImage_no_such_element() throws Exception {
		final Application a = new Application("foo", this.template);
		final File icon = getApplicationIcon(a, "png");
		Assert.assertFalse(icon.exists());
		Assert.assertEquals(
				Status.BAD_REQUEST.getStatusCode(),
				setImage("foo", null, IMAGE_PNG).getStatus());
		Assert.assertFalse(icon.exists());
	}

}
