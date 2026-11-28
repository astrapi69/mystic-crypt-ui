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
package io.github.astrapi69.mystic.crypt.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.linguafranca.pwdb.format.KdbxCredentials;
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase;
import org.linguafranca.pwdb.kdbx.jackson.KdbxEntry;
import org.linguafranca.pwdb.kdbx.jackson.KdbxGroup;
import org.linguafranca.pwdb.kdbx.jackson.KdbxHistory;
import org.linguafranca.pwdb.kdbx.jackson.model.Times;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.gen.tree.convert.BaseTreeNodeTransformer;
import io.github.astrapi69.id.generate.LongIdGenerator;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.keepass.KeePassEntryConverter;
import io.github.astrapi69.mystic.crypt.keepass.KeePassLibraryFields;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * Pins today's behaviour before #1119 changes it: importing a KeePass file whose entries the vault
 * already holds adds them again, so the vault holds each UUID twice. The commit that merges by UUID
 * turns this assertion.
 * <p>
 * Every file is written at runtime with a password made up per run and read back through the
 * library before it is imported, the way the import action reads it. The model side of the import,
 * {@link ImportKeePassDatabaseAction#importUnder}, is driven with the conversion the import uses.
 */
class KeePassImportMergeTest
{

	private static final String VAULT_ROOT = "Vault root";
	private static final String KEEPASS_ROOT = "KeePass root";
	private static final String WORK = "Work";
	private static final String PRIVATE = "Private";

	private static final Instant T1 = Instant.parse("2026-01-01T10:00:00Z");
	private static final Instant T2 = Instant.parse("2026-02-01T10:00:00Z");
	private static final Instant T3 = Instant.parse("2026-03-01T10:00:00Z");

	private static final UUID MAIL = UUID.fromString("00000000-0000-4000-8000-00000000000a");
	private static final UUID BANK = UUID.fromString("00000000-0000-4000-8000-00000000000b");
	private static final UUID SHOP = UUID.fromString("00000000-0000-4000-8000-00000000000c");

	private final String filePassword = TestPasswords.throwaway();

	@TempDir
	Path directory;

	@Test
	@DisplayName("today a second import of the same file adds every entry again under its own UUID")
	void aSecondImportOfTheSameFile_addsEveryEntryAgain() throws Exception
	{
		Vault vault = new Vault();
		File file = file(Map.of(WORK, List.of(mail("mail", T1), bank("bank", T1)), PRIVATE,
			List.of(shop("shop", T1))));

		vault.importFile(file);
		vault.importFile(file);

		assertEquals(6, vault.entries().size(), "pinned: every entry is in the vault twice");
		assertEquals(List.of(2L, 2L, 2L),
			List.of(vault.countOf(MAIL), vault.countOf(BANK), vault.countOf(SHOP)));
	}

	private static String title(final MysticCryptEntryModelBean entry)
	{
		return new String(entry.getTitle());
	}

	private static List<String> titlesOf(final List<MysticCryptEntryModelBean> versions)
	{
		assertTrue(versions != null, "the entry has no history");
		return versions.stream().map(KeePassImportMergeTest::title).toList();
	}

	private static MysticCryptEntryModelBean entryBean(final UUID id, final String title,
		final Instant modified)
	{
		return MysticCryptEntryModelBean.builder().id(id).title(title.toCharArray())
			.password("secret".toCharArray())
			.lastModificationTime(
				modified == null ? null : OffsetDateTime.ofInstant(modified, ZoneOffset.UTC))
			.build();
	}

	// ---- the files ----

	private record Spec(UUID id, String title, Instant modified, Spec previous) {
	}

	private static Spec mail(final String title, final Instant modified)
	{
		return new Spec(MAIL, title, modified, null);
	}

	private static Spec mail(final String title, final Instant modified, final Spec previous)
	{
		return new Spec(MAIL, title, modified, previous);
	}

	private static Spec bank(final String title, final Instant modified)
	{
		return new Spec(BANK, title, modified, null);
	}

	private static Spec shop(final String title, final Instant modified)
	{
		return new Spec(SHOP, title, modified, null);
	}

	/**
	 * Writes a KeePass file with a root group and one group per key of the given map, each with the
	 * entries of its list, and returns the file; its password is made up per run
	 */
	private File file(final Map<String, List<Spec>> groups) throws Exception
	{
		KdbxDatabase database = new KdbxDatabase();
		KdbxGroup root = database.getRootGroup();
		root.setName(KEEPASS_ROOT);
		for (Map.Entry<String, List<Spec>> group : new LinkedHashMap<>(groups).entrySet())
		{
			KdbxGroup keePassGroup = (KdbxGroup)database.newGroup(group.getKey());
			for (Spec spec : group.getValue())
			{
				keePassGroup.addEntry(entryOf(database, spec));
			}
			root.addGroup(keePassGroup);
		}
		File file = directory.resolve(UUID.randomUUID() + ".kdbx").toFile();
		try (OutputStream stream = new FileOutputStream(file))
		{
			database.write(credentials(), stream);
		}
		return file;
	}

	private static KdbxEntry entryOf(final KdbxDatabase database, final Spec spec)
	{
		KdbxEntry entry = (KdbxEntry)database.newEntry(spec.title());
		entry.setPassword("secret");
		if (spec.previous() != null)
		{
			KdbxHistory history = new KdbxHistory();
			history.setEntry(new ArrayList<>(List.of(entryOf(database, spec.previous()))));
			KeePassLibraryFields.setHistory(entry, history);
		}
		KeePassLibraryFields.setUuid(entry, spec.id());
		Times times = KeePassLibraryFields.getTimes(entry);
		times.setCreationTime(T1);
		times.setLastModificationTime(spec.modified());
		KeePassLibraryFields.setTimes(entry, times);
		return entry;
	}

	private KdbxCredentials credentials()
	{
		return new KdbxCredentials(filePassword.getBytes(StandardCharsets.UTF_8));
	}

	private KdbxGroup read(final File file) throws Exception
	{
		try (InputStream stream = new FileInputStream(file))
		{
			return KdbxDatabase.read(credentials(), stream).getRootGroup();
		}
	}

	// ---- the vault ----

	/**
	 * An open vault as the import sees it: a live root with one group, and the model whose tree map
	 * was taken from it
	 */
	private final class Vault
	{
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root;
		final ApplicationModelBean model;

		Vault()
		{
			root = node(0L, VAULT_ROOT);
			root.addChild(node(1L, "Existing group"));
			model = new ApplicationModelBean();
			model.setRootTreeAsMap(BaseTreeNodeTransformer.toKeyMap(root));
			model.setLastId(1L);
		}

		/** A vault read back from a saved model, its live root rebuilt from the tree map */
		Vault(final ApplicationModelBean reopened)
		{
			model = reopened;
			root = BaseTreeNodeTransformer.getRoot(reopened.getRootTreeAsMap());
		}

		void importFile(final File file) throws Exception
		{
			ImportKeePassDatabaseAction.importUnder(root, read(file), model,
				LongIdGenerator.of(model.getLastId() + 1), KeePassEntryConverter::toEntryModelBean);
		}

		/** An entry the vault already holds in its first group, as an open vault has it */
		MysticCryptEntryModelBean addEntryToExistingGroup(final UUID id, final String title,
			final Instant created, final Instant modified)
		{
			MysticCryptEntryModelBean entry = entryBean(id, title, modified);
			entry.setCreationTime(OffsetDateTime.ofInstant(created, ZoneOffset.UTC));
			root.getChildren().iterator().next().getValue().getDefaultContent().add(entry);
			return entry;
		}

		List<MysticCryptEntryModelBean> entries()
		{
			List<MysticCryptEntryModelBean> found = new ArrayList<>();
			collect(root, new ArrayList<>(), found, null);
			return found;
		}

		long countOf(final UUID id)
		{
			return entries().stream().filter(entry -> id.equals(entry.getId())).count();
		}

		MysticCryptEntryModelBean only(final UUID id)
		{
			List<MysticCryptEntryModelBean> found = entries().stream()
				.filter(entry -> id.equals(entry.getId())).toList();
			assertEquals(1, found.size(), "the vault holds " + found.size() + " entries of " + id);
			return found.get(0);
		}

		/** The names of the groups from under the vault root down to the one holding the entry */
		List<String> groupPathOf(final UUID id)
		{
			List<List<String>> paths = new ArrayList<>();
			collect(root, new ArrayList<>(), new ArrayList<>(), (path, entry) -> {
				if (id.equals(entry.getId()))
				{
					paths.add(new ArrayList<>(path));
				}
			});
			assertEquals(1, paths.size());
			return paths.get(0);
		}

		private void collect(
			final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node,
			final List<String> path, final List<MysticCryptEntryModelBean> found,
			final BiConsumer<List<String>, MysticCryptEntryModelBean> visitor)
		{
			List<MysticCryptEntryModelBean> content = node.getValue().getDefaultContent();
			if (content != null)
			{
				for (MysticCryptEntryModelBean entry : content)
				{
					found.add(entry);
					if (visitor != null)
					{
						visitor.accept(path, entry);
					}
				}
			}
			if (node.getChildren() == null)
			{
				return;
			}
			for (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> child : node
				.getChildren())
			{
				List<String> childPath = new ArrayList<>(path);
				childPath.add(child.getDisplayValue());
				collect(child, childPath, found, visitor);
			}
		}
	}

	private static BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node(
		final long id, final String name)
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> element = GenericTreeElement
			.<List<MysticCryptEntryModelBean>> builder().name(name).leaf(false).build();
		element.setDefaultContent(new ArrayList<>());
		return BaseTreeNode.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder()
			.id(id).value(element).displayValue(name).leaf(false).build();
	}
}
