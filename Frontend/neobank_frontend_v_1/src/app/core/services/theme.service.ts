// //-------------------------          theme.service.ts          -----------------------------//


import { Injectable, signal, effect } from '@angular/core';

type ThemeMode = 'dark' | 'light'; // ❌ removed 'auto'

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly STORAGE_KEY = 'theme';
  
  // only dark / light
  themeMode = signal<ThemeMode>('light');
  darkMode = signal<boolean>(false);

  constructor() {
    const savedTheme = localStorage.getItem(this.STORAGE_KEY) as ThemeMode | null;

    if (savedTheme === 'dark' || savedTheme === 'light') {
      this.themeMode.set(savedTheme);
    } else {
      this.themeMode.set('light');
    }

    const applyTheme = () => {
      const mode = this.themeMode();
      const isDark = mode === 'dark';

      this.darkMode.set(isDark);
      document.documentElement.classList.toggle('dark', isDark);
      localStorage.setItem(this.STORAGE_KEY, mode);
    };

    // initial apply
    applyTheme();

    // react to changes
    effect(() => {
      applyTheme();
    });

    // Keyboard shortcut: 't' to cycle theme
    // try {
    //   window.addEventListener('keydown', (ev) => {
    //     if (ev.key.toLowerCase() === 't' && !ev.metaKey && !ev.ctrlKey) {
    //       this.cycle();
    //     }
    //   });
    // } catch {
    //   // ignore
    // }
  }

  toggle() {
    // simple toggle
    const mode = this.themeMode();
    this.themeMode.set(mode === 'dark' ? 'light' : 'dark');
  }

  cycle(): void {
    // keep method, but now only toggles (since no auto)
    const mode = this.themeMode();
    const next: ThemeMode = mode === 'light' ? 'dark' : 'light';
    this.themeMode.set(next);
  }

  isDark(): boolean {
    return this.darkMode();
  }
}



// import { Injectable, signal, effect } from '@angular/core';
// type ThemeMode = 'dark'|'light'|'auto';

// @Injectable({ providedIn: 'root' })
// export class ThemeService {
//   private readonly STORAGE_KEY = 'theme';
  
//   // current mode: dark, light, or auto
//   themeMode = signal<ThemeMode>('auto');
//   // derived dark mode state
//   darkMode = signal<boolean>(false);
//   private mediaQueryList: MediaQueryList;

//   constructor() {
//     const savedTheme = localStorage.getItem(this.STORAGE_KEY) as ThemeMode | null;
//     if (savedTheme === 'dark' || savedTheme === 'light' || savedTheme === 'auto') {
//       this.themeMode.set(savedTheme);
//     } else {
//       this.themeMode.set('auto');
//     }

//     this.mediaQueryList = window.matchMedia('(prefers-color-scheme: dark)');

//     const applyTheme = () => {
//       const mode = this.themeMode();
//       let isDark = false;
//       if (mode === 'dark') {
//         isDark = true;
//       } else if (mode === 'light') {
//         isDark = false;
//       } else {
//         isDark = this.mediaQueryList.matches;
//       }
//       this.darkMode.set(isDark);
//       document.documentElement.classList.toggle('dark', isDark);
//       localStorage.setItem(this.STORAGE_KEY, mode);
//     };

//     // initial apply
//     applyTheme();

//     // react to changes in themeMode
//     effect(() => {
//       applyTheme();
//     });

//     // react to OS color scheme changes when in auto mode
//     const mqListener = (e: MediaQueryListEvent) => {
//       if (this.themeMode() === 'auto') {
//         const isDark = e.matches;
//         this.darkMode.set(isDark);
//         document.documentElement.classList.toggle('dark', isDark);
//       }
//     };
//     if (this.mediaQueryList.addEventListener) {
//       this.mediaQueryList.addEventListener('change', mqListener);
//     } else if ((this.mediaQueryList as any).addListener) {
//       (this.mediaQueryList as any).addListener(mqListener);
//     }

//     // Keyboard shortcut: 't' to cycle theme
//     try {
//       window.addEventListener('keydown', (ev) => {
//         if (ev.key.toLowerCase() === 't' && !ev.metaKey && !ev.ctrlKey) {
//           this.cycle();
//         }
//       });
//     } catch {
//       // ignore
//     }
//   }

//   toggle() {
//     // Quick toggle between dark/light; if in auto, switch to dark
//     const mode = this.themeMode();
//     if (mode === 'auto') {
//       this.themeMode.set('dark');
//     } else {
//       this.themeMode.set(mode === 'dark' ? 'light' : 'dark');
//     }
//   }

//   cycle(): void {
//     // Cycle through Light -> Dark -> Auto -> Light ...
//     const mode = this.themeMode();
//     const next: ThemeMode = mode === 'light' ? 'dark' : mode === 'dark' ? 'auto' : 'light';
//     this.themeMode.set(next);
//   }

//   isDark(): boolean {
//     return this.darkMode();
//   }
// }
