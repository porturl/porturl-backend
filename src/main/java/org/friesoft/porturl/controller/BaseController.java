package org.friesoft.porturl.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.List;

public abstract class BaseController {

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
}
