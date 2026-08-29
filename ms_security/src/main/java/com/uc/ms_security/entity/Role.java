package com.uc.ms_security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            nullable = false,
            unique = true,
            length = 50
    )
    private String name;

    @Column(
            nullable = false,
            length = 255
    )
    private String description;

    @OneToMany(
            mappedBy = "role",
            fetch = FetchType.LAZY
    )
    private List<UserRole> userRoles = new ArrayList<>();
}
