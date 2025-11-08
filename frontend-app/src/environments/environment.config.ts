export interface EnvironmentConfig {
  production: boolean;
  apiUrl: string;
  auth: {
    issuer: string;
    clientId: string;
    scope: string;
    redirectUri: string;
    postLogoutRedirectUri: string;
    silentRefreshRedirectUri: string;
    requireHttps: boolean;
  };
}
