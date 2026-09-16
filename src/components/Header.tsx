import React, { useRef, useState } from 'react';
import {
  Undo2,
  Redo2,
  Download,
  FolderOpen,
  Eye,
  RotateCcw,
  Smartphone,
  Sparkles,
  ChevronDown,
  Check
} from 'lucide-react';
import { SAMPLE_IMAGES } from '../utils/imagePipeline';

interface HeaderProps {
  canUndo: boolean;
  canRedo: boolean;
  onUndo: () => void;
  onRedo: () => void;
  onReset: () => void;
  isCompareMode: boolean;
  setIsCompareMode: (v: boolean) => void;
  onImageLoaded: (img: HTMLImageElement, name: string) => void;
  onExport: (format: 'png' | 'jpeg', quality: number) => void;
  onOpenApkModal: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  canUndo,
  canRedo,
  onUndo,
  onRedo,
  onReset,
  isCompareMode,
  setIsCompareMode,
  onImageLoaded,
  onExport,
  onOpenApkModal,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [showSamples, setShowSamples] = useState(false);
  const [showExportMenu, setShowExportMenu] = useState(false);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = () => {
        onImageLoaded(img, file.name);
      };
      img.src = event.target?.result as string;
    };
    reader.readAsDataURL(file);
  };

  const loadSample = (sample: { name: string; url: string }) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      onImageLoaded(img, sample.name);
      setShowSamples(false);
    };
    img.src = sample.url;
  };

  return (
    <header className="h-14 bg-zinc-900 border-b border-zinc-800 px-4 flex items-center justify-between select-none z-30 shrink-0">
      {/* Left: Branding & Native Info */}
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-amber-500 to-indigo-500 flex items-center justify-center shadow-md shadow-indigo-500/20">
            <Sparkles className="w-4 h-4 text-white" />
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <span className="font-bold tracking-tight text-zinc-100 text-sm">HyperEditor</span>
              <span className="text-[10px] uppercase font-semibold px-1.5 py-0.5 rounded bg-zinc-800 text-amber-400 border border-zinc-700">Pro</span>
            </div>
            <p className="text-[10px] text-zinc-400 hidden sm:block">Pipeline No Destructivo para Android & Web</p>
          </div>
        </div>

        <button
          id="btn-open-apk-hub"
          onClick={onOpenApkModal}
          className="hidden md:flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium bg-emerald-950/80 border border-emerald-600/40 text-emerald-300 hover:bg-emerald-900/60 transition-colors"
          title="Ver cómo compilar o descargar el APK nativo para Android"
        >
          <Smartphone className="w-3.5 h-3.5 text-emerald-400" />
          <span>Descargar APK Android</span>
        </button>
      </div>

      {/* Middle: Undo / Redo / Compare / Reset */}
      <div className="flex items-center gap-1 bg-zinc-950/80 p-1 rounded-lg border border-zinc-800/80">
        <button
          id="btn-undo"
          onClick={onUndo}
          disabled={!canUndo}
          className={`p-1.5 rounded-md transition-colors ${
            canUndo
              ? 'text-zinc-200 hover:bg-zinc-800 hover:text-white'
              : 'text-zinc-600 cursor-not-allowed'
          }`}
          title="Deshacer (Undo)"
        >
          <Undo2 className="w-4 h-4" />
        </button>

        <button
          id="btn-redo"
          onClick={onRedo}
          disabled={!canRedo}
          className={`p-1.5 rounded-md transition-colors ${
            canRedo
              ? 'text-zinc-200 hover:bg-zinc-800 hover:text-white'
              : 'text-zinc-600 cursor-not-allowed'
          }`}
          title="Rehacer (Redo)"
        >
          <Redo2 className="w-4 h-4" />
        </button>

        <div className="w-px h-4 bg-zinc-800 mx-1" />

        <button
          id="btn-compare"
          onMouseDown={() => setIsCompareMode(true)}
          onMouseUp={() => setIsCompareMode(false)}
          onMouseLeave={() => setIsCompareMode(false)}
          onTouchStart={() => setIsCompareMode(true)}
          onTouchEnd={() => setIsCompareMode(false)}
          className={`flex items-center gap-1 px-2.5 py-1 rounded-md text-xs font-medium transition-all ${
            isCompareMode
              ? 'bg-amber-500 text-zinc-950 font-semibold'
              : 'text-zinc-300 hover:bg-zinc-800'
          }`}
          title="Mantén pulsado para comparar con la foto original"
        >
          <Eye className="w-3.5 h-3.5" />
          <span className="hidden sm:inline">Original</span>
        </button>

        <button
          id="btn-reset-doc"
          onClick={onReset}
          className="p-1.5 rounded-md text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 transition-colors"
          title="Restablecer todas las ediciones"
        >
          <RotateCcw className="w-4 h-4" />
        </button>
      </div>

      {/* Right: File Actions & Export */}
      <div className="flex items-center gap-2">
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          className="hidden"
          onChange={handleFileChange}
        />

        {/* Samples Selector Dropdown */}
        <div className="relative">
          <button
            id="btn-toggle-samples"
            onClick={() => setShowSamples(!showSamples)}
            className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-md text-xs font-medium text-zinc-300 hover:text-white bg-zinc-800/80 hover:bg-zinc-800 border border-zinc-700 transition-colors"
          >
            <span>Ejemplos</span>
            <ChevronDown className="w-3 h-3 text-zinc-400" />
          </button>

          {showSamples && (
            <div className="absolute right-0 mt-1.5 w-56 bg-zinc-900 border border-zinc-700 rounded-lg shadow-xl py-1 z-50 animate-in fade-in zoom-in-95 duration-100">
              <div className="px-3 py-1.5 text-[11px] font-semibold text-zinc-400 uppercase tracking-wider border-b border-zinc-800">
                Seleccionar Foto de Muestra
              </div>
              {SAMPLE_IMAGES.map((sample, idx) => (
                <button
                  key={idx}
                  onClick={() => loadSample(sample)}
                  className="w-full text-left px-3 py-2 text-xs text-zinc-300 hover:text-white hover:bg-zinc-800 flex items-center justify-between transition-colors"
                >
                  <span className="truncate">{sample.name}</span>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Open Image Button */}
        <button
          id="btn-open-image"
          onClick={() => fileInputRef.current?.click()}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium text-zinc-200 bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 transition-colors"
        >
          <FolderOpen className="w-3.5 h-3.5 text-zinc-400" />
          <span className="hidden sm:inline">Abrir Foto</span>
        </button>

        {/* Export Dropdown */}
        <div className="relative">
          <button
            id="btn-toggle-export"
            onClick={() => setShowExportMenu(!showExportMenu)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white shadow-sm shadow-indigo-600/30 transition-colors"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Exportar</span>
            <ChevronDown className="w-3 h-3" />
          </button>

          {showExportMenu && (
            <div className="absolute right-0 mt-1.5 w-48 bg-zinc-900 border border-zinc-700 rounded-lg shadow-xl py-1 z-50">
              <button
                id="btn-export-png"
                onClick={() => {
                  onExport('png', 1.0);
                  setShowExportMenu(false);
                }}
                className="w-full text-left px-3 py-2 text-xs text-zinc-300 hover:text-white hover:bg-zinc-800 flex items-center justify-between"
              >
                <span>Guardar PNG (Sin pérdida)</span>
              </button>
              <button
                id="btn-export-jpeg-high"
                onClick={() => {
                  onExport('jpeg', 0.95);
                  setShowExportMenu(false);
                }}
                className="w-full text-left px-3 py-2 text-xs text-zinc-300 hover:text-white hover:bg-zinc-800 flex items-center justify-between"
              >
                <span>Guardar JPEG (Alta calidad)</span>
              </button>
              <button
                id="btn-export-jpeg-web"
                onClick={() => {
                  onExport('jpeg', 0.80);
                  setShowExportMenu(false);
                }}
                className="w-full text-left px-3 py-2 text-xs text-zinc-300 hover:text-white hover:bg-zinc-800 flex items-center justify-between"
              >
                <span>Guardar JPEG (Optimizada Web)</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
