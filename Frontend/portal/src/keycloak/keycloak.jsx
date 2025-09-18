import Keycloak from "keycloak-js";

export const keycloak = new Keycloak({
    url: import.meta.env.VITE_KEYCLOAK_URL,
    realm: import.meta.env.VITE_KEYCLOAK_REALM,
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
});

let initPromise = null;

export function initKeycloak({ timeoutMs = 8000 } = {}) {
    if (initPromise) return initPromise;

    const opts = {
        onLoad: "check-sso",
        pkceMethod: "S256",
        checkLoginIframe: false,
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
    };

    // 1차 SSO
    const silent = keycloak.init(opts)
        .then((ok) => !!ok)
        .catch((e) => {
            console.warn("[KC init] silent failed:", e);
            return false; // 에러여도 메인 표시
        });

    // 타임아웃
    const timeout = new Promise((resolve) =>
        setTimeout(() => resolve(false), timeoutMs)
    );

    initPromise = Promise.race([silent, timeout]);
    return initPromise;
}
