package org.ovirt.engine.ui.userportal.gin;

import org.ovirt.engine.ui.common.gin.BaseUtilsModule;
import org.ovirt.engine.ui.userportal.section.login.presenter.ConnectAutomaticallyProvider;
import org.ovirt.engine.ui.userportal.utils.ConnectAutomaticallyManager;
import org.ovirt.engine.ui.userportal.widget.basic.MainTabBasicListItemMessagesTranslator;

import com.google.inject.Singleton;

public class UtilsModule extends BaseUtilsModule {

    @Override
    protected void configure() {
        super.configure();
        bind(MainTabBasicListItemMessagesTranslator.class).in(Singleton.class);
        bind(ConnectAutomaticallyProvider.class).in(Singleton.class);
        bind(ConnectAutomaticallyManager.class).in(Singleton.class);
    }

}
