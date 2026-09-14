# Lisp 2.0

Call a function with parentheses: `(print 5)`. Quote a value or list with `'`, for example `'(1 2 3)` or `(print 'Hello)`. Define a variable with `(define x 5)` and a function with `(defun say-5 () (print 5))`. `(env)` lists the supported surface.

The evaluator bounds syntax depth, parsed nodes, globals, list length, call depth, and work per tick. Arithmetic, immutable list operations, conditionals, `progn`, `eval`, `define`, `set!`, and `defun` are available. Invalid input, division by zero, runaway recursion, and corrupt snapshots enter a stable fault state.

Robot actions are top-level forms: `(front)`, `(back)`, `(left)`, `(right)`, `(up)`, `(down)`, `(mine)`, and `(scan)`. Keeping device calls top-level guarantees that a waiting action is retried exactly once without replaying unrelated side effects.
