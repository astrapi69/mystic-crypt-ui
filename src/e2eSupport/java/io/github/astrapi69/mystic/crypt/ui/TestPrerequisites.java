package io.github.astrapi69.mystic.crypt.ui;

import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assumptions;

/**
 * What an end-to-end test needs before it can measure anything, and what happens when it is missing
 * (#333).
 * <p>
 * A skipped test is a test that does not exist. This suite skipped on two missing prerequisites - a
 * display and a built plugin zip - and the second one hid 43 whole classes for months: the run that
 * merged #306 reported "e2eTest: 113 classes, 199 tests, 56 skipped", and those 56 were every
 * plugin's end-to-end test. Eleven shipped features were verified nowhere but on whoever last ran
 * {@code make plugins} on their own machine. That is {@code quality-checks.md}'s first gate
 * principle exactly: a gate that cannot check must never report green.
 * <p>
 * So a missing prerequisite FAILS. CI satisfies both - it builds the plugins before the suite and
 * runs it under {@code xvfb-run} - so nothing here ever fires there, which is the point: the day it
 * would have fired is the day something broke.
 * <p>
 * <b>The one exception, and it is a switch rather than a condition.</b>
 * {@code -Dmystic.crypt.ui.test.skip.without.prerequisites=true} turns these back into skips, for
 * somebody running a single test locally without building thirteen plugins first. It has to be
 * typed, it is never set in {@code .github/workflows/gradle.yml}, and a run that used it says so in
 * its own skip message. A condition would come back; a switch somebody held down does not.
 */
final class TestPrerequisites
{

	/**
	 * The property that turns a missing prerequisite back into a skip. Typed by hand, for a local
	 * run, and set nowhere in this repository
	 */
	static final String SKIP_INSTEAD_OF_FAILING = "mystic.crypt.ui.test.skip.without.prerequisites";

	private TestPrerequisites()
	{
	}

	/**
	 * Requires a graphical display, because without one an end-to-end test measures nothing.
	 * <p>
	 * This used to be an assumption in the setup of every UI test, so losing the display quietly
	 * removed 149 tests while the build stayed green. CI runs the suite under
	 * {@code xvfb-run -a --server-args="-screen 0 1920x1080x24"} and {@code scripts/e2e-harness.sh}
	 * refuses to start without one, so by the time this is reached a display is supposed to exist.
	 */
	static void requireADisplay()
	{
		refuse(!GraphicsEnvironment.isHeadless(),
			"no graphical display. The end-to-end suite cannot measure anything without one, and "
				+ "skipping 149 tests is not a green result. Run it through 'make test-e2e', which "
				+ "goes via scripts/e2e-harness.sh and starts a display and a window manager");
	}

	/**
	 * Requires the given plugin zip to have been built.
	 *
	 * @param pluginZip
	 *            the zip the test installs
	 */
	static void requireBuiltPluginZip(final Path pluginZip)
	{
		refuse(Files.exists(pluginZip), "plugin zip " + pluginZip + " is not built. Run "
			+ "'make plugins'. A plugin whose test skips because its zip is missing is a plugin "
			+ "nothing verifies");
	}

	/**
	 * Requires every one of the given plugin zips, and names the ones that are missing.
	 * <p>
	 * An OR over several zips is worse than a skip: with four of five missing, the test RUNS,
	 * asserts almost nothing, and reports PASSED - JUnit records no skip, so neither the XML counts
	 * nor the "ran NO test" line from #306 can see it.
	 *
	 * @param pluginZips
	 *            every zip the test needs
	 */
	static void requireBuiltPluginZips(final Path... pluginZips)
	{
		List<String> missing = new ArrayList<>();
		for (Path pluginZip : pluginZips)
		{
			if (!Files.exists(pluginZip))
			{
				missing.add(pluginZip.toString());
			}
		}
		refuse(missing.isEmpty(),
			"these plugin zips are not built: " + missing + ". Run "
				+ "'make plugins'. All of them are required, not any of them: a test that installs "
				+ "whatever happens to be there asserts whatever happens to be there");
	}

	/**
	 * Fails with the given reason, or skips with it when the switch is held down.
	 *
	 * @param satisfied
	 *            whether the prerequisite is met
	 * @param reason
	 *            what is missing and what to do about it
	 */
	private static void refuse(final boolean satisfied, final String reason)
	{
		if (satisfied)
		{
			return;
		}
		if (Boolean.getBoolean(SKIP_INSTEAD_OF_FAILING))
		{
			Assumptions.abort(reason + " [skipped rather than failed because -D"
				+ SKIP_INSTEAD_OF_FAILING + " is set]");
		}
		throw new AssertionError(
			reason + ". If this is a deliberate partial local run, -D" + SKIP_INSTEAD_OF_FAILING
				+ "=true turns it back into a skip - it is set nowhere in CI");
	}
}
