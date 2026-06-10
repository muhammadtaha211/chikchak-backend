import { useCallback, useEffect, useState } from 'react';

// Shared list-loading hook: tracks items/loading/error, reloads when `deps`
// change, and exposes `reload` for refreshing after an admin action.
export function useList(loadFn, deps) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setItems((await loadFn()) || []);
    } catch (e) {
      setError(e.message);
      setItems([]);
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    reload();
  }, [reload]);

  return { items, loading, error, reload };
}
