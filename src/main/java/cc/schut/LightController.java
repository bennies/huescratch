package cc.schut;

import cc.schut.json.AuthResponse;
import cc.schut.json.Lights;
import cc.schut.json.Light;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@RestController
@EnableRetry
public class LightController {
    private static final Logger log = LoggerFactory.getLogger(LightController.class);

    @Value("${hue.bridge}")
    private String hueBridge;

    @Value("${hue.userid}")
    private String userId;

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
    public String poll() throws URISyntaxException {
        String response = "";
        RestTemplate restTemplate = new RestTemplate();
        URI uri = new URI(hueBridge + "api/"+ userId +"/lights/");

        ResponseEntity<Lights> lightResponse =
                restTemplate.exchange(uri,
                        HttpMethod.GET, null, new ParameterizedTypeReference<Lights>() {
                        });
        Lights lights = lightResponse.getBody();

        for(Map.Entry<String, Light> light: lights.allLights().entrySet()) {
            response += "light/"+light.getKey()+" "+(light.getValue().getState().getOn()?"on":"off")+"\n";
            response += "bri/"+light.getKey()+" "+light.getValue().getState().getBri()+"\n";
        }
        return response;
    }

    @RequestMapping("/light/{id}/{switch}")
    public String switchLight(@PathVariable("id") int id, @PathVariable("switch") String sw) throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();
        URI uri = new URI(hueBridge + "api/"+ userId +"/lights/"+id+"/state");
        String state = sw.equalsIgnoreCase("on")?"true":"false";
        restTemplate.put(uri, "{\"on\":" + state + "}");
        return "light: "+id+" "+state;
    }

    @RequestMapping("/bri/{id}/{bri}")
    public String brightnessLight(@PathVariable("id") int id, @PathVariable("bri") int bri) throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();
        URI uri = new URI(hueBridge + "api/"+ userId +"/lights/"+id+"/state");
        restTemplate.put(uri, "{\"bri\":" + bri + "}");
        return "light: "+id+" "+bri;
    }

    @RequestMapping("/reset_all")
    @Retryable(maxAttempts=500, backoff=@Backoff(delay=100, maxDelay=500))
    public void resetAll() throws URISyntaxException {
        if (hueBridge.length()==0) {
            // @TODO look at autodiscovery for the bridge.
            log.error("hue.bridge property isn't set!, add it to a application.properties file.");
            System.exit(-1);
        } else if (userId.length()==0) {
            serviceDiscovery();
        }
    }

    /**
     * Write some code to discover the hue bridge and generate a userid.
     */
    public void serviceDiscovery() throws URISyntaxException {
        userId = discoverUserId();

        Properties props = new Properties();
        props.setProperty("hue.userid", userId);
        props.setProperty("hue.bridge", hueBridge);
        saveParamChanges(props);
    }

    public String discoverUserId() throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();
        URI uri = new URI(hueBridge + "api");
        try {
            HttpEntity<String> request = new HttpEntity<>(new String("{\"devicetype\":\"hue#scratch\"}"));
            ResponseEntity<List<AuthResponse>> rateResponse =
                    restTemplate.exchange(uri,
                            HttpMethod.POST, request, new ParameterizedTypeReference<List<AuthResponse>>() {
                            });
            List<AuthResponse> response = rateResponse.getBody();
            if (response.get(0).getSuccess()!=null) {
                return response.get(0).getSuccess().getUsername();
            } else {
                log.info(response.get(0).getError().getDescription());
                throw new RuntimeException(response.get(0).getError().getDescription());
            }
        } catch (RestClientException e) {
            log.info(e.getLocalizedMessage());
            throw e;
        }
    }
    /**
     * You probably don't want to keep redoing service discovery so persist the changes.
     */
    public static void saveParamChanges(Properties props) {
        try {
            File f = new File("application.properties");
            OutputStream out = new FileOutputStream( f );
            DefaultPropertiesPersister p = new DefaultPropertiesPersister();
            p.store(props, out, "Hue scratch settings.");
        } catch (Exception e ) {
            log.error("Problem while writing the properties file.", e);
        }
    }
}
