# Workspace Interaction & Learning Rules

> **MANDATORY PROTOCOL FOR ALL ASSISTANT RESPONSES IN ARCHITECTURE-NOTES**

---

## Rule 1: Extreme Technical Rigor via "WHAT" and "WHY"
Never provide superficial, high-level summaries. Every explanation must dissect the technology down to the **Operating System kernel level, network packets, byte-level frames, memory buffers, and CPU scheduling**.

Every concept must explicitly answer:
1. **WHAT IS IT (Raw Mechanics)?**:
   * How does it physically and logically work inside the OS kernel, memory, and network wire?
   * What data structures (ring buffers, file descriptors, sequence counters, frame bits) are involved?
   * Exact packet/frame diagrams and bit-level layouts.
2. **WHY DOES IT EXIST (Engineering Tradeoffs & History)?**:
   * What exact technical bottlenecks, kernel constraints, or RFC limitations forced engineers to invent it?
   * Why can't simpler alternatives work (counter-factual analysis)?
   * Real-world physical analogies.

---

## Rule 2: Strict Pedagogical Progression
1. **Phase 1: Deep Theory First**:
   * Explain the foundational network and operating system principles thoroughly before touching code.
2. **Phase 2: Verify Comprehension & Close All Doubts**:
   * Pause and invite the user to question every nuance.
   * Do not advance to implementation until the user explicitly confirms complete clarity.
3. **Phase 3: Code Walkthrough**:
   * Map every line of code directly back to the theory and data structures discussed.
4. **Phase 4: Git Feature Branch & Pull Request**:
   * Keep all work on isolated feature branches (`feature/<name>`).
   * **Zero direct pushes to `main`**.
   * **Zero git pushes without explicit user permission**.

---

## Rule 3: Self-Descriptive Markdown Documentation
* Maintain comprehensive, beautifully organized markdown files with self-descriptive filenames in the repository (e.g., `01_DEEP_DIVE_TCP_HTTP_AND_WEBSOCKET_NETWORKING.md`).
* Update these documentation files alongside the conversation so they serve as a permanent, enterprise-grade engineering reference.
