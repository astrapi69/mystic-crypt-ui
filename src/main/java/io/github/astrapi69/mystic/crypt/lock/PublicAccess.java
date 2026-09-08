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
import java.util.Set;

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

		// "Open Database" is NOT here, although it was going to be: the entry behind that label
		// re-shows a vault that is already open (OpenDatabaseTreeFrameAction), it does not open a
		// file. Clicked without one, it throws - measured: "NullPointerException: Cannot read
		// field 'parent' because 'comp' is null", because the panel it would show does not exist.
		// Listing it would have made a crashing entry public. There is therefore no menu path into
		// a vault at all, and #266 is about building one, not about enabling this
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

	private PublicAccess()
	{
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
