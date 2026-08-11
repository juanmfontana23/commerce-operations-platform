import { useState } from 'react';
import { LoginForm } from './features/auth/LoginForm';
import { OrdersDashboard } from './features/orders/OrdersDashboard';
import { clearApiCredentials, getSellerId } from './shared/api/authSession';

export function App() {
  const [sellerId, setSellerId] = useState(() => getSellerId());

  if (sellerId === null) {
    return <LoginForm onAuthenticated={() => setSellerId(getSellerId())} />;
  }

  return <OrdersDashboard sellerId={sellerId} onLogout={() => {
    clearApiCredentials();
    document.cookie = 'XSRF-TOKEN=; Max-Age=0';
    setSellerId(null);
  }} />;
}
