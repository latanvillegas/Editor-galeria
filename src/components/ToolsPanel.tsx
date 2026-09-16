import React from 'react';
import {
  SlidersHorizontal,
  Crop,
  Sparkles,
  Layers,
  Palette,
  EyeOff,
  RotateCw,
  RotateCcw,
  FlipHorizontal,
  FlipVertical,
  Plus,
  Trash2,
  Brush,
  Type,
  Stamp,
  Sun,
  Contrast,
  Droplets,
  Thermometer,
  Compass,
  CheckCircle2,
  Shapes
} from 'lucide-react';
import {
  EditorDocumentState,
  EditorTab,
  CropRatio,
  FilterType,
  BlendMode,
  LayerItem
} from '../types';

interface ToolsPanelProps {
  activeTab: EditorTab;
  setActiveTab: (tab: EditorTab) => void;
  documentState: EditorDocumentState;
  onUpdateDocument: (updater: (prev: EditorDocumentState) => EditorDocumentState, desc?: string) => void;

  // Creative sub-tool states
  activeCreativeTool: 'brush' | 'text' | 'clone';
  setActiveCreativeTool: (tool: 'brush' | 'text' | 'clone') => void;
  brushColor: string;
  setBrushColor: (color: string) => void;
  brushSize: number;
  setBrushSize: (size: number) => void;
  brushOpacity: number;
  setBrushOpacity: (opacity: number) => void;
  isEraser: boolean;
  setIsEraser: (eraser: boolean) => void;
  currentText: string;
  setCurrentText: (txt: string) => void;
  textColor: string;
  setTextColor: (color: string) => void;
  textSize: number;
  setTextSize: (size: number) => void;
  textFont: 'sans-serif' | 'serif' | 'monospace' | 'cursive';
  setTextFont: (font: 'sans-serif' | 'serif' | 'monospace' | 'cursive') => void;
  cloneRadius: number;
  setCloneRadius: (radius: number) => void;
}

const COLOR_SWATCHES = [
  '#ffffff',
  '#f87171',
  '#fbbf24',
  '#34d399',
  '#60a5fa',
  '#a78bfa',
  '#f472b6',
  '#18181b',
];

const FILTER_PRESETS: { id: FilterType; label: string; desc: string }[] = [
  { id: 'none', label: 'Original', desc: 'Sin filtro' },
  { id: 'bw', label: 'B & N', desc: 'Blanco y negro tonal' },
  { id: 'sepia', label: 'Sepia', desc: 'Acabado vintage clásico' },
  { id: 'vivid', label: 'Vívido', desc: 'Colores de alto impacto' },
  { id: 'cinema', label: 'Cine', desc: 'Gradación cinematográfica' },
  { id: 'warm', label: 'Cálido', desc: 'Atardecer dorado suave' },
  { id: 'cool', label: 'Frío', desc: 'Tonos nórdicos y cianes' },
  { id: 'dramatic', label: 'Dramático', desc: 'Sombras profundas y microcontraste' },
  { id: 'noir', label: 'Noir', desc: 'Grano y alto contraste en B&N' },
];

const CROP_RATIOS: { id: CropRatio; label: string }[] = [
  { id: 'free', label: 'Libre' },
  { id: '1:1', label: '1:1' },
  { id: '4:3', label: '4:3' },
  { id: '16:9', label: '16:9' },
  { id: '3:2', label: '3:2' },
  { id: '9:16', label: '9:16' },
];

export const ToolsPanel: React.FC<ToolsPanelProps> = ({
  activeTab,
  setActiveTab,
  documentState,
  onUpdateDocument,
  activeCreativeTool,
  setActiveCreativeTool,
  brushColor,
  setBrushColor,
  brushSize,
  setBrushSize,
  brushOpacity,
  setBrushOpacity,
  isEraser,
  setIsEraser,
  currentText,
  setCurrentText,
  textColor,
  setTextColor,
  textSize,
  setTextSize,
  textFont,
  setTextFont,
  cloneRadius,
  setCloneRadius,
}) => {
  const TABS: { id: EditorTab; label: string; icon: React.ReactNode }[] = [
    { id: 'adjust', label: 'Ajustes', icon: <SlidersHorizontal className="w-4 h-4" /> },
    { id: 'geometry', label: 'Geometría', icon: <Crop className="w-4 h-4" /> },
    { id: 'filters', label: 'Filtros', icon: <Sparkles className="w-4 h-4" /> },
    { id: 'layers', label: 'Capas', icon: <Layers className="w-4 h-4" /> },
    { id: 'creative', label: 'Retoque', icon: <Palette className="w-4 h-4" /> },
    { id: 'masks', label: 'Máscaras', icon: <Shapes className="w-4 h-4" /> },
  ];

  return (
    <div className="w-80 md:w-96 bg-zinc-900 border-l border-zinc-800 flex flex-col h-full z-20 shrink-0">
      {/* 6 Tab Bar Header */}
      <div className="grid grid-cols-6 border-b border-zinc-800 bg-zinc-950/60 p-1">
        {TABS.map((tab) => {
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              id={`tab-${tab.id}`}
              onClick={() => setActiveTab(tab.id)}
              className={`flex flex-col items-center justify-center py-2 px-1 rounded-md transition-all ${
                isActive
                  ? 'bg-zinc-800 text-amber-400 font-semibold shadow-sm'
                  : 'text-zinc-400 hover:text-zinc-200 hover:bg-zinc-900'
              }`}
              title={tab.label}
            >
              {tab.icon}
              <span className="text-[10px] mt-1 tracking-tight truncate max-w-full">
                {tab.label}
              </span>
            </button>
          );
        })}
      </div>

      {/* Tab Content Body */}
      <div className="flex-1 overflow-y-auto p-4 space-y-5 text-zinc-300 custom-scrollbar">
        {/* TAB 1: AJUSTES (Color Adjustments) */}
        {activeTab === 'adjust' && (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-zinc-400">
                Ajustes Cromáticos Globales
              </h3>
              <button
                id="btn-reset-adjustments"
                onClick={() =>
                  onUpdateDocument(
                    (d) => ({
                      ...d,
                      adjustments: {
                        brightness: 0,
                        contrast: 0,
                        saturation: 0,
                        exposure: 0,
                        temperature: 0,
                        tint: 0,
                      },
                    }),
                    'Restablecer ajustes'
                  )
                }
                className="text-[11px] text-amber-400 hover:underline"
              >
                Restablecer
              </button>
            </div>

            {[
              {
                key: 'brightness' as const,
                label: 'Brillo',
                icon: <Sun className="w-3.5 h-3.5 text-zinc-400" />,
                min: -100,
                max: 100,
              },
              {
                key: 'contrast' as const,
                label: 'Contraste',
                icon: <Contrast className="w-3.5 h-3.5 text-zinc-400" />,
                min: -100,
                max: 100,
              },
              {
                key: 'saturation' as const,
                label: 'Saturación',
                icon: <Droplets className="w-3.5 h-3.5 text-zinc-400" />,
                min: -100,
                max: 100,
              },
              {
                key: 'exposure' as const,
                label: 'Exposición',
                icon: <Sun className="w-3.5 h-3.5 text-amber-400" />,
                min: -100,
                max: 100,
              },
              {
                key: 'temperature' as const,
                label: 'Temperatura (Calidez)',
                icon: <Thermometer className="w-3.5 h-3.5 text-orange-400" />,
                min: -100,
                max: 100,
              },
              {
                key: 'tint' as const,
                label: 'Tinte (Verde/Magenta)',
                icon: <Palette className="w-3.5 h-3.5 text-pink-400" />,
                min: -100,
                max: 100,
              },
            ].map(({ key, label, icon, min, max }) => {
              const val = documentState.adjustments[key];
              return (
                <div key={key} className="space-y-1.5 bg-zinc-950/40 p-2.5 rounded-lg border border-zinc-800/80">
                  <div className="flex items-center justify-between text-xs">
                    <span className="flex items-center gap-1.5 font-medium text-zinc-300">
                      {icon}
                      {label}
                    </span>
                    <span className="font-mono text-[11px] font-semibold text-zinc-400">
                      {val > 0 ? `+${val}` : val}
                    </span>
                  </div>
                  <input
                    type="range"
                    min={min}
                    max={max}
                    value={val}
                    onChange={(e) => {
                      const num = Number(e.target.value);
                      onUpdateDocument(
                        (d) => ({
                          ...d,
                          adjustments: { ...d.adjustments, [key]: num },
                        }),
                        `Ajustar ${label}`
                      );
                    }}
                    className="w-full h-1.5 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-amber-500"
                  />
                </div>
              );
            })}
          </div>
        )}

        {/* TAB 2: GEOMETRÍA Y RECORTE */}
        {activeTab === 'geometry' && (
          <div className="space-y-4">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-zinc-400">
              Transformación & Rotación
            </h3>

            {/* Rotation & Flip Controls */}
            <div className="grid grid-cols-4 gap-2">
              <button
                id="btn-rotate-left"
                onClick={() =>
                  onUpdateDocument(
                    (d) => ({
                      ...d,
                      geometry: {
                        ...d.geometry,
                        rotation: (d.geometry.rotation + 270) % 360,
                      },
                    }),
                    'Rotar 90° izq'
                  )
                }
                className="flex flex-col items-center justify-center p-2.5 bg-zinc-950/60 hover:bg-zinc-800 border border-zinc-800 rounded-lg text-xs transition-colors"
              >
                <RotateCcw className="w-4 h-4 text-zinc-300 mb-1" />
                <span>-90°</span>
              </button>

              <button
                id="btn-rotate-right"
                onClick={() =>
                  onUpdateDocument(
                    (d) => ({
                      ...d,
                      geometry: {
                        ...d.geometry,
                        rotation: (d.geometry.rotation + 90) % 360,
                      },
                    }),
                    'Rotar 90° der'
                  )
                }
                className="flex flex-col items-center justify-center p-2.5 bg-zinc-950/60 hover:bg-zinc-800 border border-zinc-800 rounded-lg text-xs transition-colors"
              >
                <RotateCw className="w-4 h-4 text-zinc-300 mb-1" />
                <span>+90°</span>
              </button>

              <button
                id="btn-flip-h"
                onClick={() =>
                  onUpdateDocument(
                    (d) => ({
                      ...d,
                      geometry: { ...d.geometry, flipH: !d.geometry.flipH },
                    }),
                    'Volteo horizontal'
                  )
                }
                className={`flex flex-col items-center justify-center p-2.5 border rounded-lg text-xs transition-colors ${
                  documentState.geometry.flipH
                    ? 'bg-amber-500/20 border-amber-500/50 text-amber-300'
                    : 'bg-zinc-950/60 hover:bg-zinc-800 border-zinc-800 text-zinc-300'
                }`}
              >
                <FlipHorizontal className="w-4 h-4 mb-1" />
                <span>Espejo H</span>
              </button>

              <button
                id="btn-flip-v"
                onClick={() =>
                  onUpdateDocument(
                    (d) => ({
                      ...d,
                      geometry: { ...d.geometry, flipV: !d.geometry.flipV },
                    }),
                    'Volteo vertical'
                  )
                }
                className={`flex flex-col items-center justify-center p-2.5 border rounded-lg text-xs transition-colors ${
                  documentState.geometry.flipV
                    ? 'bg-amber-500/20 border-amber-500/50 text-amber-300'
                    : 'bg-zinc-950/60 hover:bg-zinc-800 border-zinc-800 text-zinc-300'
                }`}
              >
                <FlipVertical className="w-4 h-4 mb-1" />
                <span>Espejo V</span>
              </button>
            </div>

            {/* Straighten Angle */}
            <div className="space-y-1.5 bg-zinc-950/40 p-3 rounded-lg border border-zinc-800/80">
              <div className="flex items-center justify-between text-xs">
                <span className="flex items-center gap-1.5 font-medium text-zinc-300">
                  <Compass className="w-3.5 h-3.5 text-indigo-400" />
                  Enderezado Fino Continuo
                </span>
                <span className="font-mono text-[11px] font-semibold text-zinc-400">
                  {documentState.geometry.straighten > 0
                    ? `+${documentState.geometry.straighten}°`
                    : `${documentState.geometry.straighten}°`}
                </span>
              </div>
              <input
                type="range"
                min={-45}
                max={45}
                value={documentState.geometry.straighten}
                onChange={(e) => {
                  const num = Number(e.target.value);
                  onUpdateDocument(
                    (d) => ({
                      ...d,
                      geometry: { ...d.geometry, straighten: num },
                    }),
                    'Enderezar'
                  );
                }}
                className="w-full h-1.5 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-indigo-500"
              />
              <div className="flex justify-between text-[10px] text-zinc-500 font-mono">
                <span>-45°</span>
                <span>0°</span>
                <span>+45°</span>
              </div>
            </div>

            {/* Aspect Ratio Presets */}
            <div className="space-y-2">
              <h4 className="text-xs font-semibold uppercase tracking-wider text-zinc-400">
                Relación de Aspecto de Recorte
              </h4>
              <div className="grid grid-cols-3 gap-1.5">
                {CROP_RATIOS.map((ratio) => {
                  const isSelected = documentState.geometry.cropRatio === ratio.id;
                  return (
                    <button
                      key={ratio.id}
                      onClick={() => {
                        onUpdateDocument(
                          (d) => ({
                            ...d,
                            geometry: {
                              ...d.geometry,
                              cropRatio: ratio.id,
                              cropRect:
                                ratio.id === '1:1'
                                  ? { x: 0.1, y: 0.1, width: 0.8, height: 0.8 }
                                  : ratio.id === '16:9'
                                  ? { x: 0, y: 0.15, width: 1, height: 0.5625 }
                                  : ratio.id === '9:16'
                                  ? { x: 0.22, y: 0, width: 0.5625, height: 1 }
                                  : ratio.id === '4:3'
                                  ? { x: 0.05, y: 0.1, width: 0.9, height: 0.675 }
                                  : { x: 0, y: 0, width: 1, height: 1 },
                            },
                          }),
                          `Recorte ${ratio.label}`
                        );
                      }}
                      className={`py-2 px-2 rounded-md text-xs font-medium border text-center transition-colors ${
                        isSelected
                          ? 'bg-indigo-600 border-indigo-500 text-white font-semibold shadow-sm'
                          : 'bg-zinc-950/60 border-zinc-800 text-zinc-300 hover:bg-zinc-800'
                      }`}
                    >
                      {ratio.label}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>
        )}

        {/* TAB 3: FILTROS */}
        {activeTab === 'filters' && (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-zinc-400">
                Filtros Estilizados Pro
              </h3>
            </div>

            {/* Filter Intensity Slider */}
            {documentState.filter.type !== 'none' && (
              <div className="space-y-1.5 bg-zinc-950/60 p-3 rounded-lg border border-zinc-800">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-medium text-zinc-300">Intensidad del Filtro</span>
                  <span className="font-mono text-zinc-400">
                    {documentState.filter.intensity}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={100}
                  value={documentState.filter.intensity}
                  onChange={(e) => {
                    const val = Number(e.target.value);
                    onUpdateDocument(
                      (d) => ({
                        ...d,
                        filter: { ...d.filter, intensity: val },
                      }),
                      'Intensidad filtro'
                    );
                  }}
                  className="w-full h-1.5 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-amber-500"
                />
              </div>
            )}

            {/* Filter Cards Grid */}
            <div className="grid grid-cols-2 gap-2">
              {FILTER_PRESETS.map((preset) => {
                const isActive = documentState.filter.type === preset.id;
                return (
                  <button
                    key={preset.id}
                    onClick={() =>
                      onUpdateDocument(
                        (d) => ({
                          ...d,
                          filter: { ...d.filter, type: preset.id },
                        }),
                        `Filtro ${preset.label}`
                      )
                    }
                    className={`p-2.5 rounded-lg border text-left transition-all ${
                      isActive
                        ? 'bg-amber-500/20 border-amber-500/80 shadow-md shadow-amber-500/10'
                        : 'bg-zinc-950/50 border-zinc-800 hover:border-zinc-700 hover:bg-zinc-800/60'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span
                        className={`text-xs font-semibold ${
                          isActive ? 'text-amber-400' : 'text-zinc-200'
                        }`}
                      >
                        {preset.label}
                      </span>
                      {isActive && <CheckCircle2 className="w-3.5 h-3.5 text-amber-400" />}
                    </div>
                    <p className="text-[10px] text-zinc-500 mt-1 line-clamp-2">
                      {preset.desc}
                    </p>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* TAB 4: CAPAS (Layers) */}
        {activeTab === 'layers' && (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-zinc-400">
                Pila de Capas de Fusión
              </h3>
              <div className="flex items-center gap-1">
                <button
                  id="btn-add-tint-layer"
                  onClick={() => {
                    const newLayer: LayerItem = {
                      id: 'layer_' + Date.now(),
                      name: 'Tinte Dorado',
                      type: 'color',
                      color: 'rgba(245, 158, 11, 0.4)',
                      opacity: 50,
                      blendMode: 'overlay',
                      visible: true,
                    };
                    onUpdateDocument(
                      (d) => ({ ...d, layers: [newLayer, ...d.layers] }),
                      'Añadir capa tinte'
                    );
                  }}
                  className="flex items-center gap-1 text-[11px] font-medium text-zinc-300 bg-zinc-800 hover:bg-zinc-700 px-2 py-1 rounded border border-zinc-700 transition-colors"
                >
                  <Plus className="w-3 h-3" />
                  <span>Tinte</span>
                </button>
                <button
                  id="btn-add-vignette-layer"
                  onClick={() => {
                    const newLayer: LayerItem = {
                      id: 'layer_' + Date.now(),
                      name: 'Viñeta Negra',
                      type: 'vignette',
                      color: 'rgba(0, 0, 0, 0.75)',
                      opacity: 60,
                      blendMode: 'multiply',
                      visible: true,
                    };
                    onUpdateDocument(
                      (d) => ({ ...d, layers: [newLayer, ...d.layers] }),
                      'Añadir viñeta'
                    );
                  }}
                  className="flex items-center gap-1 text-[11px] font-medium text-zinc-300 bg-zinc-800 hover:bg-zinc-700 px-2 py-1 rounded border border-zinc-700 transition-colors"
                >
                  <Plus className="w-3 h-3" />
                  <span>Viñeta</span>
                </button>
              </div>
            </div>

            {/* Base Background layer */}
            <div className="p-3 bg-zinc-950/60 rounded-lg border border-zinc-800/80 flex items-center justify-between text-xs">
              <span className="font-medium text-zinc-200">🖼️ Capa Base (Foto Principal)</span>
              <span className="text-[10px] text-zinc-500 font-mono">100% · Normal</span>
            </div>

            {/* Dynamic Layers */}
            {documentState.layers.length === 0 ? (
              <div className="text-center py-6 border border-dashed border-zinc-800 rounded-lg text-xs text-zinc-500">
                No hay capas superpuestas añadidas.
              </div>
            ) : (
              documentState.layers.map((layer) => (
                <div
                  key={layer.id}
                  className="p-3 bg-zinc-950/70 rounded-lg border border-zinc-800 space-y-2.5"
                >
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-semibold text-zinc-200">{layer.name}</span>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() =>
                          onUpdateDocument(
                            (d) => ({
                              ...d,
                              layers: d.layers.map((l) =>
                                l.id === layer.id ? { ...l, visible: !l.visible } : l
                              ),
                            }),
                            'Alternar visibilidad'
                          )
                        }
                        className={`text-xs ${
                          layer.visible ? 'text-indigo-400' : 'text-zinc-600'
                        }`}
                      >
                        {layer.visible ? 'Visible' : 'Oculta'}
                      </button>
                      <button
                        onClick={() =>
                          onUpdateDocument(
                            (d) => ({
                              ...d,
                              layers: d.layers.filter((l) => l.id !== layer.id),
                            }),
                            'Eliminar capa'
                          )
                        }
                        className="text-zinc-500 hover:text-red-400 transition-colors"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>

                  {/* Mode & Opacity */}
                  <div className="grid grid-cols-2 gap-2 text-xs">
                    <div>
                      <label className="text-[10px] text-zinc-400 block mb-1">
                        Modo de Fusión
                      </label>
                      <select
                        value={layer.blendMode}
                        onChange={(e) => {
                          const mode = e.target.value as BlendMode;
                          onUpdateDocument(
                            (d) => ({
                              ...d,
                              layers: d.layers.map((l) =>
                                l.id === layer.id ? { ...l, blendMode: mode } : l
                              ),
                            }),
                            'Modo fusión capa'
                          );
                        }}
                        className="w-full bg-zinc-900 border border-zinc-700 rounded px-2 py-1 text-xs text-zinc-200"
                      >
                        <option value="normal">Normal</option>
                        <option value="multiply">Multiply</option>
                        <option value="screen">Screen</option>
                        <option value="overlay">Overlay</option>
                        <option value="darken">Darken</option>
                        <option value="lighten">Lighten</option>
                      </select>
                    </div>

                    <div>
                      <div className="flex justify-between text-[10px] text-zinc-400 mb-1">
                        <span>Opacidad</span>
                        <span>{layer.opacity}%</span>
                      </div>
                      <input
                        type="range"
                        min={0}
                        max={100}
                        value={layer.opacity}
                        onChange={(e) => {
                          const op = Number(e.target.value);
                          onUpdateDocument(
                            (d) => ({
                              ...d,
                              layers: d.layers.map((l) =>
                                l.id === layer.id ? { ...l, opacity: op } : l
                              ),
                            }),
                            'Opacidad capa'
                          );
                        }}
                        className="w-full h-1.5 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-indigo-500 mt-2"
                      />
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        )}

        {/* TAB 5: RETOQUE CREATIVO (Brush, Text, Clone) */}
        {activeTab === 'creative' && (
          <div className="space-y-4">
            {/* Sub-tool switcher */}
            <div className="grid grid-cols-3 gap-1 bg-zinc-950 p-1 rounded-lg border border-zinc-800">
              <button
                id="btn-subtool-brush"
                onClick={() => setActiveCreativeTool('brush')}
                className={`flex items-center justify-center gap-1.5 py-1.5 rounded text-xs font-medium transition-colors ${
                  activeCreativeTool === 'brush'
                    ? 'bg-amber-500 text-zinc-950 font-semibold shadow'
                    : 'text-zinc-400 hover:text-white'
                }`}
              >
                <Brush className="w-3.5 h-3.5" />
                <span>Pincel</span>
              </button>

              <button
                id="btn-subtool-text"
                onClick={() => setActiveCreativeTool('text')}
                className={`flex items-center justify-center gap-1.5 py-1.5 rounded text-xs font-medium transition-colors ${
                  activeCreativeTool === 'text'
                    ? 'bg-amber-500 text-zinc-950 font-semibold shadow'
                    : 'text-zinc-400 hover:text-white'
                }`}
              >
                <Type className="w-3.5 h-3.5" />
                <span>Texto</span>
              </button>

              <button
                id="btn-subtool-clone"
                onClick={() => setActiveCreativeTool('clone')}
                className={`flex items-center justify-center gap-1.5 py-1.5 rounded text-xs font-medium transition-colors ${
                  activeCreativeTool === 'clone'
                    ? 'bg-amber-500 text-zinc-950 font-semibold shadow'
                    : 'text-zinc-400 hover:text-white'
                }`}
              >
                <Stamp className="w-3.5 h-3.5" />
                <span>Clonar</span>
              </button>
            </div>

            {/* Sub-tool 1: Pincel */}
            {activeCreativeTool === 'brush' && (
              <div className="space-y-3 bg-zinc-950/40 p-3 rounded-lg border border-zinc-800">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium text-zinc-300">Modo de Herramienta</span>
                  <div className="flex gap-1">
                    <button
                      onClick={() => setIsEraser(false)}
                      className={`px-2.5 py-1 rounded text-xs font-medium ${
                        !isEraser ? 'bg-amber-500 text-zinc-950 font-semibold' : 'bg-zinc-800 text-zinc-300'
                      }`}
                    >
                      Pintar
                    </button>
                    <button
                      onClick={() => setIsEraser(true)}
                      className={`px-2.5 py-1 rounded text-xs font-medium ${
                        isEraser ? 'bg-amber-500 text-zinc-950 font-semibold' : 'bg-zinc-800 text-zinc-300'
                      }`}
                    >
                      Borrador
                    </button>
                  </div>
                </div>

                {!isEraser && (
                  <div>
                    <label className="text-[11px] text-zinc-400 block mb-1.5">Color de Trazo</label>
                    <div className="flex gap-1.5 flex-wrap">
                      {COLOR_SWATCHES.map((col) => (
                        <button
                          key={col}
                          onClick={() => setBrushColor(col)}
                          className={`w-6 h-6 rounded-full border-2 transition-transform ${
                            brushColor === col ? 'scale-110 border-white shadow' : 'border-transparent'
                          }`}
                          style={{ backgroundColor: col }}
                        />
                      ))}
                    </div>
                  </div>
                )}

                <div>
                  <div className="flex justify-between text-xs text-zinc-300 mb-1">
                    <span>Grosor</span>
                    <span>{brushSize}px</span>
                  </div>
                  <input
                    type="range"
                    min={4}
                    max={120}
                    value={brushSize}
                    onChange={(e) => setBrushSize(Number(e.target.value))}
                    className="w-full h-1.5 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-amber-500"
                  />
                </div>

                {documentState.brushStrokes.length > 0 && (
                  <button
                    onClick={() =>
                      onUpdateDocument(
                        (d) => ({ ...d, brushStrokes: [] }),
                        'Limpiar trazos'
                      )
                    }
                    className="w-full py-1.5 text-xs text-red-400 hover:text-red-300 bg-red-950/30 border border-red-900/50 rounded transition-colors"
                  >
                    Borrar Todos los Trazos ({documentState.brushStrokes.length})
                  </button>
                )}
              </div>
            )}

            {/* Sub-tool 2: Texto */}
            {activeCreativeTool === 'text' && (
              <div className="space-y-3 bg-zinc-950/40 p-3 rounded-lg border border-zinc-800">
                <div>
                  <label className="text-[11px] text-zinc-400 block mb-1">Contenido de Texto</label>
                  <textarea
                    value={currentText}
                    onChange={(e) => setCurrentText(e.target.value)}
                    rows={2}
                    className="w-full bg-zinc-900 border border-zinc-700 rounded px-2.5 py-1.5 text-xs text-zinc-100 focus:outline-none focus:border-amber-500"
                    placeholder="Escribe el texto aquí..."
                  />
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="text-[11px] text-zinc-400 block mb-1">Tipografía</label>
                    <select
                      value={textFont}
                      onChange={(e) =>
                        setTextFont(
                          e.target.value as 'sans-serif' | 'serif' | 'monospace' | 'cursive'
                        )
                      }
                      className="w-full bg-zinc-900 border border-zinc-700 rounded px-2 py-1 text-xs text-zinc-200"
                    >
                      <option value="sans-serif">Sans Serif</option>
                      <option value="serif">Serif Elegante</option>
                      <option value="monospace">Monospace</option>
                      <option value="cursive">Caligrafía</option>
                    </select>
                  </div>

                  <div>
                    <div className="flex justify-between text-[11px] text-zinc-400 mb-1">
                      <span>Tamaño</span>
                      <span>{textSize}px</span>
                    </div>
                    <input
                      type="range"
                      min={16}
                      max={120}
                      value={textSize}
                      onChange={(e) => setTextSize(Number(e.target.value))}
                      className="w-full h-1.5 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-amber-500 mt-2"
                    />
                  </div>
                </div>

                <div>
                  <label className="text-[11px] text-zinc-400 block mb-1.5">Color de Texto</label>
                  <div className="flex gap-1.5">
                    {COLOR_SWATCHES.map((col) => (
                      <button
                        key={col}
                        onClick={() => setTextColor(col)}
                        className={`w-6 h-6 rounded-full border-2 transition-transform ${
                          textColor === col ? 'scale-110 border-white shadow' : 'border-transparent'
                        }`}
                        style={{ backgroundColor: col }}
                      />
                    ))}
                  </div>
                </div>

                {documentState.textOverlays.length > 0 && (
                  <button
                    onClick={() =>
                      onUpdateDocument(
                        (d) => ({ ...d, textOverlays: [] }),
                        'Limpiar textos'
                      )
                    }
                    className="w-full py-1.5 text-xs text-red-400 hover:text-red-300 bg-red-950/30 border border-red-900/50 rounded transition-colors"
                  >
                    Borrar Textos Insertados ({documentState.textOverlays.length})
                  </button>
                )}
              </div>
            )}

            {/* Sub-tool 3: Tampón Clonar */}
            {activeCreativeTool === 'clone' && (
              <div className="space-y-3 bg-zinc-950/40 p-3 rounded-lg border border-zinc-800">
                <p className="text-[11px] text-zinc-400 leading-relaxed">
                  Muestrea píxeles de una zona de la foto para reparar imperfecciones o clonar detalles con bordes suavizados.
                </p>

                <div>
                  <div className="flex justify-between text-xs text-zinc-300 mb-1">
                    <span>Radio del Parche</span>
                    <span>{cloneRadius}px</span>
                  </div>
                  <input
                    type="range"
                    min={15}
                    max={100}
                    value={cloneRadius}
                    onChange={(e) => setCloneRadius(Number(e.target.value))}
                    className="w-full h-1.5 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-amber-500"
                  />
                </div>

                {documentState.cloneStamps.length > 0 && (
                  <button
                    onClick={() =>
                      onUpdateDocument(
                        (d) => ({ ...d, cloneStamps: [] }),
                        'Limpiar clones'
                      )
                    }
                    className="w-full py-1.5 text-xs text-red-400 hover:text-red-300 bg-red-950/30 border border-red-900/50 rounded transition-colors"
                  >
                    Limpiar Parches Clonados ({documentState.cloneStamps.length})
                  </button>
                )}
              </div>
            )}
          </div>
        )}

        {/* TAB 6: MÁSCARAS Y SELECCIÓN */}
        {activeTab === 'masks' && (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-zinc-400">
                Máscaras y Ajustes Locales
              </h3>
              <button
                id="btn-toggle-mask-active"
                onClick={() =>
                  onUpdateDocument(
                    (d) => ({
                      ...d,
                      mask: { ...d.mask, enabled: !d.mask.enabled },
                    }),
                    'Activar máscara'
                  )
                }
                className={`px-2.5 py-1 rounded text-xs font-medium transition-colors ${
                  documentState.mask.enabled
                    ? 'bg-indigo-600 text-white'
                    : 'bg-zinc-800 text-zinc-400'
                }`}
              >
                {documentState.mask.enabled ? 'Activada' : 'Desactivada'}
              </button>
            </div>

            {documentState.mask.enabled && (
              <div className="space-y-3 bg-zinc-950/60 p-3 rounded-lg border border-zinc-800">
                <div className="flex gap-2">
                  <button
                    onClick={() =>
                      onUpdateDocument(
                        (d) => ({
                          ...d,
                          mask: { ...d.mask, type: 'rect' },
                        }),
                        'Máscara rectangular'
                      )
                    }
                    className={`flex-1 py-1.5 rounded text-xs font-medium border ${
                      documentState.mask.type === 'rect'
                        ? 'bg-indigo-950 border-indigo-500 text-indigo-200'
                        : 'bg-zinc-900 border-zinc-700 text-zinc-400'
                    }`}
                  >
                    Rectangular
                  </button>

                  <button
                    onClick={() =>
                      onUpdateDocument(
                        (d) => ({
                          ...d,
                          mask: { ...d.mask, type: 'ellipse' },
                        }),
                        'Máscara elíptica'
                      )
                    }
                    className={`flex-1 py-1.5 rounded text-xs font-medium border ${
                      documentState.mask.type === 'ellipse'
                        ? 'bg-indigo-950 border-indigo-500 text-indigo-200'
                        : 'bg-zinc-900 border-zinc-700 text-zinc-400'
                    }`}
                  >
                    Elíptica
                  </button>
                </div>

                <div>
                  <div className="flex justify-between text-xs text-zinc-300 mb-1">
                    <span>Difuminado Perimetral (Feather)</span>
                    <span>{documentState.mask.feather}px</span>
                  </div>
                  <input
                    type="range"
                    min={0}
                    max={50}
                    value={documentState.mask.feather}
                    onChange={(e) => {
                      const f = Number(e.target.value);
                      onUpdateDocument(
                        (d) => ({ ...d, mask: { ...d.mask, feather: f } }),
                        'Feather máscara'
                      );
                    }}
                    className="w-full h-1.5 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                  />
                </div>

                <div className="space-y-2 pt-2 border-t border-zinc-800">
                  <span className="text-[11px] font-semibold text-zinc-400 uppercase tracking-wider block">
                    Ajustes Dentro de la Máscara
                  </span>

                  <div>
                    <div className="flex justify-between text-xs text-zinc-300 mb-1">
                      <span>Brillo Local</span>
                      <span>{documentState.mask.adjustments.brightness}</span>
                    </div>
                    <input
                      type="range"
                      min={-50}
                      max={50}
                      value={documentState.mask.adjustments.brightness}
                      onChange={(e) => {
                        const b = Number(e.target.value);
                        onUpdateDocument(
                          (d) => ({
                            ...d,
                            mask: {
                              ...d.mask,
                              adjustments: { ...d.mask.adjustments, brightness: b },
                            },
                          }),
                          'Brillo máscara'
                        );
                      }}
                      className="w-full h-1.5 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                    />
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
