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
package io.github.astrapi69.mystic.crypt;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Accessors(fluent = true)
public enum MenuId
{

	/**
	 * The id for verify the checksum menu
	 */
	VERIFY_CHECKSUM(MenuId.VERIFY_CHECKSUM_KEY),

	/**
	 * The id for create a new mystic crypt database menu
	 */
	NEW_DATABASE(MenuId.NEW_DATABASE_KEY),

	/**
	 * The id for open the mystic crypt database menu
	 */
	OPEN_DATABASE(MenuId.OPEN_DATABASE_KEY),

	/**
	 * The id for open the mystic crypt database toolbar menu
	 */
	OPEN_DATABASE_TOOL_BAR(MenuId.OPEN_DATABASE_TOOL_BAR_KEY),

	/**
	 * The id for create a new mystic crypt database toolbar menu
	 */
	NEW_DATABASE_TOOL_BAR(MenuId.NEW_DATABASE_TOOL_BAR_KEY),

	/**
	 * The id for the secret key menu
	 */
	SECRET_KEY(MenuId.SECRET_KEY_KEY),

	/**
	 * The id for the menu to create a new secret key
	 */
	SECRET_KEY_NEW(MenuId.SECRET_KEY_NEW_KEY),

	/**
	 * The id for open a private key menu
	 */
	OPEN_PRIVATE_KEY(MenuId.OPEN_PRIVATE_KEY_KEY),

	/**
	 * The id for the obfuscation menu
	 */
	OBFUSCATION(MenuId.OBFUSCATION_KEY),

	/**
	 * The id for the simple obfuscation menu
	 */
	SIMPLE_OBFUSCATION(MenuId.SIMPLE_OBFUSCATION_KEY),

	/**
	 * The id for the operated obfuscation menu
	 */
	OPERATED_OBFUSCATION(MenuId.OPERATED_OBFUSCATION_KEY),

	/**
	 * The id for the convert dialog menu
	 */
	CONVERT(MenuId.CONVERT_KEY),

	/**
	 * The id for save the application file dialog menu
	 */
	SAVE_APPLICATION_FILE(MenuId.SAVE_APPLICATION_FILE_KEY),

	/**
	 * The id for 'save as' the application file dialog menu
	 */
	SAVE_AS_APPLICATION_FILE(MenuId.SAVE_AS_APPLICATION_FILE_KEY),

	/**
	 * The id for the exit application menu
	 */
	CONSOLE(MenuId.CONSOLE_KEY);

	public static final String NEW_DATABASE_KEY = "global.menu.file.new.database";
	public static final String SAVE_APPLICATION_FILE_KEY = "global.menu.file.save";
	public static final String SAVE_AS_APPLICATION_FILE_KEY = "global.menu.file.save.as";
	public static final String OPEN_DATABASE_KEY = "global.menu.file.open.database";
	public static final String SECRET_KEY_KEY = "global.menu.file.secret.key";
	public static final String SECRET_KEY_NEW_KEY = "global.menu.file.secret.key.new";
	public static final String OPEN_PRIVATE_KEY_KEY = "global.menu.file.secret.key.open.private.key";
	public static final String OBFUSCATION_KEY = "global.menu.file.obfuscation";
	public static final String SIMPLE_OBFUSCATION_KEY = "global.menu.file.obfuscation.simple";
	public static final String OPERATED_OBFUSCATION_KEY = "global.menu.file.obfuscation.operated";
	public static final String CONVERT_KEY = "global.menu.file.convert";
	public static final String TOGGLE_FULLSCREEN_KEY = "global.menu.file.toggle.fullscreen";
	public static final String CONSOLE_KEY = "global.menu.file.console";
	public static final String EXIT_KEY = "global.menu.file.exit";

	public static final String VERIFY_CHECKSUM_KEY = "global.menu.edit.verify.checksum";

	public static final String NEW_DATABASE_TOOL_BAR_KEY = "global.toolbar.menu.file.new.database";
	public static final String OPEN_DATABASE_TOOL_BAR_KEY = "global.toolbar.menu.file.open.database";

	/** the properties key from the current menu */
	String propertiesKey;

	MenuId(final String propertiesKey)
	{
		this.propertiesKey = propertiesKey;
	}


	public static Map<String, Boolean> getBaseMenuIdsAsMap()
	{
		Map<String, Boolean> menuIds = new LinkedHashMap<>();
		menuIds.put(MenuId.VERIFY_CHECKSUM.propertiesKey(), true);
		menuIds.put(MenuId.OPEN_DATABASE.propertiesKey(), true);
		menuIds.put(MenuId.OPEN_DATABASE_TOOL_BAR.propertiesKey(), true);
		menuIds.put(MenuId.SECRET_KEY.propertiesKey(), true);
		menuIds.put(MenuId.SECRET_KEY_NEW.propertiesKey(), true);
		menuIds.put(MenuId.OPEN_PRIVATE_KEY.propertiesKey(), true);
		menuIds.put(MenuId.OBFUSCATION.propertiesKey(), true);
		menuIds.put(MenuId.SIMPLE_OBFUSCATION.propertiesKey(), true);
		menuIds.put(MenuId.OPERATED_OBFUSCATION.propertiesKey(), true);
		menuIds.put(MenuId.CONVERT.propertiesKey(), true);
		menuIds.put(MenuId.CONSOLE.propertiesKey(), true);
		return menuIds;
	}
}
