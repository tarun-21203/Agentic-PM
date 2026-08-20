import { CognitoUserPool, CognitoUser, AuthenticationDetails } from 'amazon-cognito-identity-js';

const USER_POOL_ID = (import.meta as any).env?.VITE_COGNITO_USER_POOL_ID ?? '';
const CLIENT_ID = (import.meta as any).env?.VITE_COGNITO_CLIENT_ID ?? '';

type AuthSession = {
  idTokenJwt: string;
};

const TOKEN_KEY = 'agenticPmIdToken';

type JwtPayload = {
  exp?: number;
  email?: string;
  'cognito:username'?: string;
};

function readToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function getAuthToken(): string | null {
  return readToken();
}

function parseJwtPayload(token: string): JwtPayload | null {
  try {
    const parts = token.split('.');
    if (parts.length < 2) return null;
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const json = atob(base64);
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}

function isTokenExpired(token: string): boolean {
  const payload = parseJwtPayload(token);
  if (!payload?.exp) return false;
  const nowSeconds = Math.floor(Date.now() / 1000);
  return payload.exp <= nowSeconds;
}

function friendlyCognitoError(err: unknown): string {
  const code = typeof err === 'object' && err && 'code' in err ? String((err as { code?: unknown }).code) : '';
  const message =
    typeof err === 'object' && err && 'message' in err ? String((err as { message?: unknown }).message) : '';

  switch (code) {
    case 'UserNotFoundException':
    case 'NotAuthorizedException':
      return 'Invalid email or password.';
    case 'UserNotConfirmedException':
      return 'Your account is not confirmed. Please verify your email first.';
    case 'PasswordResetRequiredException':
      return 'Password reset is required for this account.';
    case 'TooManyRequestsException':
      return 'Too many attempts. Please wait a minute and try again.';
    default:
      return message || 'Sign-in failed.';
  }
}

export function logout() {
  try {
    localStorage.removeItem(TOKEN_KEY);
  } catch {
    // ignore
  }
}

export async function signInWithEmailPassword(email: string, password: string): Promise<AuthSession> {
  if (!USER_POOL_ID || !CLIENT_ID) {
    throw new Error('Cognito is not configured (missing VITE_COGNITO_USER_POOL_ID / VITE_COGNITO_CLIENT_ID).');
  }

  // amazon-cognito-identity-js expects a pool config with only the poolId/clientId.
  // REGION is not strictly required for the user pool client when using client-id+pool-id.
  const poolData = {
    UserPoolId: USER_POOL_ID,
    ClientId: CLIENT_ID,
  };

  const userPool = new CognitoUserPool(poolData as any);
  const user = new CognitoUser({ Username: email, Pool: userPool });
  const authDetails = new AuthenticationDetails({
    Username: email,
    Password: password,
  });

  const session = await new Promise<AuthSession>((resolve, reject) => {
    user.authenticateUser(authDetails, {
      onSuccess: (result: any) => {
        // Prefer idToken (API Gateway JWT authorizer validates token claims).
        const idToken = result?.getIdToken?.();
        const jwt = idToken?.getJwtToken?.();
        if (!jwt) return reject(new Error('Missing idToken JWT in Cognito auth result.'));
        resolve({ idTokenJwt: jwt });
      },
      onFailure: (err: unknown) => reject(new Error(friendlyCognitoError(err))),
    });
  });

  try {
    localStorage.setItem(TOKEN_KEY, session.idTokenJwt);
  } catch {
    // ignore storage errors
  }

  return session;
}

export function isAuthenticated(): boolean {
  const token = readToken();
  if (!token) return false;
  if (isTokenExpired(token)) {
    logout();
    return false;
  }
  return true;
}

export function getAuthenticatedEmail(): string | null {
  const token = readToken();
  if (!token) return null;
  const payload = parseJwtPayload(token);
  return payload?.email || payload?.['cognito:username'] || null;
}

