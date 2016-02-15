package org.ovirt.engine.ui.common;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.resources.client.ImageResource;

public interface CommonApplicationResources extends ClientBundle {

    @Source("images/page_prev_enable.png")
    ImageResource pagePrevEnable();

    @Source("images/page_prev_disable.png")
    ImageResource pagePrevDisable();

    @Source("images/page_next_enable.png")
    ImageResource pageNextEnable();

    @Source("images/page_next_disable.png")
    ImageResource pageNextDisable();

    @Source("images/refresh.png")
    ImageResource refreshButtonImage();

    // Slider

    @Source("images/slider.gif")
    ImageResource slider();

    @Source("images/sliderDisabled.gif")
    ImageResource sliderDisabled();

    @Source("images/sliderSliding.gif")
    ImageResource sliderSliding();

    @NotStrict
    @Source("css/SliderBar.css")
    CssResource sliderBarCss();

    @Source("images/expander.png")
    ImageResource expanderImage();

    @Source("images/button_down.png")
    ImageResource expanderDownImage();

    // Dialogs

    @Source("css/DialogBox.css")
    CssResource dialogBoxStyle();

    @Source("images/button/button_start.png")
    ImageResource dialogButtonUpStart();

    @Source("images/button/button_stretch.png")
    ImageResource dialogButtonUpStretch();

    @Source("images/button/button_end.png")
    ImageResource dialogButtonUpEnd();

    @Source("images/button/button_Over_start.png")
    ImageResource dialogButtonOverStart();

    @Source("images/button/button_Over_stretch.png")
    ImageResource dialogButtonOverStretch();

    @Source("images/button/button_Over_end.png")
    ImageResource dialogButtonOverEnd();

    @Source("images/button/button_Down_start.png")
    ImageResource dialogButtonDownStart();

    @Source("images/button/button_Down_stretch.png")
    ImageResource dialogButtonDownStretch();

    @Source("images/button/button_Down_end.png")
    ImageResource dialogButtonDownEnd();

    @Source("images/button/button_Disabled_start.png")
    ImageResource dialogButtonUpDisabledStart();

    @Source("images/button/button_Disabled_stretch.png")
    ImageResource dialogButtonUpDisabledStretch();

    @Source("images/button/button_Disabled_end.png")
    ImageResource dialogButtonUpDisabledEnd();

    @Source("images/dialog/progress.gif")
    ImageResource dialogProgressImage();

    @Source("images/icon_help.png")
    ImageResource dialogIconHelp();

    @Source("images/icon_help_down.png")
    ImageResource dialogIconHelpDown();

    @Source("images/icon_help_rollover.png")
    ImageResource dialogIconHelpRollover();

    @Source("images/but_close.png")
    ImageResource dialogIconClose();

    @Source("images/but_close_down.png")
    ImageResource dialogIconCloseDown();

    @Source("images/but_close_over.png")
    ImageResource dialogIconCloseRollover();

    @Source("images/arrows/tri_down_normal.png")
    ImageResource arrowDownNormal();

    @Source("images/arrows/tri_down_click.png")
    ImageResource arrowDownClick();

    @Source("images/arrows/tri_down_over.png")
    ImageResource arrowDownOver();

    @Source("images/arrows/tri_down_disabled.png")
    ImageResource arrowDownDisabled();

    @Source("images/arrows/tri_up_normal.png")
    ImageResource arrowUpNormal();

    @Source("images/arrows/tri_up_click.png")
    ImageResource arrowUpClick();

    @Source("images/arrows/tri_up_over.png")
    ImageResource arrowUpOver();

    @Source("images/arrows/tri_up_disabled.png")
    ImageResource arrowUpDisabled();

    // Table image columns

    @Source("images/comment.png")
    ImageResource commentImage();

    @Source("images/log_normal.gif")
    ImageResource logNormalImage();

    @Source("images/log_warning.gif")
    ImageResource logWarningImage();

    @Source("images/log_error.gif")
    ImageResource logErrorImage();

    @Source("images/ok_small.gif")
    ImageResource okSmallImage();

    @Source("images/icon_alert_configure.png")
    ImageResource alertConfigureImage();

    @Source("images/up.gif")
    ImageResource upImage();

    @Source("images/up_disabled.gif")
    ImageResource upDisabledImage();

    @Source("images/down.gif")
    ImageResource downImage();

    @Source("images/down_disabled.gif")
    ImageResource downDisabledImage();

    @Source("images/admin.png")
    ImageResource adminImage();

    @Source("images/user.png")
    ImageResource userImage();

    @Source("images/volume_bricks_down_warning.png")
    ImageResource volumeBricksDownWarning();

    @Source("images/volume_all_bricks_down_warning.png")
    ImageResource volumeAllBricksDownWarning();

    // Model-bound widgets

    @Source("images/snapshot.png")
    ImageResource snapshotImage();

    @Source("images/storage.png")
    ImageResource storageImage();

    @Source("images/disk.png")
    ImageResource diskImage();

    @Source("images/nic.png")
    ImageResource nicImage();

    @Source("images/general.png")
    ImageResource generalImage();

    @Source("images/applications.png")
    ImageResource applicationsImage();

    @Source("images/wait.png")
    ImageResource waitImage();

    @Source("images/question_mark.png")
    ImageResource questionMarkImage();

    @Source("images/disk_shareable.png")
    ImageResource shareableDiskIcon();

    @Source("images/disk_readonly.png")
    ImageResource readOnlyDiskIcon();

    @Source("images/disk_lun.png")
    ImageResource externalDiskIcon();

    @Source("images/icon_increase.png")
    ImageResource increaseIcon();

    @Source("images/icon_decrease.png")
    ImageResource decreaseIcon();

    @Source("images/disk_bootable.png")
    ImageResource bootableDiskIcon();

    @Source("images/templates.png")
    ImageResource templatesImage();

    @Source("images/vms.png")
    ImageResource vmsImage();

    @Source("images/login.png")
    ImageResource loginIcon();

    @Source("images/login_disabled.png")
    ImageResource loginDisabledIcon();

    // Network
    @Source("images/network/icn_plugged.png")
    ImageResource pluggedNetworkImage();

    @Source("images/network/icn_un_plugged.png")
    ImageResource unpluggedNetworkImage();

    @Source("images/network/icn_network_linked.png")
    ImageResource linkedNetworkImage();

    @Source("images/network/icn_network_unlinked.png")
    ImageResource unlinkedNetworkImage();

    @Source("images/comboBoxDropDownIcon.png")
    ImageResource comboBoxDropDownIcon();

    @Source("images/left_scroll_arrow.png")
    ImageResource leftScrollArrow();

    @Source("images/left_scroll_arrow_disabled.png")
    ImageResource leftScrollArrowDisabled();

    @Source("images/right_scroll_arrow.png")
    ImageResource rightScrollArrow();

    @Source("images/right_scroll_arrow_disabled.png")
    ImageResource rightScrollArrowDisabled();

    @Source("images/dropdown_arrow.png")
    ImageResource dropdownArrow();

    @Source("images/triangle_down.gif")
    ImageResource triangle_down();

    @Source("images/memory_icon.png")
    ImageResource memorySmallIcon();

    @Source("images/disk_icon.png")
    ImageResource diskIcon();

    @Source("images/vm_conf_icon.png")
    ImageResource vmConfIcon();

    @Source("images/separator.gif")
    ImageResource separator();

    @Source("images/joined.png")
    ImageResource joinedIcon();

    @Source("images/separated.png")
    ImageResource separatedIcon();

    //Collapsible panel
    @Source("images/collapsed_header_icon.png")
    ImageResource collapsedHeaderArrow();

    @Source("images/expanded_header_icon.png")
    ImageResource expandedHeaderArrow();

    //Numa popup
    @Source("images/numa/vnuma_icon.png")
    ImageResource vNumaTitleIcon();

    @Source("images/numa/partial_vnuma_light_icon.png")
    ImageResource partialVNumaIcon();

    @Source("images/numa/pinned_partial_vnuma_light_icon.png")
    ImageResource pinnedPartialVNumaIcon();

    @Source("images/numa/vnuma_light_icon.png")
    ImageResource vNumaIcon();

    @Source("images/numa/pinned_vnuma_light_icon.png")
    ImageResource pinnedVNumaIcon();

    @Source("images/numa/drag_handle.png")
    ImageResource dragHandleIcon();

    @Source("images/numa/pinned_partial_vnuma_dark_icon.png")
    ImageResource darkPinnedPartialVNumaIcon();

    @Source("images/numa/pinned_vnuma_dark_icon.png")
    ImageResource darkPinnedVNumaIcon();

    @Source("images/numa/partial_vnuma_dark_icon.png")
    ImageResource darkPartialVNumaIcon();

    @Source("images/numa/vnuma_dark_icon.png")
    ImageResource darkVNumaIcon();
}
