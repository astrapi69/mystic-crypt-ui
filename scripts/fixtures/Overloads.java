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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Picks the method an old release is called through, for the writer and reader of
 * {@code scripts/compat/measure-vault-format.sh} (#788). Shared with {@code WriteWithAnOldRelease} through the
 * multi-file source launcher, like {@code EntryLines}.
 */
public final class Overloads
{

	private Overloads()
	{
	}

	/**
	 * The public method of the given name on the given type that the given arguments are passed to
	 *
	 * @param type
	 *            the type the method is declared on or inherited by
	 * @param name
	 *            the method's name
	 * @param arguments
	 *            the arguments it is called with, a {@code null} standing for any reference type
	 * @return the method
	 * @throws NoSuchMethodException
	 *             if no method of that name takes these arguments
	 */
	public static Method method(final Class<?> type, final String name, final Object... arguments)
		throws NoSuchMethodException
	{
		return resolve(type.getMethods(), name, arguments);
	}

	/**
	 * The one of the given candidates that the given arguments are passed to: of those whose
	 * parameters take the arguments, the most specific, as the compiler would choose it. Not the
	 * first of the name and the parameter count - the order of {@link Class#getMethods()} is not
	 * specified, and the application jar of 8 has three {@code initializeTreeNodeWithTreeElement}
	 * with three parameters (#788)
	 *
	 * @param candidates
	 *            the methods to choose from, in any order
	 * @param name
	 *            the method's name
	 * @param arguments
	 *            the arguments it is called with, a {@code null} standing for any reference type
	 * @return the method
	 * @throws NoSuchMethodException
	 *             if no candidate of that name takes these arguments, or more than one does and none
	 *             of them is the most specific
	 */
	public static Method resolve(final Method[] candidates, final String name, final Object... arguments)
		throws NoSuchMethodException
	{
		List<Method> applicable = Arrays.stream(candidates)
			.filter(candidate -> candidate.getName().equals(name) && takes(candidate, arguments))
			.toList();
		List<Method> mostSpecific = applicable.stream()
			.filter(candidate -> applicable.stream()
				.noneMatch(other -> other != candidate && isMoreSpecific(other, candidate)))
			.toList();
		if (mostSpecific.size() != 1)
		{
			throw new NoSuchMethodException((mostSpecific.isEmpty() ? "no " : "more than one ") + name
				+ " takes " + Arrays.toString(arguments) + ": " + Arrays.toString(candidates));
		}
		return mostSpecific.getFirst();
	}

	/** Whether the method's parameters take the arguments, a null any reference type */
	private static boolean takes(final Method method, final Object[] arguments)
	{
		Class<?>[] parameters = method.getParameterTypes();
		if (parameters.length != arguments.length)
		{
			return false;
		}
		for (int index = 0; index < parameters.length; index++)
		{
			Class<?> parameter = boxed(parameters[index]);
			if (arguments[index] == null ? parameters[index].isPrimitive()
				: !parameter.isInstance(arguments[index]))
			{
				return false;
			}
		}
		return true;
	}

	/** Whether every parameter of the one method is a subtype of the other's, and the two differ */
	private static boolean isMoreSpecific(final Method one, final Method other)
	{
		Class<?>[] ones = one.getParameterTypes();
		Class<?>[] others = other.getParameterTypes();
		boolean differs = false;
		for (int index = 0; index < ones.length; index++)
		{
			if (!boxed(others[index]).isAssignableFrom(boxed(ones[index])))
			{
				return false;
			}
			differs |= ones[index] != others[index];
		}
		return differs;
	}

	private static Class<?> boxed(final Class<?> type)
	{
		if (!type.isPrimitive())
		{
			return type;
		}
		return switch (type.getName())
		{
			case "boolean" -> Boolean.class;
			case "byte" -> Byte.class;
			case "char" -> Character.class;
			case "short" -> Short.class;
			case "int" -> Integer.class;
			case "long" -> Long.class;
			case "float" -> Float.class;
			case "double" -> Double.class;
			default -> Void.class;
		};
	}
}
