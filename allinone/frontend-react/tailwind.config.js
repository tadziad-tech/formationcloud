/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      dropShadow: {
        glass: '0 20px 35px rgba(0,0,0,0.45)',
      },
    },
  },
  plugins: [],
}
