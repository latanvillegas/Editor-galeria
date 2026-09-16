import React, { useRef, useEffect, useState, useCallback } from 'react';
import {
  ZoomIn,
  ZoomOut,
  Maximize2,
  Hand,
  RotateCw,
  Target,
  Sparkles,
  Move
} from 'lucide-react';
import {
  EditorDocumentState,
  EditorTab,
  BrushStroke,
  CloneStamp,
  TextOverlay
} from '../types';
import { renderDocumentToCanvas } from '../utils/imagePipeline';

interface CanvasAreaProps {
  image: HTMLImageElement | null;
  documentState: EditorDocumentState;
  activeTab: EditorTab;
  isCompareMode: boolean;
  onUpdateDocument: (updater: (prev: EditorDocumentState) => EditorDocumentState, desc?: string) => void;
  // Creative sub-tool parameters passed from panel
  activeCreativeTool: 'brush' | 'text' | 'clone';
  brushColor: string;
  brushSize: number;
  brushOpacity: number;
  isEraser: boolean;
  currentText: string;
  textColor: string;
  textSize: number;
  textFont: 'sans-serif' | 'serif' | 'monospace' | 'cursive';
  cloneRadius: number;
}

export const CanvasArea: React.FC<CanvasAreaProps> = ({
  image,
  documentState,
  activeTab,
  isCompareMode,
  onUpdateDocument,
  activeCreativeTool,
  brushColor,
  brushSize,
  brushOpacity,
  isEraser,
  currentText,
  textColor,
  textSize,
  textFont,
  cloneRadius,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  // Viewport zoom & pan
  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [isPanning, setIsPanning] = useState(false);
  const [panStart, setPanStart] = useState({ x: 0, y: 0 });
  const [isHandToolActive, setIsHandToolActive] = useState(false);

  // Active stroke drawing state
  const [currentStroke, setCurrentStroke] = useState<BrushStroke | null>(null);

  // Clone stamp source selection state
  const [cloneSource, setCloneSource] = useState<{ x: number; y: number } | null>(null);

  // Re-render canvas whenever documentState or image or compare mode changes
  useEffect(() => {
    if (!canvasRef.current || !image) return;
    renderDocumentToCanvas(canvasRef.current, image, documentState, { isCompareMode });
  }, [image, documentState, isCompareMode]);

  // Fit to screen helper
  const handleFitToScreen = useCallback(() => {
    if (!containerRef.current || !canvasRef.current) return;
    const container = containerRef.current.getBoundingClientRect();
    const cw = canvasRef.current.width || 800;
    const ch = canvasRef.current.height || 600;

    const scaleX = (container.width - 48) / cw;
    const scaleY = (container.height - 48) / ch;
    const fitScale = Math.min(scaleX, scaleY, 1);

    setZoom(Math.max(0.1, Number(fitScale.toFixed(2))));
    setPan({ x: 0, y: 0 });
  }, []);

  // Initial fit on image load
  useEffect(() => {
    if (image) {
      setTimeout(handleFitToScreen, 50);
    }
  }, [image, handleFitToScreen]);

  // Helper to convert mouse event to normalized (0..1) canvas coordinates
  const getCanvasNormalizedCoord = (e: React.MouseEvent<HTMLDivElement | HTMLCanvasElement>) => {
    if (!canvasRef.current) return { x: 0.5, y: 0.5 };
    const rect = canvasRef.current.getBoundingClientRect();
    const x = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    const y = Math.max(0, Math.min(1, (e.clientY - rect.top) / rect.height));
    return { x, y };
  };

  // Canvas Mouse Down Handler
  const handleMouseDown = (e: React.MouseEvent<HTMLDivElement>) => {
    // 1. Hand tool or middle mouse button -> pan
    if (isHandToolActive || e.button === 1 || e.altKey) {
      setIsPanning(true);
      setPanStart({ x: e.clientX - pan.x, y: e.clientY - pan.y });
      return;
    }

    // 2. Creative Tool: Brush
    if (activeTab === 'creative' && activeCreativeTool === 'brush') {
      const coord = getCanvasNormalizedCoord(e);
      const newStroke: BrushStroke = {
        id: 'stroke_' + Date.now(),
        points: [coord],
        color: brushColor,
        size: brushSize,
        opacity: brushOpacity,
        isEraser: isEraser,
      };
      setCurrentStroke(newStroke);
      return;
    }

    // 3. Creative Tool: Text placement
    if (activeTab === 'creative' && activeCreativeTool === 'text') {
      const coord = getCanvasNormalizedCoord(e);
      const newText: TextOverlay = {
        id: 'text_' + Date.now(),
        text: currentText || 'HyperEditor',
        x: coord.x,
        y: coord.y,
        fontSize: textSize,
        fontFamily: textFont,
        color: textColor,
        opacity: 1.0,
      };
      onUpdateDocument(
        (doc) => ({ ...doc, textOverlays: [...doc.textOverlays, newText] }),
        'Añadir texto'
      );
      return;
    }

    // 4. Creative Tool: Clone Stamp
    if (activeTab === 'creative' && activeCreativeTool === 'clone') {
      const coord = getCanvasNormalizedCoord(e);
      if (e.shiftKey || !cloneSource) {
        // Set clone source point
        setCloneSource(coord);
      } else {
        // Apply clone stamp
        const stamp: CloneStamp = {
          id: 'stamp_' + Date.now(),
          sourceX: cloneSource.x,
          sourceY: cloneSource.y,
          targetX: coord.x,
          targetY: coord.y,
          radius: cloneRadius,
        };
        onUpdateDocument(
          (doc) => ({ ...doc, cloneStamps: [...doc.cloneStamps, stamp] }),
          'Tampón de clonar'
        );
      }
      return;
    }

    // Default: allow dragging/panning
    setIsPanning(true);
    setPanStart({ x: e.clientX - pan.x, y: e.clientY - pan.y });
  };

  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (isPanning) {
      setPan({
        x: e.clientX - panStart.x,
        y: e.clientY - panStart.y,
      });
      return;
    }

    if (currentStroke && activeTab === 'creative' && activeCreativeTool === 'brush') {
      const coord = getCanvasNormalizedCoord(e);
      const updatedPoints = [...currentStroke.points, coord];
      const updatedStroke = { ...currentStroke, points: updatedPoints };
      setCurrentStroke(updatedStroke);

      // Render dynamically into preview
      if (canvasRef.current && image) {
        renderDocumentToCanvas(
          canvasRef.current,
          image,
          {
            ...documentState,
            brushStrokes: [...documentState.brushStrokes, updatedStroke],
          },
          { isCompareMode }
        );
      }
    }
  };

  const handleMouseUp = () => {
    if (isPanning) {
      setIsPanning(false);
    }

    if (currentStroke) {
      onUpdateDocument(
        (doc) => ({
          ...doc,
          brushStrokes: [...doc.brushStrokes, currentStroke],
        }),
        isEraser ? 'Borrador' : 'Trazo de pincel'
      );
      setCurrentStroke(null);
    }
  };

  // Zoom wheel
  const handleWheel = (e: React.WheelEvent) => {
    e.preventDefault();
    const factor = e.deltaY < 0 ? 1.1 : 0.9;
    setZoom((prev) => Math.min(5.0, Math.max(0.1, Number((prev * factor).toFixed(2)))));
  };

  return (
    <div
      ref={containerRef}
      id="editor-canvas-container"
      onMouseDown={handleMouseDown}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onWheel={handleWheel}
      className={`relative flex-1 bg-zinc-950 overflow-hidden flex items-center justify-center select-none ${
        isHandToolActive ? 'cursor-grab active:cursor-grabbing' : 'cursor-crosshair'
      }`}
      style={{
        backgroundImage:
          'radial-gradient(circle at 1px 1px, rgba(255,255,255,0.05) 1px, transparent 0)',
        backgroundSize: '24px 24px',
      }}
    >
      {/* Visual Canvas Target */}
      <div
        className="relative transition-transform duration-75"
        style={{
          transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})`,
          transformOrigin: 'center center',
        }}
      >
        <canvas
          ref={canvasRef}
          id="main-photo-canvas"
          className="shadow-2xl rounded-sm max-w-none border border-zinc-800/80 bg-zinc-900"
        />

        {/* Clone Source Reticle Indicator */}
        {activeTab === 'creative' && activeCreativeTool === 'clone' && cloneSource && (
          <div
            className="absolute pointer-events-none w-8 h-8 -ml-4 -mt-4 border-2 border-dashed border-amber-400 rounded-full flex items-center justify-center animate-pulse"
            style={{
              left: `${cloneSource.x * 100}%`,
              top: `${cloneSource.y * 100}%`,
            }}
          >
            <div className="w-1 h-1 bg-amber-400 rounded-full" />
          </div>
        )}

        {/* Mask Overlay Indicator when active */}
        {activeTab === 'masks' && documentState.mask.enabled && (
          <div
            className={`absolute pointer-events-none border-2 border-indigo-400/90 bg-indigo-500/15 ${
              documentState.mask.type === 'ellipse' ? 'rounded-full' : 'rounded-md'
            }`}
            style={{
              left: `${(documentState.mask.x - documentState.mask.width / 2) * 100}%`,
              top: `${(documentState.mask.y - documentState.mask.height / 2) * 100}%`,
              width: `${documentState.mask.width * 100}%`,
              height: `${documentState.mask.height * 100}%`,
              boxShadow: `0 0 ${documentState.mask.feather}px rgba(99, 102, 241, 0.4)`,
            }}
          >
            <span className="absolute -top-6 left-1/2 -translate-x-1/2 text-[10px] font-mono px-1.5 py-0.5 rounded bg-indigo-950/90 border border-indigo-500 text-indigo-200">
              Máscara Activa ({documentState.mask.type})
            </span>
          </div>
        )}
      </div>

      {/* Floating Viewport Status & Zoom Bar */}
      <div className="absolute bottom-4 left-4 z-20 flex items-center gap-1.5 bg-zinc-900/90 backdrop-blur-md px-2.5 py-1.5 rounded-full border border-zinc-700/80 shadow-lg text-xs">
        <button
          id="btn-toggle-hand"
          onClick={() => setIsHandToolActive(!isHandToolActive)}
          className={`p-1 rounded-full transition-colors ${
            isHandToolActive ? 'bg-indigo-600 text-white' : 'text-zinc-400 hover:text-white'
          }`}
          title="Mover lienzo (Mano)"
        >
          <Hand className="w-3.5 h-3.5" />
        </button>

        <div className="w-px h-3.5 bg-zinc-700 mx-0.5" />

        <button
          id="btn-zoom-out"
          onClick={() => setZoom((z) => Math.max(0.1, Number((z - 0.15).toFixed(2))))}
          className="p-1 rounded-full text-zinc-400 hover:text-white transition-colors"
          title="Alejar"
        >
          <ZoomOut className="w-3.5 h-3.5" />
        </button>

        <span className="text-[11px] font-mono font-medium text-zinc-200 w-12 text-center">
          {Math.round(zoom * 100)}%
        </span>

        <button
          id="btn-zoom-in"
          onClick={() => setZoom((z) => Math.min(5.0, Number((z + 0.15).toFixed(2))))}
          className="p-1 rounded-full text-zinc-400 hover:text-white transition-colors"
          title="Acercar"
        >
          <ZoomIn className="w-3.5 h-3.5" />
        </button>

        <button
          id="btn-fit-screen"
          onClick={handleFitToScreen}
          className="p-1 rounded-full text-zinc-400 hover:text-white transition-colors ml-0.5"
          title="Ajustar a la pantalla"
        >
          <Maximize2 className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* Context Instructions for Active Mode */}
      {activeTab === 'creative' && (
        <div className="absolute top-4 left-4 z-20 bg-zinc-900/90 backdrop-blur-md px-3 py-1.5 rounded-lg border border-zinc-700/80 text-[11px] text-zinc-300 shadow-md flex items-center gap-2">
          {activeCreativeTool === 'brush' && (
            <span>🎨 Arrastra sobre la foto para pintar con pincel libre o borrador</span>
          )}
          {activeCreativeTool === 'text' && (
            <span>✍️ Haz clic sobre la foto donde deseas posicionar el texto</span>
          )}
          {activeCreativeTool === 'clone' && (
            <span>
              🎯 <strong className="text-amber-400">Shift + Clic</strong> para fijar origen; luego clic para estampar
            </span>
          )}
        </div>
      )}
    </div>
  );
};
