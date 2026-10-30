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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.assertj.swing.core.ComponentFinder;
import org.assertj.swing.exception.ComponentLookupException;
import org.assertj.swing.hierarchy.ComponentHierarchy;
import org.assertj.swing.hierarchy.ExistingHierarchy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A lookup that finds nothing says what it looked for and what was there, also when the hierarchy
 * hands out a null root (#484).
 * <p>
 * AssertJ-Swing 3.17.1 builds that message by printing every root, and its printer refuses a null
 * root with a {@link NullPointerException}. The NPE replaced the message in five CI runs between
 * 2026-10-13 and 2026-10-29. The hierarchy here gives a null root on purpose, the way
 * {@code WindowMonitor.rootWindows()} did on those runs.
 */
class NullRootsSkippingHierarchyTest
{

	private static final String TITLE = "a window the failure message has to list";

	private JFrame frame;

	@BeforeEach
	void showAWindow() throws Exception
	{
		TestPrerequisites.requireADisplay();
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame(TITLE);
			frame.setSize(200, 100);
			frame.setVisible(true);
		});
	}

	@AfterEach
	void disposeIt() throws Exception
	{
		SwingUtilities.invokeAndWait(frame::dispose);
	}

	@Test
	@DisplayName("the library's own finder dies with an NPE on a null root, which is what hid the failures")
	void libraryFinder_diesWithANullPointerException_onANullRoot()
	{
		ComponentFinder finder = finderOver(new WithANullRoot());

		assertThrows(NullPointerException.class, () -> finder.findByName("noComponentHasThisName"));
	}

	@Test
	@DisplayName("with null roots skipped, the lookup fails with its own message, naming what was there")
	void finderOverTheSkippingHierarchy_namesWhatItLookedForAndWhatWasThere()
	{
		ComponentFinder finder = finderOver(new NullRootsSkippingHierarchy(new WithANullRoot()));

		ComponentLookupException failure = assertThrows(ComponentLookupException.class,
			() -> finder.findByName("noComponentHasThisName"));

		assertTrue(failure.getMessage().contains("noComponentHasThisName"),
			"the message has to say what the lookup wanted: " + failure.getMessage());
		assertTrue(failure.getMessage().contains(TITLE),
			"the message has to list the window that was there: " + failure.getMessage());
	}

	@Test
	@DisplayName("everything but the null roots is handed through unchanged")
	void roots_areTheDelegatesWithoutTheNullOnes()
	{
		ComponentHierarchy delegate = new WithANullRoot();

		Collection<Container> roots = new NullRootsSkippingHierarchy(delegate).roots();

		assertTrue(roots.contains(frame), "the real window stays: " + roots);
		assertTrue(roots.stream().allMatch(root -> root != null), "no null root: " + roots);
		assertTrue(roots.size() == delegate.roots().size() - 1,
			"exactly the null root is left out: " + roots);
	}

	private static ComponentFinder finderOver(ComponentHierarchy hierarchy)
	{
		return UiTestRobots.finderOver(hierarchy);
	}

	/** The existing AWT hierarchy, with one null root in front of its windows */
	private static final class WithANullRoot implements ComponentHierarchy
	{

		private final ExistingHierarchy existing = new ExistingHierarchy();

		@Override
		public Collection<Container> roots()
		{
			List<Container> roots = new ArrayList<>();
			roots.add(null);
			roots.addAll(existing.roots());
			return roots;
		}

		@Override
		public Collection<Component> childrenOf(Component component)
		{
			return existing.childrenOf(component);
		}

		@Override
		public Container parentOf(Component component)
		{
			return existing.parentOf(component);
		}

		@Override
		public boolean contains(Component component)
		{
			return component != null && existing.contains(component);
		}

		@Override
		public void dispose(Window window)
		{
			existing.dispose(window);
		}
	}
}
