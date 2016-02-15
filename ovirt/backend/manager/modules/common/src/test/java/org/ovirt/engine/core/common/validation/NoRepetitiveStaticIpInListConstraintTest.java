package org.ovirt.engine.core.common.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.network.NetworkBootProtocol;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.utils.ValidationUtils;
import org.ovirt.engine.core.common.validation.annotation.NoRepetitiveStaticIpInList;

public class NoRepetitiveStaticIpInListConstraintTest {

    private static final String IP_1 = "10.10.10.10";
    private static final String IP_2 = "11.11.11.11";

    private Validator validator;

    @Before
    public void initValidator() {
        validator = ValidationUtils.getValidator();
    }

    @Test
    public void twoNetworkInterfacesWithSameIp() {
        List<VdsNetworkInterface> listOfInterfaces = new ArrayList<VdsNetworkInterface>();
        listOfInterfaces.add(createVdsNetworkInterfaceWithStaticIp(IP_1));
        listOfInterfaces.add(createVdsNetworkInterfaceWithStaticIp(IP_1));
        validateAndAssertResult(listOfInterfaces, false);
    }

    @Test
    public void twoNetworkInterfacesWithDifferentIp() {
        List<VdsNetworkInterface> listOfInterfaces = new ArrayList<VdsNetworkInterface>();
        listOfInterfaces.add(createVdsNetworkInterfaceWithStaticIp(IP_1));
        listOfInterfaces.add(createVdsNetworkInterfaceWithStaticIp(IP_2));
        validateAndAssertResult(listOfInterfaces, true);
    }

    @Test
    public void twoNetworkInterfacesWithEmptyIp() {
        List<VdsNetworkInterface> listOfInterfaces = new ArrayList<VdsNetworkInterface>();
        listOfInterfaces.add(createVdsNetworkInterfaceWithStaticIp(""));
        listOfInterfaces.add(createVdsNetworkInterfaceWithStaticIp(""));
        validateAndAssertResult(listOfInterfaces, true);
    }

    @Test
    public void twoNetworkInterfacesWithNullIp() {
        List<VdsNetworkInterface> listOfInterfaces = new ArrayList<VdsNetworkInterface>();
        listOfInterfaces.add(createVdsNetworkInterfaceWithStaticIp(null));
        listOfInterfaces.add(createVdsNetworkInterfaceWithStaticIp(null));
        validateAndAssertResult(listOfInterfaces, true);
    }

    private void validateAndAssertResult(List<VdsNetworkInterface> listOfInterfaces, boolean isValid) {
        NoRepetitiveStaticIpInListContainer container = new NoRepetitiveStaticIpInListContainer(listOfInterfaces);
        Set<ConstraintViolation<NoRepetitiveStaticIpInListContainer>> validate = validator.validate(container);
        Assert.assertEquals(isValid, validate.isEmpty());
    }

    private VdsNetworkInterface createVdsNetworkInterfaceWithStaticIp(String ip) {
        VdsNetworkInterface networkInterface = new VdsNetworkInterface();
        networkInterface.setAddress(ip);
        networkInterface.setBootProtocol(NetworkBootProtocol.STATIC_IP);
        return networkInterface;
    }

    private class NoRepetitiveStaticIpInListContainer {
        @SuppressWarnings("unused")
        @NoRepetitiveStaticIpInList
        private List<VdsNetworkInterface> value;

        public NoRepetitiveStaticIpInListContainer(List<VdsNetworkInterface> value) {
            this.value = value;
        }
    }
}
