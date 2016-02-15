package org.ovirt.engine.api.restapi.resource;

import org.ovirt.engine.api.model.Label;
import org.ovirt.engine.api.model.Labels;
import org.ovirt.engine.api.resource.LabelResource;
import org.ovirt.engine.core.common.businessentities.network.pseudo.NetworkLabel;

public class BackendHostNicLabelResource extends AbstractBackendSubResource<Label, NetworkLabel> implements LabelResource {

    private String id;
    private BackendHostNicLabelsResource parent;

    protected BackendHostNicLabelResource(String id, BackendHostNicLabelsResource parent) {
        super("", Label.class, NetworkLabel.class);
        this.id = id;
        this.parent = parent;
    }

    public BackendHostNicLabelsResource getParent() {
        return parent;
    }

    @Override
    public Label get() {
        Labels labels = parent.list();
        if (labels != null) {
            for (Label label : labels.getLabels()) {
                if (label.getId().equals(id)) {
                    parent.addParents(label);
                    return addLinks(label);
                }
            }
        }

        return notFound();
    }

    @Override
    protected Label doPopulate(Label model, NetworkLabel entity) {
        return model;
    }
}
