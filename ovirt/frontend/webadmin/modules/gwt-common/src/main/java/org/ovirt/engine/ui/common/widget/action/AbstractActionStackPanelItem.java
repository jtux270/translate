package org.ovirt.engine.ui.common.widget.action;

import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.uicommon.model.DeferredModelCommandInvoker;
import org.ovirt.engine.ui.common.uicommon.model.SearchableTableModelProvider;
import org.ovirt.engine.ui.uicommonweb.UICommand;

import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;

public abstract class AbstractActionStackPanelItem<M, T, W extends Widget> extends Composite {

    @UiField(provided = true)
    @WithElementId
    public AbstractActionPanel<T> actionPanel;

    @UiField(provided = true)
    @WithElementId("display")
    public W dataDisplayWidget;

    public AbstractActionStackPanelItem(M modelProvider) {
        this.dataDisplayWidget = createDataDisplayWidget(modelProvider);
        this.actionPanel = createActionPanel(modelProvider);
        addDoubleClickHandler(dataDisplayWidget, modelProvider);
    }

    @Override
    protected void initWidget(Widget widget) {
        super.initWidget(widget);

        // Add context menu handler for data display widget
        actionPanel.addContextMenuHandler(dataDisplayWidget);
    }

    public W getDataDisplayWidget() {
        return dataDisplayWidget;
    }

    protected abstract W createDataDisplayWidget(M modelProvider);

    protected abstract AbstractActionPanel<T> createActionPanel(M modelProvider);

    void addDoubleClickHandler(final W widget, final M modelProvider) {
        if (modelProvider instanceof SearchableTableModelProvider<?, ?>) {
            widget.addDomHandler(new DoubleClickHandler() {
                @Override
                public void onDoubleClick(DoubleClickEvent event) {
                    SearchableListModel model = ((SearchableTableModelProvider<?, ?>) modelProvider).getModel();
                    UICommand command = model.getDoubleClickCommand();
                    if (command != null && command.getIsExecutionAllowed()) {
                        DeferredModelCommandInvoker invoker = new DeferredModelCommandInvoker(model);
                        invoker.invokeCommand(command);
                    }
                }
            }, DoubleClickEvent.getType());
        }
    }

}
