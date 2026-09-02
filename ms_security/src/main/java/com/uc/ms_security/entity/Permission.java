package com.uc.ms_security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_permission_url_method",
                        columnNames = {"url", "method"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            nullable = false,
            length = 255
    )
    private String url;

    @Column(
            nullable = false,
            length = 10
    )
    private String method;

    @OneToMany(
            mappedBy = "permission",
            fetch = FetchType.LAZY
    )
    private List<RolePermission> rolePermissions = new ArrayList<>();
}
