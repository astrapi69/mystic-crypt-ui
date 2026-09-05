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
package io.github.astrapi69.mystic.crypt.keepass;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Icon;

import io.github.astrapi69.icon.ImageIconFactory;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

/**
 * The icon set a KeePass database refers to by index.
 * <p>
 * An imported entry carries its index in
 * {@link io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean#getKeePassIconIndex()},
 * an imported group under {@link KeePassTreeConverter#KEEPASS_ICON_INDEX_PROPERTY}. This turns such
 * an index back into the icon the user picked in KeePass.
 * <p>
 * The images under {@code img/keepass} are third party artwork shipped unmodified and are not
 * covered by this application's license - see {@code legal/keepass-icons-license.txt} for their
 * provenance (the Nuvola icon theme under LGPL-2.1, plus a handful of CC0 and public domain icons).
 */
@Log
@UtilityClass
public class KeePassIcons
{

	/** The highest icon index a KeePass database uses; the set runs from 0 to this value */
	public static final int HIGHEST_INDEX = 68;

	/** The classpath folder the icons are shipped in */
	private static final String ICON_FOLDER = "img/keepass";

	/**
	 * The file name per index. The KeePass index IS the position in this list, so a new entry may
	 * only ever be appended - the names carry the original artwork's names, which is what makes the
	 * mapping auditable against the license file
	 */
	private static final String[] FILE_NAMES = { "C00_Password.png", "C01_Package_Network.png",
			"C02_MessageBox_Warning.png", "C03_Server.png", "C04_Klipper.png",
			"C05_Edu_Languages.png", "C06_KCMDF.png", "C07_Kate.png", "C08_Socket.png",
			"C09_Identity.png", "C10_Kontact.png", "C11_Camera.png", "C12_IRKickFlash.png",
			"C13_KGPG_Key3.png", "C14_Laptop_Power.png", "C15_Scanner.png",
			"C16_Mozilla_Firebird.png", "C17_CDROM_Unmount.png", "C18_Display.png",
			"C19_Mail_Generic.png", "C20_Misc.png", "C21_KOrganizer.png", "C22_ASCII.png",
			"C23_Icons.png", "C24_Connect_Established.png", "C25_Folder_Mail.png",
			"C26_FileSave.png", "C27_NFS_Unmount.png", "C28_QuickTime.png", "C29_KGPG_Term.png",
			"C30_Konsole.png", "C31_FilePrint.png", "C32_FSView.png", "C33_Run.png",
			"C34_Configure.png", "C35_KRFB.png", "C36_Ark.png", "C37_KPercentage.png",
			"C38_Samba_Unmount.png", "C39_History.png", "C40_Mail_Find.png", "C41_VectorGfx.png",
			"C42_KCMMemory.png", "C43_EditTrash.png", "C44_KNotes.png", "C45_Cancel.png",
			"C46_Help.png", "C47_KPackage.png", "C48_Folder.png", "C49_Folder_Blue_Open.png",
			"C50_Folder_Tar.png", "C51_Decrypted.png", "C52_Encrypted.png", "C53_Apply.png",
			"C54_Signature.png", "C55_Thumbnail.png", "C56_KAddressBook.png", "C57_View_Text.png",
			"C58_KGPG.png", "C59_Package_Development.png", "C60_KFM_Home.png", "C61_Services.png",
			"C62_Tux.png", "C63_Feather.png", "C64_Apple.png", "C65_W.png", "C66_Money.png",
			"C67_Certificate.png", "C68_BlackBerry.png" };

	/** Read once per index: a tree cell renderer asks for the same icon on every repaint */
	private static final Map<Integer, Icon> LOADED = new ConcurrentHashMap<>();

	/**
	 * The classpath path of the icon with the given KeePass index
	 *
	 * @param index
	 *            the KeePass icon index, may be {@code null}
	 * @return the path, or {@code null} if there is no icon for that index
	 */
	public static String pathOf(final Integer index)
	{
		if (index == null || index < 0 || HIGHEST_INDEX < index)
		{
			return null;
		}
		return ICON_FOLDER + "/" + FILE_NAMES[index];
	}

	/**
	 * The icon with the given KeePass index
	 *
	 * @param index
	 *            the KeePass icon index, may be {@code null}
	 * @return the icon, or {@code null} if there is no icon for that index or it cannot be read
	 */
	public static Icon of(final Integer index)
	{
		String path = pathOf(index);
		if (path == null)
		{
			return null;
		}
		return LOADED.computeIfAbsent(index, missing -> load(path));
	}

	private static Icon load(final String path)
	{
		try
		{
			return ImageIconFactory.newImageIcon(path);
		}
		catch (final RuntimeException iconCannotBeRead)
		{
			// a missing or unreadable icon costs the row its picture, never the entry itself
			log.warning("the KeePass icon " + path + " could not be read: " + iconCannotBeRead);
			return null;
		}
	}
}
