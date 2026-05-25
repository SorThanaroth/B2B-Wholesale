package com.wholesale.marketplace.modules.company;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** A supplier company. {@code bankAccount} is the settlement destination. */
@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String logoUrl;

    @Column(nullable = false)
    private String bankAccount;

    private String contactEmail;

    // ----- profile / KYC details (used by admins to review supplier applications) -----
    /** Business registration / license number. */
    private String registrationNo;

    private String phone;

    @Column(columnDefinition = "text")
    private String address;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CompanyStatus status = CompanyStatus.ACTIVE;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
