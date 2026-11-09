"use client";
import { useCallback, useEffect, useState } from "react";
import { adminError, adminRequest } from "@/services/admin-api";

export function useAdminResource<T>(path: string) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [version, setVersion] = useState(0);
  const refresh = useCallback(() => setVersion((v) => v + 1), []);
  useEffect(() => {
    let active = true;
    const timer = setTimeout(() => {
      setLoading(true);
      setError("");
      adminRequest<T>(path)
        .then((result) => {
          if (active) setData(result);
        })
        .catch((e) => {
          if (active) {
            setError(adminError(e));
            setData(null);
          }
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    }, 120);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [path, version]);
  return { data, error, loading, refresh };
}
