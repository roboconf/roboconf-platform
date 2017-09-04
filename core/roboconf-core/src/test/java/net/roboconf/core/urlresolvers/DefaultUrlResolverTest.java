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

package net.roboconf.core.urlresolvers;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.roboconf.core.urlresolvers.IUrlResolver.ResolvedFile;
import net.roboconf.core.utils.Utils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class DefaultUrlResolverTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();


	@Test
	public void testResolve_fileUrl() throws Exception {
		DefaultUrlResolver resolver = new DefaultUrlResolver();

		File f = this.folder.newFile();
		ResolvedFile resolvedFile = resolver.resolve( f.toURI().toURL().toString());
		Assert.assertEquals( f, resolvedFile.getFile());
		Assert.assertTrue( resolvedFile.existedBefore());
	}


	@Test
	public void testResolve_httpUrl() throws Exception {

		// Run a socket server
		final int portNumber = 11200;
		final String content = "something";
		final CountDownLatch latch = new CountDownLatch( 1 );

		Runnable serverRunnable = new Runnable() {
			@Override
			public void run() {

				try {
					ServerSocket serverSocket = new ServerSocket( portNumber );
					latch.countDown();
					Socket clientSocket = serverSocket.accept();

					clientSocket.getOutputStream().write( "HTTP/1.0 200 OK\r\n\r\n".getBytes( "UTF-8" ));
					clientSocket.getOutputStream().write( content.getBytes( "UTF-8" ));
					clientSocket.getOutputStream().flush();

					clientSocket.close();
					serverSocket.close();

				} catch( IOException e ) {
					e.printStackTrace();
				}
			}
		};

		Thread t = new Thread( serverRunnable );
		t.start();

		// Wait the thread to be started
		latch.await();

		// Wait the server to be accepting requests.
		// Make several attempts as Travis builds are sometimes slow.
		DefaultUrlResolver resolver = new DefaultUrlResolver();
		ResolvedFile resolvedFile = null;
		for( int i=0; i<5; i++ ) {

			Thread.sleep( 200 );
			try {
				resolvedFile = resolver.resolve( "http://localhost:" + portNumber );
				break;

			} catch( Exception e ) {
				// nothing
			}
		}

		// Wait for the server to complete
		t.join();

		// Verify
		Assert.assertNotNull( resolvedFile );
		Assert.assertTrue( resolvedFile.getFile().exists());
		Assert.assertEquals( content, Utils.readFileContent( resolvedFile.getFile()));
		Assert.assertFalse( resolvedFile.existedBefore());
	}


	@Test( expected = IOException.class )
	public void testResolve_unreachableUrl() throws Exception {

		DefaultUrlResolver resolver = new DefaultUrlResolver();
		resolver.resolve( "http://localhost:19824" );
	}


	@Test( expected = IOException.class )
	public void testResolve_mavenUrl() throws Exception {

		DefaultUrlResolver resolver = new DefaultUrlResolver();
		resolver.resolve( "mvn:net.roboconf/my-app/1.0" );
	}


	@Test( expected = IOException.class )
	public void testResolve_invalidUrl() throws Exception {

		DefaultUrlResolver resolver = new DefaultUrlResolver();
		resolver.resolve( "localhost" );
	}
}
