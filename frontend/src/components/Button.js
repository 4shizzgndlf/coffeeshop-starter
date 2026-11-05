// Button.js
export function createButton({
  label,
  type = "button",
  variant = "primary",
  size = "medium",
  disabled = false,
  loading = false,
  className = "",
  onClick
} = {}) {
  const button = document.createElement("button");

  // "submit" makes it submit its form; the default "button" does nothing on its own
  button.type = type;
  button.className = className ? `button ${className}` : "button";
  button.dataset.variant = variant;
  button.dataset.size = size;

  if (disabled) {
    button.disabled = true;
  }

  function render() {
    button.disabled = disabled || loading;
    button.dataset.loading = String(loading);

    button.innerHTML = loading
      ? `
        <span class="button__spinner" aria-hidden="true"></span>
        <span class="button__label"></span>
      `
      : `
        <span class="button__label"></span>
      `;

    // textContent, so a label can never inject HTML
    button.querySelector(".button__label").textContent = loading ? "Loading..." : label;
  }

  function handleClick(event) {
    if (!disabled && !loading) {
      onClick?.(event);
    }
  }

  button.addEventListener("click", handleClick);

  render();

  return {
    element: button,

    setLabel(value) {
      label = value;
      render();
    },

    setLoading(value) {
      loading = value;
      render();
    },

    setDisabled(value) {
      disabled = value;
      render();
    },

    destroy() {
      button.removeEventListener("click", handleClick);
      button.remove();
    }
  };
}
