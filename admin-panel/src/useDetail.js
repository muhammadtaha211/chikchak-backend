import { useState } from 'react';

// State for a fetch-on-open detail dialog. Usage:
//   const detail = useDetail();
//   detail.show(() => api(`/api/admin/users/${id}`));
//   <DetailDialog open={detail.open} loading={detail.loading} ... onClose={detail.close} />
export function useDetail() {
  const [state, setState] = useState({ open: false, loading: false, error: null, data: null });

  async function show(fetchFn) {
    setState({ open: true, loading: true, error: null, data: null });
    try {
      const data = await fetchFn();
      setState({ open: true, loading: false, error: null, data });
    } catch (e) {
      setState({ open: true, loading: false, error: e.message, data: null });
    }
  }

  function close() {
    setState((s) => ({ ...s, open: false }));
  }

  return { ...state, show, close };
}
