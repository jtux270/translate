package org.ovirt.engine.ui.webadmin.section.main.view.popup.host.panels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.gwtbootstrap3.client.ui.Column;
import org.gwtbootstrap3.client.ui.Label;
import org.gwtbootstrap3.client.ui.Row;
import org.ovirt.engine.core.common.businessentities.network.ReportedConfiguration;
import org.ovirt.engine.core.common.businessentities.network.ReportedConfigurationType;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;

import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.dom.client.Style.TextAlign;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

public class NetworkOutOfSyncPanel extends FlowPanel {

    private final static String FONT = "Arial Unicode MS,Arial,sans-serif";//$NON-NLS-1$
    private final static String MAIN_COLUMN_SIZE = "LG_13";//$NON-NLS-1$
    private final static String PROPERTY_COLUMN_SIZE = "LG_6";//$NON-NLS-1$
    private final static String COLUMN_SIZE = "LG_3";//$NON-NLS-1$
    private final static String BACKGROUND_COLOR = "rgb(67, 67, 67)";//$NON-NLS-1$
    private final static String WHITE_TEXT_COLOR = "white";//$NON-NLS-1$
    private final static String TEXT_COLOR = "#c4c4c4";//$NON-NLS-1$
    private FlowPanel flowPanel;
    private final List<ReportedConfiguration> reportedConfigurationList;
    private final static ApplicationConstants constants = AssetProvider.getConstants();

    public NetworkOutOfSyncPanel(List<ReportedConfiguration> reportedConfigurationList) {
        this.flowPanel = new FlowPanel();
        this.reportedConfigurationList = reportedConfigurationList;
        sortReportedConfigurationList();
    }

    private void sortReportedConfigurationList(){
        Comparator<ReportedConfiguration> reportedConfigurationComparator = new Comparator<ReportedConfiguration>() {
            @Override public int compare(ReportedConfiguration reportedConfiguration, ReportedConfiguration t1) {
                return reportedConfiguration.getType().getName().compareTo(t1.getType().getName());
            }
        };
        Collections.sort(reportedConfigurationList, reportedConfigurationComparator);
    }

    public Widget outOfSyncTableAsWidget() {
        addTableFirstRow();
        for (ReportedConfiguration reportedConfiguration : reportedConfigurationList) {
            List<Label> values = reportedConfigurationAsList(reportedConfiguration);
            TextAlign textAlign = resolveAlignment(reportedConfiguration.getType());
            addOutOfSyncRow(values, textAlign);
        }
        return flowPanel.asWidget();
    }

    private void addOutOfSyncRow(List<Label> values, TextAlign textAlign){
        addOutOfSyncRow(values, textAlign, false);
    }

    private void addOutOfSyncRow(List<Label> values, TextAlign textAlign, boolean bottomBorderSolid) {
        Row row = new Row();
        Column mainColumn = new Column(MAIN_COLUMN_SIZE);
        boolean firstTime = true;
        for (Label label : values) {
            Column subColumn;
            if (firstTime) {
                subColumn = new Column(PROPERTY_COLUMN_SIZE);
                firstTime = false;
            }
            else {
                subColumn = new Column(COLUMN_SIZE);
                subColumn.getElement().getStyle().setTextAlign(textAlign);
            }
            subColumn.add(label);
            mainColumn.add(subColumn);
        }
        row.add(mainColumn);

        if (bottomBorderSolid){
            Row container = new Row();
            container.setPaddingBottom(4);
            container.add(row);
            container.getElement().getStyle().setProperty("margin", "0 0 4px");//$NON-NLS-1$ //$NON-NLS-2$
            container.getElement().getStyle().setProperty("borderBottomStyle", "solid");//$NON-NLS-1$ //$NON-NLS-2$
            container.getElement().getStyle().setProperty("borderWidth", "1px");//$NON-NLS-1$ //$NON-NLS-2$
            flowPanel.add(container);
        }
        else {
            flowPanel.add(row);
        }
    }

    private Label createLabel(String text) {
        return createLabel(text, false);
    }

    private Label createLabel(String text, boolean boldText) {
        Label output = new Label(text);
        output.getElement().getStyle().setBackgroundColor(BACKGROUND_COLOR);
        if (boldText) {
            output.getElement().getStyle().setFontWeight(FontWeight.BOLD);
        }
        final String textColor = boldText ? WHITE_TEXT_COLOR : TEXT_COLOR;
        output.getElement().getStyle().setColor(textColor);
        output.getElement().getStyle().setProperty("fontFamily", FONT);//$NON-NLS-1$
        return output;
    }

    private List<Label> reportedConfigurationAsList(ReportedConfiguration reportedConfiguration) {
        List<Label> values = new ArrayList<>();
        String property = reportedConfigurationTypeToString(reportedConfiguration.getType());
        values.add(createLabel(property));
        values.add(createLabel(reportedConfiguration.getActualValue()));
        values.add(createLabel(reportedConfiguration.getExpectedValue()));
        return values;
    }

    private String reportedConfigurationTypeToString(ReportedConfigurationType reportedConfigurationType) {
        switch (reportedConfigurationType) {
        case MTU:
            return constants.mtuOutOfSyncPopUp();
        case BRIDGED:
            return constants.bridgedOutOfSyncPopUp();
        case VLAN:
            return constants.vlanOutOfSyncPopUp();
        case BOOT_PROTOCOL:
            return constants.bootProtocolOutOfSyncPopUp();
        case IP_ADDRESS:
            return constants.ipAddressOutOfSyncPopUp();
        case NETMASK:
            return constants.netmaskOutOfSyncPopUp();
        case GATEWAY:
            return constants.gatewayOutOfSyncPopUp();
        case OUT_AVERAGE_LINK_SHARE:
            return constants.outAverageLinkShareOutOfSyncPopUp();
        case OUT_AVERAGE_UPPER_LIMIT:
            return constants.outAverageUpperLimitOutOfSyncPopUp();
        case OUT_AVERAGE_REAL_TIME:
            return constants.outAverageRealTimeOutOfSyncPopUp();
        default:
            return constants.unknownPropertyOutOfSyncPopUp();
        }
    }

    private TextAlign resolveAlignment(ReportedConfigurationType reportedConfigurationType) {
        return reportedConfigurationType != ReportedConfigurationType.BRIDGED ? TextAlign.RIGHT : TextAlign.CENTER;
    }


    private void addTableFirstRow() {
        List<Label> values = new ArrayList<Label>() {
            {
                add(createLabel(constants.PropertyOutOfSyncPopUp(), true));
                add(createLabel(constants.hostOutOfSyncPopUp(), true));
                add(createLabel(constants.dcOutOfSyncPopUp(), true));
            }
        };
        addOutOfSyncRow(values, TextAlign.CENTER, true);
    }

}
