import { useEffect, useState } from 'react';
import { Alert, Snackbar } from '@mui/material';
import { subscribeGlobalError } from '../utils/globalMessages';

export function GlobalErrorBar() {
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    return subscribeGlobalError((nextMessage) => setMessage(nextMessage));
  }, []);

  return (
    <Snackbar
      open={!!message}
      autoHideDuration={5000}
      onClose={() => setMessage(null)}
      anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
    >
      <Alert
        onClose={() => setMessage(null)}
        severity="error"
        variant="filled"
        sx={{ width: '100%', maxWidth: 720 }}
      >
        {message}
      </Alert>
    </Snackbar>
  );
}
