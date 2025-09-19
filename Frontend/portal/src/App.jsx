import { useEffect, useState } from "react";

export default function App() {
    const [me, setMe] = useState(null);           // BFF /api/me (세션 체크)
    const [profile, setProfile] = useState(null); // user-service /users/me
    const [keys, setKeys] = useState([]);         // user-service /keys
    const [loading, setLoading] = useState(true);

    const [navBusy, setNavBusy] = useState(false);
    const [opBusy, setOpBusy] = useState(false);  // 키 등록/삭제 중
    const [msg, setMsg] = useState("");
    const [form, setForm] = useState({ name: "", publicKey: "" });

    // 진단 덤프
    const [diagProfile, setDiagProfile] = useState(null);
    const [diagKeys, setDiagKeys] = useState(null);

    const ORIGIN = typeof window !== "undefined" ? window.location.origin : "";

    /* ---------------- helpers ---------------- */

    // JSON일 때만 안전 파싱
    async function safeJsonFromResponse(res) {
        const ct = (res.headers.get("content-type") || "").toLowerCase();
        if (!ct.includes("application/json")) return null;
        const text = await res.text().catch(() => "");
        if (!text) return null;
        try { return JSON.parse(text); } catch { return null; }
    }

    // 에러 메시지용 텍스트
    const safeText = async (res) => {
        try { return await res.text(); } catch { return ""; }
    };

    // 진단용 fetch: status/headers/ct/text/json까지 반환
    async function fetchDiag(url, opts = {}) {
        const res = await fetch(url, {
            credentials: "include",
            cache: "no-store",
            ...opts,
        });
        const headers = Object.fromEntries(res.headers.entries());
        const ct = (headers["content-type"] || "").toLowerCase();

        // body를 한 번만 소비해야 해서 text 먼저 읽고, json은 파싱 시도
        const text = await res.text().catch(() => "");
        let json = null;
        if (ct.includes("application/json") && text) {
            try { json = JSON.parse(text); } catch { /* ignore */ }
        }

        return {
            url,
            ok: res.ok,
            status: res.status,
            headers,
            contentType: ct,
            text,
            json,
        };
    }

    /* ---------------- data loads ---------------- */

    // BFF 세션 체크
    async function loadMe() {
        try {
            const res = await fetch("/api/me", {
                credentials: "include",
                cache: "no-store",
            });
            if (res.ok) {
                const data = await safeJsonFromResponse(res);
                setMe(data ?? null);
            } else {
                setMe(null);
            }
        } catch {
            setMe(null);
        } finally {
            setLoading(false);
        }
    }

    // user-service 데이터(프로필/키) 로딩 + 진단 저장
    async function loadUserData() {
        if (!me) return;
        try {
            const [p, k] = await Promise.all([
                fetchDiag("/api/ds/user/users/me"),
                fetchDiag("/api/ds/user/keys"),
            ]);

            setDiagProfile(p);
            setDiagKeys(k);

            if (p.ok && p.json) setProfile(p.json);
            else if (p.status === 403) setMsg("프로필 권한 거부(403)");

            if (k.ok && Array.isArray(k.json)) {
                setKeys(k.json);
            } else if (!k.ok) {
                setMsg(`키 목록 실패 (${k.status}) ${k.text?.slice(0, 200)}`);
            }
        } catch (e) {
            console.error(e);
            setMsg("데이터 로딩 실패");
        }
    }

    useEffect(() => { loadMe(); }, []);
    useEffect(() => { if (me) loadUserData(); }, [me]);

    /* ---------------- auth nav ---------------- */

    const onLogin = () => {
        setNavBusy(true);
        window.location.assign(`${ORIGIN}/auth/login`);
    };
    const onLogout = () => {
        setNavBusy(true);
        window.location.assign(`${ORIGIN}/logout`);
    };

    /* ---------------- actions ---------------- */

    // 키 등록
    const createKey = async (ev) => {
        ev?.preventDefault?.();

        if (!form.name.trim() || !form.publicKey.trim()) {
            setMsg("이름과 공개키를 모두 입력하세요.");
            return;
        }

        setOpBusy(true);
        setMsg("");
        try {
            const res = await fetch("/api/ds/user/keys", {
                method: "POST",
                credentials: "include",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    name: form.name.trim(),
                    publicKey: form.publicKey.trim(),
                }),
            });

            if (res.ok) {
                // POST 응답이 KeyDto JSON이면 로컬 상태에 즉시 반영
                const createdText = await res.text().catch(() => "");
                if (createdText) {
                    try {
                        const created = JSON.parse(createdText);
                        if (created && created.id != null) {
                            setKeys((prev) => [created, ...prev]);
                        }
                    } catch { /* ignore parse error */ }
                }
                setForm({ name: "", publicKey: "" });
                setMsg("키가 등록되었습니다.");
                // 최신 목록/진단 다시 조회
                await loadUserData();
            } else {
                const txt = await safeText(res);
                setMsg(`키 등록 실패 (${res.status}) ${txt}`);
            }
        } catch (err) {
            console.error(err);
            setMsg("키 등록 중 오류");
        } finally {
            setOpBusy(false);
        }
    };

    // 키 삭제
    async function deleteKey(id) {
        if (!confirm("정말 삭제할까요?")) return;
        setOpBusy(true);
        setMsg("");
        try {
            const res = await fetch(`/api/ds/user/keys/${id}`, {
                method: "DELETE",
                credentials: "include",
            });
            if (res.ok || res.status === 204) {
                setKeys((prev) => prev.filter((k) => k.id !== id));
                setMsg("삭제되었습니다.");
            } else {
                const txt = await safeText(res);
                setMsg(`삭제 실패 (${res.status}) ${txt}`);
            }
        } catch {
            setMsg("삭제 중 오류");
        } finally {
            setOpBusy(false);
        }
    }

    /* ---------------- render ---------------- */

    if (loading) return <div>로딩중...</div>;

    return (
        <main style={{ minHeight: "100vh", display: "grid", placeItems: "center" }}>
            <div style={{ width: 720, maxWidth: "95vw", padding: 24, borderRadius: 16, boxShadow: "0 10px 30px rgba(0,0,0,.08)" }}>
                <h1 style={{ marginTop: 0 }}>GJ 클라우드 — 포털</h1>

                {me ? (
                    <>
                        {/* 프로필 요약 */}
                        <section style={{ margin: "12px 0 16px" }}>
                            <b>환영합니다.</b>{" "}
                            <span>{me.user?.displayName || me.name || me.email}</span>
                            {profile && (
                                <span style={{ color: "#666", marginLeft: 8 }}>
                  (ID: {profile.id}, Email: {profile.email})
                </span>
                            )}
                        </section>

                        {/* 메시지/알림 */}
                        {msg && (
                            <div style={{ background: "#f1f7ff", border: "1px solid #d9e7ff", color: "#174ea6", padding: 10, borderRadius: 8, marginBottom: 12 }}>
                                {msg}
                            </div>
                        )}

                        {/* SSH 키 목록 */}
                        <section style={{ marginBottom: 20 }}>
                            <h3 style={{ margin: "8px 0" }}>내 SSH 키</h3>
                            {keys?.length ? (
                                <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
                                    {keys.map((k) => (
                                        <li key={k.id} style={itemRow}>
                                            <div style={{ flex: 1 }}>
                                                <div><b>{k.name}</b></div>
                                                <div style={{ fontSize: 12, color: "#666" }}>{k.fingerprint}</div>
                                                <div style={{ fontSize: 12, color: "#999", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", maxWidth: 520 }}>
                                                    {k.publicKey}
                                                </div>
                                            </div>
                                            <button onClick={() => deleteKey(k.id)} style={btnDanger} disabled={opBusy}>삭제</button>
                                        </li>
                                    ))}
                                </ul>
                            ) : (
                                <div style={{ color: "#666" }}>등록된 키가 없습니다.</div>
                            )}
                        </section>

                        {/* SSH 키 등록 폼 */}
                        <section style={{ marginBottom: 24 }}>
                            <h3 style={{ margin: "8px 0" }}>SSH 키 등록</h3>
                            <form onSubmit={createKey}>
                                <div style={fieldRow}>
                                    <label style={label}>이름</label>
                                    <input
                                        type="text"
                                        value={form.name}
                                        onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                                        placeholder="예: macbook"
                                        style={input}
                                        disabled={opBusy}
                                    />
                                </div>
                                <div style={fieldRow}>
                                    <label style={label}>공개키</label>
                                    <textarea
                                        value={form.publicKey}
                                        onChange={(e) => setForm((f) => ({ ...f, publicKey: e.target.value }))}
                                        placeholder="ssh-ed25519 AAAAC3... user@host"
                                        style={{ ...input, height: 96 }}
                                        disabled={opBusy}
                                    />
                                </div>
                                <div style={{ display: "flex", gap: 8 }}>
                                    <button type="submit" style={btn} disabled={opBusy}>
                                        {opBusy ? "등록 중..." : "등록"}
                                    </button>
                                    <button
                                        type="button"
                                        style={btnSecondary}
                                        onClick={() => setForm({ name: "", publicKey: "" })}
                                        disabled={opBusy}
                                    >
                                        초기화
                                    </button>
                                    <div style={{ flex: 1 }} />
                                    <button onClick={onLogout} style={btn} disabled={navBusy}>
                                        {navBusy ? "이동 중..." : "로그아웃"}
                                    </button>
                                </div>
                            </form>
                        </section>

                        {/* (디버그용) 진단 패널 */}
                        <details style={{ marginTop: 8 }}>
                            <summary>진단 패널</summary>
                            <div style={{ display: "grid", gap: 12, gridTemplateColumns: "1fr 1fr" }}>
                                <div>
                                    <h4>/api/ds/user/users/me</h4>
                                    <pre style={preBox}>{JSON.stringify(diagProfile, null, 2)}</pre>
                                </div>
                                <div>
                                    <h4>/api/ds/user/keys</h4>
                                    <pre style={preBox}>{JSON.stringify(diagKeys, null, 2)}</pre>
                                </div>
                            </div>
                            <button style={btnSecondary} onClick={loadUserData} disabled={opBusy}>다시 불러오기</button>
                        </details>

                        {/* (디버그용) 원본 보기 */}
                        <details style={{ marginTop: 8 }}>
                            <summary>자세히 보기 (raw state)</summary>
                            <pre style={preBox}>{JSON.stringify({ me, profile, keys }, redactTokens, 2)}</pre>
                        </details>
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

/* ---------------- util for redaction & styles ---------------- */

const redactTokens = (k, v) => {
    const key = (k || "").toLowerCase();
    if (["token", "access_token", "id_token", "refresh_token"].some((s) => key.includes(s))) return "[redacted]";
    return v;
};

const itemRow = { display: "flex", gap: 12, alignItems: "center", padding: "10px 0", borderBottom: "1px solid #eee" };
const fieldRow = { display: "flex", gap: 12, alignItems: "center", marginBottom: 10 };
const label = { width: 70, fontSize: 14, color: "#444" };
const input = { flex: 1, border: "1px solid #ddd", borderRadius: 10, padding: "10px 12px", fontSize: 14, outline: "none" };
const preBox = { background: "#f7f7f7", padding: 12, borderRadius: 8, overflow: "auto", maxHeight: 320 };
const btn = { padding: "10px 16px", borderRadius: 12, border: "1px solid #111", background: "#111", color: "#fff", cursor: "pointer" };
const btnSecondary = { ...btn, background: "#fff", color: "#111" };
const btnDanger = { ...btn, background: "#fff", color: "#c22", borderColor: "#c22" };
