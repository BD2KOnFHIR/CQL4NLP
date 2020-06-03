package edu.mayo.hsr.dhs.cql2nlp;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import edu.mayo.hsr.dhs.cql2nlp.structs.CodifiedValueSetElement;
import edu.mayo.hsr.dhs.cql2nlp.structs.VSACCodeSystem;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.*;

/**
 * Retrieves Value Sets as supplied to CQL from the NLM Value Set Authority Center and Renders them in as a FHIR R4 ValueSet
 */
public class ValueSetResolver {

    private final RestTemplate restTemplate;
    private final IParser parser;

    public ValueSetResolver(String utsAcct, String utsPass) {
        this.restTemplate = new RestTemplate();
        this.restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory("https://cts.nlm.nih.gov/fhir/ValueSet"));
        this.restTemplate.setInterceptors(Collections.singletonList(new BasicAuthenticationInterceptor(utsAcct, utsPass)));
        this.parser = FhirContext.forR4().newJsonParser();
    }

    /**
     * Gets a FHIR ValueSet for an OID
     *
     * @param oid A VSAC OID, e.g. 2.16.840.1.113762.1.4.1223.9 for COVID-19
     * @return A FHIR ValueSet object. Note that the compose component can include links to other valuesets
     */
    public ValueSet getValueSetForOID(String oid) {
        String serialized = this.restTemplate.getForEntity("/" + oid, String.class).getBody();
        return this.parser.parseResource(ValueSet.class, serialized);
    }

    /**
     * Gets a set of all display names contained within this ValueSet
     *
     * @param valueSet The value set in question
     * @return A set of unique display names
     */
    public Set<String> resolveValueSetDisplayNames(ValueSet valueSet) {
        Set<String> ret = new HashSet<>();
        Set<String> visitedOIDs = new HashSet<>();
        resolveValueSetDisplayNamesRecurs(valueSet, ret, visitedOIDs);
        return ret;
    }

    /**
     * Gets a set of all ontological codes contained within this ValueSet
     *
     * @param valueSet The value set in question
     * @return A set of unique display names
     */
    public Set<CodifiedValueSetElement> resolveValueSetCodes(ValueSet valueSet) {
        Set<CodifiedValueSetElement> ret = new HashSet<>();
        Set<String> visitedOIDs = new HashSet<>();
        resolveValueSetCodesRecurs(valueSet, ret, visitedOIDs);
        return ret;
    }

    /*
     * Utility function to visit and construct display names recursively
     */
    private void resolveValueSetDisplayNamesRecurs(ValueSet curr, Set<String> ret, Set<String> visitedOIDs) {
        if (curr.hasId()) {
            if (visitedOIDs.contains(curr.getId())) {
                return;
            }
            visitedOIDs.add(curr.getId());
        } else {
            // Short circuit here to make sure cyclic dependencies with no IDs don't happen
            return; // TODO warning message?
        }
        ValueSet.ValueSetComposeComponent compose = curr.getCompose();
        compose.getInclude().forEach(include -> {
            if (include.hasConcept()) {
                include.getConcept().forEach(concept -> {
                    if (concept.hasDisplay()) {
                        ret.add(concept.getDisplay());
                    }
                });
            }
            if (include.hasValueSet()) {
                include.getValueSet().forEach(includedValueSet -> {
                    if (includedValueSet.hasValue()) {
                        HashSet<String> child = new HashSet<>();
                        String url = includedValueSet.getValue();
                        String oid = url.split("/")[url.split("/").length - 1];
                        resolveValueSetDisplayNamesRecurs(getValueSetForOID(oid), child, visitedOIDs);
                        ret.addAll(child);
                    }
                });
            }
        });
        compose.getExclude().forEach(exclude -> {
            if (exclude.hasConcept()) {
                exclude.getConcept().forEach(concept -> {
                    if (concept.hasDisplay()) {
                        ret.remove(concept.getDisplay());
                    }
                });
            }
            if (exclude.hasValueSet()) {
                exclude.getValueSet().forEach(excludedValueSet -> {
                    if (excludedValueSet.hasValue()) {
                        HashSet<String> child = new HashSet<>();
                        String url = excludedValueSet.getValue();
                        String oid = url.split("/")[url.split("/").length - 1];
                        resolveValueSetDisplayNamesRecurs(getValueSetForOID(oid), child, visitedOIDs);
                        ret.removeAll(child);
                    }
                });
            }
        });
    }


    /*
     * Utility function to visit and construct codes recursively
     */
    private void resolveValueSetCodesRecurs(ValueSet curr, Set<CodifiedValueSetElement> ret, Set<String> visitedOIDs) {
        if (curr.hasId()) {
            if (visitedOIDs.contains(curr.getId())) {
                return;
            }
            visitedOIDs.add(curr.getId());
        } else {
            // Short circuit here to make sure cyclic dependencies with no IDs don't happen
            return; // TODO warning message?
        }
        ValueSet.ValueSetComposeComponent compose = curr.getCompose();
        compose.getInclude().forEach(include -> {
            if (include.hasConcept()) {

                String codeSystem = include.hasSystem() ? include.getSystemElement().getValue() : "UNSPECIFIED ONTOLOGY";
                include.getConcept().forEach(concept -> {
                    if (concept.hasCode()) {
                        ret.add(new CodifiedValueSetElement(VSACCodeSystem.ofUrl(codeSystem), concept.getCode()));
                    }
                });
            }
            if (include.hasValueSet()) {
                include.getValueSet().forEach(includedValueSet -> {
                    if (includedValueSet.hasValue()) {
                        HashSet<CodifiedValueSetElement> child = new HashSet<>();
                        String url = includedValueSet.getValue();
                        String oid = url.split("/")[url.split("/").length - 1];
                        resolveValueSetCodesRecurs(getValueSetForOID(oid), child, visitedOIDs);
                        ret.addAll(child);
                    }
                });
            }
        });
        compose.getExclude().forEach(exclude -> {
            String codeSystem = exclude.hasSystem() ? exclude.getSystemElement().getValue() : "UNSPECIFIED ONTOLOGY";

            if (exclude.hasConcept()) {
                exclude.getConcept().forEach(concept -> {
                    if (concept.hasCode()) {
                        ret.remove(new CodifiedValueSetElement(VSACCodeSystem.ofUrl(codeSystem), concept.getCode()));
                    }
                });
            }
            if (exclude.hasValueSet()) {
                exclude.getValueSet().forEach(excludedValueSet -> {
                    if (excludedValueSet.hasValue()) {
                        HashSet<CodifiedValueSetElement> child = new HashSet<>();
                        String url = excludedValueSet.getValue();
                        String oid = url.split("/")[url.split("/").length - 1];
                        resolveValueSetCodesRecurs(getValueSetForOID(oid), child, visitedOIDs);
                        ret.remove(child);
                    }
                });
            }
        });
    }

}
