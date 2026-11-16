/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: ['class', '[data-theme="dark"]'],
  theme: {
    extend: {
      colors: {
        trd: {
          navy: '#080D1A',
          slate: '#1E293B',
          cobalt: '#1D4ED8',
          cyan: '#0EA5E9',
          platinum: '#F8FAFC',
          white: '#FFFFFF',
        },
      },
      fontFamily: {
        trd: ['Inter Display', 'Neue Haas Grotesk', 'system-ui', 'sans-serif'],
      },
      letterSpacing: {
        'trd-sigla': '0.05em',
        'trd-descriptor': '0.48em',
      },
    },
  },
};
