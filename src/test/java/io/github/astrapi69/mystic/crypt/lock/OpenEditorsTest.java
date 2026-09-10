package io.github.astrapi69.mystic.crypt.lock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The counter the lock and the close ask about work in flight (#303).
 */
class OpenEditorsTest
{

	@BeforeEach
	void startFromAKnownState()
	{
		OpenEditors.forgetAll();
	}

	@Test
	@DisplayName("nothing open is the starting state")
	void nothingIsOpenToBeginWith()
	{
		assertFalse(OpenEditors.anyOpen());
	}

	@Test
	@DisplayName("an open editor is work in flight until it closes")
	void oneEditorOpensAndCloses()
	{
		OpenEditors.opened();
		assertTrue(OpenEditors.anyOpen(), "an open editor holds what the user typed");

		OpenEditors.closed();
		assertFalse(OpenEditors.anyOpen());
	}

	@Test
	@DisplayName("the last editor closing is what ends the state, not the first")
	void aSecondEditorOverTheFirstKeepsTheStateUntilBothAreClosed()
	{
		OpenEditors.opened();
		OpenEditors.opened();

		OpenEditors.closed();
		assertTrue(OpenEditors.anyOpen(),
			"one of two closed leaves work in flight - a counter, not a flag, is why");

		OpenEditors.closed();
		assertFalse(OpenEditors.anyOpen());
	}

	@Test
	@DisplayName("closing more often than opening does not go negative")
	void anUnbalancedCloseCannotDriveTheCounterBelowZero()
	{
		OpenEditors.closed();
		OpenEditors.closed();

		assertFalse(OpenEditors.anyOpen());

		OpenEditors.opened();
		assertTrue(OpenEditors.anyOpen(),
			"a negative counter would swallow the next real editor, which is the failure this "
				+ "guards against");
	}

	@Test
	@DisplayName("forgetting all of them clears the state")
	void forgetAllClearsEverything()
	{
		OpenEditors.opened();
		OpenEditors.opened();

		OpenEditors.forgetAll();

		assertFalse(OpenEditors.anyOpen(),
			"an editor over a vault that is gone counts for nothing");
	}
}
