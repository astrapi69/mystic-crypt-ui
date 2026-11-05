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
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * One line per entry of a vault model, in the same shape for every release from 7.x on, read by
 * reflection so that it runs against whichever application jar is on the class path: the entries
 * from their group's element, where the application keeps them (#757), each with every field 7.x
 * already had - the text fields, the expiry date, the custom properties with their values, and the
 * attachments with the sha256 of their content. Sorted, so two releases' lines compare as text.
 * <p>
 * Used by {@code WriteWithAnOldRelease} (what was written) and {@code OpenWithAnOldRelease} (what was
 * read), which the source launcher compiles together with it.
 */
public final class EntryLines
{

	private EntryLines()
	{
	}

	/**
	 * The lines of the given model
	 *
	 * @param model
	 *            an {@code ApplicationModelBean} of any release
	 * @return one line per entry, sorted
	 */
	public static List<String> of(final Object model) throws Exception
	{
		Map<?, ?> tree = (Map<?, ?>)call(model, "getRootTreeAsMap");
		List<String> lines = new ArrayList<>();
		for (Object node : tree == null ? List.of() : tree.values())
		{
			Object element = call(node, "getValue");
			Object content = call(element, "getDefaultContent");
			for (Object entry : content == null ? List.of() : (List<?>)content)
			{
				lines.add("ENTRY group=" + call(element, "getName") + " title="
					+ text(call(entry, "getTitle")) + " userName=" + text(call(entry, "getUserName"))
					+ " password=" + text(call(entry, "getPassword")) + " url="
					+ text(call(entry, "getUrl")) + " notes=" + text(call(entry, "getNotes"))
					+ " expires=" + call(entry, "getExpires") + " properties="
					+ properties(call(entry, "getProperties")) + " attachments="
					+ attachments(call(entry, "getResources")));
			}
		}
		lines.sort(null);
		return lines;
	}

	private static String properties(final Object list) throws Exception
	{
		List<String> pairs = new ArrayList<>();
		for (Object pair : list == null ? List.of() : (List<?>)list)
		{
			pairs.add(call(pair, "getKey") + "=" + call(pair, "getValue"));
		}
		return pairs.toString();
	}

	private static String attachments(final Object list) throws Exception
	{
		List<String> files = new ArrayList<>();
		for (Object attachment : list == null ? List.of() : (List<?>)list)
		{
			byte[] content = (byte[])call(attachment, "getContent");
			files.add(call(attachment, "getName") + ":" + (content == null
				? "no content"
				: HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content))
					.substring(0, 16)));
		}
		return files.toString();
	}

	private static String text(final Object value)
	{
		if (value instanceof char[] characters)
		{
			return new String(characters);
		}
		return value == null ? "" : value.toString();
	}

	private static Object call(final Object target, final String getter) throws Exception
	{
		Method method = target.getClass().getMethod(getter);
		method.setAccessible(true);
		return method.invoke(target);
	}
}
