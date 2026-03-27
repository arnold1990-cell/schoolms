import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export function LoginPage() {
  const { login } = useAuth();
  const nav = useNavigate();
  const [email, setEmail] = useState('admin@schoolms.com');
  const [password, setPassword] = useState('Admin123!');
  const [error, setError] = useState('');

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    try { await login(email, password); nav('/dashboard'); }
    catch { setError('Invalid credentials'); }
  };

  return <form className="login" onSubmit={submit}>
    <h2>SchoolMS Login</h2>
    <p>Only ADMIN and TEACHER access.</p>
    <input value={email} onChange={e => setEmail(e.target.value)} placeholder="Email" style={{width:'100%', marginBottom:8}} />
    <input type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="Password" style={{width:'100%', marginBottom:8}} />
    {error && <p style={{color:'red'}}>{error}</p>}
    <button style={{width:'100%'}}>Sign In</button>
  </form>;
}
