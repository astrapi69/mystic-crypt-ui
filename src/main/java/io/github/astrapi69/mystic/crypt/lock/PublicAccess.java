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
package io.github.astrapi69.mystic.crypt.lock;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import io.github.astrapi69.mystic.crypt.MenuId;
import io.github.astrapi69.mystic.crypt.settings.FlatLafTheme;
import io.github.astrapi69.swing.menu.enumeration.BaseMenuId;

/**
 * What may be offered while no vault is open.
 * <p>
 * The rule, and it is the whole point of this class: <b>every place that offers a privileged
 * capability asks this predicate</b>. Menu entries ask it through the list below, because menu
 * entries are an enumerable surface. Everything else - a tab in a dialog, a button, a drop target -
 * asks {@link #isOffered(boolean, boolean)} directly. A list of widgets could never be finished,
 * and the next dialog would tear a hole in it again (#232).
 * <p>
 * WHAT A LIST CANNOT SAY: a set of identifiers answers "may this be offered" with one fixed answer
 * per name. An entry whose admissibility depends on the STATE needs the predicate instead - the
 * "new database" toolbar button is one, allowed with nothing open and refused while a vault is
 * locked (#270), the lock entry another, the way back to the unlock prompt while a vault is locked
 * and nothing to do without one (#889). Such entries are deliberately absent from the sets below;
 * {@link #isOfferedWithoutSignIn(String, boolean, boolean)} decides them from the state, and the
 * inventory test knows them as a category of their own.
 * <p>
 * What this replaces was a blacklist: everything was offered without a vault except a handful of
 * named entries. Nobody decided that; the list grew. It let both KeePass entries through, which
 * answer a click with an error dialog, "Lock workspace", which silently does nothing, three of four
 * FlatLaf themes while the seven JDK themes stayed private, and every plugin entry ever added.
 * Private until named is the opposite default, and it is the one a password manager should have.
 */
public final class PublicAccess
{

	/**
	 * The menu entries offered without a vault. Everything not named here is private, including
	 * every entry added later.
	 * <p>
	 * Plugin entries are deliberately absent: a plugin declares for itself that its tool works
	 * without a vault, and until it does, it is private like everything else.
	 */
	static final Set<String> PUBLIC_MENU_IDS = Set.of(
		// the containers that have to be reachable for anything below them to be
		BaseMenuId.FILE.propertiesKey(), BaseMenuId.HELP.propertiesKey(),
		MenuId.VIEW.propertiesKey(), MenuId.PLUGINS.propertiesKey(),

		// the way back INTO a vault, which is what #266 was about: pick a database file and sign
		// in. Without it, cancelling the sign-in left Exit and a restart as the only moves.
		//
		// "Open Database" (MenuId.OPEN_DATABASE, now labelled "Show Database View") is a different
		// entry and stays private: it re-shows a vault that is already open, so it has nothing to
		// do without one. It used to throw when fired in this state and now returns instead
		// (#285), which makes it harmless rather than public - an entry that reliably does nothing
		// is not worth offering.
		//
		// "Close Database" stays private for the same shape of reason: with nothing open there is
		// nothing to close
		MenuId.OPEN_DATABASE_FILE.propertiesKey(),

		// leaving, and the settings that are not about a vault. The Plugins TAB inside that
		// dialog is NOT covered by this list and asks the predicate itself: installing and
		// enabling a plugin loads code into the running process
		BaseMenuId.EXIT.propertiesKey(), MenuId.SETTINGS.propertiesKey(),

		// a theme touches no vault. FlatLaf Light stays greyed out while it is the active theme -
		// that is newFlatLafMenuItem's own isEnabled(), a different mechanism, and this list must
		// not work against it
		BaseMenuId.LOOK_AND_FEEL.propertiesKey(), BaseMenuId.LOOK_AND_FEEL_GTK.propertiesKey(),
		BaseMenuId.LOOK_AND_FEEL_METAL.propertiesKey(),
		BaseMenuId.LOOK_AND_FEEL_OCEAN.propertiesKey(),
		BaseMenuId.LOOK_AND_FEEL_MOTIF.propertiesKey(),
		BaseMenuId.LOOK_AND_FEEL_NIMBUS.propertiesKey(),
		BaseMenuId.LOOK_AND_FEEL_SYSTEM.propertiesKey(),

		// about, licence and the donate link: text and a browser, no vault
		BaseMenuId.HELP_DONATE.propertiesKey(), BaseMenuId.HELP_LICENSE.propertiesKey(),
		BaseMenuId.HELP_INFO.propertiesKey());

	/**
	 * The toolbar items offered without a vault: none, today.
	 * <p>
	 * "New database" was going to be here - it works, and creating a vault is a way into one. It is
	 * not, because of what it does in the state that shares this list. Public and LOCKED are the
	 * same state for this decision (#237), and creating a new vault while another one is locked was
	 * measured to do this:
	 *
	 * <pre>
	 * signedIn                  : true
	 * model points at           : vault-b.mcrdb
	 * database view on screen   : true
	 * vault A's node in the tree: YES
	 * master password in memory : vault B's
	 * </pre>
	 *
	 * The screen unlocks and shows the LOCKED vault's entries, without its master password ever
	 * being typed. That is the #237 defect through another door, and it is older than this list -
	 * the blacklist this replaces never disabled that button either. Listing it here would bless
	 * it; leaving it out closes it (#270).
	 * <p>
	 * Save and the search field act on a vault that is not open, so they were never candidates.
	 */
	static final Set<String> PUBLIC_TOOLBAR_IDS = Set.of();

	/**
	 * Whether the toolbar item with the given component name is one of the public ones
	 *
	 * @param toolbarItemName
	 *            the component name of the toolbar item
	 * @return true if it is offered without a vault
	 */
	public static boolean isPublicToolbarId(final String toolbarItemName)
	{
		return toolbarItemName != null && PUBLIC_TOOLBAR_IDS.contains(toolbarItemName);
	}

	/**
	 * The entries whose admissibility without a signed-in vault depends on whether a vault is open
	 * at all, each with that decision. They are deliberately in neither list above: one name, two
	 * answers, which a list cannot express.
	 * <ul>
	 * <li>the "new database" toolbar button: with nothing open the way to a first vault, with a
	 * vault locked refused (#270)</li>
	 * <li>the lock entry: with a vault locked the way back to the unlock prompt, with nothing open
	 * nothing to do (#889)</li>
	 * </ul>
	 */
	private static final Map<String, Predicate<Boolean>> STATE_DEPENDENT_IDS = Map.of(
		MenuId.NEW_DATABASE_TOOL_BAR.propertiesKey(), WorkspaceLockDecision::mayCreateAVault,
		MenuId.LOCK_WORKSPACE.propertiesKey(), WorkspaceLockDecision::mayOfferTheUnlock);

	private PublicAccess()
	{
	}

	/**
	 * Whether a menu entry or a toolbar item is offered while nobody is signed in: the entries
	 * whose answer depends on whether a vault is open ask that, every other one is offered only
	 * when it is declared public. The menu and the toolbar both ask here, so a state-dependent
	 * entry is decided in one place for both surfaces
	 *
	 * @param name
	 *            the component name of the entry or item
	 * @param aVaultIsOpen
	 *            whether a vault is open at all, locked or not
	 * @param declaredPublic
	 *            whether the entry is on a public list or belongs to a plugin that declared itself
	 *            usable without a vault
	 * @return true if it may be offered
	 */
	public static boolean isOfferedWithoutSignIn(final String name, final boolean aVaultIsOpen,
		final boolean declaredPublic)
	{
		Predicate<Boolean> stateDependent = name == null ? null : STATE_DEPENDENT_IDS.get(name);
		if (stateDependent != null)
		{
			return stateDependent.test(aVaultIsOpen);
		}
		return isOffered(false, declaredPublic);
	}

	/**
	 * The names of the entries whose admissibility depends on the state, for the inventory that
	 * checks what the menu offers: they are a category of their own there
	 *
	 * @return the component names
	 */
	public static Set<String> stateDependentIds()
	{
		return STATE_DEPENDENT_IDS.keySet();
	}

	/**
	 * Whether a capability may be offered.
	 * <p>
	 * Signed in, everything the application has is offered. Without a vault, only what declares
	 * itself public is - which is what makes "private until named" the default for anything added
	 * later.
	 *
	 * @param signedIn
	 *            whether a vault is open
	 * @param declaredPublic
	 *            whether this capability declares that it works without one
	 * @return true if it may be offered
	 */
	public static boolean isOffered(final boolean signedIn, final boolean declaredPublic)
	{
		return signedIn || declaredPublic;
	}

	/**
	 * Whether the menu entry with the given component name is one of the public ones
	 *
	 * @param menuItemName
	 *            the component name of the menu entry
	 * @return true if it is offered without a vault
	 */
	public static boolean isPublicMenuId(final String menuItemName)
	{
		return menuItemName != null && publicMenuIds().contains(menuItemName);
	}

	/**
	 * The public menu entries, including the FlatLaf theme items. Their names are generated
	 * ({@link FlatLafTheme#menuItemName()}), so they are derived from the same source that builds
	 * them rather than repeated here - a copy would rot the next time a theme is added
	 *
	 * @return every menu entry name that is offered without a vault
	 */
	public static Set<String> publicMenuIds()
	{
		Set<String> publicIds = new LinkedHashSet<>(PUBLIC_MENU_IDS);
		Arrays.stream(FlatLafTheme.values()).map(FlatLafTheme::menuItemName)
			.forEach(publicIds::add);
		return publicIds;
	}
}
