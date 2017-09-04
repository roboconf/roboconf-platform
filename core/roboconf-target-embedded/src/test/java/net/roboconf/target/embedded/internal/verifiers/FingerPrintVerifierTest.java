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

package net.roboconf.target.embedded.internal.verifiers;

import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.roboconf.target.embedded.internal.EmbeddedHandler;
import net.schmizz.sshj.common.SecurityUtils;

/**
 * @author Vincent Zurczak - Linagora
 */
public class FingerPrintVerifierTest {

	@Test
	public void testVerify() throws Exception {

		// Setup
		final String ip1 = "ip1";
		final String ip2 = "ip2";

		Map<String,String> targetProperties = new HashMap<> ();
		FingerPrintVerifier verifier = new FingerPrintVerifier( targetProperties );

		// Find finger prints to use in the tests
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance( "RSA" );
		PublicKey publicKey1 = keyGen.generateKeyPair().getPublic();
		PublicKey publicKey2 = keyGen.generateKeyPair().getPublic();
		PublicKey publicKey3 = keyGen.generateKeyPair().getPublic();
		Assert.assertNotEquals( publicKey1, publicKey2 );
		Assert.assertNotEquals( publicKey3, publicKey2 );
		Assert.assertNotEquals( publicKey3, publicKey1 );

		targetProperties.put( EmbeddedHandler.SCP_HOST_KEY_PREFIX + ip1, SecurityUtils.getFingerprint( publicKey1 ));
		targetProperties.put( EmbeddedHandler.SCP_HOST_KEY_PREFIX + ip2, SecurityUtils.getFingerprint( publicKey2 ));

		// Check
		Assert.assertTrue( verifier.verify( ip1, 22, publicKey1 ));
		Assert.assertTrue( verifier.verify( ip2, 22, publicKey2 ));

		Assert.assertFalse( verifier.verify( ip1, 22, publicKey2 ));
		Assert.assertFalse( verifier.verify( ip2, 2215, publicKey3 ));
	}
}
