import { useState } from 'react';
import axiosInstance from '../api/axiosInstance';
import { useNavigate } from 'react-router-dom';
import './LoginForm.css';

function LoginForm() {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({ email: '', password: '' });
    const handleSocialLogin = (provider) => {
        window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`;
      };
    const [loading, setLoading] = useState(false);
    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            const response = await axiosInstance.post('/auth/login', formData);

            // 1. Store the Access Token securely
            localStorage.setItem('accessToken', response.data.accessToken);

            // 2. Redirect the user
            console.log('Login successful, token stored.');
            navigate('/dashboard');

        } catch (error) {
            console.error('Login failed:', error);
            alert('Invalid credentials.');
        } finally {
            // This ensures it turns off whether the login succeeded OR failed
            setLoading(false);
        }
    };

    return (
        <form className="login-form" onSubmit={handleSubmit}>
            <h2>Sign In</h2>
            <input
                type="email" placeholder="Email" required
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
            />
            <input
                type="password" placeholder="Password" required
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
            />
            <button type="submit" disabled={loading}>
                {loading ? 'Authenticating...' : 'Continue'}
            </button>
            <div className="social-login-container">
                <p className="divider">OR</p>
                <button 
                    type="button" 
                    className="social-btn google-btn" 
                    onClick={() => window.location.href = 'http://localhost:8083/oauth2/authorization/google'}
                >
                    Login with Google
                </button>
                <button 
                    type="button" 
                    className="social-btn github-btn" 
                    onClick={() => window.location.href = 'http://localhost:8083/oauth2/authorization/github'}
                >
                    Login with GitHub
                </button>
            </div>
            <p style={{ marginTop: '1rem', color: '#888' }}>
                Don't have an account? <a href="/register" style={{ color: '#fff' }}>Sign up here</a>
            </p>

        </form>
        
    );
}

export default LoginForm;