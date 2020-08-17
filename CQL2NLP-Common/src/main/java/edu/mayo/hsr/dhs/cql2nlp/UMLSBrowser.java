package edu.mayo.hsr.dhs.cql2nlp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.mayo.hsr.dhs.cql2nlp.structs.CodifiedValueSetElement;
import edu.mayo.hsr.dhs.cql2nlp.structs.VSACCodeSystem;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Handles usage of the UMLS metathesaurus REST service
 */
public class UMLSBrowser {

    private final ThreadLocal<RestTemplate> utsRest;
    private final ThreadLocal<ObjectMapper> om = ThreadLocal.withInitial(ObjectMapper::new);
    private final LoadingCache<CodifiedValueSetElement, JsonNode> cuiRlnCache =
            CacheBuilder
                    .newBuilder()
                    .maximumSize(10000)
                    .expireAfterAccess(5, TimeUnit.MINUTES)
                    .build(new CacheLoader<CodifiedValueSetElement, JsonNode>() {
                        @Override
                        public JsonNode load(CodifiedValueSetElement code) throws Exception {
                            try {
                                return om.get().readTree(utsRest.get().getForObject("/content/current/CUI/" + code.getCode() + "/relations", String.class));
                            } catch (Throwable t) {
                                return JsonNodeFactory.instance.objectNode();
                            }
                        }
                    });
    private final LoadingCache<String, Set<String>> displayNameCache =
            CacheBuilder
                    .newBuilder()
                    .maximumSize(10000)
                    .expireAfterAccess(5, TimeUnit.MINUTES)
                    .build(new CacheLoader<String, Set<String>>() {
                        @Override
                        public Set<String> load(String cui) throws Exception {
                            try {
                                return resolveDisplayNamesForCUI(cui);
                            } catch (Throwable t) {
                                return Collections.emptySet();
                            }
                        }
                    });

    public UMLSBrowser(String utsAcct, String utsPass) {
        this.utsRest = ThreadLocal.withInitial(() -> {
            RestTemplate ret = new RestTemplate();
            ret.setUriTemplateHandler(new DefaultUriBuilderFactory("https://uts-ws.nlm.nih.gov/rest"));
            ret.setInterceptors(
                    Collections.singletonList(new UTSAuthenticationInterceptor("https://utslogin.nlm.nih.gov/cas/v1/tickets", utsAcct, utsPass))
            );
            // TODO this may require further investigation as not sure if interceptor is thread safe
            return ret;
        });
    }


    public Set<CodifiedValueSetElement> getCUIsForValueSetElement(CodifiedValueSetElement element, boolean traverseHierarchy) throws JsonProcessingException {
        Set<CodifiedValueSetElement> ret = new HashSet<>();
        getCUIsForValueSetElementRecurs(element, traverseHierarchy, ret);
        return ret;
    }

    private void getCUIsForValueSetElementRecurs(CodifiedValueSetElement element, boolean traverseHierarchy, Set<CodifiedValueSetElement> ret) throws JsonProcessingException {
        Set<CodifiedValueSetElement> newCUIsThisIteration = new HashSet<>();
        Map<String, Object> params = new HashMap<>();
        if (!element.getCodeSystem().equals(VSACCodeSystem.UMLS)) {
            params.put("sabs", element.getCodeSystem().getUmlsMapping().name());
            params.put("inputType", "sourceUi");
            params.put("string", element.getCode());
            params.put("returnIdType", "concept");
            params.put("searchType", "exact");
            String resp = this.utsRest.get().getForObject("/search/current?sabs={sabs}&inputType={inputType}&string={string}&returnIdType={returnIdType}&searchType={searchType}", String.class, params);
            JsonNode json = new ObjectMapper().readTree(resp);
            if (json.has("result") && json.get("result").has("results")) {
                JsonNode resultArray = json.get("result").get("results");
                for (JsonNode child : resultArray) {
                    String ui = child.get("ui").asText();
                    if (ui.equalsIgnoreCase("NONE")) {
                        return;
                    } else {
                        if (ret.add(new CodifiedValueSetElement(VSACCodeSystem.UMLS, ui))) {
                            newCUIsThisIteration.add(new CodifiedValueSetElement(VSACCodeSystem.UMLS, ui));
                        }
                    }
                }
            }
        } else {
            if (ret.add(element)) {
                newCUIsThisIteration.add(element);
            }
        }

        // If traverse hierarchy (comprehensive analysis), find synonyms and children CUIs as well for items that have
        // not yet been visited (newCUIsThisIteration/not in ret already)
        if (traverseHierarchy) {
            for (CodifiedValueSetElement code : newCUIsThisIteration) {
                JsonNode json;
                try {
                    json = cuiRlnCache.get(code);
                } catch (ExecutionException e) {
                    continue;
                }
                if (json.has("result")) {
                    for (JsonNode result : json.get("result")) {
                        String relnType = result.get("relationLabel").asText();
                        if (relnType.equalsIgnoreCase("RB")) {
                            String cuiUrl = result.get("relatedId").asText();
                            String cui = cuiUrl.split("/")[cuiUrl.split("/").length - 1];
                            CodifiedValueSetElement subCode = new CodifiedValueSetElement(VSACCodeSystem.UMLS, cui);
                            // Check if we already visited this relation, if not then proceed
                            if (!ret.contains(subCode)) {
                                getCUIsForValueSetElementRecurs(subCode, true, ret);
                            }
                        }
                    }
                }
            }
        }
    }

    public Set<String> getDisplayNamesForCUI(String cui) throws JsonProcessingException {
        try {
            return displayNameCache.get(cui);
        } catch (Throwable e) {
            return Collections.emptySet();
        }
    }

    public Set<String> resolveDisplayNamesForCUI(String cui) throws JsonProcessingException {
        Set<String> ret = new HashSet<>();
        ObjectMapper om = new ObjectMapper();
        String uriTemplate = "/content/current/CUI/" + cui + "/atoms?language={lang}&pageNumber={page}";
        Map<String, String> uriVars = new HashMap<>();
        uriVars.put("lang", "ENG");
        int pageNum = 1;
        uriVars.put("page", pageNum + "");
        boolean hasResults = true;
        while (hasResults) {
            String resp = this.utsRest.get().getForObject(uriTemplate, String.class, uriVars);
            JsonNode json = om.readTree(resp).get("result");
            if (json.size() == 0) {
                hasResults = false;
                break;
            }
            for (JsonNode result : json) {
                if (result.get("ui").asText().equalsIgnoreCase("NONE")) {
                    hasResults = false;
                    break;
                } else {
                    ret.add(result.get("name").asText().toLowerCase());
                }
            }
            pageNum++;
            uriVars.put("page", pageNum + "");
        }
        return ret;
    }
}
