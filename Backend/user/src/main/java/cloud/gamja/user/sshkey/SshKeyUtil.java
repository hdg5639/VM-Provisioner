package cloud.gamja.user.sshkey;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class SshKeyUtil {
    public String fingerprint(String publicKey) throws NoSuchAlgorithmException {
        String[] parts = publicKey.trim().split("\\s+");
        if (parts.length < 2) throw new IllegalArgumentException("Invalid OpenSSH public key");
        byte[] decoded = Base64.getDecoder().decode(parts[1]);
        byte[] sha = MessageDigest.getInstance("SHA-256").digest(decoded);
        return "SHA256:" + Base64.getEncoder().withoutPadding().encodeToString(sha);
    }
    public void validate(String publicKey) {
        if (publicKey == null || publicKey.length() > 8192) throw new IllegalArgumentException("Key too large");
        if (!publicKey.startsWith("ssh-") && !publicKey.startsWith("ecdsa-") && !publicKey.startsWith("sk-"))
            throw new IllegalArgumentException("Unsupported key type");
        fingerprint(publicKey); // will throw if invalid
    }
}