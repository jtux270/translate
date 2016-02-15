package org.ovirt.engine.api.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.ovirt.engine.api.model.DetailedLink;
import org.ovirt.engine.api.model.Link;
import org.ovirt.engine.api.model.Parameter;
import org.ovirt.engine.api.model.ParametersSet;
import org.ovirt.engine.api.utils.LinkHelper.LinkFlags;

public class ApiRootLinksCreator {

    public static List<String> getAllRels(String baseUri) {
        List<String> rels = new ArrayList<String>();
        for (Link link : getLinks(baseUri)) {
            rels.add(link.getRel());
        }
        return rels;
    }

    public static List<String> getGlusterRels(String baseUri) {
        List<String> rels = new ArrayList<String>();
        for (Link link : getGlusterLinks(baseUri)) {
            rels.add(link.getRel());
        }
        return rels;
    }

    public static Collection<DetailedLink> getLinks(String baseUri) {
        Collection<DetailedLink> links = new LinkedList<DetailedLink>();
        links.add(createLink("capabilities", baseUri));
        links.add(createLink("clusters", LinkFlags.SEARCHABLE, baseUri));
        links.add(createLink("datacenters", LinkFlags.SEARCHABLE, baseUri));
        links.add(createLink("events", LinkFlags.SEARCHABLE, getEventParams(), baseUri));
        links.add(createLink("hosts", LinkFlags.SEARCHABLE, baseUri));
        links.add(createLink("networks", LinkFlags.SEARCHABLE, baseUri));
        links.add(createLink("roles", baseUri));
        links.add(createLink("storagedomains", LinkFlags.SEARCHABLE, baseUri));
        links.add(createLink("tags", baseUri));
        links.add(createLink("bookmarks", baseUri));
        links.add(createLink("templates", LinkFlags.SEARCHABLE, baseUri));
        links.add(createLink("instancetypes", LinkFlags.SEARCHABLE, baseUri));
        links.add(createLink("users", LinkFlags.SEARCHABLE, baseUri));
        links.add(createLink("groups", LinkFlags.SEARCHABLE, baseUri));
        links.add(createLink("domains", baseUri));
        links.add(createLink("vmpools", LinkFlags.SEARCHABLE, baseUri));
        links.add(createLink("vms", LinkFlags.SEARCHABLE, baseUri));
        links.add(createLink("disks", LinkFlags.SEARCHABLE, baseUri));
        links.add(createLink("jobs", baseUri));
        links.add(createLink("storageconnections", baseUri));
        links.add(createLink("vnicprofiles", baseUri));
        links.add(createLink("diskprofiles", baseUri));
        links.add(createLink("cpuprofiles", baseUri));
        links.add(createLink("schedulingpolicyunits", baseUri));
        links.add(createLink("schedulingpolicies", baseUri));
        links.add(createLink("permissions", baseUri));
        return links;
    }

    public static Collection<DetailedLink> getGlusterLinks(String baseUri) {
        Collection<DetailedLink> links = new LinkedList<DetailedLink>();
        links.add(createLink("capabilities", baseUri));
        links.add(createLink("clusters", LinkFlags.SEARCHABLE, baseUri));
        links.add(createLink("events", LinkFlags.SEARCHABLE, getEventParams(), baseUri));
        links.add(createLink("hosts", LinkFlags.SEARCHABLE, baseUri));
        links.add(createLink("networks", LinkFlags.SEARCHABLE, baseUri));
        links.add(createLink("roles", baseUri));
        links.add(createLink("tags", baseUri));
        links.add(createLink("users", LinkFlags.SEARCHABLE, baseUri));
        links.add(createLink("groups", LinkFlags.SEARCHABLE, baseUri));
        links.add(createLink("domains", baseUri));
        return links;
    }

    private static DetailedLink createLink(String rel, LinkFlags flags, String baseUri) {
        return LinkHelper.createLink(baseUri, rel, flags);
    }

    private static DetailedLink createLink(String rel, LinkFlags flags, ParametersSet params, String baseUri) {
        return LinkHelper.createLink(baseUri, rel, flags, params);
    }

    private static DetailedLink createLink(String rel, String baseUri) {
        return LinkHelper.createLink(baseUri, rel, LinkFlags.NONE);
    }

    private static ParametersSet getEventParams() {
        ParametersSet ps = new ParametersSet();
        Parameter param = new Parameter();
        param.setName("from");
        param.setValue("event_id");
        ps.getParameters().add(param);
        return ps;
    }

}
