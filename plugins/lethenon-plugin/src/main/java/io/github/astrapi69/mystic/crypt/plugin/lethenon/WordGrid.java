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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.TransferHandler;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.danekja.java.util.function.serializable.SerializableConsumer;
import org.danekja.java.util.function.serializable.SerializableSupplier;

import io.github.astrapi69.lethenon.SeedPhrase;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.swing.model.component.JMTextField;
import net.miginfocom.swing.MigLayout;

/**
 * The 24 words of a wallet typed into 24 fields, each labelled with its number, in four columns read
 * from top to bottom as step 1 of the backup shows them, behind one {@link TypedWords} (#804).
 * <p>
 * A paste of several words is a phrase: it fills the fields from the first, whichever field it is
 * pasted into, and what the fields held before is replaced. Words that do not fit stay together in
 * the last field, so nothing pasted is lost and the count check names how many there were. A paste
 * of one word is an ordinary paste into its field. Reading the paste is lethenon's
 * {@link SeedPhrase#parse}, which drops the numbers of a backup sheet and takes any space, line
 * break or comma between the words.
 * <p>
 * A field is checked when the focus leaves it and after a paste: a word that is not in the word list
 * marks the field and is named at once, in the sentence the window shows for it when the form is
 * sent, not only then; correcting it removes the mark. A field that is not marked is not checked
 * while it is typed into, so a word half typed is not called wrong.
 */
final class WordGrid extends JPanel
{

	/** The client property FlatLaf draws a field's outline from, and its value for a mistake */
	static final String OUTLINE = "JComponent.outline";

	private static final String OUTLINE_ERROR = "error";

	private static final int COLUMNS = 4;

	private static final long serialVersionUID = 1L;

	private final List<JMTextField> fields = new ArrayList<>();

	private final TypedWords words;

	private final Locale locale;

	private final transient Consumer<String> unknownWord;

	private final String fieldTooltip;

	/**
	 * Instantiates the grid
	 *
	 * @param name
	 *            what the names of its components are built from: field 5 of "Words" is
	 *            {@code txtWords5}, its label {@code lblWords5}
	 * @param words
	 *            the model the fields write to
	 * @param locale
	 *            the language of the window
	 * @param unknownWord
	 *            told the sentence that names the first word not in the word list, or an empty
	 *            sentence once there is none
	 */
	WordGrid(final String name, final TypedWords words, final Locale locale,
		final Consumer<String> unknownWord)
	{
		super(new MigLayout("insets 0, gap 6 4, wrap " + COLUMNS * 2,
			"[right][120!]".repeat(COLUMNS)));
		this.words = words;
		this.locale = locale;
		this.unknownWord = unknownWord;
		this.fieldTooltip = LethenonMessages.getString("lethenon.backup.tooltip.answer",
			"the word at this position in your note", locale);
		setToolTipText(LethenonMessages.getString("lethenon.wallet.tooltip.words",
			"the 24 words from your note, one in each field; a phrase pasted into any field fills "
				+ "all 24",
			locale));
		for (int position = 1; position <= BackupCheck.WORDS; position++)
		{
			fields.add(field(name, position));
		}
		int rows = BackupCheck.WORDS / COLUMNS;
		for (int row = 0; row < rows; row++)
		{
			for (int column = 0; column < COLUMNS; column++)
			{
				int position = column * rows + row + 1;
				JLabel label = new JLabel(String.valueOf(position));
				label.setName("lbl" + name + position);
				label.setLabelFor(fields.get(position - 1));
				add(label);
				add(fields.get(position - 1), "growx");
			}
		}
	}

	/**
	 * What a window does with the sentences of its grid when it shows its refusals in one place: a
	 * sentence about a word is shown there at once; once the word is corrected the sentence goes,
	 * but a refusal of something else that came after it stays
	 *
	 * @param refuse
	 *            shows a refusal, or takes it away when given an empty one
	 * @param shown
	 *            the refusal the window shows now
	 * @return what the grid tells about the words
	 */
	static Consumer<String> sayingIn(final Consumer<String> refuse, final Supplier<String> shown)
	{
		AtomicReference<String> said = new AtomicReference<>("");
		return sentence -> {
			if (!sentence.isEmpty() || shown.get().equals(said.get()))
			{
				refuse.accept(sentence);
			}
			said.set(sentence);
		};
	}

	/**
	 * Empties every field and the model, and takes every mark away. The model is emptied itself, not
	 * only through the fields: a field that already shows nothing tells its model nothing when it is
	 * emptied again
	 */
	void clear()
	{
		fields.forEach(field -> field.setText(""));
		words.clear();
		for (int position = 1; position <= fields.size(); position++)
		{
			mark(position);
		}
		say();
	}

	private JMTextField field(final String name, final int position)
	{
		JMTextField field = new JMTextField(10);
		field.setName("txt" + name + position);
		field.setToolTipText(fieldTooltip);
		SerializableSupplier<String> getter = () -> words.get(position);
		SerializableConsumer<String> setter = word -> words.set(position, word);
		field.setPropertyModel(LambdaModel.of(getter, setter));
		field.setTransferHandler(new PhrasePaste(field.getTransferHandler()));
		field.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(final FocusEvent focusEvent)
			{
				check(position);
			}
		});
		field.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(final DocumentEvent documentEvent)
			{
				recheckIfMarked(position);
			}

			@Override
			public void removeUpdate(final DocumentEvent documentEvent)
			{
				recheckIfMarked(position);
			}

			@Override
			public void changedUpdate(final DocumentEvent documentEvent)
			{
				recheckIfMarked(position);
			}
		});
		return field;
	}

	/** Puts the words of a paste into the model, as {@link TypedWords#spread} places them, and shows them */
	private void spread(final List<String> pasted)
	{
		words.spread(pasted);
		for (int position = 1; position <= fields.size(); position++)
		{
			fields.get(position - 1).setText(words.get(position));
		}
		for (int position = 1; position <= fields.size(); position++)
		{
			mark(position);
		}
		say();
	}

	private void check(final int position)
	{
		mark(position);
		say();
	}

	private void recheckIfMarked(final int position)
	{
		if (OUTLINE_ERROR.equals(fields.get(position - 1).getClientProperty(OUTLINE)))
		{
			check(position);
		}
	}

	private void mark(final int position)
	{
		JMTextField field = fields.get(position - 1);
		Optional<String> sentence = sentenceFor(position);
		field.putClientProperty(OUTLINE, sentence.isPresent() ? OUTLINE_ERROR : null);
		field.setToolTipText(sentence.orElse(fieldTooltip));
	}

	/** Names the first marked field, or says that none is */
	private void say()
	{
		for (int position = 1; position <= fields.size(); position++)
		{
			if (OUTLINE_ERROR.equals(fields.get(position - 1).getClientProperty(OUTLINE)))
			{
				unknownWord.accept(sentenceFor(position).orElse(""));
				return;
			}
		}
		unknownWord.accept("");
	}

	/**
	 * The sentence naming the word of this field that is not in the word list, the one a form says
	 * when it is sent (T2)
	 */
	private Optional<String> sentenceFor(final int position)
	{
		return SeedPhrase.parse(fields.get(position - 1).getText()).stream()
			.filter(word -> !SeedPhrase.isKnownWord(word)).findFirst()
			.map(word -> WalletInput.unknownWord(position, word).text(locale));
	}

	/**
	 * Ctrl+V and a drop: a paste of several words is spread over the grid, one word goes into its
	 * field as the field's own handler puts it there. Copying out of a field is the field's own
	 * handler's as well
	 */
	private final class PhrasePaste extends TransferHandler
	{

		private static final long serialVersionUID = 1L;

		private final TransferHandler standard;

		private PhrasePaste(final TransferHandler standard)
		{
			this.standard = standard;
		}

		@Override
		public boolean canImport(final TransferSupport support)
		{
			return standard.canImport(support);
		}

		@Override
		public boolean importData(final TransferSupport support)
		{
			Optional<String> pasted = textOf(support.getTransferable());
			if (pasted.isEmpty())
			{
				return standard.importData(support);
			}
			List<String> words = SeedPhrase.parse(pasted.get());
			if (words.isEmpty())
			{
				return false;
			}
			if (words.size() == 1)
			{
				return standard.importData(support);
			}
			spread(words);
			return true;
		}

		@Override
		public int getSourceActions(final JComponent component)
		{
			return standard.getSourceActions(component);
		}

		@Override
		public void exportToClipboard(final JComponent component,
			final java.awt.datatransfer.Clipboard clipboard, final int action)
		{
			standard.exportToClipboard(component, clipboard, action);
		}

		@Override
		public void exportAsDrag(final JComponent component, final InputEvent event,
			final int action)
		{
			standard.exportAsDrag(component, event, action);
		}

		@Override
		public Icon getVisualRepresentation(final Transferable transferable)
		{
			return standard.getVisualRepresentation(transferable);
		}

		/**
		 * The pasted text, or empty when there is none to read: then the field's own handler takes
		 * the paste and answers for it, the way it would without this grid
		 */
		private static Optional<String> textOf(final Transferable transferable)
		{
			if (!transferable.isDataFlavorSupported(DataFlavor.stringFlavor))
			{
				return Optional.empty();
			}
			try
			{
				return Optional.of((String)transferable.getTransferData(DataFlavor.stringFlavor));
			}
			catch (UnsupportedFlavorException | IOException unreadable)
			{
				return Optional.empty();
			}
		}
	}
}
