package org.ofbiz.core.entity.jdbc.sql.escape;


import java.util.Objects;
import java.util.Set;

public interface ReservedKeywordsAware {

    Character ESCAPE_CHARACTER_DOUBLE_QUOTE = '"';
    Character START_ESCAPE_CHARACTER_SQUARE_BRACKET = '[';
    Character END_ESCAPE_CHARACTER_SQUARE_BRACKET = ']';

    Set<String> getReservedKeywords();

    default Character getStartEscapeCharacter() {
        return ESCAPE_CHARACTER_DOUBLE_QUOTE;
    }

    default Character getEndEscapeCharacter() {
        return ESCAPE_CHARACTER_DOUBLE_QUOTE;
    }

    default String escapeColumnName(String colName) {
        Objects.requireNonNull(colName);
        final StringBuilder stringBuilder = new StringBuilder();
        if (getReservedKeywords().contains(colName.trim().toUpperCase())) {
            return stringBuilder
                    .append(getStartEscapeCharacter())
                    .append(colName)
                    .append(getEndEscapeCharacter())
                    .toString();
        }
        return colName;
    }
}
