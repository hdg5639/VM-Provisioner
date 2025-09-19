import { useEffect, useState } from "react";

export default function App() {
    const [me, setMe] = useState(null);         // BFF /api/me (세션 체크용)
    const [profile, setProfile] = useState(null); // user-service /users/me
    const [keys, setKeys] = useState([]);         // user-service /keys
    const [loading, setLoading] = useState(true);
    const [navBusy, setNavBusy] = useState(false);
    const [opBusy, setOpBusy] = useState(false);  // 키 등록/삭제 중
    const [msg, setMsg] = useState("");
    const [form, setForm] = useState({ name: "", publicKey: "" });

    const ORIGIN = typeof window !== "undefined" ? window.location.origin : "";

    // BFF 세션 체크
    async function loadMe() {
        try {
            const res = await fetch("/api/me", { credentials: "include", cache: "no-store" });
            if (res.ok) setMe(await res.json());
            else setMe(null);
        } catch {
            setMe(null);
        } finally {
            setLoading(false);
        }
    }

    // user-service 데이터 로딩
    async function loadUserData() {
        if (!me) return;
        try {
            const [pRes, kRes] = await Promise.all([
                fetch("/api/ds/user/users/me", { credentials: "include", cache: "no-store" }),
                fetch("/api/ds/user/keys", { credentials: "include", cache: "no-store" }),
            ]);
            if (pRes.ok) setProfile(await pRes.json());
            if (kRes.ok) setKeys(await kRes.json());
        } catch (e) {
            console.error(e);
            setMsg("데이터 로딩 실패");
        }
    }

    useEffect(() => { loadMe(); }, []);
    useEffect(() => { if (me) loadUserData(); }, [me]);

    const onLogin = () => {
        setNavBusy(true);
        window.location.assign(`${ORIGIN}/auth/login`);
    };
    const onLogout = () => {
        setNavBusy(true);
        window.location.assign(`${ORIGIN}/logout`);
    };

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
                setForm({ name: "", publicKey: "" });
                await loadUserData();
                setMsg("키가 등록되었습니다.");
            } else {
                const txt = await safeText(res);
                setMsg(`키 등록 실패 (${res.status}) ${txt}`);
            }
        } catch (err) {
            console.error(err); // ← err를 사용하면 no-unused-vars 해소
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
                setKeys(prev => prev.filter(k => k.id !== id));
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
                                    {keys.map(k => (
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
                                        onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                                        placeholder="예: macbook"
                                        style={input}
                                        disabled={opBusy}
                                    />
                                </div>
                                <div style={fieldRow}>
                                    <label style={label}>공개키</label>
                                    <textarea
                                        value={form.publicKey}
                                        onChange={e => setForm(f => ({ ...f, publicKey: e.target.value }))}
                                        placeholder="ssh-ed25519 AAAAC3... user@host"
                                        style={{ ...input, height: 96 }}
                                        disabled={opBusy}
                                    />
                                </div>
                                <div style={{ display: "flex", gap: 8 }}>
                                    <button type="submit" style={btn} disabled={opBusy}>
                                        {opBusy ? "등록 중..." : "등록"}
                                    </button>
                                    <button type="button" style={btnSecondary} onClick={() => setForm({ name: "", publicKey: "" })} disabled={opBusy}>
                                        초기화
                                    </button>
                                    <div style={{ flex: 1 }} />
                                    <button onClick={onLogout} style={btn} disabled={navBusy}>{navBusy ? "이동 중..." : "로그아웃"}</button>
                                </div>
                            </form>
                        </section>

                        {/* (디버그용) 원본 보기 토글 */}
                        <details style={{ marginTop: 8 }}>
                            <summary>자세히 보기 (디버그)</summary>
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

const safeText = async (res) => {
    try { return await res.text(); } catch { return ""; }
};

const redactTokens = (k, v) => {
    const key = (k || "").toLowerCase();
    if (["token", "access_token", "id_token", "refresh_token"].some(s => key.includes(s))) return "[redacted]";
    return v;
};

const itemRow = { display: "flex", gap: 12, alignItems: "center", padding: "10px 0", borderBottom: "1px solid #eee" };
const fieldRow = { display: "flex", gap: 12, alignItems: "center", marginBottom: 10 };
const label = { width: 70, fontSize: 14, color: "#444" };
const input = { flex: 1, border: "1px solid #ddd", borderRadius: 10, padding: "10px 12px", fontSize: 14, outline: "none" };
const preBox = { background: "#f7f7f7", padding: 12, borderRadius: 8, overflow: "auto", maxHeight: 280 };
const btn = { padding: "10px 16px", borderRadius: 12, border: "1px solid #111", background: "#111", color: "#fff", cursor: "pointer" };
const btnSecondary = { ...btn, background: "#fff", color: "#111" };
const btnDanger = { ...btn, background: "#fff", color: "#c22", borderColor: "#c22" };
