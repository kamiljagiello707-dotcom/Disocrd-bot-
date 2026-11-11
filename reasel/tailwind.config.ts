import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: {
          DEFAULT: '#0a0a0a',
          50: '#f5f5f4',
          100: '#e7e5e4',
          200: '#c9c7c4',
          300: '#a3a19d',
          400: '#8a8884',
          500: '#5f5d5a',
          600: '#3a3937',
          700: '#262524',
          800: '#171716',
          900: '#0f0f0f',
          950: '#0a0a0a',
        },
        // Jedyny akcent marki – neonowa limonka
        acid: {
          DEFAULT: '#c6ff00',
          dim: '#9fcc00',
          glow: 'rgba(198,255,0,0.35)',
        },
      },
      fontFamily: {
        display: ['"Syne Variable"', 'Syne', 'system-ui', 'sans-serif'],
        sans: ['"Inter Variable"', 'Inter', 'system-ui', 'sans-serif'],
      },
      fontSize: {
        'display-xl': ['clamp(3.5rem, 13vw, 14rem)', { lineHeight: '0.85', letterSpacing: '-0.04em' }],
        'display-lg': ['clamp(2.75rem, 8vw, 8rem)', { lineHeight: '0.9', letterSpacing: '-0.03em' }],
        'display-md': ['clamp(2rem, 5vw, 4.5rem)', { lineHeight: '0.95', letterSpacing: '-0.02em' }],
      },
      transitionTimingFunction: {
        expo: 'cubic-bezier(0.16, 1, 0.3, 1)',
      },
      keyframes: {
        grain: {
          '0%, 100%': { transform: 'translate(0, 0)' },
          '10%': { transform: 'translate(-5%, -10%)' },
          '30%': { transform: 'translate(3%, -15%)' },
          '50%': { transform: 'translate(12%, 9%)' },
          '70%': { transform: 'translate(9%, 4%)' },
          '90%': { transform: 'translate(-1%, 7%)' },
        },
      },
      animation: {
        grain: 'grain 8s steps(10) infinite',
      },
    },
  },
  plugins: [],
};

export default config;
