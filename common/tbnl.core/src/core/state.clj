(ns core.state
  "global (rather than plugin) state")

(declare
 ;; state management
 add-state get-state reset-state update-state remove-state list-states
 ;; command management
 register-command unregister-command
 get-command list-commands
 defcommand)

(def ^:dynamic *state*
  "global state"
  (atom {}))

(defn add-state 
  "add the state with the init"
  [state init]
  (swap! *state* assoc state init))

(defn get-state 
  "get the state"
  [state]
  (get @*state* state))

(defn reset-state 
  "reset the state to the value"
  [state value]
  (swap! *state* assoc state value))

(defn update-state 
  "update state to (f state & args)"
  [state f & args]
  (apply swap! *state* update-in [state] f args))

(defn remove-state 
  "remote the state"
  [state]
  (swap! *state* dissoc state))

(defn list-states
  "list all states"
  []
  (keys @*state*))

;;; special states

;;; command dispatcher
(let [state-name :command-dispatch]
  (defn register-command
    "register command to command dispatcher"
    [command command-impl]
    (update-state state-name
                  assoc command command-impl))

  (defn unregister-command
    "undo register-command"
    [command]
    (update-state state-name
                  dissoc command))

  (defn get-command
    "get command"
    [command]
    (get (get-state state-name) command))

  (defn list-commands
    "list all commands"
    []
    (keys (get-state state-name)))

  (defmacro defcommand
    "define and register the command"
    [command & body]
    `(do
       (defn ~command
         ~@body)
       (register-command ~(keyword command) ~command))))
