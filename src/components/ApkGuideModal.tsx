import React, { useState } from 'react';
import {
  X,
  Smartphone,
  Github,
  Check,
  Copy,
  Terminal,
  ExternalLink,
  Layers,
  Sparkles,
  ArrowRight
} from 'lucide-react';

interface ApkGuideModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const ApkGuideModal: React.FC<ApkGuideModalProps> = ({ isOpen, onClose }) => {
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null);

  if (!isOpen) return null;

  const copyToClipboard = (text: string, index: number) => {
    navigator.clipboard.writeText(text);
    setCopiedIndex(index);
    setTimeout(() => setCopiedIndex(null), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-zinc-900 border border-zinc-700 rounded-xl shadow-2xl max-w-2xl w-full max-h-[90vh] flex flex-col overflow-hidden text-zinc-200">
        {/* Modal Header */}
        <div className="px-6 py-4 border-b border-zinc-800 flex items-center justify-between bg-zinc-950/60">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-400">
              <Smartphone className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white flex items-center gap-2">
                HyperEditor Pro para Android
                <span className="text-[10px] uppercase font-semibold px-2 py-0.5 rounded bg-emerald-950 text-emerald-300 border border-emerald-700">
                  APK Listo
                </span>
              </h2>
              <p className="text-xs text-zinc-400">
                Guía de descarga y compilación del APK nativo con aceleración por hardware
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-zinc-400 hover:text-white hover:bg-zinc-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6 overflow-y-auto space-y-6 text-xs text-zinc-300">
          {/* Method 1: GitHub Actions CI Build */}
          <div className="bg-zinc-950/60 border border-emerald-500/30 rounded-xl p-4 space-y-3">
            <div className="flex items-center justify-between">
              <span className="font-bold text-sm text-emerald-400 flex items-center gap-1.5">
                <Github className="w-4 h-4" />
                Opción 1: Descargar APK automático desde GitHub Actions
              </span>
              <span className="text-[10px] px-2 py-0.5 rounded bg-emerald-900/60 text-emerald-200 font-mono">
                Recomendada
              </span>
            </div>
            <p className="text-zinc-400 leading-relaxed">
              El repositorio incluye el workflow <code>.github/workflows/android-build.yml</code> configurado con <strong>JDK 17</strong> y <strong>Gradle 8.10.2</strong>. Al sincronizar con GitHub, la compilación se ejecuta en servidores en la nube y genera el APK listo para instalar en tu tablet o móvil.
            </p>

            <ol className="list-decimal list-inside space-y-1.5 text-zinc-300 pl-1">
              <li>Haz clic en el menú superior <strong>Settings / Export &rarr; Export to GitHub</strong>.</li>
              <li>En tu repositorio de GitHub, haz clic en la pestaña <strong>Actions</strong>.</li>
              <li>Abre la última ejecución de <strong>"Android CI Build APK"</strong>.</li>
              <li>En la sección <strong>Artifacts</strong>, haz clic en <strong>app-debug-apk</strong> para descargar el archivo zip que contiene el APK.</li>
            </ol>
          </div>

          {/* Method 2: Local Android Studio */}
          <div className="bg-zinc-950/60 border border-zinc-800 rounded-xl p-4 space-y-3">
            <div className="flex items-center justify-between">
              <span className="font-bold text-sm text-zinc-200 flex items-center gap-1.5">
                <Terminal className="w-4 h-4 text-indigo-400" />
                Opción 2: Compilación Local en Android Studio
              </span>
            </div>
            <p className="text-zinc-400 leading-relaxed">
              Si prefieres compilar directamente en tu ordenador:
            </p>

            <div className="space-y-2">
              <div className="bg-zinc-900 rounded-lg p-3 border border-zinc-800 font-mono text-[11px] text-zinc-300 flex items-center justify-between">
                <code>./gradlew assembleDebug</code>
                <button
                  onClick={() => copyToClipboard('./gradlew assembleDebug', 1)}
                  className="text-zinc-400 hover:text-white p-1 rounded hover:bg-zinc-800"
                  title="Copiar comando"
                >
                  {copiedIndex === 1 ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                </button>
              </div>
              <p className="text-[11px] text-zinc-500">
                El APK generado se guardará en <code>app/build/outputs/apk/debug/app-debug.apk</code>
              </p>
            </div>
          </div>

          {/* Method 3: Integration with Aves Gallery */}
          <div className="bg-zinc-950/60 border border-zinc-800 rounded-xl p-4 space-y-2.5">
            <span className="font-bold text-sm text-zinc-200 flex items-center gap-1.5">
              <Layers className="w-4 h-4 text-amber-400" />
              Integración Nativa con Aves Gallery & Galerías Externas
            </span>
            <p className="text-zinc-400 leading-relaxed">
              HyperEditor registra los filtros de Intent <code>ACTION_EDIT</code> y <code>ACTION_SEND</code> en <code>AndroidManifest.xml</code> con soporte para imágenes <code>image/*</code>. Al pulsar "Editar" o "Compartir" en <strong>Aves Gallery</strong>, HyperEditor aparece como editor predeterminado de sistema y devuelve la foto editada a la galería no destructivamente.
            </p>
          </div>
        </div>

        {/* Modal Footer */}
        <div className="px-6 py-3 border-t border-zinc-800 bg-zinc-950 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-lg text-xs transition-colors"
          >
            Entendido
          </button>
        </div>
      </div>
    </div>
  );
};
