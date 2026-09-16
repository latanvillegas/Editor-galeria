import React, { useState, useEffect, useCallback } from 'react';
import { EditorDocumentState, EditorTab } from './types';
import { DEFAULT_DOCUMENT, SAMPLE_IMAGES } from './utils/imagePipeline';
import { Header } from './components/Header';
import { CanvasArea } from './components/CanvasArea';
import { ToolsPanel } from './components/ToolsPanel';
import { ApkGuideModal } from './components/ApkGuideModal';

export default function App() {
  const [image, setImage] = useState<HTMLImageElement | null>(null);
  const [imageName, setImageName] = useState<string>('sample-photo.jpg');

  // Document State & Undo / Redo History (atomic stack up to 50 states)
  const [documentState, setDocumentState] = useState<EditorDocumentState>(DEFAULT_DOCUMENT);
  const [history, setHistory] = useState<EditorDocumentState[]>([DEFAULT_DOCUMENT]);
  const [historyIndex, setHistoryIndex] = useState<number>(0);

  // Active Tool & Mode State
  const [activeTab, setActiveTab] = useState<EditorTab>('adjust');
  const [isCompareMode, setIsCompareMode] = useState<boolean>(false);
  const [isApkModalOpen, setIsApkModalOpen] = useState<boolean>(false);

  // Creative sub-tool state
  const [activeCreativeTool, setActiveCreativeTool] = useState<'brush' | 'text' | 'clone'>('brush');
  const [brushColor, setBrushColor] = useState<string>('#fbbf24');
  const [brushSize, setBrushSize] = useState<number>(24);
  const [brushOpacity, setBrushOpacity] = useState<number>(1.0);
  const [isEraser, setIsEraser] = useState<boolean>(false);

  const [currentText, setCurrentText] = useState<string>('HyperEditor Pro');
  const [textColor, setTextColor] = useState<string>('#ffffff');
  const [textSize, setTextSize] = useState<number>(44);
  const [textFont, setTextFont] = useState<'sans-serif' | 'serif' | 'monospace' | 'cursive'>('sans-serif');

  const [cloneRadius, setCloneRadius] = useState<number>(40);

  // Load initial sample image so the workspace starts populated immediately
  useEffect(() => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      setImage(img);
      setImageName(SAMPLE_IMAGES[0].name);
    };
    img.src = SAMPLE_IMAGES[0].url;
  }, []);

  // Update Document & append to atomic history
  const handleUpdateDocument = useCallback(
    (updater: (prev: EditorDocumentState) => EditorDocumentState) => {
      setDocumentState((prev) => {
        const next = updater(prev);
        // Truncate future history and push new state
        setHistory((prevHistory) => {
          const validHistory = prevHistory.slice(0, historyIndex + 1);
          const updated = [...validHistory, next];
          if (updated.length > 50) updated.shift();
          return updated;
        });
        setHistoryIndex((prevIndex) => Math.min(49, prevIndex + 1));
        return next;
      });
    },
    [historyIndex]
  );

  // Undo Handler
  const handleUndo = useCallback(() => {
    if (historyIndex > 0) {
      const newIndex = historyIndex - 1;
      setHistoryIndex(newIndex);
      setDocumentState(history[newIndex]);
    }
  }, [historyIndex, history]);

  // Redo Handler
  const handleRedo = useCallback(() => {
    if (historyIndex < history.length - 1) {
      const newIndex = historyIndex + 1;
      setHistoryIndex(newIndex);
      setDocumentState(history[newIndex]);
    }
  }, [historyIndex, history]);

  // Reset Document Handler
  const handleReset = useCallback(() => {
    handleUpdateDocument(() => DEFAULT_DOCUMENT);
  }, [handleUpdateDocument]);

  // Load new image handler
  const handleImageLoaded = useCallback((newImg: HTMLImageElement, name: string) => {
    setImage(newImg);
    setImageName(name);
    setDocumentState(DEFAULT_DOCUMENT);
    setHistory([DEFAULT_DOCUMENT]);
    setHistoryIndex(0);
  }, []);

  // Keyboard Shortcuts (Ctrl+Z, Ctrl+Y)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'z') {
        e.preventDefault();
        if (e.shiftKey) {
          handleRedo();
        } else {
          handleUndo();
        }
      } else if ((e.ctrlKey || e.metaKey) && e.key === 'y') {
        e.preventDefault();
        handleRedo();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleUndo, handleRedo]);

  // Drag and drop image files support
  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    const file = e.dataTransfer.files?.[0];
    if (file && file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = (event) => {
        const img = new Image();
        img.crossOrigin = 'anonymous';
        img.onload = () => {
          handleImageLoaded(img, file.name);
        };
        img.src = event.target?.result as string;
      };
      reader.readAsDataURL(file);
    }
  };

  // Export Canvas Image Handler
  const handleExport = (format: 'png' | 'jpeg', quality: number) => {
    const canvas = document.getElementById('main-photo-canvas') as HTMLCanvasElement;
    if (!canvas) return;

    const mimeType = format === 'png' ? 'image/png' : 'image/jpeg';
    const dataUrl = canvas.toDataURL(mimeType, quality);

    const link = document.createElement('a');
    const baseName = imageName.replace(/\.[^/.]+$/, '') || 'hypereditor-photo';
    link.download = `${baseName}-edited.${format}`;
    link.href = dataUrl;
    link.click();
  };

  return (
    <div
      onDragOver={handleDragOver}
      onDrop={handleDrop}
      className="flex flex-col h-screen w-screen bg-zinc-950 text-zinc-100 overflow-hidden font-sans select-none"
    >
      {/* Top Application Bar */}
      <Header
        canUndo={historyIndex > 0}
        canRedo={historyIndex < history.length - 1}
        onUndo={handleUndo}
        onRedo={handleRedo}
        onReset={handleReset}
        isCompareMode={isCompareMode}
        setIsCompareMode={setIsCompareMode}
        onImageLoaded={handleImageLoaded}
        onExport={handleExport}
        onOpenApkModal={() => setIsApkModalOpen(true)}
      />

      {/* Main Workspace Area (Canvas Viewport + Right Side Tools Panel) */}
      <div className="flex flex-1 overflow-hidden">
        <CanvasArea
          image={image}
          documentState={documentState}
          activeTab={activeTab}
          isCompareMode={isCompareMode}
          onUpdateDocument={handleUpdateDocument}
          activeCreativeTool={activeCreativeTool}
          brushColor={brushColor}
          brushSize={brushSize}
          brushOpacity={brushOpacity}
          isEraser={isEraser}
          currentText={currentText}
          textColor={textColor}
          textSize={textSize}
          textFont={textFont}
          cloneRadius={cloneRadius}
        />

        <ToolsPanel
          activeTab={activeTab}
          setActiveTab={setActiveTab}
          documentState={documentState}
          onUpdateDocument={handleUpdateDocument}
          activeCreativeTool={activeCreativeTool}
          setActiveCreativeTool={setActiveCreativeTool}
          brushColor={brushColor}
          setBrushColor={setBrushColor}
          brushSize={brushSize}
          setBrushSize={setBrushSize}
          brushOpacity={brushOpacity}
          setBrushOpacity={setBrushOpacity}
          isEraser={isEraser}
          setIsEraser={setIsEraser}
          currentText={currentText}
          setCurrentText={setCurrentText}
          textColor={textColor}
          setTextColor={setTextColor}
          textSize={textSize}
          setTextSize={setTextSize}
          textFont={textFont}
          setTextFont={setTextFont}
          cloneRadius={cloneRadius}
          setCloneRadius={setCloneRadius}
        />
      </div>

      {/* APK Compilation & Android Integration Modal */}
      <ApkGuideModal
        isOpen={isApkModalOpen}
        onClose={() => setIsApkModalOpen(false)}
      />
    </div>
  );
}
