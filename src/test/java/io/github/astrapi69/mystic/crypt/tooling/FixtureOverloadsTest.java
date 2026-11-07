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
package io.github.astrapi69.mystic.crypt.tooling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The format measurement's writer calls an old release through the overload its arguments fit,
 * whatever order {@link Class#getMethods()} returns them in (#788). It took the first of the name
 * and the parameter count, and the application jar of 8 has three
 * {@code initializeTreeNodeWithTreeElement} with three parameters: a run on JDK 25.0.4 failed in 3
 * of 12 with "argument type mismatch". The candidates here mirror those three, an element type, a
 * subtype of it and an unrelated one, and {@code scripts/fixtures/Overloads.java} is compiled the
 * way the source launcher compiles it
 */
class FixtureOverloadsTest
{

	@TempDir
	static Path compiled;

	private static Method resolve;

	/** The element type of the old API generation */
	public static class TreeElement
	{
	}

	/** The renderer's element type */
	public static class JTreeElement
	{
	}

	/** The generic element, a renderer element */
	public static class GenericTreeElement extends JTreeElement
	{
	}

	/** Three overloads of one name and parameter count, as {@code BaseTreeNodeFactory} of 8 */
	public static final class Factory
	{
		public static String initialize(final TreeElement element, final Object parent,
			final Object ids)
		{
			return "TreeElement";
		}

		public static String initialize(final JTreeElement element, final Object parent,
			final Object ids)
		{
			return "JTreeElement";
		}

		public static String initialize(final GenericTreeElement element, final Object parent,
			final Object ids)
		{
			return "GenericTreeElement";
		}

		public static String toKey(final int number)
		{
			return "int " + number;
		}
	}

	@BeforeAll
	static void compileOverloads() throws Exception
	{
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		int exit = compiler.run(null, null, null, "-d", compiled.toString(),
			Path.of("scripts", "fixtures", "Overloads.java").toString());
		assertEquals(0, exit, "scripts/fixtures/Overloads.java does not compile");
		URLClassLoader loader = new URLClassLoader(new URL[] { compiled.toUri().toURL() },
			FixtureOverloadsTest.class.getClassLoader());
		resolve = loader.loadClass("Overloads").getMethod("resolve", Method[].class, String.class,
			Object[].class);
	}

	/** Every order of the three overloads, the order getMethods() does not promise */
	static Stream<Arguments> orders()
	{
		List<Method> overloads = Arrays.stream(Factory.class.getMethods())
			.filter(method -> method.getName().equals("initialize")).toList();
		assertEquals(3, overloads.size());
		List<List<Method>> orders = new ArrayList<>();
		for (Method first : overloads)
		{
			for (Method second : overloads)
			{
				for (Method third : overloads)
				{
					if (first != second && second != third && first != third)
					{
						orders.add(List.of(first, second, third));
					}
				}
			}
		}
		return orders.stream().map(order -> Arguments.of(
			order.stream().map(method -> method.getParameterTypes()[0].getSimpleName()).toList(),
			order));
	}

	@ParameterizedTest(name = "candidates in the order {0}")
	@MethodSource("orders")
	@DisplayName("the overload the arguments fit is chosen, the most specific of those that take them")
	void resolve_choosesTheOverloadTheArgumentsFit(final List<String> label,
		final List<Method> order) throws Exception
	{
		Method chosen = choose(order, "initialize", new GenericTreeElement(), null, new Object());

		assertEquals("GenericTreeElement",
			chosen.invoke(null, new GenericTreeElement(), null, null), "chosen from " + order);
	}

	@ParameterizedTest(name = "candidates in the order {0}")
	@MethodSource("orders")
	@DisplayName("an argument of the old API generation reaches its own overload")
	void resolve_choosesTheOnlyOverloadTheArgumentFits(final List<String> label,
		final List<Method> order) throws Exception
	{
		Method chosen = choose(order, "initialize", new TreeElement(), null, new Object());

		assertEquals("TreeElement", chosen.invoke(null, new TreeElement(), null, null));
	}

	@Test
	@DisplayName("a boxed argument fits a primitive parameter")
	void resolve_acceptsABoxedArgumentForAPrimitiveParameter() throws Exception
	{
		Method chosen = choose(Arrays.asList(Factory.class.getMethods()), "toKey", 7);

		assertEquals("int 7", chosen.invoke(null, 7));
	}

	@Test
	@DisplayName("no overload that takes the arguments is an error that names the candidates")
	void resolve_refuses_whenNoOverloadTakesTheArguments()
	{
		InvocationTargetException refused = assertThrows(InvocationTargetException.class,
			() -> choose(Arrays.asList(Factory.class.getMethods()), "initialize", "a string", null,
				null));

		assertTrue(refused.getCause() instanceof NoSuchMethodException,
			refused.getCause().toString());
		assertTrue(refused.getCause().getMessage().contains("initialize"),
			refused.getCause().getMessage());
	}

	private static Method choose(final List<Method> candidates, final String name,
		final Object... arguments) throws Exception
	{
		return (Method)resolve.invoke(null, candidates.toArray(Method[]::new), name, arguments);
	}
}
