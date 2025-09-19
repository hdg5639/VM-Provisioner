package cloud.gamja.user.sshkey.domain;

import cloud.gamja.user.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name="ssh_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SshKey {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false)
    User user;
    @Column(nullable=false) String name;
    @Lob @Column(nullable=false, unique=true) String publicKey; // OpenSSH
    @Column(nullable=false, unique=true) String fingerprint;   // SHA256:...
    Instant createdAt;
    @PrePersist
    void onC(){createdAt= Instant.now();}
}