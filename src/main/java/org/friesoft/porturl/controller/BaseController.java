package org.friesoft.porturl.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class BaseController {

    @Autowired
    protected ObjectProvider<ObjectMapper> objectMapperProvider;

    protected ObjectMapper getObjectMapper() {
        return objectMapperProvider.getIfAvailable(ObjectMapper::new);
    }

    protected <T> ResponseEntity<List<T>> ok(Page<T> page, String resource) {
        HttpHeaders headers = new HttpHeaders();
        long start = page.getNumber() * (long) page.getSize();
        long end = start + page.getNumberOfElements() - 1;
        
        headers.add("Content-Range", resource + " " + start + "-" + end + "/" + page.getTotalElements());
        headers.add("Access-Control-Expose-Headers", "Content-Range");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(page.getContent());
    }

    protected Pageable getPageable(Integer page, Integer size, List<String> sort, String range) {
        int finalPage = page != null ? page : 0;
        int finalSize = size != null ? size : Integer.MAX_VALUE;
        Sort finalSort = Sort.unsorted();

        if (range != null && !range.isEmpty()) {
            try {
                List<Integer> rangeList = getObjectMapper().readValue(range, new TypeReference<>() {});
                if (rangeList.size() == 2) {
                    int start = rangeList.get(0);
                    int end = rangeList.get(1);
                    finalSize = end - start + 1;
                    if (finalSize > 0) {
                        finalPage = start / finalSize;
                    }
                }
            } catch (Exception e) {
                // Ignore parse errors
            }
        }

        if (sort != null && !sort.isEmpty()) {
            try {
                String combinedSort = String.join(",", sort);
                if (combinedSort.startsWith("[") && combinedSort.endsWith("]")) {
                    List<String> sortList = getObjectMapper().readValue(combinedSort, new TypeReference<>() {});
                    if (sortList.size() == 2) {
                        finalSort = Sort.by(Sort.Direction.fromString(sortList.get(1)), sortList.get(0));
                    }
                } else {
                    List<Sort.Order> orders = new ArrayList<>();
                    for (String s : sort) {
                        String[] parts = s.split(",");
                        if (parts.length == 2) {
                            orders.add(new Sort.Order(Sort.Direction.fromString(parts[1]), parts[0]));
                        }
                    }
                    if (!orders.isEmpty()) {
                        finalSort = Sort.by(orders);
                    }
                }
            } catch (Exception e) {
                // Ignore parse errors or invalid directions
            }
        }

        return PageRequest.of(finalPage, finalSize, finalSort);
    }

    protected Map<String, Object> getFilterMap(String filter) {
        if (filter != null && !filter.isEmpty()) {
            try {
                return getObjectMapper().readValue(filter, new TypeReference<>() {});
            } catch (Exception e) {
                return Map.of();
            }
        }
        return Map.of();
    }

    protected String getQuery(String filter) {
        Map<String, Object> filterMap = getFilterMap(filter);
        if (filterMap.containsKey("filter")) {
            return filterMap.get("filter").toString();
        } else if (filterMap.containsKey("q")) {
            return filterMap.get("q").toString();
        } else if (filter != null && !filter.isEmpty() && !filter.startsWith("{")) {
            return filter;
        }
        return null;
    }
}
