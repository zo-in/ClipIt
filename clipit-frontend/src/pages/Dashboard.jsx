import { useState, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import JobHistory from '../components/JobHistory';
import api from '../api/axiosConfig';
import logo from '../assets/logo.png';

const Dashboard = () => {
  const { logout } = useContext(AuthContext);
  const navigate = useNavigate();

  // State Management
  const [url, setUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [formats, setFormats] = useState(null);
  const [error, setError] = useState(null);
  
  // Selection State
  const [selectedFormat, setSelectedFormat] = useState(null);
  const [downloadMode, setDownloadMode] = useState('default');
  
  // Trimming State
  const [enableTrim, setEnableTrim] = useState(false);
  const [trimTimes, setTrimTimes] = useState({ start: "00:00:00", end: "00:00:10" });

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const formatResolution = (resString) => {
    if (!resString) return "Unknown";
    const parts = resString.split('x');
    return parts.length === 2 ? `${parts[1]}p` : resString;
  };

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!url) return;

    setLoading(true);
    setError(null);
    setFormats(null);
    setSelectedFormat(null);
    
    setEnableTrim(false); 
    setTrimTimes({ start: "00:00:00", end: "00:00:10" });

    try {
      const response = await api.get('/jobs/formats', { params: { url: url } });
      setFormats(response.data);
    } catch (err) {
      console.error("Fetch failed:", err);
      setError("Failed to fetch video details. Please check the URL.");
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = async () => {
    if (downloadMode !== 'audio' && !selectedFormat) return;

    const formatObj = formats?.videoFormats.find(f => f.id === selectedFormat);

    const jobRequest = {
      youtubeUrl: url,
      videoId: selectedFormat || null,
      
      startTime: enableTrim ? trimTimes.start : null, 
      endTime: enableTrim ? trimTimes.end : null,
      
      resolution: formatObj ? formatObj.resolution : null, 
      format: downloadMode === 'audio' ? 'mp3' : (formatObj?.extension || 'mp4'),
      audioOnly: downloadMode === 'audio',
      videoOnly: downloadMode === 'video'
    };

    console.log("Sending Job Request:", jobRequest);
    
    try {
      const response = await api.post('/jobs/start-job', jobRequest);
      console.log("Job Started! ID:", response.data);
    } catch (err) {
      console.error("Download failed:", err);
      alert("Failed to start job. Please check the console for errors.");
    }
  };

  return (
    <div className="min-h-screen bg-gray-900 text-gray-100 font-sans">
      <nav className="flex items-center justify-between px-8 py-4 bg-gray-800 border-b border-gray-700 shadow-md">

    <div className="flex items-center gap-3">
        <img src={logo} alt="ClipIt Logo" className="h-10 w-auto" />
    </div>
        <button onClick={handleLogout} className="px-4 py-2 text-sm font-medium text-gray-300 bg-gray-700 rounded hover:bg-red-600 hover:text-white transition-all">
          Logout
        </button>
      </nav>

      <main className="container mx-auto mt-16 px-4 max-w-3xl pb-20">
        <div className="text-center mb-10">
          <h1 className="text-4xl font-extrabold text-white mb-2">Universal Media Downloader</h1>
          <p className="text-gray-400">Paste a link from YouTube.</p>
        </div>

        {/* Search Bar */}
        <div className="bg-gray-800 p-6 rounded-xl shadow-lg border border-gray-700">
          <form onSubmit={handleSearch} className="flex gap-3">
            <input
              type="text"
              placeholder="Paste video URL here..."
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              className="flex-1 px-4 py-3 bg-gray-900 border border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-white placeholder-gray-500"
            />
            <button
              type="submit"
              disabled={loading}
              className={`px-6 py-3 font-semibold text-white rounded-lg shadow-lg transition-all ${
                loading ? 'bg-gray-600 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-500'
              }`}
            >
              {loading ? 'Fetching...' : 'Fetch'}
            </button>
          </form>
          {error && <div className="mt-4 p-3 bg-red-900/50 border border-red-700 text-red-200 rounded text-sm text-center">{error}</div>}
        </div>

        {/* --- OPTIONS AREA --- */}
        {formats && (
          <div className="mt-8 bg-gray-800 p-6 rounded-xl shadow-lg border border-gray-700 animate-fade-in">
            
            {/* 1. Mode Selection */}
            <div className="mb-8">
              <h3 className="text-xl font-bold mb-4 text-white">1. Select Mode</h3>
              <div className="flex bg-gray-900 p-1 rounded-lg border border-gray-700">
                {['default', 'audio', 'video'].map((mode) => (
                  <button
                    key={mode}
                    onClick={() => setDownloadMode(mode)}
                    className={`flex-1 py-2 text-sm font-semibold rounded-md transition-all ${
                      downloadMode === mode
                        ? 'bg-blue-600 text-white shadow-md'
                        : 'text-gray-400 hover:text-white hover:bg-gray-800'
                    }`}
                  >
                    {mode === 'default' && 'Video + Audio'}
                    {mode === 'audio' && 'Audio Only'}
                    {mode === 'video' && 'Video Only'}
                  </button>
                ))}
              </div>
            </div>

            {/* 2. Quality Selection */}
            {downloadMode !== 'audio' && (
              <div className="mb-8 animate-fade-in">
                <h3 className="text-xl font-bold mb-4 text-white">2. Select Quality</h3>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                  {formats.videoFormats.map((fmt) => (
                    <button
                      key={fmt.id}
                      onClick={() => setSelectedFormat(fmt.id)}
                      className={`flex flex-col items-center justify-center p-4 rounded-lg border transition-all ${
                        selectedFormat === fmt.id
                          ? 'bg-blue-600 border-blue-500 text-white ring-2 ring-blue-400 scale-105'
                          : 'bg-gray-700 border-gray-600 text-gray-300 hover:bg-gray-600'
                      }`}
                    >
                      <span className="text-2xl font-bold tracking-tight">{formatResolution(fmt.resolution)}</span>
                      <span className="text-xs font-medium opacity-75 mt-1 uppercase">
                        {fmt.extension} • {fmt.fps} FPS
                      </span>
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* 3. Editing Options */}
            <div className="border-t border-gray-700 pt-6 mb-6">
              <div className="flex justify-between items-center mb-6">
                <h3 className="text-xl font-bold text-white">
                  {downloadMode === 'audio' ? '2. Editing Options' : '3. Editing Options'}
                </h3>
                
                <label className="inline-flex items-center cursor-pointer">
                  <span className="mr-3 text-sm font-medium text-gray-300">Enable Trimming</span>
                  <div className="relative">
                    <input 
                      type="checkbox" 
                      className="sr-only peer" 
                      checked={enableTrim} 
                      onChange={(e) => setEnableTrim(e.target.checked)}
                    />
                    {/* The Track */}
                    <div className="w-11 h-6 bg-gray-600 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-blue-500 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  </div>
                </label>
              </div>
              
              <div className={`flex flex-wrap gap-6 items-center transition-all duration-300 ${enableTrim ? 'opacity-100 transform translate-y-0' : 'opacity-30 pointer-events-none transform -translate-y-2'}`}>
                <div className="flex gap-4 items-center">
                  <div>
                    <label className="block text-xs text-gray-500 mb-1 uppercase tracking-wider">Start Time</label>
                    <input 
                      type="text" 
                      value={trimTimes.start}
                      onChange={(e) => setTrimTimes({...trimTimes, start: e.target.value})}
                      className="w-28 bg-gray-900 border border-gray-600 rounded px-3 py-2 text-center text-white focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none font-mono"
                    />
                  </div>
                  <div className="text-gray-500 font-bold pt-5">→</div>
                  <div>
                    <label className="block text-xs text-gray-500 mb-1 uppercase tracking-wider">End Time</label>
                    <input 
                      type="text" 
                      value={trimTimes.end}
                      onChange={(e) => setTrimTimes({...trimTimes, end: e.target.value})}
                      className="w-28 bg-gray-900 border border-gray-600 rounded px-3 py-2 text-center text-white focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none font-mono"
                    />
                  </div>
                </div>
                
                <div className="text-sm text-gray-500 italic mt-2 border-l-2 border-gray-700 pl-3">
                  {downloadMode === 'audio' && "Output: MP3 Audio"}
                  {downloadMode === 'video' && "Output: Video Stream Only"}
                  {downloadMode === 'default' && "Output: Standard MP4"}
                </div>
              </div>
            </div>

            {/* 4. Action Button */}
            {(selectedFormat || downloadMode === 'audio') && (
              <div className="pt-6 border-t border-gray-700">
                <button
                  onClick={handleDownload}
                  className="w-full py-4 text-lg font-bold text-white bg-green-600 rounded-lg hover:bg-green-500 shadow-lg hover:shadow-green-500/30 transition-all transform active:scale-[0.98]"
                >
                  {downloadMode === 'audio' ? 'Download Audio' : 'Start Download'}
                </button>
              </div>
            )}
            
          </div>
        )}

        <JobHistory />
      </main>
    </div>
  );
};

export default Dashboard;