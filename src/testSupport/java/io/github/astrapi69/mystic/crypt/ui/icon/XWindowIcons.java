/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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
package io.github.astrapi69.mystic.crypt.ui.icon;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the icon a window carries on the X server, which is the icon a window manager shows in the
 * title bar, the task bar and the window switcher: the window's {@code _NET_WM_ICON}, found by the
 * window's title in the X server's window tree ({@code xwininfo -root -tree}) and read with
 * {@code xprop} (#815). A window that carries no icon of its own gets the one AWT takes from its
 * owner, so this is what the user sees, not what a Java property says.
 * <p>
 * The window tree comes from the X server itself. {@code wmctrl -l}, which this used first, reads
 * the window manager's {@code _NET_CLIENT_LIST} and failed on develop for ten seconds in a row with
 * "BadWindow" when that list still held a window that was gone (run 45830430466). Both programs
 * used now come with x11-utils, which every test display has, and both talk to the display the test
 * JVM was given in {@code DISPLAY}.
 */
public final class XWindowIcons
{

	private static final long WINDOW_TIMEOUT_MILLIS = 10_000;

	private static final long POLL_MILLIS = 100;

	private static final long PROGRAM_TIMEOUT_SECONDS = 20;

	private static final Pattern NUMBER = Pattern.compile("\\d+");

	/** A window of the tree with a title: its id and its title */
	private static final Pattern TREE_LINE = Pattern
		.compile("\\s*(0x[0-9a-fA-F]+) \"(.*)\": \\(.*");

	private XWindowIcons()
	{
	}

	/**
	 * The pixels of the 16 by 16 icon the window with the given title carries on the X server, as
	 * ARGB values row by row, waiting up to ten seconds for the window manager to list the window
	 *
	 * @param title
	 *            the window's title, exactly
	 * @return 256 ARGB values
	 * @throws IOException
	 *             if {@code xwininfo} or {@code xprop} cannot be run
	 * @throws InterruptedException
	 *             if the wait is interrupted
	 * @throws IllegalStateException
	 *             if no window has that title, or the window carries no 16 by 16 icon; the message
	 *             names the sizes it does carry
	 */
	public static int[] sixteenBySixteen(final String title)
		throws IOException, InterruptedException
	{
		List<XIcon> icons = iconsOf(title);
		return icons.stream().filter(icon -> icon.width() == 16 && icon.height() == 16).findFirst()
			.map(XIcon::argb).orElseThrow(() -> new IllegalStateException("the window '" + title
				+ "' carries no 16 x 16 icon on the X server, only " + sizes(icons)));
	}

	/**
	 * Every icon the window with the given title carries on the X server, in the order of its
	 * {@code _NET_WM_ICON}
	 *
	 * @param title
	 *            the window's title, exactly
	 * @return the icons; empty if the window carries none
	 * @throws IOException
	 *             if {@code xwininfo} or {@code xprop} cannot be run
	 * @throws InterruptedException
	 *             if the wait is interrupted
	 */
	public static List<XIcon> iconsOf(final String title) throws IOException, InterruptedException
	{
		String windowId = awaitWindowId(title, () -> run("xwininfo", "-root", "-tree"),
			WINDOW_TIMEOUT_MILLIS);
		return parse(run("xprop", "-id", windowId, "-notype", "32c", "_NET_WM_ICON"));
	}

	/**
	 * The icons an {@code xprop -notype 32c _NET_WM_ICON} line holds: width, height and that many
	 * ARGB values, one icon after the other
	 *
	 * @param xpropOutput
	 *            what xprop printed
	 * @return the icons; empty if the property is not set
	 */
	static List<XIcon> parse(final String xpropOutput)
	{
		List<Long> numbers = new ArrayList<>();
		int equals = xpropOutput.indexOf('=');
		if (equals < 0)
		{
			return List.of();
		}
		Matcher matcher = NUMBER.matcher(xpropOutput.substring(equals + 1));
		while (matcher.find())
		{
			numbers.add(Long.parseLong(matcher.group()));
		}
		List<XIcon> icons = new ArrayList<>();
		int position = 0;
		while (position + 2 <= numbers.size())
		{
			int width = numbers.get(position).intValue();
			int height = numbers.get(position + 1).intValue();
			int end = position + 2 + width * height;
			if (width <= 0 || height <= 0 || end > numbers.size())
			{
				break;
			}
			int[] argb = new int[width * height];
			for (int pixel = 0; pixel < argb.length; pixel++)
			{
				argb[pixel] = (int)numbers.get(position + 2 + pixel).longValue();
			}
			icons.add(new XIcon(width, height, argb));
			position = end;
		}
		return icons;
	}

	private static String sizes(final List<XIcon> icons)
	{
		return icons.isEmpty()
			? "none"
			: icons.stream().map(icon -> icon.width() + "x" + icon.height()).toList().toString();
	}

	/**
	 * The id of the window with the given title, reading the listing again until the window is in
	 * it or the time is up. A listing that fails is read again too: a program that walks the
	 * windows ends with "BadWindow (invalid Window parameter)" when a window goes away while it
	 * reads it, which happens while the application starts (develop {@code ad2471d2}, run
	 * 45821007085)
	 *
	 * @param title
	 *            the window's title, exactly
	 * @param listing
	 *            reads the current {@code xwininfo -root -tree} listing
	 * @param timeoutMillis
	 *            how long to keep reading
	 * @return the window's id
	 * @throws InterruptedException
	 *             if the wait is interrupted
	 * @throws IllegalStateException
	 *             if the time is up; the message names the last listing, or the last failure, which
	 *             is also the cause
	 */
	static String awaitWindowId(final String title, final WindowListing listing,
		final long timeoutMillis) throws InterruptedException
	{
		long deadline = System.currentTimeMillis() + timeoutMillis;
		String windows = "";
		IOException lastFailure = null;
		while (System.currentTimeMillis() < deadline)
		{
			try
			{
				windows = listing.read();
				lastFailure = null;
				Optional<String> windowId = windowIdIn(windows, title);
				if (windowId.isPresent())
				{
					return windowId.get();
				}
			}
			catch (IOException failure)
			{
				lastFailure = failure;
			}
			Thread.sleep(POLL_MILLIS);
		}
		String lastRead = lastFailure == null
			? "xwininfo -root -tree listed:\n" + windows
			: "the last xwininfo -root -tree failed: " + lastFailure.getMessage();
		throw new IllegalStateException("no window titled '" + title + "' on the X server after "
			+ timeoutMillis + " ms; " + lastRead, lastFailure);
	}

	/**
	 * The id of the window with the given title in an {@code xwininfo -root -tree} listing, whose
	 * lines are {@code <id> "<title>": (...)}; a window without a title is listed as
	 * {@code (has no name)} and never matches
	 *
	 * @param listing
	 *            what xwininfo printed
	 * @param title
	 *            the window's title, exactly
	 * @return the id of the first window with that title, or empty if none has it
	 */
	static Optional<String> windowIdIn(final String listing, final String title)
	{
		return listing.lines().map(TREE_LINE::matcher).filter(Matcher::matches)
			.filter(line -> line.group(2).equals(title)).map(line -> line.group(1)).findFirst();
	}

	private static String run(final String... command) throws IOException, InterruptedException
	{
		Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
		String output;
		try (InputStream stream = process.getInputStream())
		{
			output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
		if (!process.waitFor(PROGRAM_TIMEOUT_SECONDS, TimeUnit.SECONDS))
		{
			process.destroyForcibly();
			throw new IOException(String.join(" ", command) + " did not end within "
				+ PROGRAM_TIMEOUT_SECONDS + " s");
		}
		if (process.exitValue() != 0)
		{
			throw new IOException(String.join(" ", command) + " ended with exit code "
				+ process.exitValue() + ": " + output);
		}
		return output;
	}

	/**
	 * Reads the window tree of the X server, the way {@code xwininfo -root -tree} prints it
	 */
	@FunctionalInterface
	interface WindowListing
	{

		/**
		 * The current listing
		 *
		 * @return one line per window: {@code <id> "<title>": ("<instance>" "<class>") <geometry>},
		 *         or {@code <id> (has no name): ...} for a window without a title
		 * @throws IOException
		 *             if the listing cannot be read
		 * @throws InterruptedException
		 *             if the reading is interrupted
		 */
		String read() throws IOException, InterruptedException;
	}

	/**
	 * One icon of a window's {@code _NET_WM_ICON}
	 *
	 * @param width
	 *            its width in pixels
	 * @param height
	 *            its height in pixels
	 * @param argb
	 *            its pixels as ARGB values, row by row
	 */
	public record XIcon(int width, int height, int[] argb) {
	}
}
