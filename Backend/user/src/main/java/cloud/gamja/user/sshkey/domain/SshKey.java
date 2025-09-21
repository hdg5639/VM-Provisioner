package cloud.gamja.user.sshkey.domain;

import cloud.gamja.user.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="ssh_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SshKey {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private User user;
    @Column(nullable=false)
    private String name;
    @Lob @Column(nullable=false, unique=true)
    private String publicKey; // OpenSSH
    @Column(nullable=false, unique=true)
    private String fingerprint;   // SHA256
    private Instant createdAt;

    @PrePersist
    void onC(){createdAt= Instant.now();}
}