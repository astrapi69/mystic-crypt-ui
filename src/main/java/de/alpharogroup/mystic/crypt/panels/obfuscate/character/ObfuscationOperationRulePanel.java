package de.alpharogroup.mystic.crypt.panels.obfuscate.character;

import java.awt.event.ActionEvent;

import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import de.alpharogroup.crypto.obfuscation.rule.ObfuscationOperationRule;
import de.alpharogroup.crypto.obfuscation.rule.Operation;
import de.alpharogroup.model.api.Model;
import de.alpharogroup.swing.base.BasePanel;
import de.alpharogroup.swing.combobox.model.EnumComboBoxModel;
import de.alpharogroup.swing.docfilter.IntegerArrayFilter;
import lombok.Getter;

@Getter
public class ObfuscationOperationRulePanel extends BasePanel<ObfuscationOperationModelBean>
{

	private static final long serialVersionUID = 1L;

    private javax.swing.JButton btnAdd;
    private javax.swing.JComboBox<Operation> cmbOperation;
    private javax.swing.JLabel lblIndexes;
    private javax.swing.JLabel lblObfuscationOperationRule;
    private javax.swing.JLabel lblOperation;
    private javax.swing.JLabel lblOriginalChar;
    private javax.swing.JLabel lblReplaceWith;
    private javax.swing.JTextField txtIndexes;
    private javax.swing.JTextField txtOriginalChar;
    private javax.swing.JTextField txtRelpaceWith;

	public ObfuscationOperationRulePanel(final Model<ObfuscationOperationModelBean> model)
	{
		super(model);
	}
	
	protected void onAdd(final ActionEvent actionEvent)
	{
	}

	public void onIndexesValidationError(String text) {
		
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

        lblOriginalChar = new javax.swing.JLabel();
        lblReplaceWith = new javax.swing.JLabel();
        txtOriginalChar = new javax.swing.JTextField();
        txtRelpaceWith = new javax.swing.JTextField();
        btnAdd = new javax.swing.JButton();
        lblObfuscationOperationRule = new javax.swing.JLabel();
        lblIndexes = new javax.swing.JLabel();
        txtIndexes = new javax.swing.JTextField();
        
        lblOperation = new javax.swing.JLabel();
        cmbOperation = new javax.swing.JComboBox<>();

        lblOriginalChar.setText("Original Character");

        lblReplaceWith.setText("Replace with");

        btnAdd.setText("Add");

        lblObfuscationOperationRule.setFont(new java.awt.Font("Ubuntu", 1, 12)); // NOI18N
        lblObfuscationOperationRule.setText("Obfuscation Operation Rule");

        lblIndexes.setText("Indexes");

        lblOperation.setText("Operation");
        
        // == custom edit ==
		txtOriginalChar.setDocument(new RangeDocument(0, 1));
		txtRelpaceWith.setDocument(new RangeDocument(0, 1));
        
        btnAdd.addActionListener(actionEvent -> onAdd(actionEvent));
        cmbOperation.setModel(new EnumComboBoxModel<>(Operation.class));
        
        
        Document document = txtIndexes.getDocument();
        PlainDocument doc = (PlainDocument) document;
        doc.setDocumentFilter(new IntegerArrayFilter() {        	
        	public void onValidationError(String text) {
        		ObfuscationOperationRulePanel.this.onIndexesValidationError(text);
        	}
        });
        
	}
	
	
	public void onEditObfuscationOperationRule(
		ObfuscationOperationRule<Character, Character> selected)
	{
		txtOriginalChar.setText(selected.getCharacter().toString());
		txtRelpaceWith.setText(selected.getReplaceWith().toString());
		txtIndexes.setText(selected.getIndexes().toString());
		cmbOperation.setSelectedItem(selected.getOperation());		
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
	}
	

	protected void onInitializeGroupLayout()
	{		
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblObfuscationOperationRule)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(txtOriginalChar, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
                            .addComponent(lblOriginalChar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtRelpaceWith, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblReplaceWith, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtIndexes, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblIndexes, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(cmbOperation, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(lblOperation, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblObfuscationOperationRule)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblOriginalChar, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblReplaceWith)
                    .addComponent(lblIndexes)
                    .addComponent(lblOperation))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtOriginalChar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtRelpaceWith, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAdd)
                    .addComponent(txtIndexes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cmbOperation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(22, Short.MAX_VALUE))
        );
	}
	
}
