import { useEffect, useState } from "react";

export default function App() {
    const [me, setMe] = useState(null);
    const [loading, setLoading] = useState(true);
    const [navBusy, setNavBusy] = useState(false);
    const ORIGIN = typeof window !== "undefined" ? window.location.origin : "";

    async function loadMe() {
        try {
            const res = await fetch("/api/me", {
                credentials: "include",
                cache: "no-store",
            });
            if (res.ok) {
                const data = await res.json();
                setMe(data);
            } else {
                setMe(null); // 401/403 => 비로그인
            }
        } catch {
            setMe(null);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadMe();
    }, []);

    // ✅ BFF로 확실히 보냄: /auth/login (Caddy에서 BFF로 라우팅)
    const onLogin = () => {
        setNavBusy(true);
        window.location.assign(`${ORIGIN}/auth/login`);
    };

    // ✅ OIDC 로그아웃 후 BFF가 / 로 복귀시킴
    const onLogout = () => {
        setNavBusy(true);
        window.location.assign(`${ORIGIN}/logout`);
    };

    if (loading) return <div>로딩중...</div>;

    return (
        <main style={{ minHeight: "100vh", display: "grid", placeItems: "center" }}>
            <div style={{ width: 420, padding: 24, borderRadius: 16, boxShadow: "0 10px 30px rgba(0,0,0,.08)" }}>
                <h1 style={{ marginTop: 0 }}>GJ 클라우드 — 포털</h1>

                {me ? (
                    <>
                        <div style={{ margin: "12px 0" }}>
                            안녕하세요, <b>{me.user?.displayName || me.name || me.email}</b>님!
                        </div>
                        <pre style={{ background: "#f7f7f7", padding: 12, borderRadius: 8, overflow: "auto" }}>
              {JSON.stringify(me, null, 2)}
            </pre>
                        <button onClick={onLogout} style={btn} disabled={navBusy}>
                            {navBusy ? "이동 중..." : "로그아웃"}
                        </button>
                    </>
                ) : (
                    <>
                        <p style={{ color: "#666" }}>비로그인 상태입니다. 메인에서 대기합니다.</p>
                        <button onClick={onLogin} style={btn} disabled={navBusy}>
                            {navBusy ? "이동 중..." : "로그인"}
                        </button>
                    </>
                )}
            </div>
        </main>
    );
}

const btn = {
    width: "100%",
    padding: "12px 16px",
    borderRadius: 12,
    border: "1px solid #111",
    background: "#111",
    color: "#fff",
    cursor: "pointer",
};
