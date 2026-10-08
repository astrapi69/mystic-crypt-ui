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

import java.util.List;
import java.util.function.BinaryOperator;

/**
 * The short form of lethenon's launch checklist for the "Before you start" view (#540): what a
 * public start of lethenon's main chain needs an authorisation for under MiCA (Regulation (EU)
 * 2023/1114) and what it does not, one row per activity.
 * <p>
 * It is a copy in short, and the documents are the reference: the rows follow the table of
 * lethenon's {@code docs/launch/launch-checklist.md} as reviewed on 2026-10-08, and are re-checked
 * with it before the main chain starts (lethenon#144). Nothing here is legal advice, which is what
 * {@link #notice(BinaryOperator)} says first.
 * <p>
 * Every text is looked up through the function it is given, a key and the English default in, the
 * text out: the view passes {@link LethenonMessages#getString(String, String)}, a test passes one
 * bundle of an explicit language.
 */
public final class LaunchChecklist
{

	/** The prefix of every key of the view, its menu item aside */
	public static final String KEY_PREFIX = "lethenon.start.";

	/** The rows in their order, with their English text */
	private static final List<Row> ROWS = List.of(
		new Row("code", "Publish the code, under the MIT licence", "not required",
				"Art. 2(1)"),
		new Row("test.network", "Run the open test network and its faucet",
				"not required", "Art. 2(1)"),
		new Row("mining", "Mine LETH on the main chain, and pass on what was mined",
				"not required, while no listing is announced and no white paper is drawn up",
				"Art. 4(3)(b), 4(4), 4(8)"),
		new Row("seed.nodes", "Run seed nodes that relay blocks and transfers",
				"not required: nodes and miners are not a transfer service", "recital 93"),
		new Row("wallet", "Publish a wallet whose keys stay on the user's machine",
				"not required, as the definition of custody reads; to be confirmed with a lawyer",
				"Art. 3(1)(17)"),
		new Row("custody", "Hold LETH for others, or transfer it on their behalf",
				"not required, while LETH is not admitted to a trading platform", "Art. 4(5)"),
		new Row("services",
				"Exchange LETH for money or crypto-assets as a business, run a trading platform, advise",
				"required: authorisation as a crypto-asset service provider", "Art. 59"),
		new Row("admission", "Seek admission to trading, or announce the intention",
				"needs a legal person and a white paper, and ends the exemption for mined coins",
				"Art. 4(4), Art. 5"),
		new Row("white.paper", "Publish a \"white paper\"",
				"all of Title II of MiCA then applies", "Art. 4(8)"),
		new Row("outside.eu", "Serve people in the EU through a company outside it",
				"required, unless at the client's own exclusive initiative, which any solicitation ends",
				"Art. 59, Art. 61"),
		new Row("personal.data",
				"Hand out LETH for an e-mail address or other personal data",
				"not \"offered for free\"", "Art. 4(3), 2nd subparagraph"));

	private LaunchChecklist()
	{
	}

	/**
	 * One row as the code carries it: the key its translations are filed under, and its English
	 * text, which is the default when a key is missing
	 */
	private record Row(String key, String activity, String authorisation, String provision) {

		LaunchCheck in(final BinaryOperator<String> text)
		{
			String prefix = KEY_PREFIX + "check." + key + ".";
			return new LaunchCheck(text.apply(prefix + "activity", activity),
				text.apply(prefix + "authorisation", authorisation),
				text.apply(prefix + "provision", provision));
		}
	}

	/**
	 * Gets the rows of the short checklist
	 * @param text
	 *            looks up a key, falling back to the default it is given
	 * @return the rows, in the checklist's order
	 */
	public static List<LaunchCheck> checks(final BinaryOperator<String> text)
	{
		return ROWS.stream().map(row -> row.in(text)).toList();
	}

	/**
	 * Gets the notice the view starts with
	 * @param text
	 *            looks up a key, falling back to the default it is given
	 * @return the notice that none of this is legal advice
	 */
	public static String notice(final BinaryOperator<String> text)
	{
		return text.apply(KEY_PREFIX + "notice",
			"Not legal advice. Check with a lawyer before a public launch.");
	}
}
