import { useEffect, useState } from "react";
import { keycloak, initKeycloak } from "./keycloak/keycloak.jsx";

export default function App() {
    const [ready, setReady] = useState(false);
    const [authed, setAuthed] = useState(false);
    const [name, setName] = useState("");
    const [error, setError] = useState("");

    useEffect(() => {
        let t;

        keycloak.onAuthSuccess = () => {
            setAuthed(true);
            setName(
                keycloak.tokenParsed?.preferred_username ||
                keycloak.tokenParsed?.name || ""
            );
        };
        keycloak.onAuthLogout = () => setAuthed(false);

        (async () => {
            try {
                const authenticated = await initKeycloak({ timeoutMs: 8000 });
                setAuthed(authenticated);
                if (authenticated) {
                    setName(
                        keycloak.tokenParsed?.preferred_username ||
                        keycloak.tokenParsed?.name || ""
                    );
                    t = setInterval(() => {
                        keycloak.updateToken(60).catch(() =>
                            setAuthed(false)
                        );
                    }, 20000);
                }
            } catch (e) {
                setError(e?.message || "Keycloak init failed");
            } finally {
                setReady(true);
            }
        })();

        return () => t && clearInterval(t);
    }, []);

    const doLogin = () =>
        keycloak.login({ redirectUri: window.location.href });

    const doRegister = () =>
        keycloak.register({ redirectUri: window.location.href });

    const doLogout = () =>
        // 로그아웃 후에도 메인에 머물도록 홈으로 귀환
        keycloak.logout({ redirectUri: window.location.origin });

    if (!ready) return <div>로딩중...</div>;

    return (
        <main style={{minHeight:"100vh",display:"grid",placeItems:"center"}}>
            <div style={{width:420,padding:24,borderRadius:16,boxShadow:"0 10px 30px rgba(0,0,0,.08)"}}>
                <h1 style={{marginTop:0}}>GJ 클라우드 — Keycloak Test</h1>

                {error && (
                    <div style={{background:"#fff3f3",border:"1px solid #fca5a5",color:"#b91c1c",padding:12,borderRadius:12,marginBottom:16}}>
                        초기화 에러: {error}
                    </div>
                )}

                {authed ? (
                    <>
                        <div style={{margin:"12px 0"}}>안녕하세요, <b>{name}</b>님!</div>
                        <button onClick={doLogout} style={btn}>로그아웃</button>
                    </>
                ) : (
                    <>
                        {}
                        <p style={{color:"#666"}}>로그인하지 않아도 이 화면에서 대기합니다.</p>
                        <button onClick={doLogin} style={btn}>로그인</button>
                        <button onClick={doRegister} style={{...btn,marginTop:8}}>회원가입</button>
                    </>
                )}
            </div>
        </main>
    );
}

const btn = {
    width:"100%",padding:"12px 16px",borderRadius:12,
    border:"1px solid #111",background:"#111",color:"#fff",cursor:"pointer"
};
