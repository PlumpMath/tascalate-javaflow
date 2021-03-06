/**
 * ﻿Copyright 2013-2017 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.spi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class ExtendedClasspathResourceLoader extends ClasspathResourceLoader {
	final private static ThreadLocal<Map<String, byte[]>> IN_MEMORY_RESOURCES = new ThreadLocal<Map<String,byte[]>>();
	
	public ExtendedClasspathResourceLoader(final ClassLoader classLoader) {
		super(classLoader);
	}
	
	public static void runWithInMemoryResources(final Runnable block, final Map<String, byte[]> inMemoryResources) {
		runWithInMemoryResources(
			new Callable<Void>() {
				public Void call() {
					block.run();
					return null;
				}
			}, 
			inMemoryResources
		);
	}
	
	public static <V> V runWithInMemoryResources(final Callable<V> block, final Map<String, byte[]> inMemoryResources) {
		final Map<String, byte[]> resources = new HashMap<String, byte[]>(inMemoryResources);
		
		final Map<String, byte[]> previous = IN_MEMORY_RESOURCES.get();
		if (null != previous) {
			// Merge with previous ones for recursive calls
			resources.putAll(previous);
		}
		IN_MEMORY_RESOURCES.set(resources);
		
		try {
			return block.call();
		} catch (final RuntimeException ex) {
			throw ex;
		} catch (final Error ex) {
			throw ex;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} finally {
			IN_MEMORY_RESOURCES.set(previous);
		}
		
	}

	@Override
	public InputStream getResourceAsStream(String name) throws IOException {
		final Map<String, byte[]> inMemoryResources = IN_MEMORY_RESOURCES.get();
		if (null != inMemoryResources) {
			final byte[] bytecode = inMemoryResources.get(name);
			if (null != bytecode)
				return new ByteArrayInputStream(bytecode);
		}
		
		return super.getResourceAsStream(name);
	}
}
