package org.ovirt.engine.ui.common.view.popup.numa;

import org.ovirt.engine.ui.common.CommonApplicationMessages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class CpuSummaryPanel extends Composite {

    interface WidgetUiBinder extends UiBinder<Widget, CpuSummaryPanel> {
        WidgetUiBinder uiBinder = GWT.create(WidgetUiBinder.class);
    }

    private final CommonApplicationMessages messages;

    @UiField
    Label nameLabel;

    @UiField
    Label totalLabel;

    @UiField
    Label percentageLabel;

    @Inject
    public CpuSummaryPanel(CommonApplicationMessages messages) {
        initWidget(WidgetUiBinder.uiBinder.createAndBindUi(this));
        this.messages = messages;
    }

    public void setName(String name) {
        nameLabel.setTitle(name);
        nameLabel.setText(name);
    }

    public void setCpus(int totalCpus, int usedPercentage) {
        String totalCpusString = messages.numaTotalCpus(totalCpus);

        totalLabel.setTitle(totalCpusString);
        totalLabel.setText(totalCpusString);

        String percentageUsed = messages.numaPercentUsed(usedPercentage);
        percentageLabel.setTitle(percentageUsed);
        percentageLabel.setText(percentageUsed);
    }
}
