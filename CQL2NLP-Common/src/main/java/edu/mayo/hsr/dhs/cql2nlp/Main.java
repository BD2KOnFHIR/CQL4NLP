package edu.mayo.hsr.dhs.cql2nlp;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.mayo.hsr.dhs.cql2nlp.structs.CodifiedValueSetElement;
import org.hl7.fhir.r4.model.ValueSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    public static void main(String... args) throws IOException {
        Map<String, Set<String>> rulesets = new HashMap<>();
        String in = String.join("\n", Files.readAllLines(new File("test.cql").toPath()));
        Map<String, String> valueSetRaw = new CQLParser().getValueSets(in);
        ValueSetResolver rls = new ValueSetResolver("", "");
        UMLSBrowser oet = new UMLSBrowser("", "");
        valueSetRaw.forEach((id, oid) -> {
            ValueSet vs = rls.getValueSetForOID(oid);
            Set<CodifiedValueSetElement> codes = rls.resolveValueSetCodes(vs);
            Set<String> displayNames = ConcurrentHashMap.newKeySet();
            for (CodifiedValueSetElement element : codes) {
                Set<CodifiedValueSetElement> vals = null;
                try {
                    vals = oet.getCUIsForValueSetElement(element, true);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    continue;
                }
                vals.parallelStream().forEach(
                        val -> {
                            try {
                                displayNames.addAll(oet.getDisplayNamesForCUI(val.getCode()));
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }
                        }
                );
            }
            rulesets.put(id, displayNames);
        });

        return;
    }
}
