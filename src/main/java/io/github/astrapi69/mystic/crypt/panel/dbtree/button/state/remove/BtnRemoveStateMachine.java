package io.github.astrapi69.mystic.crypt.panel.dbtree.button.state.remove;

import io.github.astrapi69.file.create.FileContentInfo;
import io.github.astrapi69.swing.table.GenericJXTable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import io.github.astrapi69.mystic.crypt.panel.dbtree.AttachmentTableModel;
import io.github.astrapi69.mystic.crypt.panel.dbtree.button.state.AbstractJButtonStateMachine;

import java.util.Optional;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@SuperBuilder
public class BtnRemoveStateMachine extends AbstractJButtonStateMachine<BtnRemoveStateMachine>
	implements
		BtnRemoveState
{
	GenericJXTable<FileContentInfo> attachmentTable;

	@Override
	protected void updateButtonState()
	{
		if (attachmentTable == null)
		{
			setEnabled(false);
			return;
		}
		setEnabled(attachmentTable.getSingleSelectedRowData().isPresent());
	}

	@Override
	public void onInitialize()
	{
		updateButtonState();
	}

	@Override
	public void onTableSelection()
	{
		updateButtonState();
	}
}
