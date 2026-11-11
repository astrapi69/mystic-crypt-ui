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
package io.github.astrapi69.mystic.crypt.panel.dbtree;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.collection.pair.KeySetPair;
import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.file.create.model.FileContentInfo;

/**
 * The copy the dialog "Edit Crypt Entry" works on: what Cancel leaves and what OK takes (#881)
 */
class EntryEditingTest
{

	/** One of the six character buffers of an entry, by name, with its accessors */
	record Buffer(String name, Function<MysticCryptEntryModelBean, char[]> get,
		BiConsumer<MysticCryptEntryModelBean, char[]> set) {
		@Override
		public String toString()
		{
			return name;
		}
	}

	static Stream<Buffer> buffers()
	{
		return Stream.of(
			new Buffer("title", MysticCryptEntryModelBean::getTitle,
				MysticCryptEntryModelBean::setTitle),
			new Buffer("user name", MysticCryptEntryModelBean::getUserName,
				MysticCryptEntryModelBean::setUserName),
			new Buffer("password", MysticCryptEntryModelBean::getPassword,
				MysticCryptEntryModelBean::setPassword),
			new Buffer("repeat", MysticCryptEntryModelBean::getRepeat,
				MysticCryptEntryModelBean::setRepeat),
			new Buffer("url", MysticCryptEntryModelBean::getUrl, MysticCryptEntryModelBean::setUrl),
			new Buffer("notes", MysticCryptEntryModelBean::getNotes,
				MysticCryptEntryModelBean::setNotes));
	}

	@Test
	@DisplayName("the copy has its own attachment list, property pairs and protected names")
	void copyOf_sharesNoListAndNoPropertyWithTheEntry()
	{
		MysticCryptEntryModelBean entry = anEntry();
		MysticCryptEntryModelBean copy = EntryEditing.copyOf(entry);

		copy.getProperties().get(0).setValue("changed in the dialog");
		copy.getProperties().add(new KeyValuePair<>("added", "in the dialog"));
		copy.getResources().add(anAttachment("added.txt"));
		copy.getProtectedPropertyKeys().add("added");

		assertEquals(List.of(new KeyValuePair<>("pin", "4711")), entry.getProperties(),
			"a property changed or added in the dialog is not on the entry");
		assertEquals(1, entry.getResources().size(), "an attachment added in the dialog is not");
		assertEquals(Set.of("pin"), entry.getProtectedPropertyKeys(),
			"a protected name added in the dialog is not");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("buffers")
	@DisplayName("the copy holds the entry's own buffer, not a second copy of the secret")
	void copyOf_takesTheEntrysBuffer(final Buffer buffer)
	{
		MysticCryptEntryModelBean entry = anEntry();

		assertSame(buffer.get().apply(entry), buffer.get().apply(EntryEditing.copyOf(entry)));
	}

	@Test
	@DisplayName("an entry without properties, attachments or protected names gives a copy without them")
	void copyOf_keepsWhatIsMissingMissing()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().properties(null)
			.resources(null).build();

		MysticCryptEntryModelBean copy = EntryEditing.copyOf(entry);

		assertNull(copy.getProperties());
		assertNull(copy.getResources());
		assertNull(copy.getProtectedPropertyKeys());
	}

	@Test
	@DisplayName("OK takes every field of the copy onto the entry")
	void takeOver_takesEveryField() throws IllegalAccessException
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();
		MysticCryptEntryModelBean edited = MysticCryptEntryModelBean.builder().build();
		List<Field> fields = Stream.of(MysticCryptEntryModelBean.class.getDeclaredFields())
			.filter(field -> !Modifier.isStatic(field.getModifiers()) && !field.isSynthetic())
			.toList();
		assertFalse(fields.isEmpty(), "the entry has fields to take over");
		for (int index = 0; index < fields.size(); index++)
		{
			Field field = fields.get(index);
			field.setAccessible(true);
			field.set(edited, aValueFor(field, index));
			assertFalse(Objects.deepEquals(field.get(entry), field.get(edited)),
				"'" + field.getName() + "' differs before OK, so a field left behind shows");
		}

		EntryEditing.takeOver(entry, edited);

		for (Field field : fields)
		{
			assertTrue(Objects.deepEquals(field.get(edited), field.get(entry)),
				"OK takes '" + field.getName() + "' from the dialog onto the entry");
		}
	}

	@Test
	@DisplayName("OK leaves the buffers the entry held before alone, since a duplicate shares them")
	void takeOver_leavesTheOldBuffersToADuplicate()
	{
		MysticCryptEntryModelBean entry = anEntry();
		MysticCryptEntryModelBean duplicate = entry.duplicate();
		MysticCryptEntryModelBean edited = EntryEditing.copyOf(entry);
		edited.setPassword("typed in the dialog".toCharArray());

		EntryEditing.takeOver(entry, edited);

		assertArrayEquals("typed in the dialog".toCharArray(), entry.getPassword());
		assertArrayEquals("the password".toCharArray(), duplicate.getPassword(),
			"the duplicate keeps its password");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("buffers")
	@DisplayName("Cancel overwrites what was typed and leaves the entry's buffer as it was")
	void discard_overwritesTheTypedBuffer(final Buffer buffer)
	{
		MysticCryptEntryModelBean entry = anEntry();
		char[] kept = buffer.get().apply(entry).clone();
		MysticCryptEntryModelBean edited = EntryEditing.copyOf(entry);
		char[] typed = "typed in the dialog".toCharArray();
		buffer.set().accept(edited, typed);

		EntryEditing.discard(entry, edited);

		assertArrayEquals(new char[typed.length], typed, "what was typed is overwritten");
		assertArrayEquals(kept, buffer.get().apply(entry), "the entry keeps its " + buffer);
	}

	@Test
	@DisplayName("Cancel overwrites nothing the entry holds when nothing was typed")
	void discard_overwritesNothingOfTheEntry()
	{
		MysticCryptEntryModelBean entry = anEntry();
		MysticCryptEntryModelBean unchanged = anEntry();

		EntryEditing.discard(entry, EntryEditing.copyOf(entry));

		buffers().forEach(buffer -> assertArrayEquals(buffer.get().apply(unchanged),
			buffer.get().apply(entry), "the entry keeps its " + buffer));
		assertArrayEquals(unchanged.getResources().get(0).getContent(),
			entry.getResources().get(0).getContent(), "and its attachment");
	}

	@Test
	@DisplayName("Cancel overwrites an attachment added in the dialog and keeps the entry's")
	void discard_overwritesAnAddedAttachment()
	{
		MysticCryptEntryModelBean entry = anEntry();
		MysticCryptEntryModelBean edited = EntryEditing.copyOf(entry);
		FileContentInfo added = anAttachment("added.txt");
		edited.getResources().add(added);
		edited.getResources().add(null);

		EntryEditing.discard(entry, edited);

		assertArrayEquals(new byte[added.getContent().length], added.getContent(),
			"the added attachment is overwritten");
		assertArrayEquals(anAttachment("kept.txt").getContent(),
			entry.getResources().get(0).getContent(), "the entry's attachment is kept");
	}

	@Test
	@DisplayName("Cancel on an entry without attachments overwrites what the dialog added")
	void discard_overwritesAnAttachmentAddedToAnEntryWithout()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().resources(null)
			.build();
		MysticCryptEntryModelBean edited = EntryEditing.copyOf(entry);
		FileContentInfo added = anAttachment("added.txt");
		edited.setResources(new ArrayList<>(List.of(added)));

		EntryEditing.discard(entry, edited);

		assertArrayEquals(new byte[added.getContent().length], added.getContent());
	}

	@Test
	@DisplayName("Cancel on a copy without attachments has none to overwrite")
	void discard_withoutAttachments()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().resources(null)
			.build();
		MysticCryptEntryModelBean edited = EntryEditing.copyOf(entry);
		edited.setTitle("typed".toCharArray());
		char[] typed = edited.getTitle();

		EntryEditing.discard(entry, edited);

		assertArrayEquals(new char[5], typed);
		assertNull(edited.getResources());
	}

	private static MysticCryptEntryModelBean anEntry()
	{
		List<KeyValuePair<String, String>> properties = new ArrayList<>();
		properties.add(new KeyValuePair<>("pin", "4711"));
		List<FileContentInfo> attachments = new ArrayList<>();
		attachments.add(anAttachment("kept.txt"));
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder()
			.title("the title".toCharArray()).userName("the user".toCharArray())
			.password("the password".toCharArray()).repeat("the password".toCharArray())
			.url("https://example.org".toCharArray()).notes("the notes".toCharArray())
			.properties(properties).resources(attachments).build();
		entry.setProtectedPropertyKeys(new LinkedHashSet<>(Set.of("pin")));
		return entry;
	}

	private static FileContentInfo anAttachment(final String name)
	{
		return FileContentInfo.builder().name(name).content("attached".getBytes()).build();
	}

	/**
	 * A value for the field that a fresh entry does not have, and differs from field to field, so a
	 * field taken from the wrong place shows. A field of a type not listed here is a new field:
	 * give it a value here and take it over in {@link EntryEditing#takeOver}
	 */
	private static Object aValueFor(final Field field, final int index)
	{
		Class<?> type = field.getType();
		if (type == char[].class)
		{
			return (field.getName() + " typed").toCharArray();
		}
		if (type == boolean.class)
		{
			return true;
		}
		if (type == UUID.class)
		{
			return UUID.randomUUID();
		}
		if (type == String.class)
		{
			return field.getName() + " typed";
		}
		if (type == Integer.class)
		{
			return 100 + index;
		}
		if (type == LocalDate.class)
		{
			return LocalDate.of(2030, 1, 1).plusDays(index);
		}
		if (type == OffsetDateTime.class)
		{
			return OffsetDateTime.parse("2030-01-01T00:00:00Z").plusDays(index);
		}
		if (type == KeySetPair.class)
		{
			return new KeySetPair<>(field.getName(), new LinkedHashSet<>());
		}
		if (type == Set.class)
		{
			return new LinkedHashSet<>(Set.of(field.getName()));
		}
		if (type == List.class)
		{
			return switch (field.getName())
			{
				case "resources" -> new ArrayList<>(List.of(anAttachment("typed.txt")));
				case "properties" -> new ArrayList<>(List.of(new KeyValuePair<>("typed", "value")));
				case "history" -> new ArrayList<>(List.of(anEntry()));
				default -> fail("no value for the list '" + field.getName() + "'");
			};
		}
		return fail("no value for the field '" + field.getName() + "' of type " + type.getName()
			+ ": a new field of the entry - give it a value here and take it over in "
			+ "EntryEditing.takeOver");
	}
}
