export interface PollingController {
  start: () => void;
  stop: () => void;
  isRunning: () => boolean;
}

export interface PollingOptions {
  intervalMs: number;
  immediate?: boolean;
}

export function createPolling(task: () => Promise<void>, options: PollingOptions): PollingController {
  let timer: number | null = null;
  let active = false;
  let ticking = false;

  const runTick = async (): Promise<void> => {
    if (!active || ticking) {
      return;
    }
    ticking = true;
    try {
      await task();
    } finally {
      ticking = false;
      if (active) {
        timer = window.setTimeout(runTick, options.intervalMs);
      }
    }
  };

  return {
    start() {
      if (active) {
        return;
      }
      active = true;
      if (options.immediate !== false) {
        void runTick();
      } else {
        timer = window.setTimeout(runTick, options.intervalMs);
      }
    },
    stop() {
      active = false;
      if (timer !== null) {
        window.clearTimeout(timer);
        timer = null;
      }
    },
    isRunning() {
      return active;
    }
  };
}
