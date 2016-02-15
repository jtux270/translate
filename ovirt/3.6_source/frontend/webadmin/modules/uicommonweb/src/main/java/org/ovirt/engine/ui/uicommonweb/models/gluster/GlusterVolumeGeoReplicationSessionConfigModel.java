package org.ovirt.engine.ui.uicommonweb.models.gluster;

import java.util.LinkedHashMap;
import java.util.List;

import org.ovirt.engine.core.common.action.gluster.GlusterVolumeGeoRepSessionConfigParameters;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterGeoRepSession;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterGeoRepSessionConfiguration;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;

public class GlusterVolumeGeoReplicationSessionConfigModel extends Model {

    private ListModel<EntityModel<Pair<Boolean, GlusterGeoRepSessionConfiguration>>> configsModel;

    private LinkedHashMap<String, String> configsMap;

    private GlusterGeoRepSession geoRepSession;

    private UICommand updateConfigsCommand;
    private UICommand cancelCommand;

    public GlusterVolumeGeoReplicationSessionConfigModel(GlusterGeoRepSession selectedGeoRepSession) {
        configsModel = new ListModel<EntityModel<Pair<Boolean, GlusterGeoRepSessionConfiguration>>>();
        configsMap = new LinkedHashMap<>();
        this.geoRepSession = selectedGeoRepSession;
    }

    public ListModel<EntityModel<Pair<Boolean, GlusterGeoRepSessionConfiguration>>> getConfigsModel() {
        return configsModel;
    }

    public void setConfigsModel(ListModel<EntityModel<Pair<Boolean, GlusterGeoRepSessionConfiguration>>> configsModel) {
        this.configsModel = configsModel;
    }

    public LinkedHashMap<String, String> getConfigs() {
        return configsMap;
    }

    public UICommand getUpdateConfigsCommand() {
        return updateConfigsCommand;
    }

    public void addUpdateConfigsCommand(UICommand setCommand) {
        this.updateConfigsCommand = setCommand;
        this.getCommands().add(setCommand);
    }

    @Override
    public UICommand getCancelCommand() {
        return cancelCommand;
    }

    public void addCancelCommand(UICommand cancelCommand) {
        this.cancelCommand = cancelCommand;
        this.getCommands().add(cancelCommand);
    }

    public void copyConfigsToMap(List<GlusterGeoRepSessionConfiguration> configsTocopy) {
        for (GlusterGeoRepSessionConfiguration currentConfig : configsTocopy) {
            configsMap.put(currentConfig.getKey(), currentConfig.getValue());
        }
    }

    public GlusterGeoRepSession getGeoRepSession() {
        return geoRepSession;
    }

    public void setGeoRepSession(GlusterGeoRepSession geoRepSession) {
        this.geoRepSession = geoRepSession;
    }

    public GlusterVolumeGeoRepSessionConfigParameters formGeoRepConfigParameters(GlusterGeoRepSessionConfiguration sessionConfig) {
        return new GlusterVolumeGeoRepSessionConfigParameters(getGeoRepSession().getMasterVolumeId(),
                getGeoRepSession().getId(),
                sessionConfig.getKey(),
                sessionConfig.getValue());
    }

    public void updateCommandExecutabilities(boolean isExecutionAllowed) {
        updateConfigsCommand.setIsExecutionAllowed(isExecutionAllowed);
    }
}
