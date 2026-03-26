/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useEffect, useRef, useState } from 'react';
import { Atari2600 } from './emulator/Atari2600';
import { Play, Square, Upload } from 'lucide-react';

export default function App() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [emulator, setEmulator] = useState<Atari2600 | null>(null);
  const [isRunning, setIsRunning] = useState(false);

  useEffect(() => {
    if (canvasRef.current && !emulator) {
      const emu = new Atari2600(canvasRef.current);
      setEmulator(emu);
      
      // Load a dummy program that changes background color
      // LDA #$xx, STA COLUBK, JMP loop
      const dummyROM = new Uint8Array(4096);
      dummyROM[0] = 0xA9; // LDA
      dummyROM[1] = 0x45; // Color value
      dummyROM[2] = 0x8D; // STA
      dummyROM[3] = 0x09; // COLUBK (0x09)
      dummyROM[4] = 0x00;
      dummyROM[5] = 0x4C; // JMP
      dummyROM[6] = 0x00; // to 0x1000
      dummyROM[7] = 0x10;
      
      // Set reset vector to 0x1000
      dummyROM[0xFFC] = 0x00;
      dummyROM[0xFFD] = 0x10;
      
      emu.loadROM(dummyROM);
    }
    
    return () => {
      if (emulator) {
        emulator.stop();
      }
    };
  }, [canvasRef, emulator]);

  const handleStart = () => {
    if (emulator) {
      emulator.audio.resume();
      emulator.start();
      setIsRunning(true);
    }
  };

  const handleStop = () => {
    if (emulator) {
      emulator.stop();
      setIsRunning(false);
    }
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file && emulator) {
      const reader = new FileReader();
      reader.onload = (event) => {
        const buffer = event.target?.result as ArrayBuffer;
        const rom = new Uint8Array(buffer);
        emulator.stop();
        emulator.loadROM(rom);
        emulator.audio.resume();
        emulator.start();
        setIsRunning(true);
      };
      reader.readAsArrayBuffer(file);
    }
  };

  // Joystick handlers
  const handleButtonDown = (btn: 'up' | 'down' | 'left' | 'right' | 'button') => {
    if (emulator) emulator.joystick[btn] = true;
  };

  const handleButtonUp = (btn: 'up' | 'down' | 'left' | 'right' | 'button') => {
    if (emulator) emulator.joystick[btn] = false;
  };

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100 flex flex-col items-center justify-between p-4 md:p-8 font-sans touch-none select-none">
      
      {/* Header */}
      <div className="w-full max-w-md flex justify-between items-center mb-2">
        <h1 className="text-lg font-bold tracking-wider text-zinc-300">ATARI 2600</h1>
        <div className="flex gap-2">
          <label className="p-2 bg-zinc-800 rounded-full cursor-pointer hover:bg-zinc-700 active:bg-zinc-600 transition-colors">
            <Upload size={20} />
            <input type="file" accept=".bin,.a26" className="hidden" onChange={handleFileUpload} />
          </label>
          {isRunning ? (
            <button onClick={handleStop} className="p-2 bg-red-900/50 text-red-500 rounded-full hover:bg-red-900/80 active:bg-red-800 transition-colors">
              <Square size={20} fill="currentColor" />
            </button>
          ) : (
            <button onClick={handleStart} className="p-2 bg-green-900/50 text-green-500 rounded-full hover:bg-green-900/80 active:bg-green-800 transition-colors">
              <Play size={20} fill="currentColor" />
            </button>
          )}
        </div>
      </div>

      {/* Screen */}
      <div className="w-full max-w-md aspect-[4/3] bg-black rounded-lg border-4 border-zinc-800 shadow-2xl overflow-hidden relative flex-shrink-0">
        <canvas 
          ref={canvasRef} 
          className="w-full h-full object-fill"
          style={{ imageRendering: 'pixelated' }}
        />
        {/* Scanline overlay effect */}
        <div className="absolute inset-0 pointer-events-none bg-[linear-gradient(transparent_50%,rgba(0,0,0,0.25)_50%)] bg-[length:100%_4px]" />
      </div>

      {/* Controls */}
      <div className="w-full max-w-md flex justify-between items-center mt-4 px-4 flex-1">
        
        {/* D-Pad */}
        <div className="relative w-32 h-32">
          <div className="absolute inset-0 bg-zinc-800 rounded-full shadow-inner opacity-50"></div>
          
          {/* Up */}
          <button 
            className="absolute top-0 left-1/2 -translate-x-1/2 w-10 h-12 bg-zinc-700 rounded-t-lg active:bg-zinc-500 focus:outline-none"
            onPointerDown={() => handleButtonDown('up')}
            onPointerUp={() => handleButtonUp('up')}
            onPointerLeave={() => handleButtonUp('up')}
          ></button>
          
          {/* Down */}
          <button 
            className="absolute bottom-0 left-1/2 -translate-x-1/2 w-10 h-12 bg-zinc-700 rounded-b-lg active:bg-zinc-500 focus:outline-none"
            onPointerDown={() => handleButtonDown('down')}
            onPointerUp={() => handleButtonUp('down')}
            onPointerLeave={() => handleButtonUp('down')}
          ></button>
          
          {/* Left */}
          <button 
            className="absolute left-0 top-1/2 -translate-y-1/2 w-12 h-10 bg-zinc-700 rounded-l-lg active:bg-zinc-500 focus:outline-none"
            onPointerDown={() => handleButtonDown('left')}
            onPointerUp={() => handleButtonUp('left')}
            onPointerLeave={() => handleButtonUp('left')}
          ></button>
          
          {/* Right */}
          <button 
            className="absolute right-0 top-1/2 -translate-y-1/2 w-12 h-10 bg-zinc-700 rounded-r-lg active:bg-zinc-500 focus:outline-none"
            onPointerDown={() => handleButtonDown('right')}
            onPointerUp={() => handleButtonUp('right')}
            onPointerLeave={() => handleButtonUp('right')}
          ></button>
          
          {/* Center */}
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-10 h-10 bg-zinc-700 rounded-sm">
            <div className="absolute inset-2 rounded-full bg-zinc-800/50"></div>
          </div>
        </div>

        {/* Action Button */}
        <div className="relative w-20 h-20">
          <button 
            className="absolute inset-0 bg-red-600 rounded-full shadow-[0_4px_0_rgb(153,27,27)] active:shadow-[0_0px_0_rgb(153,27,27)] active:translate-y-1 transition-all focus:outline-none flex items-center justify-center border-4 border-zinc-800"
            onPointerDown={() => handleButtonDown('button')}
            onPointerUp={() => handleButtonUp('button')}
            onPointerLeave={() => handleButtonUp('button')}
          >
            <span className="text-red-950 font-bold text-sm">FIRE</span>
          </button>
        </div>

      </div>
      
      {/* Footer Info */}
      <div className="mt-8 text-xs text-zinc-600 font-mono text-center">
        <p>6507 CPU • TIA Video/Audio • 8KB Memory Map</p>
        <p>Upload a .bin ROM to play</p>
      </div>

    </div>
  );
}
