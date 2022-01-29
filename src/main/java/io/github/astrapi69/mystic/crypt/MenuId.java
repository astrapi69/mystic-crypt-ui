package io.github.astrapi69.mystic.crypt;

import io.github.astrapi69.swing.base.BaseMenuId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Accessors(fluent = true)
public enum MenuId {

    /**
     * The id for verify the checksum menu
     */
    VERIFY_CHECKSUM(MenuId.VERIFY_CHECKSUM_KEY),

    /**
     * The id for open the mystic crypt database menu
     */
    OPEN_DATABASE(MenuId.OPEN_DATABASE_KEY),

    /**
     * The id for open the mystic crypt database toolbar menu
     */
    OPEN_DATABASE_TOOL_BAR(MenuId.OPEN_DATABASE_TOOL_BAR_KEY),

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
     * The id for the toggle to fullscreen menu
     */
    TOGGLE_FULLSCREEN(MenuId.TOGGLE_FULLSCREEN_KEY),

    /**
     * The id for the exit application menu
     */
    CONSOLE(MenuId.CONSOLE_KEY),

    /**
     * The id for the exit application menu
     */
    EXIT(MenuId.EXIT_KEY);

    public static final String VERIFY_CHECKSUM_KEY = "global.menu.edit.verify.checksum";
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

    public static final String OPEN_DATABASE_TOOL_BAR_KEY = "global.toolbar.menu.file.open.database";

    /** the properties key from the current menu */
    String propertiesKey;

    MenuId(final String propertiesKey){
        this.propertiesKey = propertiesKey;
    }


    public static Map<String, Boolean> getBaseMenuIdsAsMap() {
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
        menuIds.put(MenuId.TOGGLE_FULLSCREEN.propertiesKey(), true);
        menuIds.put(MenuId.CONSOLE.propertiesKey(), true);
        menuIds.put(MenuId.EXIT.propertiesKey(), true);
        return menuIds;
    }
}
