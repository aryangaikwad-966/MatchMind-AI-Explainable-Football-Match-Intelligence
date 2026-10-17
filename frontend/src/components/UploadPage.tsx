import { useState } from 'react';
import {
  Upload,
  FileText,
  CheckCircle,
  AlertCircle,
  X,
  Download,
  ArrowRight,
  Database,
  Layers,
  Sparkles,
  ShieldCheck,
  Activity
} from 'lucide-react';
import apiClient from '../api/client';
import { Link } from 'react-router-dom';

const SAMPLE_CSV_CONTENT = `event_id,match_id,event_sequence,minute,second,timestamp_seconds,event_type,team_id,player_id,description,is_goal,is_disallowed,score_home_after_event,score_away_after_event,home_team_id,away_team_id,home_team_name,away_team_name
SYN-UPL-E001,SYN-UPL-02,1,0,0,0,KICK_OFF,,,Match kicks off,false,false,0,0,T-NBR,T-KNG,Northbridge FC,Kingsford United
SYN-UPL-E002,SYN-UPL-02,2,14,20,860,GOAL,T-NBR,P-101,Marford curls into top corner,true,false,1,0,T-NBR,T-KNG,Northbridge FC,Kingsford United
SYN-UPL-E003,SYN-UPL-02,3,44,10,2650,YELLOW_CARD,T-KNG,P-204,Kova booked for late challenge,false,false,1,0,T-NBR,T-KNG,Northbridge FC,Kingsford United
SYN-UPL-E004,SYN-UPL-02,4,78,35,4715,GOAL,T-KNG,P-202,Wynovan headers off corner,true,false,1,1,T-NBR,T-KNG,Northbridge FC,Kingsford United
SYN-UPL-E005,SYN-UPL-02,5,89,12,5352,GOAL,T-KNG,P-202,Wynovan stoppage time winner,true,false,1,2,T-NBR,T-KNG,Northbridge FC,Kingsford United
`;

export default function UploadPage() {
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (selectedFile) {
      setFile(selectedFile);
      setError(null);
      setResult(null);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    const droppedFile = e.dataTransfer.files[0];
    if (droppedFile) {
      if (!droppedFile.name.endsWith('.csv')) {
        setError('Please drop a valid .csv match tracking file.');
        return;
      }
      setFile(droppedFile);
      setError(null);
      setResult(null);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleUpload = async () => {
    if (!file) return;

    try {
      setUploading(true);
      setError(null);
      const response = await apiClient.uploadMatch(file);
      setResult(response);
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Upload failed. Check format and server status.');
    } finally {
      setUploading(false);
    }
  };

  const handleReset = () => {
    setFile(null);
    setResult(null);
    setError(null);
  };

  const downloadSampleCsv = () => {
    const blob = new Blob([SAMPLE_CSV_CONTENT], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'sample_matchmind_events.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const loadSampleIntoInput = () => {
    const sampleFile = new File([SAMPLE_CSV_CONTENT], 'sample_matchmind_events.csv', { type: 'text/csv' });
    setFile(sampleFile);
    setError(null);
    setResult(null);
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8 pb-12">
      {/* Header */}
      <div className="text-center space-y-3">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold bg-football-500/10 text-football-400 border border-football-500/30">
          <Database className="w-3.5 h-3.5" />
          <span>Ingestion & Validation Pipeline</span>
        </div>
        <h1 className="text-3xl sm:text-4xl font-display font-extrabold text-white tracking-tight">
          Import Match Event Stream
        </h1>
        <p className="text-sm sm:text-base text-navy-300 max-w-2xl mx-auto">
          Upload event-level tracking data to compute statistics, generate tactical turning points, and enable explainable AI analysis.
        </p>
      </div>

      {/* Feature Highlights Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="glass-panel border-navy-700/80 rounded-xl p-4 flex items-start gap-3">
          <div className="w-8 h-8 rounded-lg bg-football-500/10 border border-football-500/30 flex items-center justify-center text-football-400 flex-shrink-0">
            <Layers className="w-4 h-4" />
          </div>
          <div>
            <h4 className="text-xs font-bold text-white uppercase tracking-wider">Event Schema</h4>
            <p className="text-xs text-navy-300 mt-0.5">
              Reconstructs goals, cards, shots, fouls, and substitutions chronologically.
            </p>
          </div>
        </div>

        <div className="glass-panel border-navy-700/80 rounded-xl p-4 flex items-start gap-3">
          <div className="w-8 h-8 rounded-lg bg-purple-500/10 border border-purple-500/30 flex items-center justify-center text-purple-400 flex-shrink-0">
            <Sparkles className="w-4 h-4" />
          </div>
          <div>
            <h4 className="text-xs font-bold text-white uppercase tracking-wider">AI Integration</h4>
            <p className="text-xs text-navy-300 mt-0.5">
              Automatically indexes events for Gemini grounding and explainable Q&A.
            </p>
          </div>
        </div>

        <div className="glass-panel border-navy-700/80 rounded-xl p-4 flex items-start gap-3">
          <div className="w-8 h-8 rounded-lg bg-sky-500/10 border border-sky-500/30 flex items-center justify-center text-sky-400 flex-shrink-0">
            <ShieldCheck className="w-4 h-4" />
          </div>
          <div>
            <h4 className="text-xs font-bold text-white uppercase tracking-wider">Integrity Checks</h4>
            <p className="text-xs text-navy-300 mt-0.5">
              Calculates post-event score progression and validates chronological sequence.
            </p>
          </div>
        </div>
      </div>

      {/* Main Ingestion Box */}
      {!file && !result && (
        <div
          onDrop={handleDrop}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          className={`relative rounded-2xl p-10 sm:p-14 text-center border-2 border-dashed transition-all cursor-pointer ${
            isDragging
              ? 'border-football-400 bg-football-500/10 shadow-2xl shadow-football-900/40 scale-[1.01]'
              : 'border-navy-700 hover:border-football-500/60 glass-panel'
          }`}
        >
          <div className="relative z-10 space-y-4">
            <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-football-500/20 to-navy-800 border border-football-500/40 flex items-center justify-center mx-auto text-football-400 shadow-xl">
              <Upload className="w-8 h-8" />
            </div>

            <div>
              <p className="text-lg font-bold text-white">Drag & drop your match CSV here</p>
              <p className="text-xs text-navy-400 mt-1">Supports standard Premier League / Opta CSV schemas up to 10MB</p>
            </div>

            <div className="flex flex-wrap items-center justify-center gap-3 pt-2">
              <label className="inline-flex items-center gap-2 px-5 py-2.5 bg-football-500 hover:bg-football-600 text-white font-bold text-xs uppercase tracking-wider rounded-xl transition cursor-pointer shadow-lg shadow-football-900/30 focus-within:ring-2 focus-within:ring-football-400">
                <span>Browse Files</span>
                <input type="file" accept=".csv" onChange={handleFileSelect} className="hidden" />
              </label>

              <button
                type="button"
                onClick={loadSampleIntoInput}
                className="inline-flex items-center gap-2 px-5 py-2.5 bg-navy-800 hover:bg-navy-700 text-navy-200 hover:text-white font-semibold text-xs rounded-xl border border-navy-700 transition"
              >
                <Sparkles className="w-3.5 h-3.5 text-football-400" />
                <span>Load Sample Match CSV</span>
              </button>
            </div>

            <div className="pt-4 border-t border-navy-800 text-xs text-navy-400 flex items-center justify-center gap-2">
              <span>Need the schema format?</span>
              <button
                type="button"
                onClick={downloadSampleCsv}
                className="text-football-400 hover:text-football-300 underline font-medium inline-flex items-center gap-1"
              >
                <Download className="w-3 h-3" />
                <span>Download Sample CSV Template</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Selected File Stage */}
      {file && !result && (
        <div className="glass-panel border-navy-700/80 rounded-2xl p-6 sm:p-8 space-y-6">
          <div className="flex items-center justify-between pb-4 border-b border-navy-700/80">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-xl bg-football-500/10 border border-football-500/30 flex items-center justify-center text-football-400">
                <FileText className="w-6 h-6" />
              </div>
              <div>
                <p className="text-base font-bold text-white">{file.name}</p>
                <p className="text-xs text-navy-300">
                  {(file.size / 1024).toFixed(1)} KB • CSV Document
                </p>
              </div>
            </div>

            <button
              onClick={handleReset}
              className="p-2 text-navy-400 hover:text-white hover:bg-navy-800 rounded-xl transition"
              aria-label="Remove selected file"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <button
            onClick={handleUpload}
            disabled={uploading}
            className="w-full inline-flex items-center justify-center gap-2 px-6 py-3.5 bg-gradient-to-r from-football-500 to-emerald-600 hover:from-football-600 hover:to-emerald-700 text-white font-bold text-sm uppercase tracking-wider rounded-xl transition shadow-xl shadow-football-900/40 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {uploading ? (
              <>
                <Activity className="w-4 h-4 animate-spin" />
                <span>Parsing & Ingesting Event Stream…</span>
              </>
            ) : (
              <>
                <Upload className="w-4 h-4" />
                <span>Upload & Run Match Intelligence</span>
              </>
            )}
          </button>
        </div>
      )}

      {/* Error Card */}
      {error && (
        <div className="rounded-2xl bg-red-950/30 border border-red-500/40 p-6 space-y-4">
          <div className="flex items-start gap-3">
            <AlertCircle className="w-6 h-6 text-red-400 flex-shrink-0 mt-0.5" />
            <div>
              <p className="text-base font-bold text-red-300">Ingestion Error</p>
              <p className="text-sm text-red-200 mt-1">{error}</p>
            </div>
          </div>
          <button
            onClick={handleReset}
            className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white text-xs font-bold rounded-xl transition"
          >
            Select Another File
          </button>
        </div>
      )}

      {/* Success View */}
      {result && result.success && (
        <div className="glass-panel border-football-500/40 rounded-2xl p-6 sm:p-8 space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-navy-700/80">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-xl bg-football-500/20 border border-football-500/40 flex items-center justify-center text-football-400">
                <CheckCircle className="w-7 h-7" />
              </div>
              <div>
                <p className="text-lg font-bold text-white">Ingestion Successful</p>
                <p className="text-xs text-navy-300">
                  <strong className="text-football-400 font-mono">{result.records}</strong> events processed for Match ID:{' '}
                  <span className="font-mono text-white font-bold">{result.matchId}</span>
                </p>
              </div>
            </div>

            {result.matchId && (
              <Link
                to={`/match/${result.matchId}`}
                className="inline-flex items-center gap-2 px-5 py-2.5 bg-football-500 hover:bg-football-600 text-white text-xs font-bold uppercase tracking-wider rounded-xl transition shadow-lg shadow-football-900/30"
              >
                <span>Launch Analysis</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
            )}
          </div>

          {/* Table Preview */}
          {result.preview && result.preview.length > 0 && (
            <div className="space-y-3">
              <span className="text-xs uppercase tracking-wider font-bold text-navy-400">
                Ingested Records Preview (First 5 Rows):
              </span>
              <div className="rounded-xl overflow-x-auto border border-navy-700 bg-navy-950/70">
                <table className="w-full text-xs text-left">
                  <thead>
                    <tr className="border-b border-navy-700 bg-navy-900/90 text-navy-300 font-semibold uppercase tracking-wider text-[11px]">
                      {Object.keys(result.preview[0]).slice(0, 6).map((key) => (
                        <th key={key} className="px-4 py-2.5">
                          {key.replace(/_/g, ' ')}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {result.preview.map((row: any, idx: number) => (
                      <tr key={idx} className="border-b border-navy-800 last:border-0 hover:bg-navy-800/40 transition">
                        {Object.values(row).slice(0, 6).map((value: any, cellIdx) => (
                          <td key={cellIdx} className="px-4 py-2 font-mono text-white">
                            {String(value)}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Validation Warnings */}
          {result.errors && result.errors.length > 0 && (
            <div className="rounded-xl bg-amber-950/30 border border-amber-500/40 p-4 space-y-2">
              <span className="text-xs font-bold text-amber-400 uppercase tracking-wider">
                Data Quality & Schema Notes:
              </span>
              <ul className="text-xs text-amber-200 space-y-1">
                {result.errors.map((err: string, idx: number) => (
                  <li key={idx}>• {err}</li>
                ))}
              </ul>
            </div>
          )}

          <div className="pt-2">
            <button
              onClick={handleReset}
              className="px-4 py-2 bg-navy-800 hover:bg-navy-700 text-navy-200 hover:text-white text-xs font-semibold rounded-xl border border-navy-700 transition"
            >
              Upload Another Dataset
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
