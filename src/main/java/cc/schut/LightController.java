package cc.schut;

import cc.schut.json.Light;
import cc.schut.json.Lights;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.Map;

@RestController
public class LightController {
    private static final Logger log = LoggerFactory.getLogger(LightController.class);

    @Autowired
    private HueService hue;

    @Value("${server.port}")
    private String port;

    // not sure this is actually used.
    @RequestMapping("/crossdomain.xml")
    public String crossDomain() {
        return "<cross-domain-policy>\n" +
                " <allow-access-from domain=\"*\" to-ports=\""+port+"\"/>\n" +
                " </cross-domain-policy>"+(byte)0;
    }

    @RequestMapping("/poll")
    @Cacheable(CacheConfig.CACHE_CONF) // scratch is polling 30x per sec. that's way to many rest calls to the bridge. so caching.
    public String poll() throws URISyntaxException {
        String response = "";
        Lights lights = hue.getLights();
        for(Map.Entry<String, Light> light: lights.allLights().entrySet()) {
            response += "light/"+light.getKey()+" "+(light.getValue().getState().getOn()?"on":"off")+"\n";
            response += "bri/"+light.getKey()+" "+light.getValue().getState().getBri()+"\n";
        }
        return response;
    }

    @RequestMapping("/light/{id}/{switch}")
    public String switchLight(@PathVariable("id") int id, @PathVariable("switch") String sw) throws URISyntaxException {
        boolean state = sw.equalsIgnoreCase("on")?true:false;
        hue.switchLight(id, state);
        return "light: "+id+" "+state;
    }

    @RequestMapping("/bri/{id}/{bri}")
    public String brightnessLight(@PathVariable("id") int id, @PathVariable("bri") int bri) throws URISyntaxException {
        hue.brightnessLight(id, bri);
        return "light: "+id+" "+bri;
    }

    @RequestMapping("/reset_all")
    public void resetAll() {
    }

}
