package org.ovirt.engine.ui.uicommonweb.models.gluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeSnapshotConfig;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;

public class GlusterClusterSnapshotConfigModel extends Model {
    private EntityModel<String> dataCenter;
    private ListModel<VDSGroup> clusters;
    private ListModel<EntityModel<GlusterVolumeSnapshotConfig>> clusterConfigOptions;
    private Map<String, String> existingClusterConfigs = new HashMap<String, String>();

    public EntityModel<String> getDataCenter() {
        return this.dataCenter;
    }

    public void setDataCenter(EntityModel<String> dataCenter) {
        this.dataCenter = dataCenter;
    }

    public ListModel<VDSGroup> getClusters() {
        return this.clusters;
    }

    public void setClusters(ListModel<VDSGroup> clusters) {
        this.clusters = clusters;
    }

    public ListModel<EntityModel<GlusterVolumeSnapshotConfig>> getClusterConfigOptions() {
        return clusterConfigOptions;
    }

    public void setClusterConfigOptions(ListModel<EntityModel<GlusterVolumeSnapshotConfig>> clusterConfigOptions) {
        this.clusterConfigOptions = clusterConfigOptions;
    }

    public String getExistingClusterConfigValue(String cfgName) {
        return existingClusterConfigs.get(cfgName);
    }

    public GlusterClusterSnapshotConfigModel() {
        init();
    }

    private void init() {
        setDataCenter(new EntityModel<String>());
        setClusters(new ListModel<VDSGroup>());
        getClusters().getSelectedItemChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                clusterSelectedItemChanged();
            }
        });
        setClusterConfigOptions(new ListModel<EntityModel<GlusterVolumeSnapshotConfig>>());
    }

    public boolean validate() {
        boolean isValid = true;
        setMessage(null);
        Iterable<EntityModel<GlusterVolumeSnapshotConfig>> items1 = getClusterConfigOptions().getItems();
        for (EntityModel<GlusterVolumeSnapshotConfig> model : items1) {
            GlusterVolumeSnapshotConfig option = model.getEntity();
            if (option.getParamValue().trim().length() == 0) {
                setMessage(ConstantsManager.getInstance()
                        .getMessages()
                        .clusterSnapshotOptionValueEmpty(option.getParamName()));
                isValid = false;
                break;
            }
        }

        return isValid;
    }

    private void clusterSelectedItemChanged() {
        VDSGroup selectedCluster = getClusters().getSelectedItem();
        if (selectedCluster == null) {
            return;
        }

        AsyncDataProvider.getInstance().getGlusterSnapshotConfig(new AsyncQuery(this, new INewAsyncCallback() {

            @Override
            public void onSuccess(Object model, Object returnValue) {
                VdcQueryReturnValue vdcValue = (VdcQueryReturnValue) returnValue;
                Pair<List<GlusterVolumeSnapshotConfig>, List<GlusterVolumeSnapshotConfig>> configs =
                        (Pair<List<GlusterVolumeSnapshotConfig>, List<GlusterVolumeSnapshotConfig>>) vdcValue.getReturnValue();
                if (configs != null) {
                    List<GlusterVolumeSnapshotConfig> clusterConfigOptions = configs.getFirst();
                    Collections.sort(clusterConfigOptions, new Linq.GlusterVolumeSnapshotConfigComparator());
                    setModelItems(getClusterConfigOptions(), clusterConfigOptions, existingClusterConfigs);
                } else {
                    getClusterConfigOptions().setItems(null);
                }
            }

            private void setModelItems(ListModel<EntityModel<GlusterVolumeSnapshotConfig>> listModel,
                    List<GlusterVolumeSnapshotConfig> cfgs, Map<String, String> fetchedCfgsBackup) {
                List<EntityModel<GlusterVolumeSnapshotConfig>> coll =
                        new ArrayList<EntityModel<GlusterVolumeSnapshotConfig>>();
                for (GlusterVolumeSnapshotConfig cfg : cfgs) {
                    EntityModel<GlusterVolumeSnapshotConfig> cfgModel = new EntityModel<GlusterVolumeSnapshotConfig>();
                    cfgModel.setEntity(cfg);
                    fetchedCfgsBackup.put(cfg.getParamName(), cfg.getParamValue());
                    coll.add(cfgModel);
                }

                // set the entity items
                listModel.setItems(coll);
            }
        }),
                selectedCluster.getId(),
                null);
    }
}
