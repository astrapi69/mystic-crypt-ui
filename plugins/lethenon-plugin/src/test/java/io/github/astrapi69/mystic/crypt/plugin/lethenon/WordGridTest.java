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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import static io.github.astrapi69.mystic.crypt.plugin.lethenon.Windows.component;
import static io.github.astrapi69.mystic.crypt.plugin.lethenon.Windows.textOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.lethenon.Wallet;

/**
 * The 24 words of a restore or a backup check are typed into 24 fields, each with its number,
 * behind one model (#804): a pasted phrase fills all 24 whichever field it lands in, and a field
 * left with a word that is not in the word list is marked and named at once, not only when the
 * form is sent
 */
class WordGridTest
{

	/** The client property FlatLaf draws a field's outline from */
	private static final String OUTLINE = "JComponent.outline";

	private final TypedWords words = new TypedWords();

	private final List<String> said = new ArrayList<>();

	private final WordGrid grid = new WordGrid("Words", words, Locale.ENGLISH, said::add);

	private final List<String> phrase = List.of(Wallet.create().phrase().split(" "));

	@Test
	@DisplayName("the grid has 24 fields, each labelled with its number, in the order of the backup sheet")
	void theFields_areNumberedInTheOrderOfTheBackupSheet()
	{
		for (int position = 1; position <= 24; position++)
		{
			assertEquals(String.valueOf(position), textOf(grid, "lblWords" + position));
			assertEquals("", textOf(grid, "txtWords" + position));
		}
		// four columns read from top to bottom, as step 1 shows the words: the first row is 1, 7, 13, 19
		List<String> firstRow = Stream.of(grid.getComponents()).filter(JLabel.class::isInstance)
			.limit(4).map(label -> ((JLabel)label).getText()).toList();
		assertEquals(List.of("1", "7", "13", "19"), firstRow);
	}

	@Test
	@DisplayName("a word typed into a field is that position of the model")
	void typingIntoAField_setsItsPositionOfTheModel()
	{
		field(5).setText("abandon");

		assertEquals("abandon", words.get(5));
		assertEquals("abandon", words.phrase());
	}

	@ParameterizedTest(name = "pasted into field {0}")
	@ValueSource(ints = { 1, 13, 24 })
	@DisplayName("a pasted phrase fills all 24 fields, whichever field it is pasted into")
	void aPastedPhrase_fillsAllTwentyFourFields(final int target)
	{
		paste(target, String.join(" ", phrase));

		for (int position = 1; position <= 24; position++)
		{
			assertEquals(phrase.get(position - 1), textOf(grid, "txtWords" + position));
		}
		assertEquals(String.join(" ", phrase), words.phrase());
	}

	@Test
	@DisplayName("a phrase pasted with the numbers of the backup sheet fills the fields with the words alone")
	void aNumberedPaste_fillsTheFieldsWithTheWordsAlone()
	{
		String numbered = String.join("\n", IntStream.range(0, 24)
			.mapToObj(index -> (index + 1) + ". " + phrase.get(index)).toList());

		paste(1, numbered);

		assertEquals(String.join(" ", phrase), words.phrase());
		assertEquals(phrase.getFirst(), textOf(grid, "txtWords1"));
	}

	/**
	 * A paste of several words is a phrase, so it starts at field 1 and replaces what the fields
	 * held; words that do not fit stay in field 24, so nothing pasted is lost and the count check
	 * names how many there were
	 */
	@ParameterizedTest(name = "{argumentSetName}")
	@MethodSource("pastesOfSeveralWords")
	void aPasteOfSeveralWords_startsAtTheFirstField(final int count, final long filled)
	{
		List<String> pasted = new ArrayList<>(phrase);
		pasted.add("zoo");
		IntStream.rangeClosed(1, 24).forEach(position -> field(position).setText("abandon"));

		paste(7, String.join(" ", pasted.subList(0, count)));

		assertEquals(pasted.getFirst(), textOf(grid, "txtWords1"));
		assertEquals(filled, IntStream.rangeClosed(1, 24)
			.filter(position -> !textOf(grid, "txtWords" + position).isEmpty()).count());
		IntStream.rangeClosed(count + 1, 24).forEach(position -> assertEquals("",
			textOf(grid, "txtWords" + position), "field " + position + " keeps nothing old"));
		assertEquals(String.join(" ", pasted.subList(0, count)), words.phrase());
	}

	static Stream<Arguments> pastesOfSeveralWords()
	{
		return Stream.of(Arguments.argumentSet("two words fill fields 1 and 2", 2, 2L),
			Arguments.argumentSet("23 words leave field 24 empty", 23, 23L),
			Arguments.argumentSet("24 words fill every field", 24, 24L),
			Arguments.argumentSet("25 words fill every field, none lost", 25, 24L));
	}

	@Test
	@DisplayName("25 pasted words keep the 24th and the 25th together in field 24")
	void twentyFivePastedWords_keepTheLastTwoInFieldTwentyFour()
	{
		paste(1, String.join(" ", phrase) + " zoo");

		assertEquals(phrase.get(23) + " zoo", textOf(grid, "txtWords24"));
	}

	@Test
	@DisplayName("a single pasted word goes into its own field, and the other fields keep theirs")
	void aSingleWord_isPastedIntoItsFieldAlone()
	{
		field(1).setText("abandon");

		paste(9, "ability");

		assertEquals("ability", textOf(grid, "txtWords9"));
		assertEquals("abandon", textOf(grid, "txtWords1"));
		assertEquals("abandon ability", words.phrase());
	}

	@ParameterizedTest(name = "pasted: \"{0}\"")
	@ValueSource(strings = { "", " ", "\n\t" })
	@DisplayName("a paste with no word in it changes no field")
	void aPasteWithNoWord_changesNothing(final String pasted)
	{
		field(3).setText("abandon");

		paste(3, pasted);

		assertEquals("abandon", textOf(grid, "txtWords3"));
		assertEquals("abandon", words.phrase());
	}

	@Test
	@DisplayName("a field left with a word that is not in the word list is marked and named at once")
	void leavingAFieldWithAnUnknownWord_marksAndNamesIt()
	{
		field(5).setText("qqq");

		leave(5);

		assertEquals("error", field(5).getClientProperty(OUTLINE));
		assertEquals("Word 5, \"qqq\", is not in the word list. Check your note for a typing error.",
			said.getLast());
		assertEquals(said.getLast(), field(5).getToolTipText());
	}

	@Test
	@DisplayName("correcting a marked word removes the mark and the sentence")
	void correctingAMarkedWord_removesTheMark()
	{
		field(5).setText("qqq");
		leave(5);

		field(5).setText("abandon");

		assertNull(field(5).getClientProperty(OUTLINE));
		assertEquals("", said.getLast());
		assertEquals("the word at this position in your note", field(5).getToolTipText());
	}

	@ParameterizedTest(name = "left with \"{0}\"")
	@ValueSource(strings = { "", "abandon", "ABANDON", " abandon ", "5. abandon" })
	@DisplayName("a field left empty or with a word of the list, in any case or numbered, is not marked")
	void aFieldLeftEmptyOrWithAKnownWord_isNotMarked(final String typed)
	{
		field(5).setText(typed);

		leave(5);

		assertNull(field(5).getClientProperty(OUTLINE));
		assertTrue(said.stream().allMatch(String::isEmpty), said.toString());
	}

	@Test
	@DisplayName("a paste marks every unknown word at once and names the first")
	void aPaste_marksEveryUnknownWordAndNamesTheFirst()
	{
		List<String> mistyped = new ArrayList<>(phrase);
		mistyped.set(3, "qqq");
		mistyped.set(17, "zzz");

		paste(1, String.join(" ", mistyped));

		assertEquals("error", field(4).getClientProperty(OUTLINE));
		assertEquals("error", field(18).getClientProperty(OUTLINE));
		assertNull(field(1).getClientProperty(OUTLINE));
		assertEquals("Word 4, \"qqq\", is not in the word list. Check your note for a typing error.",
			said.getLast());
	}

	@Test
	@DisplayName("emptying the grid empties every field, the model and the marks")
	void clear_emptiesTheFieldsTheModelAndTheMarks()
	{
		List<String> mistyped = new ArrayList<>(phrase);
		mistyped.set(3, "qqq");
		paste(1, String.join(" ", mistyped));

		grid.clear();

		for (int position = 1; position <= 24; position++)
		{
			assertEquals("", textOf(grid, "txtWords" + position));
			assertNull(field(position).getClientProperty(OUTLINE));
		}
		assertEquals("", words.phrase());
		assertEquals("", said.getLast());
	}

	@Test
	@DisplayName("emptying the grid empties the model also where a field already shows nothing")
	void clear_emptiesTheModel_whereAFieldAlreadyShowsNothing()
	{
		words.set(7, "abandon");

		grid.clear();

		assertEquals("", words.get(7));
	}

	@Test
	@DisplayName("the German window names an unknown word in German")
	void theGermanWindow_namesAnUnknownWordInGerman()
	{
		List<String> german = new ArrayList<>();
		WordGrid deutsch = new WordGrid("Words", new TypedWords(), Locale.GERMAN, german::add);
		component(deutsch, "txtWords5", JTextField.class).setText("qqq");

		Windows.leave(deutsch, "txtWords5");

		assertEquals("Wort 5, \"qqq\", gibt es in der Wortliste nicht. Sieh in deiner Notiz nach, "
			+ "vielleicht hast du dich vertippt.", german.getLast());
	}

	private JTextField field(final int position)
	{
		return component(grid, "txtWords" + position, JTextField.class);
	}

	private void paste(final int position, final String text)
	{
		Windows.paste(grid, "txtWords" + position, text);
	}

	private void leave(final int position)
	{
		Windows.leave(grid, "txtWords" + position);
	}
}
