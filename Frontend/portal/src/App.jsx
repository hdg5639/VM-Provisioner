import { useEffect, useState } from "react";

export default function App() {
    const [me, setMe] = useState(null);
    const [profile, setProfile] = useState(null);
    const [keys, setKeys] = useState([]);
    const [loading, setLoading] = useState(true);

    const [navBusy, setNavBusy] = useState(false);
    const [opBusy, setOpBusy] = useState(false);
    const [msg, setMsg] = useState("");
    const [form, setForm] = useState({ name: "", publicKey: "" });

    const [diagProfile, setDiagProfile] = useState(null);
    const [diagKeys, setDiagKeys] = useState(null);

    // ★ VM API 상태
    const [vmBusy, setVmBusy] = useState(false);
    const [vmJson, setVmJson] = useState(null);
    const [vmErr, setVmErr] = useState("");

    // ★ VM 생성 폼 상태
    const [vmForm, setVmForm] = useState({
        fingerprint: "",
        vmType: "",
        name: "",
        disk: "",
        ide: "",
    });

    const ORIGIN = typeof window !== "undefined" ? window.location.origin : "";

    async function safeJsonFromResponse(res) {
        const ct = (res.headers.get("content-type") || "").toLowerCase();
        const text = await res.text().catch(() => "");
        if (!text) return null;
        if (ct.includes("application/json")) {
            try { return JSON.parse(text); } catch { return null; }
        }
        const t = text.trim();
        if (t.startsWith("{") || t.startsWith("[")) {
            try { return JSON.parse(t); } catch { /* empty */ }
        }
        return null;
    }
    const safeText = async (res) => { try { return await res.text(); } catch { return ""; } };

    async function fetchDiag(url, opts = {}) {
        const res = await fetch(url, { credentials: "include", cache: "no-store", ...opts });
        const headers = Object.fromEntries(res.headers.entries());
        const ct = (headers["content-type"] || "").toLowerCase();
        const text = await res.text().catch(() => "");
        let json = null;
        if (ct.includes("application/json") && text) {
            try { json = JSON.parse(text); } catch { /* empty */ }
        }
        return { url, ok: res.ok, status: res.status, headers, contentType: ct, text, json };
    }

    async function loadMe() {
        try {
            const res = await fetch("/api/me", { credentials: "include", cache: "no-store" });
            if (res.ok) setMe((await safeJsonFromResponse(res)) ?? null);
            else setMe(null);
        } catch { setMe(null); }
        finally { setLoading(false); }
    }

    async function loadUserData() {
        if (!me) return;
        try {
            const [p, k] = await Promise.all([
                fetchDiag("/api/ds/user/users/me"),
                fetchDiag("/api/ds/user/keys"),
            ]);
            setDiagProfile(p);
            setDiagKeys(k);

            if (p.ok) {
                const pj = p.json ?? (() => { try { return JSON.parse((p.text||"").trim()); } catch { return null; } })();
                if (pj) setProfile(pj);
            } else if (p.status === 403) {
                setMsg("프로필 권한 거부(403)");
            }

            if (k.ok) {
                const kj = Array.isArray(k.json)
                    ? k.json
                    : (() => { try { const t=(k.text||"").trim(); return t ? JSON.parse(t) : []; } catch { return []; } })();
                if (Array.isArray(kj)) {
                    setKeys(kj);
                    // ★ 기본 선택: 첫 번째 키의 fingerprint
                    if (!vmForm.fingerprint && kj.length > 0 && kj[0]?.fingerprint) {
                        setVmForm(f => ({ ...f, fingerprint: kj[0].fingerprint }));
                    }
                }
            } else {
                setMsg(`키 목록 실패 (${k.status}) ${k.text?.slice(0,200)}`);
            }
        } catch (e) {
            console.error(e);
            setMsg("데이터 로딩 실패");
        }
    }

    useEffect(() => { loadMe(); }, []);
    useEffect(() => { if (me) loadUserData(); }, [me]);

    const onLogin = () => { setNavBusy(true); window.location.assign(`${ORIGIN}/auth/login`); };
    const onLogout = () => { setNavBusy(true); window.location.assign(`${ORIGIN}/logout`); };

    const createKey = async (ev) => {
        ev?.preventDefault?.();
        if (!form.name.trim() || !form.publicKey.trim()) {
            setMsg("이름과 공개키를 모두 입력하세요."); return;
        }
        setOpBusy(true); setMsg("");
        try {
            const res = await fetch("/api/ds/user/keys", {
                method: "POST", credentials: "include",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ name: form.name.trim(), publicKey: form.publicKey.trim() }),
            });
            if (res.ok) {
                const createdText = await res.text().catch(() => "");
                if (createdText) {
                    try {
                        const created = JSON.parse(createdText);
                        if (created && created.id != null) setKeys((prev) => [created, ...prev]);
                    } catch { /* empty */ }
                }
                setForm({ name: "", publicKey: "" });
                setMsg("키가 등록되었습니다.");
                await loadUserData();
            } else {
                const txt = await safeText(res);
                setMsg(`키 등록 실패 (${res.status}) ${txt}`);
            }
        } catch (err) {
            console.error(err); setMsg("키 등록 중 오류");
        } finally { setOpBusy(false); }
    };

    async function deleteKey(id) {
        if (!confirm("정말 삭제할까요?")) return;
        setOpBusy(true); setMsg("");
        try {
            const res = await fetch(`/api/ds/user/keys/${id}`, {
                method: "DELETE", credentials: "include",
            });
            if (res.ok || res.status === 204) {
                setKeys((prev) => prev.filter((k) => k.id !== id));
                setMsg("삭제되었습니다.");
            } else {
                const txt = await safeText(res);
                setMsg(`삭제 실패 (${res.status}) ${txt}`);
            }
        } catch { setMsg("삭제 중 오류"); }
        finally { setOpBusy(false); }
    }

    // ★ VM API: GET (기존)
    async function getVm() {
        setVmBusy(true); setVmErr("");
        try {
            const d = await fetchDiag("/api/ds/vm/test");
            let body = null;
            if (d.json != null) body = d.json;
            else if ((d.text || "").trim()) {
                try { body = JSON.parse((d.text || "").trim()); } catch { body = null; }
            }
            setVmJson(body);
            if (!d.ok) setVmErr(`요청 실패 (${d.status})`);
        } catch (e) {
            console.error(e); setVmErr("요청 중 오류");
        } finally { setVmBusy(false); }
    }

    // ★ VM API: POST /api/ds/vm/vm
    async function postVm(ev) {
        ev?.preventDefault?.();

        // userId는 본인 것으로 자동 채움
        const userId = profile?.id ?? me?.user?.id ?? me?.id;
        if (!userId) {
            setVmErr("userId를 확인할 수 없습니다(로그인/프로필 확인).");
            return;
        }

        // 간단 검증
        if (!vmForm.fingerprint) { setVmErr("fingerprint를 선택하세요."); return; }
        if (!vmForm.vmType || !vmForm.name || !vmForm.disk || !vmForm.ide) {
            setVmErr("vmType / name / disk / ide를 모두 입력하세요.");
            return;
        }

        setVmBusy(true); setVmErr(""); setVmJson(null);
        try {
            const res = await fetch("/api/ds/vm/vm", {
                method: "POST",
                credentials: "include",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    userId: String(userId),
                    fingerprint: vmForm.fingerprint,
                    vmType: vmForm.vmType,
                    name: vmForm.name,
                    disk: Number(vmForm.disk),
                    ide: vmForm.ide,
                }),
            });

            const text = await res.text().catch(() => "");
            let body = null;
            if (text) { try { body = JSON.parse(text); } catch { body = { raw: text }; } }
            setVmJson(body);

            if (!res.ok) setVmErr(`생성 실패 (${res.status}) ${text?.slice(0,200) || ""}`);
            else setMsg("VM 생성 요청을 보냈습니다.");
        } catch (e) {
            console.error(e);
            setVmErr("생성 요청 중 오류");
        } finally {
            setVmBusy(false);
        }
    }

    if (loading) return <div>로딩중...</div>;

    return (
        <main style={{ minHeight: "100vh", display: "grid", placeItems: "center" }}>
            <div style={{ width: 720, maxWidth: "95vw", padding: 24, borderRadius: 16, boxShadow: "0 10px 30px rgba(0,0,0,.08)" }}>
                <h1 style={{ marginTop: 0 }}>GJ 클라우드 — 포털</h1>

                {me ? (
                    <>
                        <section style={{ margin: "12px 0 16px" }}>
                            <b>환영합니다.</b>{" "}
                            <span>{me.user?.displayName || me.name || me.email}</span>
                            {profile && (
                                <span style={{ color: "#666", marginLeft: 8 }}>
                                  (ID: {profile.id}, Email: {profile.email})
                                </span>
                            )}
                        </section>

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

                        {/* ★ VM API 테스트: /api/ds/vm */}
                        <section style={{ marginBottom: 24 }}>
                            <h3 style={{ margin: "8px 0" }}>VM API 테스트</h3>

                            {/* ★ VM 생성 폼 */}
                            <form onSubmit={postVm} style={{ marginBottom: 12 }}>
                                <div style={fieldRow}>
                                    <label style={label}>userId</label>
                                    <input
                                        type="text"
                                        value={profile?.id ?? me?.user?.id ?? me?.id ?? ""}
                                        readOnly
                                        style={{ ...input, background: "#f3f3f3" }}
                                    />
                                </div>
                                <div style={fieldRow}>
                                    <label style={label}>fingerprint</label>
                                    <select
                                        value={vmForm.fingerprint}
                                        onChange={(e) => setVmForm(f => ({ ...f, fingerprint: e.target.value }))}
                                        style={input}
                                    >
                                        <option value="">선택하세요</option>
                                        {keys.map(k => (
                                            <option key={k.id} value={k.fingerprint}>
                                                {k.name} — {k.fingerprint}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                                <div style={fieldRow}>
                                    <label style={label}>vmType</label>
                                    <input
                                        type="text"
                                        placeholder="예: UBUNTU_22"
                                        value={vmForm.vmType}
                                        onChange={(e) => setVmForm(f => ({ ...f, vmType: e.target.value }))}
                                        style={input}
                                    />
                                </div>
                                <div style={fieldRow}>
                                    <label style={label}>name</label>
                                    <input
                                        type="text"
                                        placeholder="예: dj-hdg-01"
                                        value={vmForm.name}
                                        onChange={(e) => setVmForm(f => ({ ...f, name: e.target.value }))}
                                        style={input}
                                    />
                                </div>
                                <div style={fieldRow}>
                                    <label style={label}>disk(GB)</label>
                                    <input
                                        type="number"
                                        min="1"
                                        placeholder="예: 20"
                                        value={vmForm.disk}
                                        onChange={(e) => setVmForm(f => ({ ...f, disk: e.target.value }))}
                                        style={input}
                                    />
                                </div>
                                <div style={fieldRow}>
                                    <label style={label}>ide(ISO)</label>
                                    <input
                                        type="text"
                                        placeholder="예: ubuntu-22.04.5-live-server-amd64.iso"
                                        value={vmForm.ide}
                                        onChange={(e) => setVmForm(f => ({ ...f, ide: e.target.value }))}
                                        style={input}
                                    />
                                </div>
                                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                                    <button type="submit" style={btn} disabled={vmBusy || opBusy || navBusy}>
                                        {vmBusy ? "요청 중..." : "POST /api/ds/vm/vm"}
                                    </button>
                                    {vmErr && <span style={{ color: "#c22" }}>{vmErr}</span>}
                                </div>
                            </form>

                            {/* 기존 GET 테스트 버튼 유지 */}
                            <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 8 }}>
                                <button onClick={getVm} style={btn} disabled={vmBusy || opBusy || navBusy}>
                                    {vmBusy ? "요청 중..." : "GET /api/ds/vm"}
                                </button>
                            </div>

                            <pre style={preBox}>
{vmJson != null ? JSON.stringify(vmJson, null, 2) : "아직 요청하지 않았습니다."}
                            </pre>
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

                        <details style={{ marginTop: 8 }}>
                            <summary>자세히 보기 (raw state + diag)</summary>
                            <pre style={preBox}>{JSON.stringify(
                                { me, profile, keys, diag: { profile: diagProfile, keys: diagKeys } },
                                redactTokens,
                                2
                            )}</pre>
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

const redactTokens = (k, v) => {
    const key = (k || "").toLowerCase();
    if (["token", "access_token", "id_token", "refresh_token"].some((s) => key.includes(s))) return "[redacted]";
    return v;
};

const itemRow = { display: "flex", gap: 12, alignItems: "center", padding: "10px 0", borderBottom: "1px solid #eee" };
const fieldRow = { display: "flex", gap: 12, alignItems: "center", marginBottom: 10 };
const label = { width: 90, fontSize: 14, color: "#444" }; // ← userId 라벨 폭 조금 늘림
const input = { flex: 1, border: "1px solid #ddd", borderRadius: 10, padding: "10px 12px", fontSize: 14, outline: "none" };
const preBox = { background: "#f7f7f7", padding: 12, borderRadius: 8, overflow: "auto", maxHeight: 320 };
const btn = { padding: "10px 16px", borderRadius: 12, border: "1px solid #111", background: "#111", color: "#fff", cursor: "pointer" };
const btnSecondary = { ...btn, background: "#fff", color: "#111" };
const btnDanger = { ...btn, background: "#fff", color: "#c22", borderColor: "#c22" };
