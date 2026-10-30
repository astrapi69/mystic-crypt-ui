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

import java.lang.reflect.Constructor;

import org.assertj.swing.core.BasicComponentFinder;
import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.ComponentFinder;
import org.assertj.swing.core.Robot;
import org.assertj.swing.hierarchy.ComponentHierarchy;
import org.assertj.swing.hierarchy.NewHierarchy;
import org.assertj.swing.lock.ScreenLock;

/**
 * Builds the robot and the finders of the UI tests over a {@link NullRootsSkippingHierarchy}
 * (#484).
 * <p>
 * AssertJ-Swing 3.17.1 offers no public way to give a robot or a finder a hierarchy of one's own:
 * {@code BasicRobot}'s constructor is package-private and {@code BasicComponentFinder}'s is
 * protected on a final class. Both are called reflectively here, with exactly what
 * {@code BasicRobot.robotWithNewAwtHierarchy()} passes, apart from the hierarchy. The library has
 * had no release since 2020, so the constructors this depends on do not move.
 */
final class UiTestRobots
{

	private UiTestRobots()
	{
	}

	/**
	 * What {@code BasicRobot.robotWithNewAwtHierarchy()} returns, over a hierarchy without null
	 * roots: it takes the screen lock, and sees only the components created after it
	 *
	 * @return the robot
	 */
	static Robot withNewAwtHierarchy()
	{
		Object screenLockOwner = new Object();
		ScreenLock.instance().acquire(screenLockOwner);
		try
		{
			Constructor<BasicRobot> constructor = BasicRobot.class
				.getDeclaredConstructor(Object.class, ComponentHierarchy.class);
			constructor.setAccessible(true);
			return constructor.newInstance(screenLockOwner,
				new NullRootsSkippingHierarchy(NewHierarchy.ignoreExistingComponents()));
		}
		catch (ReflectiveOperationException | RuntimeException failure)
		{
			ScreenLock.instance().release(screenLockOwner);
			throw new IllegalStateException(
				"could not build the AssertJ-Swing robot over a hierarchy without null roots",
				failure);
		}
	}

	/**
	 * A finder over the given hierarchy, built the way the library builds its own
	 *
	 * @param hierarchy
	 *            the hierarchy the finder searches and prints
	 * @return the finder
	 */
	static ComponentFinder finderOver(final ComponentHierarchy hierarchy)
	{
		try
		{
			Constructor<BasicComponentFinder> constructor = BasicComponentFinder.class
				.getDeclaredConstructor(ComponentHierarchy.class);
			constructor.setAccessible(true);
			return constructor.newInstance(hierarchy);
		}
		catch (ReflectiveOperationException failure)
		{
			throw new IllegalStateException("could not build an AssertJ-Swing component finder",
				failure);
		}
	}
}
