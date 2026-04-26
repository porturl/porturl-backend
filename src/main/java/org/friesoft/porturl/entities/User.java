package org.friesoft.porturl.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users") // "user" is a reserved keyword in many databases
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Getter
    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 255)
    @Column(unique = true, nullable = false)
    @Setter
    @Getter
    private String username;

    @Column(unique = true)
    @Setter
    @Getter
    private String email;

    @Column(unique = true)
    @Setter
    @Getter
    private String providerUserId;

    @Column
    @Setter
    @Getter
    private String image;

    @Transient
    public String getImageUrl() {
        if (image == null || image.isBlank()) {
            return null;
        }
        return org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/images/")
                .path(image)
                .toUriString();
    }
}
