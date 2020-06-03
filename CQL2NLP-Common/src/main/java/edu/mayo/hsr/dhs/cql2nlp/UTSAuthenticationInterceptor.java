package edu.mayo.hsr.dhs.cql2nlp;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Supports UTS Authentication via Ticket-Granting/Single-Use Service Ticket Scheme
 */
public class UTSAuthenticationInterceptor implements ClientHttpRequestInterceptor {

    // UTS TGT tickets expire every 8 hours
    // Add 1-hour buffer to be safe (albeit not really necessary...) but expiry period is so long it doesn't matter anyways
    private static final long TGT_VALID_LIMIT_MILLIS = 7 * 60 * 60 * 1000;

    private static final MultiValueMap<String, String> UMLS_SINGLE_USE_SERVICE_TICKET_REQ_BODY = new LinkedMultiValueMap<>();

    private static final Pattern TGT_MATCH_PATTERN = Pattern.compile("https[^\"]+");

    private final MultiValueMap<String, String> tgtReqBody;
    private final RestTemplate tgtRestTemplate = new RestTemplate();
    private String currTgt = null;
    private long tgtRefreshTime = -1;

    static {
        UMLS_SINGLE_USE_SERVICE_TICKET_REQ_BODY.put("service", Collections.singletonList("http://umlsks.nlm.nih.gov"));
    }

    /**
     * Initializes using a UTS username and Password for authentication.
     *
     * @param service The Service to Authenticate against, e.g. https://utslogin.nlm.nih.gov/cas/v1/tickets
     * @param utsUser A UTS Account Username
     * @param utsPass A UTS Password
     */
    public UTSAuthenticationInterceptor(String service, String utsUser, String utsPass) {
        this.tgtReqBody = new LinkedMultiValueMap<>();
        this.tgtReqBody.put("username", Collections.singletonList(utsUser));
        this.tgtReqBody.put("password", Collections.singletonList(utsPass));
        this.tgtRestTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(service));
        this.tgtRestTemplate.getMessageConverters().add(new org.springframework.http.converter.FormHttpMessageConverter());

    }

    /**
     * Initializes using a UTS API Key for authentication.
     *
     * @param service   The Service to Authenticate against, e.g. https://utslogin.nlm.nih.gov/cas/v1/tickets
     * @param utsApiKey A UTS API Key associated with a UTS account
     */
    public UTSAuthenticationInterceptor(String service, String utsApiKey) {
        this.tgtRestTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(service));
        this.tgtRestTemplate.getMessageConverters().add(new org.springframework.http.converter.FormHttpMessageConverter());
        this.tgtReqBody = new LinkedMultiValueMap<>();
        this.tgtReqBody.put("apikey", Collections.singletonList(utsApiKey));
    }

    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        // Check if TGT is valid, if not refresh
        if (currTgt == null || tgtRefreshTime + TGT_VALID_LIMIT_MILLIS < System.currentTimeMillis()) {
            // Refresh TGT
            try {
                this.currTgt = "/" + this.tgtRestTemplate.postForObject("/", new HttpEntity<>(this.tgtReqBody, headers), String.class);
                Matcher m = TGT_MATCH_PATTERN.matcher(this.currTgt);
                if (m.find()) {
                    // Used to handle UTS v1 auth endpoint only, others such as VSAC don't need to use this matcher
                    this.currTgt = m.group();
                    this.currTgt = this.currTgt.split("/")[this.currTgt.split("/").length - 1];
                }
                this.tgtRefreshTime = System.currentTimeMillis();
            } catch (Throwable t) {
                throw new IOException("Failed to authenticate against UTS TGT Service", t);
            }
        }
        if (this.currTgt.length() < 9) { // Blank TGT
            throw new IOException("Failed to authenticate against UTS TGT Service, Double Check Credentials");
        }
        // Now retrieve a single-use service ticket
        String serviceTicket;
        try {
            serviceTicket = this.tgtRestTemplate.postForObject("/" + this.currTgt + "/", new HttpEntity<>(UMLS_SINGLE_USE_SERVICE_TICKET_REQ_BODY, headers), String.class);
        } catch (Throwable t) {
            throw new IOException("Failed to retrieve Single-Use Service Ticket from UTS", t);
        }
        URI uri = UriComponentsBuilder.fromUri(request.getURI()).queryParam("ticket", serviceTicket).build().toUri();

        // HTTPRequest is Immutable, so we need to use a Wrapper Class
        HttpRequestWrapper wrapper = new HttpRequestWrapper(request) {
            @Override
            public URI getURI() {
                return uri;
            }
        };
        return execution.execute(wrapper, body);
    }
}
