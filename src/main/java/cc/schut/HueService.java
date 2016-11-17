package cc.schut;

import cc.schut.json.AuthResponse;
import cc.schut.json.Lights;
import cc.schut.json.NupnpResponse;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
@EnableRetry
public class HueService  implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(HueService.class);

    @Autowired
    private HueSettings hue;

    public void afterPropertiesSet() {
        serviceDiscovery();
    }

    public Lights getLights() throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();
        URI uri = new URI(hue.getBridge() + "api/"+ hue.getUserid() +"/lights/");

        ResponseEntity<Lights> lightResponse =
                restTemplate.exchange(uri,
                        HttpMethod.GET, null, new ParameterizedTypeReference<Lights>() {
                        });
        return lightResponse.getBody();
    }

    public void switchLight(int id, boolean on) throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();
        URI uri = new URI(hue.getBridge() + "api/"+ hue.getUserid() +"/lights/"+id+"/state");
        restTemplate.put(uri, "{\"on\":" + on + "}");
    }

    public void brightnessLight(int id, int bri) throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();
        URI uri = new URI(hue.getBridge() + "api/"+ hue.getUserid() +"/lights/"+id+"/state");
        restTemplate.put(uri, "{\"bri\":" + bri + "}");
    }

    /**
     * Discover the hue bridge, generate a userid and save them to a properties file.
     */
    @Retryable(maxAttempts=500, backoff=@Backoff(delay=100, maxDelay=500))
    public void serviceDiscovery() {
        if (hue.getBridge().length()==0 || hue.getUserid().length()==0) {
            try {
                if (hue.getBridge().length()==0) {
                    hue.setBridge(HueService.discoverHueBridgeUPNP());
                    if (hue.getBridge().length()==0) {
                        // didn't manage to discover the bridge try another method.
                        hue.setBridge(HueService.discoverHueBridgeN_UPNP());
                    }
                }
                if (hue.getUserid().length()==0 && hue.getBridge().length()>0)
                    hue.setUserid(HueService.discoverUserId(hue.getBridge()));
            } catch (URISyntaxException e) {
                log.error("URI incorrect when doing service discovery: ", e);
            }

            Properties props = new Properties();
            props.setProperty("hue.bridge", hue.getBridge());
            props.setProperty("hue.userid", hue.getUserid());
            saveParamChanges(props);
        }
        if (hue.getUserid().length()==0 || hue.getBridge().length()==0) {
            log.error("hue.bridge or hue.userid property isn't set!, add it to a application.properties file.");
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

    public static String discoverUserId(String hueBridge) throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();
        URI uri = new URI(hueBridge + "api");
        try {
            HttpEntity<String> request = new HttpEntity<>(new String("{\"devicetype\":\"hue#scratch\"}"));
            ResponseEntity<List<AuthResponse>> bridgeResponse =
                    restTemplate.exchange(uri,
                            HttpMethod.POST, request, new ParameterizedTypeReference<List<AuthResponse>>() {
                            });
            List<AuthResponse> responses = bridgeResponse.getBody();
            for(AuthResponse response: responses) {
                if (response.getSuccess() != null) {
                    return response.getSuccess().getUsername();
                } else {
                    log.info(response.getError().getDescription());
                    throw new RuntimeException(response.getError().getDescription());
                }
            }
            throw new RuntimeException("No proper response found!");
        } catch (RestClientException e) {
            log.info(e.getLocalizedMessage());
            throw e;
        }
    }

    /**
     * Discover the bridge using the uPNP protocol.
     * @return
     */
    public static String discoverHueBridgeUPNP() {
        GatewayDiscover discover = new GatewayDiscover(new String[]{"urn:schemas-upnp-org:device:Basic:1", "upnp:rootdevice"});
        discover.setTimeout(5000);
        log.info("Looking for Gateway Devices");
        try {
            discover.discover();
            Map<InetAddress, GatewayDevice> gateways = discover.getAllGateways();
            for (Map.Entry<InetAddress, GatewayDevice> gateway: gateways.entrySet()) {
                String model = gateway.getValue().getModelName();
                if (model.contains("Philips hue")) {
                    String url = gateway.getValue().getURLBase();
                    log.info("Found: " + url);
                    return url;
                }
            }
        } catch (IOException |SAXException |ParserConfigurationException e) {
            log.error("Error while searching for the bridge device", e);
        }
        return "";
    }

    /**
     * Discover the bridge using n-upnp. Effectively asking meethue if it knows the internal ip of your bridge.
     * This should be the 2nd thing you try. Regular upnp should be tried first.
     * @return
     */
    public static String discoverHueBridgeN_UPNP() throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();
        URI uri = new URI("https://www.meethue.com/api/nupnp");
        try {
            ResponseEntity<NupnpResponse[]> nupnpResponse =
                    restTemplate.exchange(uri,
                            HttpMethod.GET, null, new ParameterizedTypeReference<NupnpResponse[]>() {
                            });
            NupnpResponse[] responses = nupnpResponse.getBody();
            for(NupnpResponse response: responses) {
                String ip = response.getInternalipaddress();
                if (ip!=null && ip.length()>0)
                    return "http://"+ip+":80/";
            }
        } catch (RestClientException e) {
            log.info(e.getLocalizedMessage());
            throw e;
        }

        return "";
    }
}
