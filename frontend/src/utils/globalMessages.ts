const GLOBAL_ERROR_EVENT = 'agentic-pm:global-error';

type GlobalErrorDetail = {
  message: string;
};

export function publishGlobalError(message: string) {
  if (!message) return;
  window.dispatchEvent(
    new CustomEvent<GlobalErrorDetail>(GLOBAL_ERROR_EVENT, {
      detail: { message },
    }),
  );
}

export function subscribeGlobalError(handler: (message: string) => void): () => void {
  const listener = (event: Event) => {
    const customEvent = event as CustomEvent<GlobalErrorDetail>;
    const message = customEvent.detail?.message;
    if (message) handler(message);
  };
  window.addEventListener(GLOBAL_ERROR_EVENT, listener as EventListener);
  return () => window.removeEventListener(GLOBAL_ERROR_EVENT, listener as EventListener);
}
