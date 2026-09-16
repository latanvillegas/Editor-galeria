import { EditorDocumentState, BlendMode, FilterType } from '../types';

export const DEFAULT_DOCUMENT: EditorDocumentState = {
  adjustments: {
    brightness: 0,
    contrast: 0,
    saturation: 0,
    exposure: 0,
    temperature: 0,
    tint: 0,
  },
  geometry: {
    rotation: 0,
    flipH: false,
    flipV: false,
    straighten: 0,
    cropRatio: 'free',
    cropRect: { x: 0, y: 0, width: 1, height: 1 },
  },
  filter: {
    type: 'none',
    intensity: 100,
  },
  layers: [],
  brushStrokes: [],
  textOverlays: [],
  cloneStamps: [],
  mask: {
    enabled: false,
    type: 'rect',
    invert: false,
    feather: 20,
    x: 0.5,
    y: 0.5,
    width: 0.5,
    height: 0.5,
    adjustments: {
      brightness: 20,
      contrast: 15,
      saturation: 10,
    },
  },
};

export const SAMPLE_IMAGES = [
  {
    name: 'Montaña y Lago (Paisaje)',
    url: 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1600&q=80',
  },
  {
    name: 'Retrato en Golden Hour',
    url: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=1600&q=80',
  },
  {
    name: 'Arquitectura Minimalista',
    url: 'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=1600&q=80',
  },
  {
    name: 'Ciudad Nocturna Cyberpunk',
    url: 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=1600&q=80',
  },
];

export function mapBlendModeToCompositeOperation(mode: BlendMode): GlobalCompositeOperation {
  switch (mode) {
    case 'multiply': return 'multiply';
    case 'screen': return 'screen';
    case 'overlay': return 'overlay';
    case 'darken': return 'darken';
    case 'lighten': return 'lighten';
    default: return 'source-over';
  }
}

export function renderDocumentToCanvas(
  targetCanvas: HTMLCanvasElement,
  sourceImg: HTMLImageElement,
  doc: EditorDocumentState,
  options?: { isCompareMode?: boolean; isExport?: boolean }
) {
  const ctx = targetCanvas.getContext('2d', { willReadFrequently: true });
  if (!ctx) return;

  const naturalWidth = sourceImg.naturalWidth || sourceImg.width;
  const naturalHeight = sourceImg.naturalHeight || sourceImg.height;

  if (naturalWidth === 0 || naturalHeight === 0) return;

  // Determine target canvas dimensions
  const crop = doc.geometry.cropRect;
  const cropW = Math.max(10, Math.round(crop.width * naturalWidth));
  const cropH = Math.max(10, Math.round(crop.height * naturalHeight));

  const isRotated90or270 = doc.geometry.rotation === 90 || doc.geometry.rotation === 270;
  const finalWidth = isRotated90or270 ? cropH : cropW;
  const finalHeight = isRotated90or270 ? cropW : cropH;

  if (targetCanvas.width !== finalWidth || targetCanvas.height !== finalHeight) {
    targetCanvas.width = finalWidth;
    targetCanvas.height = finalHeight;
  }

  ctx.save();
  ctx.clearRect(0, 0, finalWidth, finalHeight);

  if (options?.isCompareMode) {
    // Show pristine original image fitted to current crop
    ctx.drawImage(
      sourceImg,
      crop.x * naturalWidth,
      crop.y * naturalHeight,
      crop.width * naturalWidth,
      crop.height * naturalHeight,
      0,
      0,
      finalWidth,
      finalHeight
    );
    ctx.restore();
    return;
  }

  // 1. Prepare base offscreen transformed image
  const offCanvas = document.createElement('canvas');
  offCanvas.width = finalWidth;
  offCanvas.height = finalHeight;
  const offCtx = offCanvas.getContext('2d');
  if (!offCtx) {
    ctx.restore();
    return;
  }

  offCtx.save();
  offCtx.translate(finalWidth / 2, finalHeight / 2);

  // Rotation & Straighten
  const totalRotationRad = ((doc.geometry.rotation + doc.geometry.straighten) * Math.PI) / 180;
  offCtx.rotate(totalRotationRad);

  // Flip
  const scaleX = doc.geometry.flipH ? -1 : 1;
  const scaleY = doc.geometry.flipV ? -1 : 1;
  offCtx.scale(scaleX, scaleY);

  // Draw cropped section
  offCtx.drawImage(
    sourceImg,
    crop.x * naturalWidth,
    crop.y * naturalHeight,
    crop.width * naturalWidth,
    crop.height * naturalHeight,
    -finalWidth / 2,
    -finalHeight / 2,
    finalWidth,
    finalHeight
  );
  offCtx.restore();

  // 2. Draw offCanvas into targetCanvas with color adjustments and CSS filters
  const { brightness, contrast, saturation, exposure, temperature, tint } = doc.adjustments;

  // Convert adjustments to canvas CSS filter string
  // Brightness: base 100% + slider% + exposure%
  const bVal = Math.max(0, 100 + brightness + exposure * 1.2);
  // Contrast: base 100% + slider%
  const cVal = Math.max(0, 100 + contrast);
  // Saturation: base 100% + slider%
  const sVal = Math.max(0, 100 + saturation);

  // Sepia / Hue rotate for temperature and tint
  const hueDeg = Math.round(tint * 0.9 + (temperature > 0 ? -temperature * 0.3 : -temperature * 0.4));
  const sepiaVal = temperature > 0 ? Math.min(60, Math.round(temperature * 0.4)) : 0;

  let filterStr = `brightness(${bVal}%) contrast(${cVal}%) saturate(${sVal}%)`;
  if (hueDeg !== 0) filterStr += ` hue-rotate(${hueDeg}deg)`;
  if (sepiaVal > 0) filterStr += ` sepia(${sepiaVal}%)`;

  // Apply stylize filter preset
  const fType = doc.filter.type;
  const fInt = doc.filter.intensity / 100;

  if (fType === 'bw') {
    filterStr += ` grayscale(${Math.round(100 * fInt)}%)`;
  } else if (fType === 'sepia') {
    filterStr += ` sepia(${Math.round(90 * fInt)}%) contrast(105%)`;
  } else if (fType === 'vivid') {
    filterStr += ` saturate(${Math.round(100 + 70 * fInt)}%) contrast(${Math.round(100 + 20 * fInt)}%)`;
  } else if (fType === 'cinema') {
    filterStr += ` contrast(${Math.round(100 + 35 * fInt)}%) saturate(${Math.round(100 - 15 * fInt)}%)`;
  } else if (fType === 'warm') {
    filterStr += ` sepia(${Math.round(35 * fInt)}%) saturate(${Math.round(100 + 20 * fInt)}%)`;
  } else if (fType === 'cool') {
    filterStr += ` hue-rotate(${Math.round(180 * fInt * 0.2)}deg) saturate(${Math.round(100 + 10 * fInt)}%)`;
  } else if (fType === 'dramatic') {
    filterStr += ` contrast(${Math.round(100 + 50 * fInt)}%) brightness(${Math.round(100 - 15 * fInt)}%)`;
  } else if (fType === 'noir') {
    filterStr += ` grayscale(100%) contrast(${Math.round(100 + 60 * fInt)}%) brightness(90%)`;
  }

  ctx.filter = filterStr;
  ctx.drawImage(offCanvas, 0, 0);
  ctx.filter = 'none';

  // 3. Render Layers Stack
  doc.layers.forEach((layer) => {
    if (!layer.visible || layer.opacity <= 0) return;

    ctx.save();
    ctx.globalAlpha = layer.opacity / 100;
    ctx.globalCompositeOperation = mapBlendModeToCompositeOperation(layer.blendMode);

    if (layer.type === 'color') {
      ctx.fillStyle = layer.color;
      ctx.fillRect(0, 0, finalWidth, finalHeight);
    } else if (layer.type === 'vignette') {
      const radius = Math.max(finalWidth, finalHeight) * 0.75;
      const grad = ctx.createRadialGradient(
        finalWidth / 2,
        finalHeight / 2,
        radius * 0.3,
        finalWidth / 2,
        finalHeight / 2,
        radius
      );
      grad.addColorStop(0, 'rgba(0,0,0,0)');
      grad.addColorStop(1, layer.color || 'rgba(0,0,0,0.8)');
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, finalWidth, finalHeight);
    } else if (layer.type === 'gradient') {
      const grad = ctx.createLinearGradient(0, 0, 0, finalHeight);
      grad.addColorStop(0, layer.color);
      grad.addColorStop(1, 'rgba(0,0,0,0)');
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, finalWidth, finalHeight);
    }
    ctx.restore();
  });

  // 4. Render Clone Stamps
  doc.cloneStamps.forEach((stamp) => {
    const srcX = stamp.sourceX * finalWidth;
    const srcY = stamp.sourceY * finalHeight;
    const dstX = stamp.targetX * finalWidth;
    const dstY = stamp.targetY * finalHeight;
    const r = Math.max(10, stamp.radius);

    ctx.save();
    ctx.beginPath();
    ctx.arc(dstX, dstY, r, 0, Math.PI * 2);
    ctx.clip();

    ctx.drawImage(
      targetCanvas,
      srcX - r,
      srcY - r,
      r * 2,
      r * 2,
      dstX - r,
      dstY - r,
      r * 2,
      r * 2
    );
    ctx.restore();
  });

  // 5. Render Brush Strokes
  doc.brushStrokes.forEach((stroke) => {
    if (stroke.points.length < 2) return;

    ctx.save();
    ctx.beginPath();
    const startX = stroke.points[0].x * finalWidth;
    const startY = stroke.points[0].y * finalHeight;
    ctx.moveTo(startX, startY);

    for (let i = 1; i < stroke.points.length; i++) {
      const px = stroke.points[i].x * finalWidth;
      const py = stroke.points[i].y * finalHeight;
      ctx.lineTo(px, py);
    }

    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.lineWidth = stroke.size;

    if (stroke.isEraser) {
      ctx.globalCompositeOperation = 'destination-out';
      ctx.strokeStyle = 'rgba(0,0,0,1)';
    } else {
      ctx.globalCompositeOperation = 'source-over';
      ctx.strokeStyle = stroke.color;
      ctx.globalAlpha = stroke.opacity;
    }

    ctx.stroke();
    ctx.restore();
  });

  // 6. Render Text Overlays
  doc.textOverlays.forEach((txt) => {
    if (!txt.text.trim()) return;

    ctx.save();
    ctx.font = `600 ${txt.fontSize}px ${txt.fontFamily}`;
    ctx.fillStyle = txt.color;
    ctx.globalAlpha = txt.opacity;

    const posX = txt.x * finalWidth;
    const posY = txt.y * finalHeight;

    // Multiline support
    const lines = txt.text.split('\n');
    lines.forEach((line, index) => {
      ctx.fillText(line, posX, posY + index * (txt.fontSize * 1.25));
    });
    ctx.restore();
  });

  ctx.restore();
}
