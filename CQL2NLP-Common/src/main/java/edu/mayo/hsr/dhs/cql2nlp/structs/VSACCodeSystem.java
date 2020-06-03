package edu.mayo.hsr.dhs.cql2nlp.structs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public enum VSACCodeSystem {
    CPT(UMLSSourceVocabulary.CPT, "http://www.ama-assn.org/go/cpt"),
    ICD10CM(UMLSSourceVocabulary.ICD10CM, "http://hl7.org/fhir/sid/icd-10-cm"),
    ICD10PCS(UMLSSourceVocabulary.ICD10PCS, " http://www.icd10data.com/icd10pcs"),
    ICD9CM(UMLSSourceVocabulary.ICD9CM, "http://hl7.org/fhir/sid/icd-9-cm"),
    LOINC(UMLSSourceVocabulary.LNC, "http://loinc.org"),
    RXNORM(UMLSSourceVocabulary.RXNORM, "http://www.nlm.nih.gov/research/umls/rxnorm"),
    SNOMEDCT(UMLSSourceVocabulary.SNOMEDCT_US, "http://snomed.info/sct"),
    UMLS(UMLSSourceVocabulary.UMLS, "http://www.nlm.nih.gov/research/umls");

    private final UMLSSourceVocabulary umlsMapping;
    private final HashSet<String> urls;
    private static final Map<String, VSACCodeSystem> URL_MAPPINGS = new HashMap<>();

    static {
        for (VSACCodeSystem system : VSACCodeSystem.values()) {
            for (String url : system.urls) {
                URL_MAPPINGS.put(url, system);
            }
        }
    }

    VSACCodeSystem(UMLSSourceVocabulary umlsMapping, String... urls) {
        this.umlsMapping = umlsMapping;
        this.urls = new HashSet<>(Arrays.asList(urls));
    }

    public static VSACCodeSystem ofUrl(String url) {
        return URL_MAPPINGS.get(url);
    }

    public UMLSSourceVocabulary getUmlsMapping() {
        return umlsMapping;
    }
}
