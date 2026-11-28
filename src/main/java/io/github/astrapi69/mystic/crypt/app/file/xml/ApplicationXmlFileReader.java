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
package io.github.astrapi69.mystic.crypt.app.file.xml;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Security;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Level;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.github.astrapi69.crypt.api.algorithm.AesAlgorithm;
import io.github.astrapi69.crypt.api.algorithm.Algorithm;
import io.github.astrapi69.crypt.data.key.KeyModelExtensions;
import io.github.astrapi69.crypt.data.model.CryptModel;
import io.github.astrapi69.file.create.FileFactory;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.algorithm.MysticSymmetricAlgorithm;
import io.github.astrapi69.mystic.crypt.key.PrivateKeyDecryptor;
import io.github.astrapi69.mystic.crypt.key.PrivateKeyGenericDecryptor;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.PasswordType;
import io.github.astrapi69.mystic.crypt.pw.PasswordStringDecryptor;
import io.github.astrapi69.mystic.crypt.secret.SecretBuffers;
import lombok.NonNull;
import lombok.extern.java.Log;

@Log
public class ApplicationXmlFileReader
{

	public static ApplicationModelBean read(@NonNull MasterPwFileModelBean modelObject)
	{
		PasswordType passwordType = PasswordType.resolve(modelObject.isWithMasterPw(),
			modelObject.isWithKeyFile());
		ApplicationModelBean applicationModelBean;
		if (passwordType.equals(PasswordType.PASSWORD_WITH_PRIVATE_KEY))
		{
			applicationModelBean = readApplicationFileWithPasswordAndPrivateKey(modelObject);
		}
		else if (passwordType.equals(PasswordType.PASSWORD))
		{
			applicationModelBean = readApplicationFileWithPassword(modelObject);
		}
		else
		{
			applicationModelBean = readApplicationFileWithPrivateKey(modelObject);
		}
		discardTheBackup(modelObject);
		return applicationModelBean;
	}

	/**
	 * Removes the backup a save kept, now that the file it guards was read back successfully: the
	 * read is the proof the new file is good (#847). A failed open never reaches this, so its
	 * backup stays as the way back. A backup that cannot be removed is logged and does not fail the
	 * open
	 *
	 * @param modelObject
	 *            the credentials the file was opened with
	 */
	private static void discardTheBackup(final MasterPwFileModelBean modelObject)
	{
		File applicationFile = FileFactory.newFileQuietly(modelObject.getApplicationFileInfo());
		if (applicationFile == null)
		{
			return;
		}
		try
		{
			VaultFileWriter.discardBackup(applicationFile);
		}
		catch (IOException couldNotRemove)
		{
			log.log(Level.WARNING, "the backup of " + applicationFile + " stays: "
				+ couldNotRemove.getLocalizedMessage(), couldNotRemove);
		}
	}

	/**
	 * Stamps the file that was actually opened onto the model that came out of it.
	 * <p>
	 * A database carries its own path inside its encrypted xml, which is the path it had when it
	 * was last saved. Without this, opening a file that was moved, renamed or copied leaves the
	 * application pointing at where the file used to be, and every later save goes there instead: a
	 * moved file is recreated at the old location holding the new entries, and a copy overwrites
	 * the database it was copied from. Neither says anything.
	 *
	 * @param applicationModelBean
	 *            the model read out of the file
	 * @param applicationFile
	 *            the file the user actually opened
	 * @return the same model, pointing at the file it was read from
	 * @throws VaultContentUnreadableException
	 *             when the model holds no sign-in data, which the sign-in takes over
	 */
	private static ApplicationModelBean openedFrom(final ApplicationModelBean applicationModelBean,
		final File applicationFile, final MasterPwFileModelBean credentials)
	{
		MasterPwFileModelBean signInModelBean = signInDataOf(applicationModelBean);
		if (applicationFile == null || credentials == null)
		{
			return applicationModelBean;
		}
		signInModelBean.setApplicationFileInfo(FileInfo.toFileInfo(applicationFile));
		signInModelBean.setSelectedApplicationFilePath(applicationFile.getAbsolutePath());
		// the password is not in the file and must not be: it comes from whoever just signed in,
		// and the model needs it for the next save and for the unlock dialog
		signInModelBean.setMasterPw(credentials.getMasterPw());
		return applicationModelBean;
	}

	/**
	 * The sign-in data of the model that was read. A vault holds them, and the sign-in takes them
	 * over; content that is a vault and holds none was decrypted and cannot be signed in with
	 *
	 * @param applicationModelBean
	 *            the model read out of the file
	 * @return its sign-in data
	 * @throws VaultContentUnreadableException
	 *             when it holds none
	 */
	private static MasterPwFileModelBean signInDataOf(
		final ApplicationModelBean applicationModelBean)
	{
		if (applicationModelBean == null || applicationModelBean.getMasterPwFileModelBean() == null)
		{
			throw VaultContentUnreadableException.withoutItsSignInData();
		}
		return applicationModelBean.getMasterPwFileModelBean();
	}

	public static ApplicationModelBean readApplicationFileWithPasswordAndPrivateKey(
		MasterPwFileModelBean modelObject)
	{
		File applicationFile = FileFactory.newFileQuietly(modelObject.getApplicationFileInfo());
		char[] password = modelObject.getMasterPw();
		try
		{
			File keyFile = KeyFileOnDisk.existing(modelObject.getKeyFileInfo());
			return openedFrom(getApplicationModelBean(applicationFile, password, keyFile),
				applicationFile, modelObject);
		}
		catch (Exception exception)
		{
			throw notOpened(Factors.PASSWORD_AND_KEY_FILE, exception);
		}
	}

	public static ApplicationModelBean readApplicationFileWithPrivateKey(
		MasterPwFileModelBean modelObject)
	{
		ApplicationModelBean applicationModelBean;
		File applicationFile = FileFactory.newFileQuietly(modelObject.getApplicationFileInfo());
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security
				.addProvider(MysticCryptApplicationFrame.getInstance().getBouncyCastleProvider());
		}
		try
		{
			PrivateKey privateKey;
			if (modelObject.getPrivateKeyInfo() != null)
			{
				privateKey = KeyModelExtensions.toPrivateKey(modelObject.getPrivateKeyInfo());
			}
			else
			{
				privateKey = KeyFileOnDisk.readKey(modelObject.getKeyFileInfo(), false);
			}
			applicationModelBean = openedFrom(getApplicationModelBean(applicationFile, privateKey),
				applicationFile, modelObject);
		}
		catch (Exception exception)
		{
			throw notOpened(Factors.KEY_FILE, exception);
		}
		return applicationModelBean;
	}

	public static ApplicationModelBean readApplicationFileWithPassword(
		@NonNull final MasterPwFileModelBean modelObject)
	{
		File applicationFile = FileFactory.newFileQuietly(modelObject.getApplicationFileInfo());
		char[] password = modelObject.getMasterPw();
		try
		{
			return openedFrom(getApplicationModelBean(applicationFile, password), applicationFile,
				modelObject);
		}
		catch (Exception exception)
		{
			throw notOpened(Factors.PASSWORD, exception);
		}
	}

	/**
	 * The factors a sign-in asked for, which decide the title of a refusal and the words that say a
	 * factor was wrong, the unlock prompt's own (#880, #1005)
	 */
	private enum Factors
	{
		PASSWORD_AND_KEY_FILE("both", "dialog.signin.both",
			"Authentication with Password or key file",
			"Password or key file or both are not valid",
			"The master password or the key file is wrong."), KEY_FILE("key.file",
				"dialog.signin.key.file", "Authentication with key file", "Key file is not valid",
				"This key file does not unlock this database."), PASSWORD("password",
					"dialog.signin.password", "Authentication with Password",
					"Password is not valid", "Wrong master password.");

		private final String unlockKeySuffix;
		private final String signInKeyPrefix;
		private final String defaultTitle;
		private final String defaultReason;
		private final String defaultWrong;

		Factors(final String unlockKeySuffix, final String signInKeyPrefix,
			final String defaultTitle, final String defaultReason, final String defaultWrong)
		{
			this.unlockKeySuffix = unlockKeySuffix;
			this.signInKeyPrefix = signInKeyPrefix;
			this.defaultTitle = defaultTitle;
			this.defaultReason = defaultReason;
			this.defaultWrong = defaultWrong;
		}

		String title()
		{
			return Messages.getString(signInKeyPrefix + ".title", defaultTitle);
		}

		String reason()
		{
			return Messages.getString(signInKeyPrefix + ".reason", defaultReason);
		}

		String wrong()
		{
			return Messages.getString("unlock.failed." + unlockKeySuffix, defaultWrong);
		}
	}

	/**
	 * What the sign-in shows when a database could not be opened, as {@code title::html}. A file
	 * that names a type no database holds says so and names the type, a file that decrypted but
	 * whose content cannot be read says so and names the reason (#624), and a file whose key the
	 * key file opened but whose content is in no known format says that (#650), rather than any of
	 * them being taken for a wrong password or key file. A file the format reader refuses as no
	 * database (an {@link IllegalArgumentException}, or a block size no password database has) says
	 * that, and a failed decryption says in the unlock prompt's words that the password or the key
	 * file is wrong (#1005). A decryption that authenticates nothing whose padding check refused
	 * the key, or let it through with content a wrong key could leave, is such a failed decryption,
	 * not a database that was decrypted ({@link VaultNotDecryptedException}, #1188). A database of
	 * the key file alone, opened with a password as well, says that it opens with its key file
	 * alone. The text of the exception is never shown, and the log names the failure by the types
	 * along its chain ({@link ReaderFailureTrace}). Only a failure that carries a message written
	 * for the user, such as a missing key file, keeps showing it.
	 *
	 * @param factors
	 *            what the sign-in asked for
	 * @param exception
	 *            what opening threw
	 * @return the exception to throw
	 */
	private static RuntimeException notOpened(final Factors factors, final Exception exception)
	{
		VaultNotDecryptedException notDecrypted = causeOf(exception,
			VaultNotDecryptedException.class);
		if (notDecrypted != null)
		{
			// logged as it is: it has no cause, and its message is fixed
			log.log(Level.SEVERE, notDecrypted.getMessage(), notDecrypted);
			return refusal(factors.title(), factors.wrong(), exception);
		}
		VaultOpensWithTheKeyFileAloneException keyFileAlone = causeOf(exception,
			VaultOpensWithTheKeyFileAloneException.class);
		if (keyFileAlone != null)
		{
			return databaseNotOpened("dialog.vault.key.file.alone",
				"This database opens with its key file alone, without a master password. It was "
					+ "not opened, and the file is unchanged.",
				"", keyFileAlone, exception);
		}
		VaultHoldsAForeignTypeException foreign = causeOf(exception,
			VaultHoldsAForeignTypeException.class);
		if (foreign != null)
		{
			return databaseNotOpened("dialog.vault.foreign.type",
				"This file names the type %1$s, which no database of this application holds. It "
					+ "was not opened.",
				foreign.getTypeName(), foreign, exception);
		}
		VaultContentUnreadableException unreadable = causeOf(exception,
			VaultContentUnreadableException.class);
		if (unreadable != null)
		{
			return databaseNotOpened("dialog.vault.unreadable",
				"This database was decrypted, but its content could not be read: %1$s. It was not "
					+ "opened, and the file is unchanged.",
				unreadable.getReason(), unreadable, exception);
		}
		VaultFormatUnknownException unknownFormat = causeOf(exception,
			VaultFormatUnknownException.class);
		if (unknownFormat != null)
		{
			return databaseNotOpened("dialog.vault.format.unknown",
				"The key file opened the key of this database, but its content is in no format "
					+ "this version reads (%1$s). It was not opened, and the file is unchanged.",
				unknownFormat.getReason(), unknownFormat, exception);
		}
		if (causeOf(exception, KeyFileUnavailableException.class) != null)
		{
			logByItsTypes(exception);
			return refusal(factors.title(), exception.getMessage(), exception);
		}
		if (isNoDatabase(factors, exception))
		{
			return databaseNotOpened("dialog.vault.not.a.database",
				"This file is not a database this application can open. It was not opened, and "
					+ "the file is unchanged.",
				"", ReaderFailureTrace.of(exception), exception);
		}
		logByItsTypes(exception);
		if (causeOf(exception, GeneralSecurityException.class) != null
			|| causeOf(exception, SecurityException.class) != null)
		{
			return refusal(factors.title(), factors.wrong(), exception);
		}
		return refusal(factors.title(), factors.reason(), exception);
	}

	/**
	 * Whether opening failed because the file is no database of this application: the format reader
	 * refused it ({@link IllegalArgumentException}), or, on a path that reads the key first, the
	 * envelope is not a serialized stream or ends early. A block size no password database has
	 * counts too, but only on the password path: on a key file path the same exception also comes
	 * from a foreign key smaller than the database's, which is a wrong key file
	 */
	private static boolean isNoDatabase(final Factors factors, final Exception exception)
	{
		return causeOf(exception, IllegalArgumentException.class) != null
			|| causeOf(exception, ObjectStreamException.class) != null
			|| causeOf(exception, EOFException.class) != null || factors == Factors.PASSWORD
				&& causeOf(exception, IllegalBlockSizeException.class) != null;
	}

	private static RuntimeException refusal(final String title, final String html,
		final Exception exception)
	{
		return new SignInRefusedException(title, SignInRefusal.asTheDialogShowsIt(title, html),
			exception);
	}

	/**
	 * Logs the failure by the type of each exception of its chain and the line a parser names,
	 * never by their messages
	 *
	 * @param failure
	 *            what opening threw
	 */
	private static void logByItsTypes(final Exception failure)
	{
		ReaderFailureTrace toldByItsTypes = ReaderFailureTrace.of(failure);
		log.log(Level.SEVERE, toldByItsTypes.getMessage(), toldByItsTypes);
	}

	/**
	 * A "Database not opened" failure from the bundle keys {@code <keyPrefix>.title} and
	 * {@code <keyPrefix>.message}, whose message names the given detail
	 *
	 * @param keyPrefix
	 *            the bundle key without {@code .title} or {@code .message}
	 * @param defaultMessage
	 *            the message when the bundle has none, with {@code %1$s} for the detail
	 * @param detail
	 *            what the message names, never any of the content
	 * @param cause
	 *            the failure that decides the message, logged: one of the reader's own exceptions,
	 *            whose cause is told by its types, or a failure told by its types
	 * @param exception
	 *            what opening threw
	 * @return the exception to throw
	 */
	private static RuntimeException databaseNotOpened(final String keyPrefix,
		final String defaultMessage, final String detail, final Exception cause,
		final Exception exception)
	{
		log.log(Level.SEVERE, cause.getMessage(), cause);
		String notOpenedTitle = Messages.getString(keyPrefix + ".title", "Database not opened");
		// %1$s rather than {0}: the bundle lookup formats braces itself and fills them with null
		String notOpenedReason = String
			.format(Messages.getString(keyPrefix + ".message", defaultMessage), detail);
		return new SignInRefusedException(notOpenedTitle,
			SignInRefusal.asTheDialogShowsIt(notOpenedTitle, notOpenedReason), exception);
	}

	private static <T extends Throwable> T causeOf(final Throwable failure, final Class<T> type)
	{
		for (Throwable cause = failure; cause != null; cause = cause.getCause())
		{
			if (type.isInstance(cause))
			{
				return type.cast(cause);
			}
		}
		return null;
	}

	public static ApplicationModelBean getApplicationModelBean(File applicationFile,
		char[] password) throws Exception
	{
		// PasswordVaultFormat decides by the marker in the file which of the two formats this is,
		// and reads a database written before the marker existed just as well
		char[] xml = null;
		try
		{
			xml = PasswordVaultFormat.decrypt(applicationFile, password);
			return VaultXmlCodec.toModel(xml);
		}
		catch (BadPaddingException | VaultContentUnreadableException
			| VaultHoldsAForeignTypeException failure)
		{
			// the marker is read again only here, where it decides what the failure means: the
			// format it names authenticates, the one before it does not (#1188)
			if (PasswordVaultFormat.isCurrentFormat(Files.readAllBytes(applicationFile.toPath()))
				|| !isWhatAWrongKeyLeaves(failure))
			{
				throw failure;
			}
		}
		finally
		{
			if (xml != null)
			{
				SecretBuffers.wipe(xml);
			}
		}
		// the password did not decrypt the database (#1188)
		throw new VaultNotDecryptedException();
	}

	public static ApplicationModelBean getApplicationModelBean(File applicationFile,
		char[] password, File keyFile) throws Exception
	{
		PrivateKey privateKey = KeyFileOnDisk.readKey(keyFile, true);
		byte[] encryptedBytes = ReadFileExtensions.readFileToBytearray(applicationFile);
		String passwordLayer = decryptedWithKeyFile(privateKey, encryptedBytes);
		requireAPasswordLayer(passwordLayer);
		// one unwipeable copy of the master password, and one of the xml: this path decrypts
		// through PasswordStringDecryptor, which takes a String and is a library class that is not
		// changed from this repository (#294, architecture.md)
		String passwordText = String.valueOf(password);
		// the password layer authenticates in neither of its formats (#1188): when the password
		// does not decrypt it, the database was not decrypted
		ApplicationModelBean applicationModelBean = behindThePasswordLayer(passwordLayer,
			passwordText).orElseThrow(VaultNotDecryptedException::new);
		// the key is not in the file and must not be (#350): it comes from the key file whoever
		// signed in just picked, and the model needs it for the next save and for the plugins that
		// read it. The key-only path below does the same thing for the same reason
		signInDataOf(applicationModelBean)
			.setPrivateKeyInfo(KeyModelExtensions.toKeyModel(privateKey));
		return applicationModelBean;
	}

	/**
	 * The model behind the password layer, or nothing when the password did not decrypt it: both
	 * formats refused the key, or one of them let it through and what came out is content a wrong
	 * key could leave, and the layer does not read in 8's format either
	 * ({@link #readTheWayEightWroteIt}). Each of these is the same answer: the password did not
	 * decrypt the layer
	 *
	 * @param passwordLayer
	 *            the password layer, in Base64
	 * @param passwordText
	 *            the master password
	 * @return the model, or empty when the password did not decrypt the layer
	 * @throws Exception
	 *             when the content cannot be read for another reason
	 */
	private static Optional<ApplicationModelBean> behindThePasswordLayer(final String passwordLayer,
		final String passwordText) throws Exception
	{
		String xml;
		try
		{
			xml = decryptedWithPassword(passwordLayer, passwordText);
		}
		catch (GeneralSecurityException refusedByBothFormats)
		{
			// the answer, not a failure to pass on: the password did not decrypt the layer, which
			// the caller reports and the sign-in records as such
			return Optional.empty();
		}
		try
		{
			return Optional.of(throughTheCodec(xml));
		}
		catch (VaultContentUnreadableException | VaultHoldsAForeignTypeException failure)
		{
			if (!isWhatAWrongKeyLeaves(failure))
			{
				throw failure;
			}
			return readTheWayEightWroteIt(passwordLayer, passwordText);
		}
	}

	/**
	 * Whether a failure of a decryption that authenticates nothing is what a wrong key leaves: the
	 * padding check refused the key, or let it through by chance - about once in 256 tries - and
	 * what came out is no xml at all, or a root the reader fails on before it converts anything: a
	 * name it cannot decode or resolve, or a type other than the vault (#1188). Well-formed xml
	 * whose root is the vault stays what it is on these paths as on every other, the database that
	 * was decrypted and holds what this version cannot build (#624), as a newer version's database
	 * does
	 *
	 * @param failure
	 *            what the decryption or the reader threw
	 * @return true if a wrong key leaves such a failure
	 */
	private static boolean isWhatAWrongKeyLeaves(final Exception failure)
	{
		if (failure instanceof VaultContentUnreadableException unreadable)
		{
			return unreadable.isNoXml() || unreadable.isAtTheRoot();
		}
		if (failure instanceof VaultHoldsAForeignTypeException foreign)
		{
			return foreign.isAtTheRoot();
		}
		return failure instanceof BadPaddingException;
	}

	/**
	 * Requires that what the key file opened is a password layer, which is Base64. A database of
	 * the key file alone holds the vault's xml there instead, and says so; anything else is in no
	 * format this version reads
	 *
	 * @param passwordLayer
	 *            what the key file opened
	 * @throws VaultOpensWithTheKeyFileAloneException
	 *             when it is the xml of a database of the key file alone
	 * @throws VaultFormatUnknownException
	 *             when it is neither a password layer nor xml
	 */
	private static void requireAPasswordLayer(final String passwordLayer)
	{
		try
		{
			Base64.getDecoder().decode(passwordLayer);
		}
		catch (IllegalArgumentException noBase64)
		{
			if (couldBeginAnXmlDocument(passwordLayer))
			{
				throw new VaultOpensWithTheKeyFileAloneException();
			}
			throw new VaultFormatUnknownException(noBase64);
		}
	}

	/**
	 * Whether the text could begin an xml document: after white space and a byte order mark, a
	 * {@code <}
	 */
	private static boolean couldBeginAnXmlDocument(final String text)
	{
		int index = 0;
		while (index < text.length() && " \t\r\n\uFEFF".indexOf(text.charAt(index)) >= 0)
		{
			index++;
		}
		return index < text.length() && text.charAt(index) == '<';
	}

	public static ApplicationModelBean getApplicationModelBean(File applicationFile, File keyFile)
		throws Exception
	{
		return getApplicationModelBean(applicationFile, KeyFileOnDisk.readKey(keyFile, false));
	}

	public static ApplicationModelBean getApplicationModelBean(File applicationFile,
		PrivateKey privateKey) throws Exception
	{
		byte[] encryptedBytes = ReadFileExtensions.readFileToBytearray(applicationFile);
		String xml = decryptedWithKeyFile(privateKey, encryptedBytes);
		ApplicationModelBean applicationModelBean = throughTheCodec(xml);
		signInDataOf(applicationModelBean)
			.setPrivateKeyInfo(KeyModelExtensions.toKeyModel(privateKey));
		return applicationModelBean;
	}

	/**
	 * The password layer of a database with a password and a key file, in the format this version
	 * writes or in the one 8 wrote: {@code PBEWithMD5AndDES} with the fixed salt (mystic-crypt 9),
	 * which the next save replaces (#650). Neither layer authenticates, so when both fail the
	 * failure stays the current format's, and the password is called not valid as before
	 *
	 * @param encryptedXml
	 *            the password layer, in Base64
	 * @param passwordText
	 *            the master password
	 * @return the xml
	 * @throws Exception
	 *             when the password does not decrypt it in either format
	 */
	private static String decryptedWithPassword(final String encryptedXml,
		final String passwordText) throws Exception
	{
		try
		{
			return new PasswordStringDecryptor(passwordText).decrypt(encryptedXml);
		}
		catch (GeneralSecurityException notTheCurrentFormat)
		{
			try
			{
				return PasswordVaultFormat.decryptLegacy(Base64.getDecoder().decode(encryptedXml),
					passwordText);
			}
			catch (GeneralSecurityException notTheFormatOfEight)
			{
				notTheCurrentFormat.addSuppressed(notTheFormatOfEight);
				throw notTheCurrentFormat;
			}
		}
	}

	/**
	 * The model behind the password layer read the way 8 wrote it, for a layer whose decryption
	 * gave back content a wrong key could leave. This version's format is tried first, and a layer
	 * of 8 whose length after the salt is a whole number of that format's cipher blocks passes its
	 * padding check for about one password in 256: the text it then gives back is noise, at every
	 * sign-in, and the layer is one that only 8's format reads. Only a layer that reads in 8's
	 * format changes the outcome; when it does not, the password did not decrypt the layer, and a
	 * wrong password is still a wrong password (#1188)
	 *
	 * @param encryptedXml
	 *            the password layer, in Base64
	 * @param passwordText
	 *            the master password
	 * @return the model, or empty when the layer does not read in 8's format
	 * @throws Exception
	 *             when the layer cannot be decrypted for another reason than the password
	 */
	private static Optional<ApplicationModelBean> readTheWayEightWroteIt(final String encryptedXml,
		final String passwordText) throws Exception
	{
		try
		{
			return Optional.of(throughTheCodec(PasswordVaultFormat
				.decryptLegacy(Base64.getDecoder().decode(encryptedXml), passwordText)));
		}
		catch (GeneralSecurityException | VaultContentUnreadableException
			| VaultHoldsAForeignTypeException notTheFormatOfEightEither)
		{
			// the outcome is the one the first decryption gave, which the caller reports
			return Optional.empty();
		}
	}

	/**
	 * The content behind the key that the key file opens, in the format this version writes or in
	 * the one 8 wrote. 8 encrypted the content with plain AES (mystic-crypt 9), and everything
	 * since 8.1.1 with AES-GCM, and both wrap the key the same way. A wrong key file fails at that
	 * key and stays a wrong key file; a failed GCM tag means the key was right and the content
	 * older, so the content is read the way 8 wrote it, and the next save writes the current format
	 * (#650). Content that neither reads is reported as an unknown format, never as a wrong key
	 * file.
	 *
	 * @param privateKey
	 *            the key from the key file
	 * @param encryptedBytes
	 *            the file's bytes
	 * @return the content
	 * @throws Exception
	 *             when the key file does not open the key, or the content cannot be read
	 */
	private static String decryptedWithKeyFile(final PrivateKey privateKey,
		final byte[] encryptedBytes) throws Exception
	{
		try
		{
			return decryptedWith(privateKey, MysticSymmetricAlgorithm.AES_GCM_NO_PADDING,
				encryptedBytes);
		}
		catch (AEADBadTagException notTheCurrentFormat)
		{
			try
			{
				return decryptedWith(privateKey, AesAlgorithm.AES, encryptedBytes);
			}
			catch (GeneralSecurityException | IOException notTheFormatOfEight)
			{
				notTheFormatOfEight.addSuppressed(notTheCurrentFormat);
				throw new VaultFormatUnknownException(notTheFormatOfEight);
			}
		}
	}

	private static String decryptedWith(final PrivateKey privateKey,
		final Algorithm symmetricAlgorithm, final byte[] encryptedBytes) throws Exception
	{
		CryptModel<Cipher, PrivateKey, byte[]> decryptModel = CryptModel
			.<Cipher, PrivateKey, byte[]> builder().key(privateKey).build();
		PrivateKeyGenericDecryptor<String> genericDecryptor = new PrivateKeyGenericDecryptor<>(
			new PrivateKeyDecryptor(decryptModel, symmetricAlgorithm));
		return genericDecryptor.decrypt(encryptedBytes);
	}

	/**
	 * Reads the model the way the password path does: through the codec, which skips an element it
	 * does not know instead of failing into "Password is not valid" (#402). The characters handed
	 * to it are overwritten; the String is the one copy these paths already kept (#294)
	 *
	 * @param xml
	 *            the decrypted xml
	 * @return the model
	 */
	private static ApplicationModelBean throughTheCodec(final String xml)
	{
		char[] characters = xml.toCharArray();
		try
		{
			return VaultXmlCodec.toModel(characters);
		}
		finally
		{
			SecretBuffers.wipe(characters);
		}
	}
}
