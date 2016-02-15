package org.ovirt.engine.ui.common.view.popup.numa;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.VdsNumaNode;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationMessages;
import org.ovirt.engine.ui.common.CommonApplicationResources;
import org.ovirt.engine.ui.common.presenter.popup.numa.NumaVmSelectedEvent;
import org.ovirt.engine.ui.common.presenter.popup.numa.UpdatedVnumaEvent;
import org.ovirt.engine.ui.common.widget.MenuBar;
import org.ovirt.engine.ui.common.widget.PopupPanel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.numa.VNodeModel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.dom.client.DragStartEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DraggableVirtualNumaPanel extends Composite implements HasHandlers {

    interface WidgetUiBinder extends UiBinder<Widget, DraggableVirtualNumaPanel> {
        WidgetUiBinder uiBinder = GWT.create(WidgetUiBinder.class);
    }

    interface Style extends CssResource {
        String panelOver();
    }

    static final PopupPanel menuPopup = new PopupPanel(true);

    @UiField
    FocusPanel container;

    @UiField(provided=true)
    VirtualNumaPanel numaPanel;

    @UiField
    Image dragHandle;

    @UiField
    Style style;

    private VNodeModel nodeModel;

    private final EventBus eventBus;

    private final CommonApplicationConstants commonConstants;
    private final CommonApplicationMessages commonMessages;

    private HandlerRegistration contextMenuHandlerRegistration;
    private MenuBar menuBar;

    private boolean dragEnabled;

    @Inject
    public DraggableVirtualNumaPanel(EventBus gwtEventBus, CommonApplicationResources commonResources,
            CommonApplicationMessages messages, CommonApplicationConstants commonConstants,
            VirtualNumaPanel virtualNumaPanel) {
        numaPanel = virtualNumaPanel;
        initWidget(WidgetUiBinder.uiBinder.createAndBindUi(this));
        initializeResouceIcons(commonResources);
        this.eventBus = gwtEventBus;
        this.commonConstants = commonConstants;
        this.commonMessages = messages;
        enableDrag(true);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!dragEnabled) {
            return;
        }
        contextMenuHandlerRegistration = numaPanel.addDomHandler(new ContextMenuHandler() {
            @Override
            public void onContextMenu(ContextMenuEvent event) {
                NativeEvent nativeEvent = event.getNativeEvent();
                showContextMenu(nativeEvent.getClientX(), nativeEvent.getClientY());
                event.stopPropagation();
                event.preventDefault();
            }
        }, ContextMenuEvent.getType());
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (!dragEnabled) {
            return;
        }
        contextMenuHandlerRegistration.removeHandler();
    }

    private void initializeResouceIcons(CommonApplicationResources resources) {
        numaPanel.setPinnedPartialVNumaIcon(resources.pinnedPartialVNumaIcon());
        numaPanel.setPinnedVNumaIcon(resources.pinnedVNumaIcon());
        numaPanel.setPartialVNumaIcon(resources.partialVNumaIcon());
        numaPanel.setvNumaIcon(resources.vNumaIcon());
    }

    @UiHandler("container")
    void handleMouseOver(MouseOverEvent event) {
        if (!dragEnabled) {
            return;
        }
        container.addStyleName(style.panelOver());
        dragHandle.setVisible(true);
    }

    @UiHandler("container")
    void handleMouseOut(MouseOutEvent event) {
        if (!dragEnabled) {
            return;
        }
        container.removeStyleName(style.panelOver());
        dragHandle.setVisible(false);
    }

    @UiHandler("container")
    void onDragStart(DragStartEvent event) {
        if (!dragEnabled) {
            return;
        }
        event.setData("VM_GID", nodeModel.getVm().getId().toString()); //$NON-NLS-1$
        event.setData("PINNED", String.valueOf(nodeModel.isPinned())); //$NON-NLS-1$
        event.setData("INDEX", String.valueOf(nodeModel.getVmNumaNode().getIndex())); //$NON-NLS-1$
        // show a ghost of the widget under cursor.
        NativeEvent nativeEvent = event.getNativeEvent();
        int x = nativeEvent.getClientX() - getAbsoluteLeft();
        int y = nativeEvent.getClientY() - getAbsoluteTop();
        event.getDataTransfer().setDragImage(getElement(), x, y);
    }

    @UiHandler("container")
    void onFocus(FocusEvent event) {
        NumaVmSelectedEvent.fire(this, this.nodeModel);
    }

    protected void showContextMenu(int clientX, int clientY) {
        menuPopup.setWidget(menuBar);
        menuPopup.setPopupPosition(clientX, clientY);
        menuPopup.show();
    }

    /**
     * Dragging is enabled by default, if you want to alter that behavior, call this method with false.
     *
     * @param dragEnabled
     *            Enabled or disable dragging of this panel.
     */
    public void enableDrag(boolean dragEnabled) {
        this.dragEnabled = dragEnabled;
        if (dragEnabled) {
            getElement().setDraggable(Element.DRAGGABLE_TRUE);
        } else {
            getElement().setDraggable(Element.DRAGGABLE_FALSE);
        }
    }

    @Override
    public void setStyleName(String className) {
        container.setStyleName(className);
    }

    public void setModel(VNodeModel nodeModel, List<VdsNumaNode> numaNodeList) {
        numaPanel.setModel(nodeModel);
        this.nodeModel = nodeModel;
        if (nodeModel.isLocked()) {
            enableDrag(false);
        } else {
            createMenu(numaNodeList, nodeModel.getVmNumaNode().getIndex());
        }
    }

    private void createMenu(final List<VdsNumaNode> numaNodeList, int indexToSkip) {
        menuBar = new MenuBar(true);
        for (final VdsNumaNode numaNode : numaNodeList) {
            final int nodeIndex = numaNode.getIndex();
            menuBar.addItem(commonMessages.numaNode(nodeIndex), new Command() {

                @Override
                public void execute() {
                    UpdatedVnumaEvent.fire(DraggableVirtualNumaPanel.this, nodeModel.getVm().getId(),
                            nodeModel.isPinned(), nodeModel.getVmNumaNode().getIndex(), nodeIndex);
                    menuPopup.hide();
                }
            });
        }
        if (nodeModel.isPinned()) {
            menuBar.addSeparator();
            menuBar.addItem(commonConstants.unPinNode(), new Command() {

                @Override
                public void execute() {
                    UpdatedVnumaEvent.fire(DraggableVirtualNumaPanel.this, nodeModel.getVm().getId(),
                            nodeModel.isPinned(), nodeModel.getVmNumaNode().getIndex(), -1);
                    menuPopup.hide();
                }
            });
        }
    }

    @Override
    public void fireEvent(GwtEvent<?> event) {
        eventBus.fireEvent(event);
    }
}
