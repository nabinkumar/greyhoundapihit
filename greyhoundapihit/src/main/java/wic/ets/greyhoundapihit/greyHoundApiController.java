package wic.ets.greyhoundapihit;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


@RestController
public class greyHoundApiController {
    private static final Logger LOGGER = LoggerFactory.getLogger(greyHoundApiController.class);

    RestTemplate restTemplate = new RestTemplate();

    @RequestMapping(method = RequestMethod.POST)
    public String worklogSync(@RequestBody String json,@RequestHeader String agency) throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {
        LOGGER.info("body = "+json);
        LOGGER.info("agency = " + agency);
        String testReturn = getLocationDetails(agency,json);
        return testReturn;
    }

    private String getLocationDetails(String credentialType, String json) throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {
        HttpEntity<String> request =  null;
        try {
            request = new HttpEntity<String>(json, generateAuthentication(HttpMethod.POST,credentialType,null));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        ResponseEntity<postReturnModel> worklogsResponse = restTemplate.exchange("https://gtg.tdstickets.com:38080/ws/rest/trip/search?ap=OASIS",
                                                            HttpMethod.POST,request,postReturnModel.class);
        LOGGER.info("worklogsResponse.getBody  = " + worklogsResponse.getBody());
        String[] output = worklogsResponse.getBody().getLocation().split("/");
        String hashCode = removeLast(output[output.length -1],9);
        LOGGER.info("out put " +output[output.length -1]);
        request = new HttpEntity(generateAuthentication(HttpMethod.GET,credentialType,hashCode));

        ResponseEntity<String> getResponse = restTemplate.exchange(worklogsResponse.getBody().getLocation(),HttpMethod.GET,request,String.class);
       return getResponse.getBody();
    }

      public HttpHeaders generateAuthentication(HttpMethod method,String credentialType,String hashcode) throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeyException {
        String secret = "ZqtwuGnPphYmuWeNsGEHzUkNn3FXCj"; // This is the private key that is issued to the customer
        String username = "GTG4082GLIB";
        String key = "qX7CQa6MYQJTz6g";
        if (credentialType.equalsIgnoreCase("ca")) {
            /*
            Username: GTG1267GLCB
            Password: kyCGs98MynWNgYp
            Key: Er3t7bt9XWbLVJVrxezyuq5tj75R8w

             */
            secret = "Er3t7bt9XWbLVJVrxezyuq5tj75R8w"; // This is the private key that is issued to the customer
            username = "GTG1267GLCB";
            key = "kyCGs98MynWNgYp";
        }
        String path = "/ws/rest/trip/search";
        if(method.matches("GET")) {
            path = path + "/" + hashcode;
        }

        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssx")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now());
        timestamp = timestamp + ":00";
        String authorization = key + "^" + path + "^" + method + "^" + timestamp + "^";
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
        mac.init(keySpec);
        byte[] signature = mac.doFinal(authorization.getBytes("UTF-8"));
        String hash = Base64.encodeBase64URLSafeString(signature);
        hash = username + ":" + hash;
      //  MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
        HttpHeaders headers = new HttpHeaders();

        headers.add("authentication", hash);
        headers.add("x-gtg-timestamp", timestamp);
        headers.add("Content-type", "Application/json");
        LOGGER.info("hash = " + hash);
        LOGGER.info("timestamp =" + timestamp);
        return headers;

    }
    public String removeLast(String s, int n) throws StringIndexOutOfBoundsException{

        int strLength = s.length();

        if(n>strLength){
            throw new StringIndexOutOfBoundsException("Number of character to remove from end is greater than the length of the string");
        }

        else if(null!=s && !s.isEmpty()){

            s = s.substring(0, s.length()-n);
        }

        return s;

    }

}
