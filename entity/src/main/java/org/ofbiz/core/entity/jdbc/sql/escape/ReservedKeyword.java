package org.ofbiz.core.entity.jdbc.sql.escape;


import com.google.common.base.Strings;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public interface ReservedKeyword {
    String SEPARATOR = ",";
    Character ESCAPE_CHARACTER = '"';
    DatabaseMetaData getDatabaseMetadata();

    default List<String> getReservedKeywords() {
        try {
            String sqlKeywords = Optional.ofNullable(getDatabaseMetadata().getSQLKeywords())
                    .orElse("");

            return Optional.of(sqlKeywords)
                    .filter(keywords -> !Strings.isNullOrEmpty(keywords))
                    .map(keywords -> keywords.split(SEPARATOR))
                    .map(Stream::of)
                    .map(stream -> stream
                            .map(String::trim)
                            .collect(toList()))
                    .orElse(emptyList());
        } catch (SQLException e) {
            return emptyList();
        }
    }

    default String escapeColumnName(String colName) {
        Objects.requireNonNull(colName);
        final StringBuffer stringBuffer = new StringBuffer();
        return getReservedKeywords().stream()
                .filter(reservedKeyword -> reservedKeyword.equalsIgnoreCase(colName.trim()))
                .map(column -> stringBuffer
                        .append(ESCAPE_CHARACTER)
                        .append(column)
                        .append(ESCAPE_CHARACTER)
                        .toString())
                .findFirst()
                .orElse(colName);
    }
}
