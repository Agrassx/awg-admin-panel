// Webpack dev server configuration for local development
if (config.devServer) {
    config.devServer.port = 3000;
    config.devServer.hot = true;
    config.devServer.historyApiFallback = true;
    
    // Proxy API requests to backend
    config.devServer.proxy = {
        '/api': {
            target: 'http://localhost:8080',
            changeOrigin: true,
            secure: false,
            logLevel: 'debug'
        }
    };
}
