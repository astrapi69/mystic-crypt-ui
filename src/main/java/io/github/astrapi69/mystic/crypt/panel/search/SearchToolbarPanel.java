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
package io.github.astrapi69.mystic.crypt.panel.search;

import java.util.List;

import javax.swing.event.DocumentEvent;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.ApplicationPanel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.SecretKeyTreeWithContentPanel;
import io.github.astrapi69.mystic.crypt.search.DatabaseSearchSupport;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.listener.document.DocumentListenerAdapter;
import io.github.astrapi69.swing.model.component.JMTextField;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;
import lombok.Getter;

/**
 * The search field in the toolbar, the way KeePass users expect one: typing jumps to the first node
 * that matches, Enter jumps to the next and starts over after the last. What matches is decided by
 * {@link DatabaseSearchSupport}, the same code the Search dialog uses.
 * <p>
 * The field only jumps; it never filters the tree. Every edit operation keeps the swing tree and
 * the model tree in lockstep by index, and editing on a pruned tree would let those indices diverge
 * - in a password manager that is data loss, not a display glitch.
 */
@Getter
public class SearchToolbarPanel extends BasePanel<SearchToolbarModelBean>
{

	private static final long serialVersionUID = 1L;

	private JMTextField txtToolbarSearch;

	public SearchToolbarPanel()
	{
		this(BaseModel.of(new SearchToolbarModelBean()));
	}

	/**
	 * Instantiates a new {@link SearchToolbarPanel} over the given model
	 *
	 * @param model
	 *            what the panel is to show and write into
	 */
	public SearchToolbarPanel(final IModel<SearchToolbarModelBean> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
		txtToolbarSearch = new JMTextField(18);
		txtToolbarSearch.setName("txtToolbarSearch");
		txtToolbarSearch.setToolTipText(
			"Search the database: typing jumps to the first match, Enter to the next");
		txtToolbarSearch.setPropertyModel(LambdaModel.of(() -> getModelObject().getSearchTerm(),
			getModelObject()::setSearchTerm));
		txtToolbarSearch.getDocument().addDocumentListener(new DocumentListenerAdapter()
		{
			@Override
			public void onDocumentChanged(final DocumentEvent documentEvent)
			{
				onSearchTermChanged();
			}
		});
		txtToolbarSearch.addActionListener(actionEvent -> onNextMatch());
		// the field clears itself when it is switched off: locking the workspace disables it, and
		// a search term left standing in a locked application is content on display
		txtToolbarSearch.addPropertyChangeListener("enabled", changeEvent -> {
			if (Boolean.FALSE.equals(changeEvent.getNewValue()))
			{
				txtToolbarSearch.setText("");
			}
		});
		add(txtToolbarSearch);
	}

	/**
	 * Jumps to the first match of the term as it stands. Called on every keystroke, so it does
	 * nothing at all when there is no term or nobody is signed in.
	 */
	protected void onSearchTermChanged()
	{
		// the term is taken from the document rather than read back from the model: both this
		// listener and the binding's own listener hang on the same document, and which of them runs
		// first is not defined - reading the model here would search for the term as it was one
		// keystroke ago, or for nothing at all when the whole term arrives at once
		getModelObject().setSearchTerm(txtToolbarSearch.getText());
		getModelObject().setMatchIndex(0);
		jumpToCurrentMatch();
	}

	/** Jumps to the next match, and to the first again after the last */
	protected void onNextMatch()
	{
		List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> matches = currentMatches();
		if (matches.isEmpty())
		{
			return;
		}
		getModelObject().setMatchIndex(getModelObject().nextMatchIndex(matches.size()));
		selectMatch(matches);
	}

	private void jumpToCurrentMatch()
	{
		List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> matches = currentMatches();
		if (!matches.isEmpty())
		{
			selectMatch(matches);
		}
	}

	private void selectMatch(
		final List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> matches)
	{
		SecretKeyTreeWithContentPanel treePanel = treePanel();
		if (treePanel != null)
		{
			int index = Math.min(getModelObject().getMatchIndex(), matches.size() - 1);
			treePanel.selectTreeNode(matches.get(index));
		}
	}

	/**
	 * The matches for the term as it stands, or nothing when no database is open.
	 * <p>
	 * The tree panel is resolved fresh on every call and never kept: signing in replaces the whole
	 * application panel, and a kept reference would jump around in a tree that is no longer on
	 * screen.
	 *
	 * @return the matches, never null
	 */
	private List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> currentMatches()
	{
		SecretKeyTreeWithContentPanel treePanel = treePanel();
		if (treePanel == null)
		{
			return List.of();
		}
		return DatabaseSearchSupport.findMatches(treePanel.getModelObject(),
			getModelObject().getSearchTerm());
	}

	private static SecretKeyTreeWithContentPanel treePanel()
	{
		MysticCryptApplicationFrame frame = MysticCryptApplicationFrame.getInstance();
		if (frame == null)
		{
			return null;
		}
		ApplicationPanel applicationPanel = frame.getApplicationPanel();
		return applicationPanel == null
			? null
			: applicationPanel.getSecretKeyTreeWithContentPanel();
	}

}
