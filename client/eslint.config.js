import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import { defineConfig, globalIgnores } from 'eslint/config'
<<<<<<< HEAD
=======
import { defineConfig } from 'vite'
>>>>>>> origin/main



export default defineConfig([
<<<<<<< HEAD
  globalIgnores(['dist', '**/*.backup*']),
=======
  globalIgnores(['dist']),
>>>>>>> origin/main
  {
    files: ['**/*.{js,jsx}'],
    extends: [
      js.configs.recommended,
      reactHooks.configs['recommended-latest'],
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
      parserOptions: {
        ecmaVersion: 'latest',
        ecmaFeatures: { jsx: true },
        sourceType: 'module',
      },
    },
    rules: {
<<<<<<< HEAD
      'no-unused-vars': ['error', { varsIgnorePattern: '^[A-Z_]|^handleGoogleSignup$|^Icon$', argsIgnorePattern: '^Icon$' }],
      'react-refresh/only-export-components': 'off',
=======
      'no-unused-vars': ['error', { varsIgnorePattern: '^[A-Z_]' }],
>>>>>>> origin/main
    },
  },
])
