(ns core.state)

(declare add-state
         get-state
         reset-state
         update-state
         remove-state)

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
