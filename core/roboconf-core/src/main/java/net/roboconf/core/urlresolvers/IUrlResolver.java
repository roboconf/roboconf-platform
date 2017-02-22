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

/**
 * @author Vincent Zurczak - Linagora
 */
public interface IUrlResolver {

	/**
	 * Resolves an URL as a local file.
	 * <p>
	 * Local URLs (file:/) are not copied but simply referenced.
	 * Remote URLs are downloaded as a temporary file. This method is not
	 * supposed to delete the downloaded file then. The method invoker can
	 * do whatever it needs with the file, including deleting it once its
	 * job complete.
	 * </p>
	 *
	 * @param url an URL (not null)
	 * @return a resolved file (never null)
	 * @throws IOException if something went wrong (e.g. invalid URL or impossible download)
	 */
	ResolvedFile resolve( String url ) throws IOException;


	/**
	 * @author Vincent Zurczak - Linagora
	 */
	public static class ResolvedFile {
		private final File file;
		private final boolean existedBefore;

		/**
		 * Constructor.
		 * @param file
		 * @param existedBefore
		 */
		public ResolvedFile( File file, boolean existedBefore ) {
			this.file = file;
			this.existedBefore = existedBefore;
		}

		/**
		 * @return the file
		 */
		public File getFile() {
			return file;
		}

		/**
		 * @return true if the file existed prior to the resolution
		 */
		public boolean existedBefore() {
			return existedBefore;
		}
	}
}
