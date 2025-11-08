import { EnvironmentConfig } from './environment.config';

export const environment: EnvironmentConfig = {
  production: true,
  apiUrl: 'https://api.car-sharing.local',
  auth: {
    issuer: 'https://identity.car-sharing.local/realms/car-sharing',
    clientId: 'car-sharing-spa',
    scope: 'openid profile email offline_access',
    redirectUri: '/auth/callback',
    postLogoutRedirectUri: '/',
    silentRefreshRedirectUri: '/silent-refresh.html',
    requireHttps: true
  }
};
