package org.friesoft.porturl.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@lombok.extern.slf4j.Slf4j
public class ReactAdminConfig implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;

    public ReactAdminConfig(ObjectProvider<ObjectMapper> objectMapperProvider) {
        // Fallback to a new ObjectMapper if no bean is found in the context
        this.objectMapper = objectMapperProvider.getIfAvailable(() -> {
            log.warn("No ObjectMapper bean found in context, creating a new default instance for ReactAdminConfig");
            return new ObjectMapper();
        });
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new ReactAdminPageableResolver(objectMapper));
    }

    public static class ReactAdminPageableResolver implements HandlerMethodArgumentResolver {
        private final ObjectMapper objectMapper;

        public ReactAdminPageableResolver(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(Pageable.class);
        }

        @Override
        public Pageable resolveArgument(@NonNull MethodParameter parameter, ModelAndViewContainer mavContainer,
                                        NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
            
            String rangeParam = webRequest.getParameter("range");
            String sortParam = webRequest.getParameter("sort");

            int page = 0;
            int size = 20;
            Sort sort = Sort.unsorted();

            if (rangeParam != null && !rangeParam.isEmpty()) {
                try {
                    List<Integer> range = objectMapper.readValue(rangeParam, new TypeReference<>() {});
                    if (range.size() == 2) {
                        int start = range.get(0);
                        int end = range.get(1);
                        size = end - start + 1;
                        if (size > 0) {
                            page = start / size;
                        }
                    }
                } catch (Exception e) {
                    org.slf4j.LoggerFactory.getLogger(ReactAdminPageableResolver.class)
                        .error("Failed to parse 'range' parameter: {}", rangeParam, e);
                    throw e;
                }
            } else {
                String pageStr = webRequest.getParameter("page");
                String sizeStr = webRequest.getParameter("size");
                if (pageStr != null) page = Integer.parseInt(pageStr);
                if (sizeStr != null) size = Integer.parseInt(sizeStr);
            }

            if (sortParam != null && !sortParam.isEmpty()) {
                try {
                    List<String> sortList = objectMapper.readValue(sortParam, new TypeReference<>() {});
                    if (sortList.size() == 2) {
                        String field = sortList.get(0);
                        String order = sortList.get(1);
                        sort = Sort.by(Sort.Direction.fromString(order), field);
                    }
                } catch (Exception e) {
                    org.slf4j.LoggerFactory.getLogger(ReactAdminPageableResolver.class)
                        .error("Failed to parse 'sort' parameter: {}", sortParam, e);
                    throw e;
                }
            }

            return PageRequest.of(page, size, sort);
        }
    }
}
