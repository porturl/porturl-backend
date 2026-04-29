package org.friesoft.porturl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BaseControllerTest {

    private TestController controller;
    private ObjectMapper objectMapper;

    // Subclass to test abstract BaseController
    private static class TestController extends BaseController {
        public TestController(ObjectProvider<ObjectMapper> provider) {
            this.objectMapperProvider = provider;
        }

        public Pageable publicGetPageable(Integer page, Integer size, List<String> sort, String range) {
            return super.getPageable(page, size, sort, range);
        }

        public Map<String, Object> publicGetFilterMap(String filter) {
            return super.getFilterMap(filter);
        }

        public String publicGetQuery(String filter) {
            return super.getQuery(filter);
        }
        
        public ResponseEntity<List<String>> publicOk(Page<String> page, String resource) {
            return super.ok(page, resource);
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ObjectProvider<ObjectMapper> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable(any())).thenReturn(objectMapper);
        controller = new TestController(provider);
    }

    @Test
    void getPageable_withRange_parsesCorrectly() {
        Pageable result = controller.publicGetPageable(null, null, null, "[0,9]");
        assertEquals(0, result.getPageNumber());
        assertEquals(10, result.getPageSize());
    }

    @Test
    void getPageable_withSortJson_parsesCorrectly() {
        Pageable result = controller.publicGetPageable(null, null, List.of("[\"name\",\"DESC\"]"), null);
        assertTrue(result.getSort().isSorted());
        assertEquals(Sort.Direction.DESC, result.getSort().getOrderFor("name").getDirection());
    }

    @Test
    void getPageable_withSortStrings_parsesCorrectly() {
        Pageable result = controller.publicGetPageable(null, null, List.of("name,ASC"), null);
        assertTrue(result.getSort().isSorted());
        assertEquals(Sort.Direction.ASC, result.getSort().getOrderFor("name").getDirection());
    }
    
    @Test
    void getPageable_withInvalidRange_ignores() {
        Pageable result = controller.publicGetPageable(null, null, null, "invalid");
        assertEquals(0, result.getPageNumber());
        assertEquals(Integer.MAX_VALUE, result.getPageSize());
    }
    
    @Test
    void getPageable_withInvalidSort_ignores() {
        Pageable result = controller.publicGetPageable(null, null, List.of("invalid"), null);
        assertFalse(result.getSort().isSorted());
    }

    @Test
    void getFilterMap_withValidJson_returnsMap() {
        Map<String, Object> result = controller.publicGetFilterMap("{\"q\":\"test\"}");
        assertEquals("test", result.get("q"));
    }

    @Test
    void getFilterMap_withInvalidJson_returnsEmptyMap() {
        Map<String, Object> result = controller.publicGetFilterMap("invalid");
        assertTrue(result.isEmpty());
    }

    @Test
    void getQuery_withQField_returnsQuery() {
        String result = controller.publicGetQuery("{\"q\":\"test-query\"}");
        assertEquals("test-query", result);
    }

    @Test
    void getQuery_withFilterField_returnsFilter() {
        String result = controller.publicGetQuery("{\"filter\":\"test-filter\"}");
        assertEquals("test-filter", result);
    }

    @Test
    void getQuery_withPlainString_returnsString() {
        String result = controller.publicGetQuery("plain-query");
        assertEquals("plain-query", result);
    }
    
    @Test
    void ok_returnsContentRangeHeader() {
        Page<String> page = new PageImpl<>(List.of("item1", "item2"));
        ResponseEntity<List<String>> result = controller.publicOk(page, "items");
        assertEquals("items 0-1/2", result.getHeaders().getFirst("Content-Range"));
        assertEquals("Content-Range", result.getHeaders().getFirst("Access-Control-Expose-Headers"));
    }
}
