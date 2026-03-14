# Grizzly Engine: Python Extensions (Non-Standard Syntax)

**Project goal:** Grizzly Engine is a Python interpreter in Java, created to solve data mapping issues. The same `.py` file should run in both the Grizzly engine and standard Python. **Syntax and grammar must be 100% Python** except for the minimal extensions documented below.

This document lists the **only** non-Python extensions allowed. All other syntax and grammar must match standard Python.

---

## Allowed Extensions (4 Only)

These four extensions exist for data mapping ergonomics. Files using them **will not run** in standard Python; use standard Python alternatives if dual-run is required.

| Extension | Description | Standard Python Alternative |
|-----------|-------------|-----------------------------|
| **`?.`** | Safe attribute access (returns `None` if object is null) | `getattr(obj, 'attr', None)` or explicit null checks |
| **`?[`** | Safe subscript access (returns `None` if object is null) | `obj.get(key) if obj else None` or explicit null checks |
| **`now()`** | Current datetime builtin; `now("UTC")` for timezone | `from datetime import datetime` → `datetime.now()` / `datetime.now(timezone)` |
| **`formatDate(dt, fmt)`** | Format datetime to string (Java-style patterns) | `from datetime import datetime` → `dt.strftime("%Y-%m-%d")` |

### Notes

- **`formatDate(dt, pattern)`** uses **Java DateTimeFormatter patterns** (e.g. `yyyy-MM-dd`, `dd/MM/yyyy`), not Python `strftime` patterns (`%Y-%m-%d`). For Python-compatible formatting, use `datetime.strftime()` with `%`-style patterns.
- **`now()`** returns a `DateTimeValue` compatible with `formatDate()`. For Python-compatible code, use `from datetime import datetime`.

---

## Standard Python Features (No Extensions)

The following are **standard Python** and do NOT extend the language:

| Feature | Python Version | Notes |
|---------|----------------|-------|
| `match expr: case val: case _:` | Python 3.10+ | Structural pattern matching |
| F-strings `f"{expr}"` | Python 3.6+ | |
| Raw strings `r"..."` | Python 2+ | |
| List comprehensions `[x for x in it]` | Python 2.0+ | |
| Dict comprehensions `{k:v for x in it}` | Python 2.7+ / 3.0+ | |
| Ternary `x if cond else y` | Python 2.5+ | |
| `import re`, `from datetime import datetime`, etc. | Standard | Module system |

---

## Features NOT in Grizzly (Scope Limits)

Grizzly is a subset for data mapping, not full Python:

- No `switch expr: case x: default:` (this is **not Python**; Python uses `match` since 3.10)
- No classes
- No `async`/`await`
- No decorators
- Limited imports (re, datetime, decimal only)

---

## Summary

**Allowed extensions:** `?.` `?[` `now()` `formatDate()` — 4 only.

**All other syntax:** Must be valid Python. No hacks. Production-ready.
