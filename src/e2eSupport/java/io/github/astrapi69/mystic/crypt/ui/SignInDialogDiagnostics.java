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
package io.github.astrapi69.mystic.crypt.ui;

import java.awt.AWTEvent;
import java.awt.Dialog;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.swing.core.Robot;
import org.assertj.swing.hierarchy.NewHierarchy;

/**
 * Records what is needed to tell, when the lookup of the sign-in dialog times out, why the lookup
 * did not list a dialog that is on screen (#1070).
 * <p>
 * Two runs on CI failed at that lookup, 15 seconds after the application had reported its sign-in
 * dialog open: the harness listed the dialog as showing while the robot's own hierarchy listed only
 * the shared invisible frame and an empty application frame. A rerun was green and nothing local
 * reproduced it, so the next failure has to carry the answer: who owns the dialog, whether the
 * robot's {@code WindowFilter} counts the dialog or its owner as ignored, when the robot was built
 * against each shutdown and launch of the test, which method of this JVM it was, and in which order
 * the windows were opened and closed.
 * <p>
 * It changes no behaviour: it listens for window events and reads the robot's filter by reflection,
 * in test support only.
 */
final class SignInDialogDiagnostics
{

	/** The title of the sign-in dialog */
	static final String SIGN_IN_TITLE = "Enter your credentials";

	private static final AtomicInteger METHODS_IN_THIS_JVM = new AtomicInteger();

	private final List<Mark> marks = new CopyOnWriteArrayList<>();

	private final List<WindowRecord> windowEvents = new CopyOnWriteArrayList<>();

	private final AWTEventListener listener = this::record;

	private int methodNumber;

	private long robotCreatedNanos;

	/** A point of the test, by name and time */
	record Mark(String label, long nanos) {
	}

	/** A window event, by kind, window and time */
	record WindowRecord(String kind, String window, long nanos) {
	}

	/** One window of the owner chain, as the filter sees it */
	record WindowFacts(String name, boolean displayable, boolean ignored,
		boolean implicitlyIgnored) {
	}

	/** A sign-in dialog with its owner chain, nearest owner first */
	record DialogFacts(String dialog, boolean showing, boolean ignored, boolean implicitlyIgnored,
		List<WindowFacts> owners) {
	}

	/**
	 * Starts listening and counts this test as one more method of the JVM. Call right before the
	 * robot is built
	 */
	void beginTest()
	{
		methodNumber = METHODS_IN_THIS_JVM.incrementAndGet();
		Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.WINDOW_EVENT_MASK);
	}

	/** Names the moment the robot was built; the other times are shown relative to it */
	void robotCreated()
	{
		robotCreatedNanos = System.nanoTime();
		marks.add(new Mark("robot created", robotCreatedNanos));
	}

	/**
	 * Names a point of the test
	 *
	 * @param label
	 *            what has just begun or ended
	 */
	void mark(final String label)
	{
		marks.add(new Mark(label, System.nanoTime()));
	}

	/** Stops listening, after the test */
	void endTest()
	{
		Toolkit.getDefaultToolkit().removeAWTEventListener(listener);
	}

	private void record(final AWTEvent event)
	{
		int id = event.getID();
		if (id == WindowEvent.WINDOW_OPENED || id == WindowEvent.WINDOW_CLOSED)
		{
			windowEvents.add(new WindowRecord(
				id == WindowEvent.WINDOW_OPENED ? "WINDOW_OPENED" : "WINDOW_CLOSED",
				identify(event.getSource()), System.nanoTime()));
		}
	}

	/**
	 * The report for a failed lookup of the sign-in dialog, as lines for the build to forward
	 *
	 * @param robot
	 *            the robot whose hierarchy did not list the dialog
	 * @return the report
	 */
	String report(final Robot robot)
	{
		return format(methodNumber, robotCreatedNanos, System.nanoTime(), marks, windowEvents,
			signInDialogsOf(robot));
	}

	/**
	 * Formats the record. Every time is shown in milliseconds since the robot was built
	 *
	 * @param methodNumber
	 *            which test method of the JVM this is, counted from 1
	 * @param robotCreatedNanos
	 *            when the robot was built
	 * @param nowNanos
	 *            when the lookup gave up
	 * @param marks
	 *            the named points of the test
	 * @param windowEvents
	 *            the windows opened and closed
	 * @param dialogs
	 *            the sign-in dialogs that exist
	 * @return the report
	 */
	static String format(final int methodNumber, final long robotCreatedNanos, final long nowNanos,
		final List<Mark> marks, final List<WindowRecord> windowEvents,
		final List<DialogFacts> dialogs)
	{
		StringBuilder report = new StringBuilder("sign-in dialog lookup diagnosis (#1070)\n");
		report.append("this is test method number ").append(methodNumber)
			.append(" of this JVM; times are milliseconds since the robot was built\n");
		report.append("the lookup gave up at +").append(since(robotCreatedNanos, nowNanos))
			.append('\n');
		report.append("points of the test:\n");
		marks.forEach(mark -> report.append("  +").append(since(robotCreatedNanos, mark.nanos()))
			.append(' ').append(mark.label()).append('\n'));
		appendDialogs(report, dialogs);
		report.append("windows opened and closed:\n");
		windowEvents
			.forEach(event -> report.append("  +").append(since(robotCreatedNanos, event.nanos()))
				.append(' ').append(event.kind()).append(' ').append(event.window()).append('\n'));
		return report.toString();
	}

	private static void appendDialogs(final StringBuilder report, final List<DialogFacts> dialogs)
	{
		if (dialogs.isEmpty())
		{
			report.append("no window titled \"").append(SIGN_IN_TITLE).append("\" exists\n");
			return;
		}
		report.append(dialogs.size()).append(" window(s) titled \"").append(SIGN_IN_TITLE)
			.append("\":\n");
		for (DialogFacts dialog : dialogs)
		{
			report.append("  ").append(dialog.dialog()).append(" showing=").append(dialog.showing())
				.append(" ignored=").append(dialog.ignored()).append(" implicitlyIgnored=")
				.append(dialog.implicitlyIgnored()).append('\n');
			String indent = "    owner: ";
			for (WindowFacts owner : dialog.owners())
			{
				report.append(indent).append(owner.name()).append(" displayable=")
					.append(owner.displayable()).append(" ignored=").append(owner.ignored())
					.append(" implicitlyIgnored=").append(owner.implicitlyIgnored()).append('\n');
				indent = "    owner of that: ";
			}
			if (dialog.owners().isEmpty())
			{
				report.append("    no owner (null)\n");
			}
		}
	}

	private static String since(final long fromNanos, final long toNanos)
	{
		return String.valueOf((toNanos - fromNanos) / 1_000_000);
	}

	private static String identify(final Object window)
	{
		StringBuilder name = new StringBuilder(window.getClass().getName()).append('@')
			.append(Integer.toHexString(System.identityHashCode(window)));
		if (window instanceof Dialog dialog)
		{
			name.append(" title='").append(dialog.getTitle()).append('\'');
		}
		else if (window instanceof java.awt.Frame frame)
		{
			name.append(" title='").append(frame.getTitle()).append('\'');
		}
		return name.toString();
	}

	private static List<DialogFacts> signInDialogsOf(final Robot robot)
	{
		List<DialogFacts> dialogs = new ArrayList<>();
		Object filter = filterOf(robot);
		for (Window window : Window.getWindows())
		{
			if (window instanceof Dialog dialog && SIGN_IN_TITLE.equals(dialog.getTitle()))
			{
				List<WindowFacts> owners = new ArrayList<>();
				for (Window owner = window.getOwner(); owner != null; owner = owner.getOwner())
				{
					owners.add(new WindowFacts(identify(owner), owner.isDisplayable(),
						ask(filter, "isIgnored", owner),
						ask(filter, "isImplicitlyIgnored", owner)));
				}
				dialogs.add(new DialogFacts(identify(dialog), dialog.isShowing(),
					ask(filter, "isIgnored", dialog), ask(filter, "isImplicitlyIgnored", dialog),
					owners));
			}
		}
		return dialogs;
	}

	/** The robot's {@code WindowFilter}, or null when reflection cannot reach it */
	private static Object filterOf(final Robot robot)
	{
		try
		{
			Field robotHierarchy = robot.getClass().getDeclaredField("hierarchy");
			robotHierarchy.setAccessible(true);
			Object hierarchy = robotHierarchy.get(robot);
			if (hierarchy instanceof NullRootsSkippingHierarchy skipping)
			{
				hierarchy = skipping.delegate();
			}
			Field filter = NewHierarchy.class.getDeclaredField("filter");
			filter.setAccessible(true);
			return filter.get(hierarchy);
		}
		catch (ReflectiveOperationException | RuntimeException failure)
		{
			return null;
		}
	}

	private static boolean ask(final Object filter, final String method, final Window window)
	{
		if (filter == null)
		{
			return false;
		}
		try
		{
			Method question = filter.getClass().getDeclaredMethod(method, java.awt.Component.class);
			question.setAccessible(true);
			return (Boolean)question.invoke(filter, window);
		}
		catch (ReflectiveOperationException | RuntimeException failure)
		{
			return false;
		}
	}
}
