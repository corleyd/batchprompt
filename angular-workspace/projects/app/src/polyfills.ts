(window as any).global = window;
(window as any).process = {
  env: { DEBUG: undefined },
  nextTick: function(cb: Function) {
    setTimeout(cb, 0);
  }
};
