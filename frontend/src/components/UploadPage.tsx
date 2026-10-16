import { useState } from 'react';
import { Upload, FileText, CheckCircle, AlertCircle, X } from 'lucide-react';
import apiClient from '../api/client';
import { Link } from 'react-router-dom';

export default function UploadPage() {
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

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
    const droppedFile = e.dataTransfer.files[0];
    if (droppedFile) {
      setFile(droppedFile);
      setError(null);
      setResult(null);
    }
  };

  const handleUpload = async () => {
    if (!file) return;

    try {
      setUploading(true);
      setError(null);
      const response = await apiClient.uploadMatch(file);
      setResult(response);
    } catch (err: any) {
      setError(err.response?.data?.error || 'Upload failed');
    } finally {
      setUploading(false);
    }
  };

  const handleReset = () => {
    setFile(null);
    setResult(null);
    setError(null);
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-white">Upload Match Data</h2>
        <p className="text-navy-300 mt-1">Upload a CSV file containing match events for analysis</p>
      </div>

      {/* Upload Area */}
      {!file && (
        <div
          onDrop={handleDrop}
          onDragOver={(e) => e.preventDefault()}
          className="border-2 border-dashed border-navy-700 rounded-lg p-12 text-center hover:border-football-500 transition cursor-pointer"
        >
          <Upload className="w-16 h-16 text-navy-500 mx-auto mb-4" />
          <p className="text-lg text-white mb-2">Drag and drop your CSV file here</p>
          <p className="text-sm text-navy-400 mb-4">or</p>
          <label className="inline-block px-6 py-2 bg-football-500 text-white rounded-lg hover:bg-football-600 transition cursor-pointer">
            Browse Files
            <input
              type="file"
              accept=".csv"
              onChange={handleFileSelect}
              className="hidden"
            />
          </label>
          <p className="text-xs text-navy-500 mt-4">Maximum file size: 10MB</p>
        </div>
      )}

      {/* File Selected */}
      {file && !result && (
        <div className="bg-navy-900 border border-navy-800 rounded-lg p-6">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-3">
              <FileText className="w-8 h-8 text-football-500" />
              <div>
                <p className="text-white font-semibold">{file.name}</p>
                <p className="text-sm text-navy-400">{(file.size / 1024).toFixed(2)} KB</p>
              </div>
            </div>
            <button
              onClick={handleReset}
              className="p-2 hover:bg-navy-800 rounded-lg transition"
            >
              <X className="w-5 h-5 text-navy-400" />
            </button>
          </div>
          <button
            onClick={handleUpload}
            disabled={uploading}
            className="w-full px-6 py-3 bg-football-500 text-white rounded-lg hover:bg-football-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {uploading ? 'Uploading...' : 'Upload and Analyze'}
          </button>
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="bg-red-900/20 border border-red-800 rounded-lg p-6">
          <div className="flex items-center space-x-3">
            <AlertCircle className="w-6 h-6 text-red-400" />
            <div>
              <p className="text-red-400 font-semibold">Upload Failed</p>
              <p className="text-red-300">{error}</p>
            </div>
          </div>
          <button
            onClick={handleReset}
            className="mt-4 px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition"
          >
            Try Again
          </button>
        </div>
      )}

      {/* Success */}
      {result && result.success && (
        <div className="bg-navy-900 border border-navy-800 rounded-lg p-6">
          <div className="flex items-center space-x-3 mb-6">
            <CheckCircle className="w-6 h-6 text-football-500" />
            <div>
              <p className="text-football-400 font-semibold">Upload Successful</p>
              <p className="text-white">{result.records} records imported for match {result.matchId}</p>
            </div>
          </div>

          {result.preview && result.preview.length > 0 && (
            <div className="mb-6">
              <p className="text-sm text-navy-400 mb-3">Preview (first 5 records):</p>
              <div className="bg-navy-800 rounded-lg overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-navy-700">
                      {Object.keys(result.preview[0]).slice(0, 5).map((key) => (
                        <th key={key} className="px-4 py-2 text-left text-navy-400 font-semibold">
                          {key}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {result.preview.map((row: any, idx: number) => (
                      <tr key={idx} className="border-b border-navy-700 last:border-0">
                        {Object.values(row).slice(0, 5).map((value: any, cellIdx) => (
                          <td key={cellIdx} className="px-4 py-2 text-white">
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

          {result.errors && result.errors.length > 0 && (
            <div className="bg-yellow-900/20 border border-yellow-800 rounded-lg p-4 mb-6">
              <p className="text-yellow-400 font-semibold mb-2">Validation Warnings:</p>
              <ul className="text-sm text-yellow-300 space-y-1">
                {result.errors.map((err: string, idx: number) => (
                  <li key={idx}>• {err}</li>
                ))}
              </ul>
            </div>
          )}

          <button
            onClick={handleReset}
            className="px-6 py-2 bg-navy-700 text-white rounded-lg hover:bg-navy-600 transition"
          >
            Upload Another File
          </button>
          {result.matchId && <Link to={`/match/${result.matchId}`} className="ml-3 inline-block px-6 py-2 bg-football-500 text-white rounded-lg hover:bg-football-600 transition">Open analysis</Link>}
        </div>
      )}
    </div>
  );
}
