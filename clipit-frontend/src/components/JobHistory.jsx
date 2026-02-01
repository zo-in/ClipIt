import { useEffect, useState } from 'react';
import api from '../api/axiosConfig';

const JobHistory = () => {
  const [jobs, setJobs] = useState([]);

  // Function to fetch the latest job list
  const fetchJobs = async () => {
    try {
      const response = await api.get('/jobs');
      // Reverse array to show newest jobs at the top
      setJobs(response.data.slice().reverse());
    } catch (error) {
      console.error("Error fetching jobs:", error);
    }
  };

  // Poll the server every 2 seconds
  useEffect(() => {
    fetchJobs(); // Initial fetch
    const interval = setInterval(fetchJobs, 2000); 
    return () => clearInterval(interval); // Cleanup on unmount
  }, []);

  // Handle the physical file download
  const handleDownloadFile = async (externalId, originalUrl) => {
    try {
      const response = await api.get(`/jobs/download/${externalId}`, {
        responseType: 'blob', // Important: Tell Axios this is a file, not JSON
      });
      
      // Create a temporary link to trigger the browser's download dialog
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      
      // Try to name the file based on the original URL or ID
      const suggestedName = originalUrl ? `clipit-${originalUrl.slice(-10)}.mp4` : `clipit-${externalId}.mp4`;
      link.setAttribute('download', suggestedName);
      
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      console.error("Download failed", err);
      alert("Error downloading file. It might not be ready yet.");
    }
  };

  // Helper to determine badge color
  const getStatusColor = (status) => {
    switch (status) {
      case 'COMPLETED': return 'text-green-400 bg-green-900/30 border-green-700';
      case 'FAILED': return 'text-red-400 bg-red-900/30 border-red-700';
      case 'DOWNLOADING': return 'text-blue-400 bg-blue-900/30 border-blue-700';
      case 'PROCESSING': return 'text-yellow-400 bg-yellow-900/30 border-yellow-700';
      default: return 'text-gray-400 bg-gray-800 border-gray-600';
    }
  };

  return (
    <div className="mt-12 w-full max-w-3xl animate-fade-in pb-20">
      <h3 className="text-xl font-bold text-white mb-6 border-b border-gray-700 pb-2">
        Your Downloads
      </h3>
      
      <div className="space-y-4">
        {jobs.map((job) => (
          <div key={job.externalId} className="bg-gray-800 p-5 rounded-lg border border-gray-700 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            
            {/* Left: Job Info */}
            <div className="flex-1 w-full min-w-0">
              <div className="flex justify-between items-start">
                <p className="text-white font-medium truncate w-3/4" title={job.originalUrl}>
                  {job.originalUrl || "Unknown Source"}
                </p>
                <span className={`text-[10px] font-bold px-2 py-0.5 rounded border ${getStatusColor(job.status)}`}>
                  {job.status}
                </span>
              </div>
              
              <div className="text-xs text-gray-400 mt-1">
                ID: {job.externalId.slice(0, 8)}...
              </div>
              
              {/* Live Progress Bar (Only for active jobs) */}
              {(job.status === 'DOWNLOADING' || job.status === 'PROCESSING') && (
                <div className="mt-3">
                  <div className="flex justify-between text-xs text-gray-400 mb-1">
                    <span>{job.status === 'DOWNLOADING' ? 'Downloading...' : 'Converting...'}</span>
                    <span>{job.progress}%</span>
                  </div>
                  <div className="w-full bg-gray-700 rounded-full h-1.5 overflow-hidden">
                    <div 
                      className={`h-1.5 rounded-full transition-all duration-500 ${job.status === 'DOWNLOADING' ? 'bg-blue-500' : 'bg-yellow-500'}`}
                      style={{ width: `${job.progress}%` }} 
                    ></div>
                  </div>
                </div>
              )}
            </div>

            {/* Right: Action Button */}
            <div className="sm:self-center w-full sm:w-auto">
              {job.status === 'COMPLETED' ? (
                <button
                  onClick={() => handleDownloadFile(job.externalId, job.originalUrl)}
                  className="w-full sm:w-auto px-5 py-2 bg-green-600 hover:bg-green-500 text-white text-sm font-bold rounded-lg shadow-lg hover:shadow-green-500/20 transition-all flex items-center justify-center gap-2"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                  Save File
                </button>
              ) : (
                 job.status === 'FAILED' ? (
                    <button className="text-red-500 text-sm font-medium cursor-not-allowed opacity-75">
                        Failed
                    </button>
                 ) : (
                    <div className="text-center w-full sm:w-24">
                        <span className="text-2xl font-bold text-gray-500">{job.progress}%</span>
                    </div>
                 )
              )}
            </div>

          </div>
        ))}

        {jobs.length === 0 && (
          <div className="text-center py-10 text-gray-500 border-2 border-dashed border-gray-700 rounded-xl">
            <p>No downloads yet.</p>
            <p className="text-sm">Paste a URL above to get started!</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default JobHistory;