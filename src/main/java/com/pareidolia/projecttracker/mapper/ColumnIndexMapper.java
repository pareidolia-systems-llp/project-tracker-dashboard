package com.pareidolia.projecttracker.mapper;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ColumnIndexMapper {

    public Map<String, Integer> buildColumnIndexMap(List<Object> headerRow) {

        Map<String, Integer> columnIndexMap = new HashMap<>();

        for (int i = 0; i < headerRow.size(); i++) {

            Object value = headerRow.get(i);

            if (value != null) {

                String header = value.toString();



                columnIndexMap.put(normalize(header), i);
            }
        }

        return columnIndexMap;
    }

    private String normalize(String header) {

        return header == null
                ? ""
                : header.replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }
}