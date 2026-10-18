# Git Workflow & Deployment Rules

> **MANDATORY RULES FOR ALL COMMITS AND PUSHES**

---

## 1. Explicit Permission Required Before Any Push
* **NEVER run `git push` without explicit user permission in the chat.**
* Always show the diff/commit summary and ask the user before performing any remote push.

---

## 2. No Direct Push to `main`
* **Direct pushes to `main` (or `master`) are STRICTLY FORBIDDEN.**
* All code changes must reach `main` exclusively through feature branches and Pull Requests (PRs).

---

## 3. Branching & PR Standard Workflow
For every change, task, or feature:
1. **Create and switch to a descriptive branch**:
   ```bash
   git checkout -b feature/<feature-name>
   # or
   git checkout -b fix/<bug-description>
   ```
2. **Make minimal, verified changes** and commit with descriptive semantic commit messages:
   ```bash
   git commit -m "feat(scope): concise description of changes"
   ```
3. **Request user approval before pushing**:
   * Present the branch name, commit message, and files changed.
   * Await explicit confirmation ("push it", "proceed", "yes").
4. **Push the feature branch**:
   ```bash
   git push -u origin feature/<feature-name>
   ```
5. **Open / propose the Pull Request** against `main`.

---

## 4. Credential & Environment Isolation
* Never commit secrets, tokens, `.env` files, or credentials.
* Never use global git config that leaks company or other profile credentials.
* Keep git configuration local (`user.name: VaibhavBytebuddy`).
