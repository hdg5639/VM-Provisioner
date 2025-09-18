import { useEffect, useState } from "react";

export default function App() {
    const [me, setMe] = useState(null);
    const [loading, setLoading] = useState(true);

    async function loadMe() {
        try {
            const res = await fetch("/api/me", { credentials: "include" });
            if (res.ok) {
                const data = await res.json();
                setMe(data);
            } else {
                setMe(null); // 401/403이면 비로그인 상태로 처리
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

    const onLogin = () => {
        window.location.href = "/oauth2/authorization/keycloak";
    };

    const onLogout = () => {
        window.location.href = "/logout";
    };

    if (loading) return <div>로딩중...</div>;

    return (
        <main style={{minHeight:"100vh",display:"grid",placeItems:"center"}}>
            <div style={{width:420,padding:24,borderRadius:16,boxShadow:"0 10px 30px rgba(0,0,0,.08)"}}>
                <h1 style={{marginTop:0}}>GJ 클라우드 — 포털</h1>

                {me ? (
                    <>
                        <div style={{margin:"12px 0"}}>
                            안녕하세요, <b>{me.user?.displayName || me.name || me.email}</b>님!
                        </div>
                        <pre style={{background:"#f7f7f7", padding:12, borderRadius:8, overflow:"auto"}}>
              {JSON.stringify(me, null, 2)}
            </pre>
                        <button onClick={onLogout} style={btn}>로그아웃</button>
                    </>
                ) : (
                    <>
                        <p style={{color:"#666"}}>비로그인 상태입니다. 메인에서 대기합니다.</p>
                        <button onClick={onLogin} style={btn}>로그인</button>
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
