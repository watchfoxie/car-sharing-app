/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#3f51b5',
        accent: '#ff4081',
        warn: '#f44336',
      },
    },
  },
  plugins: [],
};
