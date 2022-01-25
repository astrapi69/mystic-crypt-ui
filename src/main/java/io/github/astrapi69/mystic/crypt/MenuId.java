package io.github.astrapi69.mystic.crypt;

public enum MenuId {
    EDIT(MenuId.EDIT_ID),
    VERIFY_CHECKSUM(MenuId.VERIFY_CHECKSUM_ID);
    public static final String EDIT_ID = "EDIT";
    public static final String VERIFY_CHECKSUM_ID = "VERIFY_CHECKSUM";
    String id;
    MenuId(String id){
        this.id = id;
    }
}
