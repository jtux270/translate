package org.ovirt.engine.ui.common.widget.uicommon.popup.quota;

import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.idhandler.HasElementId;
import org.ovirt.engine.ui.common.widget.AbstractValidatedWidgetWithLabel;
import org.ovirt.engine.ui.common.widget.HasEditorDriver;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelLabelEditor;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.uicommonweb.models.quota.ChangeQuotaItemModel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class ChangeQuotaItemView extends Composite implements HasEditorDriver<ChangeQuotaItemModel>, HasElementId {

    interface Driver extends SimpleBeanEditorDriver<ChangeQuotaItemModel, ChangeQuotaItemView> {
    }

    interface ViewUiBinder extends UiBinder<Widget, ChangeQuotaItemView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface WidgetStyle extends CssResource {
        String editorContent();

        String editorWrapper();

        String editorLabel();
    }

    @UiField
    WidgetStyle style;

    @UiField
    @Ignore
    StringEntityModelLabelEditor objectNameLabel;

    @UiField
    @Ignore
    StringEntityModelLabelEditor storageDomainNameLabel;

    @UiField
    @Ignore
    StringEntityModelLabelEditor currentQuotaLabel;

    @UiField(provided = true)
    @Path(value = "quota.selectedItem")
    ListModelListBoxEditor<Quota> quotaListEditor;

    private final Driver driver = GWT.create(Driver.class);

    private final CommonApplicationConstants constants;

    public ChangeQuotaItemView(CommonApplicationConstants constants) {
        this.constants = constants;

        initEditors();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        driver.initialize(this);
        updateStyles();
    }

    void initEditors() {
        quotaListEditor = new ListModelListBoxEditor<Quota>(new NullSafeRenderer<Quota>() {
            @Override
            public String renderNullSafe(Quota quota) {
                return quota.getQuotaName();
            }
        });
    }

    void updateStyles() {
        String editorStyle = style.editorContent();

        updateEditorStyle(objectNameLabel, editorStyle);
        updateEditorStyle(storageDomainNameLabel, editorStyle);
        updateEditorStyle(currentQuotaLabel, editorStyle);
        updateEditorStyle(quotaListEditor, editorStyle);
    }

    private void updateEditorStyle(AbstractValidatedWidgetWithLabel editor, String contentStyle) {
        editor.setContentWidgetStyleName(contentStyle);
        editor.addWrapperStyleName(style.editorWrapper());
        editor.setLabelStyleName(style.editorLabel());
    }

    @Override
    public void edit(final ChangeQuotaItemModel object) {
        driver.edit(object);

        objectNameLabel.asValueBox().setValue(object.getObject().getEntity());
        storageDomainNameLabel.asValueBox().setValue(object.getStorageDomainName());
        currentQuotaLabel.asValueBox().setValue(object.getCurrentQuota().getEntity());
    }

    @Override
    public ChangeQuotaItemModel flush() {
        return driver.flush();
    }

    @Override
    public void setElementId(String elementId) {
    }

}
