// src/keycloak.jsx
import Keycloak from "keycloak-js";

export const keycloak = new Keycloak({
    url: import.meta.env.VITE_KEYCLOAK_URL,
    realm: import.meta.env.VITE_KEYCLOAK_REALM,
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
});

let initPromise = null;
export function initKeycloak() {
    if (initPromise) return initPromise;

    const optsSilent = {
        onLoad: "check-sso",
        pkceMethod: "S256",
        checkLoginIframe: false,
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
    };

    const timeout = new Promise((_, rej) =>
        setTimeout(() => rej(new Error("KC init timeout")), 8000)
    );

    initPromise = Promise.race([keycloak.init(optsSilent), timeout])
        .catch(async (e) => {
            console.warn("[KC] silent 실패 또는 타임아웃 → login-required 폴백", e);
            return keycloak.init({
                onLoad: "login-required",
                pkceMethod: "S256",
                checkLoginIframe: false,
            });
        });

    return initPromise;
}
