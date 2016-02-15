package org.ovirt.engine.core.common.businessentities;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;

/**
 * Object which represents vm virtual NUMA node information
 *
 */
public class VmNumaNode extends VdsNumaNode {

    private static final long serialVersionUID = -5384287037435972730L;

    private List<Pair<Guid, Pair<Boolean, Integer>>> vdsNumaNodeList;

    public VmNumaNode() {
        setVdsNumaNodeList(new ArrayList<Pair<Guid, Pair<Boolean, Integer>>>());
    }

    /**
     * @return vNUMA node pair list; first is the pNUMA node uuid,
     *         second is a pair(boolean for pinned or not (TRUE=pinned), integer for pNUMA node index)
     */
    public List<Pair<Guid, Pair<Boolean, Integer>>> getVdsNumaNodeList() {
        return vdsNumaNodeList;
    }

    public void setVdsNumaNodeList(List<Pair<Guid, Pair<Boolean, Integer>>> vdsNumaNodeList) {
        this.vdsNumaNodeList = vdsNumaNodeList;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((vdsNumaNodeList == null) ? 0 : vdsNumaNodeList.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        VmNumaNode other = (VmNumaNode) obj;
        if (vdsNumaNodeList == null) {
            if (other.vdsNumaNodeList != null)
                return false;
        } else if (!vdsNumaNodeList.equals(other.vdsNumaNodeList))
            return false;
        return true;
    }

}
