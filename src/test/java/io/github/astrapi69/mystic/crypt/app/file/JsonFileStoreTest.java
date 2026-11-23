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
package io.github.astrapi69.mystic.crypt.app.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link JsonFileStore} reads a file that cannot be read as nothing, and keeps it aside rather than
 * writing over it (#1082)
 */
class JsonFileStoreTest
{

	/** 2026-11-19 15:30:12 UTC, the time in the name of a file set aside */
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-19T15:30:12Z"),
		ZoneOffset.UTC);

	private final List<UnreadableFileNotices.Notice> notices = new ArrayList<>();

	@TempDir
	File directory;

	/** A small bean with one value, the shape of the settings and the memos */
	static class Remembered
	{
		String path;
	}

	@BeforeEach
	void listen()
	{
		UnreadableFileNotices.listen(notices::add);
	}

	@AfterEach
	void stopListening()
	{
		UnreadableFileNotices.listen(null);
	}

	static Stream<Arguments> unreadableFiles()
	{
		return Stream.of(Arguments.of("cut short", "{\"path\": \"/home/user/va"),
			Arguments.of("not JSON", "path=/home/user/vault.mcrdb"),
			Arguments.of("an empty file", ""), Arguments.of("a JSON null", "null"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("unreadableFiles")
	@DisplayName("a file that cannot be read reads as nothing, and the next write keeps it aside")
	void anUnreadableFile_readsAsNothing_andIsKeptAsideByTheNextWrite(final String what,
		final String content) throws IOException
	{
		File file = new File(directory, "settings.json");
		byte[] original = content.getBytes(StandardCharsets.UTF_8);
		Files.write(file.toPath(), original);

		assertTrue(JsonFileStore.read(file, Remembered.class).isEmpty(), what);

		JsonFileStore.write(remembered("/home/user/new.mcrdb"), file, Remembered.class, CLOCK);

		File keptAs = new File(directory, "settings.json.unreadable-20261119T153012");
		assertArrayEquals(original, Files.readAllBytes(keptAs.toPath()),
			"the old bytes are kept, unchanged");
		assertEquals("/home/user/new.mcrdb",
			JsonFileStore.read(file, Remembered.class).orElseThrow().path,
			"the new file is written and reads back");
		assertEquals(List.of(new UnreadableFileNotices.SetAside(file, keptAs)), notices,
			"the user is told once, with both files");
	}

	@Test
	@DisplayName("a file that can be read is written over, nothing is kept aside and nobody is told")
	void aReadableFile_isWrittenOver_withoutANotice() throws IOException
	{
		File file = new File(directory, "settings.json");
		JsonFileStore.write(remembered("/home/user/old.mcrdb"), file, Remembered.class, CLOCK);

		JsonFileStore.write(remembered("/home/user/new.mcrdb"), file, Remembered.class, CLOCK);

		assertEquals("/home/user/new.mcrdb",
			JsonFileStore.read(file, Remembered.class).orElseThrow().path);
		assertEquals(List.of("settings.json"), Arrays.asList(directory.list()),
			"no file kept aside and no file left half written");
		assertTrue(notices.isEmpty(), "notices: " + notices);
	}

	@Test
	@DisplayName("a second unreadable file in the same second is kept under a numbered name, the first one stays")
	void aSecondUnreadableFileInTheSameSecond_getsANumber() throws IOException
	{
		File file = new File(directory, "settings.json");
		Files.writeString(file.toPath(), "first");
		JsonFileStore.write(remembered("/a"), file, Remembered.class, CLOCK);
		Files.writeString(file.toPath(), "second");

		JsonFileStore.write(remembered("/b"), file, Remembered.class, CLOCK);

		assertEquals("first", Files
			.readString(new File(directory, "settings.json.unreadable-20261119T153012").toPath()));
		assertEquals("second", Files.readString(
			new File(directory, "settings.json.unreadable-20261119T153012-2").toPath()));
		assertEquals(2, notices.size());
	}

	@Test
	@DisplayName("a file that is not there reads as nothing, and writing it creates it without a notice")
	void aMissingFile_readsAsNothing_andIsCreated() throws IOException
	{
		File file = new File(new File(directory, "not-yet"), "settings.json");

		assertTrue(JsonFileStore.read(file, Remembered.class).isEmpty());
		JsonFileStore.write(remembered("/c"), file, Remembered.class, CLOCK);

		assertEquals("/c", JsonFileStore.read(file, Remembered.class).orElseThrow().path);
		assertFalse(notices.iterator().hasNext(), "notices: " + notices);
	}

	@Test
	@DisplayName("a notice published before anybody listens waits for the listener")
	void aNoticeBeforeTheListener_waitsForIt()
	{
		UnreadableFileNotices.listen(null);
		UnreadableFileNotices.SetAside early = new UnreadableFileNotices.SetAside(
			new File(directory, "a.json"), new File(directory, "a.json.unreadable-x"));
		UnreadableFileNotices.publish(early);
		assertTrue(notices.isEmpty(), "nobody listens yet");

		UnreadableFileNotices.listen(notices::add);

		assertEquals(List.of(early), notices);
	}

	private static Remembered remembered(final String path)
	{
		Remembered remembered = new Remembered();
		remembered.path = path;
		return remembered;
	}
}
