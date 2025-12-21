import { Injectable, signal, effect, PLATFORM_ID, inject, computed } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ThemeService } from './theme.service';

/**
 * Service that provides chart colors from CSS variables as hex values.
 * Reactive to theme changes - colors update automatically when dark mode toggles.
 */
@Injectable({ providedIn: 'root' })
export class ChartColorsService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);
  private readonly themeService = inject(ThemeService);

  // Trigger recalculation when theme changes
  private readonly themeVersion = signal(0);

  constructor() {
    if (this.isBrowser) {
      effect(() => {
        // Subscribe to theme changes
        this.themeService.isDark();
        // Increment version to trigger computed recalculation
        // Small delay to allow CSS variables to update
        setTimeout(() => this.themeVersion.update(v => v + 1), 50);
      });
    }
  }

  /** Chart series colors */
  readonly chart1 = computed(() => this.getHexColor('--chart-1'));
  readonly chart2 = computed(() => this.getHexColor('--chart-2'));
  readonly chart3 = computed(() => this.getHexColor('--chart-3'));
  readonly chart4 = computed(() => this.getHexColor('--chart-4'));
  readonly chart5 = computed(() => this.getHexColor('--chart-5'));

  /** Brand colors */
  readonly brand = computed(() => this.getHexColor('--brand'));
  readonly brandForeground = computed(() => this.getHexColor('--brand-foreground'));

  /** Status colors */
  readonly success = computed(() => this.getHexColor('--success'));
  readonly warning = computed(() => this.getHexColor('--warning'));
  readonly destructive = computed(() => this.getHexColor('--destructive'));

  /** Chart UI colors */
  readonly chartLabel = computed(() => this.getHexColor('--chart-label'));
  readonly chartGrid = computed(() => this.getHexColor('--chart-grid'));

  /** Background/foreground */
  readonly background = computed(() => this.getHexColor('--background'));
  readonly foreground = computed(() => this.getHexColor('--foreground'));
  readonly muted = computed(() => this.getHexColor('--muted'));
  readonly mutedForeground = computed(() => this.getHexColor('--muted-foreground'));

  /**
   * Get all chart colors as an array (useful for ApexCharts colors option)
   */
  readonly chartColors = computed(() => [
    this.chart1(),
    this.chart2(),
    this.chart3(),
    this.chart4(),
    this.chart5(),
  ]);

  /**
   * Get a CSS variable value and convert it to hex
   */
  private getHexColor(varName: string): string {
    // Depend on theme version to trigger recalculation
    this.themeVersion();

    if (!this.isBrowser) {
      return this.getFallbackColor(varName);
    }

    const value = getComputedStyle(document.documentElement)
      .getPropertyValue(varName)
      .trim();

    if (!value) {
      return this.getFallbackColor(varName);
    }

    return this.oklchToHex(value);
  }

  /**
   * Convert OKLCh color string to hex
   * Handles formats like: oklch(0.623 0.214 259.815) or oklch(0.145 0 0)
   */
  private oklchToHex(oklch: string): string {
    // Parse oklch values
    const match = oklch.match(/oklch\(\s*([\d.]+)\s+([\d.]+)\s+([\d.]+)/);
    if (!match) {
      // Try to handle as already hex or rgb
      if (oklch.startsWith('#')) return oklch;
      return '#888888'; // Fallback gray
    }

    const L = parseFloat(match[1]);
    const C = parseFloat(match[2]);
    const H = parseFloat(match[3]);

    // Convert OKLCh to OKLab
    const hRad = (H * Math.PI) / 180;
    const a = C * Math.cos(hRad);
    const b = C * Math.sin(hRad);

    // Convert OKLab to linear sRGB
    const l_ = L + 0.3963377774 * a + 0.2158037573 * b;
    const m_ = L - 0.1055613458 * a - 0.0638541728 * b;
    const s_ = L - 0.0894841775 * a - 1.2914855480 * b;

    const l = l_ * l_ * l_;
    const m = m_ * m_ * m_;
    const s = s_ * s_ * s_;

    // Linear sRGB to sRGB
    let r = +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s;
    let g = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s;
    let bVal = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s;

    // Clamp and apply gamma
    const gammaCorrect = (x: number): number => {
      if (x <= 0) return 0;
      if (x >= 1) return 1;
      return x <= 0.0031308 ? 12.92 * x : 1.055 * Math.pow(x, 1 / 2.4) - 0.055;
    };

    r = Math.round(gammaCorrect(r) * 255);
    g = Math.round(gammaCorrect(g) * 255);
    bVal = Math.round(gammaCorrect(bVal) * 255);

    // Convert to hex
    const toHex = (n: number): string => {
      const hex = Math.max(0, Math.min(255, n)).toString(16);
      return hex.length === 1 ? '0' + hex : hex;
    };

    return `#${toHex(r)}${toHex(g)}${toHex(bVal)}`;
  }

  /**
   * Fallback colors for SSR or when CSS variable is not found
   */
  private getFallbackColor(varName: string): string {
    const fallbacks: Record<string, string> = {
      '--chart-1': '#3b82f6',
      '--chart-2': '#f59e0b',
      '--chart-3': '#8b5cf6',
      '--chart-4': '#10b981',
      '--chart-5': '#6b7280',
      '--brand': '#f59e0b',
      '--brand-foreground': '#78350f',
      '--success': '#10b981',
      '--warning': '#f59e0b',
      '--destructive': '#ef4444',
      '--chart-label': '#6b7280',
      '--chart-grid': '#e5e7eb',
      '--background': '#ffffff',
      '--foreground': '#171717',
      '--muted': '#f5f5f5',
      '--muted-foreground': '#737373',
    };
    return fallbacks[varName] ?? '#888888';
  }
}
