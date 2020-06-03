package edu.mayo.hsr.dhs.cql2nlp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mayo.hsr.dhs.cql2nlp.structs.CodifiedValueSetElement;
import edu.mayo.hsr.dhs.cql2nlp.structs.UMLSSourceVocabulary;
import edu.mayo.hsr.dhs.cql2nlp.structs.VSACCodeSystem;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.*;

/**
 * Handles usage of the UMLS metathesaurus REST service
 */
public class UMLSBrowser {

    private final ThreadLocal<RestTemplate> utsRest;

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
                String resp = null;
                try {
                    resp = this.utsRest.get().getForObject("/content/current/CUI/" + code.getCode() + "/relations", String.class);
                } catch (Throwable t) {
                    continue;
                }
                JsonNode json = new ObjectMapper().readTree(resp);
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
