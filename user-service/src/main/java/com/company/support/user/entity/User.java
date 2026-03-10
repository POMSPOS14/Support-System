package com.company.support.user.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "user_full_name", nullable = false)
    private String userFullName;

    @Column(name = "user_login", nullable = false, unique = true)
    private String userLogin;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String workplace;

    @Column(name = "keycloak_id", unique = true)
    private String keycloakId;
}
