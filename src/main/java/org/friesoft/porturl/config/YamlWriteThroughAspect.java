package org.friesoft.porturl.config;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.friesoft.porturl.service.AdminService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Aspect
@Configuration
public class YamlWriteThroughAspect {

    private final AdminService adminService;

    public YamlWriteThroughAspect(@Lazy AdminService adminService) {
        this.adminService = adminService;
    }

    @Pointcut("execution(* org.friesoft.porturl.repositories.ApplicationRepository.save*(..)) || " +
              "execution(* org.friesoft.porturl.repositories.ApplicationRepository.delete*(..)) || " +
              "execution(* org.friesoft.porturl.repositories.CategoryRepository.save*(..)) || " +
              "execution(* org.friesoft.porturl.repositories.CategoryRepository.delete*(..))")
    public void repositoryMutations() {}

    @AfterReturning("repositoryMutations()")
    public void afterMutation() {
        if (!adminService.isSyncing()) {
            adminService.exportToFile();
        }
    }
}
