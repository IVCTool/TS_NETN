package org.nato.netn.base;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URL;

import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import de.fraunhofer.iosb.tc_lib.TcInconclusive;

public class NetnBaseTcParamTest {

    protected static String tcParamJson = "{ \"p1\" : \"1.0003\", \"fomFiles\":  [\"RPR-Base_v2.0.xml\", \"NETN-BASE.xml\"] }";

    @Test
    void testGetUrls() {
        try {
            NetnBaseTcParam param = new NetnBaseTcParam(tcParamJson);
            URL[] urls = param.getUrls();
            assertTrue(urls.length > 0);
        } catch (TcInconclusive | ParseException e) {
            fail(e);
        }

    }
}
