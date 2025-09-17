import Keycloak from "keycloak-js";

export const keycloak = new Keycloak({
    url: import.meta.env.VITE_KEYCLOAK_URL,
    realm: import.meta.env.VITE_KEYCLOAK_REALM,
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
});

// ✅ init을 단 한 번만 실행되게 보장
let initPromise = null;
export function initKeycloak() {
    if (!initPromise) {
        initPromise = keycloak.init({
            onLoad: "check-sso",
            pkceMethod: "S256",
            silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
            checkLoginIframe: false,
        });
    }
    return initPromise;
}