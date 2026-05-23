package com.wholesale.marketplace.modules.user;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** A merchant delivery address. A user may flag exactly one as default. */
@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    private String label;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String city;

    private String province;

    @Builder.Default
    private boolean isDefault = false;
}
