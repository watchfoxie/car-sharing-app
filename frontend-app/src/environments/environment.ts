import { EnvironmentConfig } from './environment.config';

export const environment: EnvironmentConfig = {
  production: false,
  apiUrl: 'http://localhost:8080',
  auth: {
    issuer: 'http://localhost:8180/realms/car-sharing',
    clientId: 'car-sharing-spa',
    scope: 'openid profile email offline_access',
    redirectUri: '/auth/callback',
    postLogoutRedirectUri: '/',
    silentRefreshRedirectUri: '/silent-refresh.html',
    requireHttps: false
  }
};
