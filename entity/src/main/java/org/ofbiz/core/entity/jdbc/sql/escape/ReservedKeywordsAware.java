package org.ofbiz.core.entity.jdbc.sql.escape;


import com.google.common.base.Strings;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public interface ReservedKeywordsAware {
    String SEPARATOR = ",";
    Character ESCAPE_CHARACTER_DOUBLE_QUOTE = '"';
    Character START_ESCAPE_CHARACTER_SQUARE_BRACKET = '[';
    Character END_ESCAPE_CHARACTER_SQUARE_BRACKET = ']';

    String getSqlKeywords();

    default Character getStartEscapeCharacter() {
        return ESCAPE_CHARACTER_DOUBLE_QUOTE;
    }

    default Character getEndEscapeCharacter() {
        return ESCAPE_CHARACTER_DOUBLE_QUOTE;
    }

    default List<String> getReservedKeywords() {
        return Optional.of(getSqlKeywords())
                .filter(keywords -> !Strings.isNullOrEmpty(keywords))
                .map(keywords -> keywords.split(SEPARATOR))
                .map(Stream::of)
                .map(stream -> stream
                        .map(String::trim)
                        .collect(toList()))
                .orElse(emptyList());
    }

    default String escapeColumnName(String colName) {
        Objects.requireNonNull(colName);
        final StringBuffer stringBuffer = new StringBuffer();
        return getReservedKeywords().stream()
                .filter(reservedKeyword -> reservedKeyword.equalsIgnoreCase(colName.trim()))
                .map(column -> stringBuffer
                        .append(getStartEscapeCharacter())
                        .append(column)
                        .append(getEndEscapeCharacter())
                        .toString())
                .findFirst()
                .orElse(colName);
    }
}
