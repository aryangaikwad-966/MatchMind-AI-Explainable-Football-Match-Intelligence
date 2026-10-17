import { BrowserRouter as Router, Routes, Route, Link, useLocation } from 'react-router-dom';
import { Trophy, BarChart3, Upload, Brain, Sparkles } from 'lucide-react';
import MatchList from './components/MatchList';
import MatchDetail from './components/MatchDetail';
import UploadPage from './components/UploadPage';

function Navigation() {
  const location = useLocation();

  const isMatches = location.pathname === '/' || location.pathname.startsWith('/match');
  const isUpload = location.pathname === '/upload';

  return (
    <header className="sticky top-0 z-40 bg-navy-950/85 backdrop-blur-xl border-b border-navy-700/80 shadow-lg shadow-black/20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3.5">
        <div className="flex items-center justify-between">
          {/* Logo & Brand */}
          <Link to="/" className="flex items-center space-x-3 group focus-visible:ring-2 focus-visible:ring-football-400 rounded-xl p-1 -m-1">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-football-500 via-football-600 to-navy-900 p-0.5 shadow-glow-green group-hover:scale-105 transition-transform duration-200 flex items-center justify-center">
              <div className="w-full h-full bg-navy-950 rounded-[10px] flex items-center justify-center">
                <Trophy className="w-5 h-5 text-football-400 group-hover:text-football-300 transition-colors" />
              </div>
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h1 className="text-xl font-display font-extrabold tracking-tight text-white group-hover:text-football-300 transition-colors">
                  MatchMind <span className="text-football-400 font-bold">AI</span>
                </h1>
                <span className="hidden sm:inline-flex items-center gap-1 text-[10px] uppercase tracking-wider font-semibold px-2 py-0.5 rounded-full bg-football-500/10 text-football-300 border border-football-500/30">
                  <Sparkles className="w-2.5 h-2.5 text-football-400" /> Hackathon Edition
                </span>
              </div>
              <p className="text-xs text-navy-200 hidden sm:block font-medium">Explainable Football Match Intelligence</p>
            </div>
          </Link>

          {/* Navigation Links & Engine Badge */}
          <div className="flex items-center space-x-3 sm:space-x-5">
            <nav className="flex items-center bg-navy-900/90 border border-navy-700/80 rounded-xl p-1 shadow-inner" aria-label="Main navigation">
              <Link
                to="/"
                className={`flex items-center space-x-2 px-3.5 py-1.5 rounded-lg text-sm font-medium transition-all duration-200 focus-visible:ring-2 focus-visible:ring-football-400 ${
                  isMatches
                    ? 'bg-football-500 text-white shadow-md shadow-football-900/40'
                    : 'text-navy-200 hover:text-white hover:bg-navy-800'
                }`}
              >
                <BarChart3 className="w-4 h-4" />
                <span>Matches</span>
              </Link>
              <Link
                to="/upload"
                className={`flex items-center space-x-2 px-3.5 py-1.5 rounded-lg text-sm font-medium transition-all duration-200 focus-visible:ring-2 focus-visible:ring-football-400 ${
                  isUpload
                    ? 'bg-football-500 text-white shadow-md shadow-football-900/40'
                    : 'text-navy-200 hover:text-white hover:bg-navy-800'
                }`}
              >
                <Upload className="w-4 h-4" />
                <span>Upload CSV</span>
              </Link>
            </nav>

            {/* Live Engine Status */}
            <div className="hidden md:flex items-center space-x-2 text-xs text-navy-200 bg-navy-900/60 border border-navy-700/60 px-3 py-1.5 rounded-xl">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-football-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-football-500"></span>
              </span>
              <span className="font-mono text-navy-300">Engine Active</span>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}

function App() {
  return (
    <Router future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <div className="min-h-screen flex flex-col bg-navy-950 text-gray-100">
        {/* Skip link */}
        <a
          href="#main-content"
          className="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-50 focus:bg-football-500 focus:text-white focus:px-4 focus:py-2 focus:rounded-lg focus:shadow-xl focus:font-semibold"
        >
          Skip to main content
        </a>

        {/* Header */}
        <Navigation />

        {/* Main Content */}
        <main id="main-content" className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <Routes>
            <Route path="/" element={<MatchList />} />
            <Route path="/match/:matchId" element={<MatchDetail />} />
            <Route path="/upload" element={<UploadPage />} />
          </Routes>
        </main>

        {/* Footer */}
        <footer className="bg-navy-950/90 border-t border-navy-800 mt-auto py-8 text-sm">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex flex-col md:flex-row items-center justify-between gap-4">
              <div className="flex items-center space-x-3">
                <div className="w-8 h-8 rounded-lg bg-navy-800 border border-navy-700 flex items-center justify-center">
                  <Brain className="w-4 h-4 text-football-400" />
                </div>
                <div>
                  <p className="font-semibold text-white">MatchMind AI Match Intelligence</p>
                  <p className="text-xs text-navy-300">Built for Microsoft × Premier League "Inside the Game" Hackathon</p>
                </div>
              </div>
              <div className="flex items-center gap-3 text-xs text-navy-300">
                <span className="px-2.5 py-1 rounded-md bg-navy-900 border border-navy-700/80 font-mono">
                  Synthetic Dataset v1.0
                </span>
                <span className="text-navy-400 hidden sm:inline">•</span>
                <span className="text-navy-400">Not affiliated with official clubs or leagues</span>
              </div>
            </div>
          </div>
        </footer>
      </div>
    </Router>
  );
}

export default App;
