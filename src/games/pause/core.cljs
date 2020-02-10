(ns games.pause.core
  (:require
   [re-frame.core :as rf]
   [games.events.interceptors :refer [game-db-interceptor]]))

(defn make-timer [id]
  {:id      id
   :->event (fn [gopts] [id gopts])})

(defn reg-pause-events
  "Registers pause events with the passed options.

  Reads db keys:
  - :paused?
  - :gameover?

  Sets db keys:
  - :paused?
  - :time

  Expects `timers` like:
  `[{:id      ::game-tick
     :->event (fn [x] [::game-tick x])}]`
  "
  [{:keys
    [;; TODO rename this key. do i really need this? could just be :game-dbs ?
     game-map-key
     timers]}]

  ;; pauses, ignoring whatever the current state is
  (rf/reg-event-fx
    ::pause
    [(game-db-interceptor game-map-key)]
    (fn [{:keys [db]} _game-opts]
      {:db             (assoc db :paused? true)
       :clear-timeouts timers}))

  ;; resumes the game
  (rf/reg-event-fx
    ::resume
    [(game-db-interceptor game-map-key)]
    (fn [{:keys [db]} game-opts]
      (let [updated-db (assoc db :paused? false)]
        (-> {:db updated-db}
            (assoc :dispatch-n
                   (map #((:->event %) game-opts)
                        timers))))))

  (rf/reg-event-fx
    ::toggle
    [(game-db-interceptor game-map-key)]
    ;; NOTE that events coming from keybds have extra event args,
    ;; so the interceptor passes it as a list rather than game-opts directly
    (fn [{:keys [db]} [game-opts]]
      (if-not (:gameover? db)
        (if (:paused? db)
          ;; unpause
          {:dispatch [::resume game-opts]}
          ;; pause
          {:dispatch [::pause game-opts]}))))

  (rf/reg-event-fx
    ::game-timer
    [(game-db-interceptor game-map-key)]
    (fn [{:keys [db]} game-opts]
      (let [{:keys [timer-increment]} db
            timer-increment           (or timer-increment 400)]
        {:db (update db :time #(+ % timer-increment))
         :timeout
         ;; TODO update id to use game name
         {:id    ::game-timer
          :event [::game-timer game-opts]
          :time  timer-increment}}))))
