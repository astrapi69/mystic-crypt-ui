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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.pw.GeneratePasswordModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * No model bean prints a secret in its {@code toString}. The rule exists because a Lombok
 * {@code toString} is not a debugging aid that stays in the debugger: Swing's tree copies a node's
 * {@code toString} to the system clipboard on Ctrl+C, and that string carried every password of
 * every entry in the node (#388). A failing UI test writes the same string into a build log.
 * <p>
 * Two halves, because either alone is a stand-in. The behavioural half puts a sentinel into the
 * real beans and reads their real {@code toString}, in both forms a {@code char[]} can appear in -
 * as text and as {@code [a, b, c]}. The scanning half walks every main source with a Lombok
 * {@code toString} and requires every field that can hold key material to be excluded from it, so a
 * bean written tomorrow cannot bring the leak back by not being on the list below.
 */
class NoModelBeanPrintsASecretTest
{

	private static final String SENTINEL = "sentinel-secret-7f2b";

	/**
	 * A FIELD declaration of a type that can hold key material, not preceded by a
	 * {@code @ToString.Exclude} within the annotation block above it.
	 * <p>
	 * Exactly one tab of indentation: that is class level in this codebase, which the Eclipse
	 * formatter keeps to tabs. A local variable or a parameter of the same type sits deeper, is
	 * never printed by a {@code toString}, and cannot carry the annotation - the first draft of
	 * this pattern matched three of those in {@code ExtensionInfoModel} and would have demanded an
	 * annotation the compiler rejects.
	 */
	private static final Pattern KEY_MATERIAL_FIELD = Pattern
		.compile("^\\t(?!\\t)(?:(?:private|protected|public|transient|final)[ \\t]+)*"
			+ "(char\\[\\]|byte\\[\\]|KeyModel|KeyInfo|KeyInfoModel|PrivateKey|MasterPwFileModelBean)"
			+ "[ \\t]+([A-Za-z_][A-Za-z0-9_]*)[ \\t]*(?:=[^;]*)?;", Pattern.MULTILINE);

	@Test
	@DisplayName("an entry's toString carries neither its password nor the repeat of it")
	void anEntryDoesNotPrintItsPassword()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder()
			.title("a title".toCharArray()).userName("someone".toCharArray())
			.password(SENTINEL.toCharArray()).repeat(SENTINEL.toCharArray()).build();

		assertDoesNotCarryTheSentinel(entry.toString(), "MysticCryptEntryModelBean");
	}

	@Test
	@DisplayName("the sign-in bean's toString carries neither the master password nor its repeat")
	void theSignInBeanDoesNotPrintTheMasterPassword()
	{
		MasterPwFileModelBean credentials = MasterPwFileModelBean.builder()
			.masterPw(SENTINEL.toCharArray()).repeatPw(SENTINEL.toCharArray()).build();

		assertDoesNotCarryTheSentinel(credentials.toString(), "MasterPwFileModelBean");
	}

	@Test
	@DisplayName("the application model's toString does not print the master password through the sign-in bean")
	void theApplicationModelDoesNotPrintTheMasterPassword()
	{
		ApplicationModelBean applicationModel = new ApplicationModelBean();
		applicationModel.setMasterPwFileModelBean(
			MasterPwFileModelBean.builder().masterPw(SENTINEL.toCharArray()).build());

		assertDoesNotCarryTheSentinel(applicationModel.toString(), "ApplicationModelBean");
	}

	@Test
	@DisplayName("a generated password is not printed by the generator's bean")
	void theGeneratorBeanDoesNotPrintThePassword()
	{
		GeneratePasswordModelBean generated = GeneratePasswordModelBean.builder()
			.password(SENTINEL.toCharArray()).build();

		assertDoesNotCarryTheSentinel(generated.toString(), "GeneratePasswordModelBean");
	}

	/**
	 * Every main source with a Lombok {@code toString} and a field that can hold key material - a
	 * {@code char[]}, a {@code byte[]}, or one of the key-holding model types - excludes that field
	 * from {@code toString}. This is the half that catches the bean nobody thought to add above.
	 */
	@Test
	@DisplayName("every Lombok toString in the main sources excludes the fields that can hold key material")
	void everyLombokToStringExcludesKeyMaterial() throws IOException
	{
		List<String> unguarded = new ArrayList<>();
		try (Stream<Path> sources = Files.walk(Path.of("src", "main", "java")))
		{
			sources.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
				String source = read(path);
				if (!hasALombokToString(source))
				{
					return;
				}
				for (String field : keyMaterialFieldsWithoutExclude(source))
				{
					unguarded.add(path.getFileName() + ": " + field);
				}
			});
		}

		assertTrue(unguarded.isEmpty(),
			"these fields can hold key material and would be printed by a Lombok toString - mark "
				+ "each with @ToString.Exclude (and, for a password, @EqualsAndHashCode.Exclude), "
				+ "because a tree node's toString reaches the clipboard (#388): " + unguarded);
	}

	private static void assertDoesNotCarryTheSentinel(final String printed, final String bean)
	{
		assertFalse(printed.contains(SENTINEL),
			bean + ".toString() prints the secret as text: " + printed);
		assertFalse(printed.contains(Arrays.toString(SENTINEL.toCharArray())),
			bean + ".toString() prints the secret as a character array: " + printed);
	}

	private static boolean hasALombokToString(final String source)
	{
		return Pattern.compile("^@(Data|ToString|Value)\\b", Pattern.MULTILINE).matcher(source)
			.find();
	}

	private static List<String> keyMaterialFieldsWithoutExclude(final String source)
	{
		List<String> unguarded = new ArrayList<>();
		Matcher matcher = KEY_MATERIAL_FIELD.matcher(source);
		while (matcher.find())
		{
			// the annotation block is the run of lines directly above the declaration that start
			// with '@' - an Exclude anywhere in it guards this field, one further up does not
			String[] lines = source.substring(0, matcher.start()).split("\\n");
			boolean excluded = false;
			for (int index = lines.length - 1; index >= 0; index--)
			{
				String line = lines[index].trim();
				if (!line.startsWith("@"))
				{
					break;
				}
				if (line.contains("ToString.Exclude"))
				{
					excluded = true;
				}
			}
			if (!excluded)
			{
				unguarded.add(matcher.group(1) + " " + matcher.group(2));
			}
		}
		return unguarded;
	}

	private static String read(final Path path)
	{
		try
		{
			return Files.readString(path, StandardCharsets.UTF_8);
		}
		catch (IOException exception)
		{
			throw new IllegalStateException("cannot read " + path, exception);
		}
	}
}
