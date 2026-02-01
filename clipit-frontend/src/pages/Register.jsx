import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api/axiosConfig';
import logo from '../assets/logo.png';

const Register = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: ''
  });
  
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    try {
      // 1. Send Registration Request
      await api.post('/auth/register', formData);
      
      // 2. Show Success Message
      setSuccess('Registration successful! Redirecting to login...');

      // 3. Redirect after 2 seconds so user can read the message
      setTimeout(() => {
        navigate('/login');
      }, 2000);

    } catch (err) {
      console.error("Registration failed", err);
      setError(err.response?.data || 'Registration failed. Try a different username/email.');
      setLoading(false); 
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-900 text-gray-100 font-sans">
      <div className="w-full max-w-md p-8 bg-gray-800 rounded-xl shadow-2xl border border-gray-700">
        <div className="text-center mb-8">
            <img src={logo} alt="ClipIt" className="h-16 mx-auto mb-4" />
          <h2 className="text-3xl font-bold text-blue-500">Create Account</h2>
          <p className="text-gray-400 mt-2">Join ClipIt to start downloading.</p>
        </div>
        
        {/* --- ERROR MESSAGE --- */}
        {error && (
          <div className="p-3 mb-6 text-sm text-red-200 bg-red-900/50 border border-red-700 rounded text-center animate-fade-in">
            {error}
          </div>
        )}

        {/* --- SUCCESS MESSAGE --- */}
        {success && (
          <div className="p-3 mb-6 text-sm text-green-200 bg-green-900/50 border border-green-700 rounded text-center animate-fade-in">
            {success}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Username */}
          <div>
            <label className="block text-sm font-medium text-gray-400 mb-1">Username</label>
            <input
              type="text"
              name="username"
              value={formData.username}
              onChange={handleChange}
              required
              disabled={!!success} // Disable inputs on success
              className="w-full px-4 py-2 bg-gray-900 border border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none text-white transition-all disabled:opacity-50 disabled:cursor-not-allowed"
              placeholder="johndoe"
            />
          </div>

          {/* Email */}
          <div>
            <label className="block text-sm font-medium text-gray-400 mb-1">Email Address</label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              required
              disabled={!!success}
              className="w-full px-4 py-2 bg-gray-900 border border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none text-white transition-all disabled:opacity-50 disabled:cursor-not-allowed"
              placeholder="john@example.com"
            />
          </div>

          {/* Password */}
          <div>
            <label className="block text-sm font-medium text-gray-400 mb-1">Password</label>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              required
              disabled={!!success}
              className="w-full px-4 py-2 bg-gray-900 border border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none text-white transition-all disabled:opacity-50 disabled:cursor-not-allowed"
              placeholder="••••••••"
            />
          </div>

          <button
            type="submit"
            disabled={loading || !!success}
            className={`w-full py-3 text-lg font-bold text-white rounded-lg shadow-lg transition-all transform active:scale-95 ${
              loading || success
                ? 'bg-gray-600 cursor-not-allowed scale-100' 
                : 'bg-blue-600 hover:bg-blue-500 hover:shadow-blue-500/30'
            }`}
          >
            {success ? 'Success!' : (loading ? 'Creating Account...' : 'Sign Up')}
          </button>
        </form>

        <div className="mt-6 text-center text-sm text-gray-400">
          Already have an account?{' '}
          <Link to="/login" className="text-blue-400 hover:text-blue-300 font-semibold hover:underline">
            Log in here
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Register;