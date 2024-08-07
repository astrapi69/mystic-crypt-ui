package io.github.astrapi69.mystic.crypt.wizard;

import java.math.BigInteger;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;

import com.github.lgooddatepicker.components.CalendarPanel;
import com.github.lgooddatepicker.components.DatePicker;

import io.github.astrapi69.collection.array.ArrayFactory;
import io.github.astrapi69.collection.pair.ValueBox;
import io.github.astrapi69.crypt.data.algorithm.AlgorithmExtensions;
import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.random.number.RandomBigIntegerFactory;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.model.combobox.GenericComboBoxModel;
import io.github.astrapi69.swing.model.combobox.GenericMutableComboBoxModel;
import io.github.astrapi69.swing.model.component.JMComboBox;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;

@Getter
public class DatesPanel extends BasePanel<BaseWizardStateMachineModel<CertificateInfoModel>>
{

	private JLabel lblHeader;
	private JLabel lblNotAfter;
	private JLabel lblNotBefore;
	private JLabel lblSerialNumber;
	private JLabel lblSignatureAlgorithm;
	private JLabel lblVersion;
	private CalendarPanel txtNotAfter;
	private CalendarPanel txtNotBefore;
	private JMBigIntegerTextField txtSerialNumber;
	private JMComboBox<Integer, GenericComboBoxModel<Integer>> cmbVersion;
	private JButton btnGenerateSerialNumber;
	private JMComboBox<String, GenericMutableComboBoxModel<String>> cmbSignatureAlgorithm;

	public DatesPanel(IModel<BaseWizardStateMachineModel<CertificateInfoModel>> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		lblHeader = new JLabel("Issuer");
		lblNotAfter = new JLabel("Not After:");
		lblNotBefore = new JLabel("Not Before:");
		lblSerialNumber = new JLabel("Serial Number:");
		lblSignatureAlgorithm = new JLabel("Signature Algorithm:");
		lblVersion = new JLabel("Version:");

		txtNotBefore = new CalendarPanel(new DatePicker());
		txtNotAfter = new CalendarPanel(new DatePicker());
		txtSerialNumber = new JMBigIntegerTextField();
		btnGenerateSerialNumber = new JButton();

		ValueBox<String> signatureAlgorithmValueBox;
		IModel<String> selectedSignatureAlgorithmModel;
		CertificateInfoModel modelObject = getModelObject().getModelObject();
		String selectedSignatureAlgorithm = modelObject.getSignatureAlgorithm();
		Set<String> signatureAlgorithms = AlgorithmExtensions.getAlgorithms("Signature");
		GenericMutableComboBoxModel<String> cmbApplicationFileModel;
		signatureAlgorithmValueBox = ValueBox.<String> builder().value(selectedSignatureAlgorithm)
			.build();
		selectedSignatureAlgorithmModel = LambdaModel.of(signatureAlgorithmValueBox::getValue,
			signatureAlgorithmValueBox::setValue);

		cmbApplicationFileModel = new GenericMutableComboBoxModel<>(signatureAlgorithms,
			selectedSignatureAlgorithm);

		cmbSignatureAlgorithm = new JMComboBox<>(cmbApplicationFileModel,
			selectedSignatureAlgorithmModel);

		GenericComboBoxModel<Integer> comboBoxModel;
		ValueBox<Integer> valueBox;
		IModel<Integer> selectedItemModel;
		valueBox = ValueBox.<Integer> builder().value(3).build();
		Integer[] cmbArray = ArrayFactory.newArray(1, 3);
		comboBoxModel = new GenericComboBoxModel<>(cmbArray);
		selectedItemModel = LambdaModel.of(valueBox::getValue, valueBox::setValue);
		cmbVersion = new JMComboBox<>(comboBoxModel, selectedItemModel);

		txtSerialNumber
			.setPropertyModel(LambdaModel.of(modelObject::getSerial, modelObject::setSerial));

		txtNotAfter.setSelectedDate(modelObject.getValidityModel().getNotAfter().toLocalDate());
		txtNotBefore.setSelectedDate(modelObject.getValidityModel().getNotBefore().toLocalDate());

		btnGenerateSerialNumber.setText("Generate");
		btnGenerateSerialNumber.addActionListener(this::onGenerateSerialNumber);
	}

	protected void onGenerateSerialNumber(java.awt.event.ActionEvent evt)
	{
		BigInteger serialNumber = RandomBigIntegerFactory.randomSerialNumber();
		CertificateInfoModel modelObject = getModelObject().getModelObject();
		modelObject.setSerial(serialNumber);
		getTxtSerialNumber().setText(serialNumber.toString());
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		setLayout(new MigLayout("", "[][grow]", "[][][][][]"));

		add(lblHeader, "span, align center, wrap 10");
		add(lblVersion, "cell 0 0, alignx trailing");
		add(cmbVersion, "cell 1 0, growx");

		add(lblSerialNumber, "cell 0 1, alignx trailing");
		add(txtSerialNumber, "cell 1 1, growx, split 2");
		add(btnGenerateSerialNumber, "cell 1 1");

		add(lblNotBefore, "cell 0 2, alignx trailing");
		add(txtNotBefore, "cell 1 2, growx");

		add(lblNotAfter, "cell 0 3, alignx trailing");
		add(txtNotAfter, "cell 1 3, growx");

		add(lblSignatureAlgorithm, "cell 0 4, alignx trailing");
		add(cmbSignatureAlgorithm, "cell 1 4, growx");
	}
}
