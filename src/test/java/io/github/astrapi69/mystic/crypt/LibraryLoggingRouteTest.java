/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Where the libraries' logging goes (#639).
 * <p>
 * The application itself logs through java.util.logging. Its libraries also use the Log4j API and
 * commons-logging, and both reach SLF4J, which has no provider in this application and drops what
 * it gets. That route came with resource-viewmodels' Spring Boot, as log4j-to-slf4j and spring-jcl;
 * without them the Log4j API falls back to its SimpleLogger, commons-logging follows it, and
 * library errors that were never shown appear on stderr after a StatusLogger complaint. Measured
 * against the application jar with the same calls, the class path the tests run on holds the same
 * logging modules as the runtime one. Both APIs are reached by name: neither is a compile
 * dependency here.
 * <p>
 * commons-logging reached the class path only through commons-beanutils, which file-worker 21 and
 * gson-extensions 4 no longer bring, so it is not on the class path now and
 * {@link #commonsLogging_isNotOnTheClassPath()} holds it off. If a dependency brings it back, that
 * test turns red: route it to SLF4J again (a bridge such as jcl-over-slf4j, as spring-jcl was
 * before) and restore the assertion that {@code LogFactory.getLog} returns an SLF4J log, which read
 * {@code type.matches(".*\\$Slf4j(LocationAware)?Log")} on the class name of the log.
 */
class LibraryLoggingRouteTest
{

	@Test
	void log4jApi_isRoutedToSlf4j() throws ReflectiveOperationException
	{
		Object factory = Class.forName("org.apache.logging.log4j.LogManager")
			.getMethod("getFactory").invoke(null);

		assertEquals("org.apache.logging.slf4j.SLF4JLoggerContextFactory",
			factory.getClass().getName(), "the Log4j API no longer reaches SLF4J");
	}

	@Test
	void commonsLogging_isNotOnTheClassPath()
	{
		ClassNotFoundException notThere = assertThrows(ClassNotFoundException.class,
			() -> Class.forName("org.apache.commons.logging.LogFactory"),
			"commons-logging is on the class path again: route it to SLF4J and restore the routing assertion of this test (#639)");

		assertEquals("org.apache.commons.logging.LogFactory", notThere.getMessage());
	}
}
