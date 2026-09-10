package io.github.astrapi69.mystic.crypt.lock;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * How many entry editors are open right now, so the lock and the close can ask (#303).
 * <p>
 * An edit typed into an entry dialog is already in the model - the dialog binds its fields onto the
 * entry that lives in the tree - but nothing sets the dirty flag, and every decision downstream
 * read that flag alone. The automatic lock therefore saved nothing and the automatic close that
 * followed saw a clean model, wiped every entry and dropped it. The user walked away mid-edit and
 * came back to a vault without the change.
 * <p>
 * The flag describes what the model has committed; an open editor holds what the user has typed.
 * Both are the user's work, and only one of them was being protected. Making every keystroke set
 * the dirty flag was the other way to fix it and is deliberately not taken: that flag has other
 * consumers, and redefining "unsaved" everywhere to repair one path is a wider change than the
 * defect.
 * <p>
 * A counter rather than a flag, because a second editor can be opened over the first, and the last
 * one closing is what ends the state. Static, because the dialog that opens has no path to the
 * application model and the frame it belongs to is a singleton; display-free, so the decision it
 * feeds stays testable and mutation-covered like the rest of this package.
 */
public final class OpenEditors
{

	private static final AtomicInteger OPEN = new AtomicInteger();

	private OpenEditors()
	{
	}

	/**
	 * Notes that an editor was opened
	 */
	public static void opened()
	{
		OPEN.incrementAndGet();
	}

	/**
	 * Notes that an editor was closed, confirmed or cancelled alike - a cancelled dialog has
	 * already written what was typed onto the entry, so it is not the moment to stop counting the
	 * work as pending; the moment is when no editor is left
	 */
	public static void closed()
	{
		OPEN.updateAndGet(open -> open > 0 ? open - 1 : 0);
	}

	/**
	 * Whether any editor is open
	 *
	 * @return true while at least one editor is open
	 */
	public static boolean anyOpen()
	{
		return OPEN.get() > 0;
	}

	/**
	 * Forgets every open editor. For a test that has to start from a known state, and for the
	 * moment a vault is closed - an editor over a vault that is gone counts for nothing
	 */
	public static void forgetAll()
	{
		OPEN.set(0);
	}
}
