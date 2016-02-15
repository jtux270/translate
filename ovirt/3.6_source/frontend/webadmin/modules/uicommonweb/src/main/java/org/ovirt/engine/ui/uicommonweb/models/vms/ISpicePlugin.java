package org.ovirt.engine.ui.uicommonweb.models.vms;

import org.ovirt.engine.core.compat.Version;

public interface ISpicePlugin extends ISpice {

    boolean detectBrowserPlugin();
    void setPluginVersion(Version value);
    void setSpiceBaseURL(String spiceBaseURL);

}
