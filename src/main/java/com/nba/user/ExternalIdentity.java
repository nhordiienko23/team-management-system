package com.nba.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "external_identities",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_external_identity_provider_subject",
                        columnNames = {"provider", "subject"}
                ),
                @UniqueConstraint(
                        name = "uq_external_identity_user_provider",
                        columnNames = {"user_id", "provider"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ExternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 512)
    private String provider;

    @Column(nullable = false, length = 255)
    private String subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public ExternalIdentity(String provider, String subject, User user) {
        this.provider = provider;
        this.subject = subject;
        this.user = user;
    }
}
