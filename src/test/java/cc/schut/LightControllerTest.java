package cc.schut;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;

public class LightControllerTest {
    private static final Logger log = LoggerFactory.getLogger(LightController.class);

    //@Test
    public void discoverHueBridgeN_UPNP() throws URISyntaxException {
        String bridge = HueService.discoverHueBridgeN_UPNP();
        log.info(bridge);
        Assert.assertTrue(bridge.length()>0);
    }

    //@Test
    public void discoverHueBridgeUPNP() throws URISyntaxException {
        String bridge = HueService.discoverHueBridgeUPNP();
        log.info(bridge);
        Assert.assertTrue(bridge.length()>0);
    }
}
