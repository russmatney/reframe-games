(ns games.pause.core
  (:require
   [re-frame.core :as rf]
   [games.events.interceptors :refer [game-db-interceptor]]))

(defn game-timer [id]
  {:id      id
   :->event (fn [gopts] [id gopts])})

(defn reg-events
  "Registers pause events with the passed options.

  Depends on db keys:
  - :paused?
  - :gameover?

  Expects `timers` like:
  `[{:id      ::game-tick
     :->event (fn [x] [::game-tick x])}]`
  "
  [{:keys
    [game-map-key timers
     ;; TODO remove these passed names in favor namespace+convention
     pause-event resume-event toggle-event]}]

  ;; pauses, ignoring whatever the current state is
  (rf/reg-event-fx
    pause-event
    [(game-db-interceptor game-map-key)]
    (fn [{:keys [db]} _game-opts]
      {:db             (assoc db :paused? true)
       :clear-timeouts timers}))

  ;; resumes the game
  (rf/reg-event-fx
    resume-event
    [(game-db-interceptor game-map-key)]
    (fn [{:keys [db]} game-opts]
      (let [updated-db (assoc db :paused? false)]
        (-> {:db updated-db}
            (assoc :dispatch-n
                   (map #((:->event %) game-opts)
                        timers))))))

  (rf/reg-event-fx
    toggle-event
    [(game-db-interceptor game-map-key)]
    ;; NOTE that events coming from keybds have extra event args,
    ;; so the interceptor passes it as a list rather than game-opts directly
    (fn [{:keys [db]} [game-opts]]
      (if-not (:gameover? db)
        (if (:paused? db)
          ;; unpause
          {:dispatch [resume-event game-opts]}
          ;; pause
          {:dispatch [pause-event game-opts]})))))
