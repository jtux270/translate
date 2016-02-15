package org.ovirt.engine.ui.webadmin.section.main.view.tab.template;

import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.ui.common.uicommon.model.DetailModelProvider;
import org.ovirt.engine.ui.common.view.AbstractSubTabFormView;
import org.ovirt.engine.ui.common.widget.uicommon.template.TemplateGeneralModelForm;
import org.ovirt.engine.ui.uicommonweb.models.templates.TemplateGeneralModel;
import org.ovirt.engine.ui.uicommonweb.models.templates.TemplateListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.template.SubTabTemplateGeneralPresenter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class SubTabTemplateGeneralView extends AbstractSubTabFormView<VmTemplate, TemplateListModel, TemplateGeneralModel> implements SubTabTemplateGeneralPresenter.ViewDef, Editor<TemplateGeneralModel> {

    interface ViewUiBinder extends UiBinder<Widget, SubTabTemplateGeneralView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField(provided = true)
    TemplateGeneralModelForm form;

    @Inject
    public SubTabTemplateGeneralView(DetailModelProvider<TemplateListModel, TemplateGeneralModel> modelProvider, ApplicationConstants constants) {
        super(modelProvider);
        this.form = new TemplateGeneralModelForm(modelProvider, constants);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
    }

    @Override
    public void setMainTabSelectedItem(VmTemplate selectedItem) {
        form.update();
    }

}
