package edu.mayo.hsr.dhs.cql2nlp.structs;

import java.util.Objects;

public class CodifiedValueSetElement {
    private VSACCodeSystem codeSystem;
    private String code;

    public CodifiedValueSetElement(VSACCodeSystem codeSystem, String code) {
        this.codeSystem = codeSystem;
        this.code = code;
    }

    public VSACCodeSystem getCodeSystem() {
        return codeSystem;
    }

    public void setCodeSystem(VSACCodeSystem codeSystem) {
        this.codeSystem = codeSystem;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodifiedValueSetElement that = (CodifiedValueSetElement) o;
        return Objects.equals(codeSystem, that.codeSystem) &&
                Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codeSystem, code);
    }
}
