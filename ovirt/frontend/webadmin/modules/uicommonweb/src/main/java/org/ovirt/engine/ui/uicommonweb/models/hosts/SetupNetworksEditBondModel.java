package org.ovirt.engine.ui.uicommonweb.models.hosts;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public class SetupNetworksEditBondModel extends SetupNetworksBondModel {

    public SetupNetworksEditBondModel(final VdsNetworkInterface bond,
            Collection<String> suggestedLabels,
            Map<String, String> labelToIface) {

        setTitle(ConstantsManager.getInstance()
                .getMessages()
                .editBondInterfaceTitle(bond.getName()));

        // bond name
        getBond().setIsChangable(false);
        List<String> bondName = Arrays.asList(bond.getName());
        getBond().setItems(bondName);
        getBond().setSelectedItem(bond.getName());

        // bond options
        String bondOptions = bond.getBondOptions();
        List<Map.Entry<String, EntityModel<String>>> items = (List<Map.Entry<String, EntityModel<String>>>) getBondingOptions().getItems();
        boolean found = false;
        Map.Entry<String, EntityModel<String>> customItem = null;
        for (Map.Entry<String, EntityModel<String>> pair : items) {
            String key = pair.getKey();
            if (key.equals(bondOptions)) {
                getBondingOptions().setSelectedItem(pair);
                found = true;
                break;
            } else if ("custom".equals(key)) { //$NON-NLS-1$
                customItem = pair;
            }
        }
        if (!found) {
            EntityModel<String> value = new EntityModel<String>();
            value.setEntity(bondOptions);
            customItem.setValue(value);
            getBondingOptions().setSelectedItem(customItem);
        }

        setLabelsModel(new NicLabelModel(Collections.singletonList(bond), suggestedLabels, labelToIface));
    }

}
