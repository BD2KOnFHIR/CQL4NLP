package edu.mayo.hsr.dhs.cql2nlp;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.cqframework.cql.gen.cqlLexer;
import org.cqframework.cql.gen.cqlParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper around CQL's ANTLR4 Parser to Convert into Computationally Accessible Format
 */
public class CQLParser {
    public Map<String, String> getValueSets(String cql) throws IOException {
        Map<String, String> ret = new HashMap<>();
        cqlLexer lexer = new cqlLexer(new ANTLRInputStream(new ByteArrayInputStream(
                cql.getBytes(StandardCharsets.UTF_8))));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        cqlParser parser = new cqlParser(tokens);
        cqlParser.LibraryContext lib = parser.library();
        for (cqlParser.ValuesetDefinitionContext valueset : lib.valuesetDefinition()) {
            cqlParser.IdentifierContext idContext = valueset.identifier();
            String id = null;
            if (idContext.DELIMITEDIDENTIFIER() != null) {
                id = idContext.DELIMITEDIDENTIFIER().getSymbol().getText();
            } else if (idContext.QUOTEDIDENTIFIER() != null) {
                id = idContext.QUOTEDIDENTIFIER().getSymbol().getText();
                id = id.substring(1, id.length() - 1);
            } else if (idContext.IDENTIFIER() != null) {
                id = idContext.IDENTIFIER().getSymbol().getText();
            }
            String oid = valueset.valuesetId().getText();
            if (oid.startsWith("'")) {
                oid = oid.substring(1, oid.length() - 1);
            }
            ret.put(id, oid);
        }
        return ret;
    }
}
