package org.ovirt.engine.ui.common;

import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;

public interface CommonApplicationTemplates extends SafeHtmlTemplates {

    public final static int TAB_BAR_HEIGHT = 24;

    @Template("<span><span style='position: relative; display: inline-block; vertical-align: top; height: 14px; line-height: 14px;'>{0}</span>"
            + "<span style='position: relative; margin-left: 3px; margin-right: 3px; white-space: nowrap; height: 14px; line-height: 14px;'>{1}</span></span>")
    SafeHtml imageTextButton(SafeHtml image, String text);

    @Template("<span><span style='position: relative; vertical-align: middle;'>{0}</span>" +
            "<span style='position: relative; margin-left: 3px; white-space: nowrap;'>{1}</span></span>")
    SafeHtml textImageButton(String text, SafeHtml image);

    @Template("<span><span style='position: relative; height: 22px; vertical-align: bottom; display: table-cell;'>{0}</span>"
            +
            "<span style='position: relative; padding-left: 3px; vertical-align: middle; display: table-cell;'>{1}</span></span>")
    SafeHtml dualImage(SafeHtml image1, SafeHtml image2);

    @Template("<span><span style='position: relative; height: 22px; vertical-align: bottom; display: table-cell;'>{0}</span>"
            +
            "<span style='position: relative; padding-left: 3px; vertical-align: middle; display: table-cell; width: 19px;'>{1}</span>"
            +
            "<span style='position: relative; padding-left: 3px; vertical-align: middle; display: table-cell;'>{2}</span></span>")
    SafeHtml tripleImage(SafeHtml image1, SafeHtml image2, SafeHtml image3);

    @Template("<span style='width: 18px; vertical-align: middle; text-align: center; display: table-cell;' title='{1}'>{0}</span>")
    SafeHtml imageWithTitle(SafeHtml image, String title);

    @Template("<span style='height:22px; width: 22px; vertical-align: middle; text-align: center;' title='{1}'>{0}</span>")
    SafeHtml inlineImageWithTitle(SafeHtml image, String title);

    @Template("<table cellspacing='0' cellpadding='0'><tr>" +
            "<td style='background: url({2});width:2px;'></td>" +
            "<td style='text-align:center;'>" +
            "<div class='db_bg_image {5} {6}' style='background: url({3}) repeat-x; height: 20px;'>" +
            "<span style='vertical-align: middle; line-height: 20px;' class=\"db_image_container\">{0}</span><div class=\"db_text\">{1}</div></div>" +
            "</td>" +
            "<td style='background: url({4});width:2px;'></td>" +
            "</tr></table>")
    SafeHtml dialogButton(SafeHtml image, String text, String start, String stretch,
            String end, String contentStyleName, String customContentStyleName);

    @Template("<ul style='margin-top:0'>{0}</ul>")
    SafeHtml unsignedList(SafeHtml list);

    @Template("<li>{0}</li>")
    SafeHtml listItem(SafeHtml item);

    @Template("{0} <sub>{1}</sub>")
    SafeHtml sub(String main, String sub);

    @Template("<b><font style='{0}'>{1}</font></b>")
    SafeHtml snapshotDescription(String style, String description);

    @Template("<span><span style='position: relative; display: inline-block; vertical-align: top; height: 14px; line-height: 14px;'>{0}</span>"
            + "<span style='position: relative; white-space: nowrap; height: 14px; line-height: 14px;'>{1}</span></span>")
    SafeHtml imageTextCardStatus(SafeHtml image, String text);

    @Template("Card Status: {0}")
    SafeHtml cardStatus(String status);

    @Template("Link State: {0}")
    SafeHtml linkState(String state);

    @Template("<i>{0}</i>")
    SafeHtml italicText(String text);

    @Template("<table style='min-width: 200px; width: 100%; border-bottom: 1px solid #acacac;'><tr>" +
            "<td style='width: 49%;'>{0}</td>" +
            "<td style='width: 2%; border-left: 1px solid #acacac;'></td>" +
            "<td style='white-space: normal; width: 49%; color: #acacac;'>{1}</td>" +
            "</tr></table>")
    SafeHtml typeAheadNameDescription(String name, String description);

    @Template("<table style='min-width: 200px; width: 100%; border-bottom: 1px solid #acacac;'><tr>" +
            "<td>&nbsp</td>" +
            "</tr></table>")
    SafeHtml typeAheadEmptyContent();

    @Template("<div style='width: {0}; font-style: italic;'>{1}</div>")
    SafeHtml italicFixedWidth(String pxWidth, String text);

    @Template("<span>{0} {1}</span>")
    SafeHtml iconWithText(SafeHtml icon, String text);

    @Template("<span title='{2}'>{0} {1}</span>")
    SafeHtml iconWithTextAndTitle(SafeHtml icon, String text, String title);

    @Template("<span title='{1}'>{0}</span>")
    SafeHtml textAndTitle(String text, String title);

    @Template("<div style='border-right: 1px solid #D7D7E1; height: 32px;'>{0}</div>")
    SafeHtml nonResizeableColumnHeader(SafeHtml text);
}
