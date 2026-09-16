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
package io.github.astrapi69.mystic.crypt.keepass;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.linguafranca.pwdb.kdbx.jackson.JacksonEntry;
import org.linguafranca.pwdb.kdbx.jackson.JacksonGroup;
import org.linguafranca.pwdb.kdbx.jackson.JacksonHistory;
import org.linguafranca.pwdb.kdbx.jackson.model.Times;

/**
 * The one place in this application that knows a field name of KeePassJava2, and the only one that
 * reflects on its types.
 * <p>
 * KeePassJava2 2.2.4 keeps an entry's identifier, its four timestamps and its version history
 * behind {@code protected} fields with no setter and no factory that takes them, in both of its
 * implementations. A round trip that has to return a database unchanged - the identifier a KeePass
 * user sees, the creation date they wrote, the history they kept - cannot be built on the public
 * API alone. Measured, not assumed: a subclass is no way around it either, because
 * {@code JacksonEntry.database} is package private, so an entry built that way has no database and
 * every {@code setTitle}, {@code setBinaryProperty} and {@code addEntry} on it fails.
 * <p>
 * So the bridge is reflection, and this class is the whole of it. Every field name lives here as a
 * constant, once, so that a library version which renames one has exactly one site to fix - and
 * {@code KeePassLibraryFieldsTest} resolves every one of them against the library on the classpath,
 * so that rename turns the build red instead of turning an export quietly lossy. A second place
 * that reflects would be the one nobody remembers, and a test pins that there is none (#382).
 * <p>
 * No {@code --add-opens} is needed: the library is an ordinary jar on the class path, in the
 * unnamed module, where {@code setAccessible} on a {@code protected} field is allowed. The flag the
 * build carries today is for something else - SimpleXML reaching into {@link UUID}'s own private
 * fields - and goes when the last use of that serializer does.
 * <p>
 * This is a bridge, not a destination: the way out is upstream, a setter or a factory that takes
 * these values, at which point every method here becomes a one-line delegation and the reflection
 * goes.
 */
public final class KeePassLibraryFields
{

	/**
	 * The library version these names were read from. It appears in every failure message, because
	 * "NoSuchFieldException" without it leaves a reader guessing whether the name or the version is
	 * wrong
	 */
	public static final String EXPECTED_LIBRARY_VERSION = "KeePassJava2 2.2.4";

	/** {@code JacksonEntry.uuid}, and {@code JacksonGroup.uuid}, both protected */
	public static final String UUID_FIELD = "uuid";

	/** {@code JacksonEntry.times} and {@code JacksonGroup.times}, both protected */
	public static final String TIMES_FIELD = "times";

	/** {@code JacksonEntry.history}, protected */
	public static final String HISTORY_FIELD = "history";

	private KeePassLibraryFields()
	{
	}

	/**
	 * Resolves a field of a library class, making it accessible.
	 *
	 * @param owner
	 *            the library class the field is expected on
	 * @param fieldName
	 *            the name of the field
	 * @return the accessible field
	 * @throws IllegalStateException
	 *             when the field does not exist, naming the field, the class and the library
	 *             version
	 */
	public static Field field(final Class<?> owner, final String fieldName)
	{
		try
		{
			Field field = owner.getDeclaredField(fieldName);
			field.setAccessible(true);
			return field;
		}
		catch (NoSuchFieldException | SecurityException exception)
		{
			throw new IllegalStateException("no field '" + fieldName + "' on " + owner.getName()
				+ " - this application reaches it by reflection because " + EXPECTED_LIBRARY_VERSION
				+ " offers no setter for it. A different library version that renames or removes it "
				+ "breaks the KeePass round trip, so this fails here rather than writing an export "
				+ "without it.", exception);
		}
	}

	/**
	 * The field names that do NOT resolve against the library on the classpath, empty when all of
	 * them do. The test that calls this is the reason the capsule exists.
	 *
	 * @return the unresolved names, each as {@code Class.field}
	 */
	public static List<String> unresolvedFieldNames()
	{
		List<String> unresolved = new ArrayList<>();
		collectUnresolved(JacksonEntry.class, UUID_FIELD, unresolved);
		collectUnresolved(JacksonEntry.class, TIMES_FIELD, unresolved);
		collectUnresolved(JacksonEntry.class, HISTORY_FIELD, unresolved);
		collectUnresolved(JacksonGroup.class, UUID_FIELD, unresolved);
		collectUnresolved(JacksonGroup.class, TIMES_FIELD, unresolved);
		return unresolved;
	}

	/**
	 * Sets the identifier of an entry, which the library assigns randomly and never lets go of
	 *
	 * @param entry
	 *            the entry
	 * @param identifier
	 *            the identifier it has to carry
	 */
	public static void setUuid(final JacksonEntry entry, final UUID identifier)
	{
		write(JacksonEntry.class, UUID_FIELD, entry, identifier);
	}

	/**
	 * Sets the identifier of a group
	 *
	 * @param group
	 *            the group
	 * @param identifier
	 *            the identifier it has to carry
	 */
	public static void setUuid(final JacksonGroup group, final UUID identifier)
	{
		write(JacksonGroup.class, UUID_FIELD, group, identifier);
	}

	/**
	 * Sets all four timestamps of an entry at once, because the library holds them in one object
	 * and writing three of them would look right here and lose the fourth in the file
	 *
	 * @param entry
	 *            the entry
	 * @param times
	 *            the timestamps, including the expiry flag
	 */
	public static void setTimes(final JacksonEntry entry, final Times times)
	{
		write(JacksonEntry.class, TIMES_FIELD, entry, times);
	}

	/**
	 * Sets all four timestamps of a group
	 *
	 * @param group
	 *            the group
	 * @param times
	 *            the timestamps, including the expiry flag
	 */
	public static void setTimes(final JacksonGroup group, final Times times)
	{
		write(JacksonGroup.class, TIMES_FIELD, group, times);
	}

	/**
	 * Reads all four timestamps of an entry
	 *
	 * @param entry
	 *            the entry
	 * @return its timestamps
	 */
	public static Times getTimes(final JacksonEntry entry)
	{
		return (Times)read(JacksonEntry.class, TIMES_FIELD, entry);
	}

	/**
	 * Reads all four timestamps of a group.
	 * <p>
	 * Through the capsule rather than through the library, because {@link JacksonGroup} exposes no
	 * getter for any of them - measured on 2.2.4: an entry has {@code getCreationTime()} and its
	 * siblings, a group has none at all. So a group's timestamps are unreachable in both directions
	 * without this, not only unwritable.
	 *
	 * @param group
	 *            the group
	 * @return its timestamps
	 */
	public static Times getTimes(final JacksonGroup group)
	{
		return (Times)read(JacksonGroup.class, TIMES_FIELD, group);
	}

	/**
	 * Sets the version history of an entry, which this application carries through unchanged rather
	 * than maintaining
	 *
	 * @param entry
	 *            the entry
	 * @param history
	 *            the previous versions
	 */
	public static void setHistory(final JacksonEntry entry, final JacksonHistory history)
	{
		write(JacksonEntry.class, HISTORY_FIELD, entry, history);
	}

	/**
	 * Reads the version history of an entry
	 *
	 * @param entry
	 *            the entry
	 * @return the previous versions, or {@code null} when the entry has none
	 */
	public static JacksonHistory getHistory(final JacksonEntry entry)
	{
		return (JacksonHistory)read(JacksonEntry.class, HISTORY_FIELD, entry);
	}

	private static void collectUnresolved(final Class<?> owner, final String fieldName,
		final List<String> unresolved)
	{
		try
		{
			field(owner, fieldName);
		}
		catch (IllegalStateException exception)
		{
			unresolved.add(owner.getSimpleName() + "." + fieldName);
		}
	}

	private static void write(final Class<?> owner, final String fieldName, final Object target,
		final Object value)
	{
		try
		{
			field(owner, fieldName).set(target, value);
		}
		catch (IllegalAccessException exception)
		{
			throw new IllegalStateException("cannot write '" + fieldName + "' on " + owner.getName()
				+ " of " + EXPECTED_LIBRARY_VERSION, exception);
		}
	}

	private static Object read(final Class<?> owner, final String fieldName, final Object target)
	{
		try
		{
			return field(owner, fieldName).get(target);
		}
		catch (IllegalAccessException exception)
		{
			throw new IllegalStateException("cannot read '" + fieldName + "' on " + owner.getName()
				+ " of " + EXPECTED_LIBRARY_VERSION, exception);
		}
	}
}
