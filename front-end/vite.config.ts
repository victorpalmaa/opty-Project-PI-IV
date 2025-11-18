/* --- IMPORTS ---*/
import path from 'path';
import react from '@vitejs/plugin-react-swc';
import { defineConfig, loadEnv } from 'vite';


/* --- EXPORTS ---*/
export default defineConfig(() => {

  // Load environment variables
  const env = loadEnv('', process.cwd(), '');

  return {

    // Server configuration
    server: {
      host: '::',
      port: env.APP_PORT ? parseInt(env.APP_PORT) : 3000,
    },

    // Plugins configuration
    plugins: [
      react(),
    ],

    // Alias configuration
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
  };
});
