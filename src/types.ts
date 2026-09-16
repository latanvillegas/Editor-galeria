export type EditorTab = 'adjust' | 'geometry' | 'filters' | 'layers' | 'creative' | 'masks';

export interface ColorAdjustments {
  brightness: number; // -100 to 100
  contrast: number;   // -100 to 100
  saturation: number; // -100 to 100
  exposure: number;   // -100 to 100
  temperature: number;// -100 to 100
  tint: number;       // -100 to 100
}

export type CropRatio = 'free' | '1:1' | '4:3' | '16:9' | '3:2' | '9:16';

export interface GeometrySettings {
  rotation: number;     // 0, 90, 180, 270
  flipH: boolean;
  flipV: boolean;
  straighten: number;   // -45 to 45 degrees
  cropRatio: CropRatio;
  cropRect: { x: number; y: number; width: number; height: number }; // normalized 0..1
}

export type FilterType = 'none' | 'bw' | 'sepia' | 'vivid' | 'cinema' | 'warm' | 'cool' | 'dramatic' | 'noir';

export interface FilterSettings {
  type: FilterType;
  intensity: number; // 0 to 100
}

export type BlendMode = 'normal' | 'multiply' | 'screen' | 'overlay' | 'darken' | 'lighten';

export interface LayerItem {
  id: string;
  name: string;
  type: 'color' | 'vignette' | 'gradient';
  color: string;
  opacity: number; // 0 to 100
  blendMode: BlendMode;
  visible: boolean;
}

export interface BrushStroke {
  id: string;
  points: { x: number; y: number }[]; // normalized 0..1
  color: string;
  size: number;
  opacity: number;
  isEraser: boolean;
}

export interface TextOverlay {
  id: string;
  text: string;
  x: number; // normalized 0..1
  y: number; // normalized 0..1
  fontSize: number;
  fontFamily: 'sans-serif' | 'serif' | 'monospace' | 'cursive';
  color: string;
  opacity: number;
}

export interface CloneStamp {
  id: string;
  sourceX: number; // normalized 0..1
  sourceY: number; // normalized 0..1
  targetX: number; // normalized 0..1
  targetY: number; // normalized 0..1
  radius: number;  // in px
}

export interface MaskSettings {
  enabled: boolean;
  type: 'rect' | 'ellipse';
  invert: boolean;
  feather: number; // 0 to 50px
  x: number; // normalized center
  y: number;
  width: number;
  height: number;
  adjustments: {
    brightness: number;
    contrast: number;
    saturation: number;
  };
}

export interface EditorDocumentState {
  adjustments: ColorAdjustments;
  geometry: GeometrySettings;
  filter: FilterSettings;
  layers: LayerItem[];
  brushStrokes: BrushStroke[];
  textOverlays: TextOverlay[];
  cloneStamps: CloneStamp[];
  mask: MaskSettings;
}
