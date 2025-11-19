package org.friesoft.porturl.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users") // "user" is a reserved keyword in many databases
public class User {

    @Getter
    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Email
    @Column(unique = true)
    @Setter
    @Getter
    private String email;

    @NotBlank
    @Column(unique = true)
    @Setter
    @Getter
    private String providerUserId;

}
