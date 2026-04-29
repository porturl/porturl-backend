package org.friesoft.porturl.controller;

import org.friesoft.porturl.dto.Category;
import org.friesoft.porturl.security.CustomGrantedAuthoritiesExtractor;
import org.friesoft.porturl.service.ApplicationService;
import org.friesoft.porturl.service.CategoryService;
import org.friesoft.porturl.repositories.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.*;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Optional;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private CategoryService categoryService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private ApplicationService applicationService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private CategoryRepository categoryRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private CustomGrantedAuthoritiesExtractor customGrantedAuthoritiesExtractor;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCategories_returnsOk() throws Exception {
        when(categoryService.getVisibleCategories(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        mockMvc.perform(get("/api/categories")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());
    }

    @Test
    void findCategoryById_returnsCategory() throws Exception {
        org.friesoft.porturl.entities.Category entity = new org.friesoft.porturl.entities.Category();
        entity.setId(1L);
        entity.setName("Test");

        Category dto = new Category();
        dto.setId(1L);
        dto.setName("Test");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(categoryService.mapToDto(entity)).thenReturn(dto);

        mockMvc.perform(get("/api/categories/1")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());
    }

    @Test
    void findCategoryById_returnsNotFound() throws Exception {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/categories/1")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void addCategory_success() throws Exception {
        Category requestDto = new Category();
        requestDto.setName("New Cat");

        Category responseDto = new Category();
        responseDto.setId(1L);
        responseDto.setName("New Cat");

        when(categoryRepository.findMaxSortOrder()).thenReturn(5);
        when(categoryRepository.save(any())).thenAnswer(invocation -> {
            org.friesoft.porturl.entities.Category c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(categoryService.mapToDto(any())).thenReturn(responseDto);

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(requestDto))
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isCreated());
    }

    @Test
    void addCategory_conflict() throws Exception {
        Category requestDto = new Category();
        requestDto.setName("New Cat");

        when(categoryRepository.save(any())).thenThrow(new DataIntegrityViolationException("Conflict"));

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(requestDto))
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict());
    }

    @Test
    void updateCategory_success() throws Exception {
        org.friesoft.porturl.entities.Category existing = new org.friesoft.porturl.entities.Category();
        existing.setId(1L);
        existing.setName("Old");

        Category updateDto = new Category();
        updateDto.setName("New");
        updateDto.setApplicationSortMode(Category.ApplicationSortModeEnum.CUSTOM);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.saveAndFlush(any())).thenReturn(existing);
        when(categoryService.mapToDto(any())).thenReturn(updateDto);

        mockMvc.perform(put("/api/categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(updateDto))
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        verify(applicationService).enforceApplicationSortOrder(existing);
    }

    @Test
    void updateCategory_notFound() throws Exception {
        Category updateDto = new Category();
        updateDto.setName("New");

        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(updateDto))
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void reorderCategories_success() throws Exception {
        org.friesoft.porturl.dto.CategoryReorderRequest req = new org.friesoft.porturl.dto.CategoryReorderRequest();
        req.setId(1L);
        req.setSortOrder(2);

        org.friesoft.porturl.entities.Category cat = new org.friesoft.porturl.entities.Category();
        cat.setId(1L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));

        mockMvc.perform(post("/api/categories/reorder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(List.of(req)))
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        verify(categoryRepository).save(cat);
    }

    @Test
    void deleteCategory_success() throws Exception {
        when(categoryRepository.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/categories/1")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void deleteCategory_notFound() throws Exception {
        when(categoryRepository.existsById(1L)).thenReturn(false);

        mockMvc.perform(delete("/api/categories/1")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void findApplicationsByCategory_success() throws Exception {
        when(categoryService.getApplicationsByCategory(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/categories/1/applications")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());
    }

    @Test
    void reorderApplicationsInCategory_success() throws Exception {
        mockMvc.perform(post("/api/categories/1/applications/reorder")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[1, 2, 3]")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        verify(categoryService).reorderApplicationsInCategory(1L, List.of(1L, 2L, 3L));
    }
}
