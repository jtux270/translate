package org.ovirt.engine.ui.common.widget.uicommon.disks;

import org.ovirt.engine.core.common.businessentities.Disk.DiskStorageType;
import org.ovirt.engine.ui.common.CommonApplicationConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Widget;

public class DisksViewRadioGroup extends Composite {

    CommonApplicationConstants constants = GWT.create(CommonApplicationConstants.class);

    RadioButton allButton;
    RadioButton imagesButton;
    RadioButton lunsButton;

    public DisksViewRadioGroup() {
        initWidget(getRadioGroupPanel());
    }

    private Widget getRadioGroupPanel() {
        allButton = new RadioButton("diskTypeView"); //$NON-NLS-1$
        imagesButton = new RadioButton("diskTypeView"); //$NON-NLS-1$
        lunsButton = new RadioButton("diskTypeView"); //$NON-NLS-1$

        allButton.getElement().getStyle().setMarginRight(20, Unit.PX);
        imagesButton.getElement().getStyle().setMarginRight(20, Unit.PX);
        lunsButton.getElement().getStyle().setMarginRight(20, Unit.PX);

        FlowPanel buttonsPanel = new FlowPanel();
        buttonsPanel.getElement().getStyle().setProperty("marginLeft", "auto"); //$NON-NLS-1$ //$NON-NLS-2$
        buttonsPanel.getElement().getStyle().setProperty("marginRight", "auto"); //$NON-NLS-1$ //$NON-NLS-2$
        buttonsPanel.add(allButton);
        buttonsPanel.add(imagesButton);
        buttonsPanel.add(lunsButton);

        setDiskStorageType(null);
        localize(constants);

        return buttonsPanel;
    }

    public void setClickHandler(ClickHandler clickHandler) {
        allButton.addClickHandler(clickHandler);
        imagesButton.addClickHandler(clickHandler);
        lunsButton.addClickHandler(clickHandler);
    }

    void localize(CommonApplicationConstants constants) {
        allButton.setText(constants.allDisksLabel());
        imagesButton.setText(constants.imageDisksLabel());
        lunsButton.setText(constants.lunDisksLabel());
    }

    public RadioButton getAllButton() {
        return allButton;
    }

    public RadioButton getImagesButton() {
        return imagesButton;
    }

    public RadioButton getLunsButton() {
        return lunsButton;
    }

    public DiskStorageType getDiskStorageType() {
        return imagesButton.getValue() ? DiskStorageType.IMAGE :
                lunsButton.getValue() ? DiskStorageType.LUN : null;
    }

    public void setDiskStorageType(DiskStorageType diskStorageType) {
        allButton.setValue(diskStorageType == null);
        imagesButton.setValue(diskStorageType == DiskStorageType.IMAGE);
        lunsButton.setValue(diskStorageType == DiskStorageType.LUN);
    }

}
