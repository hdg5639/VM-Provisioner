import { useEffect, useState } from "react";
import { keycloak, initKeycloak } from "./keycloak/keycloak.jsx"; // 이전에 만든 파일 그대로 사용

export default function App() {
    const [ready, setReady] = useState(false);
    const [authed, setAuthed] = useState(false);
    const [name, setName] = useState("");
    const [error, setError] = useState("");

    useEffect(() => {
        let timer;
        (async () => {
            try {
                const authenticated = await initKeycloak(); // ★ 가드된 init: 중복 init 방지
                setAuthed(authenticated);
                if (authenticated) {
                    setName(
                        keycloak.tokenParsed?.preferred_username ||
                        keycloak.tokenParsed?.name || ""
                    );
                    // 토큰 자동 갱신
                    timer = setInterval(() => {
                        keycloak.updateToken(60).catch(() =>
                            keycloak.login({ redirectUri: window.location.origin })
                        );
                    }, 20000);
                }
            } catch (e) {
                console.error(e);
                setError(e?.message || "Keycloak init failed");
            } finally {
                setReady(true);
            }
        })();
        return () => timer && clearInterval(timer);
    }, []);

    const doLogin = () =>
        keycloak.login({ redirectUri: window.location.origin });

    // ✅ 회원가입: Keycloak의 Register 화면으로 바로 이동
    const doRegister = () =>
        keycloak.register({ redirectUri: window.location.origin });

    const doLogout = () =>
        keycloak.logout({ redirectUri: window.location.origin });

    if (!ready) return <div>로딩중…</div>;

    return (
        <main style={{display:"grid",placeItems:"center",minHeight:"100vh"}}>
            <div style={{width:420,padding:24,borderRadius:16,boxShadow:"0 10px 30px rgba(0,0,0,.08)"}}>
                <h1 style={{marginTop:0}}>GJ 클라우드 — Keycloak Test</h1>
                {error && <div style={{color:"crimson"}}>에러: {error}</div>}

                {authed ? (
                    <>
                        <div style={{margin:"12px 0"}}>안녕하세요, <b>{name}</b>님!</div>
                        <button onClick={doLogout} style={btn}>로그아웃</button>
                    </>
                ) : (
                    <>
                        <button onClick={doLogin} style={btn}>로그인</button>
                        {/* ★ 여기 추가 */}
                        <button onClick={doRegister} style={{...btn, marginTop:8}}>회원가입</button>
                    </>
                )}
            </div>
        </main>
    );
}

const btn = {
    width:"100%", padding:"12px 16px", borderRadius:12,
    border:"1px solid #111", background:"#111", color:"#fff", cursor:"pointer"
};
