import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import { Trophy, BarChart3, Upload, Brain } from 'lucide-react';
import MatchList from './components/MatchList';
import MatchDetail from './components/MatchDetail';
import UploadPage from './components/UploadPage';

function App() {
  return (
    <Router>
      <div className="min-h-screen bg-navy-950">
        {/* Header */}
        <header className="bg-navy-900 border-b border-navy-800">
          <div className="max-w-7xl mx-auto px-4 py-4">
            <div className="flex items-center justify-between">
              <Link to="/" className="flex items-center space-x-3">
                <Trophy className="w-8 h-8 text-football-500" />
                <div>
                  <h1 className="text-xl font-bold text-white">MatchMind AI</h1>
                  <p className="text-xs text-navy-300">Explainable Football Match Intelligence</p>
                </div>
              </Link>
              <nav className="flex items-center space-x-6">
                <Link to="/" className="flex items-center space-x-2 text-navy-300 hover:text-white transition">
                  <BarChart3 className="w-5 h-5" />
                  <span>Matches</span>
                </Link>
                <Link to="/upload" className="flex items-center space-x-2 text-navy-300 hover:text-white transition">
                  <Upload className="w-5 h-5" />
                  <span>Upload</span>
                </Link>
              </nav>
            </div>
          </div>
        </header>

        {/* Main Content */}
        <main className="max-w-7xl mx-auto px-4 py-8">
          <Routes>
            <Route path="/" element={<MatchList />} />
            <Route path="/match/:matchId" element={<MatchDetail />} />
            <Route path="/upload" element={<UploadPage />} />
          </Routes>
        </main>

        {/* Footer */}
        <footer className="bg-navy-900 border-t border-navy-800 mt-12">
          <div className="max-w-7xl mx-auto px-4 py-6">
            <div className="flex items-center justify-between text-sm text-navy-400">
              <div className="flex items-center space-x-2">
                <Brain className="w-4 h-4" />
                <span>Powered by Synthetic Data • Microsoft × Premier League Hackathon</span>
              </div>
              <span>SYNTHETIC DEMO DATA — Not affiliated with real leagues</span>
            </div>
          </div>
        </footer>
      </div>
    </Router>
  );
}

export default App;
