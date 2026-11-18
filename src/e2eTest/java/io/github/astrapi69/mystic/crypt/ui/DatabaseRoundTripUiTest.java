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
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.EntryText;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * The data-loss gate of the manual test plan (TS-0001, release smoke) driven through the running
 * application: build a database by hand, save it, end the application through its own end path,
 * start it again, sign in, and compare everything with what was typed (TC-0011, TC-0013, TC-0014),
 * then look at an attachment and write it out (TC-0015).
 * <p>
 * The restart is the in-JVM one of {@link AbstractUiTest#shutdownApplication()}: a real process end
 * kills the test JVM, and a successor cannot be observed from inside it. That part stays manual.
 */
class DatabaseRoundTripUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String TEXT_ATTACHMENT = "gruesse.txt";

	private static final String BINARY_ATTACHMENT = "all-bytes.bin";

	private static final String TEXT_OF_THE_ATTACHMENT = "Grüße aus Köln, ß und €\n";

	@Test
	@DisplayName("a tree of folders and entries with every field, a custom field and attachments comes back after save, the application's own end and a restart")
	void aFullTreeComesBackAfterSaveEndAndRestart() throws Exception
	{
		File databaseFile = new File(tempHome, "round-trip.mcrdb");
		File text = writeTheTextAttachment();
		File binary = writeTheBinaryAttachment();
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		String textHash = sha256Of(Files.readAllBytes(text.toPath()));
		String binaryHash = sha256Of(Files.readAllBytes(binary.toPath()));
		List<String> folders = List.of("Bank", "Mail", "Shop");
		for (String folder : folders)
		{
			application.addNodeToTreeRoot(frame, folder);
		}
		Map<String, List<EntryRecord>> typed = new TreeMap<>();
		for (String folder : folders)
		{
			application.selectTreeRowByName(frame, folder);
			List<EntryRecord> entries = new ArrayList<>();
			for (int number = 1; number <= 2; number++)
			{
				EntryRecord entry = new EntryRecord(folder + " entry " + number,
					"user-" + folder.toLowerCase() + "-" + number, TestPasswords.throwaway(),
					"https://" + folder.toLowerCase() + ".example/login?a=" + number + "&b=ä",
					"Notiz " + number + " für " + folder + "\nzweite Zeile: Grüße, ß, €", Map.of(),
					Map.of());
				application.addEntry(frame, entry.title(), entry.userName(), entry.password(),
					entry.url(), entry.notes());
				entries.add(entry);
			}
			typed.put(folder, entries);
		}
		application.selectTreeRowByName(frame, "Bank");
		application.selectEntryRowByTitle(frame, "Bank entry 1");
		application.openTheEditDialog(frame).addProperty("pin", "4711")
			.addProperty("seed", "JBSWY3DPEHPK3PXP").addAttachment(text).addAttachment(binary).ok();
		EntryRecord first = typed.get("Bank").get(0);
		typed.get("Bank").set(0,
			new EntryRecord(first.title(), first.userName(), first.password(), first.url(),
				first.notes(), Map.of("pin", "4711", "seed", "JBSWY3DPEHPK3PXP"),
				Map.of(TEXT_ATTACHMENT, textHash, BINARY_ATTACHMENT, binaryHash)));
		typed.put(rootName(), List.of());
		typed.put("mykeys", List.of());

		assertEquals(typed, theTree(),
			"precondition: the open database holds exactly what was typed in the dialogs");

		application.saveDatabase();
		endTheApplicationThroughItsOwnEndPath();
		shutdownApplication();
		signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		assertEquals(typed, theTree(),
			"folders, entries in them, every field, the custom fields and the attachments (name "
				+ "and SHA-256) are what was typed, not what the model held before the save");
	}

	@Test
	@DisplayName("an attachment shown with a double click and written out with Save to is byte for byte the file that was attached, after a save and a restart")
	void theAttachmentIsShownAndWrittenOutByteForByte() throws Exception
	{
		File databaseFile = new File(tempHome, "attachment-out.mcrdb");
		File text = writeTheTextAttachment();
		File binary = writeTheBinaryAttachment();
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.addNodeToTreeRoot(frame, "Docs").selectTreeRowByName(frame, "Docs")
			.addEntry(frame, "Papers", "someone", TestPasswords.throwaway());
		application.selectEntryRowByTitle(frame, "Papers");
		application.openTheEditDialog(frame).addAttachment(text).addAttachment(binary).ok();
		application.saveDatabase();
		endTheApplicationThroughItsOwnEndPath();
		shutdownApplication();
		application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		frame = application.showMainFrame();

		application.selectTreeRowByName(frame, "Docs").selectEntryRowByTitle(frame, "Papers");
		EditCryptEntrySteps dialog = application.openTheEditDialog(frame);
		String shown = dialog.showContentOf(TEXT_ATTACHMENT);
		File writtenText = new File(tempHome, "written-" + TEXT_ATTACHMENT);
		File writtenBinary = new File(tempHome, "written-" + BINARY_ATTACHMENT);
		String proposedForText = dialog.saveAttachmentTo(TEXT_ATTACHMENT, writtenText);
		String proposedForBinary = dialog.saveAttachmentTo(BINARY_ATTACHMENT, writtenBinary);
		dialog.cancel();

		assertEquals(TEXT_OF_THE_ATTACHMENT, shown,
			"the double click shows the text that was attached");
		assertEquals(TEXT_ATTACHMENT, proposedForText, "Save to proposes the attachment's name");
		assertEquals(BINARY_ATTACHMENT, proposedForBinary,
			"Save to proposes the attachment's name");
		assertArrayEquals(Files.readAllBytes(text.toPath()),
			Files.readAllBytes(writtenText.toPath()),
			"the written text file is byte for byte the attached one, SHA-256 of the source "
				+ sha256Of(Files.readAllBytes(text.toPath())));
		assertArrayEquals(Files.readAllBytes(binary.toPath()),
			Files.readAllBytes(writtenBinary.toPath()),
			"the written binary file is byte for byte the attached one, SHA-256 of the source "
				+ sha256Of(Files.readAllBytes(binary.toPath())));
	}

	private File writeTheTextAttachment() throws IOException
	{
		File text = new File(tempHome, TEXT_ATTACHMENT);
		Files.writeString(text.toPath(), TEXT_OF_THE_ATTACHMENT, StandardCharsets.UTF_8);
		return text;
	}

	private File writeTheBinaryAttachment() throws IOException
	{
		File binary = new File(tempHome, BINARY_ATTACHMENT);
		byte[] everyByte = new byte[256];
		for (int value = 0; value < everyByte.length; value++)
		{
			everyByte[value] = (byte)value;
		}
		Files.write(binary.toPath(), everyByte);
		return binary;
	}

	/**
	 * Ends the application the way its end path does it, with the exit injected: a real
	 * {@code System.exit} would end the test JVM with it
	 */
	private static void endTheApplicationThroughItsOwnEndPath()
	{
		AtomicBoolean exitReached = new AtomicBoolean(false);
		boolean ended = GuiActionRunner.execute(() -> MysticCryptApplicationFrame.getInstance()
			.endTheApplication(() -> exitReached.set(true)));
		assertTrue(ended && exitReached.get(), "a saved vault ends without a question");
	}

	/**
	 * Every folder of the open database with its entries, read on the event thread from the tree a
	 * save reads from. Entries are sorted by title so that the order is no part of the comparison
	 */
	private static Map<String, List<EntryRecord>> theTree()
	{
		return GuiActionRunner.execute(() -> {
			BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = MysticCryptApplicationFrame
				.getInstance().getApplicationPanel().getSecretKeyTreeWithContentPanel()
				.getModelObject();
			Map<String, List<EntryRecord>> folders = new TreeMap<>();
			root.traverse().stream().filter(node -> node.getValue() != null).forEach(node -> {
				List<MysticCryptEntryModelBean> content = node.getValue().getDefaultContent();
				List<EntryRecord> records = new ArrayList<>();
				if (content != null)
				{
					content.forEach(entry -> records.add(EntryRecord.of(entry)));
				}
				records.sort((first, second) -> first.title().compareTo(second.title()));
				folders.put(node.getValue().getName(), records);
			});
			return folders;
		});
	}

	/** The name of the tree's root, which holds no entry */
	private static String rootName()
	{
		return GuiActionRunner
			.execute(() -> MysticCryptApplicationFrame.getInstance().getApplicationPanel()
				.getSecretKeyTreeWithContentPanel().getModelObject().getValue().getName());
	}

	private static String sha256Of(byte[] bytes) throws NoSuchAlgorithmException
	{
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
	}

	/** Everything the dialogs can put on an entry, as text */
	private record EntryRecord(String title, String userName, String password, String url,
		String notes, Map<String, String> properties, Map<String, String> attachments) {
		static EntryRecord of(final MysticCryptEntryModelBean entry)
		{
			List<KeyValuePair<String, String>> properties = entry.getProperties() == null
				? List.of()
				: entry.getProperties();
			List<FileContentInfo> resources = entry.getResources() == null
				? List.of()
				: entry.getResources();
			Map<String, String> attachments = new TreeMap<>();
			for (FileContentInfo resource : resources)
			{
				try
				{
					attachments.put(resource.getName(), sha256Of(resource.getContent()));
				}
				catch (NoSuchAlgorithmException missing)
				{
					throw new IllegalStateException("this Java runtime has no SHA-256", missing);
				}
			}
			return new EntryRecord(EntryText.asText(entry.getTitle()),
				EntryText.asText(entry.getUserName()), EntryText.asText(entry.getPassword()),
				EntryText.asText(entry.getUrl()), EntryText.asText(entry.getNotes()),
				properties.stream().collect(
					Collectors.toMap(KeyValuePair::getKey, KeyValuePair::getValue)),
				attachments);
		}
	}
}
