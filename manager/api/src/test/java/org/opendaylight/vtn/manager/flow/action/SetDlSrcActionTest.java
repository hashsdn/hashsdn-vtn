/*
 * Copyright (c) 2014 NEC Corporation
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.vtn.manager.flow.action;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.io.ByteArrayInputStream;

import javax.xml.bind.JAXB;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.codehaus.jettison.json.JSONObject;

import org.opendaylight.vtn.manager.TestBase;

import org.opendaylight.controller.sal.action.SetDlSrc;
import org.opendaylight.controller.sal.packet.address.EthernetAddress;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;

/**
 * JUnit test for {@link SetDlSrcAction}.
 */
public class SetDlSrcActionTest extends TestBase {
    /**
     * Test case for getter methods.
     */
    @Test
    public void testGetter() {
        for (EthernetAddress eaddr: createEthernetAddresses()) {
            byte[] bytes = (eaddr == null) ? null : eaddr.getValue();
            SetDlSrcAction act = new SetDlSrcAction(bytes);
            byte[] addr = act.getAddress();
            assertTrue(Arrays.equals(bytes, addr));
            assertEquals(null, act.getValidationStatus());

            if (bytes != null) {
                SetDlSrc sact = new SetDlSrc(bytes);
                act = new SetDlSrcAction(sact);
                assertTrue(Arrays.equals(bytes, act.getAddress()));
                assertEquals(null, act.getValidationStatus());
            }
        }
    }

    /**
     * Test case for {@link SetDlSrcAction#equals(Object)} and
     * {@link SetDlSrcAction#hashCode()}.
     */
    @Test
    public void testEquals() {
        HashSet<Object> set = new HashSet<Object>();
        List<EthernetAddress> eaddrs = createEthernetAddresses();
        for (EthernetAddress eaddr: eaddrs) {
            byte[] bytes1 = (eaddr == null) ? null : eaddr.getValue();
            byte[] bytes2 = (bytes1 == null) ? null : bytes1.clone();
            SetDlSrcAction act1 = new SetDlSrcAction(bytes1);
            SetDlSrcAction act2 = new SetDlSrcAction(bytes2);
            testEquals(set, act1, act2);
        }

        assertEquals(eaddrs.size(), set.size());
    }

    /**
     * Test case for {@link SetDlSrcAction#toString()}.
     */
    @Test
    public void testToString() {
        String prefix = "SetDlSrcAction[";
        String suffix = "]";
        for (EthernetAddress eaddr: createEthernetAddresses()) {
            byte[] bytes = (eaddr == null) ? null : eaddr.getValue();
            SetDlSrcAction act = new SetDlSrcAction(bytes);
            byte[] addr = act.getAddress();
            String a = (bytes == null)
                ? null
                : "addr=" + HexEncode.bytesToHexStringFormat(bytes);
            String required = joinStrings(prefix, suffix, ",", a);
            assertEquals(required, act.toString());
        }
    }

    /**
     * Ensure that {@link SetDlSrcAction} is serializable.
     */
    @Test
    public void testSerialize() {
        for (EthernetAddress eaddr: createEthernetAddresses()) {
            byte[] bytes = (eaddr == null) ? null : eaddr.getValue();
            SetDlSrcAction act = new SetDlSrcAction(bytes);
            serializeTest(act);
        }
    }

    /**
     * Ensure that {@link SetDlSrcAction} is mapped to XML root element.
     */
    @Test
    public void testJAXB() {
        for (EthernetAddress eaddr: createEthernetAddresses()) {
            byte[] bytes = (eaddr == null) ? null : eaddr.getValue();
            SetDlSrcAction act = new SetDlSrcAction(bytes);
            SetDlSrcAction newobj = (SetDlSrcAction)jaxbTest(act, "setdlsrc");
            assertEquals(null, newobj.getValidationStatus());
        }

        // Specifying invalid MAC address.
        String xml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<setdlsrc address=\"invalid_address\" />";
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes());
            SetDlSrcAction act = JAXB.unmarshal(in, SetDlSrcAction.class);
            Status st = act.getValidationStatus();
            assertEquals(StatusCode.BADREQUEST, st.getCode());
        } catch (Exception e) {
            unexpected(e);
        }
    }

    /**
     * Ensure that {@link SetDlSrcAction} is mapped to JSON object.
     */
    @Test
    public void testJSON() {
        for (EthernetAddress eaddr: createEthernetAddresses()) {
            byte[] bytes = (eaddr == null) ? null : eaddr.getValue();
            SetDlSrcAction act = new SetDlSrcAction(bytes);
            jsonTest(act);
        }

        // Specifying invalid MAC address.
        ObjectMapper mapper = getJsonObjectMapper();
        try {
            JSONObject json = new JSONObject();
            json.put("address", "invalid_address");
            SetDlSrcAction act =
                mapper.readValue(json.toString(), SetDlSrcAction.class);
            Status st = act.getValidationStatus();
            assertEquals(StatusCode.BADREQUEST, st.getCode());
        } catch (Exception e) {
            unexpected(e);
        }
    }
}