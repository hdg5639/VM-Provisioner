import Keycloak from "keycloak-js";

export const keycloak = new Keycloak({
    url: import.meta.env.VITE_KEYCLOAK_URL,
    realm: import.meta.env.VITE_KEYCLOAK_REALM,
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
});

let initPromise = null;
let fallbackTriggered = false;

export function initKeycloak({ timeoutMs = 8000 } = {}) {
    if (initPromise) return initPromise;

    // 1차
    initPromise = keycloak
        .init({
            onLoad: "check-sso",
            pkceMethod: "S256",
            checkLoginIframe: false,
            silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
        })
        // 실패
        .catch((e) => {
            throw e;
        });

    // 2차
    setTimeout(() => {
        if (fallbackTriggered) return;
        if (!keycloak.authenticated) {
            fallbackTriggered = true;
            keycloak.login({ redirectUri: window.location.href });
        }
    }, timeoutMs);

    return initPromise;
}