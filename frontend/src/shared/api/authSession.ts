export interface ApiCredentials {
  username: string;
  password: string;
}

let activeCredentials: ApiCredentials | null = null;
let activeSellerId: number | null = null;

export function getApiCredentials(): ApiCredentials | null {
  return activeCredentials;
}

export function setApiCredentials(credentials: ApiCredentials): void {
  activeCredentials = credentials;
}

export function getSellerId(): number | null {
  return activeSellerId;
}

export function setSellerId(sellerId: number): void {
  activeSellerId = sellerId;
}

export function clearApiCredentials(): void {
  activeCredentials = null;
  activeSellerId = null;
}

export function basicAuthorization(credentials: ApiCredentials): string {
  return `Basic ${btoa(`${credentials.username}:${credentials.password}`)}`;
}
